package com.example.arbibleapp;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

final class BirthWordReview {
    static View create(Context context, BirthWordResult result, boolean showAnswers) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(12 * context.getResources().getDisplayMetrics().density);
        card.setPadding(padding, padding, padding, padding);
        TextView title = text(context, context.getString(R.string.birth_review_title));
        card.addView(title);
        ImageView image = new ImageView(context);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setContentDescription(context.getString(R.string.birth_picture));
        BirthImages.showHistorical(image, result.imageKey);
        image.setVisibility(BirthImages.resource(result.imageKey) == 0 && (result.imageKey == null || !result.imageKey.startsWith("birth-approved:")) ? View.GONE : View.VISIBLE);
        card.addView(image, new LinearLayout.LayoutParams(-1, padding * 12));
        if (showAnswers) {
            card.addView(text(context, context.getString(R.string.birth_your_answer, safe(result.selectedWord))));
            card.addView(text(context, context.getString(R.string.birth_correct_answer,
                    safe(result.expectedWord), safe(result.displayName))));
        }
        return card;
    }
    private static String safe(String value) { return value == null ? "Not recorded" : value; }
    private static TextView text(Context context, String value) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextColor(Color.parseColor("#1D4A4B"));
        view.setTextSize(16);
        return view;
    }
}
