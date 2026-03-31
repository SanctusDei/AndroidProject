package Adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ubi.NanoScan.R; // 确保换成你实际的包名

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
public class RecentSampleAdapter extends RecyclerView.Adapter<RecentSampleAdapter.ViewHolder> {
    private JSONArray mData;

    public RecentSampleAdapter(JSONArray data) { this.mData = data; }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_sample, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        try {

            JSONObject item = mData.getJSONObject(position);
            int score = item.getInt("score");
            holder.tvTitle.setText(item.optString("label", "未知样本"));
            holder.tvTime.setText(item.optString("time", "刚刚"));
            holder.tvScore.setText(String.valueOf(score));

            if (score >= 90) {
                // 优秀：绿色系
                holder.tvScore.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                holder.tvStatus.setText("优秀");
                holder.tvStatus.setTextColor(Color.parseColor("#4CAF50"));
                holder.tvStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F1F8E9")));
            } else {
                // 良好：橙/黄色系
                holder.tvScore.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFA000")));
                holder.tvStatus.setText("良好");
                holder.tvStatus.setTextColor(Color.parseColor("#FFA000"));
                holder.tvStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFF3E0")));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() { return mData != null ? mData.length() : 0; }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTime, tvScore, tvStatus;
        ViewHolder(View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tvItemTitle);
            tvTime = v.findViewById(R.id.tvItemTime);
            tvScore = v.findViewById(R.id.tvItemScore);
            tvStatus = v.findViewById(R.id.tvItemStatus);
        }
    }
}