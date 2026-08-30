package com.houfukude.updatejunkie.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
    private val _error = MutableStateFlow<String?>(null)

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

    private val _isShizukuAvailable = MutableStateFlow(false)
    val isShizukuAvailable: StateFlow<Boolean> = _isShizukuAvailable.asStateFlow()

    private val _hasShizukuPermission = MutableStateFlow(false)
    val hasShizukuPermission: StateFlow<Boolean> = _hasShizukuPermission.asStateFlow()

    init {
        refreshStatus()
        loadApps()
    }

    fun refreshStatus() {
        _isShizukuAvailable.value = ShizukuManager.isAvailable()
        _hasShizukuPermission.value = ShizukuManager.hasPermission()
    }

    fun loadApps() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _allApps.value = repository.getInstalledApps(includeSystemApps = true)
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
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
