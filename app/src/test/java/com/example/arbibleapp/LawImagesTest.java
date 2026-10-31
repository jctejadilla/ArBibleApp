package com.example.arbibleapp;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Blob;
import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

public class LawImagesTest {
    private QuizResetPolicy.Record edit(String id, int slot, long time, String action, byte[] bytes) {
        Map<String, Object> data = new HashMap<>();
        data.put("storyTitle", LawScenarioBank.STORY_TITLE);
        data.put("quizType", 5);
        data.put("approvedByAdmin", true);
        data.put("operation", "EDIT");
        data.put("timestamp", new Timestamp(time, 0));
        data.put("itemIndex", slot);
        data.put("question", "Edited scenario " + slot);
        data.put("correctAnswerIndex", 0);
        if (action != null) data.put("imageAction", action);
        if (bytes != null) data.put("imageBytes", Blob.fromBytes(bytes));
        return new QuizResetPolicy.Record(id, data);
    }

    @Test
    public void replacementFollowsItsScenarioAndKeepPreservesIt() {
        byte[] bytes = {1, 2, 3};
        QuizResetPolicy.Record image = edit("image", 4, 1, "REPLACE", bytes);
        QuizResetPolicy.Record wording = edit("wording", 4, 2, "KEEP", null);
        List<LawScenarioBank.Scenario> bank = LawScenarioBank.resolveRecords(Arrays.asList(wording, image));
        assertArrayEquals(bytes, bank.get(4).imageBytes);
        assertNull(bank.get(0).imageBytes);
        for (int seed = 0; seed < 100; seed++) {
            for (LawScenarioBank.Scenario scenario : LawScenarioBank.selectQuiz(bank, new java.util.Random(seed))) {
                if (scenario.itemIndex == 4) assertArrayEquals(bytes, scenario.imageBytes);
                else assertNull(scenario.imageBytes);
            }
        }
    }

    @Test
    public void legacyTextEditsKeepApprovedImage() {
        List<LawScenarioBank.Scenario> bank = LawScenarioBank.resolveRecords(Arrays.asList(
                edit("image", 0, 1, "REPLACE", new byte[]{5}),
                edit("legacy", 0, 2, null, null)));
        assertArrayEquals(new byte[]{5}, bank.get(0).imageBytes);
    }

    @Test
    public void defaultClearsOnlyTargetScenarioImage() {
        List<LawScenarioBank.Scenario> bank = LawScenarioBank.resolveRecords(Arrays.asList(
                edit("image0", 0, 1, "REPLACE", new byte[]{1}),
                edit("image1", 1, 1, "REPLACE", new byte[]{2}),
                edit("default", 0, 2, "DEFAULT", null)));
        assertNull(bank.get(0).imageBytes);
        assertEquals("Edited scenario 0", bank.get(0).description);
        assertArrayEquals(new byte[]{2}, bank.get(1).imageBytes);
    }

    @Test
    public void resetClearsImagesAndKeepsOldImagesSuppressed() {
        QuizResetPolicy.Record reset = edit("reset", 0, 2, null, null);
        reset.data.put("operation", "RESTORE_ALL");
        List<QuizResetPolicy.Record> records = Arrays.asList(
                edit("before", 0, 1, "REPLACE", new byte[]{1}), reset,
                edit("after", 0, 3, "KEEP", null),
                edit("new", 1, 3, "REPLACE", new byte[]{2}));
        List<LawScenarioBank.Scenario> bank = LawScenarioBank.resolveRecords(records);
        assertNull(bank.get(0).imageBytes);
        assertArrayEquals(new byte[]{2}, bank.get(1).imageBytes);
        assertEquals(LawScenarioBank.defaults().get(0).imageRes, bank.get(0).imageRes);
        assertNull(LawScenarioBank.defaults().get(1).imageBytes);
    }

    @Test
    public void unapprovedAndOtherQuizImagesDoNotAffectLaw() {
        QuizResetPolicy.Record pending = edit("pending", 0, 1, "REPLACE", new byte[]{1});
        pending.data.put("approvedByAdmin", false);
        QuizResetPolicy.Record other = edit("other", 0, 1, "REPLACE", new byte[]{2});
        other.data.put("storyTitle", QuizResetPolicy.story(1));
        other.data.put("quizType", 1);
        assertNull(LawScenarioBank.resolveRecords(Arrays.asList(pending, other)).get(0).imageBytes);
    }

    @Test
    public void imageFieldsRejectWrongTypesAndOversizedBlobs() {
        assertTrue(LawImageData.read(null, null).valid);
        assertTrue(LawImageData.read("KEEP", null).valid);
        assertTrue(LawImageData.read("DEFAULT", null).valid);
        assertTrue(LawImageData.read("REPLACE", Blob.fromBytes(new byte[LawImageData.MAX_BYTES])).valid);
        assertFalse(LawImageData.read("REPLACE", Blob.fromBytes(new byte[LawImageData.MAX_BYTES + 1])).valid);
        assertFalse(LawImageData.read("REPLACE", Blob.fromBytes(new byte[0])).valid);
        assertFalse(LawImageData.read("REPLACE", "not bytes").valid);
        assertFalse(LawImageData.read("KEEP", Blob.fromBytes(new byte[]{1})).valid);
        assertFalse(LawImageData.read("DEFAULT", Blob.fromBytes(new byte[]{1})).valid);
        assertFalse(LawImageData.read("UNKNOWN", null).valid);
    }

    @Test
    public void malformedImageFallsBackWithoutLosingApprovedText() {
        QuizResetPolicy.Record invalid = edit("bad", 0, 2, "REPLACE", null);
        LawScenarioBank.Scenario scenario = LawScenarioBank.resolveRecords(Arrays.asList(
                edit("old", 0, 1, "REPLACE", new byte[]{1}), invalid)).get(0);
        assertNull(scenario.imageBytes);
        assertEquals("Edited scenario 0", scenario.description);
        assertTrue(scenario.isGood);
    }
}