package com.houfukude.updatejunkie

import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.core.os.LocaleListCompat
import com.houfukude.updatejunkie.data.ThemeConfig
import com.houfukude.updatejunkie.data.LanguageConfig
import com.houfukude.updatejunkie.viewmodel.AppListViewModel
import com.houfukude.updatejunkie.viewmodel.SettingsViewModel
import com.houfukude.updatejunkie.ui.MainScreen
import com.houfukude.updatejunkie.ui.Screen
import com.houfukude.updatejunkie.ui.SettingsScreen
import com.houfukude.updatejunkie.ui.theme.UpdateJunkieTheme
import rikka.shizuku.Shizuku

/**
 * 应用唯一入口 Activity，承载整个 Compose 界面。
 *
 * 职责：
 * - 根据持久化的主题配置决定深浅色；
 * - 在 Main / Settings 两个页面间切换并处理返回键；
 * - 注册与注销 Shizuku 的状态监听。
 */
class MainActivity : AppCompatActivity() {

    /** 应用列表页的 ViewModel。 */
    private val viewModel: AppListViewModel by viewModels()
    /** 设置页的 ViewModel。 */
    private val settingsViewModel: SettingsViewModel by viewModels()

    /** Shizuku Binder 连接建立时的回调，用于刷新授权状态。 */
    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        viewModel.refreshStatus()
    }

    /** Shizuku 授权结果回调（无论同意或拒绝），用于刷新授权状态。 */
    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { _, _ ->
        viewModel.refreshStatus()
    }

    /**
     * Activity 创建入口：启用边到边显示、注册 Shizuku 监听并挂载 Compose 内容。
     *
     * @param savedInstanceState 重建时保存的状态，可为 null
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Shizuku.addBinderReceivedListener(binderReceivedListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)

        setContent {
            val themeConfig by settingsViewModel.themeConfig.collectAsState()
            val languageConfig by settingsViewModel.languageConfig.collectAsState()

            LaunchedEffect(languageConfig) {
                val localeTag = when (languageConfig) {
                    LanguageConfig.FOLLOW_SYSTEM -> ""
                    LanguageConfig.CHINESE -> "zh"
                    LanguageConfig.ENGLISH -> "en"
                }
                val appLocales = LocaleListCompat.forLanguageTags(localeTag)
                if (AppCompatDelegate.getApplicationLocales() != appLocales) {
                    AppCompatDelegate.setApplicationLocales(appLocales)
                }
            }

            val darkTheme = when (themeConfig) {
                ThemeConfig.FOLLOW_SYSTEM -> isSystemInDarkTheme()
                ThemeConfig.LIGHT -> false
                ThemeConfig.DARK -> true
            }

            UpdateJunkieTheme(darkTheme = darkTheme) {
                var currentScreen by remember { mutableStateOf(Screen.Main) }

                BackHandler(enabled = currentScreen != Screen.Main) {
                    currentScreen = Screen.Main
                }

                when (currentScreen) {
                    Screen.Main -> MainScreen(
                        viewModel = viewModel,
                        onSettingsClick = { currentScreen = Screen.Settings }
                    )
                    Screen.Settings -> SettingsScreen(
                        viewModel = settingsViewModel,
                        onBack = { currentScreen = Screen.Main }
                    )
                }
            }
        }
    }

    /** Activity 销毁时注销 Shizuku 监听，避免内存泄漏。 */
    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
    }
}
