package com.example.arbibleapp;

import com.google.firebase.firestore.DocumentSnapshot;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Stable slots preserve the character and object distractor groups. */
final class JudgesContentBank {
    static final class Item implements Serializable {
        private static final long serialVersionUID = 1L;
        final int index;
        String word, description, imageDocument = "";
        byte[] imageBytes;
        Item(int index, String word, String description) {
            this.index = index; this.word = word; this.description = description;
        }
        String imageKey() {
            return imageDocument.isEmpty() ? JudgesQuizGame.KEYS[index]
                    : "judges-approved:" + imageDocument + ":" + JudgesQuizGame.KEYS[index];
        }
    }
    static List<Item> defaults() {
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < JudgesQuizGame.KEYS.length; i++)
            items.add(new Item(i, JudgesQuizGame.KEYS[i].toUpperCase(Locale.ROOT), JudgesQuizGame.DESCRIPTIONS[i]));
        return items;
    }
    static boolean validWord(String word) {
        if (word == null || !word.matches("[A-Za-z]{4,12}")) return false;
        String upper = word.toUpperCase(Locale.ROOT);
        for (int i = 1; i < upper.length() - 2; i++)
            if (upper.charAt(i) != upper.charAt(i + 1)) return true;
        return false;
    }
    static boolean unique(List<Item> items, int index, String word) {
        for (Item item : items) if (item.index != index && item.word.equalsIgnoreCase(word)) return false;
        return true;
    }
    static Item parse(Map<String, Object> data) {
        if (data == null || !JudgesQuizGame.STORY_TITLE.equals(data.get("storyTitle"))
                || !Boolean.TRUE.equals(data.get("approvedByAdmin"))) return null;
        Object type = data.get("quizType"), operation = data.get("operation");
        if (type != null && (!(type instanceof Number) || ((Number) type).doubleValue() != 6)) return null;
        if (operation != null && !"EDIT".equals(operation)) return null;
        Object slot = data.get("itemIndex"), word = data.get("question"), description = data.get("description");
        if (!(slot instanceof Number) || !(word instanceof String) || !(description instanceof String)) return null;
        double number = ((Number) slot).doubleValue();
        if (number < 0 || number >= 10 || number != Math.floor(number)) return null;
        String text = ((String) word).trim(), desc = ((String) description).trim();
        if (!validWord(text) || desc.isEmpty() || desc.length() > 300
                || !LawImageData.read(data.get("imageAction"), data.get("imageBytes")).valid) return null;
        return new Item((int) number, text.toUpperCase(Locale.ROOT), desc);
    }
    static List<Item> load(List<DocumentSnapshot> documents) {
        return resolve(QuizResetPolicy.records(documents));
    }
    static List<Item> resolve(List<QuizResetPolicy.Record> records) {
        List<Item> items = defaults();
        for (QuizResetPolicy.Record record : QuizResetPolicy.activeEdits(records, 6)) {
            Item edit = parse(record.data);
            if (edit == null || !unique(items, edit.index, edit.word)) continue;
            Item old = items.get(edit.index);
            LawImageData image = LawImageData.read(record.data.get("imageAction"), record.data.get("imageBytes"));
            if ("KEEP".equals(image.action)) {
                edit.imageBytes = old.imageBytes; edit.imageDocument = old.imageDocument;
            } else if ("REPLACE".equals(image.action)) {
                edit.imageBytes = image.bytes.clone(); edit.imageDocument = record.id;
            }
            items.set(edit.index, edit);
        }
        return items;
    }
}
