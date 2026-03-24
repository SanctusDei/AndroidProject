package com.example.milkanalytic;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar); // 这一行非常重要，否则 onCreateOptionsMenu 不起作用
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // 设置点击监听
        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    Toast.makeText(MainActivity.this, "首页", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == R.id.nav_analytic) {
                    Toast.makeText(MainActivity.this, "分析", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == R.id.nav_history) {
                    Toast.makeText(MainActivity.this, "历史", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == R.id.nav_settings) {
                    Toast.makeText(MainActivity.this, "设置", Toast.LENGTH_SHORT).show();
                     return true;
                }
                return false;
            }
        });
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_toolbar_menu, menu);
        return true;
    }

    // 处理点击事件
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_search) {
            // 执行搜索逻辑
            Toast.makeText(this, "点击了搜索", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_settings) {
            // 跳转到设置 Fragment 或 Activity
            // navController.navigate(R.id.settingsFragment);
            return true;
        } else if (id == R.id.action_about) {
            // 处理关于
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}