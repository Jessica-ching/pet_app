package com.example.pet_app.mainfeature.calender;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pet_app.ConnectionHelper;
import com.example.pet_app.HomeActivity;
import com.example.pet_app.R;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class CalendarFragment extends Fragment {

    private RecyclerView rvEvents;
    private EventAdapter adapter;
    private MaterialCalendarView calendarView;
    private HashSet<CalendarDay> eventDates = new HashSet<>();
    private List<EventModel> allDbEvents = new ArrayList<>(); // 從 DB 讀取的所有行程
    private ActivityResultLauncher<Intent> addEventLauncher;
    private CalendarDay lastSelectedDate = null;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/M/d", Locale.getDefault());
    private int currentUserId = -1;

    public CalendarFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 取得 UserID
        SharedPreferences prefs = getActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        currentUserId = prefs.getInt("UserID", -1);

        addEventLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // 新增成功後，重新從資料庫讀取
                        fetchAllEventsFromDB();
                    }
                }
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        calendarView = view.findViewById(R.id.calendarView);
        rvEvents = view.findViewById(R.id.rv_events);
        View btnAddEvent = view.findViewById(R.id.btn_add_event);

        rvEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new EventAdapter(new ArrayList<>());
        rvEvents.setAdapter(adapter);

        // 1. 從資料庫讀取資料
        fetchAllEventsFromDB();

        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            if (!selected || date.equals(lastSelectedDate)) {
                calendarView.setSelectedDate((CalendarDay) null);
                lastSelectedDate = null;
                showTenDaysEvents();
            } else {
                lastSelectedDate = date;
                updateSingleDayEvents(date);
            }
        });

        btnAddEvent.setOnClickListener(v -> {
            if (currentUserId != -1) {
                Intent intent = new Intent(getActivity(), AddEventActivity.class);
                addEventLauncher.launch(intent);
            } else {
                Toast.makeText(getContext(), "請先登入帳號才能管理行程", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次回到日曆畫面（例如新增完回來），重新從資料庫抓取所有事件日期
        fetchAllEventsFromDB();
    }

    /**
     * 🌟 核心方法：從 SQL Server 撈取醫療紀錄(Medical)與花費紀錄(Costs)作為行程
     */
    private void fetchAllEventsFromDB() {
        if (currentUserId == -1) return;

        new Thread(() -> {
            List<EventModel> tempEvents = new ArrayList<>();
            try (Connection conn = ConnectionHelper.getConnection()) {
                // 1. 撈取醫療紀錄 (關聯 Pets 表獲取名字)
                String medicalSql = "SELECT m.Date, p.PetName, m.Category, m.Dascription " +
                        "FROM Medical m JOIN Pets p ON m.PetID = p.PetID " +
                        "WHERE p.UserID = ? ORDER BY m.Date DESC";
                PreparedStatement pstmt1 = conn.prepareStatement(medicalSql);
                pstmt1.setInt(1, currentUserId);
                ResultSet rs1 = pstmt1.executeQuery();
                while (rs1.next()) {
                    // 將 SQL Date 轉為 yyyy/M/d 格式串
                    String date = rs1.getString("Date").replace("-", "/");
                    tempEvents.add(new EventModel(date, rs1.getString("PetName"),
                            rs1.getString("Category") + ": " + rs1.getString("Dascription"),
                            R.drawable.ic_pill, false, "09:00"));
                }

                // 2. 撈取花費紀錄
                String costSql = "SELECT c.Date, p.PetName, c.Category, c.Amount " +
                        "FROM Costs c JOIN Pets p ON c.PetID = p.PetID " +
                        "WHERE p.UserID = ? ORDER BY c.Date DESC";
                PreparedStatement pstmt2 = conn.prepareStatement(costSql);
                pstmt2.setInt(1, currentUserId);
                ResultSet rs2 = pstmt2.executeQuery();
                while (rs2.next()) {
                    String date = rs2.getString("Date").replace("-", "/");
                    tempEvents.add(new EventModel(date, rs2.getString("PetName"),
                            "支出: " + rs2.getString("Category") + " $" + rs2.getFloat("Amount"),
                            R.drawable.ic_record, false, "12:00"));
                }

                allDbEvents = tempEvents;

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        refreshCalendarDecorators();
                        showTenDaysEvents(); // 預設顯示未來10天
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updateSingleDayEvents(CalendarDay selectedDate) {
        List<EventModel> filteredList = new ArrayList<>();
        String targetDate = formatDate(selectedDate);
        for (EventModel event : allDbEvents) {
            if (event.getDate().equals(targetDate)) {
                filteredList.add(event);
            }
        }
        adapter.updateList(filteredList);
    }

    private void showTenDaysEvents() {
        List<EventModel> filteredList = new ArrayList<>();
        long now = System.currentTimeMillis();
        long tenDaysLater = now + (10L * 24 * 60 * 60 * 1000);

        for (EventModel event : allDbEvents) {
            try {
                Date d = sdf.parse(event.getDate());
                if (d != null && d.getTime() >= (now - 86400000) && d.getTime() <= tenDaysLater) {
                    filteredList.add(event);
                }
            } catch (Exception ignored) {}
        }
        adapter.updateList(filteredList);
    }

    private void refreshCalendarDecorators() {
        eventDates.clear();
        for (EventModel event : allDbEvents) {
            // 資料庫存的是 2026-04-28，所以用 "-" 切割
            String[] parts = event.getDate().split("-");
            if (parts.length == 3) {
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                int day = Integer.parseInt(parts[2]);
                // 注意：某些 library 的 Month 是從 0 開始，MaterialCalendarView 通常是 1-12
                eventDates.add(CalendarDay.from(year, month, day));
            }
        }
        calendarView.removeDecorators();
        calendarView.addDecorator(new EventDecorator(Color.parseColor("#E6B34D"), eventDates));
    }

    private String formatDate(CalendarDay date) {
        return date.getYear() + "/" + date.getMonth() + "/" + date.getDay();
    }

    public class EventDecorator implements DayViewDecorator {
        private final int color;
        private final Collection<CalendarDay> dates;
        public EventDecorator(int color, Collection<CalendarDay> dates) { this.color = color; this.dates = dates; }
        @Override public boolean shouldDecorate(CalendarDay day) { return dates.contains(day); }
        @Override public void decorate(DayViewFacade view) { view.addSpan(new DotSpan(8, color)); }
    }
}