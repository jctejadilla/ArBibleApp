package com.example.arbibleapp;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
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
import java.lang.reflect.Field;
import java.util.Arrays;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

@RunWith(AndroidJUnit4.class)
public class ReturnQuizTest {
    @org.junit.Before public void offline() throws Exception {
        assumeTrue(FirebaseAuth.getInstance().getCurrentUser() == null);
        com.google.android.gms.tasks.Tasks.await(com.google.firebase.firestore.FirebaseFirestore.getInstance().disableNetwork());
    }
    @org.junit.After public void online() { com.google.firebase.firestore.FirebaseFirestore.getInstance().enableNetwork(); }
    static void waitReady(ActivityScenario<QuizReturn> scenario) {
        for (int i = 0; i < 100; i++) {
            boolean[] ready = {false};
            scenario.onActivity(a -> {
                try { Field f = QuizReturn.class.getDeclaredField("contentReady"); f.setAccessible(true); ready[0] = f.getBoolean(a) && a.hasWindowFocus() && a.findViewById(R.id.returnBoard).isLaidOut(); }
                catch (Exception e) { throw new AssertionError(e); }
            });
            if (ready[0]) return;
            SystemClock.sleep(100);
        }
        fail("Return pictures did not load");
    }

    private void waitForAnimations(ActivityScenario<QuizReturn> scenario) {
        for (int i = 0; i < 100; i++) {
            boolean[] finished = {false};
            scenario.onActivity(a -> {
                try {
                    Field f = QuizReturn.class.getDeclaredField("animations"); f.setAccessible(true);
                    finished[0] = f.getInt(a) == 0;
                } catch (Exception error) { throw new AssertionError(error); }
            });
            if (finished[0]) return;
            SystemClock.sleep(50);
        }
        fail("Card animation did not finish");
    }
    private ReturnMatchingGame game(QuizReturn activity) {
        try {
            Field field = QuizReturn.class.getDeclaredField("game");
            field.setAccessible(true);
            return (ReturnMatchingGame) field.get(activity);
        } catch (Exception error) { throw new AssertionError(error); }
    }

    private View card(QuizReturn activity, int index) {
        LinearLayout board = activity.findViewById(R.id.returnBoard);
        return ((LinearLayout) board.getChildAt(index / 3)).getChildAt(index % 3);
    }

    private int[][] dimensions(QuizReturn activity) {
        LinearLayout board = activity.findViewById(R.id.returnBoard);
        int width = board.getWidth();
        assertTrue("Board must be laid out", width > 0);
        board.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        board.layout(board.getLeft(), board.getTop(), board.getLeft() + width,
                board.getTop() + board.getMeasuredHeight());
        int[][] sizes = new int[12][2];
        for (int i = 0; i < 12; i++) {
            sizes[i][0] = card(activity, i).getWidth();
            sizes[i][1] = card(activity, i).getHeight();
            assertTrue("Equal-weight columns may differ by one pixel",
                    Math.abs(sizes[i][0] - sizes[0][0]) <= 1);
            assertEquals("All cards must have the same height", sizes[0][1], sizes[i][1]);
        }
        return sizes;
    }

    private void assertDimensions(QuizReturn activity, int[][] expected) {
        int[][] actual = dimensions(activity);
        for (int i = 0; i < 12; i++) assertArrayEquals("Card " + i + " resized", expected[i], actual[i]);
    }
    private int other(ReturnMatchingGame game, int first, boolean match) {
        for (int i = 0; i < 12; i++)
            if (i != first && (game.deck[i] == game.deck[first]) == match) return i;
        throw new AssertionError();
    }

    private void screenshot(String name) throws Exception {
        if (!"true".equals(InstrumentationRegistry.getArguments().getString("captureReturnScreenshots"))) return;
        Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(bitmap);
        File directory = InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null);
        try (FileOutputStream output = new FileOutputStream(new File(directory, name + ".png"))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
        }
        bitmap.recycle();
    }

    @Test public void firstCardMatchesAndPendingMismatchSurviveRecreation() throws Exception {
        assumeTrue("Use a signed-out test device to avoid live sessions",
                FirebaseAuth.getInstance().getCurrentUser() == null);
        final int[] positions = new int[3];
        final int[][] deck = new int[1][];
        final int[][][] sizes = new int[1][][];
        try (ActivityScenario<QuizReturn> scenario = ActivityScenario.launch(QuizReturn.class)) {
            waitReady(scenario);
            scenario.onActivity(activity -> {
                ReturnMatchingGame model = game(activity);
                sizes[0] = dimensions(activity);
                deck[0] = model.deck.clone();
                positions[0] = other(model, 0, true);
                positions[1] = other(model, 0, false);
                for (int i = 0; i < 12; i++)
                    assertEquals(activity.getString(R.string.return_hidden_card, i + 1),
                            card(activity, i).getContentDescription().toString());
                assertEquals(View.GONE, activity.findViewById(R.id.btnReturnFinish).getVisibility());
                card(activity, 0).performClick();
                card(activity, positions[0]).performClick(); // Rapid tap during animation must be ignored.
                assertEquals(0, model.moves);
            });
            SystemClock.sleep(350);
            scenario.recreate();
            waitReady(scenario);
            scenario.onActivity(activity -> {
                assertDimensions(activity, sizes[0]);
                assertEquals(0, game(activity).first);
                assertArrayEquals(deck[0], game(activity).deck);
                card(activity, positions[0]).performClick();
            });
            SystemClock.sleep(350);
            screenshot("return-matched-pair");
            scenario.onActivity(activity -> {
                ReturnMatchingGame model = game(activity);
                assertDimensions(activity, sizes[0]);
                assertEquals(1, model.score());
                assertFalse(card(activity, 0).isClickable());
                card(activity, positions[1]).performClick();
                positions[2] = -1;
                for (int i = 0; i < 12; i++)
                    if (!model.matched[i] && model.deck[i] != model.deck[positions[1]]) {
                        positions[2] = i; break;
                    }
            });
            SystemClock.sleep(350);
            scenario.onActivity(activity -> card(activity, positions[2]).performClick());
            scenario.recreate();
            waitReady(scenario);
            SystemClock.sleep(1700);
            scenario.onActivity(activity -> {
                ReturnMatchingGame model = game(activity);
                assertArrayEquals(deck[0], model.deck);
                assertDimensions(activity, sizes[0]);
                assertEquals(1, model.score());
                assertEquals(2, model.moves);
                assertEquals(1, model.misses.size());
                assertEquals(-1, model.first);
                assertEquals(-1, model.second);
                assertTrue(card(activity, positions[1]).isClickable());
            });
        }
    }

    @Test public void completingSixPairsShowsFinishAndKeepsResultsWhenSignedOut() throws Exception {
        assumeTrue("Use a signed-out test device to avoid live sessions",
                FirebaseAuth.getInstance().getCurrentUser() == null);
        try (ActivityScenario<QuizReturn> scenario = ActivityScenario.launch(QuizReturn.class)) {
            waitReady(scenario);
            screenshot("return-new-board");
            for (int key = 0; key < 6; key++) {
                final int picture = key;
                final int[] pair = new int[2];
                scenario.onActivity(activity -> {
                    int found = 0;
                    for (int i = 0; i < 12; i++) if (game(activity).deck[i] == picture) pair[found++] = i;
                    card(activity, pair[0]).performClick();
                    assertEquals("First card selected", pair[0], game(activity).first);
                });
                waitForAnimations(scenario);
                scenario.onActivity(activity -> {
                    card(activity, pair[1]).performClick();
                    assertEquals("Pair matched", picture + 1, game(activity).score());
                });
                waitForAnimations(scenario);
            }
            scenario.onActivity(activity -> {
                assertEquals(6, game(activity).score());
                assertEquals(6, game(activity).moves);
                assertTrue(activity.findViewById(R.id.btnReturnFinish).isEnabled());
                assertEquals(View.VISIBLE, activity.findViewById(R.id.btnReturnFinish).getVisibility());
            });
            screenshot("return-completed");
            scenario.recreate();
            waitReady(scenario);
            scenario.onActivity(activity -> {
                assertTrue(game(activity).complete());
                activity.findViewById(R.id.btnReturnFinish).performClick();
                assertEquals(activity.getString(R.string.return_sign_in),
                        ((TextView) activity.findViewById(R.id.tvReturnFeedback)).getText().toString());
                assertTrue(game(activity).complete());
            });
        }
    }

    @Test public void reviewShowsSavedPairsWithTextFallbackAndLargeText() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
            Configuration configuration = new Configuration(target.getResources().getConfiguration());
            configuration.fontScale = 1.5f;
            Context context = new ContextThemeWrapper(target.createConfigurationContext(configuration),
                    R.style.Theme_ArBibleApp);
            View view = LayoutInflater.from(context).inflate(R.layout.item_creation_missed_question, null);
            TeacherQuizResults.QuizResultModel result = new TeacherQuizResults.QuizResultModel();
            result.wrongAnswers = Arrays.asList("Move 3: Find the identical picture for Rebuilt temple");
            result.selectedAnswers = Arrays.asList("Ezra");
            result.correctAnswers = Arrays.asList("Rebuilt temple");
            result.selectedImageKeys = Arrays.asList("ezra");
            result.correctImageKeys = Arrays.asList("temple");
            ReturnMissedMatchView.bind(view, result, 0, false);
            assertEquals(View.GONE, view.findViewById(R.id.layoutReviewAnswers).getVisibility());
            ReturnMissedMatchView.bind(view, result, 0, true);
            assertNotNull(((ImageView) view.findViewById(R.id.ivReviewCorrect)).getDrawable());
            assertEquals("Ezra", ((TextView) view.findViewById(R.id.tvReviewSelected)).getText().toString());
            int width = (int) (280 * context.getResources().getDisplayMetrics().density);
            view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            view.layout(0, 0, width, view.getMeasuredHeight());
            TextView prompt = view.findViewById(R.id.tvMissedQuestion);
            assertEquals(prompt.length(), prompt.getLayout().getLineEnd(prompt.getLineCount() - 1));
            result.correctImageKeys = Arrays.asList("missing");
            result.selectedImageKeys = null;
            ReturnMissedMatchView.bind(view, result, 0, true);
            assertEquals(View.GONE, view.findViewById(R.id.ivReviewCorrect).getVisibility());
            assertEquals("Rebuilt temple", ((TextView) view.findViewById(R.id.tvReviewCorrect)).getText().toString());
            for (String key : ReturnMatchingGame.KEYS) assertNotEquals(0, ReturnImages.resource(key));
        });
    }
}
