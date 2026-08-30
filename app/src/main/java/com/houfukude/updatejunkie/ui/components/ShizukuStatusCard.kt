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
    isAvailable: Boolean,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasPermission) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (isAvailable) "Shizuku 已连接" else "Shizuku 未连接",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (hasPermission) "已获得授权" else "未获得授权",
                    fontSize = 12.sp
                )
            }
            if (isAvailable && !hasPermission) {
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
                isAvailable = true,
                hasPermission = true,
                onRequestPermission = {}
            )
            ShizukuStatusCard(
                isAvailable = true,
                hasPermission = false,
                onRequestPermission = {}
            )
            ShizukuStatusCard(
                isAvailable = false,
                hasPermission = false,
                onRequestPermission = {}
            )
        }
    }
}