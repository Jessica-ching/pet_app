package com.example.pet_app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.pet_app.login.LoginActivity;
import com.example.pet_app.mainfeature.calender.EventModel;
import com.example.pet_app.mainfeature.record.DataManager;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.imageview.ShapeableImageView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private boolean isLoggedIn = false;
    private TextView tvPetName, tvCaloriesHint, txtWelcome;
    private ShapeableImageView imgPetAvatar;
    private LineChart lineChart;
    private TextView tvPlaceholder;

    private int currentUserId = -1;
    private List<PetModel> userPetList = new ArrayList<>();
    private int selectedPetID = -1; // 當前選中的寵物 ID
    private String selectedPetName = "";

    public HomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // 綁定 UI
        tvPetName = view.findViewById(R.id.tv_pet_name);
        txtWelcome = view.findViewById(R.id.tv_title);
        imgPetAvatar = view.findViewById(R.id.img_pet_avatar);
        lineChart = view.findViewById(R.id.chart_view);
        tvPlaceholder = view.findViewById(R.id.tv_login_hint);
        // tvCaloriesHint = view.findViewById(R.id.tv_calories_hint); //假設你有這個顯示熱量的 TextView
        LinearLayout petSelector = view.findViewById(R.id.pet_selector);

        petSelector.setOnClickListener(this::showPetListPopup);

        ImageView ivFoodTab = view.findViewById(R.id.iv_food_tab);
        ImageView ivWaterTab = view.findViewById(R.id.iv_water_tab);

        ivFoodTab.setOnClickListener(v -> {
            ivFoodTab.setAlpha(1.0f);
            ivWaterTab.setAlpha(0.3f);
            loadChartDataFromDB("food");
        });

        ivWaterTab.setOnClickListener(v -> {
            ivFoodTab.setAlpha(0.3f);
            ivWaterTab.setAlpha(1.0f);
            loadChartDataFromDB("water");
        });

        // 在 tvPlaceholder 的初始化後加入（如果 XML 裡那個提示是可點擊的）
        tvPlaceholder.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
        });

        // --- 新增：處理鈴鐺點擊 ---
        View btnNotification = view.findViewById(R.id.btn_notification);
        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> {
                String[] options = {"設定餵食提醒", "設定門診提醒", "取消所有提醒"};

                new android.app.AlertDialog.Builder(requireContext())
                        .setTitle("通知設定")
                        .setItems(options, (dialog, which) -> {
                            if (which == 0 || which == 1) {
                                showTimePickerDialog(options[which]);
                            } else {
                                Toast.makeText(getContext(), "提醒已關閉", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .show();
            });
        }
        // --- 新增結束 ---

        checkStatusAndRefreshUI();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次 Fragment 恢復顯示時（例如登入回來），都重新檢查狀態
        checkStatusAndRefreshUI();
    }

    public void checkStatusAndRefreshUI() {
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        this.isLoggedIn = prefs.getBoolean("isLoggedIn", false);
        this.currentUserId = prefs.getInt("UserID", -1);

        View view = getView();
        if (view == null) return;

        LinearLayout layoutLoginHint = view.findViewById(R.id.layout_login_hint);

        if (isLoggedIn) {
            if (layoutLoginHint != null) layoutLoginHint.setVisibility(View.GONE);
            lineChart.setVisibility(View.VISIBLE);

            String userName = prefs.getString("UserName", "用戶");
            if (txtWelcome != null) {
                txtWelcome.setText(userName + " 的寵物健康週報");
            }

            loadChartData();
        } else {
            if (layoutLoginHint != null) layoutLoginHint.setVisibility(View.VISIBLE);
            lineChart.setVisibility(View.GONE);

            if (txtWelcome != null) {
                txtWelcome.setText("請先登入以查看資料");
            }
            clearChart();
        }
    }

    private void loadChartData() {
        if (selectedPetID != -1) {
            loadChartDataFromDB("food");
        } else {
            fetchUserPets();
        }
    }

    private void clearChart() {
        lineChart.clear();
        lineChart.setNoDataText("請先登入以查看資料");
        lineChart.invalidate();
    }

    // 🌟 從資料庫抓取該使用者的所有寵物
    private void fetchUserPets() {
        if (currentUserId == -1) return;
        new Thread(() -> {
            try (Connection conn = ConnectionHelper.getConnection()) {
                String sql = "SELECT PetID, PetName, Species FROM Pets WHERE UserID = ?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, currentUserId);
                ResultSet rs = pstmt.executeQuery();

                List<PetModel> tempList = new ArrayList<>();
                while (rs.next()) {
                    tempList.add(new PetModel(rs.getInt("PetID"), rs.getString("PetName"), rs.getString("Species")));
                }

                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        userPetList.clear();
                        userPetList.addAll(tempList);

                        if (!userPetList.isEmpty()) {
                            // 🌟 這裡最重要：抓到寵物後，主動選中第一隻並載入圖表
                            updatePetSelection(userPetList.get(0));
                        } else {
                            tvPetName.setText("尚未新增寵物");
                            lineChart.setNoDataText("點擊此處新增您的第一隻寵物");
                            lineChart.invalidate();
                        }
                    });
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void showPetListPopup(View anchor) {
        PopupMenu popup = new PopupMenu(getContext(), anchor);
        if (isLoggedIn) {
            for (PetModel pet : userPetList) {
                popup.getMenu().add(pet.name);
            }
            popup.getMenu().add("+ 新增寵物");
        } else {
            popup.getMenu().add("請先登入");
        }

        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.equals("請先登入")) {
                startActivity(new Intent(getActivity(), LoginActivity.class));
            } else if (title.equals("+ 新增寵物")) {
                // 跳轉新增頁面
            } else {
                for (PetModel pet : userPetList) {
                    if (pet.name.equals(title)) {
                        updatePetSelection(pet);
                        break;
                    }
                }
            }
            return true;
        });
        popup.show();
    }

    private void updatePetSelection(PetModel pet) {
        this.selectedPetID = pet.id;
        this.selectedPetName = pet.name;
        tvPetName.setText(pet.name);
        imgPetAvatar.setImageResource(pet.species.equals("貓") ? R.drawable.cat_placeholder : R.drawable.dog_placeholder);

        // 載入該寵物的建議熱量與圖表
        fetchPetDailyGoal();
        loadChartDataFromDB("food");
    }

    // 🌟 從 DailyFood 抓取今天的建議熱量
    private void fetchPetDailyGoal() {
        new Thread(() -> {
            try (Connection conn = ConnectionHelper.getConnection()) {
                String sql = "SELECT TOP 1 Calories FROM DailyFood WHERE PetID = ? ORDER BY RecordDate DESC";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, selectedPetID);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    int cals = rs.getInt("Calories");
                    if (isAdded() && getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (tvCaloriesHint != null) tvCaloriesHint.setText("今日建議攝取：" + cals + " kcal");
                        });
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    // 🌟 核心：從資料庫撈取圖表數據 (熱量/水)
    private void loadChartDataFromDB(String type) {
        if (selectedPetID == -1) return;

        new Thread(() -> {
            List<Entry> entries = new ArrayList<>();
            ArrayList<String> xLabels = new ArrayList<>();
            String unit = type.equals("food") ? "kcal" : "ml";

            try (Connection conn = ConnectionHelper.getConnection()) {
                String table = type.equals("food") ? "DailyFood" : "DailyWater";
                String column = type.equals("food") ? "Calories" : "WaterML";

                // 🌟 修正：抓取「最新」的 7 筆，並確保按日期正向排列給圖表
                String sql = "SELECT " + column + ", RecordDate FROM (" +
                        "SELECT TOP 7 " + column + ", RecordDate FROM " + table +
                        " WHERE PetID = ? ORDER BY RecordDate DESC) AS Temp " +
                        "ORDER BY RecordDate ASC";

                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, selectedPetID);
                ResultSet rs = pstmt.executeQuery();

                int index = 0;
                while (rs.next()) {
                    entries.add(new Entry(index++, rs.getFloat(1)));
                    String date = rs.getString("RecordDate");
                    if (date != null && date.length() >= 10) {
                        xLabels.add(date.substring(5, 10)); // 擷取 MM-dd
                    }
                }

                // 🌟 安全檢查
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> updateChartUI(entries, xLabels, unit));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updateChartUI(List<Entry> entries, ArrayList<String> xLabels, String unitName) {
        if (entries.isEmpty()) {
            lineChart.clear();
            lineChart.setNoDataText("暫無紀錄資料");
            lineChart.invalidate();
            return;
        }
        LineDataSet dataSet = new LineDataSet(entries, selectedPetName + " 的紀錄");
        dataSet.setColor(Color.parseColor("#E6B34D"));
        dataSet.setCircleColor(Color.parseColor("#E6B34D"));
        dataSet.setLineWidth(3f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#FFF3E0"));

        lineChart.setData(new LineData(dataSet));
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        lineChart.getDescription().setText("單位：" + unitName);
        lineChart.invalidate();
        lineChart.animateY(800);
    }

    // --- 新增：彈出時間選擇器 ---
    private void showTimePickerDialog(String title) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);
        int minute = calendar.get(java.util.Calendar.MINUTE);

        android.app.TimePickerDialog timePickerDialog = new android.app.TimePickerDialog(requireContext(),
                (view, selectedHour, selectedMinute) -> {
                    String timeString = selectedHour + ":" + String.format("%02d", selectedMinute);
                    Toast.makeText(requireContext(),
                            title + " 已成功設定在 " + timeString,
                            Toast.LENGTH_LONG).show();
                }, hour, minute, true);

        timePickerDialog.setTitle(title);
        timePickerDialog.show();
    }
    // --- 新增結束 ---

    // 寵物簡單模型
    class PetModel {
        int id; String name, species;
        PetModel(int id, String name, String species) { this.id = id; this.name = name; this.species = species; }
    }
}