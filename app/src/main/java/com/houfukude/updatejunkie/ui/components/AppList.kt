package com.houfukude.updatejunkie.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowForwardIos
import com.houfukude.updatejunkie.R
import com.houfukude.updatejunkie.model.AppInfo
import com.houfukude.updatejunkie.ui.theme.UpdateJunkieTheme
import com.houfukude.updatejunkie.utils.MarketUtils

@Composable
fun AppList(
    apps: List<AppInfo>,
    modifier: Modifier = Modifier,
    headerContent: (@Composable () -> Unit)? = null
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        if (headerContent != null) {
            item {
                headerContent()
            }
        }
        items(apps, key = { it.packageName + it.userId }) { app ->
            AppItem(app = app)
        }
    }
}

@Composable
fun AppItem(app: AppInfo) {
    val context = LocalContext.current
    val backgroundColor = when {
        !app.isEnabled -> colorResource(R.color.item_disabled_bg)
        app.isSystemApp -> colorResource(R.color.item_system_bg)
        app.userId != "0" -> colorResource(R.color.item_user_bg)
        app.isAdbInstalled -> colorResource(R.color.item_adb_bg)
        else -> MarketUtils.getMarketColor(app.installerPackageName)?.let { colorResource(it) }
            ?: Color.Transparent
    }

    ListItem(
        modifier = Modifier.clickable {
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
                                    "已禁用",
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
                IconButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", app.packageName, null)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "App Info",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        },
        supportingContent = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val sourceText =
                        if (app.isAdbInstalled) "ADB 安装" else "来源: ${app.installerLabel ?: "未知"}"
                    Text(
                        sourceText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (app.userId != "0") {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "用户: ${app.userId}",
                            fontSize = 12.sp,
                            color = colorResource(R.color.item_user_text)
                        )
                    }
                }
                app.versionName?.let {
                    Text(
                        "版本: $it (${app.versionCode})",
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
            }
        }
    )
}

////////////////////////////////////////////////////////////////////////////////////////////////////

// preview

////////////////////////////////////////////////////////////////////////////////////////////////////

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
                    isAdbInstalled = false
                )
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
                    isAdbInstalled = false
                )
            )
        }
    }
}

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
                    isAdbInstalled = false
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
            )
        )
    }
}

