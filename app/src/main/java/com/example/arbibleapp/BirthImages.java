package com.example.arbibleapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;
import android.widget.ImageView;

final class BirthImages {
    private static final int[] RESOURCES = {
        R.drawable.birth_jesus, R.drawable.birth_mary, R.drawable.birth_joseph,
        R.drawable.birth_herod, R.drawable.birth_john, R.drawable.birth_peter,
        R.drawable.birth_star, R.drawable.birth_gold, R.drawable.birth_fish, R.drawable.birth_bread
    };
    private static final LruCache<String, Bitmap> CACHE = new LruCache<>(10);

    static int resource(String key) {
        for (int i = 0; i < BirthWordBank.KEYS.length; i++)
            if (BirthWordBank.KEYS[i].equals(key)) return RESOURCES[i];
        return 0;
    }

    static Bitmap[] prepare(android.content.Context context, java.util.List<BirthContentBank.Item> bank)
            throws java.io.IOException {
        Bitmap[] images = new Bitmap[BirthWordGame.TOTAL];
        for (int i = 0; i < images.length; i++) {
            BirthContentBank.Item item = bank.get(i);
            images[i] = item.imageBytes == null ? bundled(context.getResources(), BirthWordBank.KEYS[i])
                    : LawImageCodec.decode(item.imageBytes);
            if (images[i] == null) throw new java.io.IOException("Unreadable Birth picture");
        }
        return images;
    }

    static void showHistorical(ImageView image, String key) {
        Object token = new Object(); image.setTag(token); image.setImageDrawable(null);
        if (key == null || !key.startsWith("birth-approved:")) { show(image, key); return; }
        String[] parts = key.split(":", 3);
        if (parts.length != 3 || parts[1].isEmpty() || parts[1].contains("/")) return;
        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("quiz_questions")
                .document(parts[1]).get().addOnSuccessListener(doc -> {
                    if (image.getTag() != token) return;
                    BirthContentBank.Item item = BirthContentBank.parse(doc.getData());
                    if (item != null && BirthWordBank.KEYS[item.index].equals(parts[2]) && item.imageBytes != null)
                        LawImageCodec.display(image, item.imageBytes, 0);
                });
    }

    static void show(ImageView image, String key) {
        image.setImageBitmap(bundled(image.getResources(), key));
    }

    private static synchronized Bitmap bundled(android.content.res.Resources resources, String key) {
        int resource = resource(key);
        if (resource == 0) return null;
        Bitmap bitmap = CACHE.get(key);
        if (bitmap == null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(resources, resource, options);
            options.inSampleSize = 1;
            while (options.outWidth / options.inSampleSize > 512
                    && options.outHeight / options.inSampleSize > 512) options.inSampleSize *= 2;
            options.inJustDecodeBounds = false;
            bitmap = BitmapFactory.decodeResource(resources, resource, options);
            if (bitmap != null) CACHE.put(key, bitmap);
        }
        return bitmap;
    }
}
