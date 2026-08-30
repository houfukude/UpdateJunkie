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

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SHOW_SYSTEM = booleanPreferencesKey("show_system")
        val SHOW_DISABLED = booleanPreferencesKey("show_disabled")
        val SELECTED_INSTALLERS = stringSetPreferencesKey("selected_installers")
    }

    private companion object {
        const val NULL_LABEL_KEY = "__null_label__"
    }

    val themeConfig: Flow<ThemeConfig> = context.dataStore.data
        .map { preferences ->
            val themeName = preferences[PreferencesKeys.THEME_MODE] ?: ThemeConfig.FOLLOW_SYSTEM.name
            try {
                ThemeConfig.valueOf(themeName)
            } catch (e: IllegalArgumentException) {
                ThemeConfig.FOLLOW_SYSTEM
            }
        }

    val showSystem: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.SHOW_SYSTEM] ?: false }

    val showDisabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.SHOW_DISABLED] ?: false }

    val selectedInstallers: Flow<Set<String?>> = context.dataStore.data
        .map { preferences ->
            val set = preferences[PreferencesKeys.SELECTED_INSTALLERS] ?: emptySet()
            set.map { if (it == NULL_LABEL_KEY) null else it }.toSet()
        }

    suspend fun setThemeConfig(themeConfig: ThemeConfig) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeConfig.name
        }
    }

    suspend fun setShowSystem(show: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SHOW_SYSTEM] = show }
    }

    suspend fun setShowDisabled(show: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SHOW_DISABLED] = show }
    }

    suspend fun setSelectedInstallers(installers: Set<String?>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_INSTALLERS] = installers.map { it ?: NULL_LABEL_KEY }.toSet()
        }
    }
}
