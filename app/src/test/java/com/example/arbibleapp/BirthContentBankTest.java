package com.example.arbibleapp;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Blob;
import org.junit.Test;
import java.util.*;
import java.io.*;
import static org.junit.Assert.*;

public class BirthContentBankTest {
    private QuizResetPolicy.Record edit(int index, String label, long time) {
        Map<String,Object> data = new HashMap<>();
        data.put("storyTitle", BirthWordBank.STORY_TITLE); data.put("quizType", 10);
        data.put("operation", "EDIT"); data.put("approvedByAdmin", true); data.put("itemIndex", index);
        data.put("answerWord", BirthWordBank.ANSWERS[index]); data.put("question", label); data.put("timestamp", new Timestamp(time, 0));
        return new QuizResetPolicy.Record("edit-" + time, data);
    }
    @Test public void defaultsPreserveTenSubjectsAndAnswers() {
        List<BirthContentBank.Item> bank = BirthContentBank.defaults();
        BirthWordGame game = new BirthWordGame(19);
        assertEquals(10, bank.size()); assertEquals(10, game.rounds.length);
        for (int i = 0; i < 10; i++) {
            assertEquals(BirthWordBank.KEYS[i], bank.get(i).imageKey());
            assertEquals(BirthWordBank.NAMES[i], bank.get(i).label);
            assertEquals(BirthWordBank.ANSWERS[i], bank.get(i).answer);
        }
        bank.get(0).label = "Changed"; assertEquals("Jesus", BirthContentBank.defaults().get(0).label);
        assertEquals(10, QuizResetPolicy.type(BirthWordBank.STORY_TITLE));
    }
    @Test public void onlyTargetChangesAndNewestValidEditWins() {
        QuizResetPolicy.Record older = edit(2, "Older gate", 1), newer = edit(2, "New gate", 2);
        List<BirthContentBank.Item> bank = BirthContentBank.resolve(Arrays.asList(newer, older));
        assertEquals("New gate", bank.get(2).label);
        for (int i = 0; i < 10; i++) if (i != 2) assertEquals(BirthWordBank.NAMES[i], bank.get(i).label);
        assertTrue(QuizResetPolicy.validEdit(newer.data, newer.id, 10));
    }
    @Test public void duplicateAndMalformedLabelsOrImagesDoNotApply() {
        QuizResetPolicy.Record duplicate = edit(0, "Duplicate", 1);
        duplicate.data.put("answerWord", " mary ");
        assertFalse(BirthContentBank.unique(BirthContentBank.defaults(), BirthContentBank.parse(duplicate.data)));
        assertEquals("Jesus", BirthContentBank.resolve(Arrays.asList(duplicate)).get(0).label);
        for (String label : Arrays.asList("", " ", String.join("", Collections.nCopies(81, "a"))))
            assertNull(BirthContentBank.parse(edit(0, label, 1).data));
        for (String word : Arrays.asList("", "AB", "ABCDEFGHIJK", "JOHN DOE", "J0HN", "J?SUS")) {
            QuizResetPolicy.Record invalid = edit(0, "Valid name", 1); invalid.data.put("answerWord", word);
            assertNull(BirthContentBank.parse(invalid.data));
        }
        QuizResetPolicy.Record normalized = edit(0, "Valid name", 1); normalized.data.put("answerWord", "  prophet  ");
        assertEquals("PROPHET", BirthContentBank.parse(normalized.data).answer);
        QuizResetPolicy.Record bad = edit(0, "Valid label", 1);
        bad.data.put("imageAction", "REPLACE"); assertNull(BirthContentBank.parse(bad.data));
        bad.data.put("imageBytes", Blob.fromBytes(new byte[LawImageData.MAX_BYTES + 1]));
        assertNull(BirthContentBank.parse(bad.data));
        bad = edit(0, "Valid label", 1); bad.data.put("itemIndex", 0.5);
        assertNull(BirthContentBank.parse(bad.data));
        bad = edit(0, "Valid label", 1); bad.data.put("approvedByAdmin", false);
        assertEquals("Jesus", BirthContentBank.resolve(Arrays.asList(bad)).get(0).label);
    }
    @Test public void keepAndDefaultPreserveImageIdentityIndependentlyOfLabel() {
        QuizResetPolicy.Record upload = edit(0, "New temple", 1), keep = edit(0, "Updated temple", 2), original = edit(0, "Original picture", 3);
        upload.data.put("imageAction", "REPLACE"); upload.data.put("imageBytes", Blob.fromBytes(new byte[]{1,2}));
        BirthContentBank.Item snapshot = BirthContentBank.resolve(Arrays.asList(keep, upload)).get(0);
        assertArrayEquals(new byte[]{1,2}, snapshot.imageBytes);
        assertEquals("birth-approved:edit-1:jesus", snapshot.imageKey());
        original.data.put("imageAction", "DEFAULT");
        BirthContentBank.Item current = BirthContentBank.resolve(Arrays.asList(upload, keep, original)).get(0);
        assertEquals("Original picture", current.label); assertNull(current.imageBytes);
        assertEquals("jesus", current.imageKey());
        assertEquals("birth-approved:edit-1:jesus", snapshot.imageKey());
    }
    @Test public void resetIsScopedAndLaterApprovalApplies() {
        QuizResetPolicy.Record first = edit(0, "New temple", 1), reset = edit(0, "Unused", 2), later = edit(1, "New scroll", 3);
        reset.data.put("operation", "RESTORE_ALL");
        List<BirthContentBank.Item> bank = BirthContentBank.resolve(Arrays.asList(first, later, reset));
        assertEquals("Jesus", bank.get(0).label); assertEquals("New scroll", bank.get(1).label);
        reset.data.put("storyTitle", DivisionPairBank.STORY_TITLE); reset.data.put("quizType", 8);
        assertEquals("New temple", BirthContentBank.resolve(Arrays.asList(first, reset)).get(0).label);
    }
    @Test public void frozenContentCanBeRestoredWithoutChangingHistoricalImageReferences() throws Exception {
        QuizResetPolicy.Record upload = edit(0, "Historical temple", 1);
        upload.data.put("imageAction", "REPLACE"); upload.data.put("imageBytes", Blob.fromBytes(new byte[]{3,4}));
        List<BirthContentBank.Item> bank = BirthContentBank.resolve(Arrays.asList(upload));
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(buffer)) { out.writeObject(bank); }
        List<BirthContentBank.Item> restored;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()))) {
            restored = (List<BirthContentBank.Item>)in.readObject();
        }
        bank.get(0).label = "Later";
        assertEquals("Historical temple", restored.get(0).label);
        assertArrayEquals(new byte[]{3,4}, restored.get(0).imageBytes);
        assertEquals("birth-approved:edit-1:jesus", restored.get(0).imageKey());
    }
}
