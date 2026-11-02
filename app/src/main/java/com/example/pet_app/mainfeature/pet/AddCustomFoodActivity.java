package com.example.pet_app.mainfeature.pet; // 🌟 檢查點1：如果檔案在 chat 資料夾，請手動把 .pet 改成 .chat

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pet_app.ConnectionHelper;
import com.example.pet_app.R;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddCustomFoodActivity extends AppCompatActivity {

    // 🌟 依照你寫的 XML 元件宣告變數
    private EditText etFoodName, etFoodCals, etFoodGrams;
    private int selectedPetId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_custom_food);

        selectedPetId = getIntent().getIntExtra("PET_ID", -1);

        etFoodName = findViewById(R.id.et_custom_food_name);
        etFoodCals = findViewById(R.id.et_custom_food_calories);
        etFoodGrams = findViewById(R.id.et_custom_food_grams);
        Button btnSave = findViewById(R.id.btn_save_custom_food);
        ImageButton btnBack = findViewById(R.id.btn_back);

        // 返回鍵功能
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // 儲存功能
        btnSave.setOnClickListener(v -> saveToDB());
    }

    private void saveToDB() {
        String name = etFoodName.getText().toString().trim();
        String calsPerGramStr = etFoodCals.getText().toString().trim();
        String gramsStr = etFoodGrams.getText().toString().trim();

        if (name.isEmpty() || calsPerGramStr.isEmpty() || gramsStr.isEmpty()) {
            Toast.makeText(this, "請填寫所有欄位喔！", Toast.LENGTH_SHORT).show();
            return;
        }

        // 取得使用者輸入
        float calsPerGram = Float.parseFloat(calsPerGramStr);
        float gramsEaten = Float.parseFloat(gramsStr);
        int totalCalories = Math.round(calsPerGram * gramsEaten);

        new Thread(() -> {
            try (Connection conn = ConnectionHelper.getConnection()) {
                // 1. 存入 Snacks 表 (供以後選取)
                String sqlSnack = "INSERT INTO Snacks (Name, Calories, Gram) VALUES (?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sqlSnack)) {
                    pstmt.setString(1, name);           // 食物名稱
                    pstmt.setFloat(2, calsPerGram);     // 每公克大卡
                    pstmt.setFloat(3, 1.0f);            // 基準公克設為 1
                    pstmt.executeUpdate();
                }

                // 2. 存入 DailyFood 表 (紀錄本次進食)
                String sqlDaily = "INSERT INTO DailyFood (PetID, Calories, RecordDate) VALUES (?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sqlDaily)) {
                    String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                    pstmt.setInt(1, selectedPetId);
                    pstmt.setInt(2, totalCalories);
                    pstmt.setString(3, today);
                    pstmt.executeUpdate();
                }

                runOnUiThread(() -> {
                    Toast.makeText(this, "成功記錄「" + name + "」並加入清單！", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "儲存失敗: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}