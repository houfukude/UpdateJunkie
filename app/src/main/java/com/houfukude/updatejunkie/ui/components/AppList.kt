package com.houfukude.updatejunkie.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.rememberAsyncImagePainter
import com.houfukude.updatejunkie.R
import com.houfukude.updatejunkie.model.AppInfo
import com.houfukude.updatejunkie.ui.theme.UpdateJunkieTheme
import com.houfukude.updatejunkie.utils.MarketUtils

/**
 * 可滚动的应用列表。
 *
 * 列表项以 `包名 + userId` 作为 key，保证多用户下同一包名的多个条目互不冲突。
 *
 * @param apps 待展示的应用列表
 * @param modifier 列表容器修饰符
 * @param headerContent 置顶的头部内容（如 Shizuku 状态卡片与加载进度），为 null 时不显示
 * @param onGetUpdateUrl 获取应用更新 URL 的回调
 * @param onSetUpdateUrl 设置应用更新 URL 的回调
 * @param initialExpandedPackageName 初始展开菜单的应用包名（仅用于预览或特定引导）
 */
@Composable
fun AppList(
    apps: List<AppInfo>,
    modifier: Modifier = Modifier,
    headerContent: (@Composable () -> Unit)? = null,
    onGetUpdateUrl: (String) -> String? = { null },
    onSetUpdateUrl: (String, String) -> Unit = { _, _ -> },
    initialExpandedPackageName: String? = null
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        if (headerContent != null) {
            item {
                headerContent()
            }
        }
        items(apps, key = { it.packageName + it.userId }) { app ->
            AppItem(
                app = app,
                onGetUpdateUrl = onGetUpdateUrl,
                onSetUpdateUrl = onSetUpdateUrl,
                initialShowMenu = app.packageName == initialExpandedPackageName
            )
        }
    }
}

/**
 * 单个应用列表项。
 *
 * 背景色按优先级区分：已禁用 > 系统应用 > 非主用户 > ADB 安装 > 安装来源色。
 * 点击条目跳转到对应应用市场详情页，点击右侧箭头跳转到系统应用详情页。
 *
 * @param app 该条目对应的应用信息
 * @param onGetUpdateUrl 获取应用更新 URL 的回调
 * @param onSetUpdateUrl 设置应用更新 URL 的回调
 * @param initialShowMenu 是否初始显示操作菜单
 */
@Composable
fun AppItem(
    app: AppInfo,
    onGetUpdateUrl: (String) -> String?,
    onSetUpdateUrl: (String, String) -> Unit,
    initialShowMenu: Boolean = false
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showMenu by remember { mutableStateOf(initialShowMenu) }
    var showDialog by remember { mutableStateOf(false) }

    val backgroundColor = when {
        !app.isEnabled -> colorResource(R.color.item_disabled_bg)
        app.isSystemApp -> colorResource(R.color.item_system_bg)
        app.userId != "0" -> colorResource(R.color.item_user_bg)
        app.isAdbInstalled -> colorResource(R.color.item_adb_bg)
        else -> MarketUtils.getMarketColor(app.installerPackageName)?.let { colorResource(it) }
            ?: Color.Transparent
    }

    if (showDialog) {
        UpdateConfigDialog(
            initialUrl = onGetUpdateUrl(app.packageName) ?: "",
            onDismiss = { showDialog = false },
            onSave = { url ->
                onSetUpdateUrl(app.packageName, url)
                showDialog = false
                Toast.makeText(context, R.string.update_url_saved, Toast.LENGTH_SHORT).show()
            }
        )
    }

    ListItem(
        modifier = Modifier.clickable {
            if (app.hasUpdateUrl) {
                val url = onGetUpdateUrl(app.packageName)
                if (!url.isNullOrBlank()) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, e.localizedMessage, Toast.LENGTH_SHORT).show()
                    }
                    return@clickable
                }
            }
            MarketUtils.launchMarket(context, app.packageName, app.installerPackageName)
        },
        colors = ListItemDefaults.colors(containerColor = backgroundColor),
        headlineContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(app.label)
                        if (!app.isEnabled) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = colorResource(R.color.item_badge_disabled_bg),
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    stringResource(R.string.disabled_badge),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        app.packageName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                if (app.isSystemApp) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "System App",
                        tint = colorResource(R.color.item_badge_system_icon),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_go_to_settings)) },
                            onClick = {
                                showMenu = false
                                val intent =
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", app.packageName, null)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                context.startActivity(intent)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_go_to_market)) },
                            onClick = {
                                showMenu = false
                                MarketUtils.launchMarket(
                                    context,
                                    app.packageName,
                                    app.installerPackageName
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_copy_package_name)) },
                            onClick = {
                                showMenu = false
                                clipboardManager.setText(AnnotatedString(app.packageName))
                                Toast.makeText(
                                    context,
                                    R.string.package_name_copied,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_configure_update_url)) },
                            onClick = {
                                showMenu = false
                                showDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_go_to_update_url)) },
                            onClick = {
                                showMenu = false
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
                                        Toast.makeText(
                                            context,
                                            e.localizedMessage,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        },
        supportingContent = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val sourceText = when {
                        app.isAdbInstalled -> stringResource(R.string.adb_installed)
                        app.installerPackageName == app.packageName -> stringResource(R.string.self_updating_apps)
                        else -> stringResource(
                            R.string.source_prefix,
                            app.installerLabel ?: stringResource(R.string.unknown)
                        )
                    }
                    Text(
                        sourceText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (app.userId != "0") {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.user_prefix, app.userId),
                            fontSize = 12.sp,
                            color = colorResource(R.color.item_user_text)
                        )
                    }
                }
                app.versionName?.let {
                    Text(
                        stringResource(R.string.version_prefix, it, app.versionCode),
                        fontSize = 12.sp,
                        color = colorResource(R.color.item_secondary_text),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        leadingContent = {
            Box {
                Image(
                    painter = rememberAsyncImagePainter(app.icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .then(if (!app.isEnabled) Modifier.graphicsLayer(alpha = 0.5f) else Modifier)
                )
                if (app.hasUpdateUrl) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.extraSmall,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .graphicsLayer {
                                translationX = 4.dp.toPx()
                                translationY = 4.dp.toPx()
                            }
                    ) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = "Configured",
                            tint = Color.White,
                            modifier = Modifier
                                .size(12.dp)
                                .padding(1.dp)
                        )
                    }
                }
            }
        }
    )
}

