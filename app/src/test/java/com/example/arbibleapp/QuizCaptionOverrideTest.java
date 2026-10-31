package com.example.arbibleapp;

import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class QuizCaptionOverrideTest {
    private Map<String, Object> edit(String original) {
        Map<String, Object> data = new HashMap<>();
        data.put("approvedByAdmin", true);
        data.put("originalQuestion", original);
        data.put("question", "Updated scene");
        data.put("options", Arrays.asList("Tower block", "Odd block"));
        return data;
    }

    @Test
    public void stableIndexSurvivesChangedWording() {
        Map<String, Object> data = edit("Changed text [Approved Edit]");
        data.put("itemIndex", 4L);
        QuizCaptionOverride result = QuizCaptionOverride.parse(data, "a", 3);
        assertNotNull(result);
        assertEquals(4, result.itemIndex);
        assertEquals("Tower block", result.optionA);
        assertEquals("Odd block", result.optionB);
    }

    @Test
    public void legacyLabelsMapToTheirOriginalSlots() {
        assertEquals(0, QuizCaptionOverride.parse(
                edit("Floor 1 - Foundation Block [Approved Edit]"), "a", 3).itemIndex);
        assertEquals(4, QuizCaptionOverride.parse(
                edit("Step 5: Entering the Promised Land"), "a", 4).itemIndex);
        assertNull(QuizCaptionOverride.parse(edit("Floor 6 - Upper Structure Block"), "a", 3));
        assertNull(QuizCaptionOverride.parse(edit("Floor 10 - Tower Pinnacle Block"), "a", 3));
        assertNull(QuizCaptionOverride.parse(edit("Edited wording [Approved Edit]"), "a", 3));
        assertNull(QuizCaptionOverride.parse(edit("Step 1: Slavery in Egypt"), "a", 3));
    }

    @Test
    public void malformedEditsCannotReplaceValidContent() {
        Map<String, Object> data = edit("Floor 1 - Foundation Block");
        data.put("approvedByAdmin", false);
        assertNull(QuizCaptionOverride.parse(data, "a", 3));
        data.put("approvedByAdmin", true);
        for (Object index : Arrays.asList(-1, 5, 0.5, "0", Double.NaN)) {
            data.put("itemIndex", index);
            assertNull(QuizCaptionOverride.parse(data, "a", 3));
        }
        data.put("itemIndex", 0L);
        data.put("options", Arrays.asList("A", 3));
        assertNull(QuizCaptionOverride.parse(data, "a", 3));
        data.put("options", Arrays.asList("A", "B", "C"));
        assertNull(QuizCaptionOverride.parse(data, "a", 3));
        data.put("options", Arrays.asList("A", " "));
        assertNull(QuizCaptionOverride.parse(data, "a", 3));
        data.put("options", Arrays.asList("A", "B"));
        data.put("question", "");
        assertNull(QuizCaptionOverride.parse(data, "a", 3));
    }

    @Test
    public void captionLimitsDifferBetweenImageQuizzes() {
        Map<String, Object> data = edit("");
        data.put("itemIndex", 0L);
        data.put("options", Arrays.asList("A".repeat(25), "B"));
        assertNotNull(QuizCaptionOverride.parse(data, "a", 3));
        data.put("options", Arrays.asList("A".repeat(26), "B"));
        assertNull(QuizCaptionOverride.parse(data, "a", 3));
        data.put("options", Arrays.asList("A".repeat(60), "B"));
        assertNotNull(QuizCaptionOverride.parse(data, "a", 4));
        data.put("options", Arrays.asList("A".repeat(61), "B"));
        assertNull(QuizCaptionOverride.parse(data, "a", 4));
    }

    @Test
    public void latestTimestampWinsRegardlessOfDocumentId() {
        Map<String, Object> data = edit("Floor 1 - Foundation Block");
        data.put("timestamp", new Timestamp(10, 0));
        QuizCaptionOverride older = QuizCaptionOverride.parse(data, "z", 3);
        data.put("timestamp", new Timestamp(11, 0));
        QuizCaptionOverride newer = QuizCaptionOverride.parse(data, "a", 3);
        assertTrue(newer.isNewerThan(older));
        assertFalse(older.isNewerThan(newer));
        QuizCaptionOverride tie = QuizCaptionOverride.parse(data, "b", 3);
        assertTrue(tie.isNewerThan(newer));
        assertFalse(newer.isNewerThan(tie));
        data.remove("timestamp");
        QuizCaptionOverride missingTime = QuizCaptionOverride.parse(data, "zz", 3);
        assertFalse(missingTime.isNewerThan(newer));
        assertTrue(newer.isNewerThan(missingTime));
        assertTrue(missingTime.isNewerThan(null));
    }
}