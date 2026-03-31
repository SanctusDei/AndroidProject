package fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import com.ubi.NanoScan.databinding.FragmentSettingsBinding;
import java.io.File;

public class SettingsFragment extends Fragment {

    // 声明 ViewBinding 对象
    private FragmentSettingsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 使用 ViewBinding 加载并绑定布局
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. 初始化下拉菜单数据
        setupDropdowns();

        // 2. 设置点击和状态监听事件
        setupListeners();
    }

    private void setupDropdowns() {
        // 主题选择
        String[] themes = {"浅色", "深色", "跟随系统"};
        ArrayAdapter<String> themeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, themes);
        binding.selectTheme.setAdapter(themeAdapter);

        // 图表样式选择
        String[] charts = {"现代", "经典", "简约"};
        ArrayAdapter<String> chartAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, charts);
        binding.selectChartStyle.setAdapter(chartAdapter);

        // 语言选择
        String[] langs = {"简体中文", "繁體中文", "English"};
        ArrayAdapter<String> langAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, langs);
        binding.selectLanguage.setAdapter(langAdapter);
    }

    private void setupListeners() {
        // 主题切换逻辑
        binding.selectTheme.setOnItemClickListener((parent, view, position, id) -> {
            int mode;
            switch (position) {
                case 1: mode = AppCompatDelegate.MODE_NIGHT_YES;break;
                case 2: mode =  AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;break;
                default:
                    mode = AppCompatDelegate.MODE_NIGHT_NO;break;
            };
            AppCompatDelegate.setDefaultNightMode(mode);
        });

        // 按钮点击逻辑
        binding.btnExportData.setOnClickListener(v -> Toast.makeText(getContext(), "准备导出 UbiNIRS 分析数据...", Toast.LENGTH_SHORT).show());
        binding.btnDeleteAll.setOnClickListener(v -> Toast.makeText(getContext(), "已清空所有本地记录", Toast.LENGTH_SHORT).show());
        binding.btnClearCache.setOnClickListener(v -> handleClearCache());

        // 开关监听
        binding.swAnalysis.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // TODO: 保存分析通知偏好，例如使用 SharedPreferences
        });

        binding.swQuality.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // TODO: 保存质量警报偏好
        });

        binding.swWeekly.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // TODO: 保存周报提醒偏好
        });

    }

    private void handleClearCache() {
        if (clearAppCache()) {
            Toast.makeText(getContext(), "应用缓存已清理完毕", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "暂无缓存或清理失败", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean clearAppCache() {
        try {
            File dir = requireContext().getCacheDir();
            return deleteDir(dir);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    if (!deleteDir(new File(dir, child))) return false;
                }
            }
        }
        return dir != null && dir.delete();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 关键一步：Fragment 视图销毁时释放 binding 对象，防止内存泄漏
        binding = null;
    }
}