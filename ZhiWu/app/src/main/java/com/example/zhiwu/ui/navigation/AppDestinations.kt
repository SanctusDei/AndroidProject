package com.example.zhiwu.ui.navigation

import com.example.zhiwu.R

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    HOME("主页", R.drawable.ic_home),
    ANALYSIS("分析", R.drawable.ic_analysis),
    // 举个例子，在你的 AppDestinations 枚举里加上这一行：
    AGENT("UbiAgent", R.drawable.ic_analysis), // 这里的图标你可以之后换成专属的 AI 图标
    HISTORY("历史", R.drawable.ic_history),
    PRPFILE("用户", R.drawable.ic_account_box), // 注意这里的拼写 PRPFILE 保持和你现有资源一致
}