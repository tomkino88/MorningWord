package com.giryong.morningword;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Calendar;

/** 알람 시간 저장과 다음 알람 예약을 맡는다. */
public final class AlarmScheduler {
    private AlarmScheduler() {}

    public static final String PREFS = "morning_prefs";
    public static final String K_HOUR = "hour";
    public static final String K_MIN = "minute";
    public static final String K_ON = "enabled";
    public static final String K_WEEKDAY = "weekdays_only";

    public static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** 다음에 울릴 시각(밀리초). */
    public static long nextTrigger(Context c) {
        SharedPreferences p = prefs(c);
        int h = p.getInt(K_HOUR, 7);
        int m = p.getInt(K_MIN, 0);
        boolean weekdays = p.getBoolean(K_WEEKDAY, true);

        Calendar now = Calendar.getInstance();
        Calendar t = Calendar.getInstance();
        t.set(Calendar.HOUR_OF_DAY, h);
        t.set(Calendar.MINUTE, m);
        t.set(Calendar.SECOND, 0);
        t.set(Calendar.MILLISECOND, 0);
        if (!t.after(now)) t.add(Calendar.DAY_OF_MONTH, 1);
        if (weekdays) {
            while (t.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
                    || t.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                t.add(Calendar.DAY_OF_MONTH, 1);
            }
        }
        return t.getTimeInMillis();
    }

    private static PendingIntent alarmIntent(Context c) {
        Intent i = new Intent(c, AlarmReceiver.class);
        return PendingIntent.getBroadcast(c, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** 켜져 있으면 다음 알람을 예약하고, 꺼져 있으면 취소한다. */
    public static void schedule(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = alarmIntent(c);
        if (!prefs(c).getBoolean(K_ON, false)) {
            am.cancel(pi);
            return;
        }
        long at = nextTrigger(c);
        boolean exactOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms();
        try {
            if (exactOk) {
                Intent open = new Intent(c, MainActivity.class);
                PendingIntent show = PendingIntent.getActivity(c, 2, open,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                am.setAlarmClock(new AlarmManager.AlarmClockInfo(at, show), pi);
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            }
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        }
    }
}
