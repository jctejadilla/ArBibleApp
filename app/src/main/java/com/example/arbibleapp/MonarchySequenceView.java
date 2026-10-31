package com.example.arbibleapp;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

/** Used for immediate feedback and saved review; never reorders the student's original grid. */
final class MonarchySequenceView {
    static View review(Context context, MonarchySequenceResult result, boolean showAnswers) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(context, 8), dp(context, 12), dp(context, 8), dp(context, 12));
        card.addView(text(context, result.title == null ? context.getString(R.string.monarchy_title) : result.title, 18));
        card.addView(text(context, context.getString(R.string.monarchy_round_score,
                result.correctPositions), 16));
        if (showAnswers) fillOrders(card, result);
        return card;
    }

    static void fillOrders(LinearLayout container, MonarchySequenceResult result) {
        Context context = container.getContext();
        LinearLayout headers = row(context);
        headers.addView(text(context, context.getString(R.string.monarchy_your_order), 17), weighted());
        headers.addView(text(context, context.getString(R.string.monarchy_correct_order), 17), weighted());
        container.addView(headers);
        for (int i = 0; i < MonarchySequenceGame.SCENES; i++) {
            String selected = at(result.selectedKeys, i), correct = at(result.correctKeys, i);
            boolean right = !correct.isEmpty() && correct.equals(selected);
            LinearLayout row = row(context);
            row.addView(scene(context, selected, at(result.selectedCaptions, i), i,
                    right ? "✓" : "✗", right ? "#E1F2E5" : "#FBE6E6"), weighted());
            row.addView(scene(context, correct, at(result.correctCaptions, i), i, "✓", "#E1F2E5"), weighted());
            container.addView(row);
        }
    }

    private static View scene(Context context, String key, String caption, int position,
                              String marker, String background) {
        LinearLayout block = new LinearLayout(context);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(dp(context, 6), dp(context, 8), dp(context, 6), dp(context, 8));
        block.setBackgroundColor(Color.parseColor(background));
        block.addView(text(context, marker + " " + (position + 1), 18));
        ImageView image = new ImageView(context);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setContentDescription(caption);
        MonarchyImages.showHistorical(image, key);
        image.setVisibility(MonarchyImages.resource(key) != 0 || key.startsWith("monarchy-approved:") ? View.VISIBLE : View.GONE);
        block.addView(image, new LinearLayout.LayoutParams(-1, dp(context, 100)));
        block.addView(text(context, caption.isEmpty() ? context.getString(R.string.monarchy_not_recorded) : caption, 14));
        return block;
    }

    private static LinearLayout row(Context context) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        return row;
    }
    private static LinearLayout.LayoutParams weighted() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
        params.setMargins(3, 4, 3, 4);
        return params;
    }
    private static TextView text(Context context, String value, int size) {
        TextView text = new TextView(context);
        text.setText(value);
        text.setTextSize(size);
        text.setGravity(Gravity.CENTER);
        text.setTextColor(Color.parseColor("#1D4A4B"));
        return text;
    }
    private static String at(List<String> values, int index) {
        return values != null && index < values.size() && values.get(index) != null ? values.get(index) : "";
    }
    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
