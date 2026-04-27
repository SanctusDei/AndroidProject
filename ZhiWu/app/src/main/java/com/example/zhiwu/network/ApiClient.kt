package com.example.zhiwu.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// 1. 定义发给 Django 的请求数据格式
data class PredictRequest(
    val wavelength: List<Double>,
    val intensity: List<Double>,
    val reference: List<Double>,
    val device_mac: String,
    val label: String
)

// 2. 定义 Django 返回的数据格式
data class PredictResponse(
    val status: String,
    val record_id: Int?,
    val label: String?,
    val cotton: Double,
    val polyester: Double,
    val wool: Double,
    val nylon: Double,    // 新增
    val acrylic: Double,  // 新增
    val acetate: Double,  // 新增
    val score: Int,
    val suggestion: String,
    val message: String? = null
)


// 1. 定义 Django 返回的历史记录列表数据格式
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
// 2. 在 ZhiWuApi 接口里增加一个 GET 请求
interface ZhiWuApi {
    @POST("api/zhiwu/analysis/predict/")
    suspend fun predictSpectrum(@Body request: PredictRequest): PredictResponse

    // 👇 新增这个获取历史记录的接口
    @GET("api/zhiwu/analysis/history/")
    suspend fun getHistory(): HistoryResponse

    // 👇 新增：获取首页概览数据的接口
    @GET("api/zhiwu/analysis/dashboard/")
    suspend fun getDashboardData(): DashboardResponse
}

// 4. 创建 Retrofit 单例
object ApiClient {
    // ⚠️ 终极修复：直接把你的局域网 IP 写成字符串。
    // 请把下面的 192.168.1.100 换成你电脑真实的局域网 IPv4 地址！
    // 务必注意：网址的最后一定要带上反斜杠 "/"
    private const val BASE_URL = "http://10.224.176.184:18000/"

    val retrofit: ZhiWuApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ZhiWuApi::class.java)
    }
}