package com.houfukude.updatejunkie.ui.tv

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.net.nsd.NsdServiceInfo
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.RadioButton
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.houfukude.updatejunkie.BuildConfig
import com.houfukude.updatejunkie.R
import com.houfukude.updatejunkie.data.LanguageConfig
import com.houfukude.updatejunkie.data.ThemeConfig
import com.houfukude.updatejunkie.ui.components.ShizukuStatusCard
import com.houfukude.updatejunkie.ui.theme.UpdateJunkieTvTheme
import com.houfukude.updatejunkie.viewmodel.SettingsViewModel

/**
 * 专为 Android TV 优化的设置页面。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val themeConfig by viewModel.themeConfig.collectAsState()
    val languageConfig by viewModel.languageConfig.collectAsState()
    val lastImportUrl by viewModel.lastImportUrl.collectAsState()
    val changelogState by viewModel.changelogState.collectAsState()

    val isShizukuInstalled by viewModel.isShizukuInstalled.collectAsState()
    val isShizukuAvailable by viewModel.isShizukuAvailable.collectAsState()
    val hasShizukuPermission by viewModel.hasShizukuPermission.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is SettingsViewModel.SettingsEvent.ShowToast -> {
                    Toast.makeText(context, event.messageRes, Toast.LENGTH_SHORT).show()
                }

                is SettingsViewModel.SettingsEvent.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    TvSettingsScreenContent(
        themeConfig = themeConfig,
        onThemeConfigChange = { viewModel.setThemeConfig(it) },
        languageConfig = languageConfig,
        onLanguageConfigChange = { viewModel.setLanguageConfig(it) },
        isShizukuInstalled = isShizukuInstalled,
        isShizukuAvailable = isShizukuAvailable,
        hasShizukuPermission = hasShizukuPermission,
        onRequestShizukuPermission = { viewModel.requestShizukuPermission() },
        onDownloadShizuku = {
            val intent = Intent(Intent.ACTION_VIEW, "https://shizuku.rikka.app/download/".toUri())
            context.startActivity(intent)
        },
        lastImportUrl = lastImportUrl,
        onImportFile = { viewModel.importConfigFromFile(it) },
        onImportUrl = { viewModel.importConfigFromUrl(it) },
        onExport = { viewModel.exportConfig(it) },
        onViewConfig = { viewModel.getConfigJson() },
        discoveredDevices = viewModel.discoveredDevices.collectAsState().value,
        onStartLanDiscovery = { viewModel.startLanDiscovery() },
        onStopLanDiscovery = { viewModel.stopLanDiscovery() },
        onImportFromLan = { viewModel.importFromLanDevice(it) },
        isLanServerRunning = viewModel.isLanServerRunning.collectAsState().value,
        onToggleLanServer = { viewModel.toggleLanServer() },
        changelogState = changelogState,
        onFetchChangelog = { viewModel.fetchChangelog() },
        onDismissChangelog = { viewModel.dismissChangelog() },
        onAboutClick = {
            val intent = Intent(
                Intent.ACTION_VIEW,
                "https://github.com/houfukude/UpdateJunkie".toUri()
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        },
        onRefreshShizukuStatus = { viewModel.refreshStatus() },
        onBack = onBack
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvSettingsScreenContent(
    themeConfig: ThemeConfig,
    onThemeConfigChange: (ThemeConfig) -> Unit,
    languageConfig: LanguageConfig,
    onLanguageConfigChange: (LanguageConfig) -> Unit,
    isShizukuInstalled: Boolean,
    isShizukuAvailable: Boolean,
    hasShizukuPermission: Boolean,
    onRequestShizukuPermission: () -> Unit,
    onDownloadShizuku: () -> Unit,
    lastImportUrl: String,
    onImportFile: (Uri) -> Unit,
    onImportUrl: (String) -> Unit,
    onExport: (Uri) -> Unit,
    onViewConfig: () -> String,
    discoveredDevices: List<NsdServiceInfo>,
    onStartLanDiscovery: () -> Unit,
    onStopLanDiscovery: () -> Unit,
    onImportFromLan: (NsdServiceInfo) -> Unit,
    isLanServerRunning: Boolean,
    onToggleLanServer: () -> Unit,
    changelogState: SettingsViewModel.ChangelogState,
    onRefreshShizukuStatus: () -> Unit, // 添加该参数用于预览手动刷新
    onFetchChangelog: () -> Unit,
    onDismissChangelog: () -> Unit,
    onAboutClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    /**
     * TV 端的本地化字符串获取函数。
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

    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showUrlImportDialog by remember { mutableStateOf(false) }
    var showLanDiscoveryDialog by remember { mutableStateOf(false) }
    var showViewConfigDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onImportFile(it) }
    }

    val fileSaverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { onExport(it) }
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
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tvStringResource(R.string.settings),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Shizuku 状态卡片
            Box(modifier = Modifier.padding(bottom = 16.dp)) {
                ShizukuStatusCard(
                    isInstalled = isShizukuInstalled,
                    isAvailable = isShizukuAvailable,
                    hasPermission = hasShizukuPermission,
                    onRequestPermission = onRequestShizukuPermission,
                    onDownloadClick = onDownloadShizuku
                )
            }

            TvSettingsItem(
                title = tvStringResource(R.string.language),
                value = when (languageConfig) {
                    LanguageConfig.FOLLOW_SYSTEM -> tvStringResource(R.string.follow_system)
                    LanguageConfig.CHINESE -> tvStringResource(R.string.chinese_simplified)
                    LanguageConfig.ENGLISH -> tvStringResource(R.string.english)
                },
                icon = Icons.Default.Language,
                onClick = { showLanguageDialog = true }
            )

            TvSettingsItem(
                title = tvStringResource(R.string.theme),
                value = when (themeConfig) {
                    ThemeConfig.FOLLOW_SYSTEM -> tvStringResource(R.string.theme_follow_system)
                    ThemeConfig.LIGHT -> tvStringResource(R.string.theme_light)
                    ThemeConfig.DARK -> tvStringResource(R.string.theme_dark)
                },
                icon = Icons.Default.ColorLens,
                onClick = { showThemeDialog = true }
            )

            TvSettingsItem(
                title = tvStringResource(R.string.import_config),
                value = tvStringResource(R.string.tv_import_summary),
                icon = Icons.Default.FileUpload,
                onClick = { showImportDialog = true }
            )

            TvSettingsItem(
                title = tvStringResource(R.string.view_config),
                value = tvStringResource(R.string.tv_view_config_summary),
                icon = Icons.Default.Info,
                onClick = { showViewConfigDialog = true }
            )

            TvSettingsItem(
                title = tvStringResource(R.string.export_config),
                value = tvStringResource(R.string.tv_export_summary),
                icon = Icons.Default.FileDownload,
                onClick = { showExportDialog = true }
            )

            TvSettingsItem(
                title = tvStringResource(R.string.tv_changelog),
                value = BuildConfig.BUILD_TIME,
                icon = Icons.Default.History,
                onClick = onFetchChangelog
            )

            TvSettingsItem(
                title = tvStringResource(R.string.about),
                value = tvStringResource(R.string.version, BuildConfig.VERSION_NAME),
                icon = Icons.Default.Info,
                onClick = onAboutClick
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
                Text(tvStringResource(R.string.tv_go_back))
            }
        }
    }

    if (showThemeDialog) {
        TvOptionDialog(
            title = stringResource(R.string.select_theme),
            options = listOf(
                ThemeConfig.FOLLOW_SYSTEM to stringResource(R.string.theme_follow_system),
                ThemeConfig.LIGHT to stringResource(R.string.theme_light),
                ThemeConfig.DARK to stringResource(R.string.theme_dark)
            ),
            currentValue = themeConfig,
            onSelect = {
                onThemeConfigChange(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showLanguageDialog) {
        TvOptionDialog(
            title = stringResource(R.string.select_language),
            options = listOf(
                LanguageConfig.FOLLOW_SYSTEM to stringResource(R.string.follow_system),
                LanguageConfig.CHINESE to stringResource(R.string.chinese_simplified),
                LanguageConfig.ENGLISH to stringResource(R.string.english)
            ),
            currentValue = languageConfig,
            onSelect = {
                onLanguageConfigChange(it)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(stringResource(R.string.import_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TvDialogButton(
                        text = stringResource(R.string.import_via_url),
                        icon = Icons.Default.CloudDownload,
                        onClick = {
                            showImportDialog = false
                            showUrlImportDialog = true
                        }
                    )
                    TvDialogButton(
                        text = stringResource(R.string.import_via_file),
                        icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                        onClick = {
                            showImportDialog = false
                            filePickerLauncher.launch("application/json")
                        }
                    )
                    TvDialogButton(
                        text = stringResource(R.string.import_via_lan),
                        icon = Icons.Default.Language,
                        onClick = {
                            showImportDialog = false
                            onStartLanDiscovery()
                            showLanDiscoveryDialog = true
                        }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showImportDialog = false
                }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showUrlImportDialog) {
        var url by remember { mutableStateOf(lastImportUrl) }
        AlertDialog(
            onDismissRequest = { showUrlImportDialog = false },
            title = { Text(stringResource(R.string.import_via_url)) },
            text = {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.url_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onImportUrl(url)
                    showUrlImportDialog = false
                }) {
                    Text(stringResource(R.string.btn_import))
                }
            },
            dismissButton = {
                Button(onClick = { showUrlImportDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showExportDialog) {
        val context = LocalContext.current
        val exportFileName = "${context.packageName}_${BuildConfig.VERSION_NAME}_config.json"
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(stringResource(R.string.export_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TvDialogButton(
                        text = stringResource(R.string.export_via_file),
                        icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                        onClick = {
                            showExportDialog = false
                            fileSaverLauncher.launch(exportFileName)
                        }
                    )
                    TvDialogButton(
                        text = if (isLanServerRunning) stringResource(R.string.tv_lan_on) else stringResource(
                            R.string.tv_lan_off
                        ),
                        icon = Icons.Default.Language,
                        onClick = { onToggleLanServer() }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showExportDialog = false
                }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showLanDiscoveryDialog) {
        AlertDialog(
            onDismissRequest = {
                onStopLanDiscovery()
                showLanDiscoveryDialog = false
            },
            title = { Text(stringResource(R.string.select_device)) },
            text = {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())) {
                    if (discoveredDevices.isEmpty()) {
                        Text(
                            stringResource(R.string.searching_lan),
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        discoveredDevices.forEach { device ->
                            @Suppress("DEPRECATION")
                            val hostAddress = device.host?.hostAddress
                            TvSettingsItem(
                                title = device.serviceName,
                                value = "${hostAddress ?: "Unknown"}:${device.port}",
                                icon = Icons.Default.Language,
                                onClick = {
                                    onImportFromLan(device)
                                    showLanDiscoveryDialog = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onStopLanDiscovery()
                    showLanDiscoveryDialog = false
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showViewConfigDialog) {
        AlertDialog(
            onDismissRequest = { showViewConfigDialog = false },
            title = { Text(stringResource(R.string.view_config)) },
            text = {
                Box(
                    modifier = Modifier
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(text = onViewConfig(), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = {
                    showViewConfigDialog = false
                }) { Text(stringResource(android.R.string.ok)) }
            }
        )
    }

    // 更新日志对话框
    when (val state = changelogState) {
        is SettingsViewModel.ChangelogState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.tv_loading))
            }
        }

        is SettingsViewModel.ChangelogState.Success -> {
            AlertDialog(
                onDismissRequest = onDismissChangelog,
                title = { Text(stringResource(R.string.version, state.version)) },
                text = {
                    Box(
                        modifier = Modifier
                            .heightIn(max = 300.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(state.content)
                    }
                },
                confirmButton = {
                    Button(onClick = onDismissChangelog) { Text(stringResource(android.R.string.ok)) }
                }
            )
        }

        is SettingsViewModel.ChangelogState.Error -> {
            AlertDialog(
                onDismissRequest = onDismissChangelog,
                title = { Text(stringResource(R.string.error_prefix, "")) },
                text = { Text(state.message) },
                confirmButton = {
                    Button(onClick = onDismissChangelog) { Text(stringResource(android.R.string.ok)) }
                }
            )
        }

        else -> {}
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvSettingsItem(
    title: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = colorResource(R.color.tv_focus_bg),
            focusedContentColor = colorResource(R.color.tv_focus_text)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.small)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.labelLarge)
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvDialogButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        colors = ClickableSurfaceDefaults.colors(
            focusedContainerColor = colorResource(R.color.tv_focus_bg),
            focusedContentColor = colorResource(R.color.tv_focus_text)
        ),
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.small)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(text)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun <T> TvOptionDialog(
    title: String,
    options: List<Pair<T, String>>,
    currentValue: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { (value, label) ->
                    Surface(
                        onClick = { onSelect(value) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
                        colors = ClickableSurfaceDefaults.colors(
                            focusedContainerColor = colorResource(R.color.tv_focus_bg),
                            focusedContentColor = colorResource(R.color.tv_focus_text)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = value == currentValue, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Text(label)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

////////////////////////////////////////////////////////////////////////////////////////////////////
// Previews
////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * TV 端设置页面全局预览。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Preview(name = "TV Settings Screen Focused Preview", device = "id:tv_1080p", showBackground = true)
@Composable
fun TvSettingsScreenFocusedPreview() {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    UpdateJunkieTvTheme {
        TvSettingsScreenContent(
            themeConfig = ThemeConfig.FOLLOW_SYSTEM,
            onThemeConfigChange = {},
            languageConfig = LanguageConfig.FOLLOW_SYSTEM,
            onLanguageConfigChange = {},
            isShizukuInstalled = true,
            isShizukuAvailable = true,
            hasShizukuPermission = true,
            onRequestShizukuPermission = {},
            onDownloadShizuku = {},
            lastImportUrl = "https://example.com/config.json",
            onImportFile = {},
            onImportUrl = {},
            onExport = {},
            onViewConfig = { "{}" },
            discoveredDevices = emptyList(),
            onStartLanDiscovery = {},
            onStopLanDiscovery = {},
            onImportFromLan = {},
            isLanServerRunning = false,
            onToggleLanServer = {},
            changelogState = SettingsViewModel.ChangelogState.Idle,
            onRefreshShizukuStatus = {},
            onFetchChangelog = {},
            onDismissChangelog = {},
            onAboutClick = {},
            onBack = {},
            modifier = Modifier.focusRequester(focusRequester)
        )
    }
}
