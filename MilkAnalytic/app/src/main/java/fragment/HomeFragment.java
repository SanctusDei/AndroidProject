package fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.ubi.NanoScan.R;
import com.ubi.NanoScan.databinding.FragmentHomeBinding;
import com.ubi.NanoScan.databinding.ItemDashboardCardBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import Adapter.RecentSampleAdapter;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();

    };

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);



        binding.btnStartAnalysis.setOnClickListener( v-> {

            NavOptions navOptions = new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_home, true)
                    .setLaunchSingleTop(true)
                    .setRestoreState(true)
                    .build();

            Navigation.findNavController(view).navigate(R.id.nav_analytic,null,navOptions);

        });

        fetchDashboardData();

    }

    // TODO
    private void fetchDashboardData() {
        String url = "http://172.22.98.184:18000/api/dashboard/";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    Log.d("Home_Volley", "dashboard返回的数据: " + response.toString()); // 看看控制台
                    onResponse(response);
                },
                error -> {
                    Log.e("Home_Volley", "dashboard请求数据失败: " + error.toString());
                    // 失败时可以显示缓存或 0
//                    binding.tvTodayValue.setText("0");
                }
        );

        // 加入请求队列
        Volley.newRequestQueue(requireContext()).add(request);
    }

    private void updateRecentList(JSONArray recentArray) {
        if (recentArray.length() == 0) {
            binding.rvRecentSamples.setVisibility(View.GONE);
//
        } else {
            binding.rvRecentSamples.setVisibility(View.VISIBLE);

            binding.rvRecentSamples.setLayoutManager(new LinearLayoutManager(requireContext()));

            RecentSampleAdapter adapter = new RecentSampleAdapter(recentArray);
            binding.rvRecentSamples.setAdapter(adapter);

        }
    }

    @Override
    public void onDestroyView() {

        super.onDestroyView();
        binding = null;

    }

    private void onResponse(JSONObject response) {
        try {
// 解析后端返回的统计字段
            int todayScans = response.getInt("today_scans");
            int totalScans = response.getInt("total_scans");
            double avgScore = response.getDouble("avg_score");

// 更新 UI 仪表盘
            setupDashboardCard(binding.cardToday,
                    String.valueOf(todayScans),
                    "今日检测",
                    R.drawable.ic_flash,
                    "+2");


            setupDashboardCard(binding.cardTotal,
                    String.valueOf(totalScans),
                    "总检测数",
                    R.drawable.ic_lab_battle,
                    "+15%");


            setupDashboardCard(binding.cardScore,
                    String.format(Locale.getDefault(), "%.0f", avgScore),
                    "平均评分",
                    R.drawable.ic_trending_up,
                    "+3");


// 如果你有显示平均分的 TextView


// 解析最近活动列表
            JSONArray recentArray = response.getJSONArray("recent_activities");
            updateRecentList(recentArray);

        } catch (JSONException e) {
            Log.e("Home_JSON", "解析失败: " + e.getMessage());
        }
    }

    private void setupDashboardCard(ItemDashboardCardBinding cardBinding, String value, String label, int iconRes, String trend) {
        cardBinding.tvValue.setText(value);
        cardBinding.tvLabel.setText(label);
        cardBinding.ivIcon.setImageResource(iconRes);

        // 如果你有趋势字段，也可以在这里统一处理
        if (cardBinding.tvTrend != null) {
            cardBinding.tvTrend.setText(trend);
        }
    }
}