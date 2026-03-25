package fragment;

import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.milkanalytic.R;
import com.example.milkanalytic.databinding.FragmentAnalysisBinding;
import com.github.mikephil.charting.data.RadarData;
import com.github.mikephil.charting.data.RadarDataSet;
import com.github.mikephil.charting.data.RadarEntry;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;

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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAnalysisBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.rvComponents.setLayoutManager(new GridLayoutManager(getContext(), 2));
        updateDeviceStatus(false);
        binding.btnStartScan.setOnClickListener(v -> startAnalysisLogic());
        binding.etProductionDate.setOnClickListener(v-> showDatePicker());

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}