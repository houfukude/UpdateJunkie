package com.houfukude.updatejunkie.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 应用唯一的 DataStore 实例，文件名 `settings.preferences_pb`。 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * 设置项的持久化仓库，基于 DataStore Preferences 实现。
 *
 * 所有读取接口返回冷流（Flow），写入接口为 suspend 函数，需在协程中调用。
 *
 * @property context 应用上下文，用于访问 DataStore
 */
class SettingsRepository(private val context: Context) {

    /** DataStore 中使用的键集合。 */
    private object PreferencesKeys {
        /** 主题模式，取值见 [ThemeConfig] 的枚举名 */
        val THEME_MODE = stringPreferencesKey("theme_mode")
        /** 是否在列表中显示系统应用 */
        val SHOW_SYSTEM = booleanPreferencesKey("show_system")
        /** 是否在列表中显示已禁用的应用 */
        val SHOW_DISABLED = booleanPreferencesKey("show_disabled")
        /** 已勾选的安装来源标签集合 */
        val SELECTED_INSTALLERS = stringSetPreferencesKey("selected_installers")
    }

    private companion object {
        /**
         * DataStore 的 StringSet 不支持存放 null，
         * 用该哨兵值代表"未知来源（null）"，读写时做双向转换。
         */
        const val NULL_LABEL_KEY = "__null_label__"
    }

    /**
     * 当前主题配置流，未设置或取值非法时回退为 [ThemeConfig.FOLLOW_SYSTEM]。
     */
    val themeConfig: Flow<ThemeConfig> = context.dataStore.data
        .map { preferences ->
            val themeName = preferences[PreferencesKeys.THEME_MODE] ?: ThemeConfig.FOLLOW_SYSTEM.name
            try {
                ThemeConfig.valueOf(themeName)
            } catch (e: IllegalArgumentException) {
                ThemeConfig.FOLLOW_SYSTEM
            }
        }

    /** 是否显示系统应用，默认 false。 */
    val showSystem: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.SHOW_SYSTEM] ?: false }

    /** 是否显示已禁用应用，默认 false。 */
    val showDisabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.SHOW_DISABLED] ?: false }

    /**
     * 已勾选的安装来源标签集合，元素可为 null（表示未知来源）。
     * 读取时会将哨兵值 [NULL_LABEL_KEY] 还原为 null。
     */
    val selectedInstallers: Flow<Set<String?>> = context.dataStore.data
        .map { preferences ->
            val set = preferences[PreferencesKeys.SELECTED_INSTALLERS] ?: emptySet()
            set.map { if (it == NULL_LABEL_KEY) null else it }.toSet()
        }

    /**
     * 持久化主题配置。
     *
     * @param themeConfig 要保存的主题模式
     */
    suspend fun setThemeConfig(themeConfig: ThemeConfig) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeConfig.name
        }
    }

    /**
     * 持久化"是否显示系统应用"。
     *
     * @param show true 表示显示系统应用
     */
    suspend fun setShowSystem(show: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SHOW_SYSTEM] = show }
    }

    /**
     * 持久化"是否显示已禁用应用"。
     *
     * @param show true 表示显示已禁用应用
     */
    suspend fun setShowDisabled(show: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SHOW_DISABLED] = show }
    }

    /**
     * 持久化已勾选的安装来源标签集合。
     *
     * @param installers 安装来源标签集合，其中的 null 会被转换为哨兵值 [NULL_LABEL_KEY] 后存储
     */
    suspend fun setSelectedInstallers(installers: Set<String?>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_INSTALLERS] = installers.map { it ?: NULL_LABEL_KEY }.toSet()
        }
    }
}
