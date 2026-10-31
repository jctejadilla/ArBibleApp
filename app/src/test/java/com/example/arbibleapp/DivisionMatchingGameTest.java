package com.example.arbibleapp;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.util.*;

public class DivisionMatchingGameTest {
    @Test public void roundsSeparateRoyalObjectsAndUseEveryPairOnce() {
        for (int seed = 0; seed < 100; seed++) {
            DivisionMatchingGame game = new DivisionMatchingGame(seed);
            Set<Integer> all = new HashSet<>();
            for (DivisionMatchingGame.Round round : game.rounds) {
                assertEquals(new HashSet<>(round.characters), new HashSet<>(round.objects));
                int royals = 0;
                for (int character : round.characters) {
                    assertTrue(all.add(character));
                    if (character == 0 || character == 3 || character == 6) royals++;
                }
                assertEquals(1, royals);
            }
            assertEquals(9, all.size());
        }
    }
    @Test public void movesSwapOrReturnDisplacedObjectsWithoutDuplicates() {
        DivisionMatchingGame.Round round = new DivisionMatchingGame(4).round();
        assertTrue(round.assign(0, 0));
        assertTrue(round.assign(1, 1));
        assertTrue(round.assign(0, 1));
        assertArrayEquals(new int[]{1, 0, -1}, round.slots);
        assertTrue(round.assign(2, 1));
        assertArrayEquals(new int[]{1, 2, -1}, round.slots);
        assertEquals(-1, round.location(0));
        assertTrue(round.remove(1));
        assertFalse(round.assign(8, 0));
        assertFalse(round.assign(2, 3));
        assertFalse(round.assign(2, -1));
        assertArrayEquals(new int[]{-1, 2, -1}, round.slots);
        assertTrue(round.reset());
        assertArrayEquals(new int[]{-1, -1, -1}, round.slots);
    }
    @Test public void submissionRequiresThreeAnswersAndLocksScoring() {
        DivisionMatchingGame game = new DivisionMatchingGame(2);
        assertFalse(game.next());
        assertFalse(game.round().submit());
        for (DivisionMatchingGame.Round round : game.rounds) {
            for (int i = 0; i < 3; i++) round.assign(round.characters.get(i), i);
            assertEquals(0, round.score());
            assertTrue(round.submit());
            assertFalse(round.submit());
            assertFalse(round.reset());
            assertFalse(round.remove(round.slots[0]));
            assertFalse(round.assign(round.slots[0], 1));
            assertEquals(3, round.score());
        }
        assertTrue(game.complete());
        assertEquals(9, game.score());
        assertEquals(90, game.xp(true));
        assertEquals(0, game.xp(false));
        assertTrue(game.next());
        assertTrue(game.next());
        assertFalse(game.next());
    }
    @Test public void partialScoreCountsCorrectPairsOnly() {
        DivisionMatchingGame.Round round = new DivisionMatchingGame(5).round();
        round.assign(round.characters.get(0), 0);
        round.assign(round.characters.get(2), 1);
        round.assign(round.characters.get(1), 2);
        round.submit();
        assertEquals(1, round.score());
    }
    @Test public void recreationPreservesOrderPlacementsSelectionAndSubmission() throws Exception {
        DivisionMatchingGame game = new DivisionMatchingGame(17);
        game.round().assign(0, 2);
        game.selected = 0;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        new ObjectOutputStream(bytes).writeObject(game);
        DivisionMatchingGame restored = (DivisionMatchingGame) new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray())).readObject();
        assertEquals(game.round().characters, restored.round().characters);
        assertEquals(game.round().objects, restored.round().objects);
        assertArrayEquals(game.round().slots, restored.round().slots);
        assertEquals(0, restored.selected);
        assertFalse(restored.round().submitted);
    }
    @Test public void resultSnapshotsKeepSelectedAndCorrectObjectsSeparate() {
        DivisionMatchingGame game = new DivisionMatchingGame(8);
        DivisionMatchingGame.Round round = game.round();
        for (int i = 0; i < 3; i++) round.assign(round.characters.get((i + 1) % 3), i);
        round.submit();
        DivisionPairResult result = DivisionPairResult.from(game, 0, 0);
        assertFalse(result.correct);
        assertEquals(DivisionPairBank.CHARACTERS[round.characters.get(0)], result.characterName);
        assertEquals(DivisionPairBank.OBJECTS[round.slots[0]], result.selectedObjectName);
        assertNotEquals(result.selectedObjectKey, result.correctObjectKey);
    }
}