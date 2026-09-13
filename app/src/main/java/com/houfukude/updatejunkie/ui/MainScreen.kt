package com.houfukude.updatejunkie.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.houfukude.updatejunkie.R
import com.houfukude.updatejunkie.model.AppInfo
import com.houfukude.updatejunkie.ui.components.AppList
import com.houfukude.updatejunkie.ui.theme.UpdateJunkieTheme
import com.houfukude.updatejunkie.viewmodel.AppListUiState
import com.houfukude.updatejunkie.viewmodel.AppListViewModel

/**
 * 应用内可切换的页面。
 *
 * @property Main 应用列表面
 * @property Settings 设置页
 */
enum class Screen {
    Main,
    Settings
}

/**
 * 应用列表页的有状态入口。
 *
 * 负责收集 [AppListViewModel] 暴露的各项状态、持有筛选菜单的展开状态，
 * 并将所有交互事件转发给 ViewModel，最终委托给无状态的 [MainScreenContent] 渲染。
 *
 * @param viewModel 应用列表页的 ViewModel
 * @param onSettingsClick 点击工具栏设置图标时的回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: AppListViewModel = viewModel(),
    onSettingsClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val loadProgress by viewModel.loadProgress.collectAsState()
    val loadProgressText by viewModel.loadProgressText.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val availableInstallers by viewModel.availableInstallers.collectAsState()
    val selectedInstallers by viewModel.selectedInstallers.collectAsState()
    val showSystem by viewModel.showSystem.collectAsState()
    val showDisabled by viewModel.showDisabled.collectAsState()
    val showConfiguredOnly by viewModel.showConfiguredOnly.collectAsState()

    var showFilterMenu by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }

    MainScreenContent(
        uiState = uiState,
        isRefreshing = isRefreshing,
        loadProgress = loadProgress,
        loadProgressText = loadProgressText,
        searchQuery = searchQuery,
        onSearchQueryChange = { viewModel.setSearchQuery(it) },
        isSearchActive = isSearchActive,
        onToggleSearch = {
            isSearchActive = !isSearchActive
            if (!isSearchActive) viewModel.setSearchQuery("")
        },
        availableInstallers = availableInstallers,
        selectedInstallers = selectedInstallers,
        showSystem = showSystem,
        showDisabled = showDisabled,
        showConfiguredOnly = showConfiguredOnly,
        showFilterMenu = showFilterMenu,
        onToggleFilterMenu = { showFilterMenu = !showFilterMenu },
        onToggleInstaller = { viewModel.toggleInstallerFilter(it) },
        onToggleSystem = { viewModel.toggleSystemFilter() },
        onToggleDisabled = { viewModel.toggleDisabledFilter() },
        onToggleConfiguredOnly = { viewModel.toggleConfiguredOnlyFilter() },
        onRefresh = { viewModel.loadApps() },
        onSettingsClick = onSettingsClick,
        onGetUpdateUrl = { viewModel.getUpdateUrl(it) },
        onSetUpdateUrl = { pkg, url -> viewModel.setUpdateUrl(pkg, url) },
        onClearFilters = { viewModel.clearAllFilters() }
    )
}

/**
 * 应用列表页的无状态实现，便于预览与测试。
 *
 * 顶部工具栏提供刷新、筛选（系统应用 / 已禁用 / 安装来源）与设置入口；
 * 内容区根据 [uiState] 分别展示加载中、应用列表或错误提示。
 * 顶部固定的头部区域包含加载进度条。
 *
 * @param uiState 列表的加载状态
 * @param isRefreshing 是否正在加载应用
 * @param loadProgress 加载进度，取值 0f ~ 1f
 * @param loadProgressText 加载进度文案
 * @param availableInstallers 可选的安装来源标签列表
 * @param selectedInstallers 已勾选的安装来源标签集合
 * @param showSystem 是否显示系统应用
 * @param showDisabled 是否显示已禁用应用
 * @param showFilterMenu 筛选下拉菜单是否展开
 * @param onToggleFilterMenu 展开 / 收起筛选菜单
 * @param onToggleInstaller 勾选或取消某个安装来源
 * @param onToggleSystem 切换"显示系统应用"
 * @param onToggleDisabled 切换"显示已禁用应用"
 * @param onRefresh 触发重新加载应用列表
 * @param onSettingsClick 跳转设置页
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    uiState: AppListUiState,
    isRefreshing: Boolean,
    loadProgress: Float,
    loadProgressText: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isSearchActive: Boolean,
    onToggleSearch: () -> Unit,
    availableInstallers: List<AppListViewModel.InstallerFilterItem>,
    selectedInstallers: Set<String?>,
    showSystem: Boolean,
    showDisabled: Boolean,
    showConfiguredOnly: Boolean,
    showFilterMenu: Boolean,
    onToggleFilterMenu: () -> Unit,
    onToggleInstaller: (String?) -> Unit,
    onToggleSystem: () -> Unit,
    onToggleDisabled: () -> Unit,
    onToggleConfiguredOnly: () -> Unit,
    onRefresh: () -> Unit,
    onSettingsClick: () -> Unit,
    onGetUpdateUrl: (String) -> String?,
    onSetUpdateUrl: (String, String) -> Unit,
    onClearFilters: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (isSearchActive) {
                        IconButton(onClick = onToggleSearch) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    }
                },
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            placeholder = {
                                Text(
                                    stringResource(R.string.search_hint),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge,
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchQueryChange("") }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = stringResource(R.string.refresh)
                                        )
                                    }
                                }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            )
                        )
                    } else {
                        Text(stringResource(R.string.app_name))
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(onClick = onRefresh) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.refresh)
                            )
                        }
                        IconButton(onClick = onToggleSearch) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = stringResource(R.string.search)
                            )
                        }
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
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = stringResource(R.string.filter)
                            )
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = onToggleFilterMenu
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.show_system_apps)) },
                                trailingIcon = {
                                    Checkbox(
                                        checked = showSystem,
                                        onCheckedChange = null
                                    )
                                },
                                onClick = onToggleSystem
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.show_disabled_apps)) },
                                trailingIcon = {
                                    Checkbox(
                                        checked = showDisabled,
                                        onCheckedChange = null
                                    )
                                },
                                onClick = onToggleDisabled
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.show_configured_only)) },
                                trailingIcon = {
                                    Checkbox(
                                        checked = showConfiguredOnly,
                                        onCheckedChange = null
                                    )
                                },
                                onClick = onToggleConfiguredOnly
                            )
                            HorizontalDivider()
                            availableInstallers.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item.label) },
                                    trailingIcon = {
                                        Checkbox(
                                            checked = selectedInstallers.contains(item.key),
                                            onCheckedChange = null
                                        )
                                    },
                                    onClick = { onToggleInstaller(item.key) }
                                )
                            }
                        }
                    }
                    if (!isSearchActive) {
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = stringResource(R.string.settings)
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        val header = @Composable {
            Column {
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
                                text = stringResource(R.string.scanning_apps),
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
                    if (state.apps.isEmpty() && (isSearchActive || showSystem || showDisabled || showConfiguredOnly || selectedInstallers.isNotEmpty())) {
                        Column {
                            header()
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.no_search_results),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (showSystem || showDisabled || showConfiguredOnly || selectedInstallers.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = onClearFilters) {
                                        Text(stringResource(R.string.clear_filters))
                                    }
                                }
                            }
                        }
                    } else {
                        AppList(
                            apps = state.apps,
                            headerContent = header,
                            onGetUpdateUrl = onGetUpdateUrl,
                            onSetUpdateUrl = onSetUpdateUrl
                        )
                    }
                }

                is AppListUiState.Error -> {
                    Column {
                        header()
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.error_prefix, state.message),
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

/** 加载中（进度 50%）且 Shizuku 未授权时的列表面预览。 */
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
            isRefreshing = true,
            loadProgress = 0.5f,
            loadProgressText = "50 / 100",
            searchQuery = "",
            onSearchQueryChange = {},
            isSearchActive = false,
            onToggleSearch = {},
            availableInstallers = listOf(
                AppListViewModel.InstallerFilterItem("com.android.vending", "Google Play Store"),
                AppListViewModel.InstallerFilterItem("com.coolapk.market", "Coolapk")
            ),
            selectedInstallers = emptySet(),
            showSystem = true,
            showDisabled = true,
            showConfiguredOnly = false,
            showFilterMenu = false,
            onToggleFilterMenu = {},
            onToggleInstaller = {},
            onToggleSystem = {},
            onToggleDisabled = {},
            onToggleConfiguredOnly = {},
            onRefresh = {},
            onSettingsClick = {},
            onGetUpdateUrl = { null },
            onSetUpdateUrl = { _, _ -> },
            onClearFilters = {}
        )
    }
}







