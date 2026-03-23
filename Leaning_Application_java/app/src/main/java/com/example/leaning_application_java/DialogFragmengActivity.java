package com.example.leaning_application_java;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import utils.EditNameDialog;

public class DialogFragmengActivity extends AppCompatActivity implements EditNameDialog.OnNameSaveListener {

    private TextView tvNameDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dialog_fragmeng);

        tvNameDisplay = findViewById(R.id.tv_name);

        findViewById(R.id.btn_change_name).setOnClickListener(v -> {
            // 4. 显示对话框
            EditNameDialog dialog = new EditNameDialog();
            dialog.show(getSupportFragmentManager(), "EditName");

        });


    }

    // 5.实现接口方法,当对话框点击“确定”时，这里会被自动触发
    @Override
    public void onNameSaved(String name) {
        tvNameDisplay.setText("当前用户名：" + name);
        Toast.makeText(this,"修改成功", Toast.LENGTH_SHORT).show();

    }
}