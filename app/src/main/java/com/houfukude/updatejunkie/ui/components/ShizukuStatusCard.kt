package com.houfukude.updatejunkie.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.houfukude.updatejunkie.ui.theme.UpdateJunkieTheme

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
                    !isInstalled -> "Shizuku 未安装"
                    !isAvailable -> "Shizuku 已安装"
                    else -> "Shizuku 已连接"
                }
                Text(
                    text = statusText,
                    fontWeight = FontWeight.Bold
                )
                val detailText = when {
                    !isInstalled -> "点击按钮跳转官网下载安装"
                    !isAvailable -> "服务未运行，请先启动 Shizuku"
                    hasPermission -> "已获得授权"
                    else -> "尚未获得授权，请点击请求权限"
                }
                Text(
                    text = detailText,
                    fontSize = 12.sp
                )
            }
            
            Spacer(Modifier.width(8.dp))

            if (!isInstalled) {
                Button(onClick = onDownloadClick) {
                    Text("去下载")
                }
            } else if (isAvailable && !hasPermission) {
                Button(onClick = onRequestPermission) {
                    Text("请求授权")
                }
            }
        }
    }
}


////////////////////////////////////////////////////////////////////////////////////////////////////

// preview

////////////////////////////////////////////////////////////////////////////////////////////////////


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