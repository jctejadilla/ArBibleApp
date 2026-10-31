package com.example.arbibleapp;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Button;
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
import java.util.List;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

@RunWith(AndroidJUnit4.class)
public class BirthQuizTest {
    @org.junit.Before public void offline() throws Exception {
        assumeTrue(FirebaseAuth.getInstance().getCurrentUser() == null);
        com.google.android.gms.tasks.Tasks.await(com.google.firebase.firestore.FirebaseFirestore.getInstance().disableNetwork());
    }
    @org.junit.After public void online() { com.google.firebase.firestore.FirebaseFirestore.getInstance().enableNetwork(); }
    static void waitReady(ActivityScenario<QuizBirth> scenario) {
        for (int i = 0; i < 100; i++) {
            boolean[] ready = {false};
            scenario.onActivity(a -> {
                try { java.lang.reflect.Field f = QuizBirth.class.getDeclaredField("contentReady"); f.setAccessible(true); ready[0] = f.getBoolean(a); }
                catch (Exception e) { throw new AssertionError(e); }
            });
            if (ready[0]) return;
            android.os.SystemClock.sleep(100);
        }
        fail("Birth content did not load");
    }
    private BirthWordGame game(QuizBirth activity) {
        try {
            java.lang.reflect.Field field = QuizBirth.class.getDeclaredField("game");
            field.setAccessible(true);
            return (BirthWordGame) field.get(activity);
        } catch (Exception error) { throw new AssertionError(error); }
    }
    private LinearLayout slots(QuizBirth a) { return a.findViewById(R.id.birthSlots); }
    private Button tile(QuizBirth a, int index) {
        LinearLayout bank = a.findViewById(R.id.birthTiles);
        return (Button) ((LinearLayout) bank.getChildAt(index / 5)).getChildAt(index % 5);
    }
    private String entered(QuizBirth a) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < slots(a).getChildCount(); i++)
            text.append(((TextView) slots(a).getChildAt(i)).getText());
        return text.toString();
    }
    private String text(QuizBirth a, int id) {
        return ((TextView) a.findViewById(id)).getText().toString();
    }
    private List<String> bank(QuizBirth a) {
        List<String> values = new ArrayList<>();
        for (int t = 0; t < 10; t++) values.add(tile(a, t).getText().toString());
        return values;
    }
    private void solve(QuizBirth a, String answer) {
        for (int p = 0; p < answer.length(); p++) if (entered(a).charAt(p) == '_') {
            boolean found = false;
            for (int t = 0; t < 10; t++) {
                Button button = tile(a, t);
                if (button.isEnabled() && button.getText().charAt(0) == answer.charAt(p)) {
                    button.performClick();
                    found = true;
                    break;
                }
            }
            assertTrue(found);
        }
        assertEquals(answer, entered(a));
    }
    private void screenshot(String name) throws Exception {
        if (!"true".equals(InstrumentationRegistry.getArguments().getString("captureBirthScreenshots"))) return;
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        Bitmap image = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(image);
        File dir = InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null);
        try (FileOutputStream out = new FileOutputStream(new File(dir, name + ".png"))) {
            image.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        image.recycle();
    }

    @Test public void tileEditingHintsAndRotationKeepExactState() throws Exception {
        assumeTrue("Use a signed-out test device", FirebaseAuth.getInstance().getCurrentUser() == null);
        List<String> partial = new ArrayList<>();
        List<List<String>> banks = new ArrayList<>();
        try (ActivityScenario<QuizBirth> scenario = ActivityScenario.launch(QuizBirth.class)) {
            waitReady(scenario);
            screenshot("birth-question");
            scenario.onActivity(a -> {
                LinearLayout grid = a.findViewById(R.id.birthTiles);
                assertEquals(2, grid.getChildCount());
                for (int row = 0; row < 2; row++) assertEquals(5, ((LinearLayout) grid.getChildAt(row)).getChildCount());
                assertFalse(a.findViewById(R.id.btnBirthSubmit).isEnabled());
                String hints = entered(a);
                assertEquals(2, hints.replace("_", "").length());
                tile(a, 0).performClick();
                String once = entered(a);
                tile(a, 0).performClick();
                assertEquals(once, entered(a));
                assertFalse(tile(a, 0).isEnabled());
                int position = hints.indexOf('_');
                slots(a).getChildAt(position).performClick();
                assertEquals(hints, entered(a));
                assertTrue(tile(a, 0).isEnabled());
                tile(a, 1).performClick();
                a.findViewById(R.id.btnBirthClear).performClick();
                assertEquals(hints, entered(a));
                tile(a, 2).performClick();
                partial.add(entered(a));
                banks.add(bank(a));
            });
            scenario.recreate(); waitReady(scenario);
            scenario.onActivity(a -> {
                assertEquals(partial.get(0), entered(a));
                assertEquals(banks.get(0), bank(a));
                assertFalse(tile(a, 2).isEnabled());
                a.findViewById(R.id.btnBirthClear).performClick();
                solve(a, game(a).round().answer);
                a.findViewById(R.id.btnBirthSubmit).performClick();
                a.findViewById(R.id.btnBirthSubmit).performClick();
                assertEquals(1, game(a).score());
                assertTrue(game(a).round().correct());
            });
            scenario.recreate(); waitReady(scenario);
            scenario.onActivity(a -> {
                assertEquals(game(a).round().answer, entered(a));
                for (int t = 0; t < 10; t++) assertFalse(tile(a, t).isEnabled());
                a.findViewById(R.id.btnBirthNext).performClick();
                assertEquals(4, slots(a).getChildCount());
                assertEquals(1, game(a).current);
            });
        }
    }

    @Test public void wrongAnswerThenRemainingQuestionsRevealNamesAndFinish() throws Exception {
        assumeTrue("Use a signed-out test device", FirebaseAuth.getInstance().getCurrentUser() == null);
        try (ActivityScenario<QuizBirth> scenario = ActivityScenario.launch(QuizBirth.class)) {
            waitReady(scenario);
            String[] firstAnswer = {null};
            scenario.onActivity(a -> {
                firstAnswer[0] = game(a).round().answer;
                int p = entered(a).indexOf('_');
                int wrong = 0;
                while (tile(a, wrong).getText().charAt(0) == firstAnswer[0].charAt(p)) wrong++;
                tile(a, wrong).performClick();
                for (int t = 0; t < 10 && entered(a).contains("_"); t++)
                    if (tile(a, t).isEnabled()) tile(a, t).performClick();
                a.findViewById(R.id.btnBirthSubmit).performClick();
                assertTrue(game(a).round().submitted); assertFalse(game(a).round().correct());
                assertEquals(0, game(a).score());
                assertEquals(View.GONE, a.findViewById(R.id.btnBirthClear).getVisibility());
            });
            screenshot("birth-correction");
            for (int i = 1; i < 10; i++) {
                scenario.onActivity(a -> {
                    a.findViewById(R.id.btnBirthNext).performClick();
                    assertNotNull(((android.widget.ImageView) a.findViewById(R.id.ivBirthPicture)).getDrawable());
                    solve(a, game(a).round().answer);
                    a.findViewById(R.id.btnBirthSubmit).performClick();
                    assertTrue(game(a).round().correct());
                });
            }
            scenario.recreate(); waitReady(scenario);
            scenario.onActivity(a -> {
                assertEquals(9, game(a).score()); assertTrue(game(a).complete());
                assertEquals(a.getString(R.string.birth_finish), text(a, R.id.btnBirthNext));
                a.findViewById(R.id.btnBirthNext).performClick();
                assertFalse(a.isFinishing());
            });
        }
    }

    @Test public void savedReviewHonorsVisibilityAndHistoricalAnswersAtLargeFont() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
            Configuration config = new Configuration(target.getResources().getConfiguration());
            config.fontScale = 1.5f;
            Context context = new ContextThemeWrapper(target.createConfigurationContext(config), R.style.Theme_ArBibleApp);
            BirthWordResult result = new BirthWordResult();
            result.imageKey = "missing_asset";
            result.displayName = "John the Baptist";
            result.expectedWord = "JOHN";
            result.selectedWord = "JONH";
            LinearLayout shown = (LinearLayout) BirthWordReview.create(context, result, true);
            assertEquals(4, shown.getChildCount());
            assertEquals(View.GONE, shown.getChildAt(1).getVisibility());
            assertTrue(((TextView) shown.getChildAt(2)).getText().toString().contains("JONH"));
            TextView answer = (TextView) shown.getChildAt(3);
            assertTrue(answer.getText().toString().contains("John the Baptist"));
            int width = Math.round(320 * context.getResources().getDisplayMetrics().density);
            shown.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            shown.layout(0, 0, width, shown.getMeasuredHeight());
            assertEquals(answer.length(), answer.getLayout().getLineEnd(answer.getLineCount() - 1));
            LinearLayout hidden = (LinearLayout) BirthWordReview.create(context, result, false);
            assertEquals(2, hidden.getChildCount());
            for (String key : BirthWordBank.KEYS) assertNotEquals(0, BirthImages.resource(key));
        });
    }
}
