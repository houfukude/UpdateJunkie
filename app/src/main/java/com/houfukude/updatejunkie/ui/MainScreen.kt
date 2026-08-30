package com.houfukude.updatejunkie.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val isShizukuInstalled by viewModel.isShizukuInstalled.collectAsState()
    val isShizukuAvailable by viewModel.isShizukuAvailable.collectAsState()
    val hasShizukuPermission by viewModel.hasShizukuPermission.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val loadProgress by viewModel.loadProgress.collectAsState()
    val loadProgressText by viewModel.loadProgressText.collectAsState()

    val availableInstallers by viewModel.availableInstallers.collectAsState()
    val selectedInstallers by viewModel.selectedInstallers.collectAsState()
    val showSystem by viewModel.showSystem.collectAsState()
    val showDisabled by viewModel.showDisabled.collectAsState()

    var showFilterMenu by remember { mutableStateOf(false) }

    MainScreenContent(
        uiState = uiState,
        isShizukuInstalled = isShizukuInstalled,
        isShizukuAvailable = isShizukuAvailable,
        hasShizukuPermission = hasShizukuPermission,
        isRefreshing = isRefreshing,
        loadProgress = loadProgress,
        loadProgressText = loadProgressText,
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
        onRequestShizukuPermission = { viewModel.requestShizukuPermission() },
        onDownloadShizuku = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/"))
            context.startActivity(intent)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    uiState: AppListUiState,
    isShizukuInstalled: Boolean,
    isShizukuAvailable: Boolean,
    hasShizukuPermission: Boolean,
    isRefreshing: Boolean,
    loadProgress: Float,
    loadProgressText: String,
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
    onRequestShizukuPermission: () -> Unit,
    onDownloadShizuku: () -> Unit
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
                        IconButton(
                            onClick = onToggleFilterMenu,
                            colors = if (showFilterMenu) {
                                IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            } else {
                                IconButtonDefaults.iconButtonColors()
                            }
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "筛选")
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = onToggleFilterMenu
                        ) {
                            DropdownMenuItem(
                                text = { Text("显示系统应用") },
                                trailingIcon = { Checkbox(checked = showSystem, onCheckedChange = null) },
                                onClick = onToggleSystem
                            )
                            DropdownMenuItem(
                                text = { Text("显示已禁用应用") },
                                trailingIcon = { Checkbox(checked = showDisabled, onCheckedChange = null) },
                                onClick = onToggleDisabled
                            )
                            HorizontalDivider()
                            availableInstallers.forEach { label ->
                                DropdownMenuItem(
                                    text = { Text(label ?: "未知") },
                                    trailingIcon = {
                                        Checkbox(
                                            checked = selectedInstallers.contains(label),
                                            onCheckedChange = null
                                        )
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
        val header = @Composable {
            Column {
                ShizukuStatusCard(
                    isInstalled = isShizukuInstalled,
                    isAvailable = isShizukuAvailable,
                    hasPermission = hasShizukuPermission,
                    onRequestPermission = onRequestShizukuPermission,
                    onDownloadClick = onDownloadShizuku
                )

                if (isRefreshing) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "正在扫描应用...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = loadProgressText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        LinearProgressIndicator(
                            progress = { loadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            strokeCap = StrokeCap.Round
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        Box(modifier = Modifier.padding(innerPadding)) {
            when (val state = uiState) {
                is AppListUiState.Loading -> {
                    Column {
                        header()
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                is AppListUiState.Success -> {
                    AppList(
                        apps = state.apps,
                        headerContent = header
                    )
                }

                is AppListUiState.Error -> {
                    Column {
                        header()
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Error: ${state.message}",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
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
                        versionName = "2.1.0",
                        versionCode = 210,
                        installerPackageName = "com.android.vending",
                        installerLabel = "Google Play Store",
                        isSystemApp = false,
                        isEnabled = true,
                        userId = "0",
                        isAdbInstalled = false
                    ),
                    AppInfo(
                        packageName = "com.example.app2",
                        label = "System App",
                        icon = null,
                        versionName = "14",
                        versionCode = 140,
                        installerPackageName = null,
                        installerLabel = null,
                        isSystemApp = true,
                        isEnabled = true,
                        userId = "0",
                        isAdbInstalled = false
                    ),
                    AppInfo(
                        packageName = "com.example.app3",
                        label = "Disabled App",
                        icon = null,
                        versionName = "0.9b",
                        versionCode = 9,
                        installerPackageName = "com.coolapk.market",
                        installerLabel = "Coolapk",
                        isSystemApp = false,
                        isEnabled = false,
                        userId = "0",
                        isAdbInstalled = false
                    )
                )
            ),
            isShizukuInstalled = true,
            isShizukuAvailable = true,
            hasShizukuPermission = false,
            isRefreshing = true,
            loadProgress = 0.5f,
            loadProgressText = "50 / 100",
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
            onRequestShizukuPermission = {},
            onDownloadShizuku = {}
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
            isShizukuInstalled = true,
            isShizukuAvailable = true,
            hasShizukuPermission = true,
            isRefreshing = false,
            loadProgress = 1.0f,
            loadProgressText = "100 / 100",
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
            onRequestShizukuPermission = {},
            onDownloadShizuku = {}
        )
    }
}