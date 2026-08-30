package com.houfukude.updatejunkie.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.houfukude.updatejunkie.data.AppLoadResult
import com.houfukude.updatejunkie.data.AppRepository
import com.houfukude.updatejunkie.data.SettingsRepository
import com.houfukude.updatejunkie.model.AppInfo
import com.houfukude.updatejunkie.shizuku.ShizukuManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AppListViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)
    private val settingsRepository = SettingsRepository(application)

    companion object {
        const val ADB_INSTALLER = "ADB 安装"
    }

    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    private val _isRefreshing = MutableStateFlow(false)
    private val _loadProgress = MutableStateFlow(0f)
    private val _loadProgressText = MutableStateFlow("")
    private val _error = MutableStateFlow<String?>(null)

    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    val loadProgress: StateFlow<Float> = _loadProgress.asStateFlow()
    val loadProgressText: StateFlow<String> = _loadProgressText.asStateFlow()

    val selectedInstallers: StateFlow<Set<String?>> = settingsRepository.selectedInstallers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val showSystem: StateFlow<Boolean> = settingsRepository.showSystem
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val showDisabled: StateFlow<Boolean> = settingsRepository.showDisabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    data class FilterParams(
        val selectedInstallers: Set<String?>,
        val showSystem: Boolean,
        val showDisabled: Boolean
    )

    private val filterParams = combine(
        selectedInstallers,
        showSystem,
        showDisabled
    ) { selected, showSystem, showDisabled ->
        FilterParams(selected, showSystem, showDisabled)
    }

    val availableInstallers: StateFlow<List<String?>> = _allApps.map { apps ->
        apps.map { app ->
            if (app.isAdbInstalled) ADB_INSTALLER
            else app.installerLabel
        }.distinct().sortedBy { it ?: "" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<AppListUiState> = combine(
        _allApps, _isLoading, _error, filterParams
    ) { allApps, loading, error, filters ->
        when {
            loading -> AppListUiState.Loading
            error != null -> AppListUiState.Error(error)
            else -> {
                val filteredApps = allApps.filter { app ->
                    val matchSystem = if (filters.showSystem) true else !app.isSystemApp
                    val matchDisabled = if (filters.showDisabled) true else app.isEnabled
                    val matchInstaller = if (filters.selectedInstallers.isEmpty()) {
                        true
                    } else {
                        val appSource = if (app.isAdbInstalled) ADB_INSTALLER else app.installerLabel
                        filters.selectedInstallers.contains(appSource)
                    }
                    matchSystem && matchDisabled && matchInstaller
                }
                AppListUiState.Success(filteredApps)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppListUiState.Loading)

    private val _isShizukuInstalled = MutableStateFlow(false)
    val isShizukuInstalled: StateFlow<Boolean> = _isShizukuInstalled.asStateFlow()

    private val _isShizukuAvailable = MutableStateFlow(false)
    val isShizukuAvailable: StateFlow<Boolean> = _isShizukuAvailable.asStateFlow()

    private val _hasShizukuPermission = MutableStateFlow(false)
    val hasShizukuPermission: StateFlow<Boolean> = _hasShizukuPermission.asStateFlow()

    init {
        refreshStatus()
        loadApps()
    }

    fun refreshStatus() {
        _isShizukuInstalled.value = ShizukuManager.isInstalled(getApplication())
        _isShizukuAvailable.value = ShizukuManager.isAvailable()
        _hasShizukuPermission.value = ShizukuManager.hasPermission()
    }

    fun loadApps() {
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
                                val appInfo = result.app
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
                Toast.makeText(getApplication(), "应用列表加载完成", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
                _isRefreshing.value = false
            }
        }
    }

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

    fun toggleSystemFilter() {
        viewModelScope.launch {
            settingsRepository.setShowSystem(!showSystem.value)
        }
    }

    fun toggleDisabledFilter() {
        viewModelScope.launch {
            settingsRepository.setShowDisabled(!showDisabled.value)
        }
    }

    fun requestShizukuPermission() {
        ShizukuManager.requestPermission()
    }
}

sealed class AppListUiState {
    object Loading : AppListUiState()
    data class Success(val apps: List<AppInfo>) : AppListUiState()
    data class Error(val message: String) : AppListUiState()
}
