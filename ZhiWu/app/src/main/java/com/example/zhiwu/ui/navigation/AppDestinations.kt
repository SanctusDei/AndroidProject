package com.example.zhiwu.ui.navigation

import com.example.zhiwu.R

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    HOME("主页", R.drawable.ic_home),
    ANALYSIS("分析", R.drawable.ic_analysis),
    HISTORY("历史", R.drawable.ic_history),
    PRPFILE("用户", R.drawable.ic_account_box), // 注意这里的拼写 PRPFILE 保持和你现有资源一致
}