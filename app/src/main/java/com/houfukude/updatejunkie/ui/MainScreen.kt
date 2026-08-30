package com.houfukude.updatejunkie.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.houfukude.updatejunkie.model.AppInfo
import com.houfukude.updatejunkie.ui.components.AppList
import com.houfukude.updatejunkie.ui.components.ShizukuStatusCard
import com.houfukude.updatejunkie.ui.theme.UpdateJunkieTheme
import com.houfukude.updatejunkie.viewmodel.AppListUiState
import com.houfukude.updatejunkie.viewmodel.AppListViewModel

enum class Screen {
    Main,
    Settings
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: AppListViewModel = viewModel(),
    onSettingsClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isShizukuAvailable by viewModel.isShizukuAvailable.collectAsState()
    val hasShizukuPermission by viewModel.hasShizukuPermission.collectAsState()

    val availableInstallers by viewModel.availableInstallers.collectAsState()
    val selectedInstallers by viewModel.selectedInstallers.collectAsState()
    val showSystem by viewModel.showSystem.collectAsState()
    val showDisabled by viewModel.showDisabled.collectAsState()

    var showFilterMenu by remember { mutableStateOf(false) }

    MainScreenContent(
        uiState = uiState,
        isShizukuAvailable = isShizukuAvailable,
        hasShizukuPermission = hasShizukuPermission,
        availableInstallers = availableInstallers,
        selectedInstallers = selectedInstallers,
        showSystem = showSystem,
        showDisabled = showDisabled,
        showFilterMenu = showFilterMenu,
        onToggleFilterMenu = { showFilterMenu = !showFilterMenu },
        onToggleInstaller = { viewModel.toggleInstallerFilter(it) },
        onToggleSystem = { viewModel.toggleSystemFilter() },
        onToggleDisabled = { viewModel.toggleDisabledFilter() },
        onRefresh = { viewModel.loadApps() },
        onSettingsClick = onSettingsClick,
        onRequestShizukuPermission = { viewModel.requestShizukuPermission() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    uiState: AppListUiState,
    isShizukuAvailable: Boolean,
    hasShizukuPermission: Boolean,
    availableInstallers: List<String?>,
    selectedInstallers: Set<String?>,
    showSystem: Boolean,
    showDisabled: Boolean,
    showFilterMenu: Boolean,
    onToggleFilterMenu: () -> Unit,
    onToggleInstaller: (String?) -> Unit,
    onToggleSystem: () -> Unit,
    onToggleDisabled: () -> Unit,
    onRefresh: () -> Unit,
    onSettingsClick: () -> Unit,
    onRequestShizukuPermission: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("更新控") },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                    Box {
                        IconButton(onClick = onToggleFilterMenu) {
                            Icon(Icons.Default.FilterList, contentDescription = "筛选")
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = onToggleFilterMenu
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = showSystem, onCheckedChange = null)
                                        Text("显示系统应用")
                                    }
                                },
                                onClick = onToggleSystem
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = showDisabled, onCheckedChange = null)
                                        Text("显示已禁用应用")
                                    }
                                },
                                onClick = onToggleDisabled
                            )
                            HorizontalDivider()
                            availableInstallers.forEach { label ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = selectedInstallers.contains(label),
                                                onCheckedChange = null
                                            )
                                            Text(label ?: "未知")
                                        }
                                    },
                                    onClick = { onToggleInstaller(label) }
                                )
                            }
                        }
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            ShizukuStatusCard(
                isAvailable = isShizukuAvailable,
                hasPermission = hasShizukuPermission,
                onRequestPermission = onRequestShizukuPermission
            )

            when (val state = uiState) {
                is AppListUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is AppListUiState.Success -> {
                    AppList(apps = state.apps)
                }
                is AppListUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}




////////////////////////////////////////////////////////////////////////////////////////////////////

// preview

////////////////////////////////////////////////////////////////////////////////////////////////////

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    UpdateJunkieTheme {
        MainScreenContent(
            uiState = AppListUiState.Success(
                apps = listOf(
                    AppInfo(
                        packageName = "com.example.app1",
                        label = "Google Play App",
                        icon = null,
                        installerPackageName = "com.android.vending",
                        installerLabel = "Google Play Store",
                        isSystemApp = false,
                        isEnabled = true,
                        userId = 0,
                        isAdbInstalled = false
                    ),
                    AppInfo(
                        packageName = "com.example.app2",
                        label = "System App",
                        icon = null,
                        installerPackageName = null,
                        installerLabel = null,
                        isSystemApp = true,
                        isEnabled = true,
                        userId = 0,
                        isAdbInstalled = false
                    ),
                    AppInfo(
                        packageName = "com.example.app3",
                        label = "Disabled App",
                        icon = null,
                        installerPackageName = "com.coolapk.market",
                        installerLabel = "Coolapk",
                        isSystemApp = false,
                        isEnabled = false,
                        userId = 0,
                        isAdbInstalled = false
                    )
                )
            ),
            isShizukuAvailable = true,
            hasShizukuPermission = false,
            availableInstallers = listOf("Google Play Store", "Coolapk"),
            selectedInstallers = emptySet(),
            showSystem = true,
            showDisabled = true,
            showFilterMenu = false,
            onToggleFilterMenu = {},
            onToggleInstaller = {},
            onToggleSystem = {},
            onToggleDisabled = {},
            onRefresh = {},
            onSettingsClick = {},
            onRequestShizukuPermission = {}
        )
    }
}







@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun FilterMenuPreview() {
    UpdateJunkieTheme {
        MainScreenContent(
            uiState = AppListUiState.Success(emptyList()),
            isShizukuAvailable = true,
            hasShizukuPermission = true,
            availableInstallers = listOf("Google Play Store", "Coolapk", "ADB 安装"),
            selectedInstallers = setOf("Coolapk"),
            showSystem = true,
            showDisabled = false,
            showFilterMenu = true,
            onToggleFilterMenu = {},
            onToggleInstaller = {},
            onToggleSystem = {},
            onToggleDisabled = {},
            onRefresh = {},
            onSettingsClick = {},
            onRequestShizukuPermission = {}
        )
    }
}