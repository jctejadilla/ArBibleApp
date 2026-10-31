package com.example.arbibleapp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

final class BirthWordGame implements Serializable {
    private static final long serialVersionUID = 1L;
    static final int TOTAL = 10;
    final Round[] rounds = new Round[TOTAL];
    final int[] contentIndices = new int[TOTAL];
    int current;
    BirthWordGame(long seed) {
        this(seed, BirthWordBank.ANSWERS);
    }
    BirthWordGame(long seed, String[] answers) {
        if (answers == null || answers.length != TOTAL) throw new IllegalArgumentException("Ten answers required");
        Random random = new Random(seed);
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < TOTAL; i++) order.add(i);
        Collections.shuffle(order, random);
        for (int i = 0; i < TOTAL; i++) {
            int contentIndex = order.get(i);
            if (answers[contentIndex] == null || !answers[contentIndex].matches("[A-Z]{3,10}"))
                throw new IllegalArgumentException("Answers must contain 3-10 uppercase English letters");
            contentIndices[i] = contentIndex;
            rounds[i] = new Round(answers[contentIndex], random);
        }
    }
    Round round() { return rounds[current]; }
    boolean next() {
        if (!round().submitted || current == TOTAL - 1) return false;
        current++;
        return true;
    }
    boolean complete() {
        for (Round r : rounds) if (!r.submitted) return false;
        return true;
    }
    int score() {
        int score = 0;
        for (Round r : rounds) if (r.submitted && r.correct()) score++;
        return score;
    }
    int xp(boolean sunday) { return sunday ? score() * 20 : 0; }

    static final class Round implements Serializable {
        private static final long serialVersionUID = 1L;
        final String answer;
        final boolean[] hints;
        final int[] slots;
        final List<Character> tiles = new ArrayList<>();
        boolean submitted;
        Round(String answer, Random random) {
            this.answer = answer;
            hints = new boolean[answer.length()];
            slots = new int[answer.length()];
            Arrays.fill(slots, -1);
            List<Integer> positions = new ArrayList<>();
            for (int i = 0; i < answer.length(); i++) positions.add(i);
            Collections.shuffle(positions, random);
            int hintCount = answer.length() <= 4 ? 1 : 2;
            for (int i = 0; i < hintCount; i++) hints[positions.get(i)] = true;
            for (int i = 0; i < answer.length(); i++) if (!hints[i]) tiles.add(answer.charAt(i));
            while (tiles.size() < 10) tiles.add((char) ('A' + random.nextInt(26)));
            Collections.shuffle(tiles, random);
        }
        boolean used(int tile) {
            for (int slot : slots) if (slot == tile) return true;
            return false;
        }
        boolean select(int tile) {
            if (submitted || tile < 0 || tile >= tiles.size() || used(tile)) return false;
            for (int i = 0; i < slots.length; i++) if (!hints[i] && slots[i] < 0) {
                slots[i] = tile;
                return true;
            }
            return false;
        }
        boolean remove(int position) {
            if (submitted || position < 0 || position >= slots.length || hints[position] || slots[position] < 0) return false;
            slots[position] = -1;
            return true;
        }
        void clear() {
            if (!submitted) Arrays.fill(slots, -1);
        }
        boolean full() {
            for (int i = 0; i < slots.length; i++) if (!hints[i] && slots[i] < 0) return false;
            return true;
        }
        String entered() {
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < slots.length; i++)
                text.append(hints[i] ? answer.charAt(i) : slots[i] < 0 ? '_' : tiles.get(slots[i]));
            return text.toString();
        }
        boolean correct() { return answer.equals(entered()); }
        boolean submit() {
            if (submitted || !full()) return false;
            submitted = true;
            return true;
        }
    }
}
