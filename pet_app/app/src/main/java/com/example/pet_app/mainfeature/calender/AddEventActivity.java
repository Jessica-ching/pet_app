package com.example.pet_app.mainfeature.calender;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog; // 🌟 新增時鐘選擇器套件
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

    // 🌟 1. 補上宣告 tvEndDate 和 tvTimeRange
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

        // 🌟 2. 綁定所有 UI 元件
        tvPetNameInAdd = findViewById(R.id.tv_pet_name_in_add);
        tvStartDate = findViewById(R.id.tv_start_date);
        tvEndDate = findViewById(R.id.tv_end_date);       // 綁定結束日期
        tvTimeRange = findViewById(R.id.tv_time_range);   // 綁定時間範圍
        etEventContent = findViewById(R.id.et_event_content);
        Button btnSave = findViewById(R.id.btn_save);
        LinearLayout layoutPetSelector = findViewById(R.id.layout_pet_selector);

        // 初始化日期與時間
        Calendar c = Calendar.getInstance();
        updateDateDisplay(tvStartDate, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        updateDateDisplay(tvEndDate, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));

        // 異步讀取該使用者的寵物名單
        fetchUserPets();

        // 寵物選擇器
        layoutPetSelector.setOnClickListener(v -> showPetPopup(v));

        // 🌟 開始日期選擇器
        tvStartDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, y, m, d) -> updateDateDisplay(tvStartDate, y, m, d),
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        // 🌟 結束日期選擇器 (新增的耳朵)
        tvEndDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, y, m, d) -> updateDateDisplay(tvEndDate, y, m, d),
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        // 🌟 時間範圍選擇器：連續跳出兩個時鐘 (開始時間 -> 結束時間)
        tvTimeRange.setOnClickListener(v -> {
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            // 第一個彈窗：選開始時間
            new TimePickerDialog(this, (view1, startHour, startMinute) -> {

                // 第二個彈窗：選結束時間
                new TimePickerDialog(this, (view2, endHour, endMinute) -> {
                    // 將兩個時間組合成 "00:00 - 00:00" 的格式
                    String startTime = String.format("%02d:%02d", startHour, startMinute);
                    String endTime = String.format("%02d:%02d", endHour, endMinute);
                    tvTimeRange.setText(startTime + " - " + endTime);
                }, startHour, startMinute, true).show(); // 結束時間預設為剛選的開始時間

            }, hour, minute, true).show(); // true 表示使用 24 小時制
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

    // 🌟 小修改：讓這個方法可以指定要更新哪個 TextView (開始或結束)
    private void updateDateDisplay(TextView targetTextView, int y, int m, int d) {
        targetTextView.setText(y + "/" + (m + 1) + "/" + d);
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
        // 從畫面抓取輸入的資料
        String content = etEventContent.getText().toString().trim(); // 行程內容 (對應 DB 的 Title)
        String startDateStr = tvStartDate.getText().toString();      // 開始日期 (對應 DB 的 EvenDate)
        String timeRangeStr = tvTimeRange.getText().toString();      // 時間範圍 (對應 DB 的 EvenTime)

        // 防呆檢查
        if (selectedPetId == -1) {
            Toast.makeText(this, "請選擇寵物", Toast.LENGTH_SHORT).show();
            return;
        }
        if (content.isEmpty()) {
            Toast.makeText(this, "請輸入內容", Toast.LENGTH_SHORT).show();
            return;
        }

        // 開啟背景執行緒連線資料庫
        new Thread(() -> {
            try (Connection conn = ConnectionHelper.getConnection()) {

                // 🌟 修正後的 SQL 語法：完美對準 Events 資料表！
                // 注意拼字：Events, EvenDate, EvenTime
                // 🌟 請把原本那句 SQL 替換成下面這行（補上了 EventDate 跟 EventTime 的 t）
                String sql = "INSERT INTO Events (UserID, PetID, EventDate, EventTime, Title) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement pstmt = conn.prepareStatement(sql);

                pstmt.setInt(1, currentUserId);   // 第 1 個問號：存入 UserID
                pstmt.setInt(2, selectedPetId);   // 第 2 個問號：存入 PetID

                // 轉換日期格式 yyyy/M/d -> yyyy-MM-dd 以符合 SQL 規定
                String dbDate = startDateStr.replace("/", "-");
                pstmt.setString(3, dbDate);       // 第 3 個問號：存入 EvenDate

                pstmt.setString(4, timeRangeStr); // 第 4 個問號：存入 EvenTime (例如 09:00 - 12:00)
                pstmt.setString(5, content);      // 第 5 個問號：存入 Title (行程內容)

                // 執行寫入動作！
                pstmt.executeUpdate();

                // 成功後回到主畫面顯示提示並關閉頁面
                runOnUiThread(() -> {
                    Toast.makeText(this, "行程儲存成功！", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            } catch (Exception e) {
                e.printStackTrace();
                // 🌟 小撇步：把錯誤訊息印在畫面上，以後抓蟲更快！
                runOnUiThread(() -> Toast.makeText(this, "儲存失敗: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    static class PetSimpleModel {
        int id; String name;
        PetSimpleModel(int id, String name) { this.id = id; this.name = name; }
    }
}