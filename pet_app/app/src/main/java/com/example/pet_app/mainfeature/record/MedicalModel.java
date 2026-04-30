package com.example.pet_app.mainfeature.record;

public class MedicalModel {
    private String title;   // 對應 Category
    private String date;    // 對應 Date
    private String content; // 對應 Dascription

    public MedicalModel(String title, String date, String content) {
        this.title = title;
        this.date = date;
        this.content = content;
    }

    public String getTitle() { return title; }
    public String getDate() { return date; }
    public String getContent() { return content; }
}
