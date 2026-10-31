package com.example.arbibleapp;

import org.junit.Test;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import static org.junit.Assert.*;

public class JudgesQuizGameTest {
    @Test public void scrambleAlwaysMakesOneInteriorAdjacentSwap() {
        for (int seed = 0; seed < 100; seed++) for (String key : JudgesQuizGame.KEYS) {
            String word = key.toUpperCase(java.util.Locale.ROOT);
            String scrambled = JudgesQuizGame.scramble(key, new Random(seed));
            assertNotEquals(word, scrambled);
            assertEquals(word.charAt(0), scrambled.charAt(0));
            assertEquals(word.charAt(word.length() - 1), scrambled.charAt(word.length() - 1));
            char[] a = word.toCharArray(), b = scrambled.toCharArray();
            Arrays.sort(a); Arrays.sort(b); assertArrayEquals(a, b);
            int first = -1, changes = 0;
            for (int i = 0; i < word.length(); i++) if (word.charAt(i) != scrambled.charAt(i)) {
                if (first == -1) first = i;
                else assertEquals(first + 1, i);
                changes++;
            }
            assertEquals(2, changes);
        }
    }

    @Test public void roundsHaveUniqueChoicesAndAllTenAnswers() {
        for (int seed = 0; seed < 100; seed++) {
            JudgesQuizGame game = new JudgesQuizGame(seed);
            HashSet<Integer> answers = new HashSet<>();
            for (JudgesQuizGame.Round round : game.rounds) {
                answers.add(round.answer);
                assertEquals(4, new HashSet<>(round.choices).size());
                assertEquals(4, round.choices.size());
                assertTrue(round.choices.contains(round.answer));
                for (int choice : round.choices) assertEquals(round.answer < 4, choice < 4);
            }
            assertEquals(10, answers.size());
        }
    }

    @Test public void seedRecreatesAttemptAndAnswersCountOnce() {
        JudgesQuizGame game = new JudgesQuizGame(42);
        JudgesQuizGame restored = new JudgesQuizGame(42);
        assertFalse(game.complete());
        for (int i = 0; i < 10; i++) {
            assertEquals(game.rounds.get(i).scrambled, restored.rounds.get(i).scrambled);
            assertEquals(game.rounds.get(i).choices, restored.rounds.get(i).choices);
            assertEquals(game.rounds.get(i).answer, restored.rounds.get(i).answer);
            assertTrue(game.select(i, game.rounds.get(i).answer));
            assertFalse(game.select(i, game.rounds.get(i).answer));
        }
        assertEquals(10, game.score());
        assertTrue(game.complete());
        assertFalse(game.select(-1, 0));
        assertFalse(game.select(10, 0));
    }

    @Test public void incorrectSelectionsCompleteWithoutPoints() {
        JudgesQuizGame game = new JudgesQuizGame(3);
        for (int i = 0; i < 10; i++) {
            assertFalse(game.select(i, 99));
            JudgesQuizGame.Round round = game.rounds.get(i);
            int wrong = round.choices.stream().filter(c -> c != round.answer).findFirst().get();
            assertTrue(game.select(i, wrong));
        }
        assertEquals(0, game.score());
        assertTrue(game.complete());
    }
}
