package com.houfukude.updatejunkie.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.rememberAsyncImagePainter
import com.houfukude.updatejunkie.R
import com.houfukude.updatejunkie.model.AppInfo
import com.houfukude.updatejunkie.ui.theme.UpdateJunkieTvTheme
import com.houfukude.updatejunkie.utils.MarketUtils
import androidx.compose.material3.Surface as MobileSurface

/**
 * TV 端应用独立卡片，内置高动态遥控器焦点感知。
 *
 * @param app 该条目对应的应用信息
 * @param onItemClick 点击条目时的回调
 * @param onLongClick 长按条目时的回调，通常用于弹出操作菜单
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvAppItem(
    app: AppInfo,
    onItemClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        !app.isEnabled -> colorResource(R.color.item_disabled_bg)
        app.isSystemApp -> colorResource(R.color.item_system_bg)
        app.userId != "0" -> colorResource(R.color.item_user_bg)
        app.isAdbInstalled -> colorResource(R.color.item_adb_bg)
        else -> MarketUtils.getMarketColor(app.installerPackageName)?.let { colorResource(it) }
            ?: MaterialTheme.colorScheme.surface
    }

    Surface(
        onClick = onItemClick,
        onLongClick = onLongClick,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = backgroundColor,
            focusedContainerColor = colorResource(R.color.tv_focus_bg),
            focusedContentColor = colorResource(R.color.tv_focus_text)
        ),
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.medium),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(48.dp)) {
                Image(
                    painter = rememberAsyncImagePainter(app.icon ?: app.packageName),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.Center)
                        .then(if (!app.isEnabled) Modifier.graphicsLayer(alpha = 0.5f) else Modifier)
                )
                if (app.hasUpdateUrl) {
                    MobileSurface(
                        color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        shape = androidx.compose.material3.MaterialTheme.shapes.extraSmall,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .graphicsLayer {
                                translationX = 6.dp.toPx()
                                translationY = 6.dp.toPx()
                            }
                    ) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = "Configured",
                            tint = Color.White,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(2.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!app.isEnabled) {
                        Spacer(Modifier.width(6.dp))
                        MobileSurface(
                            color = colorResource(R.color.item_badge_disabled_bg),
                            shape = androidx.compose.material3.MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                stringResource(R.string.disabled_badge),
                                color = Color.White,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1,
                    fontSize = 10.sp
                )

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
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (app.userId != "0") {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.user_prefix, app.userId),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = colorResource(R.color.item_user_text)
                        )
                    }
                }

                app.versionName?.let {
                    Text(
                        stringResource(R.string.version_prefix, it, app.versionCode),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = colorResource(R.color.item_secondary_text)
                    )
                }
            }

            if (app.isSystemApp) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "System App",
                    tint = colorResource(R.color.item_badge_system_icon),
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.Top)
                )
            }
        }
    }
}

/**
 * TV 端菜单操作按钮。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvMenuButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        colors = ClickableSurfaceDefaults.colors(
            focusedContainerColor = colorResource(R.color.tv_focus_bg),
            focusedContentColor = colorResource(R.color.tv_focus_text)
        ),
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.small),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            Text(text)
        }
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////
// preview
////////////////////////////////////////////////////////////////////////////////////////////////////

@OptIn(ExperimentalTvMaterial3Api::class)
@Preview(name = "TV App Item States Preview", showBackground = true)
@Composable
fun TvAppItemPreview() {
    val sampleApp = AppInfo(
        packageName = "com.houfukude.updatejunkie",
        label = "Update Junkie TV",
        icon = null,
        versionName = "1.0.5",
        versionCode = 105L,
        installerPackageName = null,
        installerLabel = "ADB",
        isSystemApp = false,
        isEnabled = true,
        userId = "0",
        isAdbInstalled = true,
        hasUpdateUrl = true
    )

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    UpdateJunkieTvTheme {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .width(350.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Normal State:", style = MaterialTheme.typography.labelMedium)
            TvAppItem(
                app = sampleApp,
                onItemClick = {},
                onLongClick = {}
            )

            Spacer(Modifier.height(10.dp))

            Text("Focused State (Interactive):", style = MaterialTheme.typography.labelMedium)
            TvAppItem(
                app = sampleApp,
                onItemClick = {},
                onLongClick = {},
                modifier = Modifier.focusRequester(focusRequester)
            )
        }
    }
}
