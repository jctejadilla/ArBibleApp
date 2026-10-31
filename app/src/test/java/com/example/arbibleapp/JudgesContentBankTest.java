package com.example.arbibleapp;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Blob;
import org.junit.Test;
import java.util.*;
import java.io.*;
import static org.junit.Assert.*;

public class JudgesContentBankTest {
    private QuizResetPolicy.Record edit(int slot, String word, long time) {
        Map<String,Object> data = new HashMap<>();
        data.put("storyTitle", JudgesQuizGame.STORY_TITLE);
        data.put("quizType", 6); data.put("operation", "EDIT");
        data.put("approvedByAdmin", true); data.put("itemIndex", slot);
        data.put("question", word); data.put("description", "A new picture description");
        data.put("timestamp", new Timestamp(time, 0));
        return new QuizResetPolicy.Record("edit-" + time, data);
    }
    @Test public void defaultsRemainIndependentAndPlayable() {
        List<JudgesContentBank.Item> items = JudgesContentBank.defaults();
        assertEquals(10, items.size());
        for (int i = 0; i < 10; i++) {
            assertEquals(JudgesQuizGame.KEYS[i], items.get(i).imageKey());
            assertEquals(JudgesQuizGame.DESCRIPTIONS[i], items.get(i).description);
            assertTrue(JudgesContentBank.validWord(items.get(i).word));
        }
        items.get(0).word = "CHANGED";
        assertEquals("JOSHUA", JudgesContentBank.defaults().get(0).word);
        assertEquals(6, QuizResetPolicy.type(JudgesQuizGame.STORY_TITLE));
    }
    @Test public void invalidAndUnapprovedContentCannotReplaceDefaults() {
        for (String word : Arrays.asList("", "AAA", "AAAA", "king herod", "AB12", "ABCDEFGHIJKLM")) {
            QuizResetPolicy.Record record = edit(0, word, 1);
            assertNull(JudgesContentBank.parse(record.data));
        }
        QuizResetPolicy.Record record = edit(0, "MOSES", 1);
        record.data.put("approvedByAdmin", false);
        assertEquals("JOSHUA", JudgesContentBank.resolve(Arrays.asList(record)).get(0).word);
        record.data.put("approvedByAdmin", true); record.data.put("itemIndex", 0.5);
        assertNull(JudgesContentBank.parse(record.data));
        record.data.put("itemIndex", 0); record.data.put("quizType", 5);
        assertNull(JudgesContentBank.parse(record.data));
    }
    @Test public void newestEditWinsAndDuplicateWordIsIgnored() {
        QuizResetPolicy.Record old = edit(0, "MOSES", 1), latest = edit(0, "CALEB", 2);
        QuizResetPolicy.Record duplicate = edit(1, "caleb", 3);
        List<JudgesContentBank.Item> items = JudgesContentBank.resolve(Arrays.asList(duplicate, latest, old));
        assertEquals("CALEB", items.get(0).word);
        assertEquals("SAMSON", items.get(1).word);
        assertFalse(JudgesContentBank.unique(items, 1, "Caleb"));
        assertTrue(QuizResetPolicy.validEdit(latest.data, latest.id, 6));
    }
    @Test public void imageKeepDefaultAndRestorePreserveHistoricalReference() {
        QuizResetPolicy.Record upload = edit(0, "MOSES", 1);
        upload.data.put("imageAction", "REPLACE");
        upload.data.put("imageBytes", Blob.fromBytes(new byte[]{1,2,3}));
        QuizResetPolicy.Record keep = edit(0, "CALEB", 2);
        JudgesContentBank.Item snapshot = JudgesContentBank.resolve(Arrays.asList(keep, upload)).get(0);
        assertArrayEquals(new byte[]{1,2,3}, snapshot.imageBytes);
        assertEquals("judges-approved:edit-1:joshua", snapshot.imageKey());
        QuizResetPolicy.Record original = edit(0, "CALEB", 3);
        original.data.put("imageAction", "DEFAULT");
        JudgesContentBank.Item restoredImage = JudgesContentBank.resolve(Arrays.asList(upload, keep, original)).get(0);
        assertNull(restoredImage.imageBytes);
        assertEquals("CALEB", restoredImage.word);
        QuizResetPolicy.Record restore = edit(0, "UNUSED", 4);
        restore.data.put("operation", "RESTORE_ALL");
        assertEquals("JOSHUA", JudgesContentBank.resolve(Arrays.asList(upload, keep, restore)).get(0).word);
        assertEquals("CALEB", snapshot.word);
        assertEquals("judges-approved:edit-1:joshua", snapshot.imageKey());
        QuizResetPolicy.Record after = edit(0, "MOSES", 5);
        assertEquals("MOSES", JudgesContentBank.resolve(Arrays.asList(after, restore, keep)).get(0).word);
    }
    @Test public void invalidImageIsRejectedAndOtherStoryResetDoesNotApply() {
        QuizResetPolicy.Record bad = edit(0, "MOSES", 1);
        bad.data.put("imageAction", "REPLACE");
        assertNull(JudgesContentBank.parse(bad.data));
        QuizResetPolicy.Record good = edit(0, "MOSES", 2);
        QuizResetPolicy.Record reset = edit(0, "UNUSED", 3);
        reset.data.put("storyTitle", QuizResetPolicy.story(5));
        reset.data.put("quizType", 5); reset.data.put("operation", "RESTORE_ALL");
        assertEquals("MOSES", JudgesContentBank.resolve(Arrays.asList(good, reset)).get(0).word);
    }
    @Test public void frozenContentSurvivesSerializationAndSeedKeepsRoundOrder() throws Exception {
        List<JudgesContentBank.Item> items = JudgesContentBank.resolve(Arrays.asList(edit(0, "MOSES", 1)));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(new ArrayList<>(items)); }
        List<JudgesContentBank.Item> restored;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (List<JudgesContentBank.Item>) in.readObject();
        }
        String[] words = restored.stream().map(item -> item.word).toArray(String[]::new);
        JudgesQuizGame game = new JudgesQuizGame(17, words), again = new JudgesQuizGame(17, words);
        for (int i = 0; i < 10; i++) {
            JudgesQuizGame.Round round = game.rounds.get(i);
            assertEquals(round.scrambled, again.rounds.get(i).scrambled);
            assertEquals(round.choices, again.rounds.get(i).choices);
            assertNotEquals(words[round.answer], round.scrambled);
            for (int choice : round.choices) assertEquals(round.answer < 4, choice < 4);
        }
    }
}
