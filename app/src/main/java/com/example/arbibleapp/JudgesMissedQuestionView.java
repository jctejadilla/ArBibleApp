package com.example.arbibleapp;

import android.view.View;
import android.widget.ImageView;
import java.util.List;

final class JudgesMissedQuestionView {
    static void bind(View view, TeacherQuizResults.QuizResultModel result, int index, boolean showAnswers) {
        CreationMissedQuestionView.bind(view, result, index, showAnswers);
        bindImage(view.findViewById(R.id.ivReviewSelected), result.selectedImageKeys,
                result.selectedAnswers, index, showAnswers);
        bindImage(view.findViewById(R.id.ivReviewCorrect), result.correctImageKeys,
                result.correctAnswers, index, showAnswers);
    }

    private static void bindImage(ImageView image, List<String> keys, List<String> labels,
                                  int index, boolean visible) {
        String key = keys != null && index < keys.size() ? keys.get(index) : null;
        int resource = JudgesImages.resource(image.getContext(), key);
        image.setTag(null);
        boolean available = resource != 0 || (key != null && key.startsWith("judges-approved:"));
        image.setImageDrawable(null);
        image.setVisibility(visible && available ? View.VISIBLE : View.GONE);
        if (visible && available) JudgesImages.showHistorical(image, key);
        image.setContentDescription(labels != null && index < labels.size() ? labels.get(index) : null);
    }
}
