package com.example.arbibleapp;

final class DivisionPairBank {
    static final String STORY_TITLE = "Division of the Kingdom and Exile";
    static final String[] KEYS = {"solomon", "jeroboam", "elijah", "esther", "elisha", "josiah", "rehoboam", "daniel", "ezekiel"};
    static final String[] CHARACTERS = {"King Solomon", "Jeroboam", "Prophet Elijah", "Esther",
            "Prophet Elisha", "King Josiah", "Rehoboam", "Daniel", "Ezekiel"};
    static final String[] OBJECTS = {"Crown", "Golden Calf", "Fire", "Royal Crown", "Cloak",
            "Book of the Law", "Throne", "Lion", "Dry Bones"};
    static final String[] OBJECT_KEYS = {"crown", "golden_calf", "fire", "royal_crown", "cloak", "law_scroll", "throne", "lion", "dry_bones"};
    static final int[] PORTRAITS = {R.drawable.division_character_solomon, R.drawable.division_character_jeroboam, R.drawable.division_character_elijah, R.drawable.division_character_esther, R.drawable.division_character_elisha, R.drawable.division_character_josiah, R.drawable.division_character_rehoboam, R.drawable.division_character_daniel, R.drawable.division_character_ezekiel};
    static final int[] PICTURES = {R.drawable.division_object_crown, R.drawable.division_object_golden_calf, R.drawable.division_object_fire, R.drawable.division_object_royal_crown, R.drawable.division_object_cloak, R.drawable.division_object_law_scroll, R.drawable.division_object_throne, R.drawable.division_object_lion, R.drawable.division_object_dry_bones};
    static int index(String key) {
        for (int i = 0; i < KEYS.length; i++) if (KEYS[i].equals(key)) return i;
        return -1;
    }
    static int objectIndex(String key) {
        for (int i = 0; i < OBJECT_KEYS.length; i++) if (OBJECT_KEYS[i].equals(key)) return i;
        return -1;
    }
}