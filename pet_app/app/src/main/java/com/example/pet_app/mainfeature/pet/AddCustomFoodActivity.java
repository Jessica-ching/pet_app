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
    private EditText etFoodName, etFoodCals;
    private int selectedPetId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 🌟 檢查點2：這裡要對應你剛才貼的那份 XML 的檔名
        setContentView(R.layout.activity_add_custom_food);

        // 接收傳過來的 ID
        selectedPetId = getIntent().getIntExtra("PET_ID", -1);

        // 🌟 檢查點3：這裡的 ID 必須跟你 XML 裡的 android:id 完美一致
        etFoodName = findViewById(R.id.et_custom_food_name);
        etFoodCals = findViewById(R.id.et_custom_food_calories);
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

        if (name.isEmpty() || calsPerGramStr.isEmpty()) {
            Toast.makeText(this, "名稱或熱量不能空白喔！", Toast.LENGTH_SHORT).show();
            return;
        }

        // 取得使用者輸入的「每公克熱量」
        float calsPerGram = Float.parseFloat(calsPerGramStr);

        new Thread(() -> {
            try (Connection conn = ConnectionHelper.getConnection()) {
                // 🌟 關鍵修正：這裡要存入 Snacks 表（菜單），而不是 DailyFood（紀錄）
                // 根據你的資料庫結構：Name (名稱), Calories (熱量), Gram (基準公克)
                String sql = "INSERT INTO Snacks (Name, Calories, Gram) VALUES (?, ?, ?)";
                PreparedStatement pstmt = conn.prepareStatement(sql);

                pstmt.setString(1, name);           // 食物名稱
                pstmt.setFloat(2, calsPerGram);     // 存入每公克的大卡
                pstmt.setFloat(3, 1.0f);            // 基準公克直接設為 1.0 (因為你是輸入每公克)

                pstmt.executeUpdate();

                runOnUiThread(() -> {
                    Toast.makeText(this, "成功將「" + name + "」加入食物清單！", Toast.LENGTH_SHORT).show();
                    // 儲存完就關閉，回到「新增額外進食」頁面時，選單就會抓到這筆新資料
                    finish();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "儲存至清單失敗: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}