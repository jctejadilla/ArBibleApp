package com.example.arbibleapp;

import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.firebase.auth.FirebaseAuth;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

@RunWith(AndroidJUnit4.class)
public class JudgesQuizActivityTest {
    @org.junit.Before public void offline() throws Exception {
        assumeTrue(FirebaseAuth.getInstance().getCurrentUser() == null);
        com.google.android.gms.tasks.Tasks.await(com.google.firebase.firestore.FirebaseFirestore.getInstance().disableNetwork());
    }
    @org.junit.After public void online() {
        com.google.firebase.firestore.FirebaseFirestore.getInstance().enableNetwork();
    }
    private void waitReady(ActivityScenario<QuizJudges> scenario) {
        for (int attempt = 0; attempt < 100; attempt++) {
            final boolean[] ready = {false};
            scenario.onActivity(a -> ready[0] = ((LinearLayout) a.findViewById(R.id.judgesChoices)).getChildCount() == 2);
            if (ready[0]) return;
            android.os.SystemClock.sleep(100);
        }
        fail("Judges content did not load");
    }
    @Test public void selectionSurvivesRecreationAndCannotBeCountedTwice() {
        // Do not create live quiz sessions on a device signed into a real account.
        assumeTrue(FirebaseAuth.getInstance().getCurrentUser() == null);
        final String[] saved = new String[3];
        try (ActivityScenario<QuizJudges> scenario = ActivityScenario.launch(QuizJudges.class)) {
            waitReady(scenario);
            scenario.onActivity(activity -> {
                Button next = activity.findViewById(R.id.btnJudgesNext);
                assertFalse(next.isEnabled());
                LinearLayout grid = activity.findViewById(R.id.judgesChoices);
                LinearLayout row = (LinearLayout) grid.getChildAt(0);
                android.view.View first = row.getChildAt(0);
                first.performClick();
                saved[0] = ((TextView) activity.findViewById(R.id.tvJudgesScramble)).getText().toString();
                saved[1] = ((TextView) activity.findViewById(R.id.tvJudgesFeedback)).getText().toString();
                saved[2] = activity.findViewById(R.id.judgesProgress).getContentDescription().toString();
                first.performClick();
                assertEquals(saved[2], activity.findViewById(R.id.judgesProgress).getContentDescription().toString());
                assertTrue(next.isEnabled());
            });
            scenario.recreate();
            waitReady(scenario);
            scenario.onActivity(activity -> {
                assertEquals(saved[0], ((TextView) activity.findViewById(R.id.tvJudgesScramble)).getText().toString());
                assertEquals(saved[1], ((TextView) activity.findViewById(R.id.tvJudgesFeedback)).getText().toString());
                assertEquals(saved[2], activity.findViewById(R.id.judgesProgress).getContentDescription().toString());
                LinearLayout grid = activity.findViewById(R.id.judgesChoices);
                for (int r = 0; r < 2; r++) for (int c = 0; c < 2; c++)
                    assertFalse(((LinearLayout) grid.getChildAt(r)).getChildAt(c).isClickable());
                activity.findViewById(R.id.btnJudgesNext).performClick();
                assertFalse(activity.findViewById(R.id.btnJudgesNext).isEnabled());
                assertEquals("", ((TextView) activity.findViewById(R.id.tvJudgesFeedback)).getText().toString());
            });
        }
    }

    @Test public void allTenRoundsReachFinishAndKeepAnswersWhenSignedOut() {
        assumeTrue(FirebaseAuth.getInstance().getCurrentUser() == null);
        try (ActivityScenario<QuizJudges> scenario = ActivityScenario.launch(QuizJudges.class)) {
            waitReady(scenario);
            for (int round = 0; round < 10; round++) {
                final int index = round;
                scenario.onActivity(activity -> {
                    LinearLayout grid = activity.findViewById(R.id.judgesChoices);
                    ((LinearLayout) grid.getChildAt(0)).getChildAt(0).performClick();
                    Button next = activity.findViewById(R.id.btnJudgesNext);
                    assertTrue(next.isEnabled());
                    if (index == 9) assertEquals(activity.getString(R.string.judges_finish), next.getText().toString());
                    next.performClick();
                });
            }
            scenario.onActivity(activity -> {
                assertEquals(activity.getString(R.string.judges_sign_in),
                        ((TextView) activity.findViewById(R.id.tvJudgesFeedback)).getText().toString());
                assertTrue(activity.findViewById(R.id.btnJudgesNext).isEnabled());
            });
        }
    }
}
