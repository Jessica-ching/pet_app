package com.example.pet_app.mainfeature.calender;

public class EventModel {
    private String date;       // 例如 "2026/4/11"
    private String title;      // 例如 "餵食心絲蟲藥"
    private String subtitle;   // 例如 "大尾 · 建議時間 12:00"
    private int iconResId;     // 圖示的資源 ID，如 R.drawable.ic_pill
    private boolean isDone;    // 是否勾選完成
    private String timeTag;    // 例如 "15:30"，若為空則顯示 CheckBox

    // 🚀 新增：空的建構子 (讓首頁可以先建立空卡片，再慢慢塞資料)
    public EventModel() {
    }

    // 原本的建構子 (Constructor)
    public EventModel(String date, String title, String subtitle, int iconResId, boolean isDone, String timeTag) {
        this.date = date;
        this.title = title;
        this.subtitle = subtitle;
        this.iconResId = iconResId;
        this.isDone = isDone;
        this.timeTag = timeTag;
    }

    // Getter 方法 (讓 Adapter 可以讀取資料)
    public String getDate() { return date; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public int getIconResId() { return iconResId; }
    public boolean isDone() { return isDone; }
    public String getTimeTag() { return timeTag; }

    // Alias methods for HomeFragment
    public String getPetName() { return title; }
    public String getContent() { return subtitle; }

    // 🚀 新增：Setter 方法 (讓 HomeFragment 可以把資料設定進去)
    public void setDate(String date) { this.date = date; }
    public void setTitle(String title) { this.title = title; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public void setIconResId(int iconResId) { this.iconResId = iconResId; }
    public void setTimeTag(String timeTag) { this.timeTag = timeTag; }
    public void setDone(boolean done) { isDone = done; }
}