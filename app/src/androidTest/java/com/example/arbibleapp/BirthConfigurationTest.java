package com.example.arbibleapp;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.SystemClock;
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
public class BirthConfigurationTest {
    @Before public void offline() throws Exception {
        assumeTrue(FirebaseAuth.getInstance().getCurrentUser() == null);
        Tasks.await(FirebaseFirestore.getInstance().disableNetwork());
    }
    @After public void online() { FirebaseFirestore.getInstance().enableNetwork(); }
    private static Object field(Object target, String name) {
        try { Field f = target.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(target); }
        catch (Exception e) { throw new AssertionError(e); }
    }
    private static void set(Object target, String name, Object value) {
        try { Field f = target.getClass().getDeclaredField(name); f.setAccessible(true); f.set(target, value); }
        catch (Exception e) { throw new AssertionError(e); }
    }
    private static void call(Object target, String name, Class<?> type, Object value) {
        try { Method m = target.getClass().getDeclaredMethod(name, type); m.setAccessible(true); m.invoke(target, value); }
        catch (Exception e) { throw new AssertionError(e); }
    }
    private void waitEditor(ActivityScenario<TeacherProposeQuestionActivity> scenario, boolean loading) {
        for (int i = 0; i < 100; i++) {
            boolean[] done = {false};
            scenario.onActivity(a -> {
                Object editor = field(a, "birthEditor");
                done[0] = loading ? (Boolean)field(editor, "ready") : !(Boolean)field(editor, "processing");
            });
            if (done[0]) return;
            SystemClock.sleep(100);
        }
        fail("Birth editor did not become ready");
    }
    @Test public void teacherHasTenSubjectsAndSeparateAnswerAndPictureActions() {
        try (ActivityScenario<TeacherProposeQuestionActivity> scenario = ActivityScenario.launch(TeacherProposeQuestionActivity.class)) {
            scenario.onActivity(a -> a.findViewById(R.id.cardStory10).performClick()); waitEditor(scenario, true);
            scenario.onActivity(a -> {
                Object editor = field(a, "birthEditor");
                assertEquals(10, ((Spinner)a.findViewById(R.id.spOriginalQuestion)).getCount());
                assertEquals("Jesus", ((EditText)a.findViewById(R.id.etProposedQuestion)).getText().toString());
                assertEquals(View.GONE, a.findViewById(R.id.spCorrectAnswer).getVisibility());
                ((Button)field(editor, "original")).performClick(); assertEquals("DEFAULT", field(editor, "action"));
                assertTrue(((TextView)field(editor, "status")).getText().toString().contains("this item"));
                ((Button)field(editor, "keep")).performClick(); assertEquals("KEEP", field(editor, "action"));
                call(editor, "select", int.class, 4);
                assertEquals("JOHN", ((EditText)field(editor, "answer")).getText().toString());
                assertEquals("John the Baptist", ((EditText)a.findViewById(R.id.etProposedQuestion)).getText().toString());
                a.findViewById(R.id.btnBack).performClick(); a.findViewById(R.id.cardStory8).performClick();
                assertEquals(View.GONE, ((LinearLayout)field(editor, "container")).getVisibility());
                assertEquals(9, ((Spinner)a.findViewById(R.id.spOriginalQuestion)).getCount());
            });
        }
    }
    @Test public void cancelledFailedAndStaleUploadsPreserveSelectedSubject() {
        try (ActivityScenario<TeacherProposeQuestionActivity> scenario = ActivityScenario.launch(TeacherProposeQuestionActivity.class)) {
            scenario.onActivity(a -> a.findViewById(R.id.cardStory10).performClick()); waitEditor(scenario, true);
            scenario.onActivity(a -> {
                Object editor = field(a, "birthEditor");
                set(editor, "pickerVersion", field(editor, "version"));
                call(editor, "picked", Uri.class, null); assertEquals("KEEP", field(editor, "action"));
                call(editor, "picked", Uri.class, Uri.parse("file:///does-not-exist-return-image.png"));
            });
            waitEditor(scenario, false);
            scenario.onActivity(a -> {
                Object editor = field(a, "birthEditor");
                assertEquals("KEEP", field(editor, "action")); assertTrue(a.findViewById(R.id.btnSubmitProposal).isEnabled());
                set(editor, "pickerVersion", field(editor, "version"));
                call(editor, "select", int.class, 5);
                call(editor, "picked", Uri.class, Uri.parse("file:///stale-return-picture.png"));
                assertFalse((Boolean)field(editor, "processing"));
                assertEquals("Peter", ((EditText)a.findViewById(R.id.etProposedQuestion)).getText().toString());
            });
        }
    }
    @Test public void configuredAnswerPictureAndResultSurviveRecreation() throws Exception {
        Bitmap source = Bitmap.createBitmap(30, 30, Bitmap.Config.ARGB_8888); source.eraseColor(android.graphics.Color.RED);
        byte[] bytes = LawImageCodec.compress(source); source.recycle();
        try (ActivityScenario<QuizBirth> scenario = ActivityScenario.launch(QuizBirth.class)) {
            BirthQuizTest.waitReady(scenario);
            scenario.onActivity(a -> {
                List<BirthContentBank.Item> bank = BirthContentBank.defaults();
                bank.get(0).label = "Ark picture"; bank.get(0).answer = "ARK";
                bank.get(0).imageBytes = bytes; bank.get(0).imageDocument = "test-birth-revision";
                call(a, "persistContent", List.class, bank);
            });
            for (int i = 0; i < 100; i++) {
                boolean[] ready = {false};
                scenario.onActivity(a -> {
                    BirthWordGame game = (BirthWordGame)field(a, "game");
                    if (game != null && game.contentIndices != null) {
                        for (int position = 0; position < game.contentIndices.length; position++)
                            if (game.contentIndices[position] == 0) game.current = position;
                        ready[0] = "ARK".equals(game.round().answer);
                        if (ready[0]) {
                            try {
                                Method render = QuizBirth.class.getDeclaredMethod("render");
                                render.setAccessible(true); render.invoke(a);
                            }
                            catch (Exception e) { throw new AssertionError(e); }
                        }
                    }
                });
                if (ready[0]) break;
                SystemClock.sleep(100);
            }
            scenario.onActivity(a -> {
                BirthWordGame game = (BirthWordGame)field(a, "game"); assertEquals("ARK", game.round().answer);
                assertNotNull(((ImageView)a.findViewById(R.id.ivBirthPicture)).getDrawable());
                LinearLayout rows = a.findViewById(R.id.birthTiles);
                assertEquals(2, rows.getChildCount()); assertEquals(3, ((LinearLayout)a.findViewById(R.id.birthSlots)).getChildCount());
                ((LinearLayout)rows.getChildAt(0)).getChildAt(0).performClick();
            });
            String[] entered = {null};
            scenario.onActivity(a -> entered[0] = ((BirthWordGame)field(a, "game")).round().entered());
            scenario.recreate(); BirthQuizTest.waitReady(scenario);
            scenario.onActivity(a -> {
                BirthWordGame game = (BirthWordGame)field(a, "game"); assertEquals(entered[0], game.round().entered());
                List<BirthContentBank.Item> bank = (List<BirthContentBank.Item>)field(a, "content");
                assertArrayEquals(bytes, bank.get(0).imageBytes);
                try {
                    Method build = QuizBirth.class.getDeclaredMethod("buildResult"); build.setAccessible(true);
                    Map<String,Object> result = (Map<String,Object>)build.invoke(a);
                    BirthWordResult saved = (BirthWordResult)((List<?>)result.get("wordAnswers")).get(0);
                    assertEquals("Ark picture", saved.displayName); assertEquals("ARK", saved.expectedWord);
                    assertEquals("birth-approved:test-birth-revision:jesus", saved.imageKey);
                    assertEquals(0, result.get("xp"));
                } catch (Exception e) { throw new AssertionError(e); }
            });
        }
    }
}
