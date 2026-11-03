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
import java.util.Calendar;
import java.util.Locale;

public class MedicalAddActivity extends AppCompatActivity {

    private int selectedPetId = -1;
    private String selectedPetName; // 🌟 接收名字的變數
    private String selectedDbDate;
    private com.google.android.material.imageview.ShapeableImageView imgPetAvatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_medical_add);

        // 1. 🌟 接收上一頁傳來的 ID 與 名字
        selectedPetId = getIntent().getIntExtra("PET_ID", -1);
        selectedPetName = getIntent().getStringExtra("PET_NAME");

        // 2. 初始化元件
        TextView tvPetName = findViewById(R.id.tv_pet_name); // 🌟 精準抓取頭像下方的名字 (小花)
        imgPetAvatar = findViewById(R.id.img_pet_avatar);
        TextView tvDate = findViewById(R.id.tv_medical_date_select);
        Spinner spType = findViewById(R.id.sp_medical_type);
        EditText etNote = findViewById(R.id.et_medical_note);
        Button btnSave = findViewById(R.id.btn_save_medical);

        // 3. 🌟 顯示名字與抓取頭像
        if (tvPetName != null && selectedPetName != null) {
            tvPetName.setText(selectedPetName);
        } else if (tvPetName != null) {
            tvPetName.setText("未知寵物");
        }
        fetchPetAvatar();

        // 4. 設定 Spinner 選項
        String[] types = {"請選擇醫療類別", "疫苗接種", "手術治療", "例行檢查", "生理量測", "其他紀錄"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, types);
        spType.setAdapter(adapter);

        // 5. 日期選擇器預設日期為「今天」
        Calendar c = Calendar.getInstance();
        updateDateDisplay(tvDate, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));

        tvDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, day) -> {
                updateDateDisplay(tvDate, year, month, day);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        // 6. 儲存按鈕
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
            saveToMSSQL(selectedPetId, selectedDbDate, type, note);
        });

        // 返回按鈕
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // 修正排版與狀態列
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void updateDateDisplay(TextView tv, int year, int month, int day) {
        String uiDate = (year - 1911) + "/" + (month + 1) + "/" + day;
        tv.setText(uiDate);
        selectedDbDate = String.format(Locale.getDefault(), "%d-%02d-%02d", year, (month + 1), day);
    }

    private void fetchPetAvatar() {
        if (selectedPetId == -1) return;
        new Thread(() -> {
            try (Connection conn = ConnectionHelper.getConnection()) {
                String sql = "SELECT Species, Image FROM Pets WHERE PetID = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, selectedPetId);
                    try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            String species = rs.getString("Species");
                            byte[] imgBytes = rs.getBytes("Image");
                            runOnUiThread(() -> {
                                if (imgBytes != null && imgBytes.length > 0) {
                                    android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);
                                    imgPetAvatar.setImageBitmap(bitmap);
                                } else {
                                    imgPetAvatar.setImageResource("貓".equals(species) ? R.drawable.cat_placeholder : R.drawable.dog_placeholder);
                                }
                            });
                        }
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void saveToMSSQL(int petId, String date, String type, String note) {
        new Thread(() -> {
            try (Connection conn = ConnectionHelper.getConnection()) {
                String sql = "INSERT INTO Medical (PetID, Date, Category, Description) VALUES (?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, petId);
                    pstmt.setString(2, date);
                    pstmt.setString(3, type);
                    pstmt.setString(4, note);
                    pstmt.executeUpdate();
                }

                runOnUiThread(() -> {
                    Toast.makeText(this, "儲存成功！", Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "儲存失敗：" + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}