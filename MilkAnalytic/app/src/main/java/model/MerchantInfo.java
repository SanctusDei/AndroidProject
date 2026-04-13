package model;

import com.amap.api.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class MerchantInfo {
    public String name;
    public LatLng location;
    public int score;
    public boolean isSafe;
    public String detail;
    public int imageResId; // 用于地图 Marker 和列表头像的图片 ID

    public List<CommentInfo> comments = new ArrayList<>();

    // 更新构造方法
    public MerchantInfo(String name, LatLng location, int score, boolean isSafe, String detail, int imageResId) {
        this.name = name;
        this.location = location;
        this.score = score;
        this.isSafe = isSafe;
        this.detail = detail;
        this.imageResId = imageResId;
    }

    public void addComment(CommentInfo comment) {
        this.comments.add(0, comment);
    }
}