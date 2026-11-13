package com.giryong.morningword;

import java.time.LocalDate;

/**
 * 한국 음력 → 양력 바꾸기 (1920~2049년).
 * 한국천문연구원 음력 자료를 바탕으로 만든 표를 쓴다.
 * INFO: 아래 13비트 = 그해 달마다 큰달(30일)이면 1, 그 위 = 윤달이 든 달(없으면 0)
 * NEW_YEAR: 그해 음력 1월 1일의 양력 날짜(1970-01-01부터 센 날수)
 */
public final class KoreanLunar {
    private KoreanLunar() {}

    public static final int FIRST_YEAR = 1920;
    public static final int LAST_YEAR = 2049;

    private static final int[] INFO = {
            0x00D92, 0x00D25, 0x0BA4D, 0x00956, 0x002B5, 0x095AD, 0x006D4, 0x00DA9, 0x05D92, 0x00E92,
            0x0CD26, 0x00527, 0x00A57, 0x0B2B6, 0x00ADA, 0x006D4, 0x06EA9, 0x00749, 0x0F693, 0x00A93,
            0x0052B, 0x0CA5B, 0x0096D, 0x00B6A, 0x09B54, 0x00BA4, 0x00B49, 0x05A93, 0x00A95, 0x0F52B,
            0x0052D, 0x00AAD, 0x0B56A, 0x00DB2, 0x00DA4, 0x07D49, 0x00D4A, 0x11A95, 0x00A96, 0x00556,
            0x0CAB5, 0x00AD5, 0x006D2, 0x08EA5, 0x00EA5, 0x00E4A, 0x06C96, 0x00A9B, 0x0F556, 0x0056A,
            0x00B59, 0x0B752, 0x00752, 0x00725, 0x0964B, 0x00A4B, 0x112AB, 0x002AD, 0x0056B, 0x0CB69,
            0x00DA9, 0x00D92, 0x09B25, 0x00D25, 0x15A4D, 0x00A56, 0x002B6, 0x0D5AD, 0x006D4, 0x00DA9,
            0x0BD92, 0x00E92, 0x00D26, 0x06A56, 0x00A57, 0x112B6, 0x00B5A, 0x006D4, 0x0AEC9, 0x00749,
            0x00693, 0x09527, 0x0052B, 0x00A5B, 0x0555A, 0x0036A, 0x0FB55, 0x00BA4, 0x00B49, 0x0BA93,
            0x00A95, 0x0052D, 0x06A5D, 0x00AAD, 0x135AA, 0x005D2, 0x00DA5, 0x0BD4A, 0x00D4A, 0x00A95,
            0x0952D, 0x00556, 0x00AB5, 0x055AA, 0x006D2, 0x0CEA5, 0x00EA5, 0x00E4A, 0x0AC96, 0x00C9B,
            0x0055A, 0x06AD5, 0x00B69, 0x17752, 0x00752, 0x00B25, 0x0D64B, 0x00A4B, 0x004AB, 0x0A55B,
            0x0056D, 0x00B69, 0x05B52, 0x00D92, 0x0FD25, 0x00D25, 0x00A4D, 0x0B4AD, 0x002B6, 0x005B5
    };
    private static final int[] NEW_YEAR = {
            -18213, -17859, -17505, -17121, -16767, -16413, -16028, -15674, -15319, -14935, -14581, -14198,
            -13844, -13489, -13105, -12750, -12396, -12012, -11658, -11274, -10920, -10566, -10182, -9827,
            -9472, -9088, -8734, -8380, -7996, -7642, -7258, -6904, -6549, -6165, -5810, -5456, -5072,
            -4718, -4334, -3980, -3626, -3242, -2887, -2533, -2149, -1794, -1440, -1057, -702, -318, 36,
            391, 775, 1129, 1483, 1867, 2221, 2605, 2959, 3314, 3698, 4053, 4407, 4791, 5145, 5529, 5883,
            6237, 6622, 6976, 7331, 7715, 8069, 8423, 8806, 9161, 9545, 9900, 10254, 10638, 10992, 11346,
            11730, 12084, 12439, 12823, 13177, 13562, 13916, 14270, 14654, 15008, 15362, 15746, 16101,
            16485, 16839, 17194, 17578, 17932, 18286, 18670, 19024, 19379, 19763, 20117, 20501, 20856,
            21210, 21593, 21948, 22302, 22686, 23041, 23425, 23779, 24133, 24517, 24871, 25225, 25609,
            25964, 26319, 26703, 27057, 27441, 27795, 28149, 28533, 28887
    };

    /** 그해 윤달이 든 달(없으면 0) */
    public static int leapMonth(int y) {
        if (y < FIRST_YEAR || y > LAST_YEAR) return 0;
        return INFO[y - FIRST_YEAR] >> 13;
    }

    /** 음력 날짜를 양력으로. 없는 날짜면 null. */
    public static LocalDate toSolar(int y, int m, int d, boolean leap) {
        if (y < FIRST_YEAR || y > LAST_YEAR || m < 1 || m > 12 || d < 1) return null;
        int info = INFO[y - FIRST_YEAR];
        int leapM = info >> 13;
        if (leap && leapM != m) return null;
        int count = leapM > 0 ? 13 : 12;
        long days = NEW_YEAR[y - FIRST_YEAR];
        for (int i = 0; i < count; i++) {
            int month = (leapM > 0 && i >= leapM) ? i : i + 1;
            boolean isLeap = leapM > 0 && i == leapM;
            int len = ((info >> i) & 1) == 1 ? 30 : 29;
            if (month == m && isLeap == leap) {
                if (d > len) return null;
                return LocalDate.ofEpochDay(days + d - 1);
            }
            days += len;
        }
        return null;
    }
}
