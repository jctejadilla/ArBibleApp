package com.example.arbibleapp;

import com.google.firebase.firestore.Blob;

// Image actions are independent of scenario wording and classification.
final class LawImageData {
    static final int MAX_BYTES = 200 * 1024;
    final String action;
    final byte[] bytes;
    final boolean valid;

    private LawImageData(String action, byte[] bytes, boolean valid) {
        this.action = action;
        this.bytes = bytes;
        this.valid = valid;
    }

    static LawImageData read(Object action, Object value) {
        if (action == null || "KEEP".equals(action)) {
            return new LawImageData("KEEP", null, value == null);
        }
        if ("DEFAULT".equals(action)) return new LawImageData("DEFAULT", null, value == null);
        if ("REPLACE".equals(action) && value instanceof Blob) {
            byte[] bytes = ((Blob) value).toBytes();
            if (bytes.length > 0 && bytes.length <= MAX_BYTES) return new LawImageData("REPLACE", bytes, true);
        }
        return new LawImageData("DEFAULT", null, false);
    }
}