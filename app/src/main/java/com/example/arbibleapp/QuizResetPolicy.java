package com.example.arbibleapp;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class QuizResetPolicy {
    private static final String[] STORIES = {"Creation and the Fall",
            "Early Humanity and the Flood", "The Tower of Babel and Abraham's Covenant",
            "Slavery in Egypt and the Exodus", "The Law and Wandering in the Desert", JudgesQuizGame.STORY_TITLE, MonarchyScenarioBank.STORY_TITLE, DivisionPairBank.STORY_TITLE, ReturnMatchingGame.STORY_TITLE, BirthWordBank.STORY_TITLE};
    private static final String[] NAMES = {"Creation", "Flood", "Babel", "Exodus", "Law", "Judges", "Monarchy", "Division", "Return", "Birth"};

    static String story(int type) { return STORIES[type - 1]; }
    static String name(int type) { return NAMES[type - 1]; }
    static int type(String story) {
        for (int i = 0; i < STORIES.length; i++) if (STORIES[i].equals(story)) return i + 1;
        return 0;
    }

    static List<Record> records(List<DocumentSnapshot> documents) {
        List<Record> records = new ArrayList<>();
        for (DocumentSnapshot doc : documents) records.add(new Record(doc.getId(), doc.getData()));
        return records;
    }

    static List<Record> activeEdits(List<Record> records, int type) {
        Timestamp resetTime = null;
        List<Record> matching = new ArrayList<>();
        for (Record record : records) {
            Map<String, Object> data = record.data;
            if (data == null || !story(type).equals(data.get("storyTitle"))
                    || !Boolean.TRUE.equals(data.get("approvedByAdmin"))) continue;
            if (data.containsKey("quizType")) {
                Object value = data.get("quizType");
                if (!(value instanceof Number) || ((Number) value).doubleValue() != type) continue;
            }
            Object operation = data.get("operation");
            Timestamp time = record.time();
            if ("RESTORE_ALL".equals(operation)) {
                if (time != null && (resetTime == null || time.compareTo(resetTime) > 0)) resetTime = time;
            } else if (operation == null || "EDIT".equals(operation)) {
                matching.add(record);
            }
        }
        final Timestamp latestReset = resetTime;
        matching.removeIf(record -> latestReset != null
                && (record.time() == null || record.time().compareTo(latestReset) <= 0));
        matching.sort((a, b) -> {
            Timestamp at = a.time(), bt = b.time();
            int time = at == null ? (bt == null ? 0 : -1) : bt == null ? 1 : at.compareTo(bt);
            return time != 0 ? time : a.id.compareTo(b.id);
        });
        return matching;
    }

    static boolean validEdit(Map<String, Object> content, String id, int type) {
        if (type == 1) return CreationQuestionBank.parse(content) != null;
        if (type == 2) {
            Map<Integer, FloodWordBank.Edit> edits = new HashMap<>();
            FloodWordBank.apply(edits, content, id);
            return !edits.isEmpty();
        }
        if (type == 3) return QuizBabelOverride.parse(content, id) != null;
        if (type == 4) return QuizCaptionOverride.parse(content, id, type) != null;
        if (type == 10) return BirthContentBank.parse(content) != null;
        if (type == 9) return ReturnContentBank.parse(content) != null;
        if (type == 8) return DivisionContentBank.parse(content) != null;
        if (type == 7) return MonarchyContentBank.parse(content) != null;
        if (type == 6) return JudgesContentBank.parse(content) != null;
        return type == 5 && LawScenarioBank.parse(content, id) != null;
    }

    static final class Record {
        final String id;
        final Map<String, Object> data;
        Record(String id, Map<String, Object> data) { this.id = id; this.data = data; }
        Timestamp time() {
            Object time = data == null ? null : data.get("timestamp");
            return time instanceof Timestamp ? (Timestamp) time : null;
        }
    }
}