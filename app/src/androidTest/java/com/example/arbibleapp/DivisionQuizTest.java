package com.example.arbibleapp;

import android.view.View;
import android.widget.LinearLayout;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.firebase.auth.FirebaseAuth;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

@RunWith(AndroidJUnit4.class)
public class DivisionQuizTest {
    @org.junit.Before public void offline() throws Exception {
        assumeTrue(FirebaseAuth.getInstance().getCurrentUser() == null);
        com.google.android.gms.tasks.Tasks.await(com.google.firebase.firestore.FirebaseFirestore.getInstance().disableNetwork());
    }
    @org.junit.After public void online() { com.google.firebase.firestore.FirebaseFirestore.getInstance().enableNetwork(); }
    static void waitReady(ActivityScenario<QuizDivision> scenario) {
        for (int i = 0; i < 100; i++) {
            boolean[] ready = {false};
            scenario.onActivity(a -> ready[0] = ((LinearLayout)a.findViewById(R.id.divisionRows)).getChildCount() == 3);
            if (ready[0]) return;
            android.os.SystemClock.sleep(100);
        }
        fail("Division content did not load");
    }

    private DivisionMatchingGame game(QuizDivision activity) {
        try {
            java.lang.reflect.Field field = QuizDivision.class.getDeclaredField("game");
            field.setAccessible(true);
            return (DivisionMatchingGame) field.get(activity);
        } catch (Exception error) { throw new AssertionError(error); }
    }
    private View slot(QuizDivision activity, int row) {
        return activity.findViewById(R.id.divisionRows).findViewWithTag("slot_" + row);
    }
    private View object(QuizDivision activity, int object) {
        LinearLayout tray = activity.findViewById(R.id.divisionTray);
        int position = game(activity).round().objects.indexOf(object);
        return ((LinearLayout) tray.getChildAt(position)).getChildAt(0);
    }

    @Test public void tapMatchingRecreationResetAndLockedRound() {
        assumeTrue("Requires signed-out test device; never writes a live student attempt",
                FirebaseAuth.getInstance().getCurrentUser() == null);
        try (ActivityScenario<QuizDivision> scenario = ActivityScenario.launch(QuizDivision.class)) {
            DivisionQuizTest.waitReady(scenario);
            scenario.onActivity(activity -> {
                assertFalse(activity.findViewById(R.id.btnDivisionSubmit).isEnabled());
                int chosen = game(activity).round().characters.get(0);
                object(activity, chosen).performClick();
                slot(activity, 0).performClick();
                assertEquals(chosen, game(activity).round().slots[0]);
                assertEquals(slot(activity, 0).getLayoutParams().width, slot(activity, 0).getLayoutParams().height);
            });
            scenario.recreate();
            DivisionQuizTest.waitReady(scenario);
            scenario.onActivity(activity -> {
                assertEquals((int) game(activity).round().characters.get(0), game(activity).round().slots[0]);
                activity.findViewById(R.id.btnDivisionReset).performClick();
                assertArrayEquals(new int[]{-1, -1, -1}, game(activity).round().slots);
                for (int row = 0; row < 3; row++) {
                    object(activity, game(activity).round().characters.get(row)).performClick();
                    slot(activity, row).performClick();
                }
                assertTrue(activity.findViewById(R.id.btnDivisionSubmit).isEnabled());
                activity.findViewById(R.id.btnDivisionSubmit).performClick();
                assertEquals(3, game(activity).score());
                assertTrue(game(activity).round().submitted);
                assertEquals(View.GONE, activity.findViewById(R.id.divisionTray).getVisibility());
                assertFalse(slot(activity, 0).performClick());
            });
            scenario.recreate();
            DivisionQuizTest.waitReady(scenario);
            scenario.onActivity(activity -> {
                assertTrue(game(activity).round().submitted);
                activity.findViewById(R.id.btnDivisionNext).performClick();
                assertEquals(1, game(activity).current);
                assertArrayEquals(new int[]{-1, -1, -1}, game(activity).round().slots);
            });
        }
    }
}
