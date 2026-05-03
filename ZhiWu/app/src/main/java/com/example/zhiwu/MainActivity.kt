package com.example.zhiwu

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.localbroadcastmanager.content.LocalBroadcastManager

// 导入 UI 模块与页面
import com.example.zhiwu.ui.navigation.AppDestinations
import com.example.zhiwu.ui.screens.*
import com.example.zhiwu.ui.theme.ZhiWuTheme
import com.example.zhiwu.service.NanoBLEService
import com.kstechnologies.nirscannanolibrary.KSTNanoSDK

class MainActivity : ComponentActivity() {

    // 1. 初始化大脑 (AgentViewModel)
    private val agentViewModel = AgentViewModel()
    private var nanoBLEService: NanoBLEService? = null
    private var isConnected = false
    private var isScanningForDevice = false
    private var pendingScanAction = false // 核心标志：标识连接后是否立即扫描

    // 2. 权限清单 (适配 Android 12+)
    private val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // 3. 蓝牙搜索回调：自动匹配并连接设备
    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            if (device.name == "NIRScanNano") {
                Log.i("UbiAgent", "发现目标设备: ${device.address}，正在自动建立物理连接...")
                nanoBLEService?.connect(device.address)
                stopLeScan()
            }
        }
    }

    // 4. 核心执行逻辑：自动判断连接状态并触发扫描
    private fun autoConnectAndScan() {
        if (isConnected) {
            executePhysicalScan()
        } else {
            pendingScanAction = true // 标记：等连上后再扫
            checkPermissionAndStartScan()
        }
    }

    private fun executePhysicalScan() {
        Log.i("UbiAgent", "发送开始扫描广播...")
        val startScanIntent = Intent(KSTNanoSDK.START_SCAN)
        LocalBroadcastManager.getInstance(this).sendBroadcast(startScanIntent)
    }

    // 5. 底层 SDK 状态监听：处理连接、握手与数据回传
    private val mSdkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                KSTNanoSDK.ACTION_GATT_CONNECTED -> {
                    isConnected = true
                    agentViewModel.isDeviceConnected = true // 同步状态给 ViewModel
                }
                KSTNanoSDK.ACTION_GATT_DISCONNECTED -> {
                    isConnected = false
                    agentViewModel.isDeviceConnected = false
                    pendingScanAction = false
                }
                KSTNanoSDK.ACTION_NOTIFY_DONE -> {
                    Log.i("UbiAgent", "GATT 握手成功，SDK 就绪")
                    if (pendingScanAction) {

                        // 握手成功后，先同步时间再扫描
                        LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(KSTNanoSDK.SET_TIME))
                        agentViewModel.updateStatus("正在下载设备校准参数...")
                    }
                }
                KSTNanoSDK.ACTION_REQ_CAL_COEFF -> {
                    val size = intent.getIntExtra(KSTNanoSDK.EXTRA_REF_CAL_COEFF_SIZE, 0)
                    if (intent.getBooleanExtra(KSTNanoSDK.EXTRA_REF_CAL_COEFF_SIZE_PACKET, false)) {
                        agentViewModel.updateStatus("正在下载参考校准系数 (总大小: $size bytes)...")
                    }
                }

                // 补全：拦截校准矩阵并保存到全局变量，供 C++ 库使用
                KSTNanoSDK.REF_CONF_DATA -> {
                    val coeff = intent.getByteArrayExtra(KSTNanoSDK.EXTRA_REF_COEF_DATA)
                    val matrix = intent.getByteArrayExtra(KSTNanoSDK.EXTRA_REF_MATRIX_DATA)
                    if (coeff != null && matrix != null) {
                        // 存入全局变量，确保后续扫描能被解析
                        com.example.zhiwu.ui.screens.globalRefCoeff = coeff
                        com.example.zhiwu.ui.screens.globalRefMatrix = matrix

                        if (pendingScanAction) {
                            pendingScanAction = false // 任务达成，重置标志位
                            agentViewModel.updateStatus("✅ 校准已完成，正在启动物理扫描...")
                            executePhysicalScan()
                        }

                    }
                }

                KSTNanoSDK.SCAN_DATA -> {
                    val scanData = intent.getByteArrayExtra(KSTNanoSDK.EXTRA_DATA)
                    // 此时 globalRefMatrix 已有值，解析将不再报错
                    if (scanData != null) {
                        agentViewModel.onScanDataReceived(scanData)
                    }
                }
            }
        }
    }

    // 6. 绑定后台 Service
    private val mServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            nanoBLEService = (service as NanoBLEService.LocalBinder).service
            nanoBLEService?.initialize()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            nanoBLEService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 绑定 ViewModel 的回调接口 (神经突触)
        agentViewModel.onRequireHardwareScan = { executePhysicalScan() }
        agentViewModel.onRequireConnectionAndScan = { autoConnectAndScan() }

        // 绑定硬件服务
        bindService(Intent(this, NanoBLEService::class.java), mServiceConnection, Context.BIND_AUTO_CREATE)

        setContent {
            ZhiWuTheme {
                ZhiWuApp(agentViewModel) // 启动 UI 导航框架
            }
        }
    }

    // --- 权限与搜索辅助函数 (保留 AnalysisScreen 风格) ---
    private fun checkPermissionAndStartScan() {
        val hasPermissions = bluetoothPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        if (hasPermissions) startLeScan()
        else permissionLauncher.launch(bluetoothPermissions)
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grantedMap ->
        if (grantedMap.values.all { it }) startLeScan()
        else Toast.makeText(this, "需要蓝牙权限以自动连接设备", Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("MissingPermission")
    private fun startLeScan() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val scanner = bluetoothManager.adapter.bluetoothLeScanner
        if (!isScanningForDevice && scanner != null) {
            isScanningForDevice = true
            scanner.startScan(scanCallback)
            // 15秒搜索超时保护
            window.decorView.postDelayed({ stopLeScan() }, 15000)
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopLeScan() {
        if (isScanningForDevice) {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothManager.adapter.bluetoothLeScanner?.stopScan(scanCallback)
            isScanningForDevice = false
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction(KSTNanoSDK.ACTION_GATT_CONNECTED)
            addAction(KSTNanoSDK.ACTION_GATT_DISCONNECTED)
            addAction(KSTNanoSDK.ACTION_NOTIFY_DONE)
            addAction(KSTNanoSDK.ACTION_REQ_CAL_COEFF) // 必须加
            addAction(KSTNanoSDK.REF_CONF_DATA)        // 必须加
            addAction(KSTNanoSDK.SCAN_DATA)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(mSdkReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mSdkReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(mServiceConnection)
    }
}

// --- 老功能：UI 导航架构 ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZhiWuApp(agentViewModel: AgentViewModel = viewModel()) {
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
                AppDestinations.AGENT -> AgentScreen(viewModel = agentViewModel, modifier = Modifier.padding(innerPadding))
                AppDestinations.HISTORY -> HistoryScreen(modifier = Modifier.padding(innerPadding))
                AppDestinations.PRPFILE -> ProfileScreen(modifier = Modifier.padding(innerPadding))
            }
        }
    }
}