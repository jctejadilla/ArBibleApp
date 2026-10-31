package com.example.arbibleapp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

final class FloodGridGenerator {
    private static final int[] DR = {0, 0, 1, -1, 1, 1, -1, -1};
    private static final int[] DC = {1, -1, 0, 0, 1, -1, 1, -1};

    static char[][] generate(String[] words, Random random) {
        return generate(words, random, 100);
    }

    // The attempt limit also lets tests exercise the guaranteed fallback.
    static char[][] generate(String[] words, Random random, int attemptLimit) {
        if (words.length != 10 || FloodWordBank.hasDuplicates(words)) {
            throw new IllegalArgumentException("The puzzle requires ten unique words.");
        }
        for (String word : words) {
            if (word == null || !word.matches("[A-Z]{1,8}")) {
                throw new IllegalArgumentException("Words must contain 1-8 uppercase letters.");
            }
        }
        String[] sorted = words.clone();
        Arrays.sort(sorted, (a, b) -> Integer.compare(b.length(), a.length()));
        for (int attempt = 0; attempt < attemptLimit; attempt++) {
            char[][] grid = new char[8][8];
            boolean complete = true;
            for (String word : sorted) {
                boolean placed = false;
                for (int tries = 0; tries < 100; tries++) {
                    int r = random.nextInt(8), c = random.nextInt(8), direction = random.nextInt(8);
                    if (canPlace(grid, word, r, c, DR[direction], DC[direction])) {
                        place(grid, word, r, c, DR[direction], DC[direction]);
                        placed = true;
                        break;
                    }
                }
                if (!placed) {
                    complete = false;
                    break;
                }
            }
            if (complete) return finish(grid, words, random);
        }

        // One word per row always fits, including ten unrelated eight-letter words.
        char[][] grid = new char[10][10];
        List<Integer> rows = new ArrayList<>();
        for (int i = 0; i < 10; i++) rows.add(i);
        Collections.shuffle(rows, random);
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            int left = random.nextInt(11 - word.length());
            boolean reverse = random.nextBoolean();
            place(grid, word, rows.get(i), reverse ? left + word.length() - 1 : left,
                    0, reverse ? -1 : 1);
        }
        return finish(grid, words, random);
    }

    private static boolean canPlace(char[][] grid, String word, int r, int c, int dr, int dc) {
        int endR = r + dr * (word.length() - 1), endC = c + dc * (word.length() - 1);
        if (endR < 0 || endR >= grid.length || endC < 0 || endC >= grid.length) return false;
        for (int i = 0; i < word.length(); i++) {
            char cell = grid[r + dr * i][c + dc * i];
            if (cell != 0 && cell != word.charAt(i)) return false;
        }
        return true;
    }

    private static void place(char[][] grid, String word, int r, int c, int dr, int dc) {
        for (int i = 0; i < word.length(); i++) grid[r + dr * i][c + dc * i] = word.charAt(i);
    }

    private static char[][] finish(char[][] grid, String[] words, Random random) {
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid.length; c++) {
                if (grid[r][c] == 0) grid[r][c] = (char) ('A' + random.nextInt(26));
            }
        }
        for (String word : words) {
            if (!contains(grid, word)) throw new IllegalStateException("Missing puzzle word: " + word);
        }
        return grid;
    }

    static boolean contains(char[][] grid, String word) {
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid.length; c++) {
                for (int d = 0; d < DR.length; d++) {
                    int endR = r + DR[d] * (word.length() - 1);
                    int endC = c + DC[d] * (word.length() - 1);
                    if (endR < 0 || endR >= grid.length || endC < 0 || endC >= grid.length) continue;
                    int i = 0;
                    while (i < word.length() && grid[r + DR[d] * i][c + DC[d] * i] == word.charAt(i)) i++;
                    if (i == word.length()) return true;
                }
            }
        }
        return false;
    }
}