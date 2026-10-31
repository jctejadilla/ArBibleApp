package com.example.arbibleapp;

import com.google.firebase.firestore.DocumentSnapshot;
import java.io.Serializable;
import java.util.*;

final class BirthContentBank {
    static final class Item implements Serializable {
        private static final long serialVersionUID = 1L;
        final int index;
        String label, answer, imageDocument = "";
        byte[] imageBytes;
        Item(int index, String label) { this.index = index; this.label = label; this.answer = BirthWordBank.ANSWERS[index]; }
        String imageKey() {
            return imageDocument.isEmpty() ? BirthWordBank.KEYS[index]
                    : "birth-approved:" + imageDocument + ":" + BirthWordBank.KEYS[index];
        }
    }
    static List<Item> defaults() {
        List<Item> bank = new ArrayList<>();
        for (int i = 0; i < BirthWordGame.TOTAL; i++) bank.add(new Item(i, BirthWordBank.NAMES[i]));
        return bank;
    }
    static boolean unique(List<Item> bank, Item candidate) {
        for (Item item : bank)
            if (item.index != candidate.index && item.answer.equalsIgnoreCase(candidate.answer)) return false;
        return true;
    }
    static Item parse(Map<String,Object> data) {
        if (data == null || !BirthWordBank.STORY_TITLE.equals(data.get("storyTitle"))
                || !Boolean.TRUE.equals(data.get("approvedByAdmin"))) return null;
        Object type = data.get("quizType"), op = data.get("operation"), slot = data.get("itemIndex");
        if (type != null && (!(type instanceof Number) || ((Number)type).doubleValue() != 10)) return null;
        if (op != null && !"EDIT".equals(op)) return null;
        if (!(slot instanceof Number) || !(data.get("question") instanceof String)) return null;
        double number = ((Number)slot).doubleValue();
        String label = ((String)data.get("question")).trim();
        if (number < 0 || number >= 10 || number != Math.floor(number) || label.isEmpty() || label.length() > 80) return null;
        if (!(data.get("answerWord") instanceof String)) return null;
        String answer = ((String)data.get("answerWord")).trim().toUpperCase(Locale.ROOT);
        if (!answer.matches("[A-Z]{3,10}")) return null;
        LawImageData image = LawImageData.read(data.get("imageAction"), data.get("imageBytes"));
        if (!image.valid) return null;
        Item item = new Item((int)number, label);
        item.answer = answer;
        item.imageBytes = image.bytes == null ? null : image.bytes.clone();
        return item;
    }
    static List<Item> load(List<DocumentSnapshot> documents) { return resolve(QuizResetPolicy.records(documents)); }
    static List<Item> resolve(List<QuizResetPolicy.Record> records) {
        List<Item> bank = defaults();
        for (QuizResetPolicy.Record record : QuizResetPolicy.activeEdits(records, 10)) {
            Item edit = parse(record.data);
            if (edit == null || !unique(bank, edit)) continue;
            LawImageData image = LawImageData.read(record.data.get("imageAction"), record.data.get("imageBytes"));
            if ("KEEP".equals(image.action)) {
                Item old = bank.get(edit.index);
                edit.imageBytes = old.imageBytes; edit.imageDocument = old.imageDocument;
            } else if ("REPLACE".equals(image.action)) edit.imageDocument = record.id;
            bank.set(edit.index, edit);
        }
        return bank;
    }
}
