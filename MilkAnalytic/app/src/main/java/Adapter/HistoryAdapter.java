package Adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ubi.NanoScan.R;
import model.HistoryRecord;

import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<HistoryRecord> records;

    public HistoryAdapter(List<HistoryRecord> records) {
        this.records = records;
    }
    public interface OnItemClickListener {
        void onItemClick(HistoryRecord record);
    }

    private OnItemClickListener listener;
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setRecords(List<HistoryRecord> records) {
        this.records = records;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryRecord record = records.get(position);

        holder.tvName.setText(record.name);
        holder.tvBatch.setText(record.batchNumber);
        holder.tvDatetime.setText(record.date);

        // 格式化成分数据
        holder.tvProtein.setText(String.format(Locale.getDefault(), "%.1f%%", record.protein));
        holder.tvFat.setText(String.format(Locale.getDefault(), "%.1f%%", record.fat));
        holder.tvCalcium.setText(String.format(Locale.getDefault(), "%.0fmg", record.calcium));

        holder.tvScore.setText(String.valueOf(record.score));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(record);
            }
        });

        // 根据评分动态设置颜色 (例如：>=90 为绿色，<90 为橙色)
        if (record.score >= 90) {
            holder.tvScore.setTextColor(Color.parseColor("#16A34A")); // 绿色
        } else {
            holder.tvScore.setTextColor(Color.parseColor("#F59E0B")); // 橙黄色
        }
    }

    @Override
    public int getItemCount() {
        return records == null ? 0 : records.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvBatch, tvDatetime;
        TextView tvProtein, tvFat, tvCalcium, tvScore;

        ViewHolder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tv_name);
            tvBatch = v.findViewById(R.id.tv_batch);
            tvDatetime = v.findViewById(R.id.tv_datetime);
            tvProtein = v.findViewById(R.id.tv_protein);
            tvFat = v.findViewById(R.id.tv_fat);
            tvCalcium = v.findViewById(R.id.tv_calcium);
            tvScore = v.findViewById(R.id.tv_score);
        }
    }
}