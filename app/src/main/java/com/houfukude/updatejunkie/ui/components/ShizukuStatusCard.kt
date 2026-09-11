package com.houfukude.updatejunkie.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.houfukude.updatejunkie.R
import com.houfukude.updatejunkie.ui.theme.UpdateJunkieTheme

/**
 * 展示 Shizuku 当前状态并引导用户完成安装 / 授权的卡片。
 *
 * 卡片背景色随状态变化：未安装为中性色、已授权为主色、未授权为错误色。
 * 仅在需要用户操作时才显示按钮：未安装显示"去下载"，
 * 服务已运行但未授权显示"请求授权"。
 *
 * @param isInstalled Shizuku 应用是否已安装
 * @param isAvailable Shizuku 服务是否正在运行
 * @param hasPermission 本应用是否已获得授权
 * @param onRequestPermission 点击"请求授权"时的回调
 * @param onDownloadClick 点击"去下载"时的回调，通常跳转 Shizuku 官网
 */
@Composable
fun ShizukuStatusCard(
    isInstalled: Boolean,
    isAvailable: Boolean,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onDownloadClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                !isInstalled -> MaterialTheme.colorScheme.surfaceVariant
                hasPermission -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val statusText = when {
                    !isInstalled -> stringResource(R.string.shizuku_not_installed)
                    !isAvailable -> stringResource(R.string.shizuku_installed)
                    else -> stringResource(R.string.shizuku_connected)
                }
                Text(
                    text = statusText,
                    fontWeight = FontWeight.Bold
                )
                val detailText = when {
                    !isInstalled -> stringResource(R.string.shizuku_download_hint)
                    !isAvailable -> stringResource(R.string.shizuku_not_running_hint)
                    hasPermission -> stringResource(R.string.shizuku_authorized)
                    else -> stringResource(R.string.shizuku_not_authorized_hint)
                }
                Text(
                    text = detailText,
                    fontSize = 12.sp
                )
            }
            
            Spacer(Modifier.width(8.dp))

            if (!isInstalled) {
                Button(onClick = onDownloadClick) {
                    Text(stringResource(R.string.go_to_download))
                }
            } else if (isAvailable && !hasPermission) {
                Button(onClick = onRequestPermission) {
                    Text(stringResource(R.string.request_authorization))
                }
            }
        }
    }
}


////////////////////////////////////////////////////////////////////////////////////////////////////

// preview

////////////////////////////////////////////////////////////////////////////////////////////////////


/** 卡片四种典型状态（已授权 / 未授权 / 服务未运行 / 未安装）的预览。 */
@Preview(showBackground = true)
@Composable
fun ShizukuStatusCardPreview() {
    UpdateJunkieTheme {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ShizukuStatusCard(
                isInstalled = true,
                isAvailable = true,
                hasPermission = true,
                onRequestPermission = {},
                onDownloadClick = {}
            )
            ShizukuStatusCard(
                isInstalled = true,
                isAvailable = true,
                hasPermission = false,
                onRequestPermission = {},
                onDownloadClick = {}
            )
            ShizukuStatusCard(
                isInstalled = true,
                isAvailable = false,
                hasPermission = false,
                onRequestPermission = {},
                onDownloadClick = {}
            )
            ShizukuStatusCard(
                isInstalled = false,
                isAvailable = false,
                hasPermission = false,
                onRequestPermission = {},
                onDownloadClick = {}
            )
        }
    }
}