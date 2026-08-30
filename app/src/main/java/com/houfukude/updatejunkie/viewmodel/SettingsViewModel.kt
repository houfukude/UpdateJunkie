package com.houfukude.updatejunkie.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.houfukude.updatejunkie.data.SettingsRepository
import com.houfukude.updatejunkie.data.ThemeConfig
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)

    val themeConfig: StateFlow<ThemeConfig> = repository.themeConfig
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeConfig.FOLLOW_SYSTEM
        )

    fun setThemeConfig(themeConfig: ThemeConfig) {
        viewModelScope.launch {
            repository.setThemeConfig(themeConfig)
        }
    }
}
