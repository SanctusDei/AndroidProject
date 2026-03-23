package Adapter;

import java.util.List;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.leaning_application_java.R;

import Model.MenuItem;

public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.ViewHolder> {

    private List<MenuItem> menuItemList;

    public MenuAdapter(List<MenuItem> menuItemList) {
        this.menuItemList = menuItemList;

    }
//    定义静态类并继承 RecyclerView.ViewHolder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvTitle;

        public ViewHolder(@NonNull View itemView) {
            // 必须调用父类构造函数
            super(itemView);
            //
            tvTitle = itemView.findViewById(R.id.tv_menu_title);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu,parent,false);

        return new ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        MenuItem item = menuItemList.get(position);
        holder.tvTitle.setText(item.getTitle());

        // 设置点击跳转
        // holder.itemView 代表列表项的根目录，这里代表了item_menu.xml最外层的LinearLayout
        holder.itemView.setOnClickListener(v -> {
            if (item.getTargetActivity() != null) {
                Intent intent = new Intent(v.getContext(), item.getTargetActivity());
                v.getContext().startActivity(intent);
            }
        });


    }

    @Override
    public int getItemCount() {
        return menuItemList != null ? menuItemList.size() : 0;
    }


}
