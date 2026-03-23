package utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.example.leaning_application_java.R;


public class EditNameDialog extends DialogFragment {

    // 定义接口.用于把数据传回Activity


    public interface OnNameSaveListener {
        void onNameSaved(String name);
    }

    private OnNameSaveListener listener;

//    2.绑定Activity
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // 确保Activity 实现了接口,否则报错
        if (context instanceof OnNameSaveListener) {
            listener = (OnNameSaveListener) context;
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@NonNull Bundle saveInstanceState) {
        // 实例化一个构建器
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

//        3.动态添加一个简单的输入框
        // 加载自定义布局
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_edit_name, null);
        EditText etName = view.findViewById(R.id.et_user_name);

        builder.setView(view)
                .setTitle("修改用户名")
                .setPositiveButton("确定", (dialog, which) -> {
                    String newName = etName.getText().toString();
                    if (!newName.isEmpty() && listener != null) {
                        // 触发接口，传回数据
                        listener.onNameSaved(newName);

                    }
                })
                .setNegativeButton("取消", null);
//    生成并返回
        return builder.create();
    }

}
