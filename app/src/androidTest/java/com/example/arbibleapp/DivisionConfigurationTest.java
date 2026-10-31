package com.example.arbibleapp;

import android.graphics.Bitmap;
import android.net.Uri;
import android.widget.*;
import android.view.View;
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
public class DivisionConfigurationTest {
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
                Object editor = field(a, "divisionEditor");
                done[0] = loading ? (Boolean)field(editor, "ready") : !(Boolean)field(editor, "processing");
            });
            if (done[0]) return;
            android.os.SystemClock.sleep(100);
        }
        fail("Pair editor did not become ready");
    }
    @Test public void teacherFormLoadsNinePairsAndIndependentPictureActions() {
        try (ActivityScenario<TeacherProposeQuestionActivity> scenario = ActivityScenario.launch(TeacherProposeQuestionActivity.class)) {
            scenario.onActivity(a -> a.findViewById(R.id.cardStory8).performClick()); waitEditor(scenario, true);
            scenario.onActivity(a -> {
                Object editor = field(a, "divisionEditor");
                assertEquals(9, ((Spinner)a.findViewById(R.id.spOriginalQuestion)).getCount());
                assertEquals("King Solomon", ((EditText)a.findViewById(R.id.etProposedQuestion)).getText().toString());
                assertEquals("Crown", ((EditText)field(editor, "objectName")).getText().toString());
                assertEquals(View.GONE, a.findViewById(R.id.spCorrectAnswer).getVisibility());
                ((Button[])field(editor, "originals"))[0].performClick();
                assertArrayEquals(new String[]{"DEFAULT","KEEP"}, (String[])field(editor, "actions"));
                ((Button[])field(editor, "keeps"))[0].performClick();
                ((Button[])field(editor, "originals"))[1].performClick();
                assertArrayEquals(new String[]{"KEEP","DEFAULT"}, (String[])field(editor, "actions"));
                call(editor, "select", int.class, 3);
                assertEquals("Esther", ((EditText)a.findViewById(R.id.etProposedQuestion)).getText().toString());
                assertEquals("Royal Crown", ((EditText)field(editor, "objectName")).getText().toString());
                assertArrayEquals(new String[]{"KEEP","KEEP"}, (String[])field(editor, "actions"));
                a.findViewById(R.id.btnBack).performClick(); a.findViewById(R.id.cardStory7).performClick();
                assertEquals(View.GONE, ((LinearLayout)field(editor, "container")).getVisibility());
                assertEquals(3, ((Spinner)a.findViewById(R.id.spOriginalQuestion)).getCount());
            });
        }
    }
    @Test public void cancelledFailedAndStaleImageCallbacksKeepCurrentPair() {
        try (ActivityScenario<TeacherProposeQuestionActivity> scenario = ActivityScenario.launch(TeacherProposeQuestionActivity.class)) {
            scenario.onActivity(a -> a.findViewById(R.id.cardStory8).performClick()); waitEditor(scenario, true);
            scenario.onActivity(a -> {
                Object editor = field(a, "divisionEditor");
                set(editor, "pickerVersion", field(editor, "version")); set(editor, "pickedRole", 0);
                call(editor, "picked", Uri.class, null);
                assertArrayEquals(new String[]{"KEEP","KEEP"}, (String[])field(editor, "actions"));
                call(editor, "picked", Uri.class, Uri.parse("file:///does-not-exist-division-image.png"));
            });
            waitEditor(scenario, false);
            scenario.onActivity(a -> {
                Object editor = field(a, "divisionEditor");
                assertArrayEquals(new String[]{"KEEP","KEEP"}, (String[])field(editor, "actions"));
                assertTrue(a.findViewById(R.id.btnSubmitProposal).isEnabled());
                set(editor, "pickerVersion", field(editor, "version"));
                call(editor, "select", int.class, 6);
                call(editor, "picked", Uri.class, Uri.parse("file:///stale-picture.png"));
                assertFalse((Boolean)field(editor, "processing"));
                assertEquals("Throne", ((EditText)field(editor, "objectName")).getText().toString());
            });
        }
    }
    @Test public void customNamesAndPicturesSurvivePlacementAndRecreation() throws Exception {
        Bitmap bitmap = Bitmap.createBitmap(30, 30, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(android.graphics.Color.RED);
        byte[] bytes = LawImageCodec.compress(bitmap); bitmap.recycle();
        try (ActivityScenario<QuizDivision> scenario = ActivityScenario.launch(QuizDivision.class)) {
            DivisionQuizTest.waitReady(scenario);
            scenario.onActivity(a -> {
                List<DivisionContentBank.Pair> bank = DivisionContentBank.defaults();
                bank.get(0).characterName = "Custom character"; bank.get(0).objectName = "Custom object";
                bank.get(0).images[0] = bytes; bank.get(0).images[1] = bytes;
                call(a, "persistContent", List.class, bank);
            });
            for (int i = 0; i < 100; i++) {
                boolean[] ready = {false};
                scenario.onActivity(a -> ready[0] = "Custom character".equals(((List<DivisionContentBank.Pair>)field(a, "content")).get(0).characterName));
                if (ready[0]) break;
                android.os.SystemClock.sleep(100);
            }
            scenario.onActivity(a -> {
                assertEquals("Custom character", ((List<DivisionContentBank.Pair>)field(a, "content")).get(0).characterName);
                DivisionMatchingGame game = (DivisionMatchingGame)field(a, "game");
                int trayPosition = game.round().objects.indexOf(0), row = game.round().characters.indexOf(0);
                LinearLayout tray = a.findViewById(R.id.divisionTray);
                View object = ((LinearLayout)tray.getChildAt(trayPosition)).getChildAt(0);
                assertTrue(object.getContentDescription().toString().contains("Custom object"));
                object.performClick();
                a.findViewById(R.id.divisionRows).findViewWithTag("slot_" + row).performClick();
                assertEquals(0, game.round().slots[row]);
                assertTrue(a.findViewById(R.id.divisionRows).findViewWithTag("slot_" + row).getContentDescription().toString().contains("Custom object"));
            });
            scenario.recreate(); DivisionQuizTest.waitReady(scenario);
            scenario.onActivity(a -> {
                List<DivisionContentBank.Pair> bank = (List<DivisionContentBank.Pair>)field(a, "content");
                assertEquals("Custom character", bank.get(0).characterName); assertArrayEquals(bytes, bank.get(0).images[1]);
                DivisionMatchingGame game = (DivisionMatchingGame)field(a, "game");
                assertEquals(0, game.round().slots[game.round().characters.indexOf(0)]);
            });
        }
    }
}
