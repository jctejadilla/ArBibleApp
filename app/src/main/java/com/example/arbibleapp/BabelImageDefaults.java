package com.example.arbibleapp;

// Shared default images for gameplay and configuration previews; indices are zero-based.
final class BabelImageDefaults {
    private static final int[] TOWER = {
            R.drawable.correct1, R.drawable.correct2, R.drawable.correct3, R.drawable.correct4,
            R.drawable.correct5, R.drawable.correct6, R.drawable.correct7, R.drawable.correct8,
            R.drawable.correct9, R.drawable.correct10
    };
    private static final int[] ODD = {
            R.drawable.odd1, R.drawable.odd2, R.drawable.odd3, R.drawable.odd4, R.drawable.odd5
    };

    static int image(int floor, int block, int oddBlock) {
        if (floor < 0 || floor >= 5 || block < 0 || block >= 3 || oddBlock < 0 || oddBlock >= 3)
            throw new IllegalArgumentException("Invalid Babel floor or block");
        if (block == oddBlock) return ODD[floor];
        return TOWER[floor * 2 + (block < oddBlock ? block : block - 1)];
    }

    static byte[] bytes(LawImageData proposed, LawImageData current) {
        if (!proposed.valid) return null;
        if ("REPLACE".equals(proposed.action)) return proposed.bytes;
        if ("KEEP".equals(proposed.action) && current != null && current.valid
                && "REPLACE".equals(current.action)) return current.bytes;
        return null;
    }
}