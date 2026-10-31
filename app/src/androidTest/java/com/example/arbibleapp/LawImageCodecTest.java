package com.example.arbibleapp;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.widget.ImageView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class LawImageCodecTest {
    @Test
    public void largeImageIsCompressedWithinByteAndDimensionLimits() throws Exception {
        Bitmap input = Bitmap.createBitmap(2400, 1600, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[2400 * 1600];
        Random random = new Random(42);
        for (int i = 0; i < pixels.length; i++) pixels[i] = 0xff000000 | random.nextInt(0x1000000);
        input.setPixels(pixels, 0, 2400, 0, 0, 2400, 1600);
        byte[] bytes = LawImageCodec.compress(input);
        assertTrue(bytes.length <= LawImageData.MAX_BYTES);
        Bitmap decoded = LawImageCodec.decode(bytes);
        assertNotNull(decoded);
        assertTrue(decoded.getWidth() <= 1024);
        assertTrue(decoded.getHeight() <= 1024);
        assertEquals(1.5, (double) decoded.getWidth() / decoded.getHeight(), 0.02);
        input.recycle();
        decoded.recycle();
    }

    @Test
    public void transparentPixelsBecomeWhite() throws Exception {
        Bitmap input = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
        input.eraseColor(Color.TRANSPARENT);
        Bitmap decoded = LawImageCodec.decode(LawImageCodec.compress(input));
        assertNotNull(decoded);
        int color = decoded.getPixel(16, 16);
        assertTrue(Color.red(color) > 245 && Color.green(color) > 245 && Color.blue(color) > 245);
        input.recycle();
        decoded.recycle();
    }

    @Test
    public void corruptAndOversizedImagesAreRejected() {
        assertNull(LawImageCodec.decode(new byte[]{1, 2, 3}));
        assertNull(LawImageCodec.decode(new byte[LawImageData.MAX_BYTES + 1]));
        Bitmap input = Bitmap.createBitmap(1025, 10, Bitmap.Config.ARGB_8888);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        input.compress(Bitmap.CompressFormat.JPEG, 80, bytes);
        assertNull(LawImageCodec.decode(bytes.toByteArray()));
        input.recycle();
    }

    @Test
    public void pickerPreparationReadsExifOrientation() throws Exception {
        android.content.Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File file = File.createTempFile("law-image-", ".jpg", context.getCacheDir());
        Bitmap input = Bitmap.createBitmap(80, 40, Bitmap.Config.ARGB_8888);
        input.eraseColor(Color.RED);
        try {
            try (FileOutputStream output = new FileOutputStream(file)) {
                input.compress(Bitmap.CompressFormat.JPEG, 90, output);
            }
            ExifInterface exif = new ExifInterface(file.getAbsolutePath());
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_ROTATE_90));
            exif.saveAttributes();
            Bitmap decoded = LawImageCodec.decode(LawImageCodec.prepare(context.getContentResolver(), Uri.fromFile(file)));
            assertNotNull(decoded);
            assertEquals(40, decoded.getWidth());
            assertEquals(80, decoded.getHeight());
            decoded.recycle();
        } finally {
            input.recycle();
            file.delete();
        }
    }

    @Test
    public void mirroredOrientationFlipsThePixels() {
        Bitmap input = Bitmap.createBitmap(2, 1, Bitmap.Config.ARGB_8888);
        input.setPixel(0, 0, Color.RED);
        input.setPixel(1, 0, Color.BLUE);
        Bitmap flipped = LawImageCodec.orient(input, ExifInterface.ORIENTATION_FLIP_HORIZONTAL);
        assertEquals(Color.BLUE, flipped.getPixel(0, 0));
        assertEquals(Color.RED, flipped.getPixel(1, 0));
        flipped.recycle();
        input.recycle();
    }

    @Test
    public void lateDecodeCannotReplaceANewerDefaultPreview() throws Exception {
        android.app.Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        android.content.Context context = instrumentation.getTargetContext();
        Bitmap input = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888);
        input.eraseColor(Color.RED);
        byte[] bytes = LawImageCodec.compress(input);
        input.recycle();
        ImageView[] image = new ImageView[1];
        Future<?>[] decode = new Future<?>[1];
        instrumentation.runOnMainSync(() -> {
            image[0] = new ImageView(context);
            decode[0] = LawImageCodec.display(image[0], bytes, R.drawable.ic_law);
            LawImageCodec.display(image[0], null, R.drawable.ic_law);
            image[0].setImageDrawable(new ColorDrawable(Color.GREEN));
        });
        // Wait for this exact decode to finish posting its result, then drain UI callbacks.
        decode[0].get(5, TimeUnit.SECONDS);
        instrumentation.waitForIdleSync();
        instrumentation.runOnMainSync(() ->
                assertTrue(image[0].getDrawable() instanceof ColorDrawable));
    }
}