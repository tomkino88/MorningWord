package com.giryong.morningword;

import android.content.SharedPreferences;

import java.time.LocalDate;
import java.util.Locale;

/** 생년월일·태어난 시를 저장하고 꺼낸다. 음력이면 양력으로 바꿔서 계산에 쓴다. */
public final class ProfileStore {
    private ProfileStore() {}

    static final String K_SET = "birth_set";
    static final String K_LUNAR = "birth_lunar";
    static final String K_LEAP = "birth_leap";
    static final String K_Y = "birth_y", K_M = "birth_m", K_D = "birth_d";       // 입력한 날짜
    static final String K_SY = "birth_sy", K_SM = "birth_sm", K_SD = "birth_sd"; // 양력으로 바꾼 날짜
    static final String K_TIME_KNOWN = "birth_time_known";
    static final String K_HOUR = "birth_hour", K_MIN = "birth_min";
    public static final String K_NOTI = "fortune_noti";

    public static boolean isSet(SharedPreferences p) {
        return p.getBoolean(K_SET, false);
    }

    public static Saju.Person load(SharedPreferences p) {
        if (!isSet(p)) return null;
        try {
            LocalDate solar = LocalDate.of(p.getInt(K_SY, 1990), p.getInt(K_SM, 1), p.getInt(K_SD, 1));
            return new Saju.Person(solar, p.getBoolean(K_TIME_KNOWN, false),
                    p.getInt(K_HOUR, 0), p.getInt(K_MIN, 0));
        } catch (Exception e) {
            return null;
        }
    }

    /** 저장. 날짜가 잘못됐으면 오류 문장을, 잘 저장됐으면 null을 돌려준다. */
    public static String save(SharedPreferences p, boolean lunar, boolean leap, int y, int m, int d,
                              boolean timeKnown, int hour, int minute) {
        LocalDate solar;
        if (lunar) {
            solar = lunarToSolar(y, m, d, leap);
            if (solar == null) {
                return leap ? y + "년에는 음력 " + m + "월 윤달이 없거나 날짜가 맞지 않아요."
                        : "음력 " + y + "년 " + m + "월에는 " + d + "일이 없어요.";
            }
        } else {
            try {
                solar = LocalDate.of(y, m, d);
            } catch (Exception e) {
                return y + "년 " + m + "월에는 " + d + "일이 없어요.";
            }
        }
        if (solar.isAfter(LocalDate.now())) return "오늘 이후 날짜는 넣을 수 없어요.";

        p.edit()
                .putBoolean(K_SET, true)
                .putBoolean(K_LUNAR, lunar)
                .putBoolean(K_LEAP, lunar && leap)
                .putInt(K_Y, y).putInt(K_M, m).putInt(K_D, d)
                .putInt(K_SY, solar.getYear()).putInt(K_SM, solar.getMonthValue()).putInt(K_SD, solar.getDayOfMonth())
                .putBoolean(K_TIME_KNOWN, timeKnown)
                .putInt(K_HOUR, hour).putInt(K_MIN, minute)
                .apply();
        return null;
    }

    /** 화면에 보여줄 "1985년 3월 2일(양력) 오전 7:40" 같은 글 */
    public static String describe(SharedPreferences p) {
        if (!isSet(p)) return "";
        boolean lunar = p.getBoolean(K_LUNAR, false);
        StringBuilder sb = new StringBuilder();
        sb.append(p.getInt(K_Y, 0)).append("년 ").append(p.getInt(K_M, 0)).append("월 ")
                .append(p.getInt(K_D, 0)).append("일");
        if (lunar) {
            sb.append("(음력").append(p.getBoolean(K_LEAP, false) ? "·윤달" : "").append(")");
            sb.append(" = 양력 ").append(p.getInt(K_SY, 0)).append(".").append(p.getInt(K_SM, 0))
                    .append(".").append(p.getInt(K_SD, 0));
        } else {
            sb.append("(양력)");
        }
        if (p.getBoolean(K_TIME_KNOWN, false)) {
            int h = p.getInt(K_HOUR, 0), mi = p.getInt(K_MIN, 0);
            sb.append(String.format(Locale.KOREAN, " %s %d:%02d", h < 12 ? "오전" : "오후",
                    h % 12 == 0 ? 12 : h % 12, mi));
        } else {
            sb.append(" · 태어난 시간 모름");
        }
        return sb.toString();
    }

    /** 음력 → 양력 (한국 음력 표 사용, 1920~2049년) */
    static LocalDate lunarToSolar(int y, int m, int d, boolean leap) {
        return KoreanLunar.toSolar(y, m, d, leap);
    }
}
