package Fragment;

import android.graphics.Color;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.leaning_application_java.R;


public class DynamicFragment extends Fragment {

    // 在方法外面定义，用来记住当前是否已经是紫色
    private boolean isPurple = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dynamic, container, false);

        Button btnChangeColor = view.findViewById(R.id.btn_change_color);
        TextView tvColor = view.findViewById(R.id.tvColor);

        btnChangeColor.setOnClickListener(v -> {
            if (!isPurple) {
                // 变紫色
                view.setBackgroundColor(Color.parseColor("#FFBB86FC"));
                tvColor.setText("现在是紫色！");
                tvColor.setTextColor(Color.WHITE);
//                btnChangeColor.setTextColor(Color.WHITE);

                isPurple = true; // 标记状态
            } else {
                // 变回白色
                view.setBackgroundColor(Color.WHITE);
                tvColor.setText("现在是白色！");
                tvColor.setTextColor(Color.BLACK);
//                btnChangeColor.setTextColor(Color.BLACK);

                isPurple = false; // 标记状态
            }
        });

        return view;
    }
}