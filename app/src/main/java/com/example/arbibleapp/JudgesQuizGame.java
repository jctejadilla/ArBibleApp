package com.example.arbibleapp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Pure Java game rules. A seed reconstructs the exact attempt after recreation. */
final class JudgesQuizGame {
    static final String STORY_TITLE = "Entering the Promised Land and the Judges";
    static final String[] KEYS = {"joshua", "samson", "gideon", "rahab",
            "trumpet", "torch", "wall", "river", "rope", "jars"};
    static final String[] DESCRIPTIONS = {
            "A leader in blue guiding the Israelites",
            "A strong man with long hair beside stone pillars",
            "A man in green carrying a torch and a horn",
            "A woman at a window holding a scarlet rope",
            "A curved horn with a hollow opening",
            "A wooden handle with a flame",
            "An ancient stone city wall",
            "Blue water flowing between grassy banks",
            "A coiled scarlet cord",
            "Two clay storage containers"
    };
    final List<Round> rounds = new ArrayList<>();
    final int[] selections = new int[KEYS.length];

    JudgesQuizGame(long seed) { this(seed, KEYS); }

    JudgesQuizGame(long seed, String[] words) {
        if (words.length != KEYS.length) throw new IllegalArgumentException("Expected ten words");
        Arrays.fill(selections, -1);
        Random random = new Random(seed);
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < KEYS.length; i++) order.add(i);
        Collections.shuffle(order, random);
        for (int answer : order) {
            List<Integer> choices = new ArrayList<>();
            int first = answer < 4 ? 0 : 4;
            int end = answer < 4 ? 4 : KEYS.length;
            for (int i = first; i < end; i++) if (i != answer) choices.add(i);
            Collections.shuffle(choices, random);
            choices = new ArrayList<>(choices.subList(0, 3));
            choices.add(answer);
            Collections.shuffle(choices, random);
            rounds.add(new Round(answer, scramble(words[answer], random), choices));
        }
    }

    static String scramble(String word, Random random) {
        char[] letters = word.toUpperCase(Locale.ROOT).toCharArray();
        List<Integer> pairs = new ArrayList<>();
        for (int i = 1; i < letters.length - 2; i++)
            if (letters[i] != letters[i + 1]) pairs.add(i);
        if (pairs.isEmpty()) throw new IllegalArgumentException("Word needs distinct interior letters");
        int index = pairs.get(random.nextInt(pairs.size()));
        char temp = letters[index];
        letters[index] = letters[index + 1];
        letters[index + 1] = temp;
        return new String(letters);
    }

    boolean select(int roundIndex, int choice) {
        if (roundIndex < 0 || roundIndex >= rounds.size() || selections[roundIndex] != -1
                || !rounds.get(roundIndex).choices.contains(choice)) return false;
        selections[roundIndex] = choice;
        return true;
    }

    int score() {
        int score = 0;
        for (int i = 0; i < rounds.size(); i++)
            if (selections[i] == rounds.get(i).answer) score++;
        return score;
    }

    boolean complete() {
        for (int selection : selections) if (selection == -1) return false;
        return true;
    }

    static String label(int index) {
        String key = KEYS[index];
        return key.substring(0, 1).toUpperCase(Locale.ROOT) + key.substring(1);
    }

    static final class Round {
        final int answer;
        final String scrambled;
        final List<Integer> choices;
        Round(int answer, String scrambled, List<Integer> choices) {
            this.answer = answer;
            this.scrambled = scrambled;
            this.choices = Collections.unmodifiableList(choices);
        }
    }
}
