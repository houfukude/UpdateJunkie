package com.houfukude.updatejunkie.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.houfukude.updatejunkie.R
import com.houfukude.updatejunkie.data.AppConfigRepository
import com.houfukude.updatejunkie.data.AppLoadResult
import com.houfukude.updatejunkie.data.AppRepository
import com.houfukude.updatejunkie.data.SettingsRepository
import com.houfukude.updatejunkie.model.AppInfo
import com.houfukude.updatejunkie.shizuku.ShizukuManager
import com.houfukude.updatejunkie.utils.MarketUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 应用列表页的 ViewModel。
 *
 * 负责：加载已安装应用（含进度上报）、维护 Shizuku 状态、管理筛选条件，
 * 并将"全量应用 + 筛选条件"合并为 UI 可直接消费的 [AppListUiState]。
 *
 * @param application 应用实例，用于创建 [AppRepository] 与 [SettingsRepository]
 */
class AppListViewModel(application: Application) : AndroidViewModel(application) {
    /** 应用数据来源仓库。 */
    private val repository = AppRepository(application)
    /** 筛选条件持久化仓库。 */
    private val settingsRepository = SettingsRepository(application)

    /** 应用配置（如更新 URL）持久化仓库。 */
    private val appConfigRepository = AppConfigRepository.getInstance(application)

    companion object {
        /** ADB / 命令行安装的应用在筛选器中使用的统一标签（内部标识）。 */
        const val ADB_INSTALLER = "__adb_installed__"

        /** 自更新应用（安装来源为自身包名）的统一标签。 */
        const val SELF_UPDATER = "__self_updating__"
    }

    /**
     * 安装来源筛选条目模型。
     *
     * @property key 内部标识（包名或内部常量）
     * @property label 展示给用户的友好名称
     */
    data class InstallerFilterItem(
        val key: String?,
        val label: String
    )

    /** 全量应用列表（未过滤），按名称升序维护。 */
    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    /** 是否处于首次加载状态（展示整屏 Loading）。 */
    private val _isLoading = MutableStateFlow(true)
    /** 是否处于加载中状态（展示顶部进度条）。 */
    private val _isRefreshing = MutableStateFlow(false)
    /** 加载进度，取值 0f ~ 1f。 */
    private val _loadProgress = MutableStateFlow(0f)
    /** 加载进度文案，形如 `12 / 100`。 */
    private val _loadProgressText = MutableStateFlow("")
    /** 加载过程中的错误信息，null 表示无错误。 */
    private val _error = MutableStateFlow<String?>(null)

    /** 搜索关键词。 */
    private val _searchQuery = MutableStateFlow("")

    /** 搜索关键词的 StateFlow。 */
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** 是否正在加载应用列表。 */
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    /** 加载进度，取值 0f ~ 1f。 */
    val loadProgress: StateFlow<Float> = _loadProgress.asStateFlow()
    /** 加载进度文案，形如 `12 / 100`。 */
    val loadProgressText: StateFlow<String> = _loadProgressText.asStateFlow()

    /** 已勾选的安装来源标签集合，空集合表示不按来源过滤。 */
    val selectedInstallers: StateFlow<Set<String?>> = settingsRepository.selectedInstallers
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /** 是否显示系统应用。 */
    val showSystem: StateFlow<Boolean> = settingsRepository.showSystem
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** 是否显示已禁用的应用。 */
    val showDisabled: StateFlow<Boolean> = settingsRepository.showDisabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** 是否仅显示已配置更新地址的应用。 */
    val showConfiguredOnly: StateFlow<Boolean> = settingsRepository.showConfiguredOnly
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /**
     * 参与列表过滤的一组条件参数。
     *
     * @property selectedInstallers 已勾选的安装来源标签；为空表示不过滤
     * @property showSystem 是否显示系统应用
     * @property showDisabled 是否显示已禁用的应用
     * @property showConfiguredOnly 是否仅显示已配置更新地址的应用
     * @property searchQuery 搜索关键词
     */
    data class FilterParams(
        val selectedInstallers: Set<String?>,
        val showSystem: Boolean,
        val showDisabled: Boolean,
        val showConfiguredOnly: Boolean,
        val searchQuery: String
    )

    /** 将四个筛选条件流合并为单一的 [FilterParams] 流。 */
    private val filterParams = combine(
        selectedInstallers,
        showSystem,
        showDisabled,
        showConfiguredOnly,
        searchQuery
    ) { selected, showSystem, showDisabled, showConfiguredOnly, query ->
        FilterParams(selected, showSystem, showDisabled, showConfiguredOnly, query)
    }

    /**
     * 当前所有应用中出现过的安装来源，按优先级与名称排序。
     * 优先从已扫描的应用中提取安装来源的友好名称（Label）。
     */
    val availableInstallers: StateFlow<List<InstallerFilterItem>> = _allApps.map { apps ->
        val pkgToLabel = apps.associate { it.packageName to it.label }
        val app = getApplication<Application>()

        // 提取所有出现的来源 Key 并建立对应的友好 Label
        val keyToLabel = apps.map { appInfo ->
            when {
                appInfo.isSystemApp -> MarketUtils.SYSTEM_APP_INSTALLER
                appInfo.isAdbInstalled -> ADB_INSTALLER
                appInfo.installerPackageName == appInfo.packageName -> SELF_UPDATER
                else -> appInfo.installerPackageName
            }
        }.distinct().associateWith { key ->
            when (key) {
                MarketUtils.SYSTEM_APP_INSTALLER -> app.getString(R.string.system_app)
                ADB_INSTALLER -> app.getString(R.string.adb_installed)
                SELF_UPDATER -> app.getString(R.string.self_updating_apps)
                else -> {
                    // 1. 优先使用扫描到的安装来源 App 本身的名称
                    // 2. 其次使用 MarketUtils 定义的名称
                    // 3. 最后回退到包名
                    pkgToLabel[key] ?: MarketUtils.getMarketLabel(app, key)
                }
            }
        }

        keyToLabel.keys.toList().sortedWith { a, b ->
            val pA = when (a) {
                MarketUtils.SYSTEM_APP_INSTALLER -> 0
                ADB_INSTALLER -> 1
                SELF_UPDATER -> 2
                else -> 3
            }
            val pB = when (b) {
                MarketUtils.SYSTEM_APP_INSTALLER -> 0
                ADB_INSTALLER -> 1
                SELF_UPDATER -> 2
                else -> 3
            }

            if (pA != pB) {
                pA.compareTo(pB)
            } else {
                val labelA = keyToLabel[a] ?: ""
                val labelB = keyToLabel[b] ?: ""
                labelA.compareTo(labelB, ignoreCase = true)
            }
        }.map { key ->
            InstallerFilterItem(key, keyToLabel[key] ?: "")
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 列表页的 UI 状态。
     *
     * 由全量应用、加载状态、错误信息和筛选条件合并计算而来：
     * 优先展示 Loading，其次 Error，最后按条件过滤后输出 [AppListUiState.Success]。
     */
    val uiState: StateFlow<AppListUiState> = combine(
        _allApps, _isLoading, _error, filterParams
    ) { allApps, loading, error, filters ->
        when {
            loading -> AppListUiState.Loading
            error != null -> AppListUiState.Error(error)
            else -> {
                val app = getApplication<Application>()
                val pkgToLabel = allApps.associate { it.packageName to it.label }

                // 计算当前所有应用实际持有的安装来源 Key，用于校验筛选条件的有效性
                val currentAvailableKeys = allApps.map { appInfo ->
                    when {
                        appInfo.isSystemApp -> MarketUtils.SYSTEM_APP_INSTALLER
                        appInfo.isAdbInstalled -> ADB_INSTALLER
                        appInfo.installerPackageName == appInfo.packageName -> SELF_UPDATER
                        else -> appInfo.installerPackageName
                    }
                }.distinct().toSet()

                val filteredApps = allApps.filter { appInfo ->
                    val matchSystem = filters.showSystem || !appInfo.isSystemApp
                    val matchDisabled = filters.showDisabled || appInfo.isEnabled
                    val matchConfigured =
                        if (filters.showConfiguredOnly) appInfo.hasUpdateUrl else true

                    val appSourceKey = when {
                        appInfo.isSystemApp -> MarketUtils.SYSTEM_APP_INSTALLER
                        appInfo.isAdbInstalled -> ADB_INSTALLER
                        appInfo.installerPackageName == appInfo.packageName -> SELF_UPDATER
                        else -> appInfo.installerPackageName
                    }

                    val validSelected = filters.selectedInstallers.intersect(currentAvailableKeys)
                    val matchInstaller =
                        validSelected.isEmpty() || validSelected.contains(appSourceKey)
                    
                    val matchQuery = if (filters.searchQuery.isBlank()) {
                        true
                    } else {
                        val keywords = filters.searchQuery.trim().split(Regex("\\s+"))
                        keywords.all { keyword ->
                            appInfo.label.contains(keyword, ignoreCase = true) ||
                                    appInfo.packageName.contains(keyword, ignoreCase = true)
                        }
                    }
                    matchSystem && matchDisabled && matchConfigured && matchInstaller && matchQuery
                }.map { appInfo ->
                    // 优化展示标签：如果安装来源应用本身已安装，则使用其友好名称
                    val key = when {
                        appInfo.isSystemApp -> MarketUtils.SYSTEM_APP_INSTALLER
                        appInfo.isAdbInstalled -> ADB_INSTALLER
                        appInfo.installerPackageName == appInfo.packageName -> SELF_UPDATER
                        else -> appInfo.installerPackageName
                    }
                    val optimizedLabel = when (key) {
                        MarketUtils.SYSTEM_APP_INSTALLER -> app.getString(R.string.system_app)
                        ADB_INSTALLER -> app.getString(R.string.adb_installed)
                        SELF_UPDATER -> app.getString(R.string.self_updating_apps)
                        else -> pkgToLabel[key] ?: appInfo.installerLabel
                    }
                    if (appInfo.installerLabel != optimizedLabel) {
                        appInfo.copy(installerLabel = optimizedLabel)
                    } else {
                        appInfo
                    }
                }
                AppListUiState.Success(filteredApps)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppListUiState.Loading)

    /** Shizuku 应用是否已安装。 */
    private val _isShizukuInstalled = MutableStateFlow(false)
    val isShizukuInstalled: StateFlow<Boolean> = _isShizukuInstalled.asStateFlow()

    /** Shizuku 服务是否正在运行。 */
    private val _isShizukuAvailable = MutableStateFlow(false)
    val isShizukuAvailable: StateFlow<Boolean> = _isShizukuAvailable.asStateFlow()

    /** 本应用是否已获得 Shizuku 授权。 */
    private val _hasShizukuPermission = MutableStateFlow(false)
    val hasShizukuPermission: StateFlow<Boolean> = _hasShizukuPermission.asStateFlow()

    /**
     * 初始化：先刷新 Shizuku 状态，再开始加载应用列表。
     * 二者顺序不可颠倒，否则安装来源解析会因 Shizuku 状态未知而降级。
     */
    init {
        refreshStatus()
        loadApps()
        observeConfigChanges()
    }

    /**
     * 监听配置变更（如导入操作），实时更新列表中的状态。
     */
    private fun observeConfigChanges() {
        viewModelScope.launch {
            appConfigRepository.configsFlow.collect { configs ->
                // 仅在列表非空时批量更新，避免与初始加载冲突
                if (_allApps.value.isNotEmpty()) {
                    val updatedList = _allApps.value.map { app ->
                        val hasConfig = configs.has(app.packageName) &&
                                !configs.optJSONObject(app.packageName)?.optString("updateUrl")
                                    .isNullOrBlank()
                        if (app.hasUpdateUrl != hasConfig) {
                            app.copy(hasUpdateUrl = hasConfig)
                        } else {
                            app
                        }
                    }
                    if (updatedList != _allApps.value) {
                        _allApps.value = updatedList
                    }
                }
            }
        }
    }

    /**
     * 重新查询并更新 Shizuku 的安装、运行与授权状态。
     *
     * 应在 Activity 创建、Shizuku Binder 连接、以及授权结果回调时调用。
     */
    fun refreshStatus() {
        _isShizukuInstalled.value = ShizukuManager.isInstalled(getApplication())
        _isShizukuAvailable.value = ShizukuManager.isAvailable()
        _hasShizukuPermission.value = ShizukuManager.hasPermission()
    }

    /**
     * 加载（或重新加载）设备上的应用列表。
     *
     * 加载期间会持续更新 [loadProgress] 与 [loadProgressText]；
     * 收到第一个应用时即解除 Loading 状态，让列表尽早呈现；
     * 全部加载完成后弹出 Toast 提示，异常时写入 [_error]。
     */
    fun loadApps() {
        if (_isRefreshing.value) {
            val app = getApplication<Application>()
            Toast.makeText(app, R.string.already_refreshing, Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _isRefreshing.value = true
            _loadProgress.value = 0f
            _loadProgressText.value = ""
            _error.value = null
            _allApps.value = emptyList() // 开始加载前清空列表

            var totalCount = 0
            var loadedCount = 0
            
            try {
                repository.getInstalledAppsFlow(includeSystemApps = true)
                    .collect { result ->
                        when (result) {
                            is AppLoadResult.Total -> {
                                totalCount = result.count
                                _loadProgressText.value = "0 / $totalCount"
                            }
                            is AppLoadResult.App -> {
                                val appInfo = result.app.copy(
                                    hasUpdateUrl = !appConfigRepository.getUpdateUrl(result.app.packageName)
                                        .isNullOrBlank()
                                )
                                // 增量添加并重新排序
                                val currentList = _allApps.value.toMutableList()
                                currentList.add(appInfo)
                                _allApps.value = currentList.sortedBy { it.label.lowercase() }
                                
                                loadedCount++
                                if (totalCount > 0) {
                                    _loadProgress.value = loadedCount.toFloat() / totalCount
                                    _loadProgressText.value = "$loadedCount / $totalCount"
                                }

                                // 一旦获取到第一个应用，就取消 Loading 状态以展示界面
                                if (_isLoading.value) {
                                    _isLoading.value = false
                                }
                            }
                        }
                    }
                Toast.makeText(getApplication(), R.string.load_complete, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
                _isRefreshing.value = false
            }
        }
    }

    /**
     * 勾选或取消勾选某个安装来源，并持久化筛选结果。
     *
     * @param label 安装来源标签，null 表示"未知来源"
     */
    fun toggleInstallerFilter(label: String?) {
        viewModelScope.launch {
            val current = selectedInstallers.value.toMutableSet()
            if (current.contains(label)) {
                current.remove(label)
            } else {
                current.add(label)
            }
            settingsRepository.setSelectedInstallers(current)
        }
    }

    /** 反转并持久化"是否显示系统应用"的筛选开关。 */
    fun toggleSystemFilter() {
        viewModelScope.launch {
            settingsRepository.setShowSystem(!showSystem.value)
        }
    }

    /** 反转并持久化"是否显示已禁用应用"的筛选开关。 */
    fun toggleDisabledFilter() {
        viewModelScope.launch {
            settingsRepository.setShowDisabled(!showDisabled.value)
        }
    }

    /** 反转并持久化"是否仅显示已配置更新地址的应用"的筛选开关。 */
    fun toggleConfiguredOnlyFilter() {
        viewModelScope.launch {
            settingsRepository.setShowConfiguredOnly(!showConfiguredOnly.value)
        }
    }

    /**
     * 更新搜索关键词。
     *
     * @param query 搜索词
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * 向 Shizuku 发起授权请求。
     *
     * 授权结果通过 `Shizuku.OnRequestPermissionResultListener` 回调，
     * 最终由 [refreshStatus] 统一刷新状态。
     */
    fun requestShizukuPermission() {
        ShizukuManager.requestPermission()
    }

    /**
     * 获取指定应用的更新 URL。
     *
     * @param packageName 应用包名
     * @return 配置的 URL，若未设置则为 null
     */
    fun getUpdateUrl(packageName: String): String? {
        return appConfigRepository.getUpdateUrl(packageName)
    }

    /**
     * 设置并保存指定应用的更新 URL。
     *
     * @param packageName 应用包名
     * @param url 更新地址
     */
    fun setUpdateUrl(packageName: String, url: String) {
        appConfigRepository.setUpdateUrl(packageName, url)
        // 更新本地列表中的状态，触发 UI 刷新
        val currentList = _allApps.value.toMutableList()
        val index = currentList.indexOfFirst { it.packageName == packageName }
        if (index != -1) {
            currentList[index] = currentList[index].copy(hasUpdateUrl = url.isNotBlank())
            _allApps.value = currentList
        }
    }
}

/**
 * 应用列表页的 UI 状态。
 *
 * @see Loading 首次加载中，展示整屏进度条
 * @see Success 加载成功，携带过滤后的应用列表
 * @see Error 加载失败，携带错误信息
 */
sealed class AppListUiState {
    /** 首次加载中，列表内容为空。 */
    object Loading : AppListUiState()

    /**
     * 加载成功。
     *
     * @property apps 按当前筛选条件过滤后的应用列表
     */
    data class Success(val apps: List<AppInfo>) : AppListUiState()

    /**
     * 加载失败。
     *
     * @property message 错误描述
     */
    data class Error(val message: String) : AppListUiState()
}
