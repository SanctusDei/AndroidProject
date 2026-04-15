package com.example.zhiwu.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.zhiwu.model.HistoryInfo
import com.example.zhiwu.ui.components.HistoryItemCard

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToAnalysis: () -> Unit
) {
    val recentRecords = remember {
        listOf(
            HistoryInfo("001", "100% 棉 (Cotton)", "10分钟前", "99.2%"),
            HistoryInfo("002", "聚酯纤维 (Polyester)", "1小时前", "95.8%"),
            HistoryInfo("003", "羊毛混纺 (Wool Blend)", "昨天", "88.5%")
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(text = "系统概览", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("NIRScan Nano 光谱仪", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("设备电量: 85% | 信号: 良好", style = MaterialTheme.typography.bodyMedium)
                }
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "已连接",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedCard(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("累计分析", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("128 次", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
            }
            OutlinedCard(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("最近检测", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("100% 棉", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Text("最近检测记录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        recentRecords.forEach { record ->
            HistoryItemCard(item = record)
        }

        Button(
            onClick = { onNavigateToAnalysis() },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("开始新的光谱扫描", style = MaterialTheme.typography.titleMedium)
        }
    }
}