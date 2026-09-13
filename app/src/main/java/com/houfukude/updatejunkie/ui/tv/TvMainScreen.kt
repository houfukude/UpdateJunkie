package com.houfukude.updatejunkie.ui.tv

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.tv.material3.CheckboxDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.houfukude.updatejunkie.R
import com.houfukude.updatejunkie.model.AppInfo
import com.houfukude.updatejunkie.ui.components.TvAppItem
import com.houfukude.updatejunkie.ui.components.TvMenuButton
import com.houfukude.updatejunkie.ui.components.UpdateConfigDialog
import com.houfukude.updatejunkie.ui.theme.UpdateJunkieTvTheme
import com.houfukude.updatejunkie.utils.MarketUtils
import com.houfukude.updatejunkie.viewmodel.AppListUiState
import com.houfukude.updatejunkie.viewmodel.AppListViewModel
import androidx.compose.material3.AlertDialog as MobileAlertDialog
import androidx.compose.material3.LinearProgressIndicator as MobileLinearProgressIndicator
import androidx.compose.material3.TextButton as MobileTextButton
import androidx.tv.material3.Checkbox as TvCheckbox

/**
 * 专为 Android TV 遥控器大屏优化的主控面板界面。
 * 遵循官方规范，使用标准 LazyVerticalGrid (完美支持 D-Pad 遥控导航) 搭配 TV Material 专属交互组件。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvMainScreen(
    viewModel: AppListViewModel,
    onSettingsClick: () -> Unit,
    onGetUpdateUrl: (String) -> String? = { null },
    onSetUpdateUrl: (String, String) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val loadProgress by viewModel.loadProgress.collectAsState()
    val loadProgressText by viewModel.loadProgressText.collectAsState()

    val availableInstallers by viewModel.availableInstallers.collectAsState()
    val selectedInstallers by viewModel.selectedInstallers.collectAsState()
    val showSystem by viewModel.showSystem.collectAsState()
    val showDisabled by viewModel.showDisabled.collectAsState()
    val showConfiguredOnly by viewModel.showConfiguredOnly.collectAsState()

    TvMainScreenContent(
        uiState = uiState,
        isRefreshing = isRefreshing,
        loadProgress = loadProgress,
        loadProgressText = loadProgressText,
        availableInstallers = availableInstallers,
        selectedInstallers = selectedInstallers,
        showSystem = showSystem,
        showDisabled = showDisabled,
        showConfiguredOnly = showConfiguredOnly,
        onRefresh = { viewModel.loadApps() },
        onSettingsClick = onSettingsClick,
        onGetUpdateUrl = onGetUpdateUrl,
        onSetUpdateUrl = onSetUpdateUrl,
        onToggleInstaller = { viewModel.toggleInstallerFilter(it) },
        onToggleSystem = { viewModel.toggleSystemFilter() },
        onToggleDisabled = { viewModel.toggleDisabledFilter() },
        onToggleConfiguredOnly = { viewModel.toggleConfiguredOnlyFilter() },
        onClearFilters = { viewModel.clearAllFilters() }
    )
}

/**
 * TV 端主界面的无状态实现，便于预览与测试。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvMainScreenContent(
    uiState: AppListUiState,
    isRefreshing: Boolean,
    loadProgress: Float,
    loadProgressText: String,
    availableInstallers: List<AppListViewModel.InstallerFilterItem>,
    selectedInstallers: Set<String?>,
    showSystem: Boolean,
    showDisabled: Boolean,
    showConfiguredOnly: Boolean,
    onRefresh: () -> Unit,
    onSettingsClick: () -> Unit,
    onGetUpdateUrl: (String) -> String?,
    onSetUpdateUrl: (String, String) -> Unit,
    onToggleInstaller: (String?) -> Unit,
    onToggleSystem: () -> Unit,
    onToggleDisabled: () -> Unit,
    onToggleConfiguredOnly: () -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    /**
     * TV 端的本地化字符串获取函数。
     * 解决 stringResource 在手动切换语言后可能存在的同步延迟问题。
     */
    @Composable
    fun tvStringResource(id: Int, vararg formatArgs: Any): String {
        val currentContext = LocalContext.current
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return stringResource(id, *formatArgs)

        val currentConfig = LocalConfiguration.current
        return remember(id, locales, formatArgs, currentConfig) {
            val config = Configuration(currentConfig).apply {
                val locale = locales.get(0)
                if (locale != null) {
                    setLocale(locale)
                }
            }
            @Suppress("DEPRECATION")
            currentContext.createConfigurationContext(config).resources.getString(id, *formatArgs)
        }
    }

    var selectedAppForMenu by remember { mutableStateOf<AppInfo?>(null) }
    var appToConfigure by remember { mutableStateOf<AppInfo?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }

    if (appToConfigure != null) {
        UpdateConfigDialog(
            initialUrl = onGetUpdateUrl(appToConfigure!!.packageName) ?: "",
            onDismiss = { appToConfigure = null },
            onSave = { url ->
                onSetUpdateUrl(appToConfigure!!.packageName, url)
                appToConfigure = null
                Toast.makeText(context, R.string.update_url_saved, Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showFilterDialog) {
        TvFilterDialog(
            availableInstallers = availableInstallers,
            selectedInstallers = selectedInstallers,
            showSystem = showSystem,
            showDisabled = showDisabled,
            showConfiguredOnly = showConfiguredOnly,
            onToggleInstaller = onToggleInstaller,
            onToggleSystem = onToggleSystem,
            onToggleDisabled = onToggleDisabled,
            onToggleConfiguredOnly = onToggleConfiguredOnly,
            onClearFilters = onClearFilters,
            onDismiss = { showFilterDialog = false }
        )
    }

    if (selectedAppForMenu != null) {
        val app = selectedAppForMenu!!
        MobileAlertDialog(
            onDismissRequest = { selectedAppForMenu = null },
            title = { Text(app.label, style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TvMenuButton(
                        text = tvStringResource(R.string.menu_go_to_settings),
                        onClick = {
                            selectedAppForMenu = null
                            val intent =
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", app.packageName, null)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            context.startActivity(intent)
                        }
                    )
                    TvMenuButton(
                        text = tvStringResource(R.string.menu_go_to_market),
                        onClick = {
                            selectedAppForMenu = null
                            MarketUtils.launchMarket(
                                context,
                                app.packageName,
                                app.installerPackageName
                            )
                        }
                    )
                    TvMenuButton(
                        text = tvStringResource(R.string.menu_copy_package_name),
                        onClick = {
                            selectedAppForMenu = null
                            clipboardManager.setText(AnnotatedString(app.packageName))
                            // 在非 Composable 作用域内，我们只能使用 context 原始加载
                            // 但由于 Activity 会重建，此时 context 应该是正确的
                            Toast.makeText(
                                context,
                                R.string.package_name_copied,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                    TvMenuButton(
                        text = tvStringResource(R.string.menu_configure_update_url),
                        onClick = {
                            selectedAppForMenu = null
                            appToConfigure = app
                        }
                    )
                    TvMenuButton(
                        text = tvStringResource(R.string.menu_go_to_update_url),
                        onClick = {
                            selectedAppForMenu = null
                            val url = onGetUpdateUrl(app.packageName)
                            if (url.isNullOrBlank()) {
                                Toast.makeText(
                                    context,
                                    R.string.update_url_not_set,
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, e.localizedMessage, Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                        }
                    )
                }
            },
            confirmButton = {
                MobileTextButton(onClick = { selectedAppForMenu = null }) {
                    Text(tvStringResource(R.string.cancel))
                }
            }
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        shape = RectangleShape,
        colors = SurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 24.dp)
        ) {
            // TV 顶部控制区
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp), // 缩减底部边距给进度条留空间
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tvStringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isRefreshing) tvStringResource(R.string.scanning_apps) else tvStringResource(
                            R.string.tv_welcome_hint
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // TV 顶部操作区
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        onClick = { showFilterDialog = true },
                        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = if (showSystem || showDisabled || showConfiguredOnly || selectedInstallers.isNotEmpty())
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            focusedContainerColor = colorResource(R.color.tv_focus_bg),
                            focusedContentColor = colorResource(R.color.tv_focus_text)
                        ),
                        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.small)
                    ) {
                        Text(
                            tvStringResource(R.string.filter),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    Surface(
                        onClick = onRefresh,
                        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedContainerColor = colorResource(R.color.tv_focus_bg),
                            focusedContentColor = colorResource(R.color.tv_focus_text)
                        ),
                        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.small)
                    ) {
                        Text(
                            tvStringResource(R.string.refresh),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    Surface(
                        onClick = onSettingsClick,
                        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedContainerColor = colorResource(R.color.tv_focus_bg),
                            focusedContentColor = colorResource(R.color.tv_focus_text)
                        ),
                        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.small)
                    ) {
                        Text(
                            tvStringResource(R.string.settings),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            // 刷新进度条
            if (isRefreshing) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = loadProgressText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    MobileLinearProgressIndicator(
                        progress = { loadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        color = MaterialTheme.colorScheme.primary,
                        strokeCap = StrokeCap.Round
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 应用网格呈现区
            when (val state = uiState) {
                is AppListUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            tvStringResource(R.string.tv_loading),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                is AppListUiState.Success -> {
                    if (state.apps.isEmpty() && (showSystem || showDisabled || showConfiguredOnly || selectedInstallers.isNotEmpty())) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = tvStringResource(R.string.no_search_results),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Surface(
                                onClick = onClearFilters,
                                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    focusedContainerColor = colorResource(R.color.tv_focus_bg),
                                    focusedContentColor = colorResource(R.color.tv_focus_text)
                                ),
                                shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.medium)
                            ) {
                                Text(
                                    tvStringResource(R.string.clear_filters),
                                    modifier = Modifier.padding(
                                        horizontal = 24.dp,
                                        vertical = 12.dp
                                    )
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(300.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 48.dp)
                        ) {
                            items(state.apps, key = { it.packageName + it.userId }) { app ->
                                TvAppItem(
                                    app = app,
                                    onItemClick = {
                                        if (app.hasUpdateUrl) {
                                            val url = onGetUpdateUrl(app.packageName)
                                            if (!url.isNullOrBlank()) {
                                                try {
                                                    val intent = Intent(
                                                        Intent.ACTION_VIEW,
                                                        url.toUri()
                                                    ).apply {
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(
                                                        context,
                                                        e.localizedMessage,
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            } else {
                                                MarketUtils.launchMarket(
                                                    context,
                                                    app.packageName,
                                                    app.installerPackageName
                                                )
                                            }
                                        } else {
                                            MarketUtils.launchMarket(
                                                context,
                                                app.packageName,
                                                app.installerPackageName
                                            )
                                        }
                                    },
                                    onLongClick = {
                                        selectedAppForMenu = app
                                    }
                                )
                            }
                        }
                    }
                }

                is AppListUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = tvStringResource(R.string.error_prefix, state.message),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvFilterDialog(
    availableInstallers: List<AppListViewModel.InstallerFilterItem>,
    selectedInstallers: Set<String?>,
    showSystem: Boolean,
    showDisabled: Boolean,
    showConfiguredOnly: Boolean,
    onToggleInstaller: (String?) -> Unit,
    onToggleSystem: () -> Unit,
    onToggleDisabled: () -> Unit,
    onToggleConfiguredOnly: () -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    MobileAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.filter),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TvFilterToggleItem(
                    label = stringResource(R.string.show_system_apps),
                    checked = showSystem,
                    onCheckedChange = { onToggleSystem() }
                )
                TvFilterToggleItem(
                    label = stringResource(R.string.show_disabled_apps),
                    checked = showDisabled,
                    onCheckedChange = { onToggleDisabled() }
                )
                TvFilterToggleItem(
                    label = stringResource(R.string.show_configured_only),
                    checked = showConfiguredOnly,
                    onCheckedChange = { onToggleConfiguredOnly() }
                )

                if (availableInstallers.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.filter), // Reuse as source header
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
                    )
                    availableInstallers.forEach { item ->
                        TvFilterToggleItem(
                            label = item.label,
                            checked = selectedInstallers.contains(item.key),
                            onCheckedChange = { onToggleInstaller(item.key) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                TvMenuButton(
                    text = stringResource(R.string.clear_filters),
                    onClick = {
                        onClearFilters()
                        onDismiss()
                    }
                )
            }
        },
        confirmButton = {
            MobileTextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        }
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvFilterToggleItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            focusedContainerColor = colorResource(R.color.tv_focus_bg),
            focusedContentColor = colorResource(R.color.tv_focus_text)
        ),
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.small),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, modifier = Modifier.weight(1f))
            TvCheckbox(
                checked = checked,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////
// Previews
////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * TV 专属大屏网格系统主界面的成功态预览。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Preview(name = "TV Main Screen Success Preview", device = "id:tv_1080p", showBackground = true)
@Composable
fun TvMainScreenPreview() {
    val apps = listOf(
        AppInfo(
            "com.example.tv1",
            "电视影音",
            null,
            "2.1",
            21L,
            null,
            null,
            false,
            true,
            "0",
            false
        ),
        AppInfo("com.example.tv2", "大屏游戏", null, "1.0", 1L, null, null, false, true, "0", true),
        AppInfo(
            "com.example.tv3",
            "系统设置",
            null,
            "11.0",
            110L,
            null,
            null,
            true,
            true,
            "0",
            false
        ),
        AppInfo(
            "com.example.tv4",
            "已禁用的电视应用",
            null,
            "0.5",
            5L,
            null,
            null,
            false,
            false,
            "0",
            false
        )
    )

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    UpdateJunkieTvTheme {
        TvMainScreenContent(
            uiState = AppListUiState.Success(apps),
            isRefreshing = false,
            loadProgress = 0f,
            loadProgressText = "",
            availableInstallers = emptyList(),
            selectedInstallers = emptySet(),
            showSystem = false,
            showDisabled = false,
            showConfiguredOnly = false,
            onRefresh = {},
            onSettingsClick = {},
            onGetUpdateUrl = { null },
            onSetUpdateUrl = { _, _ -> },
            onToggleInstaller = {},
            onToggleSystem = {},
            onToggleDisabled = {},
            onToggleConfiguredOnly = {},
            onClearFilters = {},
            modifier = Modifier.focusRequester(focusRequester)
        )
    }
}

/**
 * TV 端加载中（正在刷新）状态的预览。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Preview(name = "TV Main Screen Refreshing Preview", device = "id:tv_1080p", showBackground = true)
@Composable
fun TvMainScreenRefreshingPreview() {
    UpdateJunkieTvTheme {
        TvMainScreenContent(
            uiState = AppListUiState.Success(emptyList()),
            isRefreshing = true,
            loadProgress = 0.65f,
            loadProgressText = "65 / 100",
            availableInstallers = emptyList(),
            selectedInstallers = emptySet(),
            showSystem = false,
            showDisabled = false,
            showConfiguredOnly = false,
            onRefresh = {},
            onSettingsClick = {},
            onGetUpdateUrl = { null },
            onSetUpdateUrl = { _, _ -> },
            onToggleInstaller = {},
            onToggleSystem = {},
            onToggleDisabled = {},
            onToggleConfiguredOnly = {},
            onClearFilters = {}
        )
    }
}
