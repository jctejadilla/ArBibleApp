package com.example.arbibleapp;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Blob;
import org.junit.Test;
import java.util.*;
import java.io.*;
import static org.junit.Assert.*;

public class MonarchyContentBankTest {
    private QuizResetPolicy.Record edit(int index, long time) {
        Map<String,Object> data = new HashMap<>();
        data.put("storyTitle", MonarchyScenarioBank.STORY_TITLE); data.put("quizType", 7);
        data.put("approvedByAdmin", true); data.put("operation", "EDIT");
        data.put("itemIndex", index); data.put("question", "Custom " + time);
        data.put("timestamp", new Timestamp(time, 0));
        List<Map<String,Object>> scenes = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            Map<String,Object> row = new HashMap<>();
            row.put("key", MonarchyScenarioBank.KEYS[index][i]);
            row.put("caption", "Updated scene " + i); row.put("imageAction", "KEEP"); scenes.add(row);
        }
        data.put("scenes", scenes);
        return new QuizResetPolicy.Record("revision-" + time, data);
    }
    @SuppressWarnings("unchecked")
    private Map<String,Object> row(QuizResetPolicy.Record record, int i) {
        return ((List<Map<String,Object>>)record.data.get("scenes")).get(i);
    }
    @Test public void defaultsAndOtherKingsRemainUnchanged() {
        List<MonarchyContentBank.Scenario> defaults = MonarchyContentBank.defaults();
        assertEquals(3, defaults.size());
        for (int r = 0; r < 3; r++) for (int i = 0; i < 6; i++) {
            assertEquals(MonarchyScenarioBank.KEYS[r][i], defaults.get(r).scenes.get(i).key);
            assertEquals(MonarchyScenarioBank.CAPTIONS[r][i], defaults.get(r).scenes.get(i).caption);
        }
        List<MonarchyContentBank.Scenario> bank = MonarchyContentBank.resolve(Arrays.asList(edit(0, 1)));
        assertEquals("Custom 1", bank.get(0).title);
        assertEquals("saul_gilgal", bank.get(0).scenes.get(0).key);
        assertEquals(defaults.get(1).title, bank.get(1).title);
        assertEquals(defaults.get(2).scenes.get(0).caption, bank.get(2).scenes.get(0).caption);
        assertEquals(7, QuizResetPolicy.type(MonarchyScenarioBank.STORY_TITLE));
    }
    @Test public void malformedOrUnapprovedSequencesAreRejected() {
        QuizResetPolicy.Record record = edit(0, 1);
        row(record, 0).put("key", "david_sheep");
        assertNull(MonarchyContentBank.parse(record.data));
        record = edit(0, 1); row(record, 0).put("key", row(record, 1).get("key"));
        assertNull(MonarchyContentBank.parse(record.data));
        record = edit(0, 1); ((List<?>)record.data.get("scenes")).remove(0);
        assertNull(MonarchyContentBank.parse(record.data));
        record = edit(0, 1); record.data.put("itemIndex", 1.5);
        assertNull(MonarchyContentBank.parse(record.data));
        record = edit(0, 1); record.data.put("question", "");
        assertNull(MonarchyContentBank.parse(record.data));
        record = edit(0, 1); row(record, 0).put("caption", "");
        assertNull(MonarchyContentBank.parse(record.data));
        record = edit(0, 1); record.data.put("approvedByAdmin", false);
        assertEquals(MonarchyScenarioBank.TITLES[0], MonarchyContentBank.resolve(Arrays.asList(record)).get(0).title);
    }
    @Test public void picturesFollowStableSceneKeysAcrossReordersAndKeep() {
        QuizResetPolicy.Record upload = edit(0, 1);
        row(upload, 0).put("imageAction", "REPLACE");
        row(upload, 0).put("imageBytes", Blob.fromBytes(new byte[]{1,2,3}));
        QuizResetPolicy.Record keep = edit(0, 2);
        Collections.reverse((List<?>)keep.data.get("scenes"));
        List<MonarchyContentBank.Scenario> bank = MonarchyContentBank.resolve(Arrays.asList(keep, upload));
        MonarchyContentBank.Scene scene = bank.get(0).scenes.get(5);
        assertEquals("saul_gilgal", scene.key);
        assertArrayEquals(new byte[]{1,2,3}, scene.bytes);
        assertEquals("monarchy-approved:revision-1:saul_gilgal", scene.imageKey());
        QuizResetPolicy.Record original = edit(0, 3);
        row(original, 0).put("imageAction", "DEFAULT");
        assertNull(MonarchyContentBank.resolve(Arrays.asList(upload, keep, original)).get(0).scenes.get(0).bytes);
        assertNotNull(scene.bytes);
    }
    @Test public void sixImageLimitAndBadActionsAreValidated() {
        QuizResetPolicy.Record record = edit(0, 1);
        row(record, 0).put("imageAction", "REPLACE");
        row(record, 0).put("imageBytes", Blob.fromBytes(new byte[MonarchyContentBank.IMAGE_LIMIT + 1]));
        assertFalse(QuizResetPolicy.validEdit(record.data, record.id, 7));
        for (int i = 0; i < 6; i++) {
            row(record, i).put("imageAction", "REPLACE");
            row(record, i).put("imageBytes", Blob.fromBytes(new byte[MonarchyContentBank.IMAGE_LIMIT]));
        }
        assertTrue(QuizResetPolicy.validEdit(record.data, record.id, 7));
        row(record, 0).put("imageAction", "KEEP");
        assertNull(MonarchyContentBank.parse(record.data));
    }
    @Test public void restoreCutsOffEditsAndLaterApprovalApplies() {
        QuizResetPolicy.Record first = edit(0, 1), reset = edit(0, 2), later = edit(1, 3);
        reset.data.put("operation", "RESTORE_ALL");
        List<MonarchyContentBank.Scenario> bank = MonarchyContentBank.resolve(Arrays.asList(later, first, reset));
        assertEquals(MonarchyScenarioBank.TITLES[0], bank.get(0).title);
        assertEquals("Custom 3", bank.get(1).title);
        reset.data.put("storyTitle", JudgesQuizGame.STORY_TITLE); reset.data.put("quizType", 6);
        assertEquals("Custom 1", MonarchyContentBank.resolve(Arrays.asList(first, reset)).get(0).title);
    }
    @Test public void customSequenceScoresAndHistoricalResultSurvivesLaterEdits() throws Exception {
        List<MonarchyContentBank.Scenario> bank = MonarchyContentBank.resolve(Arrays.asList(edit(0, 1)));
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(buffer)) { out.writeObject(bank); }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()))) {
            bank = (List<MonarchyContentBank.Scenario>)in.readObject();
        }
        MonarchySequenceGame game = new MonarchySequenceGame(12);
        for (int i : new int[]{1,0,2,3,4,5}) game.round().select(i);
        game.round().submit();
        assertEquals(4, game.round().score());
        assertEquals(40, game.xp(true)); assertEquals(0, game.xp(false));
        MonarchySequenceResult result = MonarchySequenceResult.from(game, 0, bank);
        assertEquals("saul_gilgal", result.correctKeys.get(0));
        assertEquals("saul_jabesh", result.selectedKeys.get(0));
        bank.get(0).title = "Later title"; bank.get(0).scenes.get(0).caption = "Later caption";
        assertEquals("Custom 1", result.title); assertEquals("Updated scene 5", result.correctCaptions.get(0));
    }
}
