package com.example.arbibleapp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

final class DivisionMatchingGame implements Serializable {
    private static final long serialVersionUID = 1L;
    static final int ROUNDS = 3, PAIRS = 3, TOTAL = 9;
    final Round[] rounds = new Round[ROUNDS];
    int current;
    int selected = -1;
    DivisionMatchingGame(long seed) {
        Random random = new Random(seed);
        for (int i = 0; i < ROUNDS; i++) rounds[i] = new Round(i, random);
    }
    Round round() { return rounds[current]; }
    boolean next() {
        if (!round().submitted || current == ROUNDS - 1) return false;
        current++;
        selected = -1;
        return true;
    }
    boolean complete() {
        for (Round round : rounds) if (!round.submitted) return false;
        return true;
    }
    int score() {
        int total = 0;
        for (Round round : rounds) total += round.score();
        return total;
    }
    int xp(boolean sunday) { return sunday ? score() * 10 : 0; }

    static final class Round implements Serializable {
        private static final long serialVersionUID = 1L;
        final List<Integer> characters = new ArrayList<>();
        final List<Integer> objects = new ArrayList<>();
        final int[] slots = {-1, -1, -1};
        boolean submitted;
        Round(int round, Random random) {
            for (int i = round * PAIRS; i < (round + 1) * PAIRS; i++) {
                characters.add(i);
                objects.add(i);
            }
            Collections.shuffle(characters, random);
            Collections.shuffle(objects, random);
        }
        int location(int object) {
            for (int i = 0; i < PAIRS; i++) if (slots[i] == object) return i;
            return -1;
        }
        boolean assign(int object, int target) {
            if (submitted || !objects.contains(object) || target < 0 || target >= PAIRS) return false;
            int source = location(object);
            if (source == target) return false;
            int displaced = slots[target];
            slots[target] = object;
            if (source >= 0) slots[source] = displaced;
            return true;
        }
        boolean remove(int object) {
            int source = location(object);
            if (submitted || source < 0) return false;
            slots[source] = -1;
            return true;
        }
        boolean reset() {
            if (submitted) return false;
            Arrays.fill(slots, -1);
            return true;
        }
        boolean full() {
            for (int object : slots) if (object < 0) return false;
            return true;
        }
        boolean submit() {
            if (submitted || !full()) return false;
            submitted = true;
            return true;
        }
        int score() {
            if (!submitted) return 0;
            int score = 0;
            for (int i = 0; i < PAIRS; i++) if (slots[i] == characters.get(i)) score++;
            return score;
        }
    }
}