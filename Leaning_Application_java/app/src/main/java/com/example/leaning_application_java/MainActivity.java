package com.example.leaning_application_java;

import Model.MenuItem;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import Adapter.MenuAdapter;

// 1. 实现接口
public class MainActivity extends AppCompatActivity  {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView rv = findViewById(R.id.rv_menu);
        rv.setLayoutManager(new LinearLayoutManager(this));

        List<MenuItem> items = new ArrayList<>();

        items.add(new MenuItem("1.Leaning_Adapter", LearnAdapter.class));
        items.add(new MenuItem("2.MPChart",ChartActivity.class));
        items.add(new MenuItem("3.Volley",VolleyActivity.class));
        items.add(new MenuItem("4.DialogFragment", DialogFragmengActivity.class));
        items.add(new MenuItem("5.StaticFragment", StaticFragmentActivity.class));
        items.add(new MenuItem("6.DynamicFragment", DynamicFragmentActivity.class));

        MenuAdapter adapter = new MenuAdapter(items);
        rv.setAdapter(adapter);


    }


    }



