package com.example.arbibleapp;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Blob;
import org.junit.Test;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class ExodusImagesTest {
    private QuizResetPolicy.Record edit(String id, long time) {
        Map<String, Object> data = new HashMap<>();
        data.put("storyTitle", QuizResetPolicy.story(4));
        data.put("quizType", 4);
        data.put("approvedByAdmin", true);
        data.put("operation", "EDIT");
        data.put("timestamp", new Timestamp(time, 0));
        data.put("itemIndex", 0);
        data.put("question", "What happens next?");
        data.put("options", Arrays.asList("Moses", "Noah"));
        return new QuizResetPolicy.Record(id, data);
    }

    private void upload(QuizResetPolicy.Record record, int slot, int value) {
        record.data.put("image" + slot + "Action", "REPLACE");
        record.data.put("image" + slot + "Bytes", Blob.fromBytes(new byte[]{(byte) value}));
    }

    @Test
    public void legacyTextEditKeepsAllThreeApprovedImages() {
        QuizResetPolicy.Record upload = edit("upload", 1);
        for (int i = 1; i <= 3; i++) upload(upload, i, i);
        QuizCaptionOverride result = QuizCaptionOverride.selectRecords(
                Arrays.asList(edit("legacy", 2), upload), 4).get(0);
        assertArrayEquals(new byte[]{1}, ExodusImageDefaults.bytes(result.image1));
        assertArrayEquals(new byte[]{2}, ExodusImageDefaults.bytes(result.image2));
        assertArrayEquals(new byte[]{3}, ExodusImageDefaults.bytes(result.image3));
    }

    @Test
    public void individualDefaultAndReplacementLeaveOtherRolesIntact() {
        QuizResetPolicy.Record first = edit("first", 1);
        for (int i = 1; i <= 3; i++) upload(first, i, i);
        QuizResetPolicy.Record second = edit("second", 2);
        second.data.put("image2Action", "DEFAULT");
        upload(second, 3, 8);
        QuizCaptionOverride result = QuizCaptionOverride.selectRecords(Arrays.asList(second, first), 4).get(0);
        assertArrayEquals(new byte[]{1}, ExodusImageDefaults.bytes(result.image1));
        assertNull(ExodusImageDefaults.bytes(result.image2));
        assertArrayEquals(new byte[]{8}, ExodusImageDefaults.bytes(result.image3));
    }

    @Test
    public void resetClearsAllImagesAndLaterKeepCannotResurrectThem() {
        QuizResetPolicy.Record first = edit("first", 1);
        for (int i = 1; i <= 3; i++) upload(first, i, i);
        QuizResetPolicy.Record reset = edit("reset", 2);
        reset.data.put("operation", "RESTORE_ALL");
        assertTrue(QuizCaptionOverride.selectRecords(Arrays.asList(first, reset), 4).isEmpty());
        QuizCaptionOverride result = QuizCaptionOverride.selectRecords(
                Arrays.asList(edit("after", 3), first, reset), 4).get(0);
        assertNull(ExodusImageDefaults.bytes(result.image1));
        assertNull(ExodusImageDefaults.bytes(result.image2));
        assertNull(ExodusImageDefaults.bytes(result.image3));
    }

    @Test
    public void unapprovedAndOtherQuizImagesDoNotApply() {
        QuizResetPolicy.Record pending = edit("pending", 2);
        upload(pending, 1, 4);
        pending.data.put("approvedByAdmin", false);
        QuizResetPolicy.Record babel = edit("babel", 3);
        upload(babel, 2, 5);
        babel.data.put("quizType", 3);
        babel.data.put("storyTitle", QuizResetPolicy.story(3));
        QuizCaptionOverride result = QuizCaptionOverride.selectRecords(
                Arrays.asList(edit("original", 1), pending, babel), 4).get(0);
        assertNull(ExodusImageDefaults.bytes(result.image1));
        assertNull(ExodusImageDefaults.bytes(result.image2));
    }
}