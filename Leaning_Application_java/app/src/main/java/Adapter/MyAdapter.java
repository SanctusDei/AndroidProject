package Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.leaning_application_java.R;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {

    private String[] mData;  // 数据源

    public MyAdapter(String[] data) {
        this.mData = data;

    }

    // 类似于展示柜
    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tv;
        public MyViewHolder(View itemView) {
            super(itemView);
            // 定义柜子里有什么
            tv = itemView.findViewById(R.id.item_text);


        }

    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // 加载定义的item布局
        // 将 XML 布局文件变成一个真正的 View 对象
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout, parent, false);
        // 将这个 View 塞入 ViewHolder 盒子中包装起来
        return new MyViewHolder(v);

    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        // 根据位置将数据填入
        holder.tv.setText(mData[position]);
    }

    @Override
    public int getItemCount() {
        return mData.length;
    }

}
