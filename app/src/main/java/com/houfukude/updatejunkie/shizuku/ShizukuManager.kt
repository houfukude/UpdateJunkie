package com.houfukude.updatejunkie.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.houfukude.updatejunkie.IShizukuService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import kotlin.time.Duration.Companion.milliseconds

/**
 * Shizuku 权限与特权服务的封装管理器。
 *
 * 通过 Shizuku User Service (Binder) 高效读取包信息，替代低效的 Shell dumpsys。
 */
object ShizukuManager {
    /** 请求 Shizuku 授权时使用的一次性请求码。 */
    const val REQUEST_CODE_SHIZUKU = 1001

    private var userService: IShizukuService? = null
    private var serviceDeferred = CompletableDeferred<IShizukuService>()
    private var isBinding = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            android.util.Log.i("ShizukuManager", "Service connected: $name")
            val stub = IShizukuService.Stub.asInterface(service)
            userService = stub
            isBinding = false
            if (!serviceDeferred.isCompleted) {
                serviceDeferred.complete(stub)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            android.util.Log.w("ShizukuManager", "Service disconnected")
            userService = null
            isBinding = false
            if (serviceDeferred.isCompleted) {
                serviceDeferred = CompletableDeferred<IShizukuService>()
            }
        }
    }

    /**
     * 系统内置的安装器包名集合。
     */
    private val GENERIC_INSTALLERS = setOf(
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.shell",
        "android"
    )

    /**
     * 绑定 Shizuku User Service。
     */
    fun bindService(context: Context) {
        if (userService != null || !hasPermission() || isBinding) return
        isBinding = true

        val isDebug = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val args = Shizuku.UserServiceArgs(ComponentName(context.packageName, ShizukuUserService::class.java.name))
            .processNameSuffix("service")
            .debuggable(isDebug)
            .version(1)
            .daemon(false)
            .tag("app_update_junkie_service")

        try {
            android.util.Log.d("ShizukuManager", "Binding user service...")
            Shizuku.bindUserService(args, serviceConnection)
        } catch (e: Exception) {
            android.util.Log.e("ShizukuManager", "Bind failed", e)
            isBinding = false
        }
    }

    private suspend fun getService(): IShizukuService? {
        if (userService != null) return userService
        if (!hasPermission()) {
            android.util.Log.w("ShizukuManager", "No permission, skip service")
            return null
        }
        
        android.util.Log.d("ShizukuManager", "Waiting for service...")
        return withTimeoutOrNull(3000.milliseconds) {
            serviceDeferred.await()
        }
    }

    /**
     * 对指定包获取安装信息。
     */
    suspend fun getInstallInfo(packageName: String): PackageInstallInfo? = withContext(Dispatchers.IO) {
        val service = getService() ?: run {
            android.util.Log.w("ShizukuManager", "Service not available for $packageName")
            return@withContext null
        }

        try {
            android.util.Log.d("ShizukuManager", "Calling service for $packageName")
            val bundle = service.getInstallInfo(packageName) ?: return@withContext null
            val initiating = bundle.getString("initiating")
            val originating = bundle.getString("originating")
            val users: List<Int> = bundle.getIntArray("users")?.toList() ?: emptyList()

            PackageInstallInfo(
                installerPackageName = resolveInstaller(initiating, originating),
                users = users
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun resolveInstaller(initiating: String?, originating: String?): String? {
        // 1. 优先使用原始来源 (Originating)，如果是 Chrome 或浏览器下载通常在此处
        if (originating != null && !GENERIC_INSTALLERS.contains(originating)) {
            return originating
        }
        // 2. 其次使用发起者 (Initiating)，真正的应用商店来源通常在此处
        if (initiating != null && !GENERIC_INSTALLERS.contains(initiating)) {
            return initiating
        }
        // 3. 回退逻辑：如果以上都不是商店，则返回非空的原始值（包括 ADB 等）
        return originating ?: initiating
    }

    suspend fun resolveUserId(packageName: String): Int {
        return getInstallInfo(packageName)?.users?.firstOrNull() ?: 0
    }

    data class PackageInstallInfo(
        val installerPackageName: String?,
        val users: List<Int>
    )

    suspend fun getInstalledUsers(packageName: String): List<Int> {
        return getInstallInfo(packageName)?.users ?: emptyList()
    }

    fun isInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    fun isAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    fun hasPermission(): Boolean {
        return try {
            val res = if (Shizuku.isPreV11()) false
            else Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            android.util.Log.d("ShizukuManager", "hasPermission: $res")
            res
        } catch (e: Throwable) {
            android.util.Log.e("ShizukuManager", "checkPermission failed", e)
            false
        }
    }

    fun requestPermission(requestCode: Int = REQUEST_CODE_SHIZUKU) {
        try {
            if (!Shizuku.isPreV11()) {
                Shizuku.requestPermission(requestCode)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun getUid(): Int {
        return try {
            Shizuku.getUid()
        } catch (e: Throwable) {
            e.printStackTrace()
            -1
        }
    }
}
