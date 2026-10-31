package com.example.arbibleapp;

import com.google.firebase.firestore.Blob;
import org.junit.Test;
import static org.junit.Assert.*;

public class BabelImageDefaultsTest {
    @Test
    public void everyOddPositionRetainsTheSameThreeFloorImages() {
        int[] odd = {R.drawable.odd1, R.drawable.odd2, R.drawable.odd3,
                R.drawable.odd4, R.drawable.odd5};
        for (int floor = 0; floor < 5; floor++) {
            int first = BabelImageDefaults.image(floor, 1, 0);
            int second = BabelImageDefaults.image(floor, 2, 0);
            assertNotEquals(first, second);
            for (int oddBlock = 0; oddBlock < 3; oddBlock++) {
                int tower = 0;
                for (int block = 0; block < 3; block++) {
                    int image = BabelImageDefaults.image(floor, block, oddBlock);
                    if (block == oddBlock) assertEquals(odd[floor], image);
                    else assertEquals(tower++ == 0 ? first : second, image);
                }
                assertEquals(2, tower);
            }
        }
    }

    @Test
    public void keepUsesApprovedUploadButDefaultClearsIt() {
        byte[] approved = {1, 2, 3};
        LawImageData current = LawImageData.read("REPLACE", Blob.fromBytes(approved));
        assertArrayEquals(approved, BabelImageDefaults.bytes(LawImageData.read(null, null), current));
        assertNull(BabelImageDefaults.bytes(LawImageData.read("DEFAULT", null), current));
        assertNull(BabelImageDefaults.bytes(LawImageData.read("KEEP", null), null));
    }

    @Test
    public void replacementUsesDraftAndInvalidDataDoesNotReuseCurrentUpload() {
        LawImageData current = LawImageData.read("REPLACE", Blob.fromBytes(new byte[]{1}));
        assertArrayEquals(new byte[]{2}, BabelImageDefaults.bytes(
                LawImageData.read("REPLACE", Blob.fromBytes(new byte[]{2})), current));
        assertNull(BabelImageDefaults.bytes(LawImageData.read("REPLACE", null), current));
    }
}