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
                PreparedStatement pstmt0 = conn.prepareStatement(sqlGoal);
                pstmt0.setInt(1, selectedPetId);
                ResultSet rs0 = pstmt0.executeQuery();
                if (rs0.next()) {
                    dbGoalCals = rs0.getInt("RecommendCalories");
                    dbGoalWater = rs0.getInt("RecommendWater");
                }

                // 統計今日熱量
                String sqlFood = "SELECT SUM(Calories) FROM DailyFood WHERE PetID = ? AND RecordDate = ?";
                PreparedStatement pstmt1 = conn.prepareStatement(sqlFood);
                pstmt1.setInt(1, selectedPetId);
                pstmt1.setString(2, today);
                ResultSet rs1 = pstmt1.executeQuery();
                if (rs1.next()) totalCals = rs1.getInt(1);

                // 更新 UI
                final int fCals = totalCals;
                final int fGoalCals = dbGoalCals > 0 ? dbGoalCals : 1;
                runOnUiThread(() -> {
                    this.currentCals = fCals;
                    this.goalCals = fGoalCals;
                    refreshProgress();
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void refreshProgress() {
        int foodProgress = (int) ((currentCals / (float) goalCals) * 100);
        pbFood.setProgress(foodProgress);
        tvFoodStats.setText(currentCals + "/" + goalCals + " 大卡");
    }
}