package com.example.arbibleapp;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.Future;
import android.widget.ImageView;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class LawImageCodec {
    static final int MAX_DIMENSION = 1024;
    static final ExecutorService WORKER = Executors.newFixedThreadPool(2);

    static byte[] prepare(ContentResolver resolver, Uri uri) throws IOException {
        return prepare(resolver, uri, LawImageData.MAX_BYTES);
    }

    static byte[] prepare(ContentResolver resolver, Uri uri, int limit) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = resolver.openInputStream(uri)) {
            BitmapFactory.decodeStream(input, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new IOException("The selected file is not a readable image.");
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / options.inSampleSize > MAX_DIMENSION * 2) {
            options.inSampleSize *= 2;
        }
        Bitmap bitmap;
        try (InputStream input = resolver.openInputStream(uri)) {
            bitmap = BitmapFactory.decodeStream(input, null, options);
        }
        if (bitmap == null) throw new IOException("The selected image could not be decoded.");
        try {
            int orientation = ExifInterface.ORIENTATION_NORMAL;
            try (InputStream input = resolver.openInputStream(uri)) {
                if (input != null) orientation = new ExifInterface(input).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            } catch (IOException ignored) {
                // Some formats have no EXIF metadata.
            }
            Bitmap oriented = orient(bitmap, orientation);
            if (oriented != bitmap) bitmap.recycle();
            bitmap = oriented;
            return compress(bitmap, limit);
        } finally {
            bitmap.recycle();
        }
    }

    static Bitmap orient(Bitmap bitmap, int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL: matrix.setScale(-1, 1); break;
            case ExifInterface.ORIENTATION_ROTATE_180: matrix.setRotate(180); break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL: matrix.setScale(1, -1); break;
            case ExifInterface.ORIENTATION_TRANSPOSE: matrix.setRotate(90); matrix.postScale(-1, 1); break;
            case ExifInterface.ORIENTATION_ROTATE_90: matrix.setRotate(90); break;
            case ExifInterface.ORIENTATION_TRANSVERSE: matrix.setRotate(-90); matrix.postScale(-1, 1); break;
            case ExifInterface.ORIENTATION_ROTATE_270: matrix.setRotate(-90); break;
            default: return bitmap;
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    static byte[] compress(Bitmap source) throws IOException { return compress(source, LawImageData.MAX_BYTES); }

    static byte[] compress(Bitmap source, int limit) throws IOException {
        float scale = Math.min(1f, (float) MAX_DIMENSION / Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, Math.round(source.getWidth() * scale));
        int height = Math.max(1, Math.round(source.getHeight() * scale));
        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawColor(Color.WHITE);
        android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(source, null, new android.graphics.Rect(0, 0, width, height), paint);
        try {
            while (true) {
                for (int quality = 85; quality >= 45; quality -= 10) {
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    if (!image.compress(Bitmap.CompressFormat.JPEG, quality, output)) {
                        throw new IOException("Could not compress this image.");
                    }
                    byte[] bytes = output.toByteArray();
                    if (bytes.length <= limit) return bytes;
                }
                if (Math.max(image.getWidth(), image.getHeight()) <= 64) {
                    throw new IOException("This image cannot fit the upload limit.");
                }
                Bitmap smaller = Bitmap.createScaledBitmap(image,
                        Math.max(1, image.getWidth() * 3 / 4), Math.max(1, image.getHeight() * 3 / 4), true);
                image.recycle();
                image = smaller;
            }
        } finally {
            image.recycle();
        }
    }

    static Bitmap decode(byte[] bytes) {
        if (bytes == null || bytes.length == 0 || bytes.length > LawImageData.MAX_BYTES) return null;
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0
                || bounds.outWidth > MAX_DIMENSION || bounds.outHeight > MAX_DIMENSION) return null;
        try {
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (RuntimeException | OutOfMemoryError e) {
            return null;
        }
    }

    // Call on the UI thread. The token prevents recycled rows and late decodes showing the wrong image.
    static Future<?> display(ImageView view, byte[] bytes, int defaultImage) {
        Object token = new Object();
        view.setTag(token);
        view.setImageResource(defaultImage);
        if (bytes == null) return null;
        WeakReference<ImageView> target = new WeakReference<>(view);
        return WORKER.submit(() -> {
            Bitmap bitmap = decode(bytes);
            ImageView current = target.get();
            if (current == null) {
                if (bitmap != null) bitmap.recycle();
                return;
            }
            new Handler(Looper.getMainLooper()).post(() -> {
                ImageView destination = target.get();
                if (destination != null && destination.getTag() == token) {
                    if (bitmap != null) destination.setImageBitmap(bitmap);
                } else if (bitmap != null) bitmap.recycle();
            });
        });
    }
}