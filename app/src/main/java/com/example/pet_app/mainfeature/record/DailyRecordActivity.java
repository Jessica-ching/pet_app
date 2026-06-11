package com.example.pet_app.mainfeature.record;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pet_app.ConnectionHelper;
import com.example.pet_app.R;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DailyRecordActivity extends AppCompatActivity {

    private int selectedPetId = -1;
    private String petName;
    private EditText etWeight, etTemp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_record);

        // 綁定 UI
        ImageView imgAvatar = findViewById(R.id.img_record_pet_avatar);
        TextView tvName = findViewById(R.id.tv_record_pet_name);
        etWeight = findViewById(R.id.et_weight);
        etTemp = findViewById(R.id.et_temperature);
        Button btnSave = findViewById(R.id.btn_save_record);

        // 1. 接收 Intent 傳過來的 ID 與名字
        selectedPetId = getIntent().getIntExtra("PET_ID", -1);
        petName = getIntent().getStringExtra("PET_NAME");

        tvName.setText(petName != null ? petName : "未知寵物");

        // 根據名字設定頭像
        if ("小花".equals(petName)) {
            imgAvatar.setImageResource(R.drawable.cat_flower);
        } else {
            imgAvatar.setImageResource(R.drawable.cat_placeholder);
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // 2. 儲存邏輯
        btnSave.setOnClickListener(v -> saveHealthData());
    }

    private void saveHealthData() {
        String weightStr = etWeight.getText().toString().trim();
        String tempStr = etTemp.getText().toString().trim();

        if (selectedPetId == -1) {
            Toast.makeText(this, "無法辨識寵物身分", Toast.LENGTH_SHORT).show();
            return;
        }

        if (weightStr.isEmpty() && tempStr.isEmpty()) {
            Toast.makeText(this, "請輸入體重或體溫", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try (Connection conn = ConnectionHelper.getConnection()) {
                conn.setAutoCommit(false); // 事務開始

                // A. 更新 Pets 表的當前體重
                if (!weightStr.isEmpty()) {
                    String sqlWeight = "UPDATE Pets SET Weight = ? WHERE PetID = ?";
                    try (PreparedStatement pstmt1 = conn.prepareStatement(sqlWeight)) {
                        pstmt1.setDouble(1, Double.parseDouble(weightStr));
                        pstmt1.setInt(2, selectedPetId);
                        pstmt1.executeUpdate();
                    }
                }

                // B. 將體溫存入 Medical 表 (作為歷史紀錄)
                if (!tempStr.isEmpty()) {
                    String sqlTemp = "INSERT INTO Medical (PetID, Date, Category, Description) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement pstmt2 = conn.prepareStatement(sqlTemp)) {
                        pstmt2.setInt(1, selectedPetId);

                        // 取得當前日期 yyyy-MM-dd
                        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                        pstmt2.setString(2, today);
                        pstmt2.setString(3, "生理量測");
                        pstmt2.setString(4, "體溫紀錄: " + tempStr + " °C");
                        pstmt2.executeUpdate();
                    }
                }

                conn.commit(); // 提交

                runOnUiThread(() -> {
                    Toast.makeText(this, "健康數據已儲存", Toast.LENGTH_SHORT).show();
                    finish();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "儲存失敗，請檢查網路連線", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}