package Adapter; // 替换为你的包名

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ubi.NanoScan.R; // 替换为你的 R 文件
import java.util.List;
import model.CommentInfo;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {
    private List<CommentInfo> comments;

    public CommentAdapter(List<CommentInfo> comments) {
        this.comments = comments;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommentInfo info = comments.get(position);

        holder.tvUsername.setText(info.username);
        holder.tvContent.setText(info.content);
        holder.tvDate.setText(info.date);

        if (info.avatarResId != 0) {
            holder.ivAvatar.setImageResource(info.avatarResId);
        } else {
            holder.ivAvatar.setImageResource(R.mipmap.ic_launcher_round); // 默认头像
        }
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvUsername;
        TextView tvDate;
        TextView tvContent;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_user_avatar);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvDate = itemView.findViewById(R.id.tv_comment_date);
            tvContent = itemView.findViewById(R.id.tv_comment_content);
        }
    }
}