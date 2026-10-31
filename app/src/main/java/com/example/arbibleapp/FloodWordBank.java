package com.example.arbibleapp;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class FloodWordBank {
    private static final String[] DEFAULT_WORDS = {
            "BEAR", "BIRD", "CAT", "COW", "DOG", "DOVE", "FROG", "LION", "PIG", "SHEEP"
    };

    static String[] defaults() {
        return DEFAULT_WORDS.clone();
    }

    static String normalize(String word) {
        if (word == null || !word.trim().matches("[A-Za-z]{1,8}")) return "";
        return word.trim().toUpperCase(Locale.ROOT);
    }

    static String[] load(List<DocumentSnapshot> documents) {
        return resolveRecords(QuizResetPolicy.records(documents));
    }

    static String[] resolveRecords(List<QuizResetPolicy.Record> records) {
        Map<Integer, Edit> edits = new HashMap<>();
        for (QuizResetPolicy.Record record : QuizResetPolicy.activeEdits(records, 2)) {
            apply(edits, record.data, record.id);
        }
        return resolve(edits);
    }

    static void apply(Map<Integer, Edit> edits, Map<String, Object> data, String id) {
        if (data == null || !Boolean.TRUE.equals(data.get("approvedByAdmin"))) return;
        int index = -1;
        if (data.containsKey("itemIndex")) {
            Object value = data.get("itemIndex");
            if (!(value instanceof Number)) return;
            double number = ((Number) value).doubleValue();
            if (number < 0 || number >= DEFAULT_WORDS.length || number != Math.floor(number)) return;
            index = ((Number) value).intValue();
        } else {
            Object original = data.get("originalQuestion");
            if (!(original instanceof String)) return;
            String word = normalize(((String) original).replace(" [Approved Edit]", ""));
            for (int i = 0; i < DEFAULT_WORDS.length; i++) {
                if (DEFAULT_WORDS[i].equals(word)) index = i;
            }
        }
        Object proposed = data.get("question");
        if (index < 0 || !(proposed instanceof String)) return;
        String word = normalize((String) proposed);
        if (word.isEmpty()) return;
        Object time = data.get("timestamp");
        Timestamp timestamp = time instanceof Timestamp ? (Timestamp) time : null;
        Edit edit = new Edit(word, timestamp, id);
        if (edit.isNewerThan(edits.get(index))) edits.put(index, edit);
    }

    static String[] resolve(Map<Integer, Edit> edits) {
        String[] words = defaults();
        for (Map.Entry<Integer, Edit> entry : edits.entrySet()) {
            words[entry.getKey()] = entry.getValue().word;
        }
        return words;
    }

    static boolean hasDuplicates(String[] words) {
        Set<String> seen = new HashSet<>();
        for (String word : words) {
            if (!seen.add(word)) return true;
        }
        return false;
    }

    static boolean duplicatesOtherSlot(String[] words, int index, String replacement) {
        for (int i = 0; i < words.length; i++) {
            if (i != index && words[i].equals(replacement)) return true;
        }
        return false;
    }

    static final class Edit {
        final String word, id;
        final Timestamp timestamp;

        Edit(String word, Timestamp timestamp, String id) {
            this.word = word;
            this.timestamp = timestamp;
            this.id = id;
        }

        boolean isNewerThan(Edit previous) {
            if (previous == null) return true;
            if (timestamp != null && previous.timestamp == null) return true;
            if (timestamp == null && previous.timestamp != null) return false;
            int comparison = timestamp == null ? 0 : timestamp.compareTo(previous.timestamp);
            return comparison > 0 || (comparison == 0 && id.compareTo(previous.id) > 0);
        }
    }
}