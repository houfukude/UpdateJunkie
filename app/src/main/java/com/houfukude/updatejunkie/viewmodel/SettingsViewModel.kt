package com.houfukude.updatejunkie.viewmodel

import android.app.Application
import android.net.Uri
import android.net.nsd.NsdServiceInfo
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.houfukude.updatejunkie.BuildConfig
import com.houfukude.updatejunkie.R
import com.houfukude.updatejunkie.data.AppConfigRepository
import com.houfukude.updatejunkie.data.LanguageConfig
import com.houfukude.updatejunkie.data.SettingsRepository
import com.houfukude.updatejunkie.data.ThemeConfig
import com.houfukude.updatejunkie.shizuku.ShizukuManager
import com.houfukude.updatejunkie.utils.LanManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
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

    /** Shizuku 应用是否已安装。 */
    private val _isShizukuInstalled = MutableStateFlow(false)
    val isShizukuInstalled: StateFlow<Boolean> = _isShizukuInstalled.asStateFlow()

    /** Shizuku 服务是否正在运行。 */
    private val _isShizukuAvailable = MutableStateFlow(false)
    val isShizukuAvailable: StateFlow<Boolean> = _isShizukuAvailable.asStateFlow()

    /** 本应用是否已获得 Shizuku 授权。 */
    private val _hasShizukuPermission = MutableStateFlow(false)
    val hasShizukuPermission: StateFlow<Boolean> = _hasShizukuPermission.asStateFlow()

    private val _changelogState = MutableStateFlow<ChangelogState>(ChangelogState.Idle)
    val changelogState: StateFlow<ChangelogState> = _changelogState.asStateFlow()

    sealed class ChangelogState {
        object Idle : ChangelogState()
        object Loading : ChangelogState()
        data class Success(val version: String, val content: String) : ChangelogState()
        data class Error(val message: String) : ChangelogState()
    }

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

    init {
        refreshStatus()
    }

    /**
     * 重新查询并更新 Shizuku 的安装、运行与授权状态。
     */
    fun refreshStatus() {
        _isShizukuInstalled.value = ShizukuManager.isInstalled(getApplication())
        _isShizukuAvailable.value = ShizukuManager.isAvailable()
        _hasShizukuPermission.value = ShizukuManager.hasPermission()
    }

    /**
     * 向 Shizuku 发起授权请求。
     */
    fun requestShizukuPermission() {
        ShizukuManager.requestPermission()
    }

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
     * 最后一次使用的导入 URL。如果从未导入过，则返回默认的 GitHub Master 路径。
     */
    val lastImportUrl: StateFlow<String> = repository.lastImportUrl
        .map {
            it
                ?: "https://raw.githubusercontent.com/houfukude/UpdateJunkie/master/config/com.houfukude.updatejunkie_config.json"
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "https://raw.githubusercontent.com/houfukude/UpdateJunkie/master/config/com.houfukude.updatejunkie_config.json"
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
                // 持久化当前使用的 URL
                repository.setLastImportUrl(urlString)
                
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
            Log.e("SettingsViewModel", "Invalid config format", e)
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

    /**
     * 从 GitHub 获取当前版本的更新日志。
     */
    fun fetchChangelog() {
        viewModelScope.launch {
            _changelogState.value = ChangelogState.Loading
            try {
                val url =
                    "https://raw.githubusercontent.com/houfukude/UpdateJunkie/master/CHANGELOG.md"
                val fullContent = withContext(Dispatchers.IO) {
                    URL(url).readText()
                }

                val version = BuildConfig.VERSION_NAME

                // 优化后的正则：
                // 1. [^\n]* 确保只在标题行内匹配，不跨行
                // 2. (?:\r?\n)+ 匹配一个或多个换行符
                // 3. (.*?) 捕获正文
                // 4. (?=\r?\n(?:##|---)|\z) 匹配到下一个标题、分割线或文件末尾
                val sectionRegex = { v: String ->
                    Regex(
                        "##\\s*\\[${Regex.escape(v)}][^\\n]*(?:\\r?\\n)+(.*?)(?=\\r?\\n(?:##|---)|\\z)",
                        RegexOption.DOT_MATCHES_ALL
                    )
                }

                var match = sectionRegex(version).find(fullContent)

                // 如果当前版本没内容，尝试匹配 [未发布]
                if (match == null) {
                    match = sectionRegex("未发布").find(fullContent)
                }

                if (match != null) {
                    val content = match.groupValues[1].trim()
                        .replace(Regex("^- ", RegexOption.MULTILINE), "• ") // 简单的格式化，把 - 换成圆点
                    if (content.isNotEmpty()) {
                        _changelogState.value = ChangelogState.Success(version, content)
                    } else {
                        _changelogState.value =
                            ChangelogState.Error("Changelog section found but content is empty")
                    }
                } else {
                    _changelogState.value =
                        ChangelogState.Error("No changelog entry found for v$version")
                }
            } catch (e: Exception) {
                _changelogState.value =
                    ChangelogState.Error(e.localizedMessage ?: "Failed to fetch changelog")
            }
        }
    }

    fun dismissChangelog() {
        _changelogState.value = ChangelogState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        lanManager.stopServer()
        lanManager.stopDiscovery()
    }
}
