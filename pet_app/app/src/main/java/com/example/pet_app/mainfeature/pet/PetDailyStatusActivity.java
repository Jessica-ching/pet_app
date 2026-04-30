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
import java.util.Date;
import java.util.Locale;

public class PetDailyStatusActivity extends AppCompatActivity {

    private ProgressBar pbFood, pbWater;
    private TextView tvFoodStats, tvWaterStats, tvName;

    private int selectedPetId;
    private int currentCals = 0;
    private int goalCals = 1;    // 建議：之後可以從 Pets 表的某個欄位讀取
    private int currentWater = 0;
    private int goalWater = 1;

    // 修改 Launcher：現在只需要通知我「資料存好了」，我重新從 DB 抓取最新總和即可
    private final ActivityResultLauncher<Intent> feedingLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // 不需要從 Intent 拿數字了，直接從 DB 重新統計最準確
                    fetchTodayStatsFromDB();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_daily_status);

        // 初始化 UI
        pbFood = findViewById(R.id.pb_food_daily);
        pbWater = findViewById(R.id.pb_water_daily);
        tvFoodStats = findViewById(R.id.tv_food_stats);
        tvWaterStats = findViewById(R.id.tv_water_stats);
        tvName = findViewById(R.id.tv_daily_pet_name);

        // 接收 ID 與名稱
        selectedPetId = getIntent().getIntExtra("PET_ID", -1);
        String petName = getIntent().getStringExtra("PET_NAME");
        tvName.setText(petName);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        findViewById(R.id.btn_add_extra_record).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddExtraFeedingActivity.class);
            intent.putExtra("PET_ID", selectedPetId);
            intent.putExtra("PET_NAME", petName);
            intent.putExtra("CURRENT_CALS", currentCals);
            intent.putExtra("GOAL_CALS", goalCals);
            intent.putExtra("CURRENT_WATER", currentWater);
            intent.putExtra("GOAL_WATER", goalWater);
            feedingLauncher.launch(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchTodayStatsFromDB(); // 每次回到此頁都刷新進度
    }

    // 🌟 核心邏輯：從資料庫統計「今日」總熱量與總水量
    private void fetchTodayStatsFromDB() {
        if (selectedPetId == -1) return;

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        new Thread(() -> {
            int totalCals = 0;
            int totalWater = 0;
            int dbGoalCals = 0;
            int dbGoalWater = 0;

            try (Connection conn = ConnectionHelper.getConnection()) {
                // 1. 抓取該寵物的「專屬目標值」 (從 Pets 表)
                // 註：請確認你的資料庫欄位名稱，這裡假設是 DailyCalories 和 DailyWater
                String sqlGoal = "SELECT RecommendCalories, RecommendWater FROM Pets WHERE PetID = ?";
                PreparedStatement pstmt0 = conn.prepareStatement(sqlGoal);
                pstmt0.setInt(1, selectedPetId);
                ResultSet rs0 = pstmt0.executeQuery();
                if (rs0.next()) {
                    dbGoalCals = rs0.getInt("RecommendCalories");
                    dbGoalWater = rs0.getInt("RecommendWater");
                }

                // 2. 統計今日總熱量
                String sqlFood = "SELECT SUM(Calories) FROM DailyFood WHERE PetID = ? AND RecordDate = ?";
                PreparedStatement pstmt1 = conn.prepareStatement(sqlFood);
                pstmt1.setInt(1, selectedPetId);
                pstmt1.setString(2, today);
                ResultSet rs1 = pstmt1.executeQuery();
                if (rs1.next()) totalCals = rs1.getInt(1);

                // 3. 統計今日總水量
                String sqlWater = "SELECT SUM(WaterML) FROM DailyWater WHERE PetID = ? AND RecordDate = ?";
                PreparedStatement pstmt2 = conn.prepareStatement(sqlWater);
                pstmt2.setInt(1, selectedPetId);
                pstmt2.setString(2, today);
                ResultSet rs2 = pstmt2.executeQuery();
                if (rs2.next()) totalWater = rs2.getInt(1);

                // 準備更新 UI
                final int finalCals = totalCals;
                final int finalWater = totalWater;
                final int finalGoalCals = dbGoalCals > 0 ? dbGoalCals : 1; // 防止目標值為 0
                final int finalGoalWater = dbGoalWater > 0 ? dbGoalWater : 1;

                runOnUiThread(() -> {
                    this.currentCals = finalCals;
                    this.currentWater = finalWater;
                    this.goalCals = finalGoalCals;   // 更新為資料庫算出的目標
                    this.goalWater = finalGoalWater; // 更新為資料庫算出的目標
                    refreshProgress();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "讀取目標設定失敗", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void refreshProgress() {
        // 食物進度
        int foodProgress = (goalCals > 0) ? (int) ((currentCals / (float) goalCals) * 100) : 0;
        pbFood.setProgress(foodProgress);
        tvFoodStats.setText(currentCals + "/" + goalCals + " 大卡");

        // 飲水進度 (假設 XML 有對應 ID)
        if (pbWater != null) {
            int waterProgress = (goalWater > 0) ? (int) ((currentWater / (float) goalWater) * 100) : 0;
            pbWater.setProgress(waterProgress);
        }
        if (tvWaterStats != null) {
            tvWaterStats.setText(currentWater + "/" + goalWater + " 毫升");
        }
    }
}