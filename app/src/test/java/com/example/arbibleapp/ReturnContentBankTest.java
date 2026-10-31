package com.example.arbibleapp;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Blob;
import org.junit.Test;
import java.util.*;
import java.io.*;
import static org.junit.Assert.*;

public class ReturnContentBankTest {
    private QuizResetPolicy.Record edit(int index, String label, long time) {
        Map<String,Object> data = new HashMap<>();
        data.put("storyTitle", ReturnMatchingGame.STORY_TITLE); data.put("quizType", 9);
        data.put("operation", "EDIT"); data.put("approvedByAdmin", true); data.put("itemIndex", index);
        data.put("question", label); data.put("timestamp", new Timestamp(time, 0));
        return new QuizResetPolicy.Record("edit-" + time, data);
    }
    @Test public void defaultsPreserveSixSubjectsWithTwoCardsEach() {
        List<ReturnContentBank.Item> bank = ReturnContentBank.defaults();
        ReturnMatchingGame game = new ReturnMatchingGame(19);
        assertEquals(6, bank.size()); assertEquals(12, game.deck.length);
        for (int i = 0; i < 6; i++) {
            assertEquals(ReturnMatchingGame.KEYS[i], bank.get(i).imageKey());
            assertEquals(ReturnMatchingGame.LABELS[i], bank.get(i).label);
            int count = 0; for (int key : game.deck) if (key == i) count++;
            assertEquals(2, count);
        }
        bank.get(0).label = "Changed"; assertEquals("Rebuilt temple", ReturnContentBank.defaults().get(0).label);
        assertEquals(9, QuizResetPolicy.type(ReturnMatchingGame.STORY_TITLE));
    }
    @Test public void onlyTargetChangesAndNewestValidEditWins() {
        QuizResetPolicy.Record older = edit(2, "Older gate", 1), newer = edit(2, "New gate", 2);
        List<ReturnContentBank.Item> bank = ReturnContentBank.resolve(Arrays.asList(newer, older));
        assertEquals("New gate", bank.get(2).label);
        for (int i = 0; i < 6; i++) if (i != 2) assertEquals(ReturnMatchingGame.LABELS[i], bank.get(i).label);
        assertTrue(QuizResetPolicy.validEdit(newer.data, newer.id, 9));
    }
    @Test public void duplicateAndMalformedLabelsOrImagesDoNotApply() {
        QuizResetPolicy.Record duplicate = edit(0, "  EZRA ", 1);
        assertFalse(ReturnContentBank.unique(ReturnContentBank.defaults(), ReturnContentBank.parse(duplicate.data)));
        assertEquals("Rebuilt temple", ReturnContentBank.resolve(Arrays.asList(duplicate)).get(0).label);
        for (String label : Arrays.asList("", " ", String.join("", Collections.nCopies(81, "a"))))
            assertNull(ReturnContentBank.parse(edit(0, label, 1).data));
        QuizResetPolicy.Record bad = edit(0, "Valid label", 1);
        bad.data.put("imageAction", "REPLACE"); assertNull(ReturnContentBank.parse(bad.data));
        bad.data.put("imageBytes", Blob.fromBytes(new byte[LawImageData.MAX_BYTES + 1]));
        assertNull(ReturnContentBank.parse(bad.data));
        bad = edit(0, "Valid label", 1); bad.data.put("itemIndex", 0.5);
        assertNull(ReturnContentBank.parse(bad.data));
        bad = edit(0, "Valid label", 1); bad.data.put("approvedByAdmin", false);
        assertEquals("Rebuilt temple", ReturnContentBank.resolve(Arrays.asList(bad)).get(0).label);
    }
    @Test public void keepAndDefaultPreserveImageIdentityIndependentlyOfLabel() {
        QuizResetPolicy.Record upload = edit(0, "New temple", 1), keep = edit(0, "Updated temple", 2), original = edit(0, "Original picture", 3);
        upload.data.put("imageAction", "REPLACE"); upload.data.put("imageBytes", Blob.fromBytes(new byte[]{1,2}));
        ReturnContentBank.Item snapshot = ReturnContentBank.resolve(Arrays.asList(keep, upload)).get(0);
        assertArrayEquals(new byte[]{1,2}, snapshot.imageBytes);
        assertEquals("return-approved:edit-1:temple", snapshot.imageKey());
        original.data.put("imageAction", "DEFAULT");
        ReturnContentBank.Item current = ReturnContentBank.resolve(Arrays.asList(upload, keep, original)).get(0);
        assertEquals("Original picture", current.label); assertNull(current.imageBytes);
        assertEquals("temple", current.imageKey());
        assertEquals("return-approved:edit-1:temple", snapshot.imageKey());
    }
    @Test public void resetIsScopedAndLaterApprovalApplies() {
        QuizResetPolicy.Record first = edit(0, "New temple", 1), reset = edit(0, "Unused", 2), later = edit(1, "New scroll", 3);
        reset.data.put("operation", "RESTORE_ALL");
        List<ReturnContentBank.Item> bank = ReturnContentBank.resolve(Arrays.asList(first, later, reset));
        assertEquals("Rebuilt temple", bank.get(0).label); assertEquals("New scroll", bank.get(1).label);
        reset.data.put("storyTitle", DivisionPairBank.STORY_TITLE); reset.data.put("quizType", 8);
        assertEquals("New temple", ReturnContentBank.resolve(Arrays.asList(first, reset)).get(0).label);
    }
    @Test public void frozenContentCanBeRestoredWithoutChangingHistoricalImageReferences() throws Exception {
        QuizResetPolicy.Record upload = edit(0, "Historical temple", 1);
        upload.data.put("imageAction", "REPLACE"); upload.data.put("imageBytes", Blob.fromBytes(new byte[]{3,4}));
        List<ReturnContentBank.Item> bank = ReturnContentBank.resolve(Arrays.asList(upload));
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(buffer)) { out.writeObject(bank); }
        List<ReturnContentBank.Item> restored;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()))) {
            restored = (List<ReturnContentBank.Item>)in.readObject();
        }
        bank.get(0).label = "Later";
        assertEquals("Historical temple", restored.get(0).label);
        assertArrayEquals(new byte[]{3,4}, restored.get(0).imageBytes);
        assertEquals("return-approved:edit-1:temple", restored.get(0).imageKey());
    }
}
