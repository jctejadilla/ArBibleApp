package com.example.arbibleapp;

import com.google.firebase.Timestamp;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import static org.junit.Assert.*;

public class QuizResetPolicyTest {
    private QuizResetPolicy.Record edit(int type, long time, String id) {
        Map<String, Object> data = new HashMap<>();
        data.put("storyTitle", QuizResetPolicy.story(type));
        data.put("quizType", type);
        data.put("approvedByAdmin", true);
        data.put("operation", "EDIT");
        data.put("timestamp", new Timestamp(time, 0));
        data.put("itemIndex", 0);
        data.put("question", type == 2 ? "TIGER" : "Updated scenario");
        data.put("options", type == 1 ? Arrays.asList("A", "B", "C", "D") : Arrays.asList("A", "B"));
        data.put("correctAnswerIndex", 1);
        return new QuizResetPolicy.Record(id, data);
    }

    private QuizResetPolicy.Record reset(int type, long time, String id) {
        QuizResetPolicy.Record record = edit(type, time, id);
        record.data.put("operation", "RESTORE_ALL");
        record.data.remove("itemIndex");
        record.data.remove("question");
        record.data.remove("options");
        record.data.remove("correctAnswerIndex");
        return record;
    }

    private void assertDefault(int type, List<QuizResetPolicy.Record> records) {
        if (type == 1) {
            List<CreationQuestionBank.QuestionModel> bank = CreationQuestionBank.resolve(records);
            List<CreationQuestionBank.QuestionModel> defaults = CreationQuestionBank.defaults();
            assertEquals(10, bank.size());
            for (int i = 0; i < 10; i++) {
                assertEquals(defaults.get(i).question, bank.get(i).question);
                assertArrayEquals(defaults.get(i).options, bank.get(i).options);
                assertEquals(defaults.get(i).correctAnswerIndex, bank.get(i).correctAnswerIndex);
            }
        } else if (type == 2) {
            String[] words = FloodWordBank.resolveRecords(records);
            assertArrayEquals(FloodWordBank.defaults(), words);
            char[][] grid = FloodGridGenerator.generate(words, new Random(42));
            for (String word : words) assertTrue(FloodGridGenerator.contains(grid, word));
        } else if (type == 3 || type == 4) {
            assertTrue(QuizCaptionOverride.selectRecords(records, type).isEmpty());
        } else {
            List<LawScenarioBank.Scenario> bank = LawScenarioBank.resolveRecords(records);
            List<LawScenarioBank.Scenario> defaults = LawScenarioBank.defaults();
            assertEquals(15, bank.size());
            for (int i = 0; i < 15; i++) {
                assertEquals(defaults.get(i).description, bank.get(i).description);
                assertEquals(defaults.get(i).isGood, bank.get(i).isGood);
                assertEquals(defaults.get(i).imageRes, bank.get(i).imageRes);
            }
        }
    }

    private void assertEdited(int type, List<QuizResetPolicy.Record> records) {
        if (type == 1) {
            assertEquals("Updated scenario", CreationQuestionBank.resolve(records).get(0).question);
            assertEquals(1, CreationQuestionBank.resolve(records).get(0).correctAnswerIndex);
        } else if (type == 2) {
            assertEquals("TIGER", FloodWordBank.resolveRecords(records)[0]);
        } else if (type == 3 || type == 4) {
            QuizCaptionOverride item = QuizCaptionOverride.selectRecords(records, type).get(0);
            assertNotNull(item);
            assertEquals("Updated scenario", item.question);
            assertEquals("A", item.optionA);
        } else {
            assertEquals("Updated scenario", LawScenarioBank.resolveRecords(records).get(0).description);
            assertFalse(LawScenarioBank.resolveRecords(records).get(0).isGood);
        }
    }

    @Test
    public void everyQuizRestoresAllContentAndAcceptsLaterEdits() {
        for (int type = 1; type <= 5; type++) {
            List<QuizResetPolicy.Record> records = new ArrayList<>();
            records.add(edit(type, 1, "first"));
            assertEdited(type, records);
            records.add(reset(type, 2, "reset"));
            assertDefault(type, records);
            records.add(edit(type, 3, "later"));
            assertEdited(type, records);
            records.add(reset(type, 4, "reset2"));
            Collections.reverse(records);
            assertDefault(type, records);
        }
    }

    @Test
    public void resettingOneQuizDoesNotResetAnyOtherQuiz() {
        for (int target = 1; target <= 5; target++) {
            List<QuizResetPolicy.Record> records = new ArrayList<>();
            for (int type = 1; type <= 5; type++) records.add(edit(type, 1, "edit" + type));
            records.add(reset(target, 2, "reset"));
            for (int type = 1; type <= 5; type++) {
                if (type == target) assertDefault(type, records);
                else assertEdited(type, records);
            }
        }
    }

    @Test
    public void resetWinsTimestampTiesAndOlderUndatedEditsStayInactive() {
        for (int type = 1; type <= 5; type++) {
            QuizResetPolicy.Record undated = edit(type, 1, "zz");
            undated.data.remove("timestamp");
            List<QuizResetPolicy.Record> records = Arrays.asList(
                    edit(type, 10, "z"), reset(type, 10, "a"), undated);
            for (int seed = 0; seed < 10; seed++) {
                Collections.shuffle(records, new Random(seed));
                assertDefault(type, records);
            }
        }
    }

    @Test
    public void unapprovedAndMalformedResetRequestsHaveNoEffect() {
        for (int type = 1; type <= 5; type++) {
            QuizResetPolicy.Record pending = reset(type, 2, "pending");
            pending.data.remove("approvedByAdmin");
            pending.data.put("status", "PENDING");
            QuizResetPolicy.Record rejected = reset(type, 2, "rejected");
            rejected.data.put("approvedByAdmin", false);
            rejected.data.put("status", "REJECTED");
            QuizResetPolicy.Record undated = reset(type, 2, "undated");
            undated.data.remove("timestamp");
            QuizResetPolicy.Record mismatched = reset(type, 2, "mismatch");
            mismatched.data.put("quizType", type == 5 ? 1 : type + 1);
            assertEdited(type, Arrays.asList(edit(type, 1, "old"), pending, rejected, undated, mismatched));
        }
    }

    @Test
    public void repeatedResetRecordDoesNotChangeTheOutcome() {
        for (int type = 1; type <= 5; type++) {
            QuizResetPolicy.Record reset = reset(type, 2, "sameProposalId");
            assertDefault(type, Arrays.asList(edit(type, 1, "edit"), reset, reset));
        }
    }

    @Test
    public void legacyEditsWithoutOperationOrQuizTypeRemainSupported() {
        for (int type = 1; type <= 5; type++) {
            QuizResetPolicy.Record legacy = edit(type, 1, "legacy");
            legacy.data.remove("operation");
            legacy.data.remove("quizType");
            assertEdited(type, Collections.singletonList(legacy));
            assertDefault(type, Arrays.asList(legacy, reset(type, 2, "reset")));
        }
    }

    @Test
    public void editTiesUseDocumentIdAndInvalidOperationsAreIgnored() {
        for (int type = 1; type <= 5; type++) {
            QuizResetPolicy.Record a = edit(type, 1, "a");
            QuizResetPolicy.Record b = edit(type, 1, "b");
            QuizResetPolicy.Record unknown = edit(type, 2, "unknown");
            unknown.data.put("operation", "UNKNOWN");
            List<QuizResetPolicy.Record> active = QuizResetPolicy.activeEdits(Arrays.asList(b, unknown, a), type);
            assertEquals(2, active.size());
            assertEquals("a", active.get(0).id);
            assertEquals("b", active.get(1).id);
        }
    }

    @Test
    public void creationReplacesStableSlotsAndNeverAppendsQuestions() {
        QuizResetPolicy.Record stable = edit(1, 1, "stable");
        stable.data.put("originalQuestion", "Already edited [Approved Edit]");
        assertEdited(1, Collections.singletonList(stable));
        QuizResetPolicy.Record legacy = edit(1, 2, "legacy");
        legacy.data.remove("itemIndex");
        legacy.data.put("originalQuestion", CreationQuestionBank.defaults().get(4).question + " [Approved Edit]");
        List<CreationQuestionBank.QuestionModel> bank = CreationQuestionBank.resolve(Arrays.asList(stable, legacy));
        assertEquals(10, bank.size());
        assertEquals("Updated scenario", bank.get(4).question);
        legacy.data.put("originalQuestion", "Unmapped edited question");
        bank = CreationQuestionBank.resolve(Collections.singletonList(legacy));
        assertEquals(10, bank.size());
        assertDefault(1, Collections.singletonList(legacy));
    }

    @Test
    public void malformedCreationChoicesCannotReplaceDefaults() {
        for (Object answer : new Object[]{-1, 4, 0.5, Double.NaN, "1", null}) {
            QuizResetPolicy.Record record = edit(1, 1, "bad");
            record.data.put("correctAnswerIndex", answer);
            assertDefault(1, Collections.singletonList(record));
        }
        QuizResetPolicy.Record record = edit(1, 1, "bad");
        record.data.put("options", Arrays.asList("A", "B"));
        assertDefault(1, Collections.singletonList(record));
        record.data.put("options", Arrays.asList("A", "B", "C", ""));
        assertDefault(1, Collections.singletonList(record));
    }
}