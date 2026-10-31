package com.example.arbibleapp;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

final class DivisionPairReview {
    static View create(Context context, DivisionPairResult result, boolean showCorrect) {
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(12, 12, 12, 12);
        addText(content, "Round " + result.round + ": " + result.characterName);
        addImage(content, result.characterName, DivisionPairBank.index(result.characterKey), 0, result.characterImageReference);
        addObject(content, "Your match: " + result.selectedObjectName, result.selectedObjectKey, result.selectedObjectImageReference);
        if (showCorrect) addObject(content, "Correct: " + result.correctObjectName, result.correctObjectKey, result.correctObjectImageReference);
        return content;
    }
    static void showForTeacher(Context context, TeacherQuizResults.QuizResultModel result) {
        android.widget.ScrollView scroll = new android.widget.ScrollView(context);
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content);
        boolean mistakes = false;
        if (result.matchedPairs != null) {
            for (DivisionPairResult pair : result.matchedPairs) if (pair != null && !pair.correct) {
                content.addView(create(context, pair, true));
                mistakes = true;
            }
        }
        if (!mistakes) addText(content, "All nine pairs matched correctly.");
        new androidx.appcompat.app.AlertDialog.Builder(context).setTitle("Matching results")
                .setView(scroll).setPositiveButton(android.R.string.ok, null).show();
    }
    private static void addText(LinearLayout content, String value) {
        TextView text = new TextView(content.getContext());
        text.setText(value);
        text.setTextColor(Color.parseColor("#1D4A4B"));
        text.setTextSize(15);
        content.addView(text);
    }
    private static void addObject(LinearLayout content, String label, String key, String reference) {
        addText(content, label);
        addImage(content, label, DivisionPairBank.objectIndex(key), 1, reference);
    }
    private static void addImage(LinearLayout content, String label, int index, int role, String reference) {
        if (index < 0) return;
        ImageView image = new ImageView(content.getContext());
        DivisionImages.historical(image, reference, index, role);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setContentDescription(label);
        int size = Math.round(72 * content.getResources().getDisplayMetrics().density);
        content.addView(image, new LinearLayout.LayoutParams(size, size));
    }
}