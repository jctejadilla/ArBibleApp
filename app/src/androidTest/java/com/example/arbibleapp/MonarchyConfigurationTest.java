package com.example.arbibleapp;

import android.graphics.Bitmap;
import android.view.View;
import android.widget.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.tasks.Tasks;
import java.lang.reflect.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

@RunWith(AndroidJUnit4.class)
public class MonarchyConfigurationTest {
    @Before public void offline() throws Exception {
        assumeTrue(FirebaseAuth.getInstance().getCurrentUser() == null);
        Tasks.await(FirebaseFirestore.getInstance().disableNetwork());
    }
    @After public void online() { FirebaseFirestore.getInstance().enableNetwork(); }
    private static Object field(Object target, String name) {
        try { Field f = target.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(target); }
        catch (Exception e) { throw new AssertionError(e); }
    }
    private static Object call(Object target, String name, Class<?> type, Object value) {
        try { Method m = target.getClass().getDeclaredMethod(name, type); m.setAccessible(true); return m.invoke(target, value); }
        catch (Exception e) { throw new AssertionError(e); }
    }
    private Button button(android.view.ViewGroup group, String label) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof Button && label.contentEquals(((Button)child).getText())) return (Button)child;
            if (child instanceof android.view.ViewGroup) {
                Button found = button((android.view.ViewGroup)child, label);
                if (found != null) return found;
            }
        }
        return null;
    }
    @Test public void teacherCanReorderScenesRestorePicturesAndSwitchKings() {
        try (ActivityScenario<TeacherProposeQuestionActivity> scenario = ActivityScenario.launch(TeacherProposeQuestionActivity.class)) {
            scenario.onActivity(a -> a.findViewById(R.id.cardStory7).performClick());
            for (int i = 0; i < 100; i++) {
                boolean[] ready = {false};
                scenario.onActivity(a -> ready[0] = (Boolean)field(field(a, "monarchyEditor"), "ready"));
                if (ready[0]) break;
                android.os.SystemClock.sleep(100);
            }
            scenario.onActivity(a -> {
                Object editor = field(a, "monarchyEditor");
                assertTrue((Boolean)field(editor, "ready"));
                assertEquals(3, ((Spinner)a.findViewById(R.id.spOriginalQuestion)).getCount());
                assertEquals(View.GONE, a.findViewById(R.id.spCorrectAnswer).getVisibility());
                List<?> draft = (List<?>)field(editor, "draft"); assertEquals(6, draft.size());
                Object first = draft.get(0);
                LinearLayout rows = (LinearLayout)field(editor, "rows");
                EditText caption = (EditText)field(first, "input"); caption.setText("New scene description");
                assertTrue(button(rows, "Move down").performClick());
                assertSame(first, draft.get(1));
                assertEquals("New scene description", field(first, "caption"));
                button(rows, "Original").performClick();
                assertEquals("DEFAULT", field(draft.get(0), "action"));
                button(rows, "Keep current").performClick();
                assertEquals("KEEP", field(draft.get(0), "action"));
                call(editor, "select", int.class, 1);
                assertEquals(MonarchyScenarioBank.TITLES[1], ((EditText)a.findViewById(R.id.etProposedQuestion)).getText().toString());
                assertEquals("david_sheep", ((MonarchyContentBank.Scene)field(((List<?>)field(editor, "draft")).get(0), "scene")).key);
                a.findViewById(R.id.btnBack).performClick();
                a.findViewById(R.id.cardStory6).performClick();
                assertEquals(View.GONE, rows.getVisibility());
                assertEquals(10, ((Spinner)a.findViewById(R.id.spOriginalQuestion)).getCount());
            });
        }
    }
    @Test public void reorderedQuizContentAndAnswerFeedbackSurviveRecreation() {
        try (ActivityScenario<QuizMonarchy> scenario = ActivityScenario.launch(QuizMonarchy.class)) {
            MonarchyQuizTest.waitReady(scenario);
            scenario.onActivity(a -> {
                List<MonarchyContentBank.Scenario> bank = MonarchyContentBank.defaults();
                bank.get(0).title = "Custom Saul sequence";
                Collections.reverse(bank.get(0).scenes);
                call(a, "persistContent", List.class, bank);
            });
            for (int i = 0; i < 100; i++) {
                boolean[] ready = {false};
                scenario.onActivity(a -> ready[0] = "Custom Saul sequence".contentEquals(((TextView)a.findViewById(R.id.tvMonarchyScenario)).getText()));
                if (ready[0]) break;
                android.os.SystemClock.sleep(100);
            }
            scenario.onActivity(a -> {
                assertEquals("Custom Saul sequence", ((TextView)a.findViewById(R.id.tvMonarchyScenario)).getText().toString());
                MonarchySequenceGame game = (MonarchySequenceGame)field(a, "game");
                for (int i = 0; i < 6; i++) game.round().select(i);
                a.findViewById(R.id.btnMonarchySubmit).performClick();
                assertEquals(6, game.score());
                List<MonarchyContentBank.Scenario> content = (List<MonarchyContentBank.Scenario>)field(a, "content");
                MonarchySequenceResult result = MonarchySequenceResult.from(game, 0, content);
                assertEquals("saul_gilgal", result.correctKeys.get(0));
            });
            scenario.recreate(); MonarchyQuizTest.waitReady(scenario);
            scenario.onActivity(a -> {
                assertEquals("Custom Saul sequence", ((TextView)a.findViewById(R.id.tvMonarchyScenario)).getText().toString());
                assertEquals(6, ((MonarchySequenceGame)field(a, "game")).score());
                List<MonarchyContentBank.Scenario> content = (List<MonarchyContentBank.Scenario>)field(a, "content");
                assertEquals("saul_gilgal", content.get(0).scenes.get(0).key);
            });
        }
    }
    @Test public void uploadCompressionFitsSixPictureBudget() throws Exception {
        Bitmap bitmap = Bitmap.createBitmap(800, 800, Bitmap.Config.ARGB_8888);
        int[] noise = new int[800 * 800]; Random random = new Random(17);
        for (int i = 0; i < noise.length; i++) noise[i] = 0xff000000 | random.nextInt(0x1000000);
        bitmap.setPixels(noise, 0, 800, 0, 0, 800, 800);
        byte[] bytes = LawImageCodec.compress(bitmap, MonarchyContentBank.IMAGE_LIMIT);
        bitmap.recycle();
        assertTrue(bytes.length <= MonarchyContentBank.IMAGE_LIMIT);
        Bitmap decoded = LawImageCodec.decode(bytes);
        assertNotNull(decoded); assertTrue(decoded.getWidth() <= 1024); decoded.recycle();
    }
}
