package com.example.pet_app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class PetAlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "pet_event_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        // 抓取我們排定行程時塞進來的文字
        String eventTitle = intent.getStringExtra("EVENT_TITLE");
        if (eventTitle == null) eventTitle = "您有一個寵物行程！";

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Android 8.0 以上需要建立通知頻道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "寵物行程通知",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("用於提醒寵物的行事曆行程");
            if (manager != null) manager.createNotificationChannel(channel);
        }

        // 建立推播通知的外觀
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.btn_star_big_on)
                .setContentTitle("🐾 寵物行程提醒")
                .setContentText(eventTitle)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        // 發射通知！
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }

        // 🚀 關鍵新增：讓郵差順便把這則行程，寫進我們首頁鈴鐺的暫存紀錄裡！
        LocalNotificationHelper.addNotification(context, "【行程】" + eventTitle);
    }
}