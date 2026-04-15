package com.example.zhiwu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.example.zhiwu.ui.navigation.AppDestinations
import com.example.zhiwu.ui.screens.AnalysisScreen
import com.example.zhiwu.ui.screens.HistoryScreen
import com.example.zhiwu.ui.screens.HomeScreen
import com.example.zhiwu.ui.screens.ProfileScreen
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
@Composable
fun ZhiWuApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = { Icon(painterResource(it.icon), contentDescription = it.label) },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("UbiNIRS 织物分析", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    )
                )
            }
        ) { innerPadding ->
            when (currentDestination) {
                AppDestinations.HOME -> HomeScreen(
                    modifier = Modifier.padding(innerPadding),
                    onNavigateToAnalysis = { currentDestination = AppDestinations.ANALYSIS }
                )
                AppDestinations.ANALYSIS -> AnalysisScreen(modifier = Modifier.padding(innerPadding))
                AppDestinations.HISTORY -> HistoryScreen(modifier = Modifier.padding(innerPadding))
                AppDestinations.PRPFILE -> ProfileScreen(modifier = Modifier.padding(innerPadding))
            }
        }
    }
}