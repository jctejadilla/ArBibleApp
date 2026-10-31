package com.example.arbibleapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;
import android.widget.ImageView;

final class MonarchyImages {
    private static final int[][] RESOURCES = {
            {R.drawable.monarchy_saul_donkeys, R.drawable.monarchy_saul_samuel,
                    R.drawable.monarchy_saul_anointed, R.drawable.monarchy_saul_chosen,
                    R.drawable.monarchy_saul_jabesh, R.drawable.monarchy_saul_gilgal},
            {R.drawable.monarchy_david_sheep, R.drawable.monarchy_david_anointed,
                    R.drawable.monarchy_david_goliath, R.drawable.monarchy_david_cave,
                    R.drawable.monarchy_david_king, R.drawable.monarchy_david_ark},
            {R.drawable.monarchy_solomon_king, R.drawable.monarchy_solomon_wisdom,
                    R.drawable.monarchy_solomon_mothers, R.drawable.monarchy_solomon_building,
                    R.drawable.monarchy_solomon_dedication, R.drawable.monarchy_solomon_sheba}
    };
    private static final LruCache<String, Bitmap> CACHE = new LruCache<>(18);
    static int resource(String key) {
        for (int r = 0; r < MonarchyScenarioBank.KEYS.length; r++)
            for (int i = 0; i < MonarchyScenarioBank.KEYS[r].length; i++)
                if (MonarchyScenarioBank.KEYS[r][i].equals(key)) return RESOURCES[r][i];
        return 0;
    }
    static void showHistorical(ImageView image, String key) {
        Object token = new Object(); image.setTag(token); image.setImageDrawable(null);
        if (key == null || !key.startsWith("monarchy-approved:")) { show(image, key); return; }
        String[] parts = key.split(":", 3);
        if (parts.length != 3 || parts[1].isEmpty() || parts[1].contains("/")) return;
        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("quiz_questions")
                .document(parts[1]).get().addOnSuccessListener(doc -> {
                    if (image.getTag() != token) return;
                    MonarchyContentBank.Scenario scenario = MonarchyContentBank.parse(doc.getData());
                    if (scenario == null) return;
                    MonarchyContentBank.Scene scene = MonarchyContentBank.find(scenario, parts[2]);
                    if (scene != null && scene.bytes != null) LawImageCodec.display(image, scene.bytes, 0);
                });
    }
    static void show(ImageView image, String key) {
        image.setImageDrawable(null);
        int resource = resource(key);
        if (resource == 0) return;
        Bitmap bitmap = CACHE.get(key);
        if (bitmap == null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(image.getResources(), resource, options);
            options.inSampleSize = 1;
            while (options.outWidth / options.inSampleSize > 512
                    && options.outHeight / options.inSampleSize > 512) options.inSampleSize *= 2;
            options.inJustDecodeBounds = false;
            bitmap = BitmapFactory.decodeResource(image.getResources(), resource, options);
            if (bitmap != null) CACHE.put(key, bitmap);
        }
        image.setImageBitmap(bitmap);
    }
}
