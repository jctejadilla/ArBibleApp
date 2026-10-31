package com.example.arbibleapp;

import com.google.firebase.firestore.DocumentSnapshot;
import java.io.Serializable;
import java.util.*;

final class MonarchyContentBank {
    static final int IMAGE_LIMIT = 120 * 1024;
    static final class Scene implements Serializable {
        private static final long serialVersionUID = 1L;
        String key, caption, imageDocument = "";
        byte[] bytes;
        Scene(String key, String caption) { this.key = key; this.caption = caption; }
        String imageKey() { return imageDocument.isEmpty() ? key : "monarchy-approved:" + imageDocument + ":" + key; }
    }
    static final class Scenario implements Serializable {
        private static final long serialVersionUID = 1L;
        int index;
        String title;
        List<Scene> scenes = new ArrayList<>();
        Scenario(int index, String title) { this.index = index; this.title = title; }
    }
    static List<Scenario> defaults() {
        List<Scenario> bank = new ArrayList<>();
        for (int r = 0; r < 3; r++) {
            Scenario scenario = new Scenario(r, MonarchyScenarioBank.TITLES[r]);
            for (int i = 0; i < 6; i++) scenario.scenes.add(new Scene(
                    MonarchyScenarioBank.KEYS[r][i], MonarchyScenarioBank.CAPTIONS[r][i]));
            bank.add(scenario);
        }
        return bank;
    }
    static Scene find(Scenario scenario, String key) {
        for (Scene scene : scenario.scenes) if (scene.key.equals(key)) return scene;
        return null;
    }
    static Scenario parse(Map<String,Object> data) {
        if (data == null || !MonarchyScenarioBank.STORY_TITLE.equals(data.get("storyTitle"))
                || !Boolean.TRUE.equals(data.get("approvedByAdmin"))) return null;
        Object type = data.get("quizType"), op = data.get("operation"), slot = data.get("itemIndex");
        if (type != null && (!(type instanceof Number) || ((Number)type).doubleValue() != 7)) return null;
        if (op != null && !"EDIT".equals(op)) return null;
        if (!(slot instanceof Number)) return null;
        double index = ((Number)slot).doubleValue();
        if (index < 0 || index >= 3 || index != Math.floor(index)) return null;
        Object title = data.get("question"), raw = data.get("scenes");
        if (!(title instanceof String) || ((String)title).trim().isEmpty()
                || ((String)title).trim().length() > 120 || !(raw instanceof List) || ((List<?>)raw).size() != 6) return null;
        Scenario scenario = new Scenario((int)index, ((String)title).trim());
        Set<String> allowed = new HashSet<>(Arrays.asList(MonarchyScenarioBank.KEYS[(int)index]));
        for (Object value : (List<?>)raw) {
            if (!(value instanceof Map)) return null;
            Map<?,?> row = (Map<?,?>)value;
            Object key = row.get("key"), caption = row.get("caption");
            if (!(key instanceof String) || !allowed.remove(key) || !(caption instanceof String)
                    || ((String)caption).trim().isEmpty() || ((String)caption).trim().length() > 300) return null;
            LawImageData image = LawImageData.read(row.get("imageAction"), row.get("imageBytes"));
            if (!image.valid || (image.bytes != null && image.bytes.length > IMAGE_LIMIT)) return null;
            Scene scene = new Scene((String)key, ((String)caption).trim());
            scene.bytes = image.bytes == null ? null : image.bytes.clone();
            scenario.scenes.add(scene);
        }
        return scenario;
    }
    static List<Scenario> load(List<DocumentSnapshot> documents) { return resolve(QuizResetPolicy.records(documents)); }
    static List<Scenario> resolve(List<QuizResetPolicy.Record> records) {
        List<Scenario> bank = defaults();
        for (QuizResetPolicy.Record record : QuizResetPolicy.activeEdits(records, 7)) {
            Scenario edit = parse(record.data);
            if (edit == null) continue;
            List<?> rows = (List<?>)record.data.get("scenes");
            for (int i = 0; i < 6; i++) {
                Scene scene = edit.scenes.get(i);
                Map<?,?> row = (Map<?,?>)rows.get(i);
                LawImageData image = LawImageData.read(row.get("imageAction"), row.get("imageBytes"));
                if ("KEEP".equals(image.action)) {
                    Scene old = find(bank.get(edit.index), scene.key);
                    scene.bytes = old.bytes; scene.imageDocument = old.imageDocument;
                } else if ("REPLACE".equals(image.action)) scene.imageDocument = record.id;
            }
            bank.set(edit.index, edit);
        }
        return bank;
    }
}
