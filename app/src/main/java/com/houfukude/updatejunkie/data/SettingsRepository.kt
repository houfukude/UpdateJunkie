package com.houfukude.updatejunkie.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.houfukude.updatejunkie.data.SettingsRepository.Companion.NULL_LABEL_KEY
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
        /** 语言配置，取值见 [LanguageConfig] 的枚举名 */
        val LANGUAGE_MODE = stringPreferencesKey("language_mode")
        /** 是否在列表中显示系统应用 */
        val SHOW_SYSTEM = booleanPreferencesKey("show_system")
        /** 是否在列表中显示已禁用的应用 */
        val SHOW_DISABLED = booleanPreferencesKey("show_disabled")

        /** 是否仅显示已配置更新地址的应用 */
        val SHOW_CONFIGURED_ONLY = booleanPreferencesKey("show_configured_only")
        /** 已勾选的安装来源标签集合 */
        val SELECTED_INSTALLERS = stringSetPreferencesKey("selected_installers")

        /** 配置的更新地址 (URL) */
        val UPDATE_URL = stringPreferencesKey("update_url")

        /** 最后一次导入配置的 URL */
        val LAST_IMPORT_URL = stringPreferencesKey("last_import_url")
    }

    private companion object {
        /**
         * DataStore 的StringSet 不支持存放 null，
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

    /**
     * 当前语言配置流，未设置或取值非法时回退为 [LanguageConfig.FOLLOW_SYSTEM]。
     */
    val languageConfig: Flow<LanguageConfig> = context.dataStore.data
        .map { preferences ->
            val langName = preferences[PreferencesKeys.LANGUAGE_MODE] ?: LanguageConfig.FOLLOW_SYSTEM.name
            try {
                LanguageConfig.valueOf(langName)
            } catch (e: IllegalArgumentException) {
                LanguageConfig.FOLLOW_SYSTEM
            }
        }

    /** 是否显示系统应用，默认 false。 */
    val showSystem: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.SHOW_SYSTEM] ?: false }

    /** 是否显示已禁用应用，默认 false。 */
    val showDisabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.SHOW_DISABLED] ?: false }

    /** 是否仅显示已配置更新地址的应用，默认 false。 */
    val showConfiguredOnly: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.SHOW_CONFIGURED_ONLY] ?: false }

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
     * 配置的更新地址流。
     */
    val updateUrl: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.UPDATE_URL] ?: "" }

    /**
     * 最后一次导入配置的 URL 流。
     */
    val lastImportUrl: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.LAST_IMPORT_URL] }

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
     * 持久化语言配置。
     *
     * @param languageConfig 要保存的语言配置
     */
    suspend fun setLanguageConfig(languageConfig: LanguageConfig) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE_MODE] = languageConfig.name
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
     * 持久化"是否仅显示已配置更新地址的应用"。
     *
     * @param show true 表示仅显示已配置应用
     */
    suspend fun setShowConfiguredOnly(show: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SHOW_CONFIGURED_ONLY] = show }
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

    /**
     * 持久化配置的更新地址。
     *
     * @param url 要保存的更新地址
     */
    suspend fun saveUpdateUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.UPDATE_URL] = url
        }
    }

    /**
     * 持久化最后一次导入配置的 URL。
     *
     * @param url 要保存的 URL
     */
    suspend fun setLastImportUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_IMPORT_URL] = url
        }
    }
}
