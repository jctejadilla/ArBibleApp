package com.example.arbibleapp;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import static org.junit.Assert.*;

public class FloodGridGeneratorTest {
    private String[] longWords() {
        return new String[]{"ELEPHANT", "ANTELOPE", "KANGAROO", "HEDGEHOG", "FLAMINGO",
                "STARFISH", "SEAHORSE", "SQUIRREL", "REINDEER", "PLATYPUS"};
    }

    // Independent oracle: read complete rows, columns and diagonals in both directions.
    // This deliberately does not call the production contains() method.
    private void assertComplete(char[][] grid, String[] words) {
        int size = grid.length;
        assertTrue(size == 8 || size == 10);
        for (char[] row : grid) {
            assertEquals(size, row.length);
            for (char cell : row) assertTrue(cell >= 'A' && cell <= 'Z');
        }
        List<String> lines = new ArrayList<>();
        for (int axis = 0; axis < size; axis++) {
            lines.add(new String(grid[axis]));
            StringBuilder column = new StringBuilder();
            for (int row = 0; row < size; row++) column.append(grid[row][axis]);
            lines.add(column.toString());
        }
        for (int key = 0; key < size * 2 - 1; key++) {
            StringBuilder descending = new StringBuilder();
            StringBuilder ascending = new StringBuilder();
            for (int row = 0; row < size; row++) {
                int colDown = row + key - (size - 1);
                int colUp = key - row;
                if (colDown >= 0 && colDown < size) descending.append(grid[row][colDown]);
                if (colUp >= 0 && colUp < size) ascending.append(grid[row][colUp]);
            }
            lines.add(descending.toString());
            lines.add(ascending.toString());
        }
        for (String word : words) {
            String reverse = new StringBuilder(word).reverse().toString();
            assertTrue("Word missing from grid: " + word,
                    lines.stream().anyMatch(line -> line.contains(word) || line.contains(reverse)));
        }
    }

    @Test
    public void everyWordAppearsAcrossOneHundredSeedsForEachBank() {
        String[] replaced = FloodWordBank.defaults();
        replaced[0] = "TIGER";
        replaced[4] = "ELEPHANT";
        for (String[] words : new String[][]{FloodWordBank.defaults(), replaced, longWords()}) {
            String[] original = words.clone();
            for (int seed = 0; seed < 100; seed++) {
                assertComplete(FloodGridGenerator.generate(words, new Random(seed)), words);
                assertArrayEquals(original, words);
            }
        }
    }

    @Test
    public void forcedFallbackFitsAllWordsAcrossOneHundredSeeds() {
        for (String[] words : new String[][]{FloodWordBank.defaults(), longWords()}) {
            for (int seed = 0; seed < 100; seed++) {
                char[][] grid = FloodGridGenerator.generate(words, new Random(seed), 0);
                assertEquals(10, grid.length);
                assertComplete(grid, words);
            }
        }
    }

    @Test
    public void normalDefaultsUseEightByEightWhenPlacementSucceeds() {
        char[][] grid = FloodGridGenerator.generate(FloodWordBank.defaults(), new Random(42));
        assertEquals(8, grid.length);
        assertComplete(grid, FloodWordBank.defaults());
    }

    @Test
    public void impossibleEightByEightBankUsesFallbackWithoutLosingWords() {
        // Ten disjoint eight-letter words require 80 cells; an 8x8 board has only 64.
        String[] words = new String[10];
        for (int i = 0; i < words.length; i++) words[i] = String.valueOf((char) ('A' + i)).repeat(8);
        char[][] grid = FloodGridGenerator.generate(words, new Random(42));
        assertEquals(10, grid.length);
        assertComplete(grid, words);
    }

    @Test
    public void malformedWordsAndWrongCountsAreRejected() {
        for (String invalid : new String[]{null, "", "tiger", "DOG1", "SEA LION", "ABCDEFGHI"}) {
            String[] words = FloodWordBank.defaults();
            words[0] = invalid;
            expectRejected(words);
        }
        expectRejected(new String[]{"BEAR"});
        String[] duplicates = FloodWordBank.defaults();
        duplicates[0] = duplicates[1];
        expectRejected(duplicates);
    }

    private void expectRejected(String[] words) {
        try {
            FloodGridGenerator.generate(words, new Random(1));
            fail("Expected invalid bank to be rejected");
        } catch (IllegalArgumentException expected) {
            // An invalid bank must never become a playable puzzle.
        }
    }
}