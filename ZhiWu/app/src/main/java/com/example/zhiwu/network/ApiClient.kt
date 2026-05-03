package com.example.zhiwu.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

// ==================== 1. 原有的手动分析数据格式 ====================
data class PredictRequest(
    val wavelength: List<Double>,
    val intensity: List<Double>,
    val reference: List<Double>,
    val device_mac: String,
    val label: String
)

data class PredictResponse(
    val status: String,
    val record_id: Int?,
    val label: String?,
    val cotton: Double,
    val polyester: Double,
    val wool: Double,
    val nylon: Double,
    val acrylic: Double,
    val acetate: Double,
    val score: Int,
    val suggestion: String,
    val message: String? = null
)

data class HistoryRecordItem(
    val id: Int,
    val label: String,
    val created_at: String,
    val score: Int
)

data class HistoryResponse(
    val status: String,
    val data: List<HistoryRecordItem>,
    val message: String?
)

data class RecentActivityItem(
    val id: Int,
    val label: String,
    val score: Int,
    val time: String
)

data class DashboardResponse(
    val total_scans: Int,
    val today_scans: Int,
    val avg_score: Double,
    val recent_activities: List<RecentActivityItem>
)

// ==================== 2. 【新增】AI 智能体专属数据格式 ====================
data class ChatResponse(
    val status: String,      // "success" 或 "action_required" 或 "error"
    val reply: String?,      // 大模型的普通回复内容
    val action: String?      // 硬件动作指令，例如 "[ACTION: SCAN]"
)

// ==================== 3. 接口定义 ====================
interface ZhiWuApi {
    // ---- 原有接口 ----
    @POST("api/zhiwu/analysis/predict/")
    suspend fun predictSpectrum(@Body request: PredictRequest): PredictResponse

    @GET("api/zhiwu/analysis/history/")
    suspend fun getHistory(): HistoryResponse

    @GET("api/zhiwu/analysis/dashboard/")
    suspend fun getDashboardData(): DashboardResponse

    // ---- 【新增】智能体聊天接口 ----
    @FormUrlEncoded
        @POST("api/UbiAgent/chat/") // 这里确保对应你 Django 里 Agent 的路由
    suspend fun sendChatMessage(
        @Field("text") text: String,
        @Field("scan_data") scanData: String? = null
    ): ChatResponse
}

// ==================== 4. 统一的 Retrofit 单例 ====================
object ApiClient {
    // 你的电脑局域网真实 IP
    private const val BASE_URL = "http://192.168.1.14:18000/"

    // 创建一次 Retrofit 实例，供所有接口复用
    private val retrofitInstance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // 暴露出的 API 接口
    val api: ZhiWuApi by lazy {
        retrofitInstance.create(ZhiWuApi::class.java)
    }
}