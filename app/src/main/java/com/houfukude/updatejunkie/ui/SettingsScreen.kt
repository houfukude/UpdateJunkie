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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.houfukude.updatejunkie.data.ThemeConfig
import com.houfukude.updatejunkie.ui.theme.UpdateJunkieTheme
import com.houfukude.updatejunkie.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val themeConfig by viewModel.themeConfig.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            ListItem(
                headlineContent = { Text("语言") },
                supportingContent = { Text("中文（简体）") },
                leadingContent = { Icon(Icons.Default.Language, contentDescription = null) },
                modifier = Modifier.clickable { /* TODO */ }
            )
            ListItem(
                headlineContent = { Text("主题") },
                supportingContent = {
                    val text = when (themeConfig) {
                        ThemeConfig.FOLLOW_SYSTEM -> "跟随系统"
                        ThemeConfig.LIGHT -> "亮色主题"
                        ThemeConfig.DARK -> "暗色主题"
                    }
                    Text(text)
                },
                leadingContent = { Icon(Icons.Default.ColorLens, contentDescription = null) },
                modifier = Modifier.clickable { showThemeDialog = true }
            )
            ListItem(
                headlineContent = { Text("关于") },
                supportingContent = { Text("版本 1.0.0") },
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                modifier = Modifier.clickable { /* TODO */ }
            )
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("选择主题") },
            text = {
                Column {
                    ThemeOption("跟随系统", themeConfig == ThemeConfig.FOLLOW_SYSTEM) {
                        viewModel.setThemeConfig(ThemeConfig.FOLLOW_SYSTEM)
                        showThemeDialog = false
                    }
                    ThemeOption("亮色主题", themeConfig == ThemeConfig.LIGHT) {
                        viewModel.setThemeConfig(ThemeConfig.LIGHT)
                        showThemeDialog = false
                    }
                    ThemeOption("暗色主题", themeConfig == ThemeConfig.DARK) {
                        viewModel.setThemeConfig(ThemeConfig.DARK)
                        showThemeDialog = false
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

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

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    UpdateJunkieTheme {
        // SettingsScreen(onBack = {}) // Needs ViewModel now
    }
}
