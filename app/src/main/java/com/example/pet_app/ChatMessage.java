package com.example.pet_app;

import java.util.List;

public class ChatMessage {
    private String content;
    private boolean isUser; // true: 使用者發送 (右邊), false: AI發送 (左邊)
    private List<Hospital> hospitalList; // 🌟 新增：存放醫院資料的列表

    // 保留原本的建構子，完全不影響原有的純文字對話
    public ChatMessage(String content, boolean isUser) {
        this(content, isUser, null);
    }

    // 🌟 新增：帶有醫院列表的建構子
    public ChatMessage(String content, boolean isUser, List<Hospital> hospitalList) {
        this.content = content;
        this.isUser = isUser;
        this.hospitalList = hospitalList;
    }

    public String getContent() { return content; }
    public boolean isUser() { return isUser; }

    // 🌟 新增：取得醫院列表
    public List<Hospital> getHospitalList() { return hospitalList; }

    // 🌟 新增：判斷這條訊息是否有包含醫院資料
    public boolean hasHospitals() {
        return hospitalList != null && !hospitalList.isEmpty();
    }
}