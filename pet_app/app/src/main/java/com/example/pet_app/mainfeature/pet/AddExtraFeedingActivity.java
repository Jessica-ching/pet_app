package com.example.pet_app.mainfeature.pet;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AddExtraFeedingActivity extends AppCompatActivity {

    private Spinner spFoodType;
    private EditText etAmount;
    private Button btnAction;
    private ArrayList<String> foodOptions = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private Map<String, Integer> foodCalorieMap = new HashMap<>();

    private int currentCals, goalCals, currentWater, goalWater, selectedPetId;
    private ProgressBar pbFood, pbWater;
    private TextView tvFoodStats, tvWaterStats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_extra_feeding);

        // UI 綁定
        spFoodType = findViewById(R.id.sp_extra_food_type);
        etAmount = findViewById(R.id.et_extra_food_amount);
        btnAction = findViewById(R.id.btn_action_feeding);
        pbFood = findViewById(R.id.pb_food_daily);
        pbWater = findViewById(R.id.pb_water_daily);
        tvFoodStats = findViewById(R.id.tv_extra_food_stats);
        tvWaterStats = findViewById(R.id.tv_extra_water_stats);

        // 接收 Intent 數據
        selectedPetId = getIntent().getIntExtra("PET_ID", -1);
        currentCals = getIntent().getIntExtra("CURRENT_CALS", 0);
        goalCals = getIntent().getIntExtra("GOAL_CALS", 250);
        currentWater = getIntent().getIntExtra("CURRENT_WATER", 0);
        goalWater = getIntent().getIntExtra("GOAL_WATER", 500);

        // 初始化選單
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, foodOptions);
        spFoodType.setAdapter(adapter);

        // 1. 從資料庫讀取食物名單
        fetchFoodDataFromDB();

        refreshProgress();

        // 2. 監聽選單切換
        spFoodType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = foodOptions.get(position);
                if (selected.equals("自定義新增...")) {
                    btnAction.setText("前往新增食物");
                    etAmount.setVisibility(View.GONE);
                } else if (selected.equals("- 請選擇種類 -")) {
                    btnAction.setText("請選擇");
                    etAmount.setVisibility(View.GONE);
                } else {
                    btnAction.setText("儲存餵食紀錄");
                    etAmount.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 3. 自定義食物回傳監聽 (雖然那邊會存資料庫，但這裡即時更新 UI 體驗更好)
        ActivityResultLauncher<Intent> customFoodLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // 重新從資料庫刷一遍最新名單
                        fetchFoodDataFromDB();
                    }
                }
        );

        btnAction.setOnClickListener(v -> {
            String currentText = btnAction.getText().toString();
            if (currentText.equals("前往新增食物")) {
                customFoodLauncher.launch(new Intent(this, AddCustomFoodActivity.class));
            } else if (currentText.equals("儲存餵食紀錄")) {
                saveFeedingToDB();
            }
        });

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    // 🌟 核心：從 MSSQL 撈取食物清單 (包含系統預設與該使用者新增的)
    private void fetchFoodDataFromDB() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        int userId = prefs.getInt("UserID", -1);

        new Thread(() -> {
            try (Connection conn = ConnectionHelper.getConnection()) {
                // 查詢該使用者的私有食物 + 系統公用食物 (UserID IS NULL)
                String sql = "SELECT FoodName, CaloriesPer100g FROM Food WHERE UserID = ? OR UserID IS NULL";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();

                ArrayList<String> tempList = new ArrayList<>();
                tempList.add("- 請選擇種類 -");
                tempList.add("自定義新增...");
                Map<String, Integer> tempMap = new HashMap<>();

                while (rs.next()) {
                    String name = rs.getString("FoodName");
                    int cal = rs.getInt("CaloriesPer100g");
                    tempList.add(name);
                    tempMap.put(name, cal);
                }

                runOnUiThread(() -> {
                    foodOptions.clear();
                    foodOptions.addAll(tempList);
                    foodCalorieMap.clear();
                    foodCalorieMap.putAll(tempMap);
                    adapter.notifyDataSetChanged();
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    // 🌟 核心：將餵食紀錄存入 DailyFood 資料表
    private void saveFeedingToDB() {
        String selectedFood = spFoodType.getSelectedItem().toString();
        String amountStr = etAmount.getText().toString().trim();

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "請輸入克數", Toast.LENGTH_SHORT).show();
            return;
        }

        int grams = Integer.parseInt(amountStr);
        int caloriesPer100g = foodCalorieMap.getOrDefault(selectedFood, 0);
        int totalCals = (caloriesPer100g * grams) / 100;

        new Thread(() -> {
            try (Connection conn = ConnectionHelper.getConnection()) {
                // 存入 DailyFood 表
                String sql = "INSERT INTO DailyFood (PetID, Calories, RecordDate) VALUES (?, ?, GETDATE())";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, selectedPetId);
                pstmt.setInt(2, totalCals);
                pstmt.executeUpdate();

                runOnUiThread(() -> {
                    Toast.makeText(this, "餵食紀錄已存檔", Toast.LENGTH_SHORT).show();
                    // 回傳讓 PetDailyStatusActivity 刷新進度條
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("ADDED_CALORIES", totalCals);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void refreshProgress() {
        pbFood.setProgress(goalCals > 0 ? (int) ((currentCals / (float) goalCals) * 100) : 0);
        tvFoodStats.setText(currentCals + "/" + goalCals + " 大卡");
        pbWater.setProgress(goalWater > 0 ? (int) ((currentWater / (float) goalWater) * 100) : 0);
        tvWaterStats.setText(currentWater + "/" + goalWater + " 毫升");
    }
}