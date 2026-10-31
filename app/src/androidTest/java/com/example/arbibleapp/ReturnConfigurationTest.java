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
public class ReturnConfigurationTest {
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
                Object editor = field(a, "returnEditor");
                done[0] = loading ? (Boolean)field(editor, "ready") : !(Boolean)field(editor, "processing");
            });
            if (done[0]) return;
            SystemClock.sleep(100);
        }
        fail("Return editor did not become ready");
    }
    @Test public void teacherHasSixSubjectsAndPictureActionsForBothCards() {
        try (ActivityScenario<TeacherProposeQuestionActivity> scenario = ActivityScenario.launch(TeacherProposeQuestionActivity.class)) {
            scenario.onActivity(a -> a.findViewById(R.id.cardStory9).performClick()); waitEditor(scenario, true);
            scenario.onActivity(a -> {
                Object editor = field(a, "returnEditor");
                assertEquals(6, ((Spinner)a.findViewById(R.id.spOriginalQuestion)).getCount());
                assertEquals("Rebuilt temple", ((EditText)a.findViewById(R.id.etProposedQuestion)).getText().toString());
                assertEquals(View.GONE, a.findViewById(R.id.spCorrectAnswer).getVisibility());
                ((Button)field(editor, "original")).performClick(); assertEquals("DEFAULT", field(editor, "action"));
                assertTrue(((TextView)field(editor, "status")).getText().toString().contains("both cards"));
                ((Button)field(editor, "keep")).performClick(); assertEquals("KEEP", field(editor, "action"));
                call(editor, "select", int.class, 4);
                assertEquals("Ezra", ((EditText)a.findViewById(R.id.etProposedQuestion)).getText().toString());
                a.findViewById(R.id.btnBack).performClick(); a.findViewById(R.id.cardStory8).performClick();
                assertEquals(View.GONE, ((LinearLayout)field(editor, "container")).getVisibility());
                assertEquals(9, ((Spinner)a.findViewById(R.id.spOriginalQuestion)).getCount());
            });
        }
    }
    @Test public void cancelledFailedAndStaleUploadsPreserveSelectedSubject() {
        try (ActivityScenario<TeacherProposeQuestionActivity> scenario = ActivityScenario.launch(TeacherProposeQuestionActivity.class)) {
            scenario.onActivity(a -> a.findViewById(R.id.cardStory9).performClick()); waitEditor(scenario, true);
            scenario.onActivity(a -> {
                Object editor = field(a, "returnEditor");
                set(editor, "pickerVersion", field(editor, "version"));
                call(editor, "picked", Uri.class, null); assertEquals("KEEP", field(editor, "action"));
                call(editor, "picked", Uri.class, Uri.parse("file:///does-not-exist-return-image.png"));
            });
            waitEditor(scenario, false);
            scenario.onActivity(a -> {
                Object editor = field(a, "returnEditor");
                assertEquals("KEEP", field(editor, "action")); assertTrue(a.findViewById(R.id.btnSubmitProposal).isEnabled());
                set(editor, "pickerVersion", field(editor, "version"));
                call(editor, "select", int.class, 5);
                call(editor, "picked", Uri.class, Uri.parse("file:///stale-return-picture.png"));
                assertFalse((Boolean)field(editor, "processing"));
                assertEquals("Nehemiah", ((EditText)a.findViewById(R.id.etProposedQuestion)).getText().toString());
            });
        }
    }
    private void waitForInteraction(ActivityScenario<QuizReturn> scenario) {
        for (int i = 0; i < 100; i++) {
            boolean[] ready = {false};
            scenario.onActivity(a -> ready[0] = ((Integer)field(a, "animations")) == 0
                    && ((ReturnMatchingGame)field(a, "game")).second == -1);
            if (ready[0]) return;
            SystemClock.sleep(100);
        }
        fail("Card animations or mismatch did not finish");
    }
    private View card(QuizReturn a, int index) { return ((View[])field(a, "cards"))[index]; }
    @Test public void bothCardsShareCustomPictureAndFlipsAndRecreationPreserveIt() throws Exception {
        Bitmap source = Bitmap.createBitmap(30, 30, Bitmap.Config.ARGB_8888); source.eraseColor(android.graphics.Color.RED);
        byte[] bytes = LawImageCodec.compress(source); source.recycle();
        int[] positions = new int[3];
        try (ActivityScenario<QuizReturn> scenario = ActivityScenario.launch(QuizReturn.class)) {
            ReturnQuizTest.waitReady(scenario);
            scenario.onActivity(a -> {
                List<ReturnContentBank.Item> bank = ReturnContentBank.defaults();
                bank.get(0).label = "Custom temple"; bank.get(0).imageBytes = bytes;
                bank.get(0).imageDocument = "test-historical-revision";
                call(a, "persistContent", List.class, bank);
            });
            for (int i = 0; i < 100; i++) {
                boolean[] ready = {false};
                scenario.onActivity(a -> ready[0] = "Custom temple".equals(((List<ReturnContentBank.Item>)field(a, "content")).get(0).label));
                if (ready[0]) break;
                SystemClock.sleep(100);
            }
            scenario.onActivity(a -> {
                assertEquals("Custom temple", ((List<ReturnContentBank.Item>)field(a, "content")).get(0).label);
                ReturnMatchingGame game = (ReturnMatchingGame)field(a, "game");
                int count = 0;
                for (int i = 0; i < 12; i++) {
                    if (game.deck[i] == 0) positions[count++] = i; else positions[2] = i;
                }
                assertEquals(2, count); card(a, positions[0]).performClick();
            });
            waitForInteraction(scenario);
            scenario.onActivity(a -> {
                assertTrue(card(a, positions[0]).getContentDescription().toString().contains("Custom temple"));
                card(a, positions[2]).performClick();
            });
            waitForInteraction(scenario);
            scenario.onActivity(a -> {
                assertEquals(-1, ((ReturnMatchingGame)field(a, "game")).second);
                ImageView[] pictures = (ImageView[])field(a, "pictures");
                assertEquals(View.INVISIBLE, pictures[positions[0]].getVisibility());
                assertNull(pictures[positions[0]].getDrawable());
                card(a, positions[0]).performClick();
            });
            waitForInteraction(scenario);
            scenario.onActivity(a -> card(a, positions[1]).performClick());
            waitForInteraction(scenario);
            scenario.onActivity(a -> {
                ImageView[] pictures = (ImageView[])field(a, "pictures");
                assertSame(((BitmapDrawable)pictures[positions[0]].getDrawable()).getBitmap(),
                        ((BitmapDrawable)pictures[positions[1]].getDrawable()).getBitmap());
                assertEquals(1, ((ReturnMatchingGame)field(a, "game")).score());
                try {
                    Method build = QuizReturn.class.getDeclaredMethod("buildResult"); build.setAccessible(true);
                    Map<String,Object> result = (Map<String,Object>)build.invoke(a);
                    assertEquals("Custom temple", ((List<?>)result.get("correctAnswers")).get(0));
                    assertEquals("return-approved:test-historical-revision:temple", ((List<?>)result.get("correctImageKeys")).get(0));
                } catch (Exception error) { throw new AssertionError(error); }
            });
            scenario.recreate(); ReturnQuizTest.waitReady(scenario);
            scenario.onActivity(a -> {
                assertEquals(1, ((ReturnMatchingGame)field(a, "game")).score());
                assertTrue(card(a, positions[0]).getContentDescription().toString().contains("Custom temple"));
                ImageView[] pictures = (ImageView[])field(a, "pictures");
                assertSame(((BitmapDrawable)pictures[positions[0]].getDrawable()).getBitmap(),
                        ((BitmapDrawable)pictures[positions[1]].getDrawable()).getBitmap());
                assertArrayEquals(bytes, ((List<ReturnContentBank.Item>)field(a, "content")).get(0).imageBytes);
            });
        }
    }
}
