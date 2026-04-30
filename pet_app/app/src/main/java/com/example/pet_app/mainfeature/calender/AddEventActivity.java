package com.example.pet_app.mainfeature.calender;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
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
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AddEventActivity extends AppCompatActivity {

    private int currentUserId = -1;
    private int selectedPetId = -1; // 儲存選中的寵物 ID
    private List<PetSimpleModel> userPets = new ArrayList<>();

    private TextView tvPetNameInAdd, tvStartDate;
    private EditText etEventContent;
    private String selectedCategory = "一般行程"; // 預設分類

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_event);

        // 取得登入狀態
        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        currentUserId = prefs.getInt("UserID", -1);

        // 綁定 UI
        tvPetNameInAdd = findViewById(R.id.tv_pet_name_in_add);
        tvStartDate = findViewById(R.id.tv_start_date);
        etEventContent = findViewById(R.id.et_event_content);
        Button btnSave = findViewById(R.id.btn_save);
        LinearLayout layoutPetSelector = findViewById(R.id.layout_pet_selector);

        // 1. 初始化日期為今天
        Calendar c = Calendar.getInstance();
        updateDateDisplay(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));

        // 2. 異步讀取該使用者的寵物名單
        fetchUserPets();

        // 寵物選擇器
        layoutPetSelector.setOnClickListener(v -> showPetPopup(v));

        // 日期選擇器
        tvStartDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, y, m, d) -> updateDateDisplay(y, m, d),
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        // 儲存按鈕
        btnSave.setOnClickListener(v -> saveEventToDB());

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void updateDateDisplay(int y, int m, int d) {
        tvStartDate.setText(y + "/" + (m + 1) + "/" + d);
    }

    private void fetchUserPets() {
        new Thread(() -> {
            try (Connection conn = ConnectionHelper.getConnection()) {
                String sql = "SELECT PetID, PetName FROM Pets WHERE UserID = ?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, currentUserId);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    userPets.add(new PetSimpleModel(rs.getInt("PetID"), rs.getString("PetName")));
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void showPetPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        for (PetSimpleModel pet : userPets) {
            popup.getMenu().add(pet.name);
        }
        popup.setOnMenuItemClickListener(item -> {
            tvPetNameInAdd.setText(item.getTitle());
            for (PetSimpleModel pet : userPets) {
                if (pet.name.equals(item.getTitle())) {
                    selectedPetId = pet.id;
                    break;
                }
            }
            return true;
        });
        popup.show();
    }

    private void saveEventToDB() {
        String content = etEventContent.getText().toString().trim();
        String dateStr = tvStartDate.getText().toString();

        if (selectedPetId == -1) {
            Toast.makeText(this, "請選擇寵物", Toast.LENGTH_SHORT).show();
            return;
        }
        if (content.isEmpty()) {
            Toast.makeText(this, "請輸入內容", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try (Connection conn = ConnectionHelper.getConnection()) {
                // 將行程存入 Medical 資料表
                String sql = "INSERT INTO Event (PetID, EventDate, Category, Dascription) VALUES (?, ?, ?, ?)";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, selectedPetId);
                // 轉換日期格式 yyyy/M/d -> yyyy-MM-dd
                String dbDate = dateStr.replace("/", "-");
                pstmt.setString(2, dbDate);
                pstmt.setString(3, selectedCategory);
                pstmt.setString(4, content);
                pstmt.executeUpdate();

                runOnUiThread(() -> {
                    Toast.makeText(this, "行程儲存成功", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "儲存失敗", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    static class PetSimpleModel {
        int id; String name;
        PetSimpleModel(int id, String name) { this.id = id; this.name = name; }
    }
}