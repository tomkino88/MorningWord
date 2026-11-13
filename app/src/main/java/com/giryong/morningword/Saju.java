package com.giryong.morningword;

import java.time.LocalDate;

/**
 * 사주 계산 도구 (안드로이드 기능을 쓰지 않는 순수 계산).
 * - 기둥(주) 번호 0~59: 0=갑자, 1=을축 ... 59=계해
 * - 천간 번호 0~9: 갑을병정무기경신임계
 * - 지지 번호 0~11: 자축인묘진사오미신유술해
 * - 오행 번호 0~4: 나무(木) 불(火) 흙(土) 쇠(金) 물(水)
 */
public final class Saju {
    private Saju() {}

    public static final String[] STEM = {"갑", "을", "병", "정", "무", "기", "경", "신", "임", "계"};
    public static final String[] STEM_H = {"甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"};
    public static final String[] BRANCH = {"자", "축", "인", "묘", "진", "사", "오", "미", "신", "유", "술", "해"};
    public static final String[] BRANCH_H = {"子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"};
    public static final String[] ELEM = {"나무", "불", "흙", "쇠", "물"};
    public static final String[] ELEM_H = {"木", "火", "土", "金", "水"};
    /** 시(時) 이름: 자시 23:30~01:29 ... (한국 표준시 기준 30분 늦춘 전통 방식) */
    public static final String[] HOUR_RANGE = {
            "23:30~01:29", "01:30~03:29", "03:30~05:29", "05:30~07:29",
            "07:30~09:29", "09:30~11:29", "11:30~13:29", "13:30~15:29",
            "15:30~17:29", "17:30~19:29", "19:30~21:29", "21:30~23:29"};

    static final int[] BRANCH_ELEM = {4, 2, 0, 0, 2, 1, 1, 2, 3, 3, 2, 4};

    public static int stemOf(int pillar) { return pillar % 10; }
    public static int branchOf(int pillar) { return pillar % 12; }
    public static int stemElem(int stem) { return stem / 2; }
    public static int branchElem(int branch) { return BRANCH_ELEM[branch]; }

    public static String name(int pillar) {
        return STEM[stemOf(pillar)] + BRANCH[branchOf(pillar)];
    }

    public static String hanja(int pillar) {
        return STEM_H[stemOf(pillar)] + BRANCH_H[branchOf(pillar)];
    }

    /** 그날의 일진(일주) 번호. 기준: 2000-01-01 = 무오일(54). */
    public static int dayPillar(LocalDate d) {
        return (int) Math.floorMod(d.toEpochDay() + 17, 60L);
    }

    /** 태어난 시각(시·분)이 몇 번째 시(지지)인지. 자시는 23:30부터. */
    public static int hourBranch(int hour, int minute) {
        int t = Math.floorMod(hour * 60 + minute - (23 * 60 + 30), 1440);
        return t / 120;
    }

    /** 23:30 이후에 태어났으면 다음 날 자시로 본다. */
    public static boolean isNextDayZi(int hour, int minute) {
        return hour * 60 + minute >= 23 * 60 + 30;
    }

    /** 천간·지지 번호로 기둥 번호(0~59)를 찾는다. 짝이 맞지 않으면 -1. */
    public static int pillarOf(int stem, int branch) {
        for (int i = 0; i < 60; i++) {
            if (i % 10 == stem && i % 12 == branch) return i;
        }
        return -1;
    }

    /** 시주: 일간(태어난 날의 천간)과 시지로 정한다. 갑·기일 → 갑자시부터 시작. */
    public static int hourPillar(int dayStem, int hourBranch) {
        int stem = ((dayStem % 5) * 2 + hourBranch) % 10;
        return pillarOf(stem, hourBranch);
    }

    /**
     * 십신(열 가지 관계). me=나의 일간, other=상대 천간.
     * 0 비견 1 겁재 2 식신 3 상관 4 편재 5 정재 6 편관 7 정관 8 편인 9 정인
     */
    public static int tenGod(int me, int other) {
        int rel = Math.floorMod(stemElem(other) - stemElem(me), 5);
        boolean samePolarity = (me % 2) == (other % 2);
        return rel * 2 + (samePolarity ? 0 : 1);
    }

    public static final String[] TEN_GOD = {
            "비견", "겁재", "식신", "상관", "편재", "정재", "편관", "정관", "편인", "정인"};

    /** 두 지지 사이 관계: 0 없음, 1 육합, 2 삼합, 3 같은 지지, -1 충 */
    public static int branchRelation(int a, int b) {
        if (a == b) return 3;
        if (Math.abs(a - b) == 6) return -1;
        if ((a + b) % 12 == 1) return 1;
        if (a % 4 == b % 4) return 2;
        return 0;
    }

    /** 태어난 정보로 정한 사람의 사주(일주·시주)와 부족한 오행. */
    public static final class Person {
        public final LocalDate solarBirth;
        public final boolean timeKnown;
        public final int hour, minute;
        public final int dayPillar;
        public final int hourPillar; // 시간을 모르면 -1
        public final int[] elemCount = new int[5];
        public final int weakElem;

        public Person(LocalDate solarBirth, boolean timeKnown, int hour, int minute) {
            this.solarBirth = solarBirth;
            this.timeKnown = timeKnown;
            this.hour = hour;
            this.minute = minute;
            LocalDate dayBase = (timeKnown && isNextDayZi(hour, minute))
                    ? solarBirth.plusDays(1) : solarBirth;
            dayPillar = Saju.dayPillar(dayBase);
            hourPillar = timeKnown ? Saju.hourPillar(stemOf(dayPillar), hourBranch(hour, minute)) : -1;

            elemCount[stemElem(stemOf(dayPillar))]++;
            elemCount[branchElem(branchOf(dayPillar))]++;
            if (hourPillar >= 0) {
                elemCount[stemElem(stemOf(hourPillar))]++;
                elemCount[branchElem(branchOf(hourPillar))]++;
            }
            // 가장 적은 오행. 같으면 나를 도와주는(생해 주는) 오행부터 차례로 본다.
            int me = stemElem(stemOf(dayPillar));
            int best = -1;
            for (int k = 0; k < 5; k++) {
                int e = Math.floorMod(me - 1 + k, 5);
                if (best < 0 || elemCount[e] < elemCount[best]) best = e;
            }
            weakElem = best;
        }

        public int dayStem() { return stemOf(dayPillar); }
        public int dayBranch() { return branchOf(dayPillar); }
    }
}