/**
 * 用于配置应用更新地址的对话框。
 *
 * @param initialUrl 初始填入的 URL
 * @param onDismiss 对话框取消回调
 * @param onSave 点击保存回调，返回输入的 URL
 */
@Composable
fun UpdateConfigDialog(
    initialUrl: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var url by remember { mutableStateOf(initialUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.configure_update_url_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.update_url_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(url) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

////////////////////////////////////////////////////////////////////////////////////////////////////

// preview

/** 普通应用与系统应用两种条目的样式预览。 */
@Preview(showBackground = true)
@Composable
fun AppItemPreview() {
    UpdateJunkieTheme {
        Column {
            AppItem(
                app = AppInfo(
                    packageName = "com.example.app1",
                    label = "Normal App (Play Store)",
                    icon = null,
                    versionName = "1.2.3",
                    versionCode = 45,
                    installerPackageName = "com.android.vending",
                    installerLabel = "Google Play Store",
                    isSystemApp = false,
                    isEnabled = true,
                    userId = "0",
                    isAdbInstalled = false,
                    hasUpdateUrl = true
                ),
                onGetUpdateUrl = { null },
                onSetUpdateUrl = { _, _ -> }
            )
            AppItem(
                app = AppInfo(
                    packageName = "com.example.system",
                    label = "System App",
                    icon = null,
                    versionName = "10",
                    versionCode = 100,
                    installerPackageName = null,
                    installerLabel = null,
                    isSystemApp = true,
                    isEnabled = true,
                    userId = "0",
                    isAdbInstalled = false,
                    hasUpdateUrl = true
                ),
                onGetUpdateUrl = { null },
                onSetUpdateUrl = { _, _ -> }
            )
        }
    }
}

/** 应用列表（无头部内容）的整体预览，展示第二项菜单展开的状态。 */
@Preview(showBackground = true)
@Composable
fun AppListPreview() {
    UpdateJunkieTheme {
        AppList(
            apps = listOf(
                AppInfo(
                    packageName = "com.example.app1",
                    label = "Play Store App",
                    icon = null,
                    versionName = "1.0",
                    versionCode = 1,
                    installerPackageName = "com.android.vending",
                    installerLabel = "Google Play Store",
                    isSystemApp = false,
                    isEnabled = true,
                    userId = "0",
                    isAdbInstalled = false,
                    hasUpdateUrl = true
                ),
                AppInfo(
                    packageName = "com.example.app2",
                    label = "System App",
                    icon = null,
                    versionName = "12",
                    versionCode = 120,
                    installerPackageName = null,
                    installerLabel = null,
                    isSystemApp = true,
                    isEnabled = true,
                    userId = "0",
                    isAdbInstalled = false
                )
            ),
            initialExpandedPackageName = "com.example.app2"
        )
    }
}

