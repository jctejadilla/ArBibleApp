package com.example.arbibleapp;

import java.util.Calendar;
import java.util.TimeZone;

/** Eligibility is evaluated at first completion and retained for subsequent retries. */
final class JudgesRewardPolicy {
    private JudgesRewardPolicy() {}

    static boolean isSunday(long completedAt, TimeZone timeZone) {
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTimeInMillis(completedAt);
        return calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
    }

    static int xp(int score, boolean completedOnSunday) {
        return completedOnSunday ? score * 20 : 0;
    }
}