/** 筛选菜单展开且已勾选"酷安"时的列表面预览。 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun FilterMenuPreview() {
    UpdateJunkieTheme {
        MainScreenContent(
            uiState = AppListUiState.Success(emptyList()),
            isRefreshing = false,
            loadProgress = 1.0f,
            loadProgressText = "100 / 100",
            searchQuery = "",
            onSearchQueryChange = {},
            isSearchActive = false,
            onToggleSearch = {},
            availableInstallers = listOf(
                AppListViewModel.InstallerFilterItem("com.android.vending", "Google Play Store"),
                AppListViewModel.InstallerFilterItem("com.coolapk.market", "Coolapk"),
                AppListViewModel.InstallerFilterItem(AppListViewModel.ADB_INSTALLER, "ADB 安装")
            ),
            selectedInstallers = setOf("Coolapk"),
            showSystem = true,
            showDisabled = false,
            showConfiguredOnly = false,
            showFilterMenu = true,
            onToggleFilterMenu = {},
            onToggleInstaller = {},
            onToggleSystem = {},
            onToggleDisabled = {},
            onToggleConfiguredOnly = {},
            onRefresh = {},
            onSettingsClick = {},
            onGetUpdateUrl = { null },
            onSetUpdateUrl = { _, _ -> },
            onClearFilters = {}
        )
    }
}
