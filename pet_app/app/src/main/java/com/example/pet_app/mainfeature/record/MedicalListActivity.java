package com.example.pet_app.mainfeature.record;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pet_app.ConnectionHelper;
import com.example.pet_app.R;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class MedicalListActivity extends AppCompatActivity {
    private MedicalAdapter adapter;
    private int selectedPetId;
    private String petName;
    private List<MedicalModel> medicalRecords = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_medical_list);

        // 1. 取得傳過來的 ID 與名稱
        selectedPetId = getIntent().getIntExtra("PET_ID", -1);
        petName = getIntent().getStringExtra("PET_NAME");

        TextView tvName = findViewById(R.id.tv_pet_name);
        tvName.setText(petName);

        // 返回按鈕
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // 加入按鈕 (跳轉到新增醫療紀錄頁面)
        findViewById(R.id.btn_add_medical).setOnClickListener(v -> {
            Intent intent = new Intent(this, MedicalAddActivity.class);
            intent.putExtra("PET_ID", selectedPetId);
            intent.putExtra("PET_NAME", petName);
            startActivity(intent);
        });

        // 設定 RecyclerView
        RecyclerView rv = findViewById(R.id.rv_medical_records);
        rv.setLayoutManager(new LinearLayoutManager(this));

        // 先給空清單
        adapter = new MedicalAdapter(medicalRecords);
        rv.setAdapter(adapter);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 每次回到頁面都重新從資料庫抓取最新資料
        loadMedicalRecords();
    }

    // 🌟 核心：從 MSSQL 抓取資料
    private void loadMedicalRecords() {
        if (selectedPetId == -1) return;

        new Thread(() -> {
            List<MedicalModel> tempList = new ArrayList<>();
            try (Connection conn = ConnectionHelper.getConnection()) {
                // 根據 PetID 查詢紀錄，按日期降序排列 (最新的在上面)
                String sql = "SELECT Category, Date, Dascription FROM Medical WHERE PetID = ? ORDER BY Date DESC";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, selectedPetId);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    // 將資料庫資料封裝進 Model (假設你的 MedicalModel 建構子是這三個欄位)
                    // Category 當標題，Date 當日期，Dascription 當內容
                    tempList.add(new MedicalModel(
                            rs.getString("Category"),
                            rs.getString("Date"),
                            rs.getString("Dascription")
                    ));
                }

                runOnUiThread(() -> {
                    medicalRecords.clear();
                    medicalRecords.addAll(tempList);
                    adapter.notifyDataSetChanged();

                    if (medicalRecords.isEmpty()) {
                        Toast.makeText(this, "尚無醫療紀錄", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "讀取紀錄失敗", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}