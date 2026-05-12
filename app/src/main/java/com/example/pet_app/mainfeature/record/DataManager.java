package com.example.pet_app.mainfeature.record;

import com.example.pet_app.mainfeature.calender.EventModel;
import com.example.pet_app.mainfeature.pet.PetModel;

import java.util.ArrayList;
import java.util.List;

// 建立一個存放資料的中心
public class DataManager {
    // 靜態變數，讓所有頁面都能讀取同一個清單
    public static List<EventModel> allEvents = new ArrayList<>();
    public static List<PetModel> allPets = new ArrayList<>();
    public static List<MedicalModel> medicalRecords = new ArrayList<>();

}
