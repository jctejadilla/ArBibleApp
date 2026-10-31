package com.example.arbibleapp;

import com.google.firebase.firestore.DocumentSnapshot;
import java.io.Serializable;
import java.util.*;

final class DivisionContentBank {
    static final class Pair implements Serializable {
        private static final long serialVersionUID = 1L;
        final int index;
        String characterName, objectName;
        final byte[][] images = new byte[2][];
        final String[] imageDocuments = {"", ""};
        Pair(int index, String characterName, String objectName) {
            this.index = index; this.characterName = characterName; this.objectName = objectName;
        }
    }
    static List<Pair> defaults() {
        List<Pair> bank = new ArrayList<>();
        for (int i = 0; i < 9; i++) bank.add(new Pair(i, DivisionPairBank.CHARACTERS[i], DivisionPairBank.OBJECTS[i]));
        return bank;
    }
    static String name(Object value) { return value instanceof String ? ((String)value).trim() : ""; }
    static boolean unique(List<Pair> bank, Pair candidate) {
        for (Pair pair : bank) {
            if (pair.index == candidate.index || pair.index / 3 != candidate.index / 3) continue;
            if (pair.characterName.equalsIgnoreCase(candidate.characterName)
                    || pair.objectName.equalsIgnoreCase(candidate.objectName)) return false;
        }
        return true;
    }
    static Pair parse(Map<String,Object> data) {
        if (data == null || !DivisionPairBank.STORY_TITLE.equals(data.get("storyTitle"))
                || !Boolean.TRUE.equals(data.get("approvedByAdmin"))) return null;
        Object type = data.get("quizType"), operation = data.get("operation"), slot = data.get("itemIndex");
        if (type != null && (!(type instanceof Number) || ((Number)type).doubleValue() != 8)) return null;
        if (operation != null && !"EDIT".equals(operation)) return null;
        if (!(slot instanceof Number)) return null;
        double index = ((Number)slot).doubleValue();
        if (index < 0 || index >= 9 || index != Math.floor(index)) return null;
        String character = name(data.get("question")), object = name(data.get("objectName"));
        if (character.isEmpty() || object.isEmpty() || character.length() > 80 || object.length() > 80) return null;
        Pair pair = new Pair((int)index, character, object);
        for (int role = 0; role < 2; role++) {
            LawImageData image = LawImageData.read(data.get("image" + (role + 1) + "Action"),
                    data.get("image" + (role + 1) + "Bytes"));
            if (!image.valid) return null;
            pair.images[role] = image.bytes == null ? null : image.bytes.clone();
        }
        return pair;
    }
    static List<Pair> load(List<DocumentSnapshot> documents) { return resolve(QuizResetPolicy.records(documents)); }
    static List<Pair> resolve(List<QuizResetPolicy.Record> records) {
        List<Pair> bank = defaults();
        for (QuizResetPolicy.Record record : QuizResetPolicy.activeEdits(records, 8)) {
            Pair edit = parse(record.data);
            if (edit == null || !unique(bank, edit)) continue;
            Pair old = bank.get(edit.index);
            for (int role = 0; role < 2; role++) {
                LawImageData image = LawImageData.read(record.data.get("image" + (role + 1) + "Action"),
                        record.data.get("image" + (role + 1) + "Bytes"));
                if ("KEEP".equals(image.action)) {
                    edit.images[role] = old.images[role]; edit.imageDocuments[role] = old.imageDocuments[role];
                } else if ("REPLACE".equals(image.action)) edit.imageDocuments[role] = record.id;
            }
            bank.set(edit.index, edit);
        }
        return bank;
    }
}
