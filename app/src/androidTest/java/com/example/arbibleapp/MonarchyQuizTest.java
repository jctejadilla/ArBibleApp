package com.example.arbibleapp;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.firebase.auth.FirebaseAuth;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

@RunWith(AndroidJUnit4.class)
public class MonarchyQuizTest {
    @org.junit.Before public void offline() throws Exception {
        assumeTrue(FirebaseAuth.getInstance().getCurrentUser() == null);
        com.google.android.gms.tasks.Tasks.await(com.google.firebase.firestore.FirebaseFirestore.getInstance().disableNetwork());
    }
    @org.junit.After public void online() { com.google.firebase.firestore.FirebaseFirestore.getInstance().enableNetwork(); }
    static void waitReady(ActivityScenario<QuizMonarchy> scenario) {
        for (int i = 0; i < 100; i++) {
            boolean[] ready = {false};
            scenario.onActivity(a -> ready[0] = ((LinearLayout)a.findViewById(R.id.monarchyGrid)).getChildCount() == 2);
            if (ready[0]) return;
            android.os.SystemClock.sleep(100);
        }
        fail("Monarchy content did not load");
    }
    private MonarchySequenceGame game(QuizMonarchy activity) {
        try {
            java.lang.reflect.Field field = QuizMonarchy.class.getDeclaredField("game");
            field.setAccessible(true);
            return (MonarchySequenceGame) field.get(activity);
        } catch (Exception error) {
            throw new AssertionError(error);
        }
    }
    private View scene(QuizMonarchy activity, int scene) {
        LinearLayout grid = activity.findViewById(R.id.monarchyGrid);
        for (int r = 0; r < grid.getChildCount(); r++) {
            LinearLayout row = (LinearLayout) grid.getChildAt(r);
            for (int c = 0; c < row.getChildCount(); c++) {
                View card = row.getChildAt(c);
                if (Integer.valueOf(scene).equals(card.getTag())) return card;
            }
        }
        throw new AssertionError("Scene missing");
    }
    private List<Integer> order(QuizMonarchy activity) {
        List<Integer> order = new ArrayList<>();
        LinearLayout grid = activity.findViewById(R.id.monarchyGrid);
        for (int r = 0; r < 2; r++) for (int c = 0; c < 3; c++)
            order.add((Integer) ((LinearLayout) grid.getChildAt(r)).getChildAt(c).getTag());
        return order;
    }
    private int uniformHeight(QuizMonarchy activity) {
        LinearLayout grid = activity.findViewById(R.id.monarchyGrid);
        int width = Math.round(288 * activity.getResources().getDisplayMetrics().density);
        grid.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        grid.layout(0, 0, width, grid.getMeasuredHeight());
        int height = scene(activity, 0).getHeight();
        int cardWidth = scene(activity, 0).getWidth();
        assertTrue(height > 0);
        for (int i = 0; i < 6; i++) {
            assertEquals("Uniform card height", height, scene(activity, i).getHeight());
            assertTrue("Uniform columns within pixel rounding",
                    Math.abs(cardWidth - scene(activity, i).getWidth()) <= 1);
        }
        return height;
    }
    private void assertSymbols(QuizMonarchy activity, String... expected) {
        for (int i = 0; i < expected.length; i++) {
            androidx.cardview.widget.CardView card = (androidx.cardview.widget.CardView) scene(activity, i);
            LinearLayout content = (LinearLayout) card.getChildAt(0);
            assertEquals(expected[i], ((TextView) content.getChildAt(0)).getText().toString());
        }
    }
    private String text(QuizMonarchy activity, int id) {
        return ((TextView) activity.findViewById(id)).getText().toString();
    }
    private void screenshot(String name) throws Exception {
        if (!"true".equals(InstrumentationRegistry.getArguments().getString("captureMonarchyScreenshots"))) return;
        Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(bitmap);
        File dir = InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null);
        try (FileOutputStream out = new FileOutputStream(new File(dir, name + ".png"))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        bitmap.recycle();
    }

    @Test public void editingPartialCreditCorrectionAndRotation() throws Exception {
        assumeTrue("Use a signed-out test device", FirebaseAuth.getInstance().getCurrentUser() == null);
        final List<List<Integer>> initial = new ArrayList<>();
        final int[] cardHeight = new int[1];
        try (ActivityScenario<QuizMonarchy> scenario = ActivityScenario.launch(QuizMonarchy.class)) {
            waitReady(scenario);
            screenshot("monarchy-grid");
            scenario.onActivity(activity -> {
                initial.add(order(activity));
                cardHeight[0] = uniformHeight(activity);
                assertFalse(activity.findViewById(R.id.btnMonarchySubmit).isEnabled());
                scene(activity, 4).performClick();
                assertEquals(cardHeight[0], uniformHeight(activity));
                scene(activity, 4).performClick();
                assertEquals(cardHeight[0], uniformHeight(activity));
                assertEquals(activity.getString(R.string.monarchy_selected, 1),
                        text(activity, R.id.tvMonarchySelected));
                scene(activity, 2).performClick();
                activity.findViewById(R.id.btnMonarchyUndo).performClick();
                assertEquals(cardHeight[0], uniformHeight(activity));
                assertEquals(activity.getString(R.string.monarchy_selected, 1),
                        text(activity, R.id.tvMonarchySelected));
                activity.findViewById(R.id.btnMonarchyReset).performClick();
                assertEquals(cardHeight[0], uniformHeight(activity));
                assertEquals(activity.getString(R.string.monarchy_selected, 0),
                        text(activity, R.id.tvMonarchySelected));
                scene(activity, 1).performClick();
                scene(activity, 0).performClick();
            });
            scenario.recreate();
            waitReady(scenario);
            scenario.onActivity(activity -> {
                assertEquals(initial.get(0), order(activity));
                assertEquals(cardHeight[0], uniformHeight(activity));
                assertEquals(activity.getString(R.string.monarchy_selected, 2),
                        text(activity, R.id.tvMonarchySelected));
                for (int i = 2; i < 6; i++) scene(activity, i).performClick();
                assertTrue(activity.findViewById(R.id.btnMonarchySubmit).isEnabled());
                activity.findViewById(R.id.btnMonarchySubmit).performClick();
                activity.findViewById(R.id.btnMonarchySubmit).performClick();
                assertSymbols(activity, "x", "x", "+", "+", "+", "+");
                assertEquals(4, game(activity).score());
                for (int i = 0; i < 6; i++) assertFalse(scene(activity, i).isClickable());
                assertFalse(activity.findViewById(R.id.btnMonarchyReset).isEnabled());
                assertEquals(cardHeight[0], uniformHeight(activity));
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            screenshot("monarchy-correction");
            scenario.recreate();
            waitReady(scenario);
            scenario.onActivity(activity -> {
                assertEquals(initial.get(0), order(activity));
                assertEquals(cardHeight[0], uniformHeight(activity));
                assertSymbols(activity, "x", "x", "+", "+", "+", "+");
                activity.findViewById(R.id.btnMonarchyNext).performClick();
                assertEquals(MonarchyScenarioBank.TITLES[1], text(activity, R.id.tvMonarchyScenario));
                assertFalse(activity.findViewById(R.id.btnMonarchySubmit).isEnabled());
            });
        }
    }

    @Test public void allThreePerfectRoundsFinishWith18AndNoUnnecessaryCorrection() {
        assumeTrue("Use a signed-out test device", FirebaseAuth.getInstance().getCurrentUser() == null);
        try (ActivityScenario<QuizMonarchy> scenario = ActivityScenario.launch(QuizMonarchy.class)) {
            waitReady(scenario);
            for (int r = 0; r < 3; r++) {
                final int round = r;
                scenario.onActivity(activity -> {
                    assertEquals(MonarchyScenarioBank.TITLES[round], text(activity, R.id.tvMonarchyScenario));
                    int height = uniformHeight(activity);
                    for (int i = 0; i < 6; i++) scene(activity, i).performClick();
                    assertEquals(height, uniformHeight(activity));
                    activity.findViewById(R.id.btnMonarchySubmit).performClick();
                    assertEquals(height, uniformHeight(activity));
                    assertSymbols(activity, "+", "+", "+", "+", "+", "+");
                    if (round < 2) activity.findViewById(R.id.btnMonarchyNext).performClick();
                });
            }
            scenario.recreate();
            waitReady(scenario);
            scenario.onActivity(activity -> {
                assertEquals(18, game(activity).score());
                assertEquals(activity.getString(R.string.monarchy_finish), text(activity, R.id.btnMonarchyNext));
                activity.findViewById(R.id.btnMonarchyNext).performClick();
                assertTrue(game(activity).complete());
                assertTrue(activity.findViewById(R.id.btnMonarchyNext).isEnabled());
            });
        }
    }

    @Test public void historicalCaptionsAndFullSequencesRemainReadableWithMissingAssets() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
            Configuration config = new Configuration(target.getResources().getConfiguration());
            config.fontScale = 1.5f;
            Context context = new ContextThemeWrapper(target.createConfigurationContext(config), R.style.Theme_ArBibleApp);
            MonarchySequenceGame game = new MonarchySequenceGame(0);
            for (int i : new int[]{1,0,2,3,4,5}) game.round().select(i);
            game.round().submit();
            MonarchySequenceResult snapshot = MonarchySequenceResult.from(game, 0);
            snapshot.selectedKeys.set(0, "unknown");
            snapshot.selectedCaptions.set(0, "Saved historical caption that should remain fully readable");
            LinearLayout review = (LinearLayout) MonarchySequenceView.review(context, snapshot, true);
            assertEquals(9, review.getChildCount()); // Title, score, headers, six positions.
            LinearLayout firstRow = (LinearLayout) review.getChildAt(3);
            LinearLayout selected = (LinearLayout) firstRow.getChildAt(0);
            assertEquals(View.GONE, selected.getChildAt(1).getVisibility());
            TextView caption = (TextView) selected.getChildAt(2);
            assertEquals(snapshot.selectedCaptions.get(0), caption.getText().toString());
            int width = Math.round(320 * context.getResources().getDisplayMetrics().density);
            review.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            review.layout(0, 0, width, review.getMeasuredHeight());
            assertEquals(caption.length(), caption.getLayout().getLineEnd(caption.getLineCount() - 1));
            LinearLayout hidden = (LinearLayout) MonarchySequenceView.review(context, snapshot, false);
            assertEquals(2, hidden.getChildCount());
            for (String[] keys : MonarchyScenarioBank.KEYS)
                for (String key : keys) assertNotEquals(0, MonarchyImages.resource(key));
        });
    }
}
