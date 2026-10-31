package com.example.arbibleapp;

import android.app.Instrumentation;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.firebase.auth.FirebaseAuth;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

@RunWith(AndroidJUnit4.class)
public class DivisionDragTest {
    @org.junit.Before public void offline() throws Exception {
        assumeTrue(FirebaseAuth.getInstance().getCurrentUser() == null);
        com.google.android.gms.tasks.Tasks.await(com.google.firebase.firestore.FirebaseFirestore.getInstance().disableNetwork());
    }
    @org.junit.After public void online() { com.google.firebase.firestore.FirebaseFirestore.getInstance().enableNetwork(); }

    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
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
    private View object(QuizDivision activity, int id) {
        LinearLayout tray = activity.findViewById(R.id.divisionTray);
        return ((LinearLayout) tray.getChildAt(game(activity).round().objects.indexOf(id))).getChildAt(0);
    }
    private float[] center(View view) {
        Rect bounds = new Rect();
        assertTrue("Target visible", view.getGlobalVisibleRect(bounds));
        return new float[]{bounds.exactCenterX(), bounds.exactCenterY()};
    }
    private void motion(long down, int action, float x, float y) {
        MotionEvent event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, x, y, 0);
        event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        assertTrue("Injected touch", instrumentation.getUiAutomation().injectInputEvent(event, true));
        event.recycle();
    }
    private void move(long down, float[] from, float[] to) {
        for (int i = 1; i <= 24; i++) {
            float fraction = i / 24f;
            motion(down, MotionEvent.ACTION_MOVE, from[0] + (to[0] - from[0]) * fraction,
                    from[1] + (to[1] - from[1]) * fraction);
            SystemClock.sleep(i == 4 ? 150 : 20);
        }
    }
    private void drag(float[] from, float[] to, boolean longPress) {
        long down = SystemClock.uptimeMillis();
        motion(down, MotionEvent.ACTION_DOWN, from[0], from[1]);
        SystemClock.sleep(longPress ? 650 : 40);
        move(down, from, to);
        motion(down, MotionEvent.ACTION_UP, to[0], to[1]);
        SystemClock.sleep(350);
        instrumentation.waitForIdleSync();
    }
    @Test public void realTouchDragDropSwapReturnCancelAndLock() {
        assumeTrue("Fresh signed-out emulator required", FirebaseAuth.getInstance().getCurrentUser() == null);
        try (ActivityScenario<QuizDivision> scenario = ActivityScenario.launch(QuizDivision.class)) {
            DivisionQuizTest.waitReady(scenario);
            SystemClock.sleep(600);
            final float[][] points = new float[2][];
            scenario.onActivity(a -> { points[0] = center(object(a, 0)); points[1] = center(slot(a, 0)); });
            drag(points[0], points[1], false);
            scenario.onActivity(a -> {
                assertEquals("Direct drag places object", 0, game(a).round().slots[0]);
                points[0] = center(object(a, 1)); points[1] = center(slot(a, 1));
            });
            drag(points[0], points[1], true);
            scenario.onActivity(a -> {
                assertEquals("Long press also works", 1, game(a).round().slots[1]);
                points[0] = center(slot(a, 0)); points[1] = center(slot(a, 1));
            });
            drag(points[0], points[1], false);
            scenario.onActivity(a -> {
                assertArrayEquals(new int[]{1, 0, -1}, game(a).round().slots);
                points[0] = center(slot(a, 1)); points[1] = center(a.findViewById(R.id.divisionTray));
            });
            drag(points[0], points[1], false);
            scenario.onActivity(a -> {
                assertArrayEquals(new int[]{1, -1, -1}, game(a).round().slots);
                points[0] = center(slot(a, 0)); points[1] = center(a.findViewById(R.id.tvDivisionRound));
            });
            drag(points[0], points[1], false);
            scenario.onActivity(a -> {
                assertArrayEquals("Outside drop cancels", new int[]{1, -1, -1}, game(a).round().slots);
                object(a, 0).performClick(); slot(a, 1).performClick();
                object(a, 2).performClick(); slot(a, 2).performClick();
                a.findViewById(R.id.btnDivisionSubmit).performClick();
            });
            instrumentation.waitForIdleSync();
            SystemClock.sleep(250);
            scenario.onActivity(a -> {
                points[0] = center(slot(a, 0)); points[1] = center(slot(a, 1));
            });
            drag(points[0], points[1], false);
            scenario.onActivity(a -> {
                assertArrayEquals("Submitted answers locked", new int[]{1, 0, 2}, game(a).round().slots);
                assertEquals(slot(a, 0).getWidth(), slot(a, 0).getHeight());
                assertTrue(slot(a, 0).getWidth() <= Math.round(125 * a.getResources().getDisplayMetrics().density));
            });
        }
    }

    @Test public void edgeDragScrollsToHiddenThirdSlot() {
        assumeTrue("Fresh signed-out emulator required", FirebaseAuth.getInstance().getCurrentUser() == null);
        try (ActivityScenario<QuizDivision> scenario = ActivityScenario.launch(QuizDivision.class)) {
            DivisionQuizTest.waitReady(scenario);
            scenario.onActivity(a -> {
                ScrollView scroll = a.findViewById(R.id.divisionScroll);
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) scroll.getLayoutParams();
                params.weight = 0;
                params.height = Math.round(220 * a.getResources().getDisplayMetrics().density);
                scroll.setLayoutParams(params);
            });
            instrumentation.waitForIdleSync();
            SystemClock.sleep(400);
            final float[][] points = new float[3][];
            scenario.onActivity(a -> {
                points[0] = center(object(a, 2));
                Rect rect = new Rect();
                a.findViewById(R.id.divisionScroll).getGlobalVisibleRect(rect);
                points[1] = new float[]{center(slot(a, 0))[0], rect.bottom - 10};
            });
            long down = SystemClock.uptimeMillis();
            motion(down, MotionEvent.ACTION_DOWN, points[0][0], points[0][1]);
            move(down, points[0], points[1]);
            SystemClock.sleep(1100);
            scenario.onActivity(a -> {
                assertTrue("List scrolls during drag", a.findViewById(R.id.divisionScroll).getScrollY() > 0);
                points[2] = center(slot(a, 2));
            });
            move(down, points[1], points[2]);
            motion(down, MotionEvent.ACTION_UP, points[2][0], points[2][1]);
            SystemClock.sleep(350);
            scenario.onActivity(a -> assertEquals(2, game(a).round().slots[2]));
        }
    }
}
