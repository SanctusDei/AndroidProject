package fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.milkanalytic.R;
import com.example.milkanalytic.databinding.FragmentHistoryBinding;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. 初始化 RecyclerView
        adapter = new HistoryAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);

        // 2. 加载模拟数据
        loadDummyData();
        updateStatistics(); // 统计不受搜索影响
        adapter.setRecords(allRecords);

        // 3. 监听搜索框输入
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

        // 处理空状态 UI
        if (filteredList.isEmpty()) {
            binding.recyclerView.setVisibility(View.GONE);
            binding.layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            binding.recyclerView.setVisibility(View.VISIBLE);
            binding.layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void updateStatistics() {
        if (allRecords.isEmpty()) return;

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

    private void loadDummyData() {
        allRecords.add(new HistoryRecord(1, "全脂牛奶 A", "2026-03-24", "14:30", "A001", 95, 3.2, 3.8, 120, "up"));
        allRecords.add(new HistoryRecord(2, "低脂牛奶 B", "2026-03-23", "10:15", "B002", 88, 3.0, 1.5, 115, "stable"));
        // ... (添加剩余假数据)
    }
}