package com.example.arbibleapp;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Blob;
import org.junit.Test;
import java.util.*;
import java.io.*;
import static org.junit.Assert.*;

public class DivisionContentBankTest {
    private QuizResetPolicy.Record edit(int index, long time) {
        Map<String,Object> data = new HashMap<>();
        data.put("storyTitle", DivisionPairBank.STORY_TITLE); data.put("quizType", 8);
        data.put("operation", "EDIT"); data.put("approvedByAdmin", true); data.put("itemIndex", index);
        data.put("question", "New character " + index); data.put("objectName", "New object " + index);
        data.put("timestamp", new Timestamp(time, 0));
        return new QuizResetPolicy.Record("edit-" + time, data);
    }
    @Test public void defaultsPreserveAllPairsAndFixedRoundGroups() {
        List<DivisionContentBank.Pair> bank = DivisionContentBank.defaults();
        assertEquals(9, bank.size());
        for (int i = 0; i < 9; i++) {
            assertEquals(i, bank.get(i).index);
            assertEquals(DivisionPairBank.CHARACTERS[i], bank.get(i).characterName);
            assertEquals(DivisionPairBank.OBJECTS[i], bank.get(i).objectName);
        }
        assertEquals(0, bank.get(0).index / 3);
        assertEquals(1, bank.get(3).index / 3);
        assertEquals(2, bank.get(6).index / 3);
        bank.get(0).characterName = "Changed";
        assertEquals("King Solomon", DivisionContentBank.defaults().get(0).characterName);
        assertEquals(8, QuizResetPolicy.type(DivisionPairBank.STORY_TITLE));
    }
    @Test public void onlyTargetPairChangesAndNewestApprovalWins() {
        QuizResetPolicy.Record old = edit(4, 1), newer = edit(4, 2);
        newer.data.put("question", "Latest character");
        List<DivisionContentBank.Pair> bank = DivisionContentBank.resolve(Arrays.asList(newer, old));
        assertEquals("Latest character", bank.get(4).characterName);
        for (int i = 0; i < 9; i++) if (i != 4) assertEquals(DivisionPairBank.OBJECTS[i], bank.get(i).objectName);
        assertTrue(QuizResetPolicy.validEdit(newer.data, newer.id, 8));
    }
    @Test public void duplicateNamesAreRejectedWithinRoundIgnoringCaseAndWhitespace() {
        QuizResetPolicy.Record record = edit(0, 1);
        record.data.put("question", "  jErObOaM ");
        DivisionContentBank.Pair pair = DivisionContentBank.parse(record.data);
        assertFalse(DivisionContentBank.unique(DivisionContentBank.defaults(), pair));
        assertEquals("King Solomon", DivisionContentBank.resolve(Arrays.asList(record)).get(0).characterName);
        record.data.put("question", "Unique"); record.data.put("objectName", " FIRE ");
        assertFalse(DivisionContentBank.unique(DivisionContentBank.defaults(), DivisionContentBank.parse(record.data)));
        record.data.put("objectName", "Lion");
        assertTrue(DivisionContentBank.unique(DivisionContentBank.defaults(), DivisionContentBank.parse(record.data)));
    }
    @Test public void malformedUnapprovedAndOversizeImagesAreRejected() {
        QuizResetPolicy.Record record = edit(0, 1); record.data.put("question", " ");
        assertNull(DivisionContentBank.parse(record.data));
        record = edit(0, 1); record.data.put("objectName", String.join("", Collections.nCopies(81, "a")));
        assertNull(DivisionContentBank.parse(record.data));
        record = edit(0, 1); record.data.put("itemIndex", 0.5);
        assertNull(DivisionContentBank.parse(record.data));
        record = edit(0, 1); record.data.put("image1Action", "REPLACE");
        assertNull(DivisionContentBank.parse(record.data));
        record.data.put("image1Bytes", Blob.fromBytes(new byte[LawImageData.MAX_BYTES + 1]));
        assertNull(DivisionContentBank.parse(record.data));
        record = edit(0, 1); record.data.put("approvedByAdmin", false);
        assertEquals("King Solomon", DivisionContentBank.resolve(Arrays.asList(record)).get(0).characterName);
    }
    @Test public void independentImageActionsKeepOriginalReference() {
        QuizResetPolicy.Record portrait = edit(0, 1), object = edit(0, 2), restore = edit(0, 3);
        portrait.data.put("image1Action", "REPLACE"); portrait.data.put("image1Bytes", Blob.fromBytes(new byte[]{1,2}));
        object.data.put("image2Action", "REPLACE"); object.data.put("image2Bytes", Blob.fromBytes(new byte[]{3,4}));
        DivisionContentBank.Pair snapshot = DivisionContentBank.resolve(Arrays.asList(object, portrait)).get(0);
        assertArrayEquals(new byte[]{1,2}, snapshot.images[0]); assertArrayEquals(new byte[]{3,4}, snapshot.images[1]);
        assertEquals("edit-1", snapshot.imageDocuments[0]); assertEquals("edit-2", snapshot.imageDocuments[1]);
        restore.data.put("image1Action", "DEFAULT");
        DivisionContentBank.Pair pair = DivisionContentBank.resolve(Arrays.asList(restore, object, portrait)).get(0);
        assertNull(pair.images[0]); assertEquals("", pair.imageDocuments[0]);
        assertArrayEquals(snapshot.images[1], pair.images[1]);
        assertNotNull(snapshot.images[0]);
    }
    @Test public void resetIsScopedAndLaterEditsStillApply() {
        QuizResetPolicy.Record first = edit(0, 1), reset = edit(0, 2), later = edit(1, 3);
        reset.data.put("operation", "RESTORE_ALL");
        List<DivisionContentBank.Pair> bank = DivisionContentBank.resolve(Arrays.asList(later, first, reset));
        assertEquals("King Solomon", bank.get(0).characterName); assertEquals("New character 1", bank.get(1).characterName);
        reset.data.put("storyTitle", MonarchyScenarioBank.STORY_TITLE); reset.data.put("quizType", 7);
        assertEquals("New character 0", DivisionContentBank.resolve(Arrays.asList(reset, first)).get(0).characterName);
    }
    @Test public void frozenContentAndResultReferencesSurviveSerializationAndLaterEdits() throws Exception {
        QuizResetPolicy.Record edit = edit(0, 1);
        edit.data.put("image1Action", "REPLACE"); edit.data.put("image1Bytes", Blob.fromBytes(new byte[]{1}));
        edit.data.put("image2Action", "REPLACE"); edit.data.put("image2Bytes", Blob.fromBytes(new byte[]{2}));
        List<DivisionContentBank.Pair> bank = DivisionContentBank.resolve(Arrays.asList(edit));
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(buffer)) { out.writeObject(bank); }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()))) {
            bank = (List<DivisionContentBank.Pair>)in.readObject();
        }
        DivisionMatchingGame game = new DivisionMatchingGame(18);
        for (int row = 0; row < 3; row++) game.round().assign(game.round().characters.get(row), row);
        game.round().submit();
        DivisionPairResult result = DivisionPairResult.from(game, 0, game.round().characters.indexOf(0), bank);
        assertTrue(result.correct); assertEquals("solomon", result.characterKey);
        assertEquals("crown", result.correctObjectKey);
        assertEquals("edit-1", result.characterImageReference);
        assertEquals("edit-1", result.correctObjectImageReference);
        bank.get(0).characterName = "Later";
        assertEquals("New character 0", result.characterName);
        assertEquals(30, game.xp(true)); assertEquals(0, game.xp(false));
    }
}
