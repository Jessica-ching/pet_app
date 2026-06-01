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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pet_app.login.LoginActivity;
import com.example.pet_app.mainfeature.calender.EventAdapter;
import com.example.pet_app.mainfeature.calender.EventModel;
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
    private TextView tvPetName, txtWelcome, tvCaloriesHint;
    private ShapeableImageView imgPetAvatar;
    private LineChart lineChart;
    private TextView tvPlaceholder;

    // 首頁專用的清單與 Adapter
    private RecyclerView rvHomeEvents;
    private EventAdapter homeEventAdapter;

    private int currentUserId = -1;
    private List<PetModel> userPetList = new ArrayList<>();
    private int selectedPetID = -1;
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
        LinearLayout petSelector = view.findViewById(R.id.pet_selector);

        // 綁定首頁的清單 (RecyclerView)
        rvHomeEvents = view.findViewById(R.id.rv_home_events);
        if (rvHomeEvents != null) {
            rvHomeEvents.setLayoutManager(new LinearLayoutManager(getContext()));
            homeEventAdapter = new EventAdapter(new ArrayList<>());
            rvHomeEvents.setAdapter(homeEventAdapter);
        }

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

        tvPlaceholder.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
        });

        View btnNotification = view.findViewById(R.id.btn_notification);
        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> {
                showNotificationHistory();
            });
        }

        checkStatusAndRefreshUI();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
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

            // 抓取當日行程，塞給首頁清單
            fetchTodayEventsList();

        } else {
            if (layoutLoginHint != null) layoutLoginHint.setVisibility(View.VISIBLE);
            lineChart.setVisibility(View.GONE);
            if (txtWelcome != null) txtWelcome.setText("請先登入以查看資料");
            clearChart();
            if (homeEventAdapter != null) homeEventAdapter.updateList(new ArrayList<>());
        }
    }

    // 🌟 直接撈取當日行程，並使用你原本的 EventModel 格式！
    private void fetchTodayEventsList() {
        if (currentUserId == -1 || rvHomeEvents == null) return;

        new Thread(() -> {
            List<EventModel> todayEvents = new ArrayList<>();
            try (Connection conn = ConnectionHelper.getConnection()) {
                String sql = "SELECT Title, EventTime FROM Events " +
                        "WHERE UserID = ? AND CAST(EventDate AS DATE) = CAST(GETDATE() AS DATE) " +
                        "ORDER BY EventTime ASC";

                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, currentUserId);
                ResultSet rs = pstmt.executeQuery();

                // 取得今天的日期字串，例如 "01"
                java.util.Calendar calendar = java.util.Calendar.getInstance();
                String todayStr = String.format("%02d", calendar.get(java.util.Calendar.DAY_OF_MONTH));

                while (rs.next()) {
                    String title = rs.getString("Title");
                    String time = rs.getString("EventTime");

                    // 🚀 重點：直接使用你原本寫好的 EventModel 建構子，什麼都不用改！
                    EventModel event = new EventModel(
                            "2026/06/" + todayStr, // Date
                            "kk",                  // Title (這裡先放寵物名 kk)
                            title,                 // Subtitle (行程名稱，如 看醫生)
                            0,                     // IconResId
                            false,                 // isDone
                            time                   // TimeTag (時間)
                    );
                    todayEvents.add(event);
                }

                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        homeEventAdapter.updateList(todayEvents);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // ========== 下面都是你原本的圖表、寵物資料與鈴鐺通知代碼，一字未改 ==========

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

        fetchPetDailyGoal();
        loadChartDataFromDB("food");
    }

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
                            // tvCaloriesHint.setText(...)
                        });
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void loadChartDataFromDB(String type) {
        if (selectedPetID == -1) return;

        new Thread(() -> {
            List<Entry> entries = new ArrayList<>();
            ArrayList<String> xLabels = new ArrayList<>();
            String unit = type.equals("food") ? "kcal" : "ml";

            try (Connection conn = ConnectionHelper.getConnection()) {
                String table = type.equals("food") ? "DailyFood" : "DailyWater";
                String column = type.equals("food") ? "Calories" : "WaterML";

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
                        xLabels.add(date.substring(5, 10));
                    }
                }

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

    class PetModel {
        int id; String name, species;
        PetModel(int id, String name, String species) { this.id = id; this.name = name; this.species = species; }
    }

    private void showNotificationHistory() {
        if (getContext() == null) return;

        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(getContext());

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        layout.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(getContext());
        title.setText("通知紀錄");
        title.setTextSize(20f);
        title.setTextColor(Color.BLACK);
        title.setPadding(0, 0, 0, 30);
        layout.addView(title);

        android.widget.ListView listView = new android.widget.ListView(getContext());

        java.util.List<String> realNotifications = LocalNotificationHelper.getNotifications(getContext());

        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_list_item_1,
                realNotifications
        );
        listView.setAdapter(adapter);
        layout.addView(listView);

        bottomSheetDialog.setContentView(layout);
        bottomSheetDialog.show();
    }
}