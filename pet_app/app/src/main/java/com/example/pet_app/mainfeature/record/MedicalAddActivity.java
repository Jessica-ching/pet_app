package com.example.pet_app.mainfeature.record;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MedicalAddActivity extends AppCompatActivity {

    private int selectedPetId;
    private String selectedDbDate; // 專門存給資料庫用的格式 (yyyy-MM-dd)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_medical_add);

        // 取得傳過來的 ID
        selectedPetId = getIntent().getIntExtra("PET_ID", -1);

        TextView tvDate = findViewById(R.id.tv_medical_date_select);
        Spinner spType = findViewById(R.id.sp_medical_type);
        EditText etNote = findViewById(R.id.et_medical_note);
        Button btnSave = findViewById(R.id.btn_save_medical);

        // 1. 設定 Spinner 選項
        String[] types = {"請選擇醫療類別", "疫苗接種", "手術治療", "例行檢查", "生理量測", "其他紀錄"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, types);
        spType.setAdapter(adapter);

        // 2. 日期選擇器
        Calendar c = Calendar.getInstance();
        // 預設當天
        updateDateDisplay(tvDate, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));

        tvDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, day) -> {
                updateDateDisplay(tvDate, year, month, day);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        // 3. 儲存按鈕
        btnSave.setOnClickListener(v -> {
            String type = spType.getSelectedItem().toString();
            String note = etNote.getText().toString().trim();

            if (selectedPetId == -1) {
                Toast.makeText(this, "錯誤：遺失寵物資料", Toast.LENGTH_SHORT).show();
                return;
            }
            if (spType.getSelectedItemPosition() == 0 || note.isEmpty()) {
                Toast.makeText(this, "請完整輸入資訊", Toast.LENGTH_SHORT).show();
                return;
            }

            // 執行存檔
            saveToMSSQL(selectedPetId, selectedDbDate, type, note);
        });

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // 同時更新顯示(民國年)與儲存格式(西元年)
    private void updateDateDisplay(TextView tv, int year, int month, int day) {
        // UI 顯示：民國年 (如：115/4/28)
        String uiDate = (year - 1911) + "/" + (month + 1) + "/" + day;
        tv.setText(uiDate);

        // 存檔格式：西元年 (如：2026-04-28)
        selectedDbDate = String.format(Locale.getDefault(), "%d-%02d-%02d", year, (month + 1), day);
    }

    private void saveToMSSQL(int petId, String date, String type, String note) {
        new Thread(() -> {
            try (Connection conn = ConnectionHelper.getConnection()) {
                // 注意：這裡使用你提供的欄位名 Dascription (a 而不是 e)
                String sql = "INSERT INTO Medical (PetID, Date, Category, Dascription) VALUES (?, ?, ?, ?)";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, petId);
                pstmt.setString(2, date);
                pstmt.setString(3, type);
                pstmt.setString(4, note);

                pstmt.executeUpdate();

                runOnUiThread(() -> {
                    Toast.makeText(this, "儲存成功！", Toast.LENGTH_SHORT).show();
                    finish(); // 結束後會回到 MedicalListActivity，那邊的 onResume 會自動刷新
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "儲存失敗：" + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}