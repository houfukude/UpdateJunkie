package com.houfukude.updatejunkie.ui

import android.content.Intent
import android.net.Uri
import android.net.nsd.NsdServiceInfo
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.houfukude.updatejunkie.BuildConfig
import com.houfukude.updatejunkie.R
import com.houfukude.updatejunkie.data.LanguageConfig
import com.houfukude.updatejunkie.data.ThemeConfig
import com.houfukude.updatejunkie.ui.theme.UpdateJunkieTheme
import com.houfukude.updatejunkie.viewmodel.SettingsViewModel

/**
 * 设置页的有状态入口。
 *
 * 负责从 [SettingsViewModel] 收集主题配置，并将其转发给无状态的 [SettingsScreenContent]。
 *
 * @param viewModel 设置页的 ViewModel，负责主题配置的读写
 * @param onBack 点击返回箭头时的回调
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val themeConfig by viewModel.themeConfig.collectAsState()
    val languageConfig by viewModel.languageConfig.collectAsState()
    val lastImportUrl by viewModel.lastImportUrl.collectAsState()

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

    SettingsScreenContent(
        themeConfig = themeConfig,
        onThemeConfigChange = { viewModel.setThemeConfig(it) },
        languageConfig = languageConfig,
        onLanguageConfigChange = { viewModel.setLanguageConfig(it) },
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
        onAboutClick = {
            val intent = Intent(
                Intent.ACTION_VIEW,
                "https://github.com/houfukude/UpdateJunkie".toUri()
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        },
        onBack = onBack
    )
}

/**
 * 设置页的无状态实现，便于预览与测试。
 *
 * 提供语言、主题、关于三项入口，其中主题项可弹出单选对话框实时切换。
 *
 * @param themeConfig 当前选中的主题配置
 * @param onThemeConfigChange 主题配置变更时的回调
 * @param languageConfig 当前选中的语言配置
 * @param onLanguageConfigChange 语言配置变更时的回调
 * @param onImportFile 通过文件导入的回调
 * @param onImportUrl 通过 URL 导入的回调
 * @param onExport 导出配置的回调
 * @param discoveredDevices 局域网发现的设备列表
 * @param onStartLanDiscovery 开始局域网扫描的回调
 * @param onStopLanDiscovery 停止局域网扫描的回调
 * @param onImportFromLan 从局域网设备导入的回调
 * @param isLanServerRunning 局域网服务端运行状态
 * @param onToggleLanServer 切换局域网服务端状态的回调
 * @param onAboutClick 点击“关于”项时的回调
 * @param onBack 点击返回箭头时的回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    themeConfig: ThemeConfig,
    onThemeConfigChange: (ThemeConfig) -> Unit,
    languageConfig: LanguageConfig,
    onLanguageConfigChange: (LanguageConfig) -> Unit,
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
    onAboutClick: () -> Unit,
    onBack: () -> Unit
) {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.language)) },
                supportingContent = {
                    val text = when (languageConfig) {
                        LanguageConfig.FOLLOW_SYSTEM -> stringResource(R.string.follow_system)
                        LanguageConfig.CHINESE -> stringResource(R.string.chinese_simplified)
                        LanguageConfig.ENGLISH -> stringResource(R.string.english)
                    }
                    Text(text)
                },
                leadingContent = { Icon(Icons.Default.Language, contentDescription = null) },
                modifier = Modifier.clickable { showLanguageDialog = true }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.theme)) },
                supportingContent = {
                    val text = when (themeConfig) {
                        ThemeConfig.FOLLOW_SYSTEM -> stringResource(R.string.theme_follow_system)
                        ThemeConfig.LIGHT -> stringResource(R.string.theme_light)
                        ThemeConfig.DARK -> stringResource(R.string.theme_dark)
                    }
                    Text(text)
                },
                leadingContent = { Icon(Icons.Default.ColorLens, contentDescription = null) },
                modifier = Modifier.clickable { showThemeDialog = true }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.import_config)) },
                leadingContent = { Icon(Icons.Default.FileUpload, contentDescription = null) },
                modifier = Modifier.clickable { showImportDialog = true }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.view_config)) },
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                modifier = Modifier.clickable { showViewConfigDialog = true }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.export_config)) },
                leadingContent = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                modifier = Modifier.clickable { showExportDialog = true }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.build_time)) },
                supportingContent = { Text(BuildConfig.BUILD_TIME) },
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.about)) },
                supportingContent = {
                    Text(
                        stringResource(
                            R.string.version,
                            BuildConfig.VERSION_NAME
                        )
                    )
                },
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                modifier = Modifier.clickable { onAboutClick() }
            )
        }
    }

    if (showViewConfigDialog) {
        AlertDialog(
            onDismissRequest = { showViewConfigDialog = false },
            title = { Text(stringResource(R.string.view_config)) },
            text = {
                val scrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text = onViewConfig(),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showViewConfigDialog = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.select_theme)) },
            text = {
                Column {
                    ThemeOption(stringResource(R.string.theme_follow_system), themeConfig == ThemeConfig.FOLLOW_SYSTEM) {
                        onThemeConfigChange(ThemeConfig.FOLLOW_SYSTEM)
                        showThemeDialog = false
                    }
                    ThemeOption(stringResource(R.string.theme_light), themeConfig == ThemeConfig.LIGHT) {
                        onThemeConfigChange(ThemeConfig.LIGHT)
                        showThemeDialog = false
                    }
                    ThemeOption(stringResource(R.string.theme_dark), themeConfig == ThemeConfig.DARK) {
                        onThemeConfigChange(ThemeConfig.DARK)
                        showThemeDialog = false
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.select_language)) },
            text = {
                Column {
                    ThemeOption(stringResource(R.string.follow_system), languageConfig == LanguageConfig.FOLLOW_SYSTEM) {
                        onLanguageConfigChange(LanguageConfig.FOLLOW_SYSTEM)
                        showLanguageDialog = false
                    }
                    ThemeOption(stringResource(R.string.chinese_simplified), languageConfig == LanguageConfig.CHINESE) {
                        onLanguageConfigChange(LanguageConfig.CHINESE)
                        showLanguageDialog = false
                    }
                    ThemeOption(stringResource(R.string.english), languageConfig == LanguageConfig.ENGLISH) {
                        onLanguageConfigChange(LanguageConfig.ENGLISH)
                        showLanguageDialog = false
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(stringResource(R.string.import_dialog_title)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            showImportDialog = false
                            showUrlImportDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.import_via_url))
                    }
                    FilledTonalButton(
                        onClick = {
                            showImportDialog = false
                            filePickerLauncher.launch("application/json")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.import_via_file))
                    }
                    FilledTonalButton(
                        onClick = {
                            showImportDialog = false
                            onStartLanDiscovery()
                            showLanDiscoveryDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.import_via_lan))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
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
                TextButton(onClick = {
                    onImportUrl(url)
                    showUrlImportDialog = false
                }) {
                    Text(stringResource(R.string.btn_import))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlImportDialog = false }) {
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
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            showExportDialog = false
                            fileSaverLauncher.launch(exportFileName)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.export_via_file))
                    }
                    FilledTonalButton(
                        onClick = {
                            onToggleLanServer()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        colors = if (isLanServerRunning) {
                            ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            ButtonDefaults.filledTonalButtonColors()
                        }
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(stringResource(R.string.export_via_lan))
                            Text(
                                stringResource(
                                    R.string.lan_service_status,
                                    if (isLanServerRunning) stringResource(R.string.lan_service_on)
                                    else stringResource(R.string.lan_service_off)
                                ),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
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
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (discoveredDevices.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    stringResource(R.string.searching_lan),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    } else {
                        discoveredDevices.forEach { device ->
                            @Suppress("DEPRECATION")
                            val hostAddress = device.host?.hostAddress
                            ListItem(
                                headlineContent = { Text(device.serviceName) },
                                supportingContent = { Text("${hostAddress ?: "Unknown"}:${device.port}") },
                                leadingContent = {
                                    Icon(
                                        Icons.Default.Language,
                                        contentDescription = null
                                    )
                                },
                                modifier = Modifier.clickable {
                                    onImportFromLan(device)
                                    showLanDiscoveryDialog = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onStopLanDiscovery()
                    showLanDiscoveryDialog = false
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}


/**
 * 主题选择对话框中的单个单选项。
 *
 * @param text 选项文案
 * @param selected 是否为当前选中项
 * @param onClick 选中该选项时的回调
 */
@Composable
private fun ThemeOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////

// preview

////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * 设置页预览。
 */
@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    UpdateJunkieTheme {
        SettingsScreenContent(
            themeConfig = ThemeConfig.FOLLOW_SYSTEM,
            onThemeConfigChange = {},
            languageConfig = LanguageConfig.FOLLOW_SYSTEM,
            onLanguageConfigChange = {},
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
            onAboutClick = {},
            onBack = {}
        )
    }
}
