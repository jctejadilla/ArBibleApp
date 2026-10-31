package com.example.arbibleapp;

import org.junit.Test;
import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import static org.junit.Assert.*;

public class ReturnMatchingGameTest {
    private int other(ReturnMatchingGame game, int first, boolean match) {
        for (int i = 0; i < game.deck.length; i++)
            if (i != first && (game.deck[i] == game.deck[first]) == match) return i;
        throw new AssertionError("No card");
    }

    private ReturnMatchingGame restore(ReturnMatchingGame game) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) { output.writeObject(game); }
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (ReturnMatchingGame) input.readObject();
        }
    }

    @Test public void everyDeckHasExactlyTwoCopiesOfEachPictureAndDifferentSeedsShuffle() {
        HashSet<String> arrangements = new HashSet<>();
        for (int seed = 0; seed < 100; seed++) {
            ReturnMatchingGame game = new ReturnMatchingGame(seed);
            int[] counts = new int[6];
            for (int key : game.deck) counts[key]++;
            for (int count : counts) assertEquals(2, count);
            for (int i = 0; i < 12; i++) assertFalse(game.faceUp(i));
            arrangements.add(Arrays.toString(game.deck));
            assertArrayEquals(game.deck, new ReturnMatchingGame(seed).deck);
        }
        assertTrue(arrangements.size() > 90);
    }

    @Test public void aMatchingPairCountsExactlyOnceAndSameCardCannotMatchItself() {
        ReturnMatchingGame game = new ReturnMatchingGame(42);
        int twin = other(game, 0, true);
        assertEquals(ReturnMatchingGame.Selection.IGNORED, game.select(-1, 0));
        assertEquals(ReturnMatchingGame.Selection.IGNORED, game.select(12, 0));
        assertEquals(ReturnMatchingGame.Selection.FIRST, game.select(0, 0));
        assertEquals(ReturnMatchingGame.Selection.IGNORED, game.select(0, 0));
        assertEquals(0, game.moves);
        assertEquals(ReturnMatchingGame.Selection.MATCH, game.select(twin, 0));
        assertEquals(1, game.score());
        assertEquals(1, game.moves);
        assertTrue(game.faceUp(0));
        assertTrue(game.faceUp(twin));
        assertEquals(ReturnMatchingGame.Selection.IGNORED, game.select(twin, 0));
        assertEquals(1, game.score());
    }

    @Test public void mismatchBlocksThirdCardAndWaitsUntilDeadlineWithoutPenalty() {
        ReturnMatchingGame game = new ReturnMatchingGame(42);
        int different = other(game, 0, false);
        game.select(0, 500);
        assertEquals(ReturnMatchingGame.Selection.MISMATCH, game.select(different, 500));
        int third = different == 1 ? 2 : 1;
        assertEquals(ReturnMatchingGame.Selection.IGNORED, game.select(third, 600));
        assertFalse(game.resolveMismatch(game.hideAt - 1));
        assertTrue(game.faceUp(0));
        assertTrue(game.faceUp(different));
        assertEquals(1, game.moves);
        assertEquals(0, game.score());
        assertEquals(game.deck[0], game.misses.get(0).firstKey);
        assertEquals(game.deck[different], game.misses.get(0).secondKey);
        assertTrue(game.resolveMismatch(game.hideAt));
        assertFalse(game.faceUp(0));
        assertFalse(game.faceUp(different));
        assertFalse(game.resolveMismatch(Long.MAX_VALUE));
        game.select(0, 2000);
        game.select(other(game, 0, true), 2000);
        assertEquals(1, game.score());
    }

    @Test public void serializationPreservesFirstCardPendingMismatchAndMatchedPairs() throws Exception {
        ReturnMatchingGame game = new ReturnMatchingGame(18);
        game.select(0, 100);
        game = restore(game);
        assertEquals(0, game.first);
        int different = other(game, 0, false);
        game.select(different, 400);
        ReturnMatchingGame restored = restore(game);
        assertArrayEquals(game.deck, restored.deck);
        assertEquals(game.first, restored.first);
        assertEquals(game.second, restored.second);
        assertEquals(game.hideAt, restored.hideAt);
        assertEquals(game.moves, restored.moves);
        assertEquals(game.misses.get(0).secondKey, restored.misses.get(0).secondKey);
        assertTrue(restored.resolveMismatch(restored.hideAt));
        restored.select(0, 2000);
        restored.select(other(restored, 0, true), 2000);
        ReturnMatchingGame matched = restore(restored);
        assertEquals(1, matched.score());
        assertArrayEquals(restored.matched, matched.matched);
        assertEquals(1, matched.misses.size());
    }

    @Test public void completingAllPairsAwardsAtMost120SundayXpAndZeroTrialXp() {
        ReturnMatchingGame game = new ReturnMatchingGame(7);
        for (int i = 0; i < game.deck.length; i++) if (!game.matched[i]) {
            assertFalse(game.complete());
            game.select(i, 0);
            game.select(other(game, i, true), 0);
        }
        assertTrue(game.complete());
        assertEquals(6, game.moves);
        assertEquals(6, game.score());
        assertEquals(120, JudgesRewardPolicy.xp(game.score(), true));
        assertEquals(0, JudgesRewardPolicy.xp(game.score(), false));
        assertTrue(game.misses.isEmpty());
        assertEquals(ReturnMatchingGame.Selection.IGNORED, game.select(0, 0));
    }
}
