package com.example.arbibleapp;

/** Fixed, chronological scene IDs; historical results store caption snapshots separately. */
final class MonarchyScenarioBank {
    static final String STORY_TITLE = "The Monarchy: Saul, David, and Solomon";
    static final String[] IDS = {"saul", "david", "solomon"};
    static final String[] TITLES = {"Saul: Israel’s First King", "David: From Shepherd to King",
            "Solomon: Wisdom and the Temple"};
    static final String[][] KEYS = {
            {"saul_donkeys", "saul_samuel", "saul_anointed", "saul_chosen", "saul_jabesh", "saul_gilgal"},
            {"david_sheep", "david_anointed", "david_goliath", "david_cave", "david_king", "david_ark"},
            {"solomon_king", "solomon_wisdom", "solomon_mothers", "solomon_building",
                    "solomon_dedication", "solomon_sheba"}
    };
    static final String[][] CAPTIONS = {
            {"Saul searches for lost donkeys", "Saul meets Samuel", "Samuel anoints Saul",
                    "Saul is publicly chosen as king", "Saul rescues Jabesh",
                    "Saul’s kingship is reaffirmed at Gilgal"},
            {"David tends sheep", "Samuel anoints David", "David faces Goliath",
                    "David hides from Saul", "David becomes king over Israel",
                    "David brings the Ark to Jerusalem"},
            {"Solomon becomes king", "Solomon asks God for wisdom",
                    "Solomon resolves the mothers’ dispute", "Solomon builds the temple",
                    "Solomon dedicates the temple", "Solomon welcomes the Queen of Sheba"}
    };

    static String caption(String key) {
        for (int r = 0; r < KEYS.length; r++)
            for (int i = 0; i < KEYS[r].length; i++) if (KEYS[r][i].equals(key)) return CAPTIONS[r][i];
        return "";
    }
}
