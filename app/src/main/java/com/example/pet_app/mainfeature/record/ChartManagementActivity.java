package com.example.pet_app.mainfeature.record;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import com.example.pet_app.ConnectionHelper;
import com.example.pet_app.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChartManagementActivity extends AppCompatActivity {

    private TextView tvToolbarTitle, tvSelectedDateRange;
    private ImageButton ivFood, ivWater, ivWeight, ivTemp;
    private LineChart lineChart;
    private String currentType = "food";
    private int selectedPetId;
    private String petName;

    // 用來儲存 SQL 查詢的日期範圍
    private String sqlStartDate = "";
    private String sqlEndDate = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart_management);

        // 1. 初始化元件
        tvToolbarTitle = findViewById(R.id.tv_toolbar_title);
        tvSelectedDateRange = findViewById(R.id.tv_selected_date_range);
        ivFood = findViewById(R.id.iv_food_tab);
        ivWater = findViewById(R.id.iv_water_tab);
        ivWeight = findViewById(R.id.iv_weight_tab);
        ivTemp = findViewById(R.id.iv_temp_tab);
        lineChart = findViewById(R.id.chart_view_management);
        LinearLayout layoutDateRange = findViewById(R.id.layout_date_range);

        // 2. 接收傳值 (從 RecordFragment 傳來的 ID)
        selectedPetId = getIntent().getIntExtra("PET_ID", -1);
        petName = getIntent().getStringExtra("PET_NAME");
        if (petName != null) {
            tvToolbarTitle.setText(petName + " 狀態圖表管理");
        }

        // 預設日期區間 (最近 7 天)
        setupDefaultDateRange();

        // 3. 設定監聽器
        layoutDateRange.setOnClickListener(v -> showDateRangePicker());
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        ivFood.setOnClickListener(v -> { currentType = "food"; updateIconStyles(); refreshChartData(); });
        ivWater.setOnClickListener(v -> { currentType = "water"; updateIconStyles(); refreshChartData(); });
        ivWeight.setOnClickListener(v -> { currentType = "weight"; updateIconStyles(); refreshChartData(); });
        ivTemp.setOnClickListener(v -> { currentType = "temp"; updateIconStyles(); refreshChartData(); });

        setupChartStyle();
        refreshChartData();
    }

    private void setupDefaultDateRange() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        long now = System.currentTimeMillis();
        sqlEndDate = sdf.format(new Date(now));
        sqlStartDate = sdf.format(new Date(now - (7L * 24 * 60 * 60 * 1000))); // 7天前
        tvSelectedDateRange.setText(sqlStartDate.replace("-", "/") + " ~ " + sqlEndDate.replace("-", "/"));
    }

    private void setupChartStyle() {
        lineChart.getDescription().setEnabled(false);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getXAxis().setDrawGridLines(false);
        lineChart.getAxisRight().setEnabled(false); // 隱藏右側 Y 軸
    }

    private void showDateRangePicker() {
        MaterialDatePicker<Pair<Long, Long>> dateRangePicker =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText("選擇查詢區間")
                        .build();

        dateRangePicker.show(getSupportFragmentManager(), "DATE_RANGE_PICKER");

        dateRangePicker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            sqlStartDate = sdf.format(new Date(selection.first));
            sqlEndDate = sdf.format(new Date(selection.second));

            tvSelectedDateRange.setText(sqlStartDate.replace("-", "/") + " ~ " + sqlEndDate.replace("-", "/"));
            refreshChartData();
        });
    }

    private void refreshChartData() {
        if (selectedPetId == -1) return;

        new Thread(() -> {
            List<Entry> entries = new ArrayList<>();
            String label = "";
            int color = Color.BLUE;
            String sql = "";

            // 根據選取的 Tab 決定 SQL
            switch (currentType) {
                case "food":
                    label = "進食量 (g)";
                    color = Color.parseColor("#FFA500");
                    sql = "SELECT Calories as val, RecordDate as dt FROM DailyFood WHERE PetID = ? AND RecordDate BETWEEN ? AND ? ORDER BY RecordDate ASC";
                    break;
                case "water":
                    label = "飲水量 (ml)";
                    color = Color.parseColor("#2196F3");
                    sql = "SELECT WaterML as val, RecordDate as dt FROM DailyWater WHERE PetID = ? AND RecordDate BETWEEN ? AND ? ORDER BY RecordDate ASC";
                    break;
                case "temp":
                    label = "體溫 (℃)";
                    color = Color.RED;
                    // 從 Medical 撈取類別為生理量測且包含體溫字眼的紀錄
                    sql = "SELECT CAST(REPLACE(REPLACE(Dascription, '體溫紀錄: ', ''), ' °C', '') AS FLOAT) as val, Date as dt " +
                            "FROM Medical WHERE PetID = ? AND Category = '生理量測' AND Date BETWEEN ? AND ? ORDER BY Date ASC";
                    break;
                case "weight":
                    label = "體重 (kg)";
                    color = Color.GRAY;
                    // 註：目前 Pets 表只有當前體重，這裡假設你有一個歷史表或從 Medical 撈。
                    // 若無歷史表，則顯示當前單點或暫不顯示
                    break;
            }

            try (Connection conn = ConnectionHelper.getConnection()) {
                if (sql.isEmpty()) return;
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, selectedPetId);
                pstmt.setString(2, sqlStartDate);
                pstmt.setString(3, sqlEndDate);
                ResultSet rs = pstmt.executeQuery();

                int count = 0;
                while (rs.next()) {
                    float val = rs.getFloat("val");
                    entries.add(new Entry(count++, val));
                }

                List<Entry> finalEntries = entries;
                String finalLabel = label;
                int finalColor = color;

                runOnUiThread(() -> {
                    if (finalEntries.isEmpty()) {
                        lineChart.clear();
                        Toast.makeText(this, "此區間無資料", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    LineDataSet dataSet = new LineDataSet(finalEntries, finalLabel);
                    dataSet.setColor(finalColor);
                    dataSet.setCircleColor(finalColor);
                    dataSet.setLineWidth(2f);
                    dataSet.setValueTextSize(10f);

                    LineData lineData = new LineData(dataSet);
                    lineChart.setData(lineData);
                    lineChart.animateX(1000);
                    lineChart.invalidate();
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updateIconStyles() {
        // 更新圖標選中狀態邏輯 (略，與你原本邏輯相同)
    }
}