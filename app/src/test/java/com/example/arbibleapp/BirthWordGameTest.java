package com.example.arbibleapp;

import org.junit.Test;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;

public class BirthWordGameTest {
    private void solve(BirthWordGame.Round round) {
        for (int p = 0; p < round.answer.length(); p++) if (!round.hints[p] && round.slots[p] < 0) {
            boolean found = false;
            for (int t = 0; t < 10; t++) if (!round.used(t) && round.tiles.get(t) == round.answer.charAt(p)) {
                assertTrue(round.select(t));
                found = true;
                break;
            }
            assertTrue("Every missing letter must be available", found);
        }
    }
    @Test public void customAnswersKeepTenTilesHintsAndRepeatedLettersSolvable() {
        String[] answers = BirthWordBank.ANSWERS.clone(); answers[0] = "AAAAAAAAAA"; answers[1] = "ARK";
        for (int seed = 0; seed < 100; seed++) {
            BirthWordGame game = new BirthWordGame(seed, answers);
            for (BirthWordGame.Round round : game.rounds) {
                assertEquals(10, round.tiles.size());
                int hints = 0; for (boolean hint : round.hints) if (hint) hints++;
                assertEquals(round.answer.length() <= 4 ? 1 : 2, hints);
                solve(round); assertTrue(round.submit()); assertTrue(round.correct());
            }
            assertEquals(10, game.score()); assertEquals(0, game.xp(false)); assertEquals(200, game.xp(true));
        }
    }
    @Test public void allQuestionsAreSolvableWithExactlyTenTilesAndCorrectHints() {
        Set<Integer> firstQuestions = new HashSet<>();
        for (int seed = 0; seed < 250; seed++) {
            BirthWordGame game = new BirthWordGame(seed);
            firstQuestions.add(game.contentIndices[0]);
            boolean[] seen = new boolean[BirthWordGame.TOTAL];
            for (int i = 0; i < 10; i++) {
                BirthWordGame.Round round = game.rounds[i];
                int contentIndex = game.contentIndices[i];
                assertTrue(contentIndex >= 0 && contentIndex < BirthWordGame.TOTAL);
                assertFalse(seen[contentIndex]);
                seen[contentIndex] = true;
                assertEquals(BirthWordBank.ANSWERS[contentIndex], round.answer);
                assertEquals(10, round.tiles.size());
                int count = 0;
                for (boolean hint : round.hints) if (hint) count++;
                assertEquals(round.answer.length() <= 4 ? 1 : 2, count);
                solve(round);
                assertEquals(round.answer, round.entered());
            }
            for (boolean indexSeen : seen) assertTrue(indexSeen);
        }
        assertTrue("Different attempts should vary the first question", firstQuestions.size() > 1);
    }
    @Test public void hintsStayFixedAndTheExactDuplicateTileReturns() {
        BirthWordGame.Round round = new BirthWordGame.Round("PETER", new Random(8));
        round.tiles.set(0, 'E');
        round.tiles.set(1, 'E');
        assertTrue(round.select(0));
        assertFalse(round.select(0));
        assertTrue(round.select(1));
        int position = -1;
        for (int i = 0; i < round.slots.length; i++) {
            if (round.slots[i] == 0) position = i;
            if (round.hints[i]) assertFalse(round.remove(i));
        }
        assertTrue(round.remove(position));
        assertFalse(round.used(0));
        assertTrue(round.used(1));
        assertTrue(round.select(0));
        assertEquals(0, round.slots[position]);
        String hints = new BirthWordGame.Round("PETER", new Random(8)).entered();
        round.clear();
        assertEquals(hints, round.entered());
        assertFalse(round.used(0));
        assertFalse(round.used(1));
    }
    @Test public void incompleteAndInvalidActionsCannotAdvanceOrScore() {
        BirthWordGame game = new BirthWordGame(1);
        assertFalse(game.round().select(-1));
        assertFalse(game.round().select(10));
        assertFalse(game.round().remove(-1));
        assertFalse(game.round().submit());
        assertFalse(game.next());
        assertFalse(game.complete());
        assertEquals(0, game.score());
    }
    @Test public void oneSubmissionLocksWrongAnswerAndSnapshot() {
        BirthWordGame game = new BirthWordGame(1);
        BirthWordGame.Round round = game.round();
        int p = 0;
        while (round.hints[p]) p++;
        int wrong = 0;
        while (round.tiles.get(wrong) == round.answer.charAt(p)) wrong++;
        round.select(wrong);
        for (int t = 0; t < 10 && !round.full(); t++) if (!round.used(t)) round.select(t);
        assertTrue(round.submit());
        String entered = round.entered();
        assertFalse(round.correct());
        assertEquals(0, game.score());
        assertFalse(round.submit());
        assertFalse(round.remove(p));
        assertFalse(round.select(0));
        round.clear();
        assertEquals(entered, round.entered());
        BirthWordResult result = BirthWordResult.from(game, 0);
        assertEquals(entered, result.selectedWord);
        int contentIndex = game.contentIndices[0];
        assertEquals(BirthWordBank.ANSWERS[contentIndex], result.expectedWord);
        assertEquals(BirthWordBank.NAMES[contentIndex], result.displayName);
        assertFalse(result.correct);
        assertTrue(game.next());
        assertEquals(entered, result.selectedWord);
    }
    @Test public void perfectQuizScoresTenAndOnlySundayEarnsTwoHundredXP() {
        BirthWordGame game = new BirthWordGame(99);
        for (int i = 0; i < 10; i++) {
            solve(game.round());
            assertTrue(game.round().submit());
            assertFalse(game.round().submit());
            assertEquals(i + 1, game.score());
            assertEquals(i < 9, game.next());
        }
        assertTrue(game.complete());
        assertEquals(200, game.xp(true));
        assertEquals(0, game.xp(false));
    }
    @Test public void recreationPreservesUnfinishedAndSubmittedState() throws Exception {
        BirthWordGame game = new BirthWordGame(54);
        solve(game.round());
        game.round().submit();
        game.next();
        game.round().select(3);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        new ObjectOutputStream(bytes).writeObject(game);
        BirthWordGame restored = (BirthWordGame) new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray())).readObject();
        assertEquals(1, restored.current);
        assertEquals(1, restored.score());
        assertEquals(game.round().tiles, restored.round().tiles);
        assertArrayEquals(game.round().hints, restored.round().hints);
        assertArrayEquals(game.round().slots, restored.round().slots);
        assertEquals(game.round().entered(), restored.round().entered());
    }
}
