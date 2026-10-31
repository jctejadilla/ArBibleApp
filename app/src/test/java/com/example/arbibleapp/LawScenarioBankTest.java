package com.example.arbibleapp;

import com.google.firebase.Timestamp;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import static org.junit.Assert.*;

public class LawScenarioBankTest {
    private Map<String, Object> edit(int index, String description, int answer, long time) {
        Map<String, Object> data = new HashMap<>();
        data.put("storyTitle", LawScenarioBank.STORY_TITLE);
        data.put("approvedByAdmin", true);
        data.put("itemIndex", index);
        data.put("question", description);
        data.put("correctAnswerIndex", answer);
        data.put("timestamp", new Timestamp(time, 0));
        return data;
    }

    private LawScenarioBank.Edit reset(long time, String id) {
        Map<String, Object> data = edit(0, "", 0, time);
        data.put("operation", "RESTORE_ALL");
        return LawScenarioBank.parse(data, id);
    }

    private void assertDefaults(List<LawScenarioBank.Scenario> bank) {
        List<LawScenarioBank.Scenario> defaults = LawScenarioBank.defaults();
        assertEquals(15, bank.size());
        for (int i = 0; i < 15; i++) {
            assertEquals(i, bank.get(i).itemIndex);
            assertEquals(defaults.get(i).description, bank.get(i).description);
            assertEquals(defaults.get(i).isGood, bank.get(i).isGood);
            assertEquals(defaults.get(i).imageRes, bank.get(i).imageRes);
            assertFalse(bank.get(i).overridden);
        }
    }

    @Test
    public void defaultsPreserveAllFifteenOriginalScenarios() {
        List<LawScenarioBank.Scenario> bank = LawScenarioBank.defaults();
        String[] expected = {"Helping a friend who fell down", "Taking a toy without asking",
                "Listening to your parents", "Telling a lie to stay out of trouble",
                "Sharing your lunch with someone", "Cheating during a test",
                "Praying before eating", "Bullying a classmate", "Helping clean the classroom",
                "Stealing money from a bag", "Forgiving someone who hurt you",
                "Destroying school property", "Respecting your teacher",
                "Ignoring someone who needs help", "Being honest even when scared"};
        assertEquals(expected.length, bank.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], bank.get(i).description);
            assertEquals(i % 2 == 0, bank.get(i).isGood);
            assertEquals(i, bank.get(i).itemIndex);
            assertTrue(bank.get(i).imageRes != 0);
        }
        bank.get(0).description = "Changed";
        assertDefaults(LawScenarioBank.load(Collections.emptyList()));
    }

    @Test
    public void editChangesDescriptionAndClassificationButKeepsImage() {
        List<LawScenarioBank.Scenario> bank = LawScenarioBank.resolve(Arrays.asList(
                LawScenarioBank.parse(edit(0, "Hurting a friend", 1, 1), "a"),
                LawScenarioBank.parse(edit(1, "Returning a borrowed toy", 0, 1), "b")));
        assertEquals("Hurting a friend", bank.get(0).description);
        assertFalse(bank.get(0).isGood);
        assertTrue(bank.get(1).isGood);
        assertTrue(bank.get(0).overridden);
        assertEquals(LawScenarioBank.defaults().get(0).imageRes, bank.get(0).imageRes);
        assertFalse(bank.get(2).overridden);
    }

    @Test
    public void legacyMatchingRequiresAnActualDefaultDescription() {
        Map<String, Object> data = edit(0, "Updated", 1, 1);
        data.remove("itemIndex");
        data.put("originalQuestion", "Helping a friend who fell down [Approved Edit]");
        assertEquals(0, LawScenarioBank.parse(data, "a").index);
        data.put("originalQuestion", "Scenario 1: You find money on the ground that someone dropped.");
        assertNull(LawScenarioBank.parse(data, "a"));
        data.put("originalQuestion", "Already changed description [Approved Edit]");
        assertNull(LawScenarioBank.parse(data, "a"));
    }

    @Test
    public void newestValidEditWinsWithDeterministicTies() {
        LawScenarioBank.Edit older = LawScenarioBank.parse(edit(2, "Older", 1, 1), "z");
        LawScenarioBank.Edit newer = LawScenarioBank.parse(edit(2, "Newer", 0, 2), "a");
        LawScenarioBank.Edit tie = LawScenarioBank.parse(edit(2, "Tie winner", 1, 2), "b");
        List<LawScenarioBank.Edit> edits = Arrays.asList(older, newer, tie);
        for (int seed = 0; seed < 20; seed++) {
            Collections.shuffle(edits, new Random(seed));
            List<LawScenarioBank.Scenario> bank = LawScenarioBank.resolve(edits);
            assertEquals("Tie winner", bank.get(2).description);
            assertFalse(bank.get(2).isGood);
        }
    }

    @Test
    public void restoreResetsEveryScenarioRegardlessOfArrivalOrder() {
        List<LawScenarioBank.Edit> edits = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            edits.add(LawScenarioBank.parse(edit(i, "Changed " + i, i % 2 == 0 ? 1 : 0, 10), "edit" + i));
        }
        edits.add(reset(11, "restore"));
        for (int seed = 0; seed < 20; seed++) {
            Collections.shuffle(edits, new Random(seed));
            assertDefaults(LawScenarioBank.resolve(edits));
        }
    }

    @Test
    public void restorationWinsTimestampTiesAndSuppressesUndatedEdits() {
        Map<String, Object> missingTime = edit(1, "No timestamp", 0, 1);
        missingTime.remove("timestamp");
        assertDefaults(LawScenarioBank.resolve(Arrays.asList(reset(10, "a"),
                LawScenarioBank.parse(edit(0, "Same timestamp", 1, 10), "z"),
                LawScenarioBank.parse(missingTime, "zz"))));
    }

    @Test
    public void laterApprovalsWorkAndSecondRestoreResetsThemAgain() {
        List<LawScenarioBank.Edit> edits = new ArrayList<>(Arrays.asList(
                LawScenarioBank.parse(edit(0, "Old", 1, 1), "a"), reset(2, "reset"),
                LawScenarioBank.parse(edit(14, "New", 1, 3), "b")));
        List<LawScenarioBank.Scenario> bank = LawScenarioBank.resolve(edits);
        assertEquals(LawScenarioBank.defaults().get(0).description, bank.get(0).description);
        assertEquals("New", bank.get(14).description);
        assertFalse(bank.get(14).isGood);
        edits.add(reset(4, "reset2"));
        assertDefaults(LawScenarioBank.resolve(edits));
    }

    @Test
    public void invalidPendingAndRejectedRecordsHaveNoEffect() {
        assertNull(LawScenarioBank.parse(null, "a"));
        for (Object index : new Object[]{-1, 15, 0.5, Double.NaN, "0", null}) {
            Map<String, Object> data = edit(0, "Changed", 0, 1);
            data.put("itemIndex", index);
            assertNull(LawScenarioBank.parse(data, "a"));
        }
        for (Object classification : new Object[]{-1, 2, 0.5, Double.NaN, "0", null}) {
            Map<String, Object> data = edit(0, "Changed", 0, 1);
            data.put("correctAnswerIndex", classification);
            assertNull(LawScenarioBank.parse(data, "a"));
        }
        Map<String, Object> data = edit(0, " ", 0, 1);
        assertNull(LawScenarioBank.parse(data, "a"));
        data.put("operation", "RESTORE_ALL");
        for (String status : new String[]{"PENDING", "REJECTED"}) {
            data.put("status", status);
            data.remove("approvedByAdmin");
            assertNull(LawScenarioBank.parse(data, "a"));
        }
        data.put("approvedByAdmin", false);
        assertNull(LawScenarioBank.parse(data, "a"));
        data.put("approvedByAdmin", true);
        data.put("storyTitle", "Early Humanity and the Flood");
        assertNull(LawScenarioBank.parse(data, "a"));
        data.put("storyTitle", LawScenarioBank.STORY_TITLE);
        data.remove("timestamp");
        assertNull(LawScenarioBank.parse(data, "a"));
    }

    @Test
    public void selectionKeepsTenUniqueItemsAndTheirApprovedClassification() {
        List<LawScenarioBank.Scenario> bank = LawScenarioBank.resolve(Collections.singletonList(
                LawScenarioBank.parse(edit(0, "Approved scenario", 1, 1), "a")));
        Set<Integer> seenAcrossRuns = new HashSet<>();
        for (int seed = 0; seed < 100; seed++) {
            List<LawScenarioBank.Scenario> quiz = LawScenarioBank.selectQuiz(bank, new Random(seed));
            Set<Integer> indexes = new HashSet<>();
            assertEquals(10, quiz.size());
            for (LawScenarioBank.Scenario item : quiz) {
                assertTrue(indexes.add(item.itemIndex));
                seenAcrossRuns.add(item.itemIndex);
                if (item.itemIndex == 0) {
                    assertEquals("Approved scenario", item.description);
                    assertFalse(item.isGood);
                }
            }
            for (int i = 0; i < 15; i++) assertEquals(i, bank.get(i).itemIndex);
        }
        assertEquals(15, seenAcrossRuns.size());
    }
}