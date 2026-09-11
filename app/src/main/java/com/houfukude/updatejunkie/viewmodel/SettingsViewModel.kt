package com.houfukude.updatejunkie.viewmodel

import android.app.Application
import android.net.Uri
import android.net.nsd.NsdServiceInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.houfukude.updatejunkie.R
import com.houfukude.updatejunkie.data.AppConfigRepository
import com.houfukude.updatejunkie.data.LanguageConfig
import com.houfukude.updatejunkie.data.SettingsRepository
import com.houfukude.updatejunkie.data.ThemeConfig
import com.houfukude.updatejunkie.utils.LanManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

/**
 * 设置页的 ViewModel，负责主题配置的读取与写入。
 *
 * @param application 应用实例，用于创建 [SettingsRepository]
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    /** 设置持久化仓库。 */
    private val repository = SettingsRepository(application)

    /** 应用特定配置仓库。 */
    private val appConfigRepository = AppConfigRepository.getInstance(application)

    /** 局域网共享管理器。 */
    private val lanManager = LanManager(application)

    private val _eventFlow = MutableSharedFlow<SettingsEvent>()
    val eventFlow: SharedFlow<SettingsEvent> = _eventFlow.asSharedFlow()

    /** 局域网发现的设备列表。 */
    val discoveredDevices = lanManager.discoveredDevices

    /** 局域网服务端状态。 */
    val isLanServerRunning = lanManager.isServerRunning

    sealed class SettingsEvent {
        data class ShowToast(val messageRes: Int) : SettingsEvent()
        data class Error(val message: String) : SettingsEvent()
    }

    /**
     * 当前主题配置。
     *
     * 在 [viewModelScope] 中通过 [SharingStarted.WhileSubscribed] 转换为热流，
     * 无订阅者 5 秒后自动停止上游收集。
     */
    val themeConfig: StateFlow<ThemeConfig> = repository.themeConfig
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeConfig.FOLLOW_SYSTEM
        )

    /**
     * 当前语言配置。
     */
    val languageConfig: StateFlow<LanguageConfig> = repository.languageConfig
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LanguageConfig.FOLLOW_SYSTEM
        )

    /**
     * 更新并持久化主题配置。
     *
     * @param themeConfig 用户选择的主题模式
     */
    fun setThemeConfig(themeConfig: ThemeConfig) {
        viewModelScope.launch {
            repository.setThemeConfig(themeConfig)
        }
    }

    /**
     * 更新并持久化语言配置。
     *
     * @param languageConfig 用户选择的语言模式
     */
    fun setLanguageConfig(languageConfig: LanguageConfig) {
        viewModelScope.launch {
            repository.setLanguageConfig(languageConfig)
        }
    }

    /**
     * 将所有配置导出为 JSON 字符串。
     */
    fun exportConfig(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = appConfigRepository.getAllConfigs().toString(2)
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use {
                        it.write(json.toByteArray())
                    }
                }
                _eventFlow.emit(SettingsEvent.ShowToast(R.string.export_success))
            } catch (e: Exception) {
                _eventFlow.emit(SettingsEvent.Error(e.localizedMessage ?: "Export failed"))
            }
        }
    }

    /**
     * 从 URI 导入配置。
     */
    fun importConfigFromFile(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                        it.bufferedReader().readText()
                    }
                }
                if (json != null) {
                    processImport(json)
                }
            } catch (e: Exception) {
                _eventFlow.emit(SettingsEvent.Error(e.localizedMessage ?: "Import failed"))
            }
        }
    }

    /**
     * 从 URL 导入配置。
     */
    fun importConfigFromUrl(urlString: String) {
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    URL(urlString).readText()
                }
                processImport(json)
            } catch (e: Exception) {
                _eventFlow.emit(SettingsEvent.Error(e.localizedMessage ?: "Import failed"))
            }
        }
    }

    private suspend fun processImport(json: String) {
        try {
            val jsonObject = JSONObject(json)
            // 简单的格式校验：检查是否为有效的 JSON 对象
            // 这里可以根据实际需求增加更详细的校验
            appConfigRepository.importConfigs(jsonObject)
            _eventFlow.emit(SettingsEvent.ShowToast(R.string.import_success))
        } catch (e: Exception) {
            _eventFlow.emit(SettingsEvent.ShowToast(R.string.invalid_config_format))
        }
    }

    /** 开始局域网发现。 */
    fun startLanDiscovery() = lanManager.startDiscovery()

    /** 停止局域网发现。 */
    fun stopLanDiscovery() = lanManager.stopDiscovery()

    /** 切换局域网服务端。 */
    fun toggleLanServer() {
        if (lanManager.isServerRunning.value) {
            lanManager.stopServer()
        } else {
            lanManager.startServer { appConfigRepository.getAllConfigs().toString() }
        }
    }

    /** 从发现的设备导入。 */
    fun importFromLanDevice(device: NsdServiceInfo) {
        viewModelScope.launch {
            val json = lanManager.fetchConfig(device)
            if (json != null) {
                processImport(json)
            } else {
                _eventFlow.emit(SettingsEvent.Error("Import failed from ${device.serviceName}"))
            }
        }
    }

    /**
     * 获取所有应用配置的 JSON 字符串。
     */
    fun getConfigJson(): String {
        return appConfigRepository.getAllConfigs().toString(2)
    }

    override fun onCleared() {
        super.onCleared()
        lanManager.stopServer()
        lanManager.stopDiscovery()
    }
}
