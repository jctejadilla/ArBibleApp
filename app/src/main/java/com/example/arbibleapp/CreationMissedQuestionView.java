package com.example.arbibleapp;

import android.view.View;
import android.widget.TextView;
import java.util.List;
import java.util.Locale;

// Uses the saved attempt only; later question edits must not change historical feedback.
final class CreationMissedQuestionView {
    static void bind(View view, TeacherQuizResults.QuizResultModel result, int index, boolean showAnswers) {
        ((TextView) view.findViewById(R.id.tvMissedQuestion)).setText(
                String.format(Locale.getDefault(), "Missed question %d\n%s",
                        index + 1, recorded(result.wrongAnswers, index)));
        ((TextView) view.findViewById(R.id.tvReviewSelected)).setText(recorded(result.selectedAnswers, index));
        ((TextView) view.findViewById(R.id.tvReviewCorrect)).setText(recorded(result.correctAnswers, index));
        view.findViewById(R.id.layoutReviewAnswers).setVisibility(showAnswers ? View.VISIBLE : View.GONE);
    }

    private static String recorded(List<String> values, int index) {
        if (values == null || index < 0 || index >= values.size()) return "Not recorded";
        String value = values.get(index);
        return value == null || value.trim().isEmpty() ? "Not recorded" : value;
    }
}