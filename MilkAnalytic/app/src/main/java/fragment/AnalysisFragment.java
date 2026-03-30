package fragment;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
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
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;

import com.github.mikephil.charting.data.Entry;
import com.kstechnologies.nirscannanolibrary.KSTNanoSDK;
import com.kstechnologies.nirscannanolibrary.SettingsManager;

import com.ubi.NanoScan.NanoBLEService;
import com.ubi.NanoScan.R;
import com.ubi.NanoScan.databinding.FragmentAnalysisBinding;
import com.github.mikephil.charting.data.RadarData;
import com.github.mikephil.charting.data.RadarDataSet;
import com.github.mikephil.charting.data.RadarEntry;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.Provider;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import utils.ComponentAdapter;
import utils.ComponentItem;


public class AnalysisFragment extends Fragment {
    private FragmentAnalysisBinding binding;

    // 用于接受NIRScan扫描的数据
    private ArrayList<Entry> mIntensityFloat;
    private ArrayList<Entry> mAbsorbanceFloat;
    private ArrayList<Entry> mReflectanceFloat;
    private ArrayList<Float> mWavelengthFloat;
    private ArrayList<String> mXValues;


    // BLE 连接
    private NanoBLEService mBLEService;
    private String mDeviceAddress;
    private Context mContext;


    private KSTNanoSDK.ScanResults results;
    private final BroadcastReceiver scanDataReadyReceiver = new scanDataReadyReceiver();
    private final BroadcastReceiver refReadyReceiver = new refReadyReceiver();
    private final BroadcastReceiver notifyCompleteReceiver = new notifyCompleteReceiver();
    private final BroadcastReceiver scanStartedReceiver = new ScanStartedReceiver();
    private final BroadcastReceiver requestCalCoeffReceiver = new requestCalCoeffReceiver();
    private final BroadcastReceiver requestCalMatrixReceiver = new requestCalMatrixReceiver();
    private final BroadcastReceiver disconnReceiver = new DisconnReceiver();

    private final IntentFilter scanDataReadyFilter = new IntentFilter(KSTNanoSDK.SCAN_DATA);
    private final IntentFilter refReadyFilter = new IntentFilter(KSTNanoSDK.REF_CONF_DATA);
    private final IntentFilter notifyCompleteFilter = new IntentFilter(KSTNanoSDK.ACTION_NOTIFY_DONE);
    private final IntentFilter requestCalCoeffFilter = new IntentFilter(KSTNanoSDK.ACTION_REQ_CAL_COEFF);
    private final IntentFilter requestCalMatrixFilter = new IntentFilter(KSTNanoSDK.ACTION_REQ_CAL_MATRIX);
    private final IntentFilter disconnFilter = new IntentFilter(KSTNanoSDK.ACTION_GATT_DISCONNECTED);
    private final IntentFilter scanStartedFilter = new IntentFilter(NanoBLEService.ACTION_SCAN_STARTED);

    private final BroadcastReceiver scanConfReceiver = new ScanConfReceiver();
    private final IntentFilter scanConfFilter = new IntentFilter(KSTNanoSDK.SCAN_CONF_DATA);


    // 扫描蓝牙设备
    private BluetoothLeScanner mBluetoothLeScanner;
    private boolean mScanning = false;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private static final long SCAN_PERIOD = 10000; // 扫描持续10秒

    // 监听蓝牙真实连接状态的广播
    // 新增：专门处理蓝牙连接状态的核心 Receiver
    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (KSTNanoSDK.ACTION_GATT_CONNECTED.equals(action)) {
                // 连接成功，更新 UI
                updateDeviceStatus(true); // 切换按钮为“开始扫描”
                binding.layoutDeviceStatus.btnConnectBle.setText("已连接");
                binding.layoutDeviceStatus.btnConnectBle.setEnabled(false);

                // 重要：保存这个成功的 MAC 地址，下次进来就不用再扫了
                SettingsManager.storeStringPref(requireContext(),
                        SettingsManager.SharedPreferencesKeys.preferredDevice, mDeviceAddress);

                // 自动同步配置：根据 SDK 要求，必须先同步配置才能开始物理扫描
                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(KSTNanoSDK.GET_SCAN_CONF));
                Toast.makeText(context, "光谱仪已就绪", Toast.LENGTH_SHORT).show();
            }
            else if (KSTNanoSDK.ACTION_GATT_DISCONNECTED.equals(action)) {
                updateDeviceStatus(false);
                binding.layoutDeviceStatus.btnConnectBle.setText("连接断开");
                binding.layoutDeviceStatus.btnConnectBle.setEnabled(true);
                Toast.makeText(context, "光谱仪连接已断开", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private final ServiceConnection mServiceConection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mBLEService = ((NanoBLEService.LocalBinder) iBinder).getService();
            if (mBLEService != null) {
                if (!mBLEService.initialize()) {
                    Log.e("AnalysisFragment", "无法初始化蓝牙底层适配器");
                    Toast.makeText(requireContext(), "蓝牙初始化失败，请检查开关", Toast.LENGTH_SHORT).show();
                } else {
                    Log.d("AnalysisFragment", "蓝牙服务绑定并初始化成功");
                    // 如果已经有地址，尝试静默连接
                    if (mDeviceAddress != null) {
                        mBLEService.connect(mDeviceAddress);
                    }
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBLEService = null;
        }
    };

    // 定义接收器

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // 初始化存储位置
        SettingsManager.storeBooleanPref(requireContext(),SettingsManager.SharedPreferencesKeys.saveOS,false);
        SettingsManager.storeBooleanPref(requireContext(),SettingsManager.SharedPreferencesKeys.saveSD,false);
        SettingsManager.storeBooleanPref(requireContext(),SettingsManager.SharedPreferencesKeys.continuousScan,false);

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAnalysisBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. 获取从上一个页面传来的设备 MAC 地址
        if (getArguments() != null) {
            mDeviceAddress = getArguments().getString("device_address");
        }

        binding.rvComponents.setLayoutManager(new GridLayoutManager(getContext(), 2));
        updateDeviceStatus(false);
        binding.btnStartScan.setOnClickListener(v -> startAnalysisLogic());
        binding.etProductionDate.setOnClickListener(v-> showDatePicker());

        // 绑定蓝牙连接按钮
        binding.layoutDeviceStatus.btnConnectBle.setOnClickListener(v -> bleConnected());
    }

    private void bleConnected() {
        // 1. 如果没有地址，尝试从本地缓存读取
        if (mDeviceAddress == null) {
            mDeviceAddress = SettingsManager.getStringPref(requireContext(),
                    SettingsManager.SharedPreferencesKeys.preferredDevice, null);
        }

        // 2. 如果依然没有地址，说明是第一次使用，启动自动扫描
        if (mDeviceAddress == null) {
            startAutoConnect();
            return;
        }

        // 3. 已经有地址了，检查服务并连接
        if (mBLEService != null) {
            mBLEService.connect(mDeviceAddress);
            binding.layoutDeviceStatus.btnConnectBle.setText("连接中...");
            binding.layoutDeviceStatus.btnConnectBle.setEnabled(false);
        } else {
            // 如果服务还没绑定好，尝试重新绑定
            Intent gattServiceIntent = new Intent(requireContext(), NanoBLEService.class);
            requireContext().bindService(gattServiceIntent, mServiceConection, Context.BIND_AUTO_CREATE);
            Toast.makeText(requireContext(), "正在初始化蓝牙服务，请稍后重试", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 注册广播
        registerReceivers();

        mXValues = new ArrayList<>();
        // 存放光强数据
        mIntensityFloat = new ArrayList<>();
        // 存放吸光率数据
        mAbsorbanceFloat = new ArrayList<>();
        // 存放放射率数据
        mReflectanceFloat = new ArrayList<>();
        // 存放波长数据
        mWavelengthFloat = new ArrayList<>();

        registerReceivers();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onPause() {
        super.onPause();
        // 【必须】注销所有注册的广播，防止内存泄漏和重复回调崩溃
        if (mScanning && mBluetoothLeScanner != null) {
            mBluetoothLeScanner.stopScan(mLeScanCallback);
        }
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(gattUpdateReceiver);
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(requireContext());
        lbm.unregisterReceiver(scanDataReadyReceiver);
        lbm.unregisterReceiver(refReadyReceiver);
        lbm.unregisterReceiver(notifyCompleteReceiver);
        lbm.unregisterReceiver(requestCalCoeffReceiver);
        lbm.unregisterReceiver(requestCalMatrixReceiver);
        lbm.unregisterReceiver(disconnReceiver);
        lbm.unregisterReceiver(scanConfReceiver);
        lbm.unregisterReceiver(scanStartedReceiver);
        lbm.unregisterReceiver(gattUpdateReceiver); // 注销新增的连接监听
    }

    @Override
    public void onStart() {
        super.onStart();
        // Ble 绑定服务
        mContext = requireContext();
        Intent gattServiceIntent = new Intent(mContext, NanoBLEService.class);
        mContext.bindService(gattServiceIntent, mServiceConection, Context.BIND_ABOVE_CLIENT);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mBLEService != null) {
            mContext.unbindService(mServiceConection);
        }
    }

    private void showDatePicker() {
        // 1. 设置日期约束：不允许选择未来的日期
        CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder();
        constraintsBuilder.setValidator(DateValidatorPointBackward.now());

        // 2. 创建 Builder
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("选择生产日期")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .setCalendarConstraints(constraintsBuilder.build())
                .setTheme(R.style.Widget_App_DatePicker) // 明确指定样式
                .build();

        // 3. 设置淡入淡出动效（选填，MaterialDatePicker 默认已有极佳动画）
        datePicker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
            // 修正时区偏移
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.setTimeInMillis(selection);
            binding.etProductionDate.setText(sdf.format(calendar.getTime()));
        });

        datePicker.show(getParentFragmentManager(), "DATE_PICKER");
    }

    private void updateDeviceStatus(boolean connected) {
        if (connected) {
            binding.btnStartScan.setText("开始扫描");
            binding.btnStartScan.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_play));
        } else {
            binding.btnStartScan.setText("模拟扫描");
            binding.btnStartScan.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_sparkles));
        }
    }

    private void startAnalysisLogic() {
        binding.btnStartScan.setEnabled(false);
        binding.btnStartScan.setText("分析中...");

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (binding == null) return;

            float p = 3.2f + (float) (Math.random() * 0.4 - 0.2);
            float f = 3.8f + (float) (Math.random() * 0.4 - 0.2);
            float l = 4.7f + (float) (Math.random() * 0.3 - 0.15);
            float c = 120f + (float) (Math.random() * 10 - 5);
            int score = (int) (85 + Math.random() * 15);

            updateUI(score, p, f, l, c);

            binding.btnStartScan.setEnabled(true);
            updateDeviceStatus(false);
            Toast.makeText(getContext(), "分析完成！", Toast.LENGTH_SHORT).show();
        }, 2000);
    }

    private void updateUI(int score, float p, float f, float l, float c) {
        binding.layoutResults.setVisibility(View.VISIBLE);

        // 1. 进度条动画
        binding.qualityMeterLayout.tvQualityScore.setText(String.valueOf(score));
        ObjectAnimator animator = ObjectAnimator.ofInt(binding.qualityMeterLayout.qualityProgressBar, "progress", 0, score);
        animator.setDuration(1000);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();

        // 2. 列表数据
        List<ComponentItem> items = new ArrayList<>();
        items.add(new ComponentItem("蛋白质", String.format(Locale.US, "%.2f", p), "g/100ml"));
        items.add(new ComponentItem("脂肪", String.format(Locale.US, "%.2f", f), "g/100ml"));
        items.add(new ComponentItem("乳糖", String.format(Locale.US, "%.2f", l), "g/100ml"));
        items.add(new ComponentItem("钙", String.format(Locale.US, "%.0f", c), "mg/100ml"));
        binding.rvComponents.setAdapter(new ComponentAdapter(items));

        // 3. 雷达图渲染
        updateRadarChart(p, f, l);

        // 4. 动态建议
        updateSuggestions(p, f, c);
    }

    private void updateRadarChart(float p, float f, float l) {
        List<RadarEntry> entries = new ArrayList<>();
        entries.add(new RadarEntry(p * 20f));
        entries.add(new RadarEntry(f * 20f));
        entries.add(new RadarEntry(l * 15f));

        RadarDataSet dataSet = new RadarDataSet(entries, "成分分布");
        dataSet.setColor(Color.parseColor("#2563EB"));
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(120);

        binding.radarChart.setData(new RadarData(dataSet));
        binding.radarChart.getDescription().setEnabled(false);
        binding.radarChart.invalidate();
    }

    private void updateSuggestions(float protein, float fat, float calcium) {
        binding.containerSuggestions.removeAllViews();
        if (protein > 3.0) addSuggestionView("蛋白质含量优秀", "符合优质牛奶标准", "#F0FDF4", "#059669");
        if (fat > 3.5) addSuggestionView("脂肪含量正常", "在全脂牛奶标准范围内", "#F0FDF4", "#059669");
        if (calcium > 100) addSuggestionView("钙含量丰富", "有助于骨骼健康", "#EFF6FF", "#2563EB");
    }

    private void addSuggestionView(String title, String content, String bgColor, String textColor) {
        View v = getLayoutInflater().inflate(R.layout.item_suggestion, binding.containerSuggestions, false);
        v.getBackground().setTint(Color.parseColor(bgColor));
        ((TextView) v.findViewById(R.id.tv_suggest_title)).setText(title);
        ((TextView) v.findViewById(R.id.tv_suggest_title)).setTextColor(Color.parseColor(textColor));
        ((TextView) v.findViewById(R.id.tv_suggest_content)).setText(content);
        binding.containerSuggestions.addView(v);
    }

    private void startSpectrumScan() {
        if(mBLEService != null) {
            Intent intent = new Intent(KSTNanoSDK.GET_SCAN_CONF);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
        }


    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void registerReceivers() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(requireContext());
        // ... 保持你原有的其他注册不变 ...
        lbm.registerReceiver(scanDataReadyReceiver, scanDataReadyFilter);
        lbm.registerReceiver(refReadyReceiver, refReadyFilter);
        lbm.registerReceiver(notifyCompleteReceiver, notifyCompleteFilter);
        lbm.registerReceiver(requestCalCoeffReceiver, requestCalCoeffFilter);
        lbm.registerReceiver(requestCalMatrixReceiver, requestCalMatrixFilter);
        lbm.registerReceiver(disconnReceiver, disconnFilter);
        lbm.registerReceiver(scanConfReceiver, scanConfFilter);
        lbm.registerReceiver(scanStartedReceiver, scanStartedFilter);

        // 【新增】注册连接状态的过滤器
        IntentFilter gattFilter = new IntentFilter();
        gattFilter.addAction(KSTNanoSDK.ACTION_GATT_CONNECTED);
        gattFilter.addAction(KSTNanoSDK.ACTION_GATT_DISCONNECTED);
        gattFilter.addAction(KSTNanoSDK.ACTION_GATT_SERVICES_DISCOVERED);
        lbm.registerReceiver(gattUpdateReceiver, gattFilter);
    }

    public class scanDataReadyReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {

            // 数据获取于解码
            // 从Intent中获取数据
            // 使用KSTNanoSDK提供的函数进行解码
            byte[] scanData = intent.getByteArrayExtra(KSTNanoSDK.EXTRA_DATA);

            String scanType = intent.getStringExtra(KSTNanoSDK.EXTRA_SCAN_TYPE);
            /*
             * 7 bytes representing the current data
             * byte0: uint8_t     year; //< years since 2000
             * byte1: uint8_t     month; /**< months since January [0-11]
             * byte2: uint8_t     day; /**< day of the month [1-31]
             * byte3: uint8_t     day_of_week; /**< days since Sunday [0-6]
             * byte3: uint8_t     hour; /**< hours since midnight [0-23]
             * byte5: uint8_t     minute; //< minutes after the hour [0-59]
             * byte6: uint8_t     second; //< seconds after the minute [0-60]
             */
            String scanDate = intent.getStringExtra(KSTNanoSDK.EXTRA_SCAN_DATE);
            // 获取校准参考
            KSTNanoSDK.ReferenceCalibration ref = KSTNanoSDK.ReferenceCalibration.currentCalibration.get(0);
            // 执行光谱插值计算 scanData(原始扫描字节流) -> refCalCoefficients(校准系数，用于波长修正) -> refCalMatrix(校准矩阵，用于修正光谱中的强度偏差) -> results(包含波长，反射率和吸光度的结构化数据)
            results = KSTNanoSDK.KSTNanoSDK_dlpSpecScanInterpReference(scanData, ref.getRefCalCoefficients(), ref.getRefCalMatrix());
            // 清空旧数据
            mXValues.clear();
            mIntensityFloat.clear();
            mAbsorbanceFloat.clear();
            mReflectanceFloat.clear();
            mWavelengthFloat.clear();

            int index;
            // 数据计算与转换
            for (index = 0; index < results.getLength(); index++) {
                // X坐标->波长
                mXValues.add(String.format("%.02f", KSTNanoSDK.ScanResults.getSpatialFreq(mContext, results.getWavelength()[index])));
                // 原始光强
                mIntensityFloat.add(new Entry((float) results.getUncalibratedIntensity()[index], index));
                // 吸光率
                mAbsorbanceFloat.add(new Entry((-1) * (float) Math.log10((double) results.getUncalibratedIntensity()[index] / (double) results.getIntensity()[index]), index));
                // 反射率
                mReflectanceFloat.add(new Entry((float) results.getUncalibratedIntensity()[index] / results.getIntensity()[index], index));
                // 波长数值
                mWavelengthFloat.add((float) results.getWavelength()[index]);
            }
            /**
             *
             *
             * 找出了(波长、吸光率、反射度)的边界值
             * 方便数据归一化和UI统计显示
             *
             * */
            float minWavelength = mWavelengthFloat.get(0);
            float maxWavelength = mWavelengthFloat.get(0);

            for (Float f : mWavelengthFloat) {
                if (f < minWavelength) minWavelength = f;
                if (f > maxWavelength) maxWavelength = f;
            }

            float minAbsorbance = mAbsorbanceFloat.get(0).getY();
            float maxAbsorbance = mAbsorbanceFloat.get(0).getY();

            for (Entry e : mAbsorbanceFloat) {
                if (e.getY() < minAbsorbance) minAbsorbance = e.getY();
                if (e.getY() > maxAbsorbance) maxAbsorbance = e.getY();
            }

            float minReflectance = mReflectanceFloat.get(0).getY();
            float maxReflectance = mReflectanceFloat.get(0).getY();

            for (Entry e : mReflectanceFloat) {
                if (e.getY() < minReflectance) minReflectance = e.getY();
                if (e.getY() > maxReflectance) maxReflectance = e.getY();
            }

            float minIntensity = mIntensityFloat.get(0).getY();
            float maxIntensity = mIntensityFloat.get(0).getY();

            for (Entry e : mIntensityFloat) {
                if (e.getY() < minIntensity) minIntensity = e.getY();
                if (e.getY() > maxIntensity) maxIntensity = e.getY();
            }
            // 包含精确时间的时间戳
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("ddMMyyhhmmss", java.util.Locale.getDefault());
            String ts = simpleDateFormat.format(new Date());

            // POST spectrum data.
            // 数据上传
            // 构建JSON
            // 1.创建JSONObject
        }
    }

    public class refReadyReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            // 校准系数,用于波长映射的系数
            byte[] refCoeff = intent.getByteArrayExtra(KSTNanoSDK.EXTRA_REF_COEF_DATA);
            // 校准矩阵,用于补偿传感器非线性特性的矩阵
            byte[] refMatrix = intent.getByteArrayExtra(KSTNanoSDK.EXTRA_REF_MATRIX_DATA);
            ArrayList<KSTNanoSDK.ReferenceCalibration> refCal = new ArrayList<>();
            refCal.add(new KSTNanoSDK.ReferenceCalibration(refCoeff, refMatrix));
            KSTNanoSDK.ReferenceCalibration.writeRefCalFile(mContext, refCal);
//            calProgress.setVisibility(View.GONE);
        }
    }

    public class notifyCompleteReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(KSTNanoSDK.SET_TIME));
        }
    }

    public class ScanStartedReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
//            calProgress.setVisibility(View.VISIBLE);
            binding.layoutDeviceStatus.btnConnectBle.setText(getString(R.string.scanning));
        }
    }


    // TODO
    public class requestCalCoeffReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            intent.getIntExtra(KSTNanoSDK.EXTRA_REF_CAL_COEFF_SIZE, 0);
            Boolean size = intent.getBooleanExtra(KSTNanoSDK.EXTRA_REF_CAL_COEFF_SIZE_PACKET, false);
            if (size) {
//                calProgress.setVisibility(View.INVISIBLE);
//                barProgressDialog = new ProgressDialog(NewScanActivity.this);
//
//                barProgressDialog.setTitle(getString(R.string.dl_ref_cal));
//                barProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//                barProgressDialog.setProgress(0);
//                barProgressDialog.setMax(intent.getIntExtra(KSTNanoSDK.EXTRA_REF_CAL_COEFF_SIZE, 0));
//                barProgressDialog.setCancelable(false);
//                barProgressDialog.show();
            } else {
//                barProgressDialog.setProgress(barProgressDialog.getProgress() + intent.getIntExtra(KSTNanoSDK.EXTRA_REF_CAL_COEFF_SIZE, 0));
            }
        }
    }

    // TODO
    public class requestCalMatrixReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
//            intent.getIntExtra(KSTNanoSDK.EXTRA_REF_CAL_MATRIX_SIZE, 0);
//            Boolean size = intent.getBooleanExtra(KSTNanoSDK.EXTRA_REF_CAL_MATRIX_SIZE_PACKET, false);
//            if (size) {
//                barProgressDialog.dismiss();
//                barProgressDialog = new ProgressDialog(NewScanActivity.this);
//
//                barProgressDialog.setTitle(getString(R.string.dl_cal_matrix));
//                barProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//                barProgressDialog.setProgress(0);
//                barProgressDialog.setMax(intent.getIntExtra(KSTNanoSDK.EXTRA_REF_CAL_MATRIX_SIZE, 0));
//                barProgressDialog.setCancelable(false);
//                barProgressDialog.show();
//            } else {
//                barProgressDialog.setProgress(barProgressDialog.getProgress() + intent.getIntExtra(KSTNanoSDK.EXTRA_REF_CAL_MATRIX_SIZE, 0));
//            }
//            if (barProgressDialog.getProgress() == barProgressDialog.getMax()) {
//
//                LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(KSTNanoSDK.REQUEST_ACTIVE_CONF));
//            }
        }
    }

    public class DisconnReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(mContext, R.string.nano_disconnected, Toast.LENGTH_SHORT).show();

            requireActivity().finish();
        }
    }


    private class ScanConfReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            byte[] smallArray = intent.getByteArrayExtra(KSTNanoSDK.EXTRA_DATA);
            byte[] addArray = new byte[smallArray.length * 3];
            byte[] largeArray = new byte[smallArray.length + addArray.length];

            System.arraycopy(smallArray, 0, largeArray, 0, smallArray.length);
            System.arraycopy(addArray, 0, largeArray, smallArray.length, addArray.length);

            Log.w("_JNI","largeArray Size: "+ largeArray.length);
            KSTNanoSDK.ScanConfiguration scanConf = KSTNanoSDK.KSTNanoSDK_dlpSpecScanReadConfiguration(intent.getByteArrayExtra(KSTNanoSDK.EXTRA_DATA));

//            barProgressDialog.dismiss();
//            btn_scan.setClickable(true);
//            btn_scan.setBackgroundColor(ContextCompat.getColor(mContext, R.color.kst_red));
//            mMenu.findItem(R.id.action_settings).setEnabled(true);

            SettingsManager.storeStringPref(mContext, SettingsManager.SharedPreferencesKeys.scanConfiguration, scanConf.getConfigName());
        }
    }

    @SuppressLint("MissingPermission")
    // 核心：启动扫描并自动连接
    private void startAutoConnect() {
        BluetoothManager bluetoothManager = (BluetoothManager) requireContext().getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = bluetoothManager.getAdapter();
        mBluetoothLeScanner = adapter.getBluetoothLeScanner();

        if (mBluetoothLeScanner == null) {
            Toast.makeText(getContext(), "蓝牙未开启或不支持", Toast.LENGTH_SHORT).show();
            return;
        }

        // 如果 8 秒内没搜到，停止并恢复按钮状态
        mHandler.postDelayed(() -> {
            if (mScanning) {
                mScanning = false;
                mBluetoothLeScanner.stopScan(mLeScanCallback);
                binding.layoutDeviceStatus.btnConnectBle.setText("未找到设备");
                binding.layoutDeviceStatus.btnConnectBle.setEnabled(true);
            }
        }, SCAN_PERIOD);

        mScanning = true;
        mBluetoothLeScanner.startScan(mLeScanCallback);
        binding.layoutDeviceStatus.btnConnectBle.setText("正在搜索光谱仪...");
        binding.layoutDeviceStatus.btnConnectBle.setEnabled(false);
    }
    @SuppressLint("MissingPermission")
    private final ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String deviceName = device.getName();
            String deviceAddress = device.getAddress();

            // 1. 打印所有发现的设备，用于实时监控
            Log.d("AnalysisFragment", "扫描中 -> 名称: " + deviceName + " 地址: " + deviceAddress);

            // 2. 自动判定逻辑：
            // 判定条件：名字包含 "Nano"（不区分大小写）
            if (deviceName != null && deviceName.toLowerCase().contains("nano")) {

                // 锁定成功，立即停止扫描防止重复触发
                mScanning = false;
                if (mBluetoothLeScanner != null) {
                    mBluetoothLeScanner.stopScan(mLeScanCallback);
                }

                // 3. 获取 MAC 地址并存储
                mDeviceAddress = deviceAddress;
                Log.i("AnalysisFragment", "找到目标设备！MAC: " + mDeviceAddress);

                // 持久化保存，下次启动可直接连接
                SettingsManager.storeStringPref(requireContext(),
                        SettingsManager.SharedPreferencesKeys.preferredDevice, mDeviceAddress);

                // 4. 切换到主线程发起连接并更新 UI
                mHandler.post(() -> {
                    if (binding != null) {
                        binding.layoutDeviceStatus.btnConnectBle.setText("发现设备，连接中...");
                    }

                    if (mBLEService != null) {
                        // 调用 NanoBLEService 的连接方法
                        mBLEService.connect(mDeviceAddress);
                    } else {
                        Log.e("AnalysisFragment", "服务尚未初始化，无法连接");
                    }
                });
            }
        }
    };
}