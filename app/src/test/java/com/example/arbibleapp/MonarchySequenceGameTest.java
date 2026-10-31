package com.example.arbibleapp;

import org.junit.Test;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;

public class MonarchySequenceGameTest {
    private void submit(MonarchySequenceGame.Round round, int... order) {
        for (int scene : order) assertTrue(round.select(scene));
        assertTrue(round.submit());
    }
    private MonarchySequenceGame restore(MonarchySequenceGame game) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(game); }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (MonarchySequenceGame) in.readObject();
        }
    }

    @Test public void gridsContainAllScenesOnceAndSeedsShuffle() {
        Set<String> grids = new HashSet<>();
        for (int seed = 0; seed < 100; seed++) {
            MonarchySequenceGame game = new MonarchySequenceGame(seed);
            for (MonarchySequenceGame.Round round : game.rounds) {
                assertEquals(6, round.grid.size());
                assertEquals(new HashSet<>(Arrays.asList(0,1,2,3,4,5)), new HashSet<>(round.grid));
            }
            grids.add(game.round().grid.toString());
            assertEquals(game.round().grid, new MonarchySequenceGame(seed).round().grid);
        }
        assertTrue(grids.size() > 50);
    }

    @Test public void selectionUndoResetAndIncompleteSubmit() {
        MonarchySequenceGame game = new MonarchySequenceGame(1);
        MonarchySequenceGame.Round round = game.round();
        List<Integer> grid = new ArrayList<>(round.grid);
        assertFalse(round.select(-1));
        assertFalse(round.select(6));
        assertFalse(round.undo());
        assertFalse(round.submit());
        assertFalse(game.next());
        assertTrue(round.select(4));
        assertFalse(round.select(4));
        assertTrue(round.select(2));
        assertTrue(round.undo());
        assertEquals(Arrays.asList(4), round.selected);
        assertTrue(round.select(1));
        assertTrue(round.reset());
        assertTrue(round.selected.isEmpty());
        assertFalse(round.reset());
        assertEquals(grid, round.grid);
    }

    @Test public void partialCreditLocksOnceAndNextRequiresSubmission() {
        MonarchySequenceGame game = new MonarchySequenceGame(4);
        submit(game.round(), 1,0,2,3,4,5);
        assertEquals(4, game.score());
        assertEquals(40, game.xp(true));
        assertEquals(0, game.xp(false));
        assertFalse(game.round().submit());
        assertFalse(game.round().undo());
        assertFalse(game.round().reset());
        assertFalse(game.round().select(0));
        assertEquals(4, game.score());
        assertTrue(game.next());
        assertEquals(1, game.current);
        assertTrue(game.round().selected.isEmpty());
        assertFalse(game.next());
    }

    @Test public void threeRoundsScoreOutOf18AndSundayXpCapsAt180() {
        MonarchySequenceGame game = new MonarchySequenceGame(5);
        for (int r = 0; r < 3; r++) {
            submit(game.round(), 0,1,2,3,4,5);
            if (r < 2) { assertFalse(game.complete()); assertTrue(game.next()); }
        }
        assertTrue(game.complete());
        assertEquals(18, game.score());
        assertEquals(180, game.xp(true));
        assertEquals(0, game.xp(false));
        assertFalse(game.next());
        assertEquals(2, game.current);
    }

    @Test public void completelyWrongSequenceScoresZeroAndPreservesFullCorrection() {
        MonarchySequenceGame game = new MonarchySequenceGame(6);
        submit(game.round(), 5,4,3,2,1,0);
        MonarchySequenceResult result = MonarchySequenceResult.from(game, 0);
        assertEquals(0, game.score());
        assertEquals(6, result.correctKeys.size());
        assertEquals(6, result.selectedKeys.size());
        assertEquals("saul_gilgal", result.selectedKeys.get(0));
        assertEquals("saul_donkeys", result.correctKeys.get(0));
        assertEquals("Saul searches for lost donkeys", result.correctCaptions.get(0));
        assertTrue(MonarchySequenceResult.orderedText(result.correctCaptions).startsWith("1. "));
    }

    @Test public void restoredStateKeepsGridSelectionAndSubmittedScores() throws Exception {
        MonarchySequenceGame original = new MonarchySequenceGame(8);
        original.round().select(3);
        original.round().select(0);
        MonarchySequenceGame restored = restore(original);
        assertEquals(original.round().grid, restored.round().grid);
        assertEquals(Arrays.asList(3,0), restored.round().selected);
        assertTrue(restored.round().reset());
        submit(restored.round(), 1,0,2,3,4,5);
        restored.next();
        restored.round().select(5);
        MonarchySequenceGame again = restore(restored);
        assertEquals(1, again.current);
        assertEquals(4, again.score());
        assertTrue(again.rounds[0].submitted);
        assertEquals(Arrays.asList(5), again.round().selected);
        assertFalse(again.rounds[0].reset());
    }

    @Test public void resultSnapshotsAreIndependentLists() {
        MonarchySequenceGame game = new MonarchySequenceGame(12);
        submit(game.round(), 0,1,2,3,4,5);
        MonarchySequenceResult first = MonarchySequenceResult.from(game, 0);
        MonarchySequenceResult second = MonarchySequenceResult.from(game, 0);
        first.correctCaptions.set(0, "Historical caption");
        assertEquals("Saul searches for lost donkeys", second.correctCaptions.get(0));
        assertEquals(6, second.correctPositions);
    }
}
