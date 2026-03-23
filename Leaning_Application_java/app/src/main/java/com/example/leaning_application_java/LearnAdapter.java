package com.example.leaning_application_java;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import Adapter.MyAdapter;

public class LearnAdapter extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_learn_adapter);

//        1.准备简单数据
        String[] iotDevice = {"ESP32 控制器", "STM32 核心板", "超声波传感器", "红外循迹模块"};

//        2.找到 RecyclerView
        RecyclerView rv = findViewById(R.id.rv_main_list);

//        3.布局样式设置(设置为普通的纵向列表)
        rv.setLayoutManager(new LinearLayoutManager(this));

//        4.绑定适配器
        MyAdapter adapter =new MyAdapter(iotDevice);
        rv.setAdapter(adapter);


    }
}