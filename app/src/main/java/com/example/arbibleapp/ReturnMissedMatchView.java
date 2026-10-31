package com.example.arbibleapp;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;

final class ReturnMissedMatchView {
    static void bind(View view, TeacherQuizResults.QuizResultModel result, int index, boolean showAnswers) {
        CreationMissedQuestionView.bind(view, result, index, showAnswers);
        ((TextView) view.findViewById(R.id.tvMissedQuestion)).setText(
                view.getContext().getString(R.string.return_review_miss, index + 1,
                        recorded(result.wrongAnswers, index)));
        bindImage(view.findViewById(R.id.ivReviewSelected), result.selectedImageKeys,
                result.selectedAnswers, index, showAnswers);
        bindImage(view.findViewById(R.id.ivReviewCorrect), result.correctImageKeys,
                result.correctAnswers, index, showAnswers);
    }

    private static String recorded(List<String> values, int index) {
        return values != null && index < values.size() && values.get(index) != null ? values.get(index) : "";
    }

    private static void bindImage(ImageView image, List<String> keys, List<String> labels,
                                  int index, boolean visible) {
        String key = recorded(keys, index);
        image.setTag(null);
        image.setImageDrawable(null);
        image.setVisibility(visible && (ReturnImages.resource(key) != 0 || key.startsWith("return-approved:")) ? View.VISIBLE : View.GONE);
        if (visible) ReturnImages.showHistorical(image, key);
        image.setContentDescription(recorded(labels, index));
    }
}
