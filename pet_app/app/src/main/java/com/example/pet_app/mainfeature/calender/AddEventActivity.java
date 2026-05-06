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

    private TextView tvPetNameInAdd, tvStartDate, tvEndDate, tvTimeRange;
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
        tvEndDate = findViewById(R.id.tv_end_date);
        tvTimeRange = findViewById(R.id.tv_time_range);
        etEventContent = findViewById(R.id.et_event_content);
        Button btnSave = findViewById(R.id.btn_save);
        LinearLayout layoutPetSelector = findViewById(R.id.layout_pet_selector);

        // 1. 初始化日期為今天
        Calendar initCalendar = Calendar.getInstance();
        updateDateDisplay(initCalendar.get(Calendar.YEAR), initCalendar.get(Calendar.MONTH), initCalendar.get(Calendar.DAY_OF_MONTH));

        // 2. 異步讀取該使用者的寵物名單
        fetchUserPets();

        // 寵物選擇器
        layoutPetSelector.setOnClickListener(v -> showPetPopup(v));

        // 日期選擇器
        // 開始日期選擇器
        tvStartDate.setOnClickListener(v -> {
            Calendar startCalendar = Calendar.getInstance();
            new DatePickerDialog(this, (view, y, m, d) -> {
                // 直接更新開始日期
                tvStartDate.setText(String.format("%04d/%02d/%02d", y, (m + 1), d));
            }, startCalendar.get(Calendar.YEAR), startCalendar.get(Calendar.MONTH), startCalendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        // 結束日期選擇器
        tvEndDate.setOnClickListener(v -> {
            Calendar endCalendar = Calendar.getInstance();
            new DatePickerDialog(this, (view, y, m, d) -> {
                // 🌟 修正：這裡要更新的是 tvEndDate
                tvEndDate.setText(String.format("%04d/%02d/%02d", y, (m + 1), d));
            }, endCalendar.get(Calendar.YEAR), endCalendar.get(Calendar.MONTH), endCalendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        tvTimeRange.setOnClickListener(v -> showTimeRangePicker(tvTimeRange));

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
        // 補零格式：2024/05/07
        String date = String.format("%04d/%02d/%02d", y, (m + 1), d);
        tvStartDate.setText(date);
        // 如果你希望初始化時結束日期也跟著變今天：
        tvEndDate.setText(date);
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

    private void showTimeRangePicker(TextView targetTextView) {
        Calendar c = Calendar.getInstance();

        // 第一步：選開始時間
        new android.app.TimePickerDialog(this, (view, hourOfDay, minute) -> {
            String startTime = String.format("%02d:%02d", hourOfDay, minute);

            // 第二步：選結束時間
            new android.app.TimePickerDialog(this, (view2, hourOfDay2, minute2) -> {
                String endTime = String.format("%02d:%02d", hourOfDay2, minute2);

                // 第三步：組合並顯示
                targetTextView.setText(startTime + " - " + endTime);

            }, hourOfDay, minute, true).show(); // 預設結束時間跟開始時間一樣，比較好選

            Toast.makeText(this, "請選擇結束時間", Toast.LENGTH_SHORT).show();

        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();

        Toast.makeText(this, "請選擇開始時間", Toast.LENGTH_SHORT).show();
    }

    private void saveEventToDB() {
        String title = etEventContent.getText().toString().trim(); // 使用者輸入的內容
        String startDate = tvStartDate.getText().toString();      // EvenDate 存這個
        String endDate = ((TextView)findViewById(R.id.tv_end_date)).getText().toString();
        String timeRange = ((TextView)findViewById(R.id.tv_time_range)).getText().toString(); // EvenTime 存這個

        if (selectedPetId == -1 || title.isEmpty() || startDate.contains("選擇")) {
            Toast.makeText(this, "請填寫完整資訊", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try (Connection conn = ConnectionHelper.getConnection()) {
                // 欄位：UserID, PetID, EvenDate, EvenTime, Title
                String sql = "INSERT INTO Events (UserID, PetID, EventDate, EventTime, Title) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement pstmt = conn.prepareStatement(sql);

                pstmt.setInt(1, currentUserId);
                pstmt.setInt(2, selectedPetId);

                // EvenDate (Date 類型): 存開始日期，要把 / 換成 -
                pstmt.setString(3, startDate.replace("/", "-"));

                // EvenTime (nvarchar): 直接存 "09:00 - 11:00"
                pstmt.setString(4, timeRange);

                // Title (nvarchar): 我們把標題跟日期範圍拼在一起，這樣顯示時才完整
                // 例如: "帶去打疫苗 (2024/05/07 - 2024/05/08)"
                String fullTitle = title;
                if (!endDate.contains("選擇")) {
                    fullTitle += " (" + startDate + " ~ " + endDate + ")";
                }
                pstmt.setString(5, fullTitle);

                pstmt.executeUpdate();

                runOnUiThread(() -> {
                    Toast.makeText(this, "行程已同步至雲端", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "連線失敗，請稍後再試", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    static class PetSimpleModel {
        int id; String name;
        PetSimpleModel(int id, String name) { this.id = id; this.name = name; }
    }
}