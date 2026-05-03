package com.example.zhiwu

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zhiwu.network.ApiClient
import kotlinx.coroutines.launch

// 1. 定义聊天消息的数据结构
data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val isHardwareAction: Boolean = false // 标识这条消息是否带有“正在调度硬件”的转圈动画
)

// 2. 智能体超级大脑 ViewModel
class AgentViewModel : ViewModel() {

    // 维护聊天记录的列表，Compose UI 会自动观察它的变化并触发重绘
    val messages = mutableStateListOf(
        ChatMessage("您好！我是 UbiAgent，您的专属光谱分析智能体。请问需要我帮您检测什么？", isUser = false)
    )
    var isDeviceConnected = false

    // 修改逻辑：不再直接触发，而是告知 Activity 我们的意图
    fun handleScanAction() {
        if (isDeviceConnected) {
            // 场景 A：已连接，直接扣动扳机
            onRequireHardwareScan?.invoke()
        } else {
            // 场景 B：未连接，先展示 UI 状态，再请求 Activity 建立连接
            messages.add(ChatMessage("检测到设备未连接，正在尝试自动寻检并连接 NIRScan...", isUser = false, isHardwareAction = true))
            onRequireConnectionAndScan?.invoke()
        }
    }
    // 这个回调像一个“神经突触”，用于通知 MainActivity 去真正扣动物理硬件的扳机
    var onRequireHardwareScan: (() -> Unit)? = null

    // 补充对应的回调变量
    var onRequireConnectionAndScan: (() -> Unit)? = null

    /**
     * 场景 A：用户在输入框发文字给大模型
     */
    fun sendMessage(userText: String) {
        if (userText.isBlank()) return

        // 把用户的话显示到右侧界面上
        messages.add(ChatMessage(userText, isUser = true))

        viewModelScope.launch {
            try {
                // 1. 使用你统一的 ApiClient 发送网络请求给 Django 中枢
                val response = ApiClient.api.sendChatMessage(text = userText)

                // 2. 拦截器：判断大脑是否下达了硬件物理动作指令
                if (response.status == "action_required" && response.action == "START_SCAN") {

                    handleScanAction()

                }
                // 3. 正常回复：直接把大模型的话显示出来
                else if (response.status == "success" && response.reply != null) {
                    messages.add(ChatMessage(response.reply, isUser = false))
                }

            } catch (e: Exception) {
                messages.add(ChatMessage("网络请求失败，请检查轻薄本的中枢服务是否开启，或 IP 地址是否正确。\n报错详情: ${e.message}", isUser = false))
            }
        }
    }

    /**
     * 场景 B：硬件扫描完毕，带着真实 Hex 数据再次请求大模型出报告
     */
    fun sendHardwareData(originalUserText: String, scanDataHex: String) {
        viewModelScope.launch {
            try {
                // 告诉云端：我扫完了，这是刚刚用户的话，以及新鲜出炉的物理数据，快给我出报告！
                val response = ApiClient.api.sendChatMessage(
                    text = originalUserText,
                    scanData = scanDataHex
                )

                // 把之前那个“正在调度硬件...”的转圈圈消息删掉
                if (messages.isNotEmpty() && messages.last().isHardwareAction) {
                    messages.removeLast()
                }

                // 渲染出最终的检测报告
                if (response.status == "success" && response.reply != null) {
                    messages.add(ChatMessage("✅ **硬件扫描成功！**\n\n${response.reply}", isUser = false))
                } else {
                    messages.add(ChatMessage("云端解析光谱数据失败，请重试。", isUser = false))
                }

            } catch (e: Exception) {
                // 发生错误时也要把转圈圈动画消掉
                if (messages.isNotEmpty() && messages.last().isHardwareAction) {
                    messages.removeLast()
                }
                messages.add(ChatMessage("回传硬件数据到云端失败: ${e.message}", isUser = false))
            }
        }
    }
    /**
     * 补全逻辑：接收硬件原始数据，转换为 Hex 并触发回传
     * 解决 MainActivity 中的 Unresolved reference 报错
     */
    // AgentViewModel.kt 内部

    /**
     * 更新 UI 状态消息，让用户知道校准进度
     */
    fun updateStatus(statusText: String) {
        // 查找最后一条消息，如果是“正在调度硬件”，则直接修改它的文字
        if (messages.isNotEmpty() && messages.last().isHardwareAction) {
            val lastIndex = messages.size - 1
            messages[lastIndex] = messages[lastIndex].copy(text = statusText)
        } else {
            messages.add(ChatMessage(statusText, isUser = false, isHardwareAction = true))
        }
    }

    /**
     * 接收硬件原始数据，转换为 Hex 并触发回传
     * 解决 MainActivity 中的 Unresolved reference 报错
     */
    fun onScanDataReceived(scanData: ByteArray) {
        val hexData = scanData.joinToString(separator = "") { String.format("%02X", it) }
        val originalUserMsg = messages.lastOrNull { it.isUser }?.text ?: "光谱分析请求"
        sendHardwareData(originalUserMsg, hexData)
    }
}
