package com.example.zhiwu.ui.screens
// 导入网络请求客户端


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.zhiwu.model.HistoryInfo
import com.example.zhiwu.network.ApiClient
import com.example.zhiwu.ui.components.HistoryItemCard
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    // 状态管理
    var historyList by remember { mutableStateOf<List<HistoryInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // 每次进入该页面时，自动触发网络请求拉取云端数据
    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        try {
            // 调用刚写好的网络接口
            val response = ApiClient.api.getHistory()
            if (response.status == "success") {
                // 🚨 修复区：将后端数据映射成你的 HistoryInfo UI 模型，使用正确的参数名
                historyList = response.data.map { item ->
                    HistoryInfo(
                        id = item.id.toString(),
                        materialName = item.label,
                        scanDate = item.created_at.substring(0, 16).replace("T", " "), // 简单格式化时间
                        confidence = "${item.score}%"
                    )
                }
            } else {
                errorMessage = response.message ?: "获取历史记录失败"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "网络连接异常，请检查服务器"
        } finally {
            isLoading = false
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            isLoading -> {
                // 加载中动画
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("正在同步云端数据...", color = Color.Gray)
                }
            }
            errorMessage != null -> {
                // 错误提示
                Text("⚠️ $errorMessage", color = MaterialTheme.colorScheme.error)
            }
            historyList.isEmpty() -> {
                // 空数据提示
                Text("暂无扫描记录，快去扫一扫吧！", color = Color.Gray)
            }
            else -> {
                // 成功渲染列表
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(historyList) { historyItem ->
                        HistoryItemCard(item = historyItem)
                    }
                }
            }
        }
    }
}