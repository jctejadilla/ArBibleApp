package com.example.arbibleapp;

import com.google.firebase.Timestamp;
import org.junit.Test;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class FloodWordBankTest {
    private Map<String, Object> proposal(String original, String word, long time) {
        Map<String, Object> data = new HashMap<>();
        data.put("approvedByAdmin", true);
        data.put("originalQuestion", original);
        data.put("question", word);
        data.put("timestamp", new Timestamp(time, 0));
        return data;
    }

    @Test
    public void emptyApprovalsKeepIndependentDefaults() {
        String[] words = FloodWordBank.load(Collections.emptyList());
        assertArrayEquals(FloodWordBank.defaults(), words);
        assertEquals(10, words.length);
        words[0] = "TIGER";
        assertEquals("BEAR", FloodWordBank.defaults()[0]);
    }

    @Test
    public void normalizationAcceptsOnlyOneToEightAsciiLetters() {
        assertEquals("TIGER", FloodWordBank.normalize(" tiger "));
        assertEquals("A", FloodWordBank.normalize("a"));
        assertEquals("ELEPHANT", FloodWordBank.normalize("elephant"));
        for (String invalid : new String[]{null, "", " ", "SEA LION", "DOG1",
                "DOG!", "ABCDEFGHI", "caf\u00e9", "stra\u00dfe"}) {
            assertEquals("", FloodWordBank.normalize(invalid));
        }
    }

    @Test
    public void stableIndexAndLegacyLabelReplaceOnlyTheirSlots() {
        Map<Integer, FloodWordBank.Edit> edits = new HashMap<>();
        Map<String, Object> indexed = proposal("Previously changed wording", "tiger", 10);
        indexed.put("itemIndex", 9L);
        FloodWordBank.apply(edits, indexed, "a");
        FloodWordBank.apply(edits, proposal(" bear [Approved Edit]", "zebra", 10), "b");
        String[] expected = FloodWordBank.defaults();
        expected[0] = "ZEBRA";
        expected[9] = "TIGER";
        assertArrayEquals(expected, FloodWordBank.resolve(edits));
    }

    @Test
    public void newestValidApprovalWinsInEitherArrivalOrder() {
        Map<String, Object> older = proposal("BEAR", "TIGER", 10);
        Map<String, Object> newer = proposal("BEAR", "ZEBRA", 11);
        for (boolean reversed : new boolean[]{false, true}) {
            Map<Integer, FloodWordBank.Edit> edits = new HashMap<>();
            FloodWordBank.apply(edits, reversed ? newer : older, reversed ? "a" : "z");
            FloodWordBank.apply(edits, reversed ? older : newer, reversed ? "z" : "a");
            FloodWordBank.apply(edits, proposal("BEAR", "INVALID123", 12), "zz");
            assertEquals("ZEBRA", FloodWordBank.resolve(edits)[0]);
        }
    }

    @Test
    public void tiesAndMissingTimestampsHaveDeterministicPrecedence() {
        for (boolean reversed : new boolean[]{false, true}) {
            Map<Integer, FloodWordBank.Edit> edits = new HashMap<>();
            Map<String, Object> a = proposal("BEAR", "TIGER", 10);
            Map<String, Object> b = proposal("BEAR", "ZEBRA", 10);
            FloodWordBank.apply(edits, reversed ? b : a, reversed ? "b" : "a");
            FloodWordBank.apply(edits, reversed ? a : b, reversed ? "a" : "b");
            Map<String, Object> missing = proposal("BEAR", "HORSE", 20);
            missing.remove("timestamp");
            FloodWordBank.apply(edits, missing, "zz");
            assertEquals("ZEBRA", FloodWordBank.resolve(edits)[0]);
            edits.clear();
            a.remove("timestamp");
            b.put("timestamp", "invalid");
            FloodWordBank.apply(edits, b, "b");
            FloodWordBank.apply(edits, a, "a");
            assertEquals("ZEBRA", FloodWordBank.resolve(edits)[0]);
            FloodWordBank.apply(edits, proposal("BEAR", "HORSE", 1), "0");
            assertEquals("HORSE", FloodWordBank.resolve(edits)[0]);
        }
    }

    @Test
    public void malformedOrUnapprovedEditsLeaveDefaultsIntact() {
        Map<Integer, FloodWordBank.Edit> edits = new HashMap<>();
        FloodWordBank.apply(edits, null, "a");
        for (Object index : new Object[]{-1, 10, 1.5, Double.NaN,
                Double.POSITIVE_INFINITY, "0", null}) {
            Map<String, Object> data = proposal("BEAR", "TIGER", 10);
            data.put("itemIndex", index);
            FloodWordBank.apply(edits, data, "a");
        }
        Map<String, Object> data = proposal("BEAR", "TIGER", 10);
        data.put("approvedByAdmin", false);
        FloodWordBank.apply(edits, data, "b");
        data.put("approvedByAdmin", "true");
        FloodWordBank.apply(edits, data, "b");
        data.put("approvedByAdmin", true);
        data.put("question", 123);
        FloodWordBank.apply(edits, data, "b");
        FloodWordBank.apply(edits, proposal("Unknown [Approved Edit]", "TIGER", 10), "c");
        assertArrayEquals(FloodWordBank.defaults(), FloodWordBank.resolve(edits));
    }

    @Test
    public void duplicateChecksAllowSameSlotButDetectConflictingApprovals() {
        String[] words = FloodWordBank.defaults();
        assertFalse(FloodWordBank.duplicatesOtherSlot(words, 0, "BEAR"));
        assertFalse(FloodWordBank.duplicatesOtherSlot(words, 0, "TIGER"));
        assertTrue(FloodWordBank.duplicatesOtherSlot(words, 0, "BIRD"));
        Map<Integer, FloodWordBank.Edit> edits = new HashMap<>();
        FloodWordBank.apply(edits, proposal("BEAR", "TIGER", 1), "a");
        FloodWordBank.apply(edits, proposal("BIRD", "tiger", 2), "b");
        assertTrue(FloodWordBank.hasDuplicates(FloodWordBank.resolve(edits)));
        FloodWordBank.apply(edits, proposal("BIRD", "ZEBRA", 3), "c");
        assertFalse(FloodWordBank.hasDuplicates(FloodWordBank.resolve(edits)));
    }
}