package com.example.pet_app.mainfeature.pet;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pet_app.ConnectionHelper;
import com.example.pet_app.R;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PetDailyStatusActivity extends AppCompatActivity {

    private ProgressBar pbFood, pbWater;
    private TextView tvFoodStats, tvWaterStats, tvName;

    private int selectedPetId;
    private int currentCals = 0;
    private int goalCals = 1;
    private int currentWater = 0;
    private int goalWater = 1;

    private final ActivityResultLauncher<Intent> feedingLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    fetchTodayStatsFromDB();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_daily_status);

        // 1. 初始化 UI 元件 (對應你剛才給我的 XML ID)
        pbFood = findViewById(R.id.pb_food_daily);
        pbWater = findViewById(R.id.pb_water_daily);
        tvFoodStats = findViewById(R.id.tv_food_stats);
        tvWaterStats = findViewById(R.id.tv_water_stats);
        tvName = findViewById(R.id.tv_daily_pet_name);
        TextView tvTitle = findViewById(R.id.tv_toolbar_title);

        // 2. 接收 Intent 資料
        selectedPetId = getIntent().getIntExtra("PET_ID", -1);
        String petName = getIntent().getStringExtra("PET_NAME");

        // 3. 設定顯示內容
        if (tvName != null) tvName.setText(petName);
        if (tvTitle != null) tvTitle.setText("今日進食情況");

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // 4. 跳轉按鈕
        findViewById(R.id.btn_add_extra_record).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddExtraFeedingActivity.class);
            intent.putExtra("PET_ID", selectedPetId);
            intent.putExtra("PET_NAME", petName);
            feedingLauncher.launch(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchTodayStatsFromDB();
    }

    private void fetchTodayStatsFromDB() {
        if (selectedPetId == -1) return;
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        new Thread(() -> {
            int totalCals = 0;
            int totalWater = 0;
            int dbGoalCals = 0;
            int dbGoalWater = 0;

            try (Connection conn = ConnectionHelper.getConnection()) {
                // 抓取目標值
                String sqlGoal = "SELECT RecommendCalories, RecommendWater FROM Pets WHERE PetID = ?";
                try (PreparedStatement pstmt0 = conn.prepareStatement(sqlGoal)) {
                    pstmt0.setInt(1, selectedPetId);
                    try (ResultSet rs0 = pstmt0.executeQuery()) {
                        if (rs0.next()) {
                            dbGoalCals = rs0.getInt("RecommendCalories");
                            dbGoalWater = rs0.getInt("RecommendWater");
                        }
                    }
                }

                // 統計今日熱量
                String sqlFood = "SELECT SUM(Calories) FROM DailyFood WHERE PetID = ? AND RecordDate = ?";
                try (PreparedStatement pstmt1 = conn.prepareStatement(sqlFood)) {
                    pstmt1.setInt(1, selectedPetId);
                    pstmt1.setString(2, today);
                    try (ResultSet rs1 = pstmt1.executeQuery()) {
                        if (rs1.next()) totalCals = rs1.getInt(1);
                    }
                }

                // 統計今日飲水
                String sqlWater = "SELECT SUM(WaterML) FROM DailyWater WHERE PetID = ? AND RecordDate = ?";
                try (PreparedStatement pstmt2 = conn.prepareStatement(sqlWater)) {
                    pstmt2.setInt(1, selectedPetId);
                    pstmt2.setString(2, today);
                    try (ResultSet rs2 = pstmt2.executeQuery()) {
                        if (rs2.next()) totalWater = rs2.getInt(1);
                    }
                }

                // 更新 UI
                final int fCals = totalCals;
                final int fGoalCals = dbGoalCals > 0 ? dbGoalCals : 1;
                final int fWater = totalWater;
                final int fGoalWater = dbGoalWater > 0 ? dbGoalWater : 1;

                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        this.currentCals = fCals;
                        this.goalCals = fGoalCals;
                        this.currentWater = fWater;
                        this.goalWater = fGoalWater;
                        refreshProgress();
                    }
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void refreshProgress() {
        // 更新食物進度
        int foodProgress = (int) ((currentCals / (float) goalCals) * 100);
        pbFood.setProgress(foodProgress);
        tvFoodStats.setText(currentCals + "/" + goalCals + " 大卡");

        // 檢查異常數據
        checkAnomaliesLocally();

        // 更新飲水進度
        int waterProgress = (int) ((currentWater / (float) goalWater) * 100);
        pbWater.setProgress(waterProgress);
        tvWaterStats.setText(currentWater + "/" + goalWater + " ml");
    }

    private void checkAnomaliesLocally() {
        new Thread(() -> {
            try (Connection conn = ConnectionHelper.getConnection()) {
                List<Float> intakeHistory = new ArrayList<>();
                String sqlHistory = "SELECT TOP 8 Calories FROM DailyFood WHERE PetID = ? ORDER BY RecordDate DESC";
                try (PreparedStatement pstmt = conn.prepareStatement(sqlHistory)) {
                    pstmt.setInt(1, selectedPetId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) intakeHistory.add(rs.getFloat(1));
                    }
                }

                if (intakeHistory.isEmpty()) return;

                float today = intakeHistory.get(0);
                StringBuilder alerts = new StringBuilder();

                // 1. 與平均比較
                if (intakeHistory.size() > 1) {
                    float sum = 0;
                    for (int i = 1; i < intakeHistory.size(); i++) sum += intakeHistory.get(i);
                    float avg = sum / (intakeHistory.size() - 1);
                    if (today < avg * 0.85) {
                        alerts.append(String.format("⚠️ 食量較七日平均下降 %.0f%%\n", (1 - today/avg)*100));
                    }
                }

                // 2. 連續下降
                if (intakeHistory.size() >= 4) {
                    if (intakeHistory.get(0) < intakeHistory.get(1) && 
                        intakeHistory.get(1) < intakeHistory.get(2) && 
                        intakeHistory.get(2) < intakeHistory.get(3)) {
                        alerts.append("⚠️ 偵測到進食量連續 3 天下降\n");
                    }
                }

                // 3. 低於建議量
                if (today < goalCals * 0.8) {
                    alerts.append("⚠️ 今日進食量未達標 (低於 80%)\n");
                }

                final String msg = alerts.toString();
                if (!msg.isEmpty()) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        // 也可以考慮在 UI 上加一個警示 icon
                    });
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }
}