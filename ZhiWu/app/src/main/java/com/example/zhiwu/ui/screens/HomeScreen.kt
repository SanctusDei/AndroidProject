package com.example.zhiwu.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.zhiwu.model.HistoryInfo
import com.example.zhiwu.network.ApiClient
import com.example.zhiwu.ui.components.HistoryItemCard

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToAnalysis: () -> Unit
) {
    // 动态状态管理
    var totalScans by remember { mutableIntStateOf(0) }
    var recentLabel by remember { mutableStateOf("--") }
    var recentRecords by remember { mutableStateOf<List<HistoryInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // 每次进入首页时，自动从 Django 获取最新概览数据
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val response = ApiClient.api.getDashboardData()

            totalScans = response.total_scans

            // 如果有最近的记录，提取第一条作为“最近检测”的标签
            if (response.recent_activities.isNotEmpty()) {
                recentLabel = response.recent_activities.first().label

                // 将后端返回的 recent_activities 映射为 HistoryInfo
                recentRecords = response.recent_activities.map { item ->
                    HistoryInfo(
                        id = item.id.toString(),
                        materialName = item.label,
                        scanDate = item.time, // 这里后端传回的是 "HH:MM" 或格式化好的时间
                        confidence = "${item.score}%"
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 失败时可以在这里处理，例如弹个 Toast 或保持默认值
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(text = "系统概览", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        // 顶部设备状态卡片 (后续如果你想，也可以把电量和连接状态做成全局动态的)
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

        // 数据统计双卡片
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedCard(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("累计分析", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text("$totalScans 次", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
            OutlinedCard(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("最近检测", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text(recentLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        Text("最近检测记录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        // 动态渲染最新记录
        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (recentRecords.isEmpty()) {
            Text("暂无记录", color = Color.Gray, modifier = Modifier.padding(start = 4.dp))
        } else {
            recentRecords.forEach { record ->
                HistoryItemCard(item = record)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { onNavigateToAnalysis() },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("开始新的光谱扫描", style = MaterialTheme.typography.titleMedium)
        }
    }
}