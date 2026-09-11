package com.houfukude.updatejunkie.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.houfukude.updatejunkie.R
import com.houfukude.updatejunkie.data.ThemeConfig
import com.houfukude.updatejunkie.data.LanguageConfig
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
    val themeConfig by viewModel.themeConfig.collectAsState()
    val languageConfig by viewModel.languageConfig.collectAsState()
    SettingsScreenContent(
        themeConfig = themeConfig,
        onThemeConfigChange = { viewModel.setThemeConfig(it) },
        languageConfig = languageConfig,
        onLanguageConfigChange = { viewModel.setLanguageConfig(it) },
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
 * @param onBack 点击返回箭头时的回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    themeConfig: ThemeConfig,
    onThemeConfigChange: (ThemeConfig) -> Unit,
    languageConfig: LanguageConfig,
    onLanguageConfigChange: (LanguageConfig) -> Unit,
    onBack: () -> Unit
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

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
                headlineContent = { Text(stringResource(R.string.about)) },
                supportingContent = { Text(stringResource(R.string.version, "1.0.0")) },
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                modifier = Modifier.clickable { /* TODO */ }
            )
        }
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
            onBack = {}
        )
    }
}
