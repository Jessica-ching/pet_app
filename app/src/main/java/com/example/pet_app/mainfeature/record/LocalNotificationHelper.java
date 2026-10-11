package com.example.pet_app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;

public class LocalNotificationHelper {
    private static final String PREF_NAME = "NotificationPrefs";
    private static final String KEY_NOTIFS = "notifications_list";
    private static final String CHANNEL_ID = "pet_health_channel";

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

    // 發送系統通知並存入本地紀錄
    public static void triggerHealthAlert(Context context, String petName, String alertMsg) {
        String fullMsg = "【健康警示】" + petName + ": " + alertMsg;
        
        // 1. 存入本地清單
        addNotification(context, fullMsg);

        // 2. 發送系統推播
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "寵物健康提醒", NotificationManager.IMPORTANCE_HIGH);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("🐾 寵物健康異常警示")
                .setContentText(fullMsg)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
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