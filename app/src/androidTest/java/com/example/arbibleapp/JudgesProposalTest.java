package com.example.arbibleapp;

import android.view.View;
import android.widget.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.tasks.Tasks;
import org.junit.*;
import org.junit.runner.RunWith;
import java.lang.reflect.*;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

@RunWith(AndroidJUnit4.class)
public class JudgesProposalTest {
    @Before public void offline() throws Exception {
        assumeTrue(FirebaseAuth.getInstance().getCurrentUser() == null);
        Tasks.await(FirebaseFirestore.getInstance().disableNetwork());
    }
    @After public void online() { FirebaseFirestore.getInstance().enableNetwork(); }
    private static Object field(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(target);
    }
    @Test public void sixthStoryShowsTenItemsAndImageControlsAndKeepsLawIndependent() {
        try (ActivityScenario<TeacherProposeQuestionActivity> scenario = ActivityScenario.launch(TeacherProposeQuestionActivity.class)) {
            scenario.onActivity(a -> {
                a.findViewById(R.id.cardStory6).performClick();
                Spinner items = a.findViewById(R.id.spOriginalQuestion);
                assertEquals(10, items.getCount());
                assertEquals(View.VISIBLE, a.findViewById(R.id.llLawControls).getVisibility());
                assertEquals(View.GONE, a.findViewById(R.id.spLawClassification).getVisibility());
                assertEquals(View.GONE, a.findViewById(R.id.spCorrectAnswer).getVisibility());
                try {
                    Field ready = a.getClass().getDeclaredField("lawReady"); ready.setAccessible(true); ready.set(a, true);
                    Method show = a.getClass().getDeclaredMethod("showJudgesItem", int.class); show.setAccessible(true); show.invoke(a, 0);
                    assertEquals("JOSHUA", ((EditText)a.findViewById(R.id.etProposedQuestion)).getText().toString());
                    assertEquals(JudgesQuizGame.DESCRIPTIONS[0], ((EditText)field(a, "judgesDescription")).getText().toString());
                    a.findViewById(R.id.btnDefaultLawImage).performClick();
                    assertEquals("DEFAULT", field(a, "lawImageAction"));
                    show.invoke(a, 1);
                    assertEquals("KEEP", field(a, "lawImageAction"));
                    assertEquals("SAMSON", ((EditText)a.findViewById(R.id.etProposedQuestion)).getText().toString());
                    a.findViewById(R.id.btnBack).performClick();
                    a.findViewById(R.id.cardStory5).performClick();
                    assertEquals(View.GONE, ((EditText)field(a, "judgesDescription")).getVisibility());
                    assertEquals(View.VISIBLE, a.findViewById(R.id.spLawClassification).getVisibility());
                    assertEquals(15, items.getCount());
                } catch (Exception e) { throw new AssertionError(e); }
            });
        }
    }
}
