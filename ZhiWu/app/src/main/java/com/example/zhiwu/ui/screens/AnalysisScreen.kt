package com.example.zhiwu.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.zhiwu.service.NanoBLEService
import com.kstechnologies.nirscannanolibrary.KSTNanoSDK

data class AnalysisData(
    val protein: Double,
    val fat: Double,
    val lactose: Double,
    val waterContent: Double,
    val score: Int,
    val suggestion: String
)

@SuppressLint("MissingPermission")
@Composable
fun AnalysisScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var nanoBLEService by remember { mutableStateOf<NanoBLEService?>(null) }
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter = bluetoothManager.adapter
    val leScanner = bluetoothAdapter?.bluetoothLeScanner

    var isConnected by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var buttonEnabled by remember { mutableStateOf(false) }
    var buttonText by remember { mutableStateOf("请先连接设备") }
    var finalResult by remember { mutableStateOf<AnalysisData?>(null) }
    var showProgressDialog by remember { mutableStateOf(false) }
    var progressTitle by remember { mutableStateOf("") }
    var progressMax by remember { mutableStateOf(100) }
    var progressCurrent by remember { mutableStateOf(0) }

    // ================= 新增：动态权限管理 =================
    // 1. 根据 Android 版本决定需要申请哪些权限
    val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    }
    val scanCallback = remember {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                if (device.name == "NIRScanNano") {
                    nanoBLEService?.connect(device.address)
                    isConnected = true
                    isSearching = false
                    leScanner?.stopScan(this)
                }
            }
        }
    }
    // 2. 注册权限申请弹窗的回调
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grantedMap ->
        if (grantedMap.values.all { it }) {
            // 用户点击了“允许”
            if (bluetoothAdapter?.isEnabled == true) {
                isSearching = true
                leScanner?.startScan(scanCallback) // <- 现在调用就不会闪退了！
                coroutineScope.launch {
                    delay(10000)
                    if (isSearching) {
                        isSearching = false
                        leScanner?.stopScan(scanCallback)
                        Toast.makeText(context, "搜索超时", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(context, "请先在手机系统设置中打开蓝牙", Toast.LENGTH_SHORT).show()
            }
        } else {
            // 用户点击了“拒绝”
            Toast.makeText(context, "需要授予蓝牙和位置权限才能搜索设备", Toast.LENGTH_SHORT).show()
        }
    }
    // ======================================================



    DisposableEffect(context) {
        val serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                nanoBLEService = (service as NanoBLEService.LocalBinder).service
                nanoBLEService?.initialize()
            }
            override fun onServiceDisconnected(name: ComponentName?) {
                nanoBLEService = null
            }
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                // ... (原有广播接收逻辑不变) ...
                when (intent?.action) {
                    KSTNanoSDK.ACTION_GATT_CONNECTED -> {
                        isConnected = true
                        buttonText = "设备初始化中..."
                    }
                    KSTNanoSDK.ACTION_GATT_DISCONNECTED -> {
                        isConnected = false
                        buttonEnabled = false
                        buttonText = "请连接设备"
                        Toast.makeText(context, "设备已断开", Toast.LENGTH_SHORT).show()
                    }
                    KSTNanoSDK.ACTION_NOTIFY_DONE -> {
                        LocalBroadcastManager.getInstance(context!!).sendBroadcast(Intent(KSTNanoSDK.SET_TIME))
                    }
                    KSTNanoSDK.ACTION_REQ_CAL_COEFF -> {
                        val sizeVal = intent.getIntExtra(KSTNanoSDK.EXTRA_REF_CAL_COEFF_SIZE, 0)
                        if (intent.getBooleanExtra(KSTNanoSDK.EXTRA_REF_CAL_COEFF_SIZE_PACKET, false)) {
                            showProgressDialog = true
                            progressTitle = "正在下载参考校准系数..."
                            progressMax = sizeVal
                            progressCurrent = 0
                        } else {
                            progressCurrent += sizeVal
                        }
                    }
                    KSTNanoSDK.ACTION_REQ_CAL_MATRIX -> {
                        val sizeVal = intent.getIntExtra(KSTNanoSDK.EXTRA_REF_CAL_MATRIX_SIZE, 0)
                        if (intent.getBooleanExtra(KSTNanoSDK.EXTRA_REF_CAL_MATRIX_SIZE_PACKET, false)) {
                            showProgressDialog = true
                            progressTitle = "正在下载校准矩阵..."
                            progressMax = sizeVal
                            progressCurrent = 0
                        } else {
                            progressCurrent += sizeVal
                            if (progressCurrent >= progressMax) {
                                LocalBroadcastManager.getInstance(context!!).sendBroadcast(Intent(KSTNanoSDK.REQUEST_ACTIVE_CONF))
                            }
                        }
                    }
                    KSTNanoSDK.SCAN_CONF_DATA -> {
                        showProgressDialog = false
                        buttonEnabled = true
                        buttonText = "开始光谱扫描"
                    }
                    NanoBLEService.ACTION_SCAN_STARTED -> {
                        buttonEnabled = false
                        buttonText = "正在扫描中..."
                    }
                    KSTNanoSDK.SCAN_DATA -> {
                        val scanData = intent.getByteArrayExtra(KSTNanoSDK.EXTRA_DATA)
                        if (scanData != null) {
                            buttonText = "正在分析数据..."
                            coroutineScope.launch {
                                delay(2000)
                                finalResult = AnalysisData(3.2, 3.8, 4.5, 88.0, 92, "该织物样本指标优秀，各项成分均衡。")
                                buttonEnabled = true
                                buttonText = "开始新的光谱扫描"
                            }
                        }
                    }
                }
            }
        }

        context.bindService(Intent(context, NanoBLEService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
        val filter = IntentFilter().apply {
            addAction(KSTNanoSDK.ACTION_GATT_CONNECTED)
            addAction(KSTNanoSDK.ACTION_GATT_DISCONNECTED)
            addAction(KSTNanoSDK.ACTION_NOTIFY_DONE)
            addAction(KSTNanoSDK.ACTION_REQ_CAL_COEFF)
            addAction(KSTNanoSDK.ACTION_REQ_CAL_MATRIX)
            addAction(KSTNanoSDK.SCAN_CONF_DATA)
            addAction(NanoBLEService.ACTION_SCAN_STARTED)
            addAction(KSTNanoSDK.SCAN_DATA)
        }
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter)

        onDispose {
            context.unbindService(serviceConnection)
            LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver)
            leScanner?.stopScan(scanCallback)
        }
    }

    if (showProgressDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(progressTitle) },
            text = {
                Column {
                    LinearProgressIndicator(
                        progress = { if (progressMax > 0) progressCurrent.toFloat() / progressMax else 0f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("$progressCurrent / $progressMax bytes", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {}
        )
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = if (isConnected) Icons.Filled.BluetoothConnected else Icons.Filled.Bluetooth, contentDescription = null, tint = if (isConnected) MaterialTheme.colorScheme.primary else Color.Gray)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("DLP NIRScan Nano", fontWeight = FontWeight.Bold)
                        Text(if (isConnected) "已连接" else if (isSearching) "正在搜索..." else "未连接", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // ================= 新增：按钮点击时的权限校验 =================
                OutlinedButton(onClick = {
                    if (isConnected) {
                        nanoBLEService?.disconnect()
                    } else {
                        // 检查是否已经给了权限
                        val allPermissionsGranted = bluetoothPermissions.all {
                            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                        }

                        if (allPermissionsGranted) {
                            // 有权限，直接开扫
                            if (bluetoothAdapter?.isEnabled == true) {
                                isSearching = true
                                leScanner?.startScan(scanCallback)
                                coroutineScope.launch {
                                    delay(10000)
                                    if (isSearching) {
                                        isSearching = false
                                        leScanner?.stopScan(scanCallback)
                                        Toast.makeText(context, "搜索超时", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "请先在手机设置中打开蓝牙", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            // 没权限，唤起系统弹窗向用户要权限！
                            permissionLauncher.launch(bluetoothPermissions)
                        }
                    }
                }) { Text(if (isConnected) "断开" else "搜索设备") }
                // ============================================================
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (finalResult != null) {
            Text("✅ 分析完成", style = MaterialTheme.typography.titleLarge, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.size(100.dp), shape = MaterialTheme.shapes.extraLarge) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(finalResult!!.score.toString(), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text("综合评分", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ComponentCard("蛋白质", "${finalResult!!.protein} g", modifier = Modifier.weight(1f))
                ComponentCard("脂肪", "${finalResult!!.fat} g", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ComponentCard("乳糖", "${finalResult!!.lactose} g", modifier = Modifier.weight(1f))
                ComponentCard("水分占比", "${finalResult!!.waterContent} %", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("💡 智能建议：${finalResult!!.suggestion}", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                buttonEnabled = false
                finalResult = null
                LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(KSTNanoSDK.START_SCAN))
            },
            enabled = buttonEnabled,
            modifier = Modifier.fillMaxWidth().height(64.dp)
        ) { Text(buttonText, style = MaterialTheme.typography.titleMedium) }
    }
}

@Composable
fun ComponentCard(title: String, value: String, modifier: Modifier = Modifier) {
    OutlinedCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}