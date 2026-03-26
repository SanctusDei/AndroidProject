package Adapter;


import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.milkanalytic.R;

import java.util.ArrayList;
import java.util.List;


import model.HistoryRecord;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<HistoryRecord> records = new ArrayList<>();

    public void setRecords(List<HistoryRecord> newRecords) {
        this.records = newRecords;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryRecord record = records.get(position);

        holder.tvName.setText(record.name);
        holder.tvBatch.setText(record.batchNumber);
        holder.tvDatetime.setText(record.date + " " + record.time);
        holder.tvProtein.setText(record.protein + "g");
        holder.tvFat.setText(record.fat + "g");
        holder.tvCalcium.setText(record.calcium + "mg");
        holder.tvScore.setText(String.valueOf(record.score));

        // 1. 处理趋势图标 (你需要导入对应图标到 drawable)
        if ("up".equals(record.trend)) {
            holder.ivTrend.setImageResource(android.R.drawable.arrow_up_float);
            holder.ivTrend.setColorFilter(Color.parseColor("#22C55E")); // green-500
        } else if ("down".equals(record.trend)) {
            holder.ivTrend.setImageResource(android.R.drawable.arrow_down_float);
            holder.ivTrend.setColorFilter(Color.parseColor("#EF4444")); // red-500
        } else {
            holder.ivTrend.setImageResource(android.R.drawable.ic_menu_more);
            holder.ivTrend.setColorFilter(Color.parseColor("#9CA3AF")); // gray-400
        }

        // 2. 处理评分颜色 (对应 getScoreColor)
        GradientDrawable bgShape = (GradientDrawable) holder.layoutScore.getBackground();
        if (record.score >= 90) {
            bgShape.setColor(Color.parseColor("#F0FDF4")); // green-50
            holder.tvScore.setTextColor(Color.parseColor("#16A34A")); // green-600
        } else if (record.score >= 80) {
            bgShape.setColor(Color.parseColor("#FEFCE8")); // yellow-50
            holder.tvScore.setTextColor(Color.parseColor("#CA8A04")); // yellow-600
        } else {
            bgShape.setColor(Color.parseColor("#FFF7ED")); // orange-50
            holder.tvScore.setTextColor(Color.parseColor("#EA580C")); // orange-600
        }
    }

    @Override
    public int getItemCount() { return records.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvBatch, tvDatetime, tvProtein, tvFat, tvCalcium, tvScore;
        ImageView ivTrend;
        LinearLayout layoutScore;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvBatch = itemView.findViewById(R.id.tv_batch);
            tvDatetime = itemView.findViewById(R.id.tv_datetime);
            tvProtein = itemView.findViewById(R.id.tv_protein);
            tvFat = itemView.findViewById(R.id.tv_fat);
            tvCalcium = itemView.findViewById(R.id.tv_calcium);
            tvScore = itemView.findViewById(R.id.tv_score);
            ivTrend = itemView.findViewById(R.id.iv_trend);
            layoutScore = itemView.findViewById(R.id.layout_score);
        }
    }
}