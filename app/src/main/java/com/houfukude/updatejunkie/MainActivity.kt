package com.houfukude.updatejunkie

import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import com.houfukude.updatejunkie.data.ThemeConfig
import com.houfukude.updatejunkie.viewmodel.AppListViewModel
import com.houfukude.updatejunkie.viewmodel.SettingsViewModel
import com.houfukude.updatejunkie.ui.MainScreen
import com.houfukude.updatejunkie.ui.Screen
import com.houfukude.updatejunkie.ui.SettingsScreen
import com.houfukude.updatejunkie.ui.theme.UpdateJunkieTheme
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {

    private val viewModel: AppListViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        viewModel.refreshStatus()
    }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { _, _ ->
        viewModel.refreshStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Shizuku.addBinderReceivedListener(binderReceivedListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)

        setContent {
            val themeConfig by settingsViewModel.themeConfig.collectAsState()
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

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
    }
}
