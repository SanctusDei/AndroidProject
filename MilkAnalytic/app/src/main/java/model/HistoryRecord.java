package model;

public class HistoryRecord {
    public int id;
    public String name;
    public String date;
    public String time;
    public String batchNumber;
    public int score;
    public double protein;
    public double fat;
    public int calcium;
    public String trend; // "up", "down", "stable"

    public HistoryRecord(int id, String name, String date, String time, String batchNumber, int score, double protein, double fat, int calcium, String trend) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.time = time;
        this.batchNumber = batchNumber;
        this.score = score;
        this.protein = protein;
        this.fat = fat;
        this.calcium = calcium;
        this.trend = trend;
    }
}