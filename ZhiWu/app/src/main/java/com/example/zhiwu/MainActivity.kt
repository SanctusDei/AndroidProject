package com.example.zhiwu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.example.zhiwu.model.HistoryInfo
import com.example.zhiwu.ui.theme.ZhiWuTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZhiWuTheme {
                ZhiWuApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewScreenSizes
@Composable
fun ZhiWuApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            painterResource(it.icon),
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
            CenterAlignedTopAppBar(
                title = { Text("UbiNIRS 织物分析", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        }) { innerPadding ->
            when (currentDestination) {
                AppDestinations.HOME -> HomeScreen(
                    modifier = Modifier.padding(innerPadding),
                    onNavigateToAnalysis = {
                        currentDestination = AppDestinations.ANALYSIS
                    }
                )
                AppDestinations.ANALYSIS -> AnalysisScreen(modifier = Modifier.padding(innerPadding))
                AppDestinations.HISTORY -> HistoryScreen(modifier = Modifier.padding(innerPadding))
                AppDestinations.PRPFILE -> ProfileScreen(modifier = Modifier.padding(innerPadding))
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    HOME("主页", R.drawable.ic_home),
    ANALYSIS("分析", R.drawable.ic_analysis),
    HISTORY("历史", R.drawable.ic_history),
    PRPFILE("用户", R.drawable.ic_account_box),
}

// ==========================================
// 1. 主页 (HomeScreen)
// ==========================================
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
            .verticalScroll(rememberScrollState()) // 添加滚动支持，防止小屏幕溢出
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "系统概览",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                    Text("NIRScan Nano 光谱仪", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("设备电量: 85% | 信号: 良好", style = MaterialTheme.typography.bodyMedium)
                }
                // 修复了图标显示
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "已连接",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("开始新的光谱扫描", style = MaterialTheme.typography.titleMedium)
        }
    }
}

// ==========================================
// 2. 分析页 (AnalysisScreen)
// ==========================================
@Composable
fun AnalysisScreen(modifier: Modifier = Modifier) {
    var isConnected by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf("") }

    LaunchedEffect(isConnecting) {
        if (isConnecting) {
            delay(1500)
            isConnected = true
            isConnecting = false
        }
    }
    LaunchedEffect(isScanning) {
        if (isScanning) {
            delay(2500)
            scanResult = "分析结果：100% 棉 (Cotton)\n置信度：98.5%"
            isScanning = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = if (isConnected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 修复了蓝牙图标逻辑，使用官方 Material Icons
                    Icon(
                        imageVector = if (isConnected) Icons.Filled.BluetoothConnected else Icons.Filled.Bluetooth,
                        contentDescription = "Bluetooth",
                        tint = if (isConnected) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("DLP NIRScan Nano", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = when {
                                isConnecting -> "正在建立连接..."
                                isConnected -> "已连接 | 电量: 90%"
                                else -> "未连接 (请开启设备电源)"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isConnected) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        if (isConnected) {
                            isConnected = false
                            scanResult = ""
                        } else {
                            isConnecting = true
                        }
                    },
                    enabled = !isConnecting && !isScanning
                ) {
                    Text(if (isConnected) "断开" else "连接")
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isScanning) {
                CircularProgressIndicator(modifier = Modifier.size(80.dp), strokeWidth = 6.dp)
                Spacer(modifier = Modifier.height(24.dp))
                Text("正在采集近红外光谱...", style = MaterialTheme.typography.titleMedium)
                Text("请保持设备紧贴织物表面", color = Color.Gray)
            } else if (scanResult.isNotEmpty()) {
                Text("✅ 扫描完成", style = MaterialTheme.typography.titleMedium, color = Color(0xFF4CAF50))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = scanResult,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                Text(
                    text = if (isConnected) "设备就绪，请将传感器对准织物" else "请先连接 NIRScan Nano 设备",
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                isScanning = true
                scanResult = ""
            },
            enabled = isConnected && !isScanning,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            Text(
                text = if (!isConnected) "需先连接设备" else if (isScanning) "扫描中..." else "开始光谱扫描",
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

// ==========================================
// 3. 历史记录页 (HistoryScreen)
// ==========================================
@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    val historyList = remember {
        listOf(
            HistoryInfo("001", "100% 棉 (Cotton)", "2026-04-15 14:30", "99.2%"),
            HistoryInfo("002", "聚酯纤维 (Polyester)", "2026-04-14 09:15", "95.8%"),
            HistoryInfo("003", "羊毛混纺 (Wool Blend)", "2026-04-12 16:45", "88.5%"),
            HistoryInfo("004", "亚麻 (Linen)", "2026-04-10 11:20", "92.0%"),
            HistoryInfo("005", "真丝 (Silk)", "2026-04-08 15:10", "97.1%")
        )
    }

    LazyColumn(
        modifier = modifier
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

@Composable
fun HistoryItemCard(item: HistoryInfo) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
                    text = item.materialName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "扫描时间: ${item.scanDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = item.confidence, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("匹配度", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

// ==========================================
// 4. 个人中心页 (ProfileScreen) - 全新设计
// ==========================================
@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    var isOfflineMode by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // 头像区域
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = "User Avatar",
                modifier = Modifier.size(60.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("UbiNIRS 研究员", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("南京 · 物联网工程开发", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(32.dp))

        // 设置选项列表
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("设备与采集设置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("本地离线推理模式", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = isOfflineMode,
                        onCheckedChange = { isOfflineMode = it }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Text("清理本地缓存数据", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}