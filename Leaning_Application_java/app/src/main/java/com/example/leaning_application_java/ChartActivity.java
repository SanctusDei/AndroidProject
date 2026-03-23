package com.example.leaning_application_java;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.IDataSet;
import com.google.android.material.tabs.TabLayout;

import java.io.IOException;
import java.io.InputStream;

import utils.SpectrumStaticLoader;

public class ChartActivity extends AppCompatActivity {

    private LineChart lineChart;
    private SpectrumStaticLoader.SpectrumData allData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chart);

        lineChart = findViewById(R.id.lineChart);
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        try {
            InputStream is = getAssets().open("suhuangHadamard 1_019001_20260316_105311.csv");
            allData = SpectrumStaticLoader.loadAllData(is);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (allData == null) {
            Toast.makeText(this, "文件读取失败", Toast.LENGTH_SHORT);
            return;
        }

//        设置Tab切换监听
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
//                        根据选中的位置(0, 1, 2) 切换数据
                updateChart(tab.getPosition());
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}

        });
//        初始打开默认是第一张图
        updateChart(0);
    }

    private void updateChart(int position) {
        LineData lineData;

        if(position == 0) { // 光强 Intensity
            LineDataSet setRef = new LineDataSet(allData.intensityRef, "Reference");
            setRef.setColor(Color.GRAY);
            LineDataSet setSam = new LineDataSet(allData.intensitySample,"Sample");
            setSam.setColor(Color.BLUE);
            lineData = new LineData(setRef, setSam);
            lineChart.getDescription().setText("Intensity (unitless)");
        } else if (position == 1) { // 吸光度 Absorbance
            LineDataSet setAbs = new LineDataSet(allData.absorbance, "Absorbance");
            setAbs.setColor(Color.RED);
            setAbs.setDrawFilled(true);
            setAbs.setFillAlpha(48);
            lineData = new LineData(setAbs);
            lineChart.getDescription().setText("Absorbance (AU)");
        } else {
            LineDataSet setReflect = new LineDataSet(allData.reflectance, "Reflectance");
            setReflect.setColor(Color.GREEN);
            lineData = new LineData(setReflect);
            lineChart.getDescription().setText("Reflectance (%)");
        }

        // 应用同一的样式
        styleChart(lineData);
        lineChart.setData(lineData);

        // 关键,重置轴范围并刷新
        lineChart.notifyDataSetChanged();
        lineChart.animateX(400);
        lineChart.invalidate();

    }

    private void styleChart(LineData data) {
        for (IDataSet set : data.getDataSets()) {
            LineDataSet lds = (LineDataSet) set;
            lds.setDrawCircles(false);
            lds.setLineWidth(1.5f);
            lds.setDrawValues(false);
        }



//        设置 X 轴在底部
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getAxisRight().setEnabled(false);


        // 1. 设置图表四周的偏移量（防止坐标轴文字被切掉）
        lineChart.setExtraOffsets(10f, 10f, 10f, 10f);

        // 2. 设置 X 轴标签
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(50f); // 每隔50nm显示一个刻度
        xAxis.setYOffset(10f);    // X轴文字距离图表的距离

        // 3. 设置 Y 轴标签偏移
        lineChart.getAxisLeft().setXOffset(10f);
        lineChart.getAxisRight().setEnabled(false);

        // 4. 背景色设置
        lineChart.setDrawGridBackground(false);
        lineChart.setBackgroundColor(Color.WHITE);

        // 在 styleChart 里调整
        lineChart.setExtraLeftOffset(0f); // 将左侧额外偏移设为 0
        lineChart.setExtraRightOffset(15f); // 给右侧留点空间，平衡视觉



    }
}

