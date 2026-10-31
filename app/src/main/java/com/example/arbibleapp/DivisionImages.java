package com.example.arbibleapp;

import android.widget.ImageView;
import com.google.firebase.firestore.FirebaseFirestore;

final class DivisionImages {
    static int resource(int index, int role) {
        return role == 0 ? DivisionPairBank.PORTRAITS[index] : DivisionPairBank.PICTURES[index];
    }
    static void show(ImageView view, DivisionContentBank.Pair pair, int role) {
        LawImageCodec.display(view, pair.images[role], resource(pair.index, role));
    }
    static void historical(ImageView view, String reference, int index, int role) {
        Object token = new Object(); view.setTag(token); view.setImageDrawable(null);
        if (reference == null || reference.isEmpty()) {
            if (index >= 0 && index < 9) view.setImageResource(resource(index, role));
            return;
        }
        if (reference.contains("/")) return;
        FirebaseFirestore.getInstance().collection("quiz_questions").document(reference).get().addOnSuccessListener(doc -> {
            if (view.getTag() != token) return;
            DivisionContentBank.Pair pair = DivisionContentBank.parse(doc.getData());
            if (pair != null && pair.index == index && pair.images[role] != null)
                LawImageCodec.display(view, pair.images[role], 0);
        });
    }
}
