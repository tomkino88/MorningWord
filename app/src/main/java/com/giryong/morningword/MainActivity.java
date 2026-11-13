package com.giryong.morningword;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
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
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
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

    // 운세
    public static final String EXTRA_FORTUNE = "open_fortune";
    private ScrollView scrollView;
    private TextView fortuneHeader;
    private LinearLayout fortuneBox;
    private boolean weekly = false;

    private int ink, muted, sun, sunInk, bgTop, bgBottom, card, line;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = AlarmScheduler.prefs(this);
        setColors();
        setContentView(buildUi());
        getWindow().setStatusBarColor(bgTop);

        if (!handleIntent(getIntent())) showPhrase(Phrases.todayIndex());
        maybeScrollToFortune(getIntent());
        updateTime();
        askNotificationPermission();
        AlarmReceiver.ensureChannel(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
        maybeScrollToFortune(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AlarmScheduler.schedule(this);
        updateStatus();
        renderFortune(); // 날짜가 바뀌었을 수 있으니 다시 그린다
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
        scrollView = scroll;
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

        // 운세
        fortuneHeader = text("오늘의 운세", 16, ink, true);
        root.addView(fortuneHeader, full(28));
        fortuneBox = new LinearLayout(this);
        fortuneBox.setOrientation(LinearLayout.VERTICAL);
        fortuneBox.setPadding(dp(22), dp(22), dp(22), dp(20));
        fortuneBox.setBackground(round(card, line, 18));
        root.addView(fortuneBox, full(10));
        renderFortune();

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

    // ---------- 운세 ----------
    private static final String[] WEEK = {"월", "화", "수", "목", "금", "토", "일"};

    private String dayLabel(LocalDate d) {
        return d.getMonthValue() + "/" + d.getDayOfMonth() + "(" + WEEK[d.getDayOfWeek().getValue() - 1] + ")";
    }

    private void maybeScrollToFortune(Intent i) {
        if (i == null || !i.getBooleanExtra(EXTRA_FORTUNE, false) || scrollView == null) return;
        weekly = false;
        renderFortune();
        scrollView.post(() -> scrollView.smoothScrollTo(0, Math.max(0, fortuneHeader.getTop() - dp(12))));
    }

    private void renderFortune() {
        if (fortuneBox == null) return;
        fortuneBox.removeAllViews();
        Saju.Person p = ProfileStore.load(prefs);

        if (p == null) {
            TextView t = text("생년월일과 태어난 시간을 넣으면\n매일 아침 나만의 운세를 볼 수 있어요.", 16, ink, false);
            t.setLineSpacing(0, 1.35f);
            fortuneBox.addView(t);
            Button set = pill("생년월일 넣기", true);
            set.setOnClickListener(v -> showBirthDialog());
            fortuneBox.addView(set, wrap(16));
            addFortuneNote();
            return;
        }

        // 내 사주 정보
        String mine = ProfileStore.describe(prefs) + "\n내 일주 " + Saju.name(p.dayPillar)
                + "(" + Saju.hanja(p.dayPillar) + ")";
        if (p.hourPillar >= 0) {
            mine += " · 시주 " + Saju.name(p.hourPillar) + "(" + Saju.hanja(p.hourPillar) + ")";
        }
        TextView info = text(mine, 13, muted, false);
        info.setLineSpacing(0, 1.3f);
        fortuneBox.addView(info);

        // 오늘 / 이번 주 고르기
        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        Button tToday = pill("오늘", false);
        Button tWeek = pill("7일 운세", false);
        for (Button b : new Button[]{tToday, tWeek}) {
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            b.setPadding(dp(14), dp(6), dp(14), dp(6));
        }
        Button on = weekly ? tWeek : tToday;
        on.setBackground(round(ink, 0, 999));
        on.setTextColor(bgTop);
        tToday.setOnClickListener(v -> { weekly = false; renderFortune(); });
        tWeek.setOnClickListener(v -> { weekly = true; renderFortune(); });
        LinearLayout.LayoutParams tl = wrap(0);
        tl.rightMargin = dp(8);
        tabs.addView(tToday, tl);
        tabs.addView(tWeek, wrap(0));
        fortuneBox.addView(tabs, full(14));

        LocalDate today = LocalDate.now();
        if (!weekly) {
            Fortune f = Fortune.of(p, today);
            TextView date = text(today.getMonthValue() + "월 " + today.getDayOfMonth() + "일 · "
                    + Saju.name(f.dayPillar) + "(" + Saju.hanja(f.dayPillar) + ")일", 13, sun, true);
            fortuneBox.addView(date, full(18));
            fortuneBox.addView(text(f.starText(), 24, sun, false), full(4));
            TextView kw = text(f.keyword, 21, ink, true);
            kw.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
            fortuneBox.addView(kw, full(4));
            TextView sum = text(f.summary, 16, ink, false);
            sum.setLineSpacing(0, 1.4f);
            fortuneBox.addView(sum, full(10));

            addLine("💼 일", f.work);
            addLine("💰 돈", f.money);
            addLine("🤝 사람", f.people);
            if (!f.extra.isEmpty()) {
                TextView ex = text(f.extra, 14, muted, false);
                ex.setLineSpacing(0, 1.35f);
                fortuneBox.addView(ex, full(12));
            }
            fortuneBox.addView(text("🍀 행운의 색 " + f.luckyColor + " · 숫자 " + f.luckyNumber
                    + " · 방향 " + f.luckyDirection, 14, ink, false), full(12));
            TextView adv = text("“" + f.advice + "”", 17, ink, false);
            adv.setTypeface(Typeface.SERIF);
            fortuneBox.addView(adv, full(14));

            Button share = pill("운세 보내기", false);
            share.setOnClickListener(v -> shareText(today.getMonthValue() + "월 " + today.getDayOfMonth()
                    + "일 운세 " + f.starText() + "\n" + f.keyword + "\n" + f.summary
                    + "\n💼 " + f.work + "\n💰 " + f.money + "\n🤝 " + f.people));
            fortuneBox.addView(share, wrap(16));
        } else {
            for (int i = 0; i < 7; i++) {
                LocalDate d = today.plusDays(i);
                Fortune f = Fortune.of(p, d);
                if (i > 0) {
                    View div = new View(this);
                    div.setBackgroundColor(line);
                    LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
                    dl.topMargin = dp(12);
                    fortuneBox.addView(div, dl);
                }
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                TextView dt = text(dayLabel(d) + (i == 0 ? " 오늘" : ""), 14, i == 0 ? sun : ink, true);
                row.addView(dt, new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                row.addView(text(f.starText(), 15, sun, false));
                fortuneBox.addView(row, full(12));
                fortuneBox.addView(text(f.keyword + " · " + Saju.name(f.dayPillar) + "일", 15, ink, true), full(2));
                TextView s = text(f.summary, 14, muted, false);
                s.setLineSpacing(0, 1.3f);
                fortuneBox.addView(s, full(2));
            }
        }

        CheckBox noti = new CheckBox(this);
        noti.setText("아침 알림에 운세도 같이 받기");
        noti.setTextColor(ink);
        noti.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        noti.setChecked(prefs.getBoolean(ProfileStore.K_NOTI, true));
        noti.setOnCheckedChangeListener((b, c) -> prefs.edit().putBoolean(ProfileStore.K_NOTI, c).apply());
        fortuneBox.addView(noti, full(16));

        Button edit = pill("생년월일 바꾸기", false);
        edit.setOnClickListener(v -> showBirthDialog());
        fortuneBox.addView(edit, wrap(8));
        addFortuneNote();
    }

    private void addLine(String label, String body) {
        TextView t = text(label + "   " + body, 15, ink, false);
        t.setLineSpacing(0, 1.3f);
        fortuneBox.addView(t, full(8));
    }

    private void addFortuneNote() {
        TextView n = text("사주 계산 규칙(그날의 일진과 내 일주의 관계)으로 고른 재미용 운세예요. "
                + "미래를 맞히는 것은 아니에요.", 12, muted, false);
        n.setLineSpacing(0, 1.3f);
        fortuneBox.addView(n, full(14));
    }

    private void shareText(String s) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, s);
        startActivity(Intent.createChooser(send, "운세 보내기"));
    }

    private NumberPicker picker(int min, int max, int value, String unit) {
        NumberPicker np = new NumberPicker(this);
        np.setMinValue(min);
        np.setMaxValue(max);
        String[] shown = new String[max - min + 1];
        for (int i = min; i <= max; i++) shown[i - min] = i + unit;
        np.setDisplayedValues(shown);
        np.setValue(Math.max(min, Math.min(max, value)));
        np.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        return np;
    }

    private void showBirthDialog() {
        boolean had = ProfileStore.isSet(prefs);
        int curYear = LocalDate.now().getYear();

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(8), dp(22), dp(4));

        // 양력/음력
        RadioGroup cal = new RadioGroup(this);
        cal.setOrientation(LinearLayout.HORIZONTAL);
        RadioButton solar = new RadioButton(this);
        solar.setText("양력");
        solar.setId(View.generateViewId());
        RadioButton lunar = new RadioButton(this);
        lunar.setText("음력");
        lunar.setId(View.generateViewId());
        cal.addView(solar);
        cal.addView(lunar);
        box.addView(cal);

        CheckBox leap = new CheckBox(this);
        leap.setText("윤달이에요");
        box.addView(leap);

        boolean isLunar = had && prefs.getBoolean(ProfileStore.K_LUNAR, false);
        cal.check(isLunar ? lunar.getId() : solar.getId());
        leap.setChecked(had && prefs.getBoolean(ProfileStore.K_LEAP, false));
        leap.setVisibility(isLunar ? View.VISIBLE : View.GONE);
        cal.setOnCheckedChangeListener((g, id) ->
                leap.setVisibility(id == lunar.getId() ? View.VISIBLE : View.GONE));

        // 날짜
        LinearLayout dateRow = new LinearLayout(this);
        dateRow.setOrientation(LinearLayout.HORIZONTAL);
        dateRow.setGravity(Gravity.CENTER);
        NumberPicker py = picker(1920, curYear, had ? prefs.getInt(ProfileStore.K_Y, 1990) : 1990, "년");
        py.setWrapSelectorWheel(false);
        NumberPicker pm = picker(1, 12, had ? prefs.getInt(ProfileStore.K_M, 1) : 1, "월");
        NumberPicker pd = picker(1, 31, had ? prefs.getInt(ProfileStore.K_D, 1) : 1, "일");
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        dateRow.addView(py, np);
        dateRow.addView(pm, new LinearLayout.LayoutParams(np));
        dateRow.addView(pd, new LinearLayout.LayoutParams(np));
        box.addView(dateRow, full(4));

        // 태어난 시간
        CheckBox noTime = new CheckBox(this);
        noTime.setText("태어난 시간을 몰라요");
        box.addView(noTime, full(10));

        LinearLayout timeRow = new LinearLayout(this);
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        timeRow.setGravity(Gravity.CENTER);
        NumberPicker ph = new NumberPicker(this);
        ph.setMinValue(0);
        ph.setMaxValue(23);
        String[] hours = new String[24];
        for (int h = 0; h < 24; h++) hours[h] = (h < 12 ? "오전 " : "오후 ") + (h % 12 == 0 ? 12 : h % 12) + "시";
        ph.setDisplayedValues(hours);
        ph.setValue(had ? prefs.getInt(ProfileStore.K_HOUR, 7) : 7);
        ph.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        NumberPicker pmin = new NumberPicker(this);
        pmin.setMinValue(0);
        pmin.setMaxValue(59);
        String[] mins = new String[60];
        for (int m = 0; m < 60; m++) mins[m] = String.format(Locale.KOREAN, "%02d분", m);
        pmin.setDisplayedValues(mins);
        pmin.setValue(had ? prefs.getInt(ProfileStore.K_MIN, 0) : 0);
        pmin.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        timeRow.addView(ph, new LinearLayout.LayoutParams(np));
        timeRow.addView(pmin, new LinearLayout.LayoutParams(np));
        box.addView(timeRow);

        boolean unknown = had && !prefs.getBoolean(ProfileStore.K_TIME_KNOWN, false);
        noTime.setChecked(unknown);
        timeRow.setAlpha(unknown ? 0.3f : 1f);
        ph.setEnabled(!unknown);
        pmin.setEnabled(!unknown);
        noTime.setOnCheckedChangeListener((b, c) -> {
            timeRow.setAlpha(c ? 0.3f : 1f);
            ph.setEnabled(!c);
            pmin.setEnabled(!c);
        });

        TextView hint = new TextView(this);
        hint.setText("시간을 모르면 날짜만으로 봐요.\n밤 11시 30분 이후에 태어났으면 다음 날로 계산해요(전통 방식).");
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        hint.setLineSpacing(0, 1.3f);
        box.addView(hint, full(6));

        ScrollView sv = new ScrollView(this);
        sv.addView(box);

        AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle("생년월일 넣기")
                .setView(sv)
                .setPositiveButton("저장", null)
                .setNegativeButton("취소", null)
                .create();
        dlg.setOnShowListener(d -> dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            boolean lu = cal.getCheckedRadioButtonId() == lunar.getId();
            String err = ProfileStore.save(prefs, lu, lu && leap.isChecked(),
                    py.getValue(), pm.getValue(), pd.getValue(),
                    !noTime.isChecked(), ph.getValue(), pmin.getValue());
            if (err != null) {
                Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                return;
            }
            weekly = false;
            renderFortune();
            dlg.dismiss();
            Toast.makeText(this, "저장했어요. 오늘의 운세를 확인해 보세요", Toast.LENGTH_SHORT).show();
        }));
        dlg.show();
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
