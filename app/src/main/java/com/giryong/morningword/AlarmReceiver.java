package com.giryong.morningword;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Random;

/** 알람 시간이 되면 문구를 골라 알림으로 띄우고, 다음 날 알람을 다시 예약한다. */
public class AlarmReceiver extends BroadcastReceiver {
    public static final String CHANNEL = "morning_word";
    public static final String EXTRA_TEST = "test";
    public static final String EXTRA_INDEX = "phrase_index";

    @Override
    public void onReceive(Context c, Intent intent) {
        int idx = Phrases.randomIndex(Phrases.ALL, -1, new Random());
        showNotification(c, idx);
        if (!intent.getBooleanExtra(EXTRA_TEST, false)) {
            AlarmScheduler.schedule(c);
        }
    }

    public static void ensureChannel(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null || nm.getNotificationChannel(CHANNEL) != null) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL, "아침 응원 문구",
                NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("정한 시간에 오늘의 응원 문구를 보내드려요");
        ch.enableVibration(true);
        nm.createNotificationChannel(ch);
    }

    static void showNotification(Context c, int idx) {
        ensureChannel(c);
        String[] p = Phrases.LIST[idx];

        Intent open = new Intent(c, MainActivity.class)
                .putExtra(EXTRA_INDEX, idx)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 1, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification n = new Notification.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_notify)
                .setContentTitle("좋은 아침이에요 · " + p[0] + " 한마디")
                .setContentText(p[1])
                .setStyle(new Notification.BigTextStyle().bigText(p[1]))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .build();

        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm != null) {
            try {
                nm.notify(1001, n);
            } catch (SecurityException ignored) {
                // 알림 권한이 없으면 표시되지 않는다.
            }
        }
    }
}
