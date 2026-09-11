package com.houfukude.updatejunkie.utils

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.ServerSocket
import java.net.Socket

/**
 * 局域网配置共享管理器。
 * 提供基于 NSD (Network Service Discovery) 的设备发现及简单的 TCP 配置文件传输。
 */
class LanManager(private val context: Context) {
    private val nsdManager = try {
        context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    } catch (e: Exception) {
        Log.e("LanManager", "Failed to get NsdManager", e)
        null
    }
    private val serviceType = "_updatejunkie._tcp"
    private var serverSocket: ServerSocket? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    private val _discoveredDevices = MutableStateFlow<List<NsdServiceInfo>>(emptyList())
    val discoveredDevices = _discoveredDevices.asStateFlow()

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning = _isServerRunning.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * 启动局域网服务，允许其他设备发现并获取配置。
     * @param jsonProvider 提供待传输的 JSON 字符串的回调
     */
    fun startServer(jsonProvider: () -> String) {
        if (_isServerRunning.value) return

        scope.launch {
            try {
                serverSocket = ServerSocket(0).also { socket ->
                    val port = socket.localPort
                    registerService(port)
                    _isServerRunning.value = true
                    Log.d("LanManager", "Server started on port $port")

                    while (isActive) {
                        val client = try {
                            socket.accept()
                        } catch (e: Exception) {
                            break
                        }
                        launch { handleClient(client, jsonProvider()) }
                    }
                }
            } catch (e: Exception) {
                Log.e("LanManager", "Failed to start server", e)
                _isServerRunning.value = false
            }
        }
    }

    /**
     * 停止局域网服务并注销 NSD 注册。
     */
    fun stopServer() {
        _isServerRunning.value = false
        try {
            serverSocket?.close()
            serverSocket = null
        } catch (e: Exception) { /* ignore */
        }

        registrationListener?.let {
            try {
                nsdManager?.unregisterService(it)
            } catch (e: Exception) { /* ignore */
            }
            registrationListener = null
        }
    }

    private fun registerService(port: Int) {
        val manager = nsdManager ?: return

        // 进一步限制服务名称：移除所有空格，并限制长度在 32 字符以内
        val deviceModel = Build.MODEL.filter { it.isLetterOrDigit() || it == '-' || it == '_' }
            .ifBlank { "Device" }
            .take(20)

        val name = "UJ-${deviceModel}-${(100..999).random()}"
        val type = this.serviceType

        Log.d(
            "LanManager",
            "Registering service: final_name='$name', final_type='$type', port=$port"
        )

        val serviceInfo = NsdServiceInfo().apply {
            setServiceName(name)
            setServiceType(type)
            setPort(port)
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {
                Log.d(
                    "LanManager",
                    "Service registered successfully: name=${info.serviceName}, type=${info.serviceType}"
                )
            }

            override fun onRegistrationFailed(info: NsdServiceInfo, error: Int) {
                Log.e("LanManager", "Service registration failed: error_code=$error, info=$info")
            }

            override fun onServiceUnregistered(info: NsdServiceInfo) {
                Log.d("LanManager", "Service unregistered: ${info.serviceName}")
            }

            override fun onUnregistrationFailed(info: NsdServiceInfo, error: Int) {
                Log.e("LanManager", "Service unregistration failed: $error")
            }
        }

        try {
            Log.i("LanManager", "Calling nsdManager.registerService...")
            manager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: IllegalArgumentException) {
            Log.e("LanManager", "Invalid service info! name='$name', type='$type'", e)

            // 回退尝试：使用最简单的名称
            if (name != "UpdateJunkie") {
                Log.i("LanManager", "Retrying with fallback name 'UpdateJunkie'...")
                val fallbackInfo = NsdServiceInfo().apply {
                    setServiceName("UpdateJunkie")
                    setServiceType(type)
                    setPort(port)
                }
                try {
                    manager.registerService(
                        fallbackInfo,
                        NsdManager.PROTOCOL_DNS_SD,
                        registrationListener
                    )
                } catch (e2: Exception) {
                    Log.e("LanManager", "Fallback registration failed", e2)
                    _isServerRunning.value = false
                }
            } else {
                _isServerRunning.value = false
            }
        } catch (e: Exception) {
            Log.e("LanManager", "Unexpected error during registration", e)
            _isServerRunning.value = false
        }
    }

    private fun handleClient(socket: Socket, json: String) {
        socket.use {
            try {
                it.getOutputStream().use { out ->
                    out.write(json.toByteArray())
                    out.flush()
                }
            } catch (e: Exception) {
                Log.e("LanManager", "Error sending data to client", e)
            }
        }
    }

    /**
     * 开始扫描局域网内的其他服务。
     */
    fun startDiscovery() {
        val manager = nsdManager ?: return
        _discoveredDevices.value = emptyList()
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                try {
                    manager.stopServiceDiscovery(this)
                } catch (e: Exception) {
                }
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                try {
                    manager.stopServiceDiscovery(this)
                } catch (e: Exception) {
                }
            }

            override fun onDiscoveryStarted(serviceType: String) {
                Log.d("LanManager", "Discovery started")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d("LanManager", "Discovery stopped")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType == serviceType || serviceInfo.serviceType == "$serviceType.") {
                    manager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {}
                        override fun onServiceResolved(info: NsdServiceInfo) {
                            val current = _discoveredDevices.value.toMutableList()
                            if (current.none { it.serviceName == info.serviceName }) {
                                current.add(info)
                                _discoveredDevices.value = current
                            }
                        }
                    })
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                val current =
                    _discoveredDevices.value.filter { it.serviceName != serviceInfo.serviceName }
                _discoveredDevices.value = current
            }
        }

        manager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    /**
     * 停止扫描。
     */
    fun stopDiscovery() {
        discoveryListener?.let {
            try {
                nsdManager?.stopServiceDiscovery(it)
            } catch (e: Exception) { /* ignore */
            }
            discoveryListener = null
        }
    }

    /**
     * 从指定设备下载配置。
     */
    suspend fun fetchConfig(info: NsdServiceInfo): String? = withContext(Dispatchers.IO) {
        try {
            Socket(info.host, info.port).use { socket ->
                socket.soTimeout = 5000
                socket.getInputStream().bufferedReader().readText()
            }
        } catch (e: Exception) {
            Log.e("LanManager", "Failed to fetch config from ${info.host}", e)
            null
        }
    }
}
