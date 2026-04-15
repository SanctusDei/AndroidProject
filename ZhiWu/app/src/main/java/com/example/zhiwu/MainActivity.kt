package com.example.zhiwu

import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.example.zhiwu.ui.theme.ZhiWuTheme

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
                title = {Text("织 物")},

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        }
        ) { innerPadding ->
            when (currentDestination) {
                AppDestinations.HOME -> HomeScreen(modifier = Modifier.padding(innerPadding))
                AppDestinations.ANALYSIS -> AnalysisScreen(modifier = Modifier.padding(innerPadding))
                AppDestinations.HISTORY -> HistoryScreen(modifier = Modifier.padding(innerPadding))
                // 注意你代码里的拼写是 PRPFILE，保持一致即可
                AppDestinations.PRPFILE -> ProfileScreen(modifier = Modifier.padding(innerPadding))
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    HOME("Home", R.drawable.ic_home),
    ANALYSIS("Analysis", R.drawable.ic_analysis),
    HISTORY("History", R.drawable.ic_history,),
    PRPFILE("Profile", R.drawable.ic_account_box),
}


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier.fillMaxSize()) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )


}

// 预览
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ZhiWuTheme {
        Greeting("Android")
    }
}


@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    // TODO
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp), // 屏幕四周留出呼吸感间距
        verticalArrangement = Arrangement.spacedBy(20.dp) // 每个模块之间空出 20dp
    ) {

        // --- 模块 1：欢迎语 ---
        Text(
            text = "欢迎使用织物APP",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // --- 模块 2：硬件设备状态卡片 ---
        // 使用 ElevatedCard 做一个有立体感的卡片
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
                // 一个绿色的连接成功图标
//                Icon(
//                    imageVector = Icons.Filled.CheckCircle,
//                    contentDescription = "已连接",
//                    tint = Color(0xFF4CAF50), // 绿色
//                    modifier = Modifier.size(36.dp)
//                )
            }
        }

        // --- 模块 3：数据统计 (左右并排的两个小卡片) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 左边卡片：累计扫描
            OutlinedCard(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("累计分析", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("128 次", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
            }

            // 右边卡片：最近检测
            OutlinedCard(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("最近检测", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("100% 棉", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // 占位符，把按钮推到屏幕最下方
        Spacer(modifier = Modifier.weight(1f))

        // --- 模块 4：底部显眼的快捷按钮 ---
        Button(

            onClick = {

                // TODO

                },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp) // 让按钮稍微高一点，方便手指点击
        ) {
            Text("开始新的光谱扫描", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun AnalysisScreen(modifier: Modifier = Modifier) {
    // TODO
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("这是分析页")
    }
}

@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    // TODO
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("这是记录页")
    }
}

@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    // TODO
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("这是个人界面")
    }
}