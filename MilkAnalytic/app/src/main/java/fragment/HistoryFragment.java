package fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.ubi.NanoScan.R;
import com.ubi.NanoScan.databinding.FragmentHistoryBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import Adapter.HistoryAdapter;
import model.HistoryRecord;

public class HistoryFragment extends Fragment {

    private FragmentHistoryBinding binding;
    private HistoryAdapter adapter;
    private List<HistoryRecord> allRecords = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. 初始化 RecyclerView (传入空列表防止 NullPointer)
        adapter = new HistoryAdapter(new ArrayList<>());
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);

        // 2. 设置点击跳转监听 (务必在 fetch 之前设置)
        adapter.setOnItemClickListener(record -> {
            Bundle bundle = new Bundle();
            bundle.putInt("record_id", record.id);

            // 使用 Navigation 组件跳转到详情页
            Navigation.findNavController(view).navigate(R.id.spectrumDetailFragment, bundle);
        });

        // 3. 从 Django 后端拉取真实数据
        fetchHistoryData();

        // 4. 监听搜索框输入
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterData(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    } // 修复：之前这里缺少结束括号

    private void fetchHistoryData() {
        String url = "http://172.22.98.184:18000/api/history/";

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET, url, null,
                response -> {
                    if (binding == null) return; // 容错：防止异步回调时 Fragment 已销毁

                    allRecords.clear();
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);
                            allRecords.add(HistoryRecord.fromJson(obj));
                        }

                        adapter.setRecords(allRecords);
                        updateStatistics();
                        toggleEmptyState(allRecords.isEmpty());

                    } catch (JSONException e) {
                        Log.e("History_JSON", "解析异常: " + e.getMessage());
                    }
                },
                error -> {
                    Log.e("History_Volley", "请求失败: " + error.toString());
                    toggleEmptyState(true);
                }
        );

        Volley.newRequestQueue(requireContext()).add(request);
    }

    private void filterData(String query) {
        List<HistoryRecord> filteredList = new ArrayList<>();
        if (query.isEmpty()) {
            filteredList.addAll(allRecords);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (HistoryRecord record : allRecords) {
                if (record.name.toLowerCase().contains(lowerCaseQuery) ||
                        record.batchNumber.toLowerCase().contains(lowerCaseQuery)) {
                    filteredList.add(record);
                }
            }
        }
        adapter.setRecords(filteredList);
        toggleEmptyState(filteredList.isEmpty());
    }

    private void toggleEmptyState(boolean isEmpty) {
        if (binding == null) return;
        if (isEmpty) {
            binding.recyclerView.setVisibility(View.GONE);
            binding.layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            binding.recyclerView.setVisibility(View.VISIBLE);
            binding.layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void updateStatistics() {
        if (allRecords.isEmpty() || binding == null) return;

        int totalCount = allRecords.size();
        double totalScore = 0;
        int excellentCount = 0;

        for (HistoryRecord r : allRecords) {
            totalScore += r.score;
            if (r.score >= 90) excellentCount++;
        }

        binding.tvTotalCount.setText(String.valueOf(totalCount));
        binding.tvAvgScore.setText(String.format(Locale.getDefault(), "%.1f", totalScore / totalCount));
        binding.tvExcellentRate.setText(String.format(Locale.getDefault(), "%d%%", (excellentCount * 100) / totalCount));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}