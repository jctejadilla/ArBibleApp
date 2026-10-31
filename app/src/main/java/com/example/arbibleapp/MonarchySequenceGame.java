package com.example.arbibleapp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

final class MonarchySequenceGame implements Serializable {
    private static final long serialVersionUID = 1L;
    static final int SCENARIOS = 3, SCENES = 6, TOTAL = SCENARIOS * SCENES;
    final Round[] rounds = new Round[SCENARIOS];
    int current;

    MonarchySequenceGame(long seed) {
        Random random = new Random(seed);
        for (int i = 0; i < rounds.length; i++) rounds[i] = new Round(random);
    }
    Round round() { return rounds[current]; }
    boolean next() {
        if (!round().submitted || current >= SCENARIOS - 1) return false;
        current++;
        return true;
    }
    boolean complete() {
        for (Round round : rounds) if (!round.submitted) return false;
        return true;
    }
    int score() {
        int score = 0;
        for (Round round : rounds) if (round.submitted) score += round.score();
        return score;
    }
    int xp(boolean sunday) { return sunday ? score() * 10 : 0; }

    static final class Round implements Serializable {
        private static final long serialVersionUID = 1L;
        final List<Integer> grid = new ArrayList<>();
        final List<Integer> selected = new ArrayList<>();
        boolean submitted;
        Round(Random random) {
            for (int i = 0; i < SCENES; i++) grid.add(i);
            Collections.shuffle(grid, random);
        }
        boolean select(int scene) {
            if (submitted || scene < 0 || scene >= SCENES || selected.contains(scene)) return false;
            selected.add(scene);
            return true;
        }
        boolean undo() {
            if (submitted || selected.isEmpty()) return false;
            selected.remove(selected.size() - 1);
            return true;
        }
        boolean reset() {
            if (submitted || selected.isEmpty()) return false;
            selected.clear();
            return true;
        }
        boolean submit() {
            if (submitted || selected.size() != SCENES) return false;
            submitted = true;
            return true;
        }
        int score() {
            if (!submitted) return 0;
            int score = 0;
            for (int i = 0; i < selected.size(); i++) if (selected.get(i) == i) score++;
            return score;
        }
    }
}
