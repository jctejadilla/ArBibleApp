package com.example.arbibleapp;

import java.util.ArrayList;
import java.util.List;

/** Firestore-compatible snapshots keep past reviews independent of future scene edits. */
public class MonarchySequenceResult {
    public String scenarioId, title;
    public int correctPositions;
    public List<String> selectedKeys, correctKeys, selectedCaptions, correctCaptions;

    public MonarchySequenceResult() {}

    static MonarchySequenceResult from(MonarchySequenceGame game, int scenario) {
        return from(game, scenario, MonarchyContentBank.defaults());
    }
    static MonarchySequenceResult from(MonarchySequenceGame game, int scenario,
                                       List<MonarchyContentBank.Scenario> bank) {
        MonarchySequenceGame.Round round = game.rounds[scenario];
        if (!round.submitted) throw new IllegalStateException("Scenario has not been submitted");
        MonarchySequenceResult result = new MonarchySequenceResult();
        result.scenarioId = MonarchyScenarioBank.IDS[scenario];
        result.title = bank.get(scenario).title;
        result.correctPositions = round.score();
        result.selectedKeys = new ArrayList<>();
        result.correctKeys = new ArrayList<>();
        result.selectedCaptions = new ArrayList<>();
        result.correctCaptions = new ArrayList<>();
        for (int position = 0; position < MonarchySequenceGame.SCENES; position++) {
            int chosen = round.selected.get(position);
            result.selectedKeys.add(bank.get(scenario).scenes.get(chosen).imageKey());
            result.correctKeys.add(bank.get(scenario).scenes.get(position).imageKey());
            result.selectedCaptions.add(bank.get(scenario).scenes.get(chosen).caption);
            result.correctCaptions.add(bank.get(scenario).scenes.get(position).caption);
        }
        return result;
    }

    static String orderedText(List<String> captions) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < captions.size(); i++) {
            if (i > 0) text.append("\n");
            text.append(i + 1).append(". ").append(captions.get(i));
        }
        return text.toString();
    }
}
