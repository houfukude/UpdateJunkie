package com.houfukude.updatejunkie.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.houfukude.updatejunkie.data.SettingsRepository
import com.houfukude.updatejunkie.data.ThemeConfig
import com.houfukude.updatejunkie.data.LanguageConfig
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 设置页的 ViewModel，负责主题配置的读取与写入。
 *
 * @param application 应用实例，用于创建 [SettingsRepository]
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    /** 设置持久化仓库。 */
    private val repository = SettingsRepository(application)

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
}
