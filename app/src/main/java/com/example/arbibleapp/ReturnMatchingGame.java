package com.example.arbibleapp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

final class ReturnMatchingGame implements Serializable {
    private static final long serialVersionUID = 1L;
    static final String STORY_TITLE = "Return from Exile and Anticipation of the Messiah";
    static final String[] KEYS = {"temple", "scroll", "gate", "families", "ezra", "nehemiah"};
    static final String[] LABELS = {"Rebuilt temple", "Scripture scroll", "Jerusalem gate",
            "Returning families", "Ezra", "Nehemiah"};
    static final int PAIRS = 6;
    static final long FLIP_MS = 240;
    static final long MISMATCH_MS = 1000;
    enum Selection { IGNORED, FIRST, MATCH, MISMATCH }

    final int[] deck = new int[PAIRS * 2];
    final boolean[] matched = new boolean[PAIRS * 2];
    final List<Miss> misses = new ArrayList<>();
    int first = -1, second = -1, moves;
    long hideAt;

    ReturnMatchingGame(long seed) {
        List<Integer> cards = new ArrayList<>();
        for (int key = 0; key < PAIRS; key++) { cards.add(key); cards.add(key); }
        Collections.shuffle(cards, new Random(seed));
        for (int i = 0; i < deck.length; i++) deck[i] = cards.get(i);
    }

    Selection select(int position, long now) {
        if (position < 0 || position >= deck.length || matched[position]
                || position == first || second != -1 || complete()) return Selection.IGNORED;
        if (first == -1) {
            first = position;
            return Selection.FIRST;
        }
        second = position;
        moves++;
        if (deck[first] == deck[second]) {
            matched[first] = matched[second] = true;
            first = second = -1;
            return Selection.MATCH;
        }
        misses.add(new Miss(deck[first], deck[second], moves));
        // Keep both fronts visible for a full second after the reveal animation.
        hideAt = now + FLIP_MS + MISMATCH_MS;
        return Selection.MISMATCH;
    }

    boolean resolveMismatch(long now) {
        if (second == -1 || now < hideAt) return false;
        first = second = -1;
        hideAt = 0;
        return true;
    }

    boolean faceUp(int position) {
        return matched[position] || position == first || position == second;
    }

    int score() {
        int count = 0;
        for (boolean value : matched) if (value) count++;
        return count / 2;
    }

    boolean complete() { return score() == PAIRS; }

    static final class Miss implements Serializable {
        private static final long serialVersionUID = 1L;
        final int firstKey, secondKey, move;
        Miss(int firstKey, int secondKey, int move) {
            this.firstKey = firstKey;
            this.secondKey = secondKey;
            this.move = move;
        }
    }
}
