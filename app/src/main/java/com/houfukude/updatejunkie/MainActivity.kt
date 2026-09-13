package com.houfukude.updatejunkie

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.os.LocaleListCompat
import com.houfukude.updatejunkie.data.LanguageConfig
import com.houfukude.updatejunkie.data.ThemeConfig
import com.houfukude.updatejunkie.shizuku.ShizukuManager
import com.houfukude.updatejunkie.ui.MainScreen
import com.houfukude.updatejunkie.ui.Screen
import com.houfukude.updatejunkie.ui.SettingsScreen
import com.houfukude.updatejunkie.ui.theme.UpdateJunkieTheme
import com.houfukude.updatejunkie.viewmodel.AppListViewModel
import com.houfukude.updatejunkie.viewmodel.SettingsViewModel
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
        settingsViewModel.refreshStatus()
    }

    /** Shizuku 授权结果回调（无论同意或拒绝），用于刷新授权状态。 */
    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { _, _ ->
        settingsViewModel.refreshStatus()
    }

    /**
     * Activity 创建入口：启用边到边显示、注册 Shizuku 监听并挂载 Compose 内容。
     *
     * @param savedInstanceState 重建时保存的状态，可为 null
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 初始化 Shizuku 管理器并注册监听器
        ShizukuManager.init()
        try {
            Shizuku.addBinderReceivedListener(binderReceivedListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        } catch (e: Throwable) {
            Log.e("MainActivity", "Failed to add Shizuku listeners", e)
        }

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
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        } catch (e: Throwable) {
            // 忽略销毁时的 Binder 异常，防止由于 Binder 已死亡导致的二次崩溃
            Log.w("MainActivity", "Error removing Shizuku listeners", e)
        }
    }
}
