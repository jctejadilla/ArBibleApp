package com.example.arbibleapp;

import org.junit.Test;
import java.util.Calendar;
import java.util.TimeZone;
import static org.junit.Assert.*;

public class JudgesRewardPolicyTest {
    private long time(int day, int hour, int minute, String zone) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(zone));
        calendar.clear();
        calendar.set(2026, Calendar.OCTOBER, day, hour, minute);
        return calendar.getTimeInMillis();
    }

    @Test public void onlySundayAwardsXpForEveryPossibleScore() {
        TimeZone zone = TimeZone.getTimeZone("Asia/Taipei");
        // October 25, 2026 is Sunday; October 26–31 are Monday–Saturday.
        for (int day = 25; day <= 31; day++) {
            boolean eligible = JudgesRewardPolicy.isSunday(time(day, 12, 0, zone.getID()), zone);
            assertEquals(day == 25, eligible);
            for (int score = 0; score <= 10; score++)
                assertEquals(day == 25 ? score * 20 : 0, JudgesRewardPolicy.xp(score, eligible));
        }
    }

    @Test public void eligibilityUsesLocalDayAtMidnight() {
        TimeZone taipei = TimeZone.getTimeZone("Asia/Taipei");
        assertFalse(JudgesRewardPolicy.isSunday(time(24, 23, 59, taipei.getID()), taipei));
        assertTrue(JudgesRewardPolicy.isSunday(time(25, 0, 0, taipei.getID()), taipei));
        assertTrue(JudgesRewardPolicy.isSunday(time(25, 23, 59, taipei.getID()), taipei));
        assertFalse(JudgesRewardPolicy.isSunday(time(26, 0, 0, taipei.getID()), taipei));
        long instant = time(25, 0, 0, taipei.getID());
        assertFalse(JudgesRewardPolicy.isSunday(instant, TimeZone.getTimeZone("UTC")));
    }

    @Test public void retainedCompletionDecisionDoesNotDependOnRetryDay() {
        TimeZone zone = TimeZone.getTimeZone("Asia/Taipei");
        boolean completedSunday = JudgesRewardPolicy.isSunday(time(25, 23, 59, zone.getID()), zone);
        boolean retryMonday = JudgesRewardPolicy.isSunday(time(26, 0, 1, zone.getID()), zone);
        assertTrue(completedSunday);
        assertFalse(retryMonday);
        assertEquals(160, JudgesRewardPolicy.xp(8, completedSunday));
        boolean completedSaturday = JudgesRewardPolicy.isSunday(time(24, 23, 59, zone.getID()), zone);
        assertEquals(0, JudgesRewardPolicy.xp(8, completedSaturday));
    }
}
