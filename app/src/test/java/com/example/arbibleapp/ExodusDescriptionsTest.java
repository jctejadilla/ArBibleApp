package com.example.arbibleapp;

import com.google.firebase.Timestamp;
import org.junit.Test;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class ExodusDescriptionsTest {
    @Test
    public void allDefaultScenesHaveBriefDescriptionsWithinChoiceLimits() {
        for (int step = 0; step < 5; step++) {
            assertFalse(ExodusImageDefaults.description(step).trim().isEmpty());
            for (boolean correct : new boolean[]{true, false}) {
                String caption = ExodusImageDefaults.caption(step, correct);
                assertFalse(caption.trim().isEmpty());
                assertTrue(caption.length() <= 60);
                assertFalse(caption.toLowerCase().contains("correct"));
            }
        }
    }

    @Test
    public void existingApprovedDescriptionsArePreservedUntilReset() {
        Map<String, Object> data = new HashMap<>();
        data.put("storyTitle", QuizResetPolicy.story(4));
        data.put("quizType", 4);
        data.put("approvedByAdmin", true);
        data.put("timestamp", new Timestamp(1, 0));
        data.put("originalQuestion", "Step 1: Slavery in Egypt");
        data.put("question", "A teacher's existing scene description.");
        data.put("options", Arrays.asList("The family travels together.", "The people stay behind."));
        QuizResetPolicy.Record edit = new QuizResetPolicy.Record("edit", data);
        QuizCaptionOverride result = QuizCaptionOverride.selectRecords(Arrays.asList(edit), 4).get(0);
        assertEquals(data.get("question"), result.question);
        assertEquals("The family travels together.", result.optionA);
        assertEquals("The people stay behind.", result.optionB);

        Map<String, Object> reset = new HashMap<>(data);
        reset.put("operation", "RESTORE_ALL");
        reset.put("timestamp", new Timestamp(2, 0));
        assertTrue(QuizCaptionOverride.selectRecords(
                Arrays.asList(edit, new QuizResetPolicy.Record("reset", reset)), 4).isEmpty());
    }
}