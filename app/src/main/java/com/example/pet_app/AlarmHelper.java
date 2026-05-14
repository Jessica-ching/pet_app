package com.example.pet_app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import java.util.Calendar;

public class AlarmHelper {

    // 呼叫這個方法就能排定通知
    public static void scheduleEvent(Context context, int eventId, String title,
                                     int year, int month, int day, int hour, int minute) {

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, PetAlarmReceiver.class);
        intent.putExtra("EVENT_TITLE", title);

        // 打包成 PendingIntent (FLAG_IMMUTABLE 是新版 Android 必備的)
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                eventId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 設定觸發時間
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, day, hour, minute, 0); // Calendar 的月份是 0~11 所以要減 1

        // 如果設定的時間已經過去了，就不要設鬧鐘
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            return;
        }

        // 設定精準鬧鐘
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
}