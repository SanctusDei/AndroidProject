package fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MarkerOptions;
import com.ubi.NanoScan.databinding.FragmentCommunityBinding;

import java.util.ArrayList;
import java.util.List;

import Adapter.MerchantAdapter;
import model.MerchantInfo;
import model.CommentInfo;
import Adapter.CommentAdapter;
import android.graphics.Color; // 需要使用颜色常量
import android.widget.Button;
import android.widget.EditText;

import com.ubi.NanoScan.R; // 必须引入你的项目 R 文件
import utils.MarkerUtils; // 引入刚才写的工具类

public class CommunityFragment extends Fragment {
    private FragmentCommunityBinding binding;
    private AMap aMap;

    private static final LatLng NUIST_CENTER = new LatLng(32.206, 118.717);

    // 统一的数据源
    private List<MerchantInfo> globalMerchantList = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapsInitializer.updatePrivacyShow(getContext(), true, true);
        MapsInitializer.updatePrivacyAgree(getContext(), true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCommunityBinding.inflate(inflater, container, false);
        binding.mapView.onCreate(savedInstanceState);

        initMap();
        generateDataAndBind(); // 生成数据并绑定视图

        return binding.getRoot();
    }

    private void initMap() {
        if (aMap == null) {
            aMap = binding.mapView.getMap();
        }
        aMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(
                NUIST_CENTER, 15, 0, 0
        )));

        aMap.setOnInfoWindowClickListener(marker -> {
            // 根据 marker 找到对应的 MerchantInfo
            for (MerchantInfo info : globalMerchantList) {
                if (info.name.equals(marker.getTitle())) {
                    showCommentDialog(info);
                    break;
                }
            }
        });
    }

    // --- 核心方法：数据驱动 UI ---
    private void generateDataAndBind() {
        globalMerchantList.clear();

        globalMerchantList.add(new MerchantInfo("西苑北门百佳超市", new LatLng(32.204, 118.715),
                92, true, "优质鲜奶专供点", R.drawable.baijia));

        // 为优质商家添加特定的图片资源 ID (需要事先准备这三个 drawable 文件)
        globalMerchantList.add(new MerchantInfo("东苑罗森超市", new LatLng(32.208, 118.719),
                95, true, "官方认证：乳制品持续达标", R.drawable.luosen));


        globalMerchantList.add(new MerchantInfo("中苑食堂水吧", new LatLng(32.210, 118.710),
                88, true, "各项指标优良", R.drawable.supermarket));

        // 渲染到地图
        renderMarkersToMap();

        // 渲染到列表
        renderListToBottomSheet();
    }

    private void renderMarkersToMap() {
        aMap.clear();
        if (getContext() == null) return;

        for (MerchantInfo info : globalMerchantList) {
            MarkerOptions options = new MarkerOptions().position(info.location);

            // 根据质量状态决定边框颜色
            int borderColor = info.isSafe ? Color.GREEN : Color.RED;

            // 使用工具类生成自定义图标（假设图标大小 45dp）
            // 注意：因为只标出了那3个固定商家，所以假设他们都有特定的图片，
            // 真实情况通常会有一个默认头像。
            options.icon(MarkerUtils.getRoundedMarkerBitmap(
                    getContext(),
                    info.imageResId != 0 ? info.imageResId : R.drawable.ic_launcher_background, // 如果没设图片，用默认图标占位
                    borderColor,
                    45)); // 目标大小 45dp

            // 设置 InfoWindow 气泡信息
            if (info.isSafe) {
                options.title(info.name)
                        .snippet("✅ " + info.detail);
            } else {
                // 如果将来有预警商家，标题前加个警告
                options.title("⚠️ " + info.name)
                        .snippet("预警: " + info.detail);
            }

            aMap.addMarker(options);
        }
    }

    private void renderListToBottomSheet() {
        // 更新 UI 上的收录数字
        binding.tvMerchantCountBadge.setText("收录 " + globalMerchantList.size() + " 家");

        // 设置 RecyclerView
        binding.rvMerchants.setLayoutManager(new LinearLayoutManager(getContext()));
        MerchantAdapter adapter = new MerchantAdapter(globalMerchantList);
        binding.rvMerchants.setAdapter(adapter);
    }


    private void showCommentDialog(MerchantInfo info) {
        // 1. 如果没有评价，先塞入几条高逼格的模拟数据
        if (info.comments.isEmpty()) {
            info.addComment(new CommentInfo(
                    "24计软院篮球大王",
                    R.drawable.whitemanba, // 建议换成几张真实的头像图片
                    "经常买这家的酸奶，真的很好吃！",
                    "昨天 18:30"
            ));
            info.addComment(new CommentInfo(
                    "23物联网第一深情",
                    R.drawable.nailong,
                    "蛋白质含量达到了 3.3g/100ml，对得起这个价格，没踩坑。",
                    "3天前"
            ));
        }

        // 2. 绑定 UI
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_merchant_comments, null);
        // ... 获取 RecyclerView, EditText, Button ... (代码同上一条回复)

        // 3. 设置适配器
        RecyclerView rv = dialogView.findViewById(R.id.rv_comment_list);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        CommentAdapter adapter = new CommentAdapter(info.comments);
        rv.setAdapter(adapter);

        // 4. 提交新评价（模拟当前用户的操作）
        Button btnSubmit = dialogView.findViewById(R.id.btn_submit_comment);
        EditText etComment = dialogView.findViewById(R.id.et_new_comment);

        btnSubmit.setOnClickListener(v -> {
            String content = etComment.getText().toString().trim();
            if (!content.isEmpty()) {
                // 模拟当前登录用户发布了一条评价
                CommentInfo newComment = new CommentInfo(
                        "我 (UbiNIRS 开发者)",
                        R.mipmap.ic_launcher_round, // 你的头像
                        content,
                        "刚刚"
                );
                info.addComment(newComment);

                adapter.notifyItemInserted(0);
                rv.scrollToPosition(0); // 滚动到最顶部看新评价
                etComment.setText("");
            }
        });

        // 5. 显示 Dialog
        new AlertDialog.Builder(getContext()).setView(dialogView).show();
    }
    // ... 原有的生命周期方法 (onResume, onPause, onDestroyView) 保持不变 ...
    @Override
    public void onResume() { super.onResume(); binding.mapView.onResume(); }
    @Override
    public void onPause() { super.onPause(); binding.mapView.onPause(); }
    @Override
    public void onDestroyView() { super.onDestroyView(); binding.mapView.onDestroy(); binding = null; }
}