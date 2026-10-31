package com.example.arbibleapp;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class LawScenarioBank {
    static final String STORY_TITLE = "The Law and Wandering in the Desert";

    static List<Scenario> defaults() {
        List<Scenario> scenarioBank = new ArrayList<>();
        scenarioBank.add(new Scenario("Helping a friend who fell down", R.drawable.law_helping_friend, true));
        scenarioBank.add(new Scenario("Taking a toy without asking", R.drawable.law_taking_toy, false));
        scenarioBank.add(new Scenario("Listening to your parents", R.drawable.law_listening_parents, true));
        scenarioBank.add(new Scenario("Telling a lie to stay out of trouble", R.drawable.law_telling_lie, false));
        scenarioBank.add(new Scenario("Sharing your lunch with someone", R.drawable.law_sharing_lunch, true));
        scenarioBank.add(new Scenario("Cheating during a test", R.drawable.law_cheating_test, false));
        scenarioBank.add(new Scenario("Praying before eating", R.drawable.law_praying_meal, true));
        scenarioBank.add(new Scenario("Bullying a classmate", R.drawable.law_bullying_classmate, false));
        scenarioBank.add(new Scenario("Helping clean the classroom", R.drawable.law_cleaning_classroom, true));
        scenarioBank.add(new Scenario("Stealing money from a bag", R.drawable.law_stealing_money, false));
        scenarioBank.add(new Scenario("Forgiving someone who hurt you", R.drawable.law_forgiving_friend, true));
        scenarioBank.add(new Scenario("Destroying school property", R.drawable.law_destroying_property, false));
        scenarioBank.add(new Scenario("Respecting your teacher", R.drawable.law_respecting_teacher, true));
        scenarioBank.add(new Scenario("Ignoring someone who needs help", R.drawable.law_ignoring_help, false));
        scenarioBank.add(new Scenario("Being honest even when scared",R.drawable.law_honest_scared,true));


        for (int i = 0; i < scenarioBank.size(); i++) scenarioBank.get(i).itemIndex = i;
        return scenarioBank;
    }

    static List<Scenario> load(List<DocumentSnapshot> documents) {
        return resolveRecords(QuizResetPolicy.records(documents));
    }

    static List<Scenario> resolveRecords(List<QuizResetPolicy.Record> records) {
        List<Edit> edits = new ArrayList<>();
        for (QuizResetPolicy.Record record : QuizResetPolicy.activeEdits(records, 5)) {
            Edit edit = parse(record.data, record.id);
            if (edit != null) edits.add(edit);
        }
        return resolve(edits);
    }

    static Edit parse(Map<String, Object> data, String id) {
        if (data == null || !STORY_TITLE.equals(data.get("storyTitle"))
                || !Boolean.TRUE.equals(data.get("approvedByAdmin"))) return null;
        Object operation = data.get("operation");
        boolean restore = "RESTORE_ALL".equals(operation);
        if (operation != null && !restore && !"EDIT".equals(operation)) return null;
        Object time = data.get("timestamp");
        Timestamp timestamp = time instanceof Timestamp ? (Timestamp) time : null;
        if (restore) return timestamp == null ? null : new Edit(-1, "", false, true, timestamp, id);
        List<Scenario> defaults = defaults();
        int index = -1;
        if (data.containsKey("itemIndex")) {
            Object value = data.get("itemIndex");
            if (!(value instanceof Number)) return null;
            double number = ((Number) value).doubleValue();
            if (number < 0 || number >= defaults.size() || number != Math.floor(number)) return null;
            index = ((Number) value).intValue();
        } else {
            Object original = data.get("originalQuestion");
            if (!(original instanceof String)) return null;
            String label = ((String) original).replace(" [Approved Edit]", "").trim();
            for (Scenario scenario : defaults) {
                if (scenario.description.equalsIgnoreCase(label)) index = scenario.itemIndex;
            }
        }
        Object question = data.get("question");
        Object answer = data.get("correctAnswerIndex");
        if (index < 0 || !(question instanceof String) || ((String) question).trim().isEmpty()
                || !(answer instanceof Number)) return null;
        double classification = ((Number) answer).doubleValue();
        if (classification != 0 && classification != 1) return null;
        Edit edit = new Edit(index, ((String) question).trim(), classification == 0,
                false, timestamp, id);
        edit.image = LawImageData.read(data.get("imageAction"), data.get("imageBytes"));
        return edit;
    }

    static List<Scenario> resolve(List<Edit> edits) {
        List<Edit> ordered = new ArrayList<>(edits);
        ordered.sort((a, b) -> {
            int time = a.timestamp == null ? (b.timestamp == null ? 0 : -1)
                    : b.timestamp == null ? 1 : a.timestamp.compareTo(b.timestamp);
            if (time != 0) return time;
            // A restore resets every edit with the same approval timestamp.
            if (a.restore != b.restore) return a.restore ? 1 : -1;
            return a.id.compareTo(b.id);
        });
        List<Scenario> bank = defaults();
        for (Edit edit : ordered) {
            if (edit.restore) {
                bank = defaults();
            } else {
                Scenario scenario = bank.get(edit.index);
                scenario.description = edit.description;
                scenario.isGood = edit.isGood;
                scenario.overridden = true;
                if (!edit.image.valid || "DEFAULT".equals(edit.image.action)) scenario.imageBytes = null;
                else if ("REPLACE".equals(edit.image.action)) scenario.imageBytes = edit.image.bytes.clone();
            }
        }
        return bank;
    }

    static List<Scenario> selectQuiz(List<Scenario> bank, Random random) {
        List<Scenario> shuffled = new ArrayList<>(bank);
        Collections.shuffle(shuffled, random);
        return new ArrayList<>(shuffled.subList(0, 10));
    }

    static final class Scenario {
        int itemIndex;
        String description;
        final int imageRes;
        byte[] imageBytes;
        boolean isGood, overridden;

        Scenario(String description, int imageRes, boolean isGood) {
            this.description = description;
            this.imageRes = imageRes;
            this.isGood = isGood;
        }
    }

    static final class Edit {
        final int index;
        final String description, id;
        final boolean isGood, restore;
        final Timestamp timestamp;
        LawImageData image = LawImageData.read(null, null);

        Edit(int index, String description, boolean isGood, boolean restore, Timestamp timestamp, String id) {
            this.index = index;
            this.description = description;
            this.isGood = isGood;
            this.restore = restore;
            this.timestamp = timestamp;
            this.id = id;
        }
    }
}