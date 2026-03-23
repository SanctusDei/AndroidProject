package com.example.simplecomputer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SecondActivity extends AppCompatActivity {

    private Button btnBack;
    private TextView txtFinalResult;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_second);

        btnBack = findViewById(R.id.btnBack);
//        1.找到结果显示控件
        txtFinalResult = findViewById(R.id.txtFinalResult);

//        2.获取传递过来的意图
        Intent intent = getIntent();

//        3.提取数据
        double result = intent.getDoubleExtra("AddResult",0.0);
        String userName = intent.getStringExtra("UserName");
//        4.将结果显示出来
        txtFinalResult.setText(userName+"同学，你的计算结果是: " + result);

        btnBack.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                finish();
            }
        });

    }
}