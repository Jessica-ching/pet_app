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
        if (currentUserId == -1) {
            android.util.Log.e("CalendarDebug", "UserID 為空，請檢查登入狀態");
            return;
        }

        new Thread(() -> {
            List<EventModel> tempEvents = new ArrayList<>();
            try (Connection conn = ConnectionHelper.getConnection()) {
                if (conn == null) {
                    android.util.Log.e("CalendarDebug", "資料庫連線失敗");
                    return;
                }

                // 1. 抓取一般行程 (Events)
                // 注意：EvenDate, EvenTime, Title
                String sql1 = "SELECT e.EventDate, p.PetName, e.Title, e.EventTime " +
                        "FROM Events e JOIN Pets p ON e.PetID = p.PetID WHERE p.UserID = ?";
                try (PreparedStatement ps1 = conn.prepareStatement(sql1)) {
                    ps1.setInt(1, currentUserId);
                    try (ResultSet rs1 = ps1.executeQuery()) {
                        while (rs1.next()) {
                            String d = rs1.getString("EventDate").replace("-", "/"); // 轉為 yyyy/MM/dd
                            tempEvents.add(new EventModel(d, rs1.getString("PetName"), "📅 " + rs1.getString("Title"), R.drawable.ic_record, false, rs1.getString("EventTime")));
                        }
                    }
                }

                // 2. 抓取醫療紀錄 (Medical)
                // 注意：Date, Category, Description (確認拼字是 e 還是 a)
                String sql2 = "SELECT m.Date, p.PetName, m.Category, m.Description " +
                        "FROM Medical m JOIN Pets p ON m.PetID = p.PetID WHERE p.UserID = ?";
                try (PreparedStatement ps2 = conn.prepareStatement(sql2)) {
                    ps2.setInt(1, currentUserId);
                    try (ResultSet rs2 = ps2.executeQuery()) {
                        while (rs2.next()) {
                            String d = rs2.getString("Date").replace("-", "/");
                            tempEvents.add(new EventModel(d, rs2.getString("PetName"), "🏥 " + rs2.getString("Category") + ": " + rs2.getString("Description"), R.drawable.ic_pill, false, "醫療"));
                        }
                    }
                }

                allDbEvents = tempEvents;
                android.util.Log.d("CalendarDebug", "總共抓到: " + allDbEvents.size() + " 筆資料");

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        refreshCalendarDecorators();
                        if (lastSelectedDate != null) {
                            updateSingleDayEvents(lastSelectedDate);
                        } else {
                            showTenDaysEvents();
                        }
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("CalendarDebug", "SQL 錯誤: " + e.getMessage());
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

        System.out.println("DEBUG: 點擊日期=" + targetDate);
        for (EventModel event : allDbEvents) {
            System.out.println("DEBUG: 比較清單中的日期=" + event.getDate());
        }
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
            try {
                // 因為你在 fetch 時已經 replace("-", "/") 了，這裡統一用 "/" 切割
                String[] parts = event.getDate().split("/");
                if (parts.length == 3) {
                    int year = Integer.parseInt(parts[0]);
                    int month = Integer.parseInt(parts[1]);
                    int day = Integer.parseInt(parts[2]);

                    // 這裡存入 HashSet，供 shouldDecorate 判斷
                    eventDates.add(CalendarDay.from(year, month, day));
                }
            } catch (Exception e) {
                android.util.Log.e("CalendarErr", "日期解析失敗: " + event.getDate());
            }
        }
        calendarView.removeDecorators();
        calendarView.addDecorator(new EventDecorator(Color.parseColor("#E6B34D"), eventDates));
    }

    private String formatDate(CalendarDay date) {
        // 使用 %02d 確保月份和日期都是兩位數，例如 2026/05/01
        return String.format(Locale.getDefault(), "%d/%02d/%02d",
                date.getYear(), date.getMonth(), date.getDay());
    }

    public class EventDecorator implements DayViewDecorator {
        private final int color;
        private final Collection<CalendarDay> dates;
        public EventDecorator(int color, Collection<CalendarDay> dates) { this.color = color; this.dates = dates; }
        @Override public boolean shouldDecorate(CalendarDay day) { return dates.contains(day); }
        @Override public void decorate(DayViewFacade view) { view.addSpan(new DotSpan(8, color)); }
    }
}