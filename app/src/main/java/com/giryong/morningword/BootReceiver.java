package com.giryong.morningword;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** 폰을 껐다 켜거나 앱을 업데이트하면 알람이 지워지므로 다시 예약한다. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        AlarmScheduler.schedule(c);
    }
}
