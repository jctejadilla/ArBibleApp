package com.example.arbibleapp;

/** Stable labels and keys preserve the original submitted match in historical results. */
public class DivisionPairResult {
    public int round;
    public String characterKey, characterName, selectedObjectKey, selectedObjectName,
            correctObjectKey, correctObjectName;
    public String characterImageReference, selectedObjectImageReference, correctObjectImageReference;
    public boolean correct;
    public DivisionPairResult() {}
    static DivisionPairResult from(DivisionMatchingGame game, int roundIndex, int row) {
        return from(game, roundIndex, row, DivisionContentBank.defaults());
    }
    static DivisionPairResult from(DivisionMatchingGame game, int roundIndex, int row,
                                   java.util.List<DivisionContentBank.Pair> bank) {
        DivisionMatchingGame.Round round = game.rounds[roundIndex];
        if (!round.submitted) throw new IllegalStateException("Round not submitted");
        int character = round.characters.get(row), selected = round.slots[row];
        DivisionPairResult result = new DivisionPairResult();
        result.round = roundIndex + 1;
        result.characterKey = DivisionPairBank.KEYS[character];
        result.characterName = bank.get(character).characterName;
        result.characterImageReference = bank.get(character).imageDocuments[0];
        result.selectedObjectKey = DivisionPairBank.OBJECT_KEYS[selected];
        result.selectedObjectName = bank.get(selected).objectName;
        result.selectedObjectImageReference = bank.get(selected).imageDocuments[1];
        result.correctObjectKey = DivisionPairBank.OBJECT_KEYS[character];
        result.correctObjectName = bank.get(character).objectName;
        result.correctObjectImageReference = bank.get(character).imageDocuments[1];
        result.correct = character == selected;
        return result;
    }
}