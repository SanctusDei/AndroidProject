package com.ubi.NanoScan;

import android.Manifest;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private View navLineIndicator;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. 沉浸式状态栏设置
        setupStatusBar();

        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_navigation);
        navLineIndicator = findViewById(R.id.nav_line_indicator);

        // 2. 初始化 Navigation 组件
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();

            // 绑定 NavController 到 BottomNav
            NavigationUI.setupWithNavController(bottomNav, navController);

            // 3. 核心监听：跳转页面时刷新蓝条位置、背景方块和圆点
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                // post 确保 View 已经完成 layout 测量，从而拿到准确的 getLeft()
                bottomNav.post(() -> updateNavVisuals(destination.getId()));
            });

            // 4. 初始化位置：确保 App 刚打开时蓝条就在首页图标下方
            bottomNav.post(() -> updateNavVisuals(navController.getCurrentDestination().getId()));
        }
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void updateNavVisuals(int destId) {
        // --- 第一部分：蓝条（横条）精准平移 ---
        View selectedItem = bottomNav.findViewById(destId);
        if (selectedItem != null && navLineIndicator != null) {
            // 计算 Tab 的物理中心点：左边距 + 宽度的一半
            int itemLeft = selectedItem.getLeft();
            int itemWidth = selectedItem.getWidth();
            float itemCenterX = itemLeft + (itemWidth / 2f);

            // 计算蓝条的 translationX (中心点 - 蓝条半宽)
            float targetX = itemCenterX - (navLineIndicator.getWidth() / 2f);

            navLineIndicator.animate()
                    .translationX(targetX)
                    .setDuration(350) // 稍微拉长一点点，增加灵动感
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }

        // --- 第二部分：手动控制图标背景方块 (React 风格) ---
        for (int i = 0; i < bottomNav.getMenu().size(); i++) {
            int itemId = bottomNav.getMenu().getItem(i).getItemId();
            View itemView = bottomNav.findViewById(itemId);
            if (itemView == null) continue;

            // 关键：获取 Material 3 内部包裹图标容器的 ID
            View iconContainer = itemView.findViewById(com.google.android.material.R.id.navigation_bar_item_icon_container);

            if (itemId == destId) {
                // 选中：设置淡蓝方块背景 + 缩放动画
                if (iconContainer != null) {
                    iconContainer.setBackgroundResource(R.drawable.bg_nav_active);
                    iconContainer.animate().scaleX(1.1f).scaleY(1.1f).setDuration(250).start();
                }

                // 活跃圆点 (Badge)
                BadgeDrawable badge = bottomNav.getOrCreateBadge(itemId);
                badge.setBackgroundColor(Color.parseColor("#2563EB")); // 主题蓝
                badge.setVisible(true);
            } else {
                // 未选中：移除背景 + 恢复原始比例
                if (iconContainer != null) {
                    iconContainer.setBackground(null);
                    iconContainer.animate().scaleX(1.0f).scaleY(1.0f).setDuration(250).start();
                }
                bottomNav.removeBadge(itemId);
            }
        }
    }

    private void checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(new String[] {
                    Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION},100);
        } else {

            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            },100);
        }
    }
}