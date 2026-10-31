package com.example.arbibleapp;

import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class CreationQuestionBank {
    static List<QuestionModel> defaults() {
        String[] questions = {
                "Who were the first man and woman created by God?",
                "Where did Adam and Eve live at the beginning?",
                "What was the name of the garden?",
                "Who created Adam and Eve?",
                "What did God tell Adam and Eve not to eat?",
                "Which animal tricked Eve?",
                "What did the serpent say would happen if Eve ate the fruit?",
                "Why did God create Eve?",
                "What did Adam and Eve use to cover themselves?",
                "What is one important lesson from the story?"
        };

        String[][] options = {
                {"Noah and his wife", "Abraham and Sarah", "Adam and Eve", "Moses and Miriam"},
                {"In a castle", "In a garden", "In a city", "On a mountain"},
                {"Garden of Peace", "Garden of Life", "Garden of Eden", "Garden of Hope"},
                {"An angel", "A king", "God", "A farmer"},
                {"Apples", "Grapes", "Bread", "Fruit from the Tree of Knowledge"},
                {"Lion", "Monkey", "Dove", "Serpent (snake)"},
                {"She would get sick", "She would fall asleep", "She would disappear", "She would be like God, knowing good and evil"},
                {"To work in the garden", "To name the animals", "To be a helper and companion for Adam", "To build a house"},
                {"Animal skin", "Grass", "Blankets", "Leaves"},
                {"Always hide", "Animals can talk", "We should obey God", "Gardens are dangerous"}
        };

        int[] correctAnswers = {2, 1, 2, 2, 3, 3, 3, 2, 3, 2};


        List<QuestionModel> bank = new ArrayList<>();
        for (int i = 0; i < questions.length; i++) {
            bank.add(new QuestionModel(i, questions[i], options[i], correctAnswers[i]));
        }
        return bank;
    }

    static List<QuestionModel> load(List<DocumentSnapshot> documents) {
        return resolve(QuizResetPolicy.records(documents));
    }

    static List<QuestionModel> resolve(List<QuizResetPolicy.Record> records) {
        List<QuestionModel> bank = defaults();
        for (QuizResetPolicy.Record record : QuizResetPolicy.activeEdits(records, 1)) {
            QuestionModel edit = parse(record.data);
            if (edit != null) bank.set(edit.itemIndex, edit);
        }
        return bank;
    }

    static QuestionModel parse(Map<String, Object> data) {
        if (data == null || !Boolean.TRUE.equals(data.get("approvedByAdmin"))) return null;
        List<QuestionModel> defaults = defaults();
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
            for (QuestionModel question : defaults) {
                if (question.question.equalsIgnoreCase(label)) index = question.itemIndex;
            }
        }
        Object text = data.get("question"), choices = data.get("options"), answer = data.get("correctAnswerIndex");
        if (index < 0 || !(text instanceof String) || ((String) text).trim().isEmpty()
                || !(choices instanceof List) || !(answer instanceof Number)) return null;
        List<?> options = (List<?>) choices;
        double correct = ((Number) answer).doubleValue();
        if (options.size() != 4 || correct < 0 || correct > 3 || correct != Math.floor(correct)) return null;
        String[] labels = new String[4];
        for (int i = 0; i < 4; i++) {
            if (!(options.get(i) instanceof String)) return null;
            labels[i] = ((String) options.get(i)).trim();
            if (labels[i].isEmpty() || labels[i].length() > 100) return null;
        }
        return new QuestionModel(index, ((String) text).trim(), labels, (int) correct);
    }

    static final class QuestionModel {
        final int itemIndex, correctAnswerIndex;
        final String question;
        final String[] options;
        QuestionModel(int itemIndex, String question, String[] options, int correctAnswerIndex) {
            this.itemIndex = itemIndex;
            this.question = question;
            this.options = options;
            this.correctAnswerIndex = correctAnswerIndex;
        }
    }
}