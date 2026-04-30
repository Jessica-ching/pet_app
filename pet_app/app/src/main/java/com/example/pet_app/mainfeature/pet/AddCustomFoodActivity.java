package com.example.pet_app.mainfeature.pet;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.pet_app.ConnectionHelper;
import com.example.pet_app.R;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class AddCustomFoodActivity extends AppCompatActivity {

    private int currentUserId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_custom_food);

        // 取得當前 UserID (用於關聯是誰新增的食物)
        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        currentUserId = prefs.getInt("UserID", -1);

        EditText etName = findViewById(R.id.et_custom_food_name);
        EditText etCalorie = findViewById(R.id.et_custom_food_calories);
        Button btnSave = findViewById(R.id.btn_save_custom_food);

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String calorieStr = etCalorie.getText().toString().trim();

            if (name.isEmpty() || calorieStr.isEmpty()) {
                Toast.makeText(this, "請完整輸入內容", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentUserId == -1) {
                Toast.makeText(this, "請先登入帳號", Toast.LENGTH_SHORT).show();
                return;
            }

            // --- 修改：改為儲存到 MSSQL ---
            saveFoodToDB(name, calorieStr);
        });

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // 🌟 核心方法：儲存到 MSSQL 資料庫
    private void saveFoodToDB(String name, String cal) {
        new Thread(() -> {
            try (Connection conn = ConnectionHelper.getConnection()) {
                // 假設你的 Food 資料表欄位為：FoodName, CaloriesPer100g, UserID
                // 這樣才能區分這是哪個使用者的私藏食譜
                String sql = "INSERT INTO Food (FoodName, CaloriesPer100g, UserID) VALUES (?, ?, ?)";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, name);
                pstmt.setFloat(2, Float.parseFloat(cal));
                pstmt.setInt(3, currentUserId);

                pstmt.executeUpdate();

                runOnUiThread(() -> {
                    Toast.makeText(this, "自定義食物已儲存至雲端", Toast.LENGTH_SHORT).show();

                    // 回傳結果給上一個頁面 (可能是 FeedingActivity)
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("FOOD_NAME", name);
                    resultIntent.putExtra("FOOD_CALORIE", Integer.parseInt(cal));
                    setResult(RESULT_OK, resultIntent);

                    finish();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "儲存失敗：" + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}