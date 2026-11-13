package com.giryong.morningword;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {

    public static final String EXTRA_FORTUNE = "open_fortune";

    private SharedPreferences prefs;
    private final Random rnd = new Random();
    private String cat = Phrases.ALL;
    private int current = -1;
    private boolean weekly = false;

    private ScrollView scrollView;
    private TextView phraseView, sourceView, tagView, timeView, ampmView, statusView, fortuneHeader;
    private LinearLayout fortuneBox;
    private final List<TextView> catSegs = new ArrayList<>();

    // 색
    private int bg, surface, ink, ink2, ink3, line, accent, accentSoft, rippleColor;
    private int primary, onPrimary, segBg, trackOff;
    private boolean night;
    // 글씨체
    private Typeface fLight, fReg, fSemi, fBold, fSerif, fSerifBold;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = AlarmScheduler.prefs(this);
        setColors();
        loadFonts();
        setContentView(buildUi());
        setupBars();

        if (!handleIntent(getIntent())) showPhrase(Phrases.todayIndex(), false);
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
        showPhrase(idx, false);
        return true;
    }

    // =====================================================================
    // 디자인 바탕: 색 · 글씨체 · 작은 도구들
    // =====================================================================

    private void setColors() {
        night = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        // 파스텔 녹색 톤
        if (night) {
            bg = 0xFF0F1512; surface = 0xFF18211C; ink = 0xFFEDF4EF; ink2 = 0xFFA8BAAE;
            ink3 = 0xFF6D7F73; line = 0xFF25312A; accent = 0xFF8FD3A8; accentSoft = 0xFF1E3027;
            primary = 0xFF8FD3A8; onPrimary = 0xFF0F2619; segBg = 0xFF202B25; trackOff = 0xFF34423A;
            rippleColor = 0x22FFFFFF;
        } else {
            bg = 0xFFF1F6F2; surface = 0xFFFFFFFF; ink = 0xFF1D2B23; ink2 = 0xFF55665B;
            ink3 = 0xFF92A398; line = 0xFFE1EBE4; accent = 0xFF4F9D72; accentSoft = 0xFFE2F2E7;
            primary = 0xFF3F7F5E; onPrimary = 0xFFFFFFFF; segBg = 0xFFE3ECE5; trackOff = 0xFFCFDCD3;
            rippleColor = 0x14000000;
        }
    }

    private void loadFonts() {
        fLight = font(R.font.pretendard_light, Typeface.DEFAULT);
        fReg = font(R.font.pretendard_regular, Typeface.DEFAULT);
        fSemi = font(R.font.pretendard_semibold, Typeface.DEFAULT_BOLD);
        fBold = font(R.font.pretendard_bold, Typeface.DEFAULT_BOLD);
        fSerif = font(R.font.gowun_batang_regular, Typeface.SERIF);
        fSerifBold = font(R.font.gowun_batang_bold, Typeface.create(Typeface.SERIF, Typeface.BOLD));
    }

    private Typeface font(int id, Typeface fallback) {
        try {
            return getResources().getFont(id);
        } catch (Exception e) {
            return fallback;
        }
    }

    private void setupBars() {
        Window w = getWindow();
        w.setStatusBarColor(bg);
        w.setNavigationBarColor(bg);
        if (!night) {
            int flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 27) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            w.getDecorView().setSystemUiVisibility(flags);
        }
    }

    private int dp(float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics()));
    }

    private GradientDrawable round(int fill, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    private GradientDrawable outline(int strokeColor, float radiusDp) {
        GradientDrawable g = round(0x00000000, radiusDp);
        g.setStroke(dp(1), strokeColor);
        return g;
    }

    /** 누르면 물결이 퍼지는 배경 */
    private RippleDrawable ripple(android.graphics.drawable.Drawable content, float radiusDp) {
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), content, round(0xFFFFFFFF, radiusDp));
    }

    private TextView tv(CharSequence s, float sp, int color, Typeface f) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setTypeface(f);
        t.setIncludeFontPadding(false);
        return t;
    }

    private LinearLayout col() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private LinearLayout.LayoutParams wrap(int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(topDp);
        return lp;
    }

    private LinearLayout.LayoutParams full(int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(topDp);
        return lp;
    }

    private LinearLayout.LayoutParams weight(float w) {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, w);
    }

    /** 흰 카드 */
    private LinearLayout card() {
        LinearLayout c = col();
        c.setBackground(round(surface, 24));
        c.setPadding(dp(24), dp(24), dp(24), dp(24));
        return c;
    }

    /** 꽉 찬 버튼(primary) / 테두리 버튼 */
    private TextView button(String label, boolean primary) {
        TextView b = tv(label, 15, primary ? onPrimary : ink, fSemi);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(20), dp(13), dp(20), dp(13));
        b.setBackground(ripple(primary ? round(this.primary, 999) : outline(line, 999), 999));
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    /** 글자만 있는 작은 버튼 */
    private TextView textButton(String label, int color) {
        TextView b = tv(label, 14, color, fSemi);
        b.setPadding(dp(12), dp(10), dp(12), dp(10));
        b.setBackground(ripple(round(0x00000000, 12), 12));
        b.setClickable(true);
        return b;
    }

    private View divider(int topDp) {
        View v = new View(this);
        v.setBackgroundColor(line);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(1)));
        lp.topMargin = dp(topDp);
        v.setLayoutParams(lp);
        return v;
    }

    private void tintSwitch(Switch s) {
        int[][] states = {{android.R.attr.state_checked}, {}};
        s.setThumbTintList(new ColorStateList(states, new int[]{0xFFFFFFFF, night ? 0xFFB9C6BE : 0xFFFFFFFF}));
        s.setTrackTintList(new ColorStateList(states, new int[]{accent, trackOff}));
    }

    /** 왼쪽 글 + 오른쪽 스위치 한 줄 */
    private LinearLayout switchRow(String label, String sub, boolean checked,
                                   android.widget.CompoundButton.OnCheckedChangeListener l) {
        LinearLayout r = row();
        LinearLayout texts = col();
        texts.addView(tv(label, 15, ink, fSemi));
        if (sub != null) texts.addView(tv(sub, 13, ink3, fReg), wrap(4));
        r.addView(texts, weight(1));
        Switch s = new Switch(this);
        s.setChecked(checked);
        tintSwitch(s);
        s.setOnCheckedChangeListener(l);
        r.addView(s);
        r.setOnClickListener(v -> s.toggle());
        r.setPadding(0, dp(6), 0, dp(6));
        return r;
    }

    /** 둥근 바 안에서 하나를 고르는 버튼 묶음 */
    private LinearLayout segmented(String[] labels, int selected, List<TextView> out,
                                   java.util.function.IntConsumer onPick) {
        LinearLayout box = row();
        box.setBackground(round(segBg, 999));
        box.setPadding(dp(4), dp(4), dp(4), dp(4));
        List<TextView> segs = out != null ? out : new ArrayList<>();
        segs.clear();
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            TextView s = tv(labels[i], labels.length > 4 ? 13 : 14, ink2, fSemi);
            s.setGravity(Gravity.CENTER);
            s.setPadding(dp(8), dp(9), dp(8), dp(9));
            s.setOnClickListener(v -> {
                styleSegments(segs, idx);
                onPick.accept(idx);
            });
            segs.add(s);
            box.addView(s, weight(1));
        }
        styleSegments(segs, selected);
        return box;
    }

    private void styleSegments(List<TextView> segs, int selected) {
        for (int i = 0; i < segs.size(); i++) {
            TextView s = segs.get(i);
            boolean on = i == selected;
            s.setBackground(on ? round(surface, 999) : null);
            s.setTextColor(on ? ink : ink3);
            if (on) s.setElevation(dp(1)); else s.setElevation(0);
        }
    }

    private CharSequence stars(int n, float sizeSp) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        for (int i = 1; i <= 5; i++) {
            int start = sb.length();
            sb.append("★");
            sb.setSpan(new ForegroundColorSpan(i <= n ? accent : line), start, sb.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (i < 5) sb.append(" ");
        }
        return sb;
    }

    // =====================================================================
    // 화면 만들기
    // =====================================================================

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scrollView = scroll;
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(bg);
        scroll.setVerticalScrollBarEnabled(false);

        LinearLayout root = col();
        root.setPadding(dp(20), dp(28), dp(20), dp(48));
        scroll.addView(root);

        // ---- 머리글 ----
        LinearLayout brand = row();
        View dot = new View(this);
        dot.setBackground(round(accent, 999));
        brand.addView(dot, new LinearLayout.LayoutParams(dp(8), dp(8)));
        TextView brandName = tv("출근길 한마디", 13, ink2, fSemi);
        LinearLayout.LayoutParams bl = wrap(0);
        bl.leftMargin = dp(8);
        brand.addView(brandName, bl);
        root.addView(brand);

        String today = new SimpleDateFormat("M월 d일 EEEE", Locale.KOREAN).format(new Date());
        root.addView(tv(today, 14, ink3, fReg), wrap(26));
        TextView hello = tv(greeting(), 30, ink, fBold);
        hello.setLetterSpacing(-0.02f);
        root.addView(hello, wrap(8));

        // ---- 문구 종류 고르기 ----
        int sel = 0;
        for (int i = 0; i < Phrases.CATS.length; i++) if (Phrases.CATS[i].equals(cat)) sel = i;
        root.addView(segmented(Phrases.CATS, sel, catSegs, idx -> {
            cat = Phrases.CATS[idx];
            showPhrase(Phrases.randomIndex(cat, current, rnd), true);
        }), full(26));

        // ---- 문구 카드 ----
        LinearLayout pc = card();
        pc.setPadding(dp(26), dp(26), dp(26), dp(22));

        tagView = tv("오늘의 문구", 12, accent, fSemi);
        tagView.setBackground(round(accentSoft, 999));
        tagView.setPadding(dp(10), dp(6), dp(10), dp(6));
        pc.addView(tagView, wrap(0));

        TextView quote = tv("“", 54, accent, fSerifBold);
        quote.setAlpha(0.35f);
        pc.addView(quote, wrap(14));

        phraseView = tv("", 22, ink, fSerif);
        phraseView.setLineSpacing(dp(4), 1.25f);
        phraseView.setMinHeight(dp(110));
        phraseView.setOnClickListener(v -> showPhrase(Phrases.randomIndex(cat, current, rnd), true));
        LinearLayout.LayoutParams pl = full(-10);
        pc.addView(phraseView, pl);
        sourceView = tv("", 13, ink3, fReg);
        sourceView.setLineSpacing(dp(2), 1.2f);
        pc.addView(sourceView, full(14));

        pc.addView(divider(24));
        LinearLayout actions = row();
        TextView next = button("다른 문구", true);
        next.setOnClickListener(v -> showPhrase(Phrases.randomIndex(cat, current, rnd), true));
        actions.addView(next, wrap(0));
        View gap = new View(this);
        actions.addView(gap, weight(1));
        TextView share = textButton("보내기", ink2);
        share.setOnClickListener(v -> sharePhrase());
        TextView copy = textButton("복사", ink2);
        copy.setOnClickListener(v -> copyPhrase());
        actions.addView(share);
        actions.addView(copy);
        pc.addView(actions, full(16));
        root.addView(pc, full(14));

        // ---- 운세 ----
        LinearLayout fh = row();
        fortuneHeader = tv("오늘의 운세", 20, ink, fBold);
        fh.addView(fortuneHeader, weight(1));
        fh.addView(tv("사주 일진 기준", 12, ink3, fReg));
        root.addView(fh, full(40));

        fortuneBox = card();
        root.addView(fortuneBox, full(14));
        renderFortune();

        TextView note = tv("그날의 일진과 내 일주를 사주 규칙(십신·합·충)으로 비교해 고른 재미용 운세예요. "
                + "미래를 맞히는 것은 아니에요.", 12, ink3, fReg);
        note.setLineSpacing(dp(2), 1.2f);
        note.setPadding(dp(6), 0, dp(6), 0);
        root.addView(note, full(12));

        // ---- 알람 ----
        root.addView(tv("아침 알림", 20, ink, fBold), full(40));
        LinearLayout ac = card();

        LinearLayout timeRow = row();
        timeRow.setGravity(Gravity.BOTTOM);
        ampmView = tv("", 18, ink2, fSemi);
        timeView = tv("", 52, ink, fLight);
        timeView.setLetterSpacing(-0.02f);
        timeRow.addView(timeView);
        LinearLayout.LayoutParams al = wrap(0);
        al.leftMargin = dp(8);
        al.bottomMargin = dp(8);
        timeRow.addView(ampmView, al);
        timeRow.setOnClickListener(v -> pickTime());
        timeRow.setBackground(ripple(round(0x00000000, 16), 16));
        ac.addView(timeRow, wrap(0));
        ac.addView(tv("시간을 누르면 바꿀 수 있어요", 13, ink3, fReg), wrap(6));

        ac.addView(divider(20));
        ac.addView(switchRow("알림 켜기", null, prefs.getBoolean(AlarmScheduler.K_ON, false), (b, checked) -> {
            prefs.edit().putBoolean(AlarmScheduler.K_ON, checked).apply();
            if (checked) checkExactAlarmPermission();
            AlarmScheduler.schedule(this);
            updateStatus();
            if (checked) Toast.makeText(this, "알림을 켰어요", Toast.LENGTH_SHORT).show();
        }), full(10));
        ac.addView(switchRow("평일만 받기", "월요일 ~ 금요일", prefs.getBoolean(AlarmScheduler.K_WEEKDAY, true),
                (b, checked) -> {
                    prefs.edit().putBoolean(AlarmScheduler.K_WEEKDAY, checked).apply();
                    AlarmScheduler.schedule(this);
                    updateStatus();
                }), full(6));

        statusView = tv("", 13, ink2, fReg);
        statusView.setLineSpacing(dp(2), 1.2f);
        statusView.setBackground(round(bg, 14));
        statusView.setPadding(dp(14), dp(12), dp(14), dp(12));
        ac.addView(statusView, full(16));

        TextView test = button("알림 미리 받아보기", false);
        test.setOnClickListener(v -> {
            if (!hasNotificationPermission()) {
                Toast.makeText(this, "알림 권한을 허용해야 받을 수 있어요", Toast.LENGTH_LONG).show();
                askNotificationPermission();
                return;
            }
            sendBroadcast(new Intent(this, AlarmReceiver.class).putExtra(AlarmReceiver.EXTRA_TEST, true));
        });
        ac.addView(test, full(16));
        root.addView(ac, full(14));

        TextView foot = tv("출근길 한마디 · 오늘도 무사히", 12, ink3, fReg);
        foot.setGravity(Gravity.CENTER);
        root.addView(foot, full(32));

        return scroll;
    }

    private String greeting() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (h < 5) return "늦은 밤이에요,\n푹 쉬어요";
        if (h < 11) return "좋은 아침이에요";
        if (h < 17) return "오늘도 잘하고 있어요";
        return "오늘도 수고했어요";
    }

    private void showPhrase(int idx, boolean animate) {
        current = idx;
        String[] p = Phrases.LIST[idx];
        int cut = p[1].lastIndexOf(" — ");
        String main = cut > 0 ? p[1].substring(0, cut) : p[1];
        String src = cut > 0 ? "— " + p[1].substring(cut + 3) : "";
        Runnable set = () -> {
            phraseView.setText(main);
            sourceView.setText(src);
            sourceView.setVisibility(src.isEmpty() ? View.GONE : View.VISIBLE);
            tagView.setText(p[0]);
        };
        if (!animate) {
            set.run();
            return;
        }
        sourceView.animate().alpha(0f).setDuration(140).start();
        phraseView.animate().alpha(0f).translationY(dp(4)).setDuration(140).withEndAction(() -> {
            set.run();
            phraseView.setTranslationY(-dp(4));
            phraseView.animate().alpha(1f).translationY(0).setDuration(220).start();
            sourceView.animate().alpha(1f).setDuration(220).start();
        }).start();
    }

    private void sharePhrase() {
        if (current < 0) return;
        shareText(Phrases.LIST[current][1], "문구 보내기");
    }

    private void copyPhrase() {
        if (current < 0) return;
        ClipboardManager cm = getSystemService(ClipboardManager.class);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("출근길 한마디", Phrases.LIST[current][1]));
            Toast.makeText(this, "복사했어요", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareText(String s, String title) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, s);
        startActivity(Intent.createChooser(send, title));
    }

    // =====================================================================
    // 운세
    // =====================================================================

    private static final String[] WEEK = {"월", "화", "수", "목", "금", "토", "일"};
    private static final int[] LUCKY_SWATCH = {0xFF4FA77A, 0xFFE0574A, 0xFFE7B53A, 0xFFFFFFFF, 0xFF2D3B66};

    private void maybeScrollToFortune(Intent i) {
        if (i == null || !i.getBooleanExtra(EXTRA_FORTUNE, false) || scrollView == null) return;
        weekly = false;
        renderFortune();
        scrollView.post(() -> scrollView.smoothScrollTo(0, Math.max(0, fortuneHeader.getTop() - dp(16))));
    }

    private void renderFortune() {
        if (fortuneBox == null) return;
        fortuneBox.removeAllViews();
        Saju.Person p = ProfileStore.load(prefs);

        if (p == null) {
            TextView t = tv("태어난 날을 알려주시면\n매일 아침 운세를 함께 보내드려요", 19, ink, fSerif);
            t.setLineSpacing(dp(4), 1.2f);
            fortuneBox.addView(t);
            TextView sub = tv("생년월일과 태어난 시간(모르면 생략)만 있으면 돼요.", 13, ink3, fReg);
            fortuneBox.addView(sub, wrap(12));
            TextView set = button("생년월일 넣기", true);
            set.setOnClickListener(v -> showBirthDialog());
            fortuneBox.addView(set, wrap(20));
            return;
        }

        fortuneBox.addView(segmented(new String[]{"오늘", "7일 운세"}, weekly ? 1 : 0, null, idx -> {
            weekly = idx == 1;
            fortuneBox.post(this::renderFortune);
        }), full(0));

        LocalDate today = LocalDate.now();
        if (!weekly) renderToday(p, today); else renderWeek(p, today);

        fortuneBox.addView(divider(22));
        fortuneBox.addView(switchRow("아침 알림에 운세도 받기", null,
                prefs.getBoolean(ProfileStore.K_NOTI, true),
                (b, c) -> prefs.edit().putBoolean(ProfileStore.K_NOTI, c).apply()), full(10));

        LinearLayout mine = row();
        String info = ProfileStore.describe(prefs) + "\n일주 " + Saju.name(p.dayPillar)
                + "(" + Saju.hanja(p.dayPillar) + ")";
        if (p.hourPillar >= 0) info += " · 시주 " + Saju.name(p.hourPillar) + "(" + Saju.hanja(p.hourPillar) + ")";
        TextView it = tv(info, 12, ink3, fReg);
        it.setLineSpacing(dp(2), 1.2f);
        mine.addView(it, weight(1));
        TextView edit = textButton("수정", accent);
        edit.setOnClickListener(v -> showBirthDialog());
        mine.addView(edit);
        fortuneBox.addView(mine, full(10));
    }

    private void renderToday(Saju.Person p, LocalDate today) {
        Fortune f = Fortune.of(p, today);

        LinearLayout top = row();
        top.setGravity(Gravity.TOP);
        LinearLayout left = col();
        left.addView(tv(today.getMonthValue() + "월 " + today.getDayOfMonth() + "일 · "
                + Saju.name(f.dayPillar) + "(" + Saju.hanja(f.dayPillar) + ")일", 13, ink3, fReg));
        TextView kw = tv(f.keyword, 24, ink, fSerifBold);
        kw.setLineSpacing(dp(2), 1.1f);
        left.addView(kw, wrap(10));
        top.addView(left, weight(1));
        TextView badge = tv(f.scoreWord(), 13, accent, fSemi);
        badge.setBackground(round(accentSoft, 999));
        badge.setPadding(dp(12), dp(7), dp(12), dp(7));
        top.addView(badge);
        fortuneBox.addView(top, full(24));

        fortuneBox.addView(tv(stars(f.stars, 18), 18, accent, fReg), wrap(12));

        TextView sum = tv(f.summary, 16, ink, fReg);
        sum.setLineSpacing(dp(4), 1.2f);
        fortuneBox.addView(sum, full(18));

        fortuneBox.addView(divider(22));
        addDetail("일", f.work, 18);
        addDetail("돈", f.money, 14);
        addDetail("사람", f.people, 14);

        if (!f.extra.isEmpty()) {
            TextView ex = tv(f.extra, 14, ink2, fReg);
            ex.setLineSpacing(dp(3), 1.2f);
            ex.setBackground(round(accentSoft, 16));
            ex.setPadding(dp(16), dp(14), dp(16), dp(14));
            fortuneBox.addView(ex, full(20));
        }

        // 행운 타일 3개
        LinearLayout tiles = row();
        tiles.addView(luckyTile("행운의 색", f.luckyColor, f.luckyElem >= 0 ? LUCKY_SWATCH[f.luckyElem] : 0), weight(1));
        LinearLayout.LayoutParams m = weight(1);
        m.leftMargin = dp(8);
        tiles.addView(luckyTile("숫자", f.luckyNumber, 0), m);
        LinearLayout.LayoutParams m2 = weight(1);
        m2.leftMargin = dp(8);
        tiles.addView(luckyTile("방향", f.luckyDirection, 0), m2);
        fortuneBox.addView(tiles, full(16));

        TextView adv = tv("“" + f.advice + "”", 17, ink, fSerif);
        adv.setGravity(Gravity.CENTER);
        adv.setLineSpacing(dp(3), 1.2f);
        fortuneBox.addView(adv, full(24));

        TextView share = textButton("운세 보내기", ink2);
        share.setOnClickListener(v -> shareText(today.getMonthValue() + "월 " + today.getDayOfMonth()
                + "일 운세 · " + f.scoreWord() + " " + f.starText() + "\n\n" + f.keyword + "\n" + f.summary
                + "\n\n일 · " + f.work + "\n돈 · " + f.money + "\n사람 · " + f.people, "운세 보내기"));
        LinearLayout sr = row();
        sr.setGravity(Gravity.CENTER);
        sr.addView(share);
        fortuneBox.addView(sr, full(10));
    }

    private void addDetail(String label, String body, int topDp) {
        LinearLayout r = row();
        r.setGravity(Gravity.TOP);
        TextView l = tv(label, 13, ink3, fSemi);
        r.addView(l, new LinearLayout.LayoutParams(dp(44), ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView b = tv(body, 15, ink, fReg);
        b.setLineSpacing(dp(3), 1.2f);
        r.addView(b, weight(1));
        fortuneBox.addView(r, full(topDp));
    }

    private LinearLayout luckyTile(String label, String value, int swatch) {
        LinearLayout t = col();
        t.setBackground(round(bg, 16));
        t.setPadding(dp(14), dp(14), dp(10), dp(14));
        t.addView(tv(label, 12, ink3, fReg));
        LinearLayout v = row();
        if (swatch != 0) {
            View s = new View(this);
            GradientDrawable g = round(swatch, 999);
            g.setStroke(dp(1), line);
            s.setBackground(g);
            LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(dp(12), dp(12));
            sl.rightMargin = dp(6);
            v.addView(s, sl);
        }
        TextView val = tv(value, 15, ink, fSemi);
        val.setMaxLines(1);
        val.setEllipsize(TextUtils.TruncateAt.END);
        v.addView(val);
        t.addView(v, wrap(8));
        return t;
    }

    private void renderWeek(Saju.Person p, LocalDate today) {
        for (int i = 0; i < 7; i++) {
            LocalDate d = today.plusDays(i);
            Fortune f = Fortune.of(p, d);

            LinearLayout r = row();
            r.setPadding(dp(12), dp(14), dp(12), dp(14));
            if (i == 0) r.setBackground(round(accentSoft, 16));

            LinearLayout day = col();
            day.setGravity(Gravity.CENTER_HORIZONTAL);
            TextView num = tv(String.valueOf(d.getDayOfMonth()), 20, i == 0 ? accent : ink, fBold);
            num.setGravity(Gravity.CENTER);
            day.addView(num);
            TextView wd = tv(i == 0 ? "오늘" : WEEK[d.getDayOfWeek().getValue() - 1], 12,
                    i == 0 ? accent : ink3, fSemi);
            wd.setGravity(Gravity.CENTER);
            day.addView(wd, wrap(4));
            r.addView(day, new LinearLayout.LayoutParams(dp(40), ViewGroup.LayoutParams.WRAP_CONTENT));

            LinearLayout mid = col();
            mid.setPadding(dp(12), 0, dp(8), 0);
            mid.addView(tv(f.keyword, 15, ink, fSemi));
            TextView s = tv(Saju.name(f.dayPillar) + "일 · " + firstSentence(f.summary), 13, ink2, fReg);
            s.setMaxLines(2);
            s.setEllipsize(TextUtils.TruncateAt.END);
            s.setLineSpacing(dp(2), 1.1f);
            mid.addView(s, wrap(6));
            r.addView(mid, weight(1));

            r.addView(tv(stars(f.stars, 11), 11, accent, fReg));
            fortuneBox.addView(r, full(i == 0 ? 18 : 4));
        }
    }

    private String firstSentence(String s) {
        int k = s.indexOf(". ");
        return k > 0 ? s.substring(0, k + 1) : s;
    }

    // ---------- 생년월일 입력 창 ----------

    private NumberPicker picker(int min, int max, int value, String unit) {
        NumberPicker np = new NumberPicker(this);
        np.setMinValue(min);
        np.setMaxValue(max);
        String[] shown = new String[max - min + 1];
        for (int i = min; i <= max; i++) shown[i - min] = unit.equals("분") ? String.format(Locale.KOREAN, "%02d분", i) : i + unit;
        np.setDisplayedValues(shown);
        np.setValue(Math.max(min, Math.min(max, value)));
        np.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        stylePicker(np);
        return np;
    }

    private void stylePicker(NumberPicker np) {
        if (Build.VERSION.SDK_INT >= 29) {
            np.setTextColor(ink);
            np.setTextSize(dp(17));
            np.setSelectionDividerHeight(dp(1));
        }
    }

    private void showBirthDialog() {
        boolean had = ProfileStore.isSet(prefs);
        int curYear = LocalDate.now().getYear();

        Dialog dlg = new Dialog(this);
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout box = col();
        box.setBackground(round(surface, 28));
        box.setPadding(dp(24), dp(26), dp(24), dp(20));

        box.addView(tv("생년월일 넣기", 20, ink, fBold));
        box.addView(tv("운세 계산에만 쓰고, 폰 밖으로 보내지 않아요.", 13, ink3, fReg), wrap(8));

        final boolean[] lunar = {had && prefs.getBoolean(ProfileStore.K_LUNAR, false)};
        final boolean[] leap = {had && prefs.getBoolean(ProfileStore.K_LEAP, false)};

        LinearLayout leapRow = switchRow("윤달이에요", null, leap[0], (b, c) -> leap[0] = c);
        leapRow.setVisibility(lunar[0] ? View.VISIBLE : View.GONE);

        box.addView(segmented(new String[]{"양력", "음력"}, lunar[0] ? 1 : 0, null, idx -> {
            lunar[0] = idx == 1;
            leapRow.setVisibility(lunar[0] ? View.VISIBLE : View.GONE);
        }), full(20));
        box.addView(leapRow, full(6));

        LinearLayout dateRow = row();
        dateRow.setGravity(Gravity.CENTER);
        NumberPicker py = picker(1920, curYear, had ? prefs.getInt(ProfileStore.K_Y, 1990) : 1990, "년");
        py.setWrapSelectorWheel(false);
        NumberPicker pm = picker(1, 12, had ? prefs.getInt(ProfileStore.K_M, 1) : 1, "월");
        NumberPicker pd = picker(1, 31, had ? prefs.getInt(ProfileStore.K_D, 1) : 1, "일");
        dateRow.addView(py, weight(1.3f));
        dateRow.addView(pm, weight(1));
        dateRow.addView(pd, weight(1));
        box.addView(dateRow, full(8));

        final boolean[] noTime = {had && !prefs.getBoolean(ProfileStore.K_TIME_KNOWN, false)};
        LinearLayout timeRow = row();
        timeRow.setGravity(Gravity.CENTER);
        NumberPicker ph = new NumberPicker(this);
        ph.setMinValue(0);
        ph.setMaxValue(23);
        String[] hours = new String[24];
        for (int h = 0; h < 24; h++) hours[h] = (h < 12 ? "오전 " : "오후 ") + (h % 12 == 0 ? 12 : h % 12) + "시";
        ph.setDisplayedValues(hours);
        ph.setValue(had ? prefs.getInt(ProfileStore.K_HOUR, 7) : 7);
        ph.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        stylePicker(ph);
        NumberPicker pmin = picker(0, 59, had ? prefs.getInt(ProfileStore.K_MIN, 0) : 0, "분");
        timeRow.addView(ph, weight(1.3f));
        timeRow.addView(pmin, weight(1));

        Runnable applyTime = () -> {
            timeRow.setAlpha(noTime[0] ? 0.25f : 1f);
            ph.setEnabled(!noTime[0]);
            pmin.setEnabled(!noTime[0]);
        };
        box.addView(divider(8));
        box.addView(switchRow("태어난 시간을 몰라요", "모르면 날짜만으로 봐요", noTime[0], (b, c) -> {
            noTime[0] = c;
            applyTime.run();
        }), full(12));
        box.addView(timeRow, full(4));
        applyTime.run();

        TextView hint = tv("밤 11시 30분 이후 출생은 다음 날로 계산해요(전통 방식).", 12, ink3, fReg);
        box.addView(hint, wrap(8));

        LinearLayout btns = row();
        TextView cancel = button("취소", false);
        TextView save = button("저장", true);
        btns.addView(cancel, weight(1));
        LinearLayout.LayoutParams sl = weight(1);
        sl.leftMargin = dp(10);
        btns.addView(save, sl);
        box.addView(btns, full(22));

        cancel.setOnClickListener(v -> dlg.dismiss());
        save.setOnClickListener(v -> {
            String err = ProfileStore.save(prefs, lunar[0], lunar[0] && leap[0],
                    py.getValue(), pm.getValue(), pd.getValue(),
                    !noTime[0], ph.getValue(), pmin.getValue());
            if (err != null) {
                Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                return;
            }
            weekly = false;
            renderFortune();
            dlg.dismiss();
            Toast.makeText(this, "저장했어요", Toast.LENGTH_SHORT).show();
        });

        ScrollView sv = new ScrollView(this);
        sv.addView(box);
        FrameLayout frame = new FrameLayout(this);
        frame.setPadding(dp(12), dp(12), dp(12), dp(12));
        frame.addView(sv);
        dlg.setContentView(frame);

        Window w = dlg.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            w.setGravity(Gravity.BOTTOM);
        }
        dlg.show();
    }

    // =====================================================================
    // 알람
    // =====================================================================

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
        int h12 = h % 12 == 0 ? 12 : h % 12;
        timeView.setText(String.format(Locale.KOREAN, "%d:%02d", h12, m));
        ampmView.setText(h < 12 ? "오전" : "오후");
    }

    private void updateStatus() {
        if (statusView == null) return;
        if (!prefs.getBoolean(AlarmScheduler.K_ON, false)) {
            statusView.setText("알림이 꺼져 있어요");
            return;
        }
        long at = AlarmScheduler.nextTrigger(this);
        String s = new SimpleDateFormat("M월 d일 (E) a h:mm", Locale.KOREAN).format(new Date(at));
        String warn = hasNotificationPermission() ? "" : "\n알림 권한이 꺼져 있어 알림이 오지 않아요";
        statusView.setText("다음 알림  " + s + warn);
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
