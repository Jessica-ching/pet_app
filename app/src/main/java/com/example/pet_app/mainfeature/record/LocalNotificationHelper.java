package com.example.pet_app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;

public class LocalNotificationHelper {
    private static final String PREF_NAME = "NotificationPrefs";
    private static final String KEY_NOTIFS = "notifications_list";

    // 新增一筆通知 (會自動把最新的擠到最上面)
    public static void addNotification(Context context, String message) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String jsonString = prefs.getString(KEY_NOTIFS, "[]");

        try {
            JSONArray oldArray = new JSONArray(jsonString);
            JSONArray newArray = new JSONArray();

            newArray.put(message); // 把最新的塞在第 0 個位子

            // 把舊的接在後面 (最多保留 20 筆，才不會爆掉)
            int limit = Math.min(oldArray.length(), 19);
            for (int i = 0; i < limit; i++) {
                newArray.put(oldArray.getString(i));
            }

            // 存回手機本地端
            prefs.edit().putString(KEY_NOTIFS, newArray.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 讀取所有通知給 ListView 用
    public static List<String> getNotifications(Context context) {
        List<String> list = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        try {
            JSONArray jsonArray = new JSONArray(prefs.getString(KEY_NOTIFS, "[]"));
            for (int i = 0; i < jsonArray.length(); i++) {
                list.add(jsonArray.getString(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 如果目前完全沒通知，塞一個預設提示
        if (list.isEmpty()) {
            list.add("目前沒有任何新通知喔！");
        }

        return list;
    }
}