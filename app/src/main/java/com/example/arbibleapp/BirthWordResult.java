package com.example.arbibleapp;

/** Snapshot of the submitted question, independent of later question-bank changes. */
public class BirthWordResult {
    public String imageKey, displayName, expectedWord, selectedWord;
    public boolean correct;
    public BirthWordResult() {}
    static BirthWordResult from(BirthWordGame game, int index) {
        return from(game, index, BirthContentBank.defaults().get(game.contentIndices[index]));
    }
    static BirthWordResult from(BirthWordGame game, int index, BirthContentBank.Item item) {
        BirthWordResult result = new BirthWordResult();
        result.imageKey = item.imageKey();
        result.displayName = item.label;
        result.expectedWord = game.rounds[index].answer;
        result.selectedWord = game.rounds[index].entered();
        result.correct = game.rounds[index].correct();
        return result;
    }
}
