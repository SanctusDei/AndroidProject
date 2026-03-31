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
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;

import com.kstechnologies.nirscannanolibrary.KSTNanoSDK;
import com.kstechnologies.nirscannanolibrary.SettingsManager;
import com.ubi.NanoScan.NanoBLEService;
import com.ubi.NanoScan.R;
import com.ubi.NanoScan.databinding.FragmentAnalysisBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import utils.ComponentAdapter;
import utils.ComponentItem;

public class AnalysisFragment extends Fragment {

    private static final String TAG = "UbiNIRS_Analysis";
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

    // --- 生命周期：创建与绑定 ---
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
            if (mNanoBLEService != null && preferredDevice != null) {
                mNanoBLEService.connect(preferredDevice);
            } else {
                scanLeDevice(true);
            }
        });
    }


    public class scanDataReadyReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {

            binding.btnStartScan.setText("开始分析");

            byte[] scanData = intent.getByteArrayExtra(KSTNanoSDK.EXTRA_DATA);
            if (scanData == null) return;

            // 官方 JNI 解析
            if (!KSTNanoSDK.ReferenceCalibration.currentCalibration.isEmpty()) {
                KSTNanoSDK.ReferenceCalibration ref = KSTNanoSDK.ReferenceCalibration.currentCalibration.get(0);
                KSTNanoSDK.ScanResults results = KSTNanoSDK.KSTNanoSDK_dlpSpecScanInterpReference(
                        scanData, ref.getRefCalCoefficients(), ref.getRefCalMatrix()
                );

                Log.d(TAG, "官方 JNI 解析成功，波长数量: " + results.getLength());

                // TODO: 模拟显示结果，后续可在此处替换为你真实的识别逻辑
                startAnalysisLogic();
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

            // 官方逻辑：在这里彻底唤醒按钮
            binding.btnStartScan.setEnabled(true);
            binding.btnStartScan.setClickable(true);
            binding.btnStartScan.setBackgroundColor(ContextCompat.getColor(mContext, R.color.kst_red));
            binding.layoutDeviceStatus.btnConnectBle.setText("已连接");

            Log.d(TAG, "设备全链路初始化完毕，按钮已激活！");
        }
    }

    /** 8. 断开连接 */
    public class DisconnReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(mContext, R.string.nano_disconnected, Toast.LENGTH_SHORT).show();
            binding.btnStartScan.setEnabled(false);
            binding.layoutDeviceStatus.btnConnectBle.setText("点击连接");
            connected = false;
        }
    }

    // ================== 服务与蓝牙管理 ==================

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

            if (preferredDevice != null) {
                scanPreferredLeDevice(true);
            } else {
                scanLeDevice(true);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mNanoBLEService = null;
        }
    };
    @SuppressLint("Missingpermission")
    private void scanLeDevice(final boolean enable) {
        if (enable && mBluetoothLeScanner != null) {
            binding.layoutDeviceStatus.btnConnectBle.setText("搜索中...");
            mHandler.postDelayed(() -> {
                mBluetoothLeScanner.stopScan(mLeScanCallback);
                if (!connected) notConnectedDialog();
            }, NanoBLEService.SCAN_PERIOD);
            mBluetoothLeScanner.startScan(mLeScanCallback);
        } else if (mBluetoothLeScanner != null) {
            mBluetoothLeScanner.stopScan(mLeScanCallback);
        }
    }
    @SuppressLint("Missingpermission")
    private void scanPreferredLeDevice(final boolean enable) {
        if (enable && mBluetoothLeScanner != null) {
            binding.layoutDeviceStatus.btnConnectBle.setText("搜索首选设备...");
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

    // ================== 安全取值防护盾 ==================

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


    private void startAnalysisLogic() {
        binding.btnStartScan.setEnabled(false);
        binding.btnStartScan.setText("分析中...");
        mHandler.postDelayed(() -> {
            if (binding == null) return;
            binding.layoutResults.setVisibility(View.VISIBLE);
            binding.qualityMeterLayout.tvQualityScore.setText(String.valueOf(92));
            ObjectAnimator.ofInt(binding.qualityMeterLayout.qualityProgressBar, "progress", 0, 92).setDuration(800).start();

            List<ComponentItem> items = new ArrayList<>();
            items.add(new ComponentItem("蛋白质", String.format(Locale.US, "%.2f", 3.2f), "g/100ml"));
            items.add(new ComponentItem("脂肪", String.format(Locale.US, "%.2f", 3.8f), "g/100ml"));
            binding.rvComponents.setAdapter(new ComponentAdapter(items));

            binding.btnStartScan.setEnabled(true);
            binding.btnStartScan.setText("开始分析");

        }, 1500);
    }
}