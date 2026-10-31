package com.example.arbibleapp;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

final class JudgesImages {
    private static final int[] RESOURCES = {
            R.drawable.judges_joshua, R.drawable.judges_samson, R.drawable.judges_gideon,
            R.drawable.judges_rahab, R.drawable.judges_trumpet, R.drawable.judges_torch,
            R.drawable.judges_wall, R.drawable.judges_river, R.drawable.judges_rope,
            R.drawable.judges_jars
    };

    private JudgesImages() {}

    static int resource(Context context, String key) {
        for (int i = 0; i < JudgesQuizGame.KEYS.length; i++)
            if (JudgesQuizGame.KEYS[i].equals(key)) return RESOURCES[i];
        return 0;
    }

    static void showHistorical(ImageView image, String key) {
        Object token = new Object();
        image.setTag(token);
        image.setImageDrawable(null);
        if (key == null || !key.startsWith("judges-approved:")) {
            show(image, key);
            return;
        }
        String[] parts = key.split(":", 3);
        if (parts.length != 3 || parts[1].isEmpty() || parts[1].contains("/")) return;
        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("quiz_questions")
                .document(parts[1]).get().addOnSuccessListener(doc -> {
                    if (image.getTag() != token) return;
                    JudgesContentBank.Item item = JudgesContentBank.parse(doc.getData());
                    if (item == null || !JudgesQuizGame.KEYS[item.index].equals(parts[2])) return;
                    LawImageData data = LawImageData.read(doc.get("imageAction"), doc.get("imageBytes"));
                    if (data.valid && "REPLACE".equals(data.action))
                        LawImageCodec.display(image, data.bytes, 0);
                });
    }

    static void show(ImageView image, String key) {
        int resource = resource(image.getContext(), key);
        image.setImageDrawable(null);
        if (resource == 0) return;
        // The bundled originals are larger than the cards. Avoid decoding every
        // review image at full resolution on devices with smaller heaps.
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(image.getResources(), resource, options);
        options.inSampleSize = 1;
        while (options.outWidth / options.inSampleSize > 768
                && options.outHeight / options.inSampleSize > 768) options.inSampleSize *= 2;
        options.inJustDecodeBounds = false;
        image.setImageBitmap(BitmapFactory.decodeResource(image.getResources(), resource, options));
    }
}
