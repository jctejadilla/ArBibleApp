package com.example.arbibleapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;
import android.widget.ImageView;

final class ReturnImages {
    private static final int[] RESOURCES = {R.drawable.return_temple, R.drawable.return_scroll,
            R.drawable.return_gate, R.drawable.return_families, R.drawable.return_ezra,
            R.drawable.return_nehemiah};
    private static final LruCache<String, Bitmap> CACHE = new LruCache<>(6);

    static int resource(String key) {
        for (int i = 0; i < ReturnMatchingGame.KEYS.length; i++)
            if (ReturnMatchingGame.KEYS[i].equals(key)) return RESOURCES[i];
        return 0;
    }

    static Bitmap[] prepare(android.content.Context context, java.util.List<ReturnContentBank.Item> bank)
            throws java.io.IOException {
        Bitmap[] images = new Bitmap[ReturnMatchingGame.PAIRS];
        for (int i = 0; i < images.length; i++) {
            ReturnContentBank.Item item = bank.get(i);
            images[i] = item.imageBytes == null ? bundled(context.getResources(), ReturnMatchingGame.KEYS[i])
                    : LawImageCodec.decode(item.imageBytes);
            if (images[i] == null) throw new java.io.IOException("Unreadable Return picture");
        }
        return images;
    }

    static void showHistorical(ImageView image, String key) {
        Object token = new Object(); image.setTag(token); image.setImageDrawable(null);
        if (key == null || !key.startsWith("return-approved:")) { show(image, key); return; }
        String[] parts = key.split(":", 3);
        if (parts.length != 3 || parts[1].isEmpty() || parts[1].contains("/")) return;
        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("quiz_questions")
                .document(parts[1]).get().addOnSuccessListener(doc -> {
                    if (image.getTag() != token) return;
                    ReturnContentBank.Item item = ReturnContentBank.parse(doc.getData());
                    if (item != null && ReturnMatchingGame.KEYS[item.index].equals(parts[2]) && item.imageBytes != null)
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
