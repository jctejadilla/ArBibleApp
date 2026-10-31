package com.example.arbibleapp;

// Stable image roles: main scene, correct next scene, incorrect next scene.
final class ExodusImageDefaults {
    static final String[] LABELS = {"Main scene", "Correct next scenario", "Incorrect next scenario"};
    private static final int[][] IMAGES = {
            {R.drawable.exodus_slavery, R.drawable.exodus_basket, R.drawable.exodus_eden},
            {R.drawable.exodus_basket, R.drawable.exodus_pharaoh, R.drawable.exodus_noah},
            {R.drawable.exodus_pharaoh, R.drawable.exodus_journey, R.drawable.exodus_babel},
            {R.drawable.exodus_journey, R.drawable.exodus_law, R.drawable.exodus_calf},
            {R.drawable.exodus_law, R.drawable.exodus_promised_land, R.drawable.exodus_king}
    };

    private static final String[] DESCRIPTIONS = {
            "The Israelites are forced to work as slaves in Egypt.",
            "Baby Moses is placed in a basket on the river.",
            "Moses confronts Pharaoh and asks him to free the Israelites.",
            "The Israelites leave Egypt and journey through the wilderness.",
            "Moses receives God's Law for the Israelites."
    };
    private static final String[][] CAPTIONS = {
            {"Baby Moses is placed in a basket on the river.", "Adam and Eve live in the Garden of Eden."},
            {"Moses asks Pharaoh to free the Israelites.", "Noah builds an ark to prepare for the flood."},
            {"The Israelites leave Egypt and begin their journey.", "People build a tall tower in Babel."},
            {"Moses receives God's Law for the Israelites.", "The people worship a golden calf."},
            {"The Israelites enter the Promised Land.", "A king rules over the people of Israel."}
    };

    static String description(int step) {
        return DESCRIPTIONS[step];
    }

    static String caption(int step, boolean correct) {
        return CAPTIONS[step][correct ? 0 : 1];
    }

    static int image(int step, int role) {
        return IMAGES[step][role];
    }

    static byte[] bytes(LawImageData image) {
        return image != null && image.valid && "REPLACE".equals(image.action) ? image.bytes : null;
    }
}