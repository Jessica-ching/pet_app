package com.example.pet_app.mainfeature.pet;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pet_app.ConnectionHelper;
import com.example.pet_app.R;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;

public class PetInfoActivity extends AppCompatActivity {

    private int selectedPetId;
    private TextView tvTitleName, tvType, tvAge, tvWeight, tvGender, tvSterilized, tvGoalCals, tvGoalWater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_info);

        // 1. 初始化所有 UI 元件
        tvTitleName = findViewById(R.id.tv_pet_info_name);
        tvType = findViewById(R.id.tv_info_type);
        tvAge = findViewById(R.id.tv_info_age);
        tvWeight = findViewById(R.id.tv_info_weight);
        tvGender = findViewById(R.id.tv_info_gender);
        tvSterilized = findViewById(R.id.tv_info_sterilized);
        tvGoalCals = findViewById(R.id.tv_info_goal_cals);
        tvGoalWater = findViewById(R.id.tv_info_goal_water);

        // 2. 取得 Intent 傳過來的 PetID
        selectedPetId = getIntent().getIntExtra("PET_ID", -1);

        if (selectedPetId == -1) {
            Toast.makeText(this, "查無此寵物資料", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 返回按鈕
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // 編輯按鈕
        findViewById(R.id.btn_edit_pet).setOnClickListener(v -> {
            // 導向你的編輯頁面 (這裡假設你用 CreatePetInfoActivity 來兼任編輯功能)
            Intent intent = new Intent(this, com.example.pet_app.login.CreatePetInfoActivity.class);

            // 告訴下一頁：這是「編輯模式」
            intent.putExtra("IS_EDIT_MODE", true);
            intent.putExtra("PET_ID", selectedPetId);

            // 如果你想讓使用者在下一頁直接看到舊資料，可以把現有的值也傳過去
            intent.putExtra("EXISTING_NAME", tvTitleName.getText().toString());
            intent.putExtra("EXISTING_WEIGHT", tvWeight.getText().toString());

            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 每次回到這頁都重新抓取，確保顯示最新資訊
        fetchPetDetailsFromDB();
    }

    // 🌟 從資料庫讀取詳細資訊
    private void fetchPetDetailsFromDB() {
        new Thread(() -> {
            try (Connection conn = ConnectionHelper.getConnection()) {
                // 根據 PetDB 文件，選取正確欄位名
                // 注意：DailyCalories 與 DailyWater 是我們之前手動在 Pets 表增加的欄位
                String sql = "SELECT PetName, Species, Gender, Birthday, Weight, IsSterilized, RecommendCalories, RecommendWater FROM Pets WHERE PetID = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, selectedPetId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            String name = rs.getString("PetName");
                            String species = rs.getString("Species");
                            String gender = rs.getString("Gender");
                            String birthday = rs.getString("Birthday"); // 格式 YYYY-MM-DD
                            float weight = rs.getFloat("Weight");
                            boolean isSterilized = rs.getBoolean("IsSterilized");
                            int goalCals = rs.getInt("RecommendCalories");
                            int goalWater = rs.getInt("RecommendWater");

                            // 計算年齡
                            String ageDisplay = calculateAge(birthday);

                            runOnUiThread(() -> {
                                tvTitleName.setText(name);
                                tvType.setText(species);
                                tvGender.setText(gender);
                                tvAge.setText(ageDisplay);
                                tvWeight.setText(String.format("%.1f", weight));
                                tvSterilized.setText(isSterilized ? "已結紮" : "未結紮");
                                tvGoalCals.setText(String.valueOf(goalCals));
                                tvGoalWater.setText(String.valueOf(goalWater));
                            });
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "讀取資料庫失敗", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // 輔助方法：根據 Birthday 字串計算年齡
    private String calculateAge(String birthday) {
        if (birthday == null || birthday.isEmpty()) return "未知";
        try {
            // 假設生日格式為 "2022-05-20"
            String[] parts = birthday.split("-");
            int birthYear = Integer.parseInt(parts[0]);
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            int age = currentYear - birthYear;

            if (age <= 0) return "未滿 1 歲";
            return age + " 歲";
        } catch (Exception e) {
            return "未知";
        }
    }
}