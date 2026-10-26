package com.giryong.morningword;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {

    private TextView phraseView, tagView, timeView, statusView;
    private final List<Button> catButtons = new ArrayList<>();
    private String cat = Phrases.ALL;
    private int current = -1;
    private final Random rnd = new Random();
    private SharedPreferences prefs;

    private int ink, muted, sun, sunInk, bgTop, bgBottom, card, line;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = AlarmScheduler.prefs(this);
        setColors();
        setContentView(buildUi());
        getWindow().setStatusBarColor(bgTop);

        if (!handleIntent(getIntent())) showPhrase(Phrases.todayIndex());
        updateTime();
        askNotificationPermission();
        AlarmReceiver.ensureChannel(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AlarmScheduler.schedule(this);
        updateStatus();
    }

    private boolean handleIntent(Intent i) {
        if (i == null || !i.hasExtra(AlarmReceiver.EXTRA_INDEX)) return false;
        int idx = i.getIntExtra(AlarmReceiver.EXTRA_INDEX, 0);
        if (idx < 0 || idx >= Phrases.LIST.length) return false;
        showPhrase(idx);
        return true;
    }

    // ---------- 색 ----------
    private void setColors() {
        boolean night = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        if (night) {
            ink = 0xFFEEF0FA; muted = 0xFFA3A9C6; sun = 0xFFF2A04A; sunInk = 0xFF1B1408;
            bgTop = 0xFF141A33; bgBottom = 0xFF3A2A2E; card = 0xFF1B2242; line = 0xFF2E3657;
        } else {
            ink = 0xFF1D2340; muted = 0xFF5D6482; sun = 0xFFE8892B; sunInk = 0xFFFFFFFF;
            bgTop = 0xFFEEF1F8; bgBottom = 0xFFFBE9D7; card = 0xFFFFFFFF; line = 0xFFD6DBEA;
        }
    }

    private int dp(float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics()));
    }

    private GradientDrawable round(int fill, int stroke, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radiusDp));
        if (stroke != 0) g.setStroke(dp(1), stroke);
        return g;
    }

    private TextView text(String s, float sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private Button pill(String label, boolean primary) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        b.setStateListAnimator(null);
        b.setPadding(dp(18), dp(10), dp(18), dp(10));
        b.setMinHeight(dp(44));
        b.setMinimumHeight(dp(44));
        stylePill(b, primary);
        return b;
    }

    private void stylePill(Button b, boolean primary) {
        if (primary) {
            b.setBackground(round(sun, 0, 999));
            b.setTextColor(sunInk);
        } else {
            b.setBackground(round(0x00000000, line, 999));
            b.setTextColor(ink);
        }
    }

    private LinearLayout.LayoutParams wrap(int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(topDp);
        return lp;
    }

    private LinearLayout.LayoutParams full(int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(topDp);
        return lp;
    }

    // ---------- 화면 ----------
    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{bgTop, bgTop, bgBottom});
        scroll.setBackground(bg);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(28), dp(20), dp(40));
        scroll.addView(root);

        // 제목 + 날짜
        TextView title = text("출근길 한마디", 26, ink, true);
        title.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
        root.addView(title);
        String today = new SimpleDateFormat("M월 d일 EEEE", Locale.KOREAN).format(new Date());
        root.addView(text(today, 14, muted, false), wrap(4));

        // 종류 고르기
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        for (String c : Phrases.CATS) {
            Button b = pill(c, false);
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            b.setPadding(dp(14), dp(6), dp(14), dp(6));
            b.setOnClickListener(v -> {
                cat = c;
                refreshChips();
                showPhrase(Phrases.randomIndex(cat, current, rnd));
            });
            LinearLayout.LayoutParams lp = wrap(0);
            lp.rightMargin = dp(8);
            chips.addView(b, lp);
            catButtons.add(b);
        }
        root.addView(chips, full(22));
        refreshChips();

        // 문구 카드
        LinearLayout cardBox = new LinearLayout(this);
        cardBox.setOrientation(LinearLayout.VERTICAL);
        cardBox.setPadding(dp(24), dp(28), dp(24), dp(22));
        cardBox.setBackground(round(card, line, 18));
        cardBox.setMinimumHeight(dp(240));

        tagView = text("오늘의 문구", 13, sun, true);
        tagView.setLetterSpacing(0.08f);
        cardBox.addView(tagView);

        phraseView = text("", 23, ink, false);
        phraseView.setTypeface(Typeface.SERIF);
        phraseView.setLineSpacing(0, 1.45f);
        phraseView.setOnClickListener(v -> showPhrase(Phrases.randomIndex(cat, current, rnd)));
        cardBox.addView(phraseView, full(12));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        Button next = pill("다른 문구", true);
        next.setOnClickListener(v -> showPhrase(Phrases.randomIndex(cat, current, rnd)));
        Button share = pill("보내기", false);
        share.setOnClickListener(v -> sharePhrase());
        Button copy = pill("복사", false);
        copy.setOnClickListener(v -> copyPhrase());
        LinearLayout.LayoutParams a1 = wrap(0); a1.rightMargin = dp(8);
        LinearLayout.LayoutParams a2 = wrap(0); a2.rightMargin = dp(8);
        actions.addView(next, a1);
        actions.addView(share, a2);
        actions.addView(copy, wrap(0));
        cardBox.addView(actions, full(26));
        root.addView(cardBox, full(14));

        // 알람 설정
        View divider = new View(this);
        divider.setBackgroundColor(line);
        LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        dl.topMargin = dp(28);
        root.addView(divider, dl);

        root.addView(text("아침 알람", 16, ink, true), full(20));

        timeView = text("", 40, ink, true);
        timeView.setPadding(0, dp(4), 0, dp(4));
        timeView.setOnClickListener(v -> pickTime());
        root.addView(timeView, wrap(6));
        root.addView(text("시간을 누르면 바꿀 수 있어요", 13, muted, false));

        Switch onOff = new Switch(this);
        onOff.setText("알람 켜기");
        onOff.setTextColor(ink);
        onOff.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        onOff.setChecked(prefs.getBoolean(AlarmScheduler.K_ON, false));
        onOff.setOnCheckedChangeListener((b, checked) -> {
            prefs.edit().putBoolean(AlarmScheduler.K_ON, checked).apply();
            if (checked) checkExactAlarmPermission();
            AlarmScheduler.schedule(this);
            updateStatus();
            if (checked) Toast.makeText(this, "알람을 켰어요", Toast.LENGTH_SHORT).show();
        });
        root.addView(onOff, full(16));

        CheckBox weekdays = new CheckBox(this);
        weekdays.setText("평일만 받기 (월~금)");
        weekdays.setTextColor(ink);
        weekdays.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        weekdays.setChecked(prefs.getBoolean(AlarmScheduler.K_WEEKDAY, true));
        weekdays.setOnCheckedChangeListener((b, checked) -> {
            prefs.edit().putBoolean(AlarmScheduler.K_WEEKDAY, checked).apply();
            AlarmScheduler.schedule(this);
            updateStatus();
        });
        root.addView(weekdays, full(8));

        statusView = text("", 14, muted, false);
        root.addView(statusView, full(10));

        Button test = pill("지금 알림 한 번 받아보기", false);
        test.setOnClickListener(v -> {
            if (!hasNotificationPermission()) {
                Toast.makeText(this, "알림 권한을 허용해야 받을 수 있어요", Toast.LENGTH_LONG).show();
                askNotificationPermission();
                return;
            }
            Intent i = new Intent(this, AlarmReceiver.class).putExtra(AlarmReceiver.EXTRA_TEST, true);
            sendBroadcast(i);
        });
        root.addView(test, wrap(18));

        return scroll;
    }

    private void refreshChips() {
        for (Button b : catButtons) {
            boolean on = b.getText().toString().equals(cat);
            if (on) {
                b.setBackground(round(ink, 0, 999));
                b.setTextColor(bgTop);
            } else {
                stylePill(b, false);
            }
        }
    }

    private void showPhrase(int idx) {
        current = idx;
        String[] p = Phrases.LIST[idx];
        phraseView.animate().alpha(0f).setDuration(150).withEndAction(() -> {
            phraseView.setText(p[1]);
            tagView.setText(p[0] + " 한마디");
            phraseView.animate().alpha(1f).setDuration(220).start();
        }).start();
    }

    private void sharePhrase() {
        if (current < 0) return;
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, Phrases.LIST[current][1]);
        startActivity(Intent.createChooser(send, "문구 보내기"));
    }

    private void copyPhrase() {
        if (current < 0) return;
        ClipboardManager cm = getSystemService(ClipboardManager.class);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("출근길 한마디", Phrases.LIST[current][1]));
            Toast.makeText(this, "복사했어요", Toast.LENGTH_SHORT).show();
        }
    }

    // ---------- 알람 ----------
    private void pickTime() {
        int h = prefs.getInt(AlarmScheduler.K_HOUR, 7);
        int m = prefs.getInt(AlarmScheduler.K_MIN, 0);
        new TimePickerDialog(this, (view, hour, minute) -> {
            prefs.edit().putInt(AlarmScheduler.K_HOUR, hour).putInt(AlarmScheduler.K_MIN, minute).apply();
            updateTime();
            AlarmScheduler.schedule(this);
            updateStatus();
        }, h, m, false).show();
    }

    private void updateTime() {
        int h = prefs.getInt(AlarmScheduler.K_HOUR, 7);
        int m = prefs.getInt(AlarmScheduler.K_MIN, 0);
        String ampm = h < 12 ? "오전" : "오후";
        int h12 = h % 12 == 0 ? 12 : h % 12;
        timeView.setText(String.format(Locale.KOREAN, "%s %d:%02d", ampm, h12, m));
    }

    private void updateStatus() {
        if (statusView == null) return;
        if (!prefs.getBoolean(AlarmScheduler.K_ON, false)) {
            statusView.setText("알람이 꺼져 있어요");
            return;
        }
        long at = AlarmScheduler.nextTrigger(this);
        String s = new SimpleDateFormat("M월 d일(E) a h:mm", Locale.KOREAN).format(new Date(at));
        String warn = hasNotificationPermission() ? "" : "\n⚠ 알림 권한이 꺼져 있어 알림이 오지 않아요";
        statusView.setText("다음 알림: " + s + warn);
    }

    private boolean hasNotificationPermission() {
        return Build.VERSION.SDK_INT < 33
                || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private void askNotificationPermission() {
        if (!hasNotificationPermission()) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 10);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        updateStatus();
    }

    /** 안드로이드 12(S)~12L에서는 정확한 알람 허용을 따로 받아야 한다. */
    private void checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && Build.VERSION.SDK_INT < 33) {
            AlarmManager am = getSystemService(AlarmManager.class);
            if (am != null && !am.canScheduleExactAlarms()) {
                Toast.makeText(this, "정확한 시간에 울리도록 '알람 및 리마인더'를 허용해 주세요",
                        Toast.LENGTH_LONG).show();
                try {
                    startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:" + getPackageName())));
                } catch (Exception ignored) {
                }
            }
        }
    }
}
