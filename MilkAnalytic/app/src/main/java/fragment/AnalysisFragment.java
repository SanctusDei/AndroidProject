package fragment;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.kstechnologies.nirscannanolibrary.KSTNanoSDK;
import com.kstechnologies.nirscannanolibrary.SettingsManager;
import com.ubi.NanoScan.NanoBLEService;
import com.ubi.NanoScan.R;
import com.ubi.NanoScan.databinding.FragmentAnalysisBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import Adapter.ComponentAdapter;
import utils.ComponentItem;

public class AnalysisFragment extends Fragment {

    private static final String TAG = "AnalysisFragment";
    private static final String DEVICE_NAME = "NIRScanNano";

    private FragmentAnalysisBinding binding;
    private Context mContext;
    private Handler mHandler;

    // --- 官方架构：蓝牙相关组件 ---
    private NanoBLEService mNanoBLEService;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private String preferredDevice;
    private boolean connected = false;

    // 进度条
    private ProgressDialog barProgressDialog;


    private final BroadcastReceiver scanDataReadyReceiver = new scanDataReadyReceiver();
    private final BroadcastReceiver refReadyReceiver = new refReadyReceiver();
    private final BroadcastReceiver notifyCompleteReceiver = new notifyCompleteReceiver();
    private final BroadcastReceiver scanStartedReceiver = new ScanStartedReceiver();
    private final BroadcastReceiver requestCalCoeffReceiver = new requestCalCoeffReceiver();
    private final BroadcastReceiver requestCalMatrixReceiver = new requestCalMatrixReceiver();
    private final BroadcastReceiver disconnReceiver = new DisconnReceiver();
    private final BroadcastReceiver scanConfReceiver = new ScanConfReceiver();

    // Volley 请求
    private RequestQueue mQueue;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SettingsManager.storeBooleanPref(requireContext(), SettingsManager.SharedPreferencesKeys.saveOS, false);
        SettingsManager.storeBooleanPref(requireContext(), SettingsManager.SharedPreferencesKeys.saveSD, false);
        SettingsManager.storeBooleanPref(requireContext(), SettingsManager.SharedPreferencesKeys.continuousScan, false);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAnalysisBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mContext = requireContext();
        mHandler = new Handler();


        if (getArguments() != null) {
            preferredDevice = getArguments().getString("device_address");
        }

        requestPermissions(new String[]{
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
        }, 1);

        initUI();


        Intent gattServiceIntent = new Intent(mContext, NanoBLEService.class);
        mContext.bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

    }

    private void initUI() {
        binding.rvComponents.setLayoutManager(new GridLayoutManager(mContext, 2));

        binding.btnStartScan.setEnabled(false); // 默认禁用，等 ScanConfReceiver 唤醒
        binding.btnStartScan.setOnClickListener(v -> {
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(KSTNanoSDK.START_SCAN));
        });

        binding.layoutDeviceStatus.btnConnectBle.setOnClickListener(v -> {
            if (mNanoBLEService != null) {

                if (preferredDevice != null) {
                    mNanoBLEService.connect(preferredDevice);
                    connected = true;
                } else {
                    scanLeDevice(true);
                }
            } else {
                Toast.makeText(mContext, "服务正在启动，请稍后", Toast.LENGTH_SHORT).show();
            }
        });
    }


    public class scanDataReadyReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {

            // 1.获取原始字节流
            byte[] scanData = intent.getByteArrayExtra(KSTNanoSDK.EXTRA_DATA);
            if (scanData == null) return;

            // 官方 JNI 解析
            if (!KSTNanoSDK.ReferenceCalibration.currentCalibration.isEmpty()) {
                KSTNanoSDK.ReferenceCalibration ref = KSTNanoSDK.ReferenceCalibration.currentCalibration.get(0);
                KSTNanoSDK.ScanResults results = KSTNanoSDK.KSTNanoSDK_dlpSpecScanInterpReference(
                        scanData, ref.getRefCalCoefficients(), ref.getRefCalMatrix()
                );

                Log.d(TAG, "官方 JNI 解析成功，波长数量: " + results.getLength());

                // TODO: 核心逻辑，将扫描获得的数据发送到django后端并获取分析的结果。

                startAnalysis(results);
            }
        }
    }

    /** 2. 参考校准数据已准备好 */
    public class refReadyReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "SDK内部拼接完成，返回完整的参考数据");
            byte[] refCoeff = intent.getByteArrayExtra(KSTNanoSDK.EXTRA_REF_COEF_DATA);
            byte[] refMatrix = intent.getByteArrayExtra(KSTNanoSDK.EXTRA_REF_MATRIX_DATA);

            ArrayList<KSTNanoSDK.ReferenceCalibration> refCal = new ArrayList<>();
            refCal.add(new KSTNanoSDK.ReferenceCalibration(refCoeff, refMatrix));
            KSTNanoSDK.ReferenceCalibration.writeRefCalFile(mContext, refCal);

            if (barProgressDialog != null && barProgressDialog.isShowing()) {
                barProgressDialog.dismiss();
            }
        }
    }

    /** 3. 扫描已启动 */
    public class ScanStartedReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {

            binding.btnStartScan.setText(getString(R.string.scanning));
        }
    }

    /** 4. 通知配置已完成 -> 触发时间同步 */
    public class notifyCompleteReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "GATT通知已全部订阅，发送 SET_TIME");
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(KSTNanoSDK.SET_TIME));
        }
    }

    /** 5. 请求校准系数 (加入了防止 Android 12+ 崩溃的修复) */
    public class requestCalCoeffReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int sizeVal = getSafeInt(intent, KSTNanoSDK.EXTRA_REF_CAL_COEFF_SIZE, 0);
            boolean isSizePacket = isFirstPacket(intent, KSTNanoSDK.EXTRA_REF_CAL_COEFF_SIZE_PACKET);

            if (isSizePacket) {

                if (barProgressDialog != null) barProgressDialog.dismiss();

                barProgressDialog = new ProgressDialog(mContext);
                barProgressDialog.setTitle(getString(R.string.dl_ref_cal));
                barProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                barProgressDialog.setProgress(0);
                barProgressDialog.setMax(sizeVal);
                barProgressDialog.setCancelable(false);
                barProgressDialog.show();
            } else {
                if (barProgressDialog != null) {
                    barProgressDialog.setProgress(barProgressDialog.getProgress() + sizeVal);
                }
            }
        }
    }

    /** 6. 请求校准矩阵 (同上包含修复) */
    public class requestCalMatrixReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int sizeVal = getSafeInt(intent, KSTNanoSDK.EXTRA_REF_CAL_MATRIX_SIZE, 0);
            boolean isSizePacket = isFirstPacket(intent, KSTNanoSDK.EXTRA_REF_CAL_MATRIX_SIZE_PACKET);

            if (isSizePacket) {
                if (barProgressDialog != null) barProgressDialog.dismiss();

                barProgressDialog = new ProgressDialog(mContext);
                barProgressDialog.setTitle(getString(R.string.dl_cal_matrix));
                barProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                barProgressDialog.setProgress(0);
                barProgressDialog.setMax(sizeVal);
                barProgressDialog.setCancelable(false);
                barProgressDialog.show();

            } else {
                if (barProgressDialog != null) {
                    barProgressDialog.setProgress(barProgressDialog.getProgress() + sizeVal);

                    if (barProgressDialog.getProgress() == barProgressDialog.getMax()) {
                        Log.d(TAG, "矩阵同步满格，触发 REQUEST_ACTIVE_CONF");
                        LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(KSTNanoSDK.REQUEST_ACTIVE_CONF));
                    }
                }
            }
        }
    }

    /** 7. 收到扫描配置 (最后一步：激活按钮) */
    private class ScanConfReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            byte[] confData = intent.getByteArrayExtra(KSTNanoSDK.EXTRA_DATA);
            if (confData != null) {
                KSTNanoSDK.ScanConfiguration scanConf = KSTNanoSDK.KSTNanoSDK_dlpSpecScanReadConfiguration(confData);
                SettingsManager.storeStringPref(mContext, SettingsManager.SharedPreferencesKeys.scanConfiguration, scanConf.getConfigName());
            }

            if (barProgressDialog != null) barProgressDialog.dismiss();

            // 在这里彻底唤醒按钮
            binding.btnStartScan.setEnabled(true);
            binding.btnStartScan.setClickable(true);
            binding.tvHint.setText(getString(R.string.analysis_hint_connected));
            binding.btnStartScan.setBackgroundColor(ContextCompat.getColor(mContext, R.color.kst_red));
            binding.layoutDeviceStatus.btnConnectBle.setBackgroundColor(ContextCompat.getColor(mContext,R.color.kst_red));
            binding.layoutDeviceStatus.btnConnectBle.setText(getString(R.string.analysis_ble_connected));

            Log.d(TAG, "设备全链路初始化完毕，按钮已激活！");
        }
    }

    /** 8. 断开连接 */
    public class DisconnReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(mContext, R.string.nano_disconnected, Toast.LENGTH_SHORT).show();
            binding.btnStartScan.setEnabled(false);
            binding.layoutDeviceStatus.btnConnectBle.setText(R.string.analysis_ble_click2connect);
            connected = false;
        }
    }



    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mNanoBLEService = ((NanoBLEService.LocalBinder) service).getService();
            if (!mNanoBLEService.initialize()) {
                Log.e(TAG, "NanoBLEService 初始化失败");
                return;
            }

            BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

              // 自动触发蓝牙搜索
//            if (preferredDevice != null) {
//                scanPreferredLeDevice(true);
//            } else {
//                scanLeDevice(true);
//            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mNanoBLEService = null;
        }
    };
    @SuppressLint("Missingpermission")
    private void scanLeDevice(final boolean enable) {
        if (enable && mBluetoothLeScanner != null) {
            binding.layoutDeviceStatus.btnConnectBle.setText(R.string.analysis_ble_searching);
            mHandler.postDelayed(() -> {
                mBluetoothLeScanner.stopScan(mLeScanCallback);
                if (!connected) {
                    binding.layoutDeviceStatus.btnConnectBle.setText(R.string.analysis_ble_click2connect);
                    notConnectedDialog();
                };
            }, NanoBLEService.SCAN_PERIOD);
            mBluetoothLeScanner.startScan(mLeScanCallback);
        } else if (mBluetoothLeScanner != null) {
            mBluetoothLeScanner.stopScan(mLeScanCallback);
        }
    }
    @SuppressLint("Missingpermission")
    private void scanPreferredLeDevice(final boolean enable) {
        if (enable && mBluetoothLeScanner != null) {
            binding.layoutDeviceStatus.btnConnectBle.setText(R.string.analysis_ble_prefer_searching);
            mHandler.postDelayed(() -> {
                mBluetoothLeScanner.stopScan(mPreferredLeScanCallback);
                if (!connected) scanLeDevice(true);
            }, NanoBLEService.SCAN_PERIOD);
            mBluetoothLeScanner.startScan(mPreferredLeScanCallback);
        } else if (mBluetoothLeScanner != null) {
            mBluetoothLeScanner.stopScan(mPreferredLeScanCallback);
        }
    }
    @SuppressLint("Missingpermission")
    private final ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (device.getName() != null && device.getName().equals(DEVICE_NAME)) {
                mNanoBLEService.connect(device.getAddress());
                connected = true;
                scanLeDevice(false);
            }
        }
    };
    @SuppressLint("Missingpermission")
    private final ScanCallback mPreferredLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (device.getName() != null && device.getName().equals(DEVICE_NAME) && device.getAddress().equals(preferredDevice)) {
                mNanoBLEService.connect(device.getAddress());
                connected = true;
                scanPreferredLeDevice(false);
            }
        }
    };

    private void notConnectedDialog() {
        new AlertDialog.Builder(mContext)
                .setTitle(R.string.not_connected_title)
                .setCancelable(false)
                .setMessage(R.string.not_connected_message)
                .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                .show();
    }

    // ================== 生命周期管理 ==================

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(mContext);
        lbm.registerReceiver(scanDataReadyReceiver, new IntentFilter(KSTNanoSDK.SCAN_DATA));
        lbm.registerReceiver(refReadyReceiver, new IntentFilter(KSTNanoSDK.REF_CONF_DATA));
        lbm.registerReceiver(notifyCompleteReceiver, new IntentFilter(KSTNanoSDK.ACTION_NOTIFY_DONE));
        lbm.registerReceiver(requestCalCoeffReceiver, new IntentFilter(KSTNanoSDK.ACTION_REQ_CAL_COEFF));
        lbm.registerReceiver(requestCalMatrixReceiver, new IntentFilter(KSTNanoSDK.ACTION_REQ_CAL_MATRIX));
        lbm.registerReceiver(disconnReceiver, new IntentFilter(KSTNanoSDK.ACTION_GATT_DISCONNECTED));
        lbm.registerReceiver(scanConfReceiver, new IntentFilter(KSTNanoSDK.SCAN_CONF_DATA));
        lbm.registerReceiver(scanStartedReceiver, new IntentFilter(NanoBLEService.ACTION_SCAN_STARTED));
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(mContext);
        lbm.unregisterReceiver(scanDataReadyReceiver);
        lbm.unregisterReceiver(refReadyReceiver);
        lbm.unregisterReceiver(notifyCompleteReceiver);
        lbm.unregisterReceiver(requestCalCoeffReceiver);
        lbm.unregisterReceiver(requestCalMatrixReceiver);
        lbm.unregisterReceiver(disconnReceiver);
        lbm.unregisterReceiver(scanConfReceiver);
        lbm.unregisterReceiver(scanStartedReceiver);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mContext.unbindService(mServiceConnection);
        mHandler.removeCallbacksAndMessages(null);
        binding = null;
    }


    private int getSafeInt(Intent intent, String key, int defaultValue) {
        Bundle bundle = intent.getExtras();
        if (bundle == null) return defaultValue;
        Object val = bundle.get(key);
        return (val instanceof Integer) ? (Integer) val : defaultValue;
    }

    private boolean isFirstPacket(Intent intent, String key) {
        Bundle bundle = intent.getExtras();
        if (bundle == null) return false;
        Object val = bundle.get(key);
        return val instanceof Boolean && (Boolean) val;
    }


    private void startAnalysis(KSTNanoSDK.ScanResults results) {
        if(binding == null || results == null) return;

        binding.btnStartScan.setEnabled(false);
        binding.btnStartScan.setText(R.string.analysis_scan_analysising);

        JSONObject jsonResult = new JSONObject();
        try {
            // 关键数据：波长、原始光强、参考光强
            jsonResult.put("wavelength", new JSONArray(results.getWavelength()));
            jsonResult.put("intensity", new JSONArray(results.getUncalibratedIntensity()));
            jsonResult.put("reference", new JSONArray(results.getIntensity()));

            // 可选：添加设备 MAC 或标签
            jsonResult.put("device_mac", preferredDevice != null ? preferredDevice : "NIRScanNano");
            jsonResult.put("label", "Milk_Sample");

        } catch (JSONException e) {
            Log.e("Volley_JSON", "JSON构建失败: " + e.toString());
            resetScanButton();
            return;
        }

        JsonObjectRequest request = getJsonObjectRequest(jsonResult);

        // 5. 加入请求队列 (mQueue 需在 onViewCreated 初始化)
        if (mQueue == null) mQueue = Volley.newRequestQueue(mContext);
        mQueue.add(request);

    }

    @NonNull
    private JsonObjectRequest getJsonObjectRequest(JSONObject jsonResult) {
        String url = "http://172.22.98.184:18000/api/analysis/predict/";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                jsonResult,
                response -> {
                    // --- 请求成功：解析后端预测的结果 ---
                    try {
                        double protein = response.optDouble("protein", 0.0);
                        double fat = response.optDouble("fat", 0.0);

                        // --- 新增：解析乳糖和水分指标 ---
                        double lactose = response.optDouble("lactose", 0.0);
                        double waterContent = response.optDouble("water_content", 0.0);

                        // 2. 解析评分与建议
                        int score = response.optInt("score", 0);
                        String suggestion = response.optString("suggestion", "分析完成");

                        // 3. 更新 UI (增加参数传递)
                        // 注意：你需要同步修改 showResultsUI 方法的定义
                        showResultsUI(protein, fat, lactose, waterContent, score, suggestion);
                    } catch (Exception e) {
                        Log.e("Volley_Response", "解析响应失败");
                        resetScanButton();
                    }
                },

                error -> {
                    // --- 请求失败 ---
                    Toast.makeText(mContext, "服务器连接失败，请检查后端状态", Toast.LENGTH_SHORT).show();
                    Log.e("Volley_Error", error.toString());
                    resetScanButton();
                }
        );
        return request;
    }

    // TODO:展示结果的UI
    // 增加 lactose 和 waterContent 参数
    private void showResultsUI(double protein, double fat, double lactose, double waterContent, int score, String suggestion) {
        if (binding == null) return;

        // 显示结果布局
        binding.layoutResults.setVisibility(View.VISIBLE);

        // 1. 设置质量分数和进度条动画 (增加颜色联动)
        binding.qualityMeterLayout.tvQualityScore.setText(String.valueOf(score));

        // 根据得分改变进度条颜色（可选：及格绿，不及格红）
        int progressColor = score >= 80 ? Color.GREEN : (score >= 60 ? Color.YELLOW : Color.RED);
        binding.qualityMeterLayout.qualityProgressBar.setProgressTintList(ColorStateList.valueOf(progressColor));

        ObjectAnimator.ofInt(binding.qualityMeterLayout.qualityProgressBar, "progress", 0, score)
                .setDuration(1000)
                .start();

        // 2. 填充成分列表 (新增乳糖、水分占比)
        List<ComponentItem> items = new ArrayList<>();
        items.add(new ComponentItem("蛋白质", String.format(Locale.US, "%.2f", protein), "g/100ml"));
        items.add(new ComponentItem("脂肪", String.format(Locale.US, "%.2f", fat), "g/100ml"));

        // --- 新增项 ---
        items.add(new ComponentItem("乳糖", String.format(Locale.US, "%.2f", lactose), "g/100ml"));
        items.add(new ComponentItem("水分占比", String.format(Locale.US, "%.1f", waterContent), "%"));

        // 如果你还想显示绝对水分克数，可以再加一项
        // items.add(new ComponentItem("含水量", String.format(Locale.US, "%.1f", waterContent), "g"));

        binding.rvComponents.setAdapter(new ComponentAdapter(items));

        // 3. 动态添加建议文本 (美化 UI)
        binding.containerSuggestions.removeAllViews();
        TextView tvSuggestion = new TextView(mContext);
        tvSuggestion.setText("💡 AI 分析建议：\n" + suggestion);
        tvSuggestion.setLineSpacing(0, 1.2f); // 增加行间距，更好看
        tvSuggestion.setPadding(10, 10, 10, 10);
        tvSuggestion.setTextColor(ContextCompat.getColor(mContext, R.color.black));
        binding.containerSuggestions.addView(tvSuggestion);

        updateRadarChart(protein, fat, lactose, waterContent, score);
        // 4. 恢复扫描按钮
        resetScanButton();
    }


    private void updateRadarChart(double protein, double fat, double lactose, double waterContent, int score) {
        if (binding == null) return;

        // 1. 数据归一化处理 (雷达图各轴量程需一致，建议统一转为 0-100 的评分)
        // 逻辑：蛋白3.2、脂肪3.6、乳糖4.5 左右为满分 100
        float pScore = (float) Math.min(100, (protein / 3.5) * 100);
        float fScore = (float) Math.min(100, (fat / 4.0) * 100);
        float lScore = (float) Math.min(100, (lactose / 5.0) * 100);
        float wScore = (float) Math.max(0, 100 - (Math.abs(waterContent - 88.0) * 10)); // 偏离88%越多分越低
        float totalScore = (float) score;

        ArrayList<com.github.mikephil.charting.data.RadarEntry> entries = new ArrayList<>();
        entries.add(new com.github.mikephil.charting.data.RadarEntry(pScore));
        entries.add(new com.github.mikephil.charting.data.RadarEntry(fScore));
        entries.add(new com.github.mikephil.charting.data.RadarEntry(lScore));
        entries.add(new com.github.mikephil.charting.data.RadarEntry(wScore));
        entries.add(new com.github.mikephil.charting.data.RadarEntry(totalScore));

        com.github.mikephil.charting.data.RadarDataSet dataSet = new com.github.mikephil.charting.data.RadarDataSet(entries, "指标评分");

        // 2. 样式美化 (使用你项目里的 kst_red 或品牌蓝)
        int themeColor = ContextCompat.getColor(mContext, R.color.kst_red);
        dataSet.setColor(themeColor); // 线条颜色
        dataSet.setFillColor(themeColor); // 填充颜色
        dataSet.setDrawFilled(true); // 开启填充
        dataSet.setFillAlpha(100); // 透明度
        dataSet.setLineWidth(2f);
        dataSet.setDrawHighlightCircleEnabled(true);

        com.github.mikephil.charting.data.RadarData data = new com.github.mikephil.charting.data.RadarData(dataSet);
        data.setValueTextSize(8);
        data.setDrawValues(false); // 不在点上显示具体数值，显得干净

        // 3. 配置 Chart 坐标轴与标签
        binding.radarChart.setData(data);
        binding.radarChart.getDescription().setEnabled(false); // 隐藏右下角描述

        // 自定义 X 轴标签
        String[] labels = {"蛋白质", "脂肪", "乳糖", "纯净度", "综合"};
        binding.radarChart.getXAxis().setValueFormatter(new com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels));
        binding.radarChart.getXAxis().setTextColor(Color.BLACK);

        // Y 轴（蜘蛛网的层级）
        binding.radarChart.getYAxis().setAxisMinimum(0f);
        binding.radarChart.getYAxis().setAxisMaximum(100f); // 固定量程 0-100
        binding.radarChart.getYAxis().setLabelCount(5, false);
        binding.radarChart.getYAxis().setDrawLabels(false); // 隐藏内部数字

        binding.radarChart.animateXY(1000, 1000); // 炫酷的展开动画
        binding.radarChart.invalidate(); // 刷新
    }
    // TODO:重置按键状态
    private void resetScanButton() {
        if (binding != null) {

            binding.btnStartScan.setEnabled(true);
            binding.btnStartScan.setText(getString(R.string.analysis_scan));

        }
    }
}