package com.example.arbibleapp;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class QuizBabelOverride {
    final int itemIndex;
    final int oddBlockIndex;
    final String documentId;
    final Timestamp timestamp;
    final LawImageData image1;
    final LawImageData image2;
    final LawImageData image3;

    private QuizBabelOverride(int itemIndex, int oddBlockIndex, String documentId, Timestamp timestamp, LawImageData image1, LawImageData image2, LawImageData image3) {
        this.itemIndex = itemIndex;
        this.oddBlockIndex = oddBlockIndex;
        this.documentId = documentId;
        this.timestamp = timestamp;
        this.image1 = image1;
        this.image2 = image2;
        this.image3 = image3;
    }

    static Map<Integer, QuizBabelOverride> select(List<DocumentSnapshot> documents) {
        return selectRecords(QuizResetPolicy.records(documents));
    }

    static Map<Integer, QuizBabelOverride> selectRecords(List<QuizResetPolicy.Record> records) {
        Map<Integer, QuizBabelOverride> result = new HashMap<>();
        for (QuizResetPolicy.Record record : QuizResetPolicy.activeEdits(records, 3)) {
            QuizBabelOverride edit = parse(record.data, record.id);
            if (edit != null) {
                QuizBabelOverride existing = result.get(edit.itemIndex);
                if (edit.isNewerThan(existing)) {
                    LawImageData i1 = edit.image1, i2 = edit.image2, i3 = edit.image3;
                    if (existing != null) {
                        if ("KEEP".equals(edit.image1.action)) i1 = existing.image1;
                        if ("KEEP".equals(edit.image2.action)) i2 = existing.image2;
                        if ("KEEP".equals(edit.image3.action)) i3 = existing.image3;
                    }
                    edit = new QuizBabelOverride(edit.itemIndex, edit.oddBlockIndex, edit.documentId, edit.timestamp, i1, i2, i3);
                    result.put(edit.itemIndex, edit);
                }
            }
        }
        return result;
    }

    static QuizBabelOverride parse(Map<String, Object> data, String id) {
        if (data == null || !Boolean.TRUE.equals(data.get("approvedByAdmin"))) return null;
        int index = -1;
        if (data.containsKey("itemIndex")) {
            Object value = data.get("itemIndex");
            if (!(value instanceof Number)) return null;
            double number = ((Number) value).doubleValue();
            if (number < 0 || number >= 5 || number != Math.floor(number)) return null;
            index = ((Number) value).intValue();
        } else {
            Object original = data.get("originalQuestion");
            if (!(original instanceof String)) return null;
            String label = ((String) original).replace(" [Approved Edit]", "").trim();
            Pattern pattern = Pattern.compile("^Floor ([1-5]) - .+$");
            Matcher matcher = pattern.matcher(label);
            if (matcher.matches()) index = Integer.parseInt(matcher.group(1)) - 1;
        }
        if (index < 0) return null;
        
        Object oddBlockObj = data.get("oddBlockIndex");
        if (!(oddBlockObj instanceof Number)) return null;
        int oddBlockIndex = ((Number) oddBlockObj).intValue();
        if (oddBlockIndex < 0 || oddBlockIndex > 2) return null;

        Object time = data.get("timestamp");
        LawImageData img1 = LawImageData.read(data.get("image1Action"), data.get("image1Bytes"));
        LawImageData img2 = LawImageData.read(data.get("image2Action"), data.get("image2Bytes"));
        LawImageData img3 = LawImageData.read(data.get("image3Action"), data.get("image3Bytes"));
        
        return new QuizBabelOverride(index, oddBlockIndex, id,
                time instanceof Timestamp ? (Timestamp) time : null, img1, img2, img3);
    }

    boolean isNewerThan(QuizBabelOverride previous) {
        if (previous == null) return true;
        if (timestamp != null && previous.timestamp == null) return true;
        if (timestamp == null && previous.timestamp != null) return false;
        int comparison = timestamp == null ? 0 : timestamp.compareTo(previous.timestamp);
        return comparison > 0 || (comparison == 0 && documentId.compareTo(previous.documentId) > 0);
    }
}
