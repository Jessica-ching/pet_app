package com.example.pet_app.mainfeature.record;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
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

    private RecyclerView rvMedicalList;
    private MedicalAdapter adapter;
    private List<MedicalModel> recordList = new ArrayList<>();

    // 🌟 宣告標頭元件變數
    private TextView tvPetNameHeader;
    private int selectedPetId = -1;
    private String selectedPetName; // 🌟 宣告存名字的變數

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medical_list);

        // 1. 🌟 關鍵接收：同時接收上一頁傳來的 ID 與 名字
        selectedPetId = getIntent().getIntExtra("PET_ID", -1);
        selectedPetName = getIntent().getStringExtra("PET_NAME"); // 🌟 接收「小黑」名字

        // 2. 初始化元件
        rvMedicalList = findViewById(R.id.rv_medical_records);
        tvPetNameHeader = findViewById(R.id.tv_pet_name); // 🌟 綁定標頭的 TextView

        // 3. 🌟 關鍵顯示：把「小花」蓋成「小黑」
        if (tvPetNameHeader != null && selectedPetName != null) {
            tvPetNameHeader.setText(selectedPetName); // 🌟 設定 correct Name
        }

        adapter = new MedicalAdapter(recordList);
        if (rvMedicalList != null) {
            rvMedicalList.setLayoutManager(new LinearLayoutManager(this));
            rvMedicalList.setAdapter(adapter);
        }

        if (rvMedicalList != null) {
            rvMedicalList.setLayoutManager(new LinearLayoutManager(this));
        }

        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // 4. 「加入」按鈕
        TextView btnAddMedical = findViewById(R.id.btn_add_medical);
        if (btnAddMedical != null) {
            btnAddMedical.setOnClickListener(v -> {
                // 跳轉到新增頁面
                Intent intent = new Intent(MedicalListActivity.this, MedicalAddActivity.class);
                intent.putExtra("PET_ID", selectedPetId);

                // 🌟 關鍵傳送：把「小黑」這個名字繼續傳給下一頁 (圖三)
                intent.putExtra("PET_NAME", selectedPetName);

                startActivity(intent);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchMedicalRecords();
    }

    private void fetchMedicalRecords() {
        if (selectedPetId == -1) return;

        new Thread(() -> {
            List<MedicalModel> tempList = new ArrayList<>();
            try (Connection conn = ConnectionHelper.getConnection()) {
                // ⚠️ 請再次確認資料表名稱與欄位名稱 (例如是否為 MedicalDate)
                String sql = "SELECT Date, Category, Description FROM Medical WHERE PetID = ? ORDER BY Date DESC";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, selectedPetId);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    String dbDate = rs.getString("Date");
                    String category = rs.getString("Category");
                    String desc = rs.getString("Description");

                    // 🌟 優化日期轉換邏輯
                    String displayDate = formatToMinguo(dbDate);

                    String finalReason = (desc != null && !desc.trim().isEmpty()) ? desc : category;

                    // 這裡 MedicalModel 的建構子要對齊你的定義
                    MedicalModel record = new MedicalModel(0, displayDate, category, finalReason);
                    tempList.add(record);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            runOnUiThread(() -> {
                recordList.clear();
                recordList.addAll(tempList);
                adapter.notifyDataSetChanged(); // 🌟 現在這裡直接 notify 即可
            });
        }).start();
    }

    private String formatToMinguo(String dbDate) {
        if (dbDate == null || dbDate.isEmpty()) return "無日期";
        try {
            // 處理 yyyy-MM-dd 或 yyyy/MM/dd
            String separator = dbDate.contains("-") ? "-" : "/";
            String[] parts = dbDate.split(separator);
            if (parts.length >= 3) {
                int year = Integer.parseInt(parts[0]) - 1911;
                return year + "/" + parts[1] + "/" + parts[2];
            }
        } catch (Exception e) {
            return dbDate; // 失敗就回傳原始字串
        }
        return dbDate;
    }
}