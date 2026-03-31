package model;

import org.json.JSONObject;

public class HistoryRecord {
    public int id;
    public String name;
    public String date;
    public int score;
    public double protein;
    public double fat;
    public double calcium;
    public String batchNumber;
    public String trend;

    // 自动从 Django 的 JSON 格式映射
    public static HistoryRecord fromJson(JSONObject json) {
        HistoryRecord r = new HistoryRecord();
        r.id = json.optInt("id", 0);
        r.name = json.optString("label", "未知样品");
        r.date = json.optString("date", "未知时间");
        r.score = json.optInt("score", 0);
        r.protein = json.optDouble("protein", 0.0);
        r.fat = json.optDouble("fat", 0.0);

        // 如果后端目前还没有传 batch 和 calcium，我们先生成一些美观的占位数据
        r.batchNumber = json.optString("batch", "B" + (json.optInt("id", 0) + 1000));
        r.calcium = json.optDouble("calcium", 115.0);
        r.trend = "stable";
        return r;
    }
}