package com.example.zhiwu.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.zhiwu.model.HistoryInfo
import com.example.zhiwu.ui.components.HistoryItemCard

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