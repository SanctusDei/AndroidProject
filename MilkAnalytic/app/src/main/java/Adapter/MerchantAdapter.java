package Adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ubi.NanoScan.R; // 注意：改成你自己的包名下的 R
import java.util.List;

import model.MerchantInfo;

public class MerchantAdapter extends RecyclerView.Adapter<MerchantAdapter.ViewHolder> {

    private List<MerchantInfo> merchantList;

    public MerchantAdapter(List<MerchantInfo> list) {
        this.merchantList = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_merchant_status, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MerchantInfo info = merchantList.get(position);

        holder.tvName.setText(info.name);
        holder.tvDetail.setText(info.detail);
        holder.tvScore.setText(info.score + "分");

        if (info.isSafe) {
            holder.tvScore.setTextColor(Color.parseColor("#4CAF50")); // 绿色
            // 如果你有 ic_safe 图片，可以换成：holder.ivIcon.setImageResource(R.drawable.ic_safe);
            holder.ivIcon.setImageResource(android.R.drawable.ic_input_add); // 暂时用系统加号代替对勾占位
        } else {
            holder.tvScore.setTextColor(Color.parseColor("#F44336")); // 红色
            holder.ivIcon.setImageResource(android.R.drawable.ic_dialog_alert); // 系统的警告图标
        }
    }

    @Override
    public int getItemCount() {
        return merchantList == null ? 0 : merchantList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName;
        TextView tvDetail;
        TextView tvScore;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_status_icon);
            tvName = itemView.findViewById(R.id.tv_merchant_name);
            tvDetail = itemView.findViewById(R.id.tv_detect_detail);
            tvScore = itemView.findViewById(R.id.tv_ai_score);
        }
    }
}