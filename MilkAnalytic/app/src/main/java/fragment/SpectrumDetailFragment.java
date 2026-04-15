package fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.ubi.NanoScan.R;
import com.ubi.NanoScan.databinding.FragmentSpectrumDetailBinding;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class SpectrumDetailFragment extends Fragment {

    private FragmentSpectrumDetailBinding binding;
    private int recordId;

    // 存储解析后的数据点
    private List<Entry> intensitySampleEntries = new ArrayList<>();
    private List<Entry> intensityRefEntries = new ArrayList<>();
    private List<Entry> reflectanceEntries = new ArrayList<>();
    private List<Entry> absorbanceEntries = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            recordId = getArguments().getInt("record_id", -1);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSpectrumDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupChart();
        initToggleButtons(); // 提前初始化监听
        fetchSpectrumDetail();
    }

    private void fetchSpectrumDetail() {

        String url = "http://" + getString(R.string.severUrl) + ":18000/api/spectrum/" + recordId + "/";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    if (binding == null) return;
                    try {
                        binding.tvDetailTitle.setText(response.getString("label"));

                        JSONArray w = response.getJSONArray("wavelengths");
                        JSONArray i_s = response.getJSONArray("intensity_sample");
                        JSONArray i_r = response.getJSONArray("intensity_ref");
                        JSONArray ref = response.getJSONArray("reflectance");
                        JSONArray abs = response.getJSONArray("absorbance");

                        clearEntries();

                        for (int k = 0; k < w.length(); k++) {
                            float wav = (float) w.getDouble(k);
                            intensitySampleEntries.add(new Entry(wav, (float) i_s.getDouble(k)));
                            intensityRefEntries.add(new Entry(wav, (float) i_r.getDouble(k)));
                            reflectanceEntries.add(new Entry(wav, (float) ref.getDouble(k)));
                            absorbanceEntries.add(new Entry(wav, (float) abs.getDouble(k)));
                        }

                        // 默认显示吸光度
                        updateChart(absorbanceEntries, "吸光度 (Absorbance)", "#2563EB", true);

                    } catch (JSONException e) { e.printStackTrace(); }
                },
                error -> {
                    if (getContext() != null)
                        Toast.makeText(getContext(), "无法获取光谱详情，请检查网络", Toast.LENGTH_SHORT).show();
                }
        );
        Volley.newRequestQueue(requireContext()).add(request);
    }

    private void setupChart() {
        binding.chartSpectrum.getDescription().setEnabled(false);
        binding.chartSpectrum.setNoDataText("正在加载光谱数据...");

        XAxis xAxis = binding.chartSpectrum.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(10f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) { return (int) value + " nm"; }
        });

        binding.chartSpectrum.getAxisRight().setEnabled(false);
        binding.chartSpectrum.getAxisLeft().setDrawGridLines(true);
        binding.chartSpectrum.getAxisLeft().setGridColor(Color.parseColor("#F1F1F1"));
    }

    private void updateChart(List<Entry> entries, String label, String colorHex, boolean isFilled) {
        if (entries.isEmpty() || binding == null) return;

        LineDataSet dataSet = new LineDataSet(entries, label);
        int color = Color.parseColor(colorHex);

        dataSet.setColor(color);
        dataSet.setLineWidth(2.5f);
        dataSet.setDrawCircles(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawValues(false);

        if (isFilled) {
            dataSet.setDrawFilled(true);
            dataSet.setFillColor(color);
            dataSet.setFillAlpha(30);
        }

        binding.chartSpectrum.setData(new LineData(dataSet));
        binding.chartSpectrum.animateX(600);
        binding.chartSpectrum.invalidate();
    }

    private void showIntensityChart() {
        if (intensitySampleEntries.isEmpty()) return;

        LineDataSet setS = new LineDataSet(intensitySampleEntries, "样品光强");
        setS.setColor(Color.parseColor("#3B82F6"));
        setS.setDrawCircles(false);

        LineDataSet setR = new LineDataSet(intensityRefEntries, "参考光强");
        setR.setColor(Color.parseColor("#94A3B8"));
        setR.setDrawCircles(false);
        setR.enableDashedLine(10f, 5f, 0f);

        binding.chartSpectrum.setData(new LineData(setS, setR));
        binding.chartSpectrum.animateX(600);
        binding.chartSpectrum.invalidate();
    }

    private void initToggleButtons() {
        binding.toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked || intensitySampleEntries.isEmpty()) return;
            if (checkedId == R.id.btn_int) showIntensityChart();
            else if (checkedId == R.id.btn_ref) updateChart(reflectanceEntries, "反射率", "#10B981", false);
            else if (checkedId == R.id.btn_abs) updateChart(absorbanceEntries, "吸光度", "#2563EB", true);
        });
    }

    private void clearEntries() {
        intensitySampleEntries.clear();
        intensityRefEntries.clear();
        reflectanceEntries.clear();
        absorbanceEntries.clear();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}