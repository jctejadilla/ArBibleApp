package com.example.arbibleapp;

import android.content.Context;
import android.content.res.Configuration;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Arrays;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class CreationMissedQuestionViewTest {
    private Context context(float fontScale) {
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Configuration config = new Configuration(target.getResources().getConfiguration());
        config.fontScale = fontScale;
        return new ContextThemeWrapper(target.createConfigurationContext(config), R.style.Theme_ArBibleApp);
    }

    private View card(Context context) {
        return LayoutInflater.from(context).inflate(R.layout.item_creation_missed_question, null);
    }

    private String text(View view, int id) {
        return ((TextView) view.findViewById(id)).getText().toString();
    }

    @Test
    public void incompleteHistoryPreservesPairsAndDistinguishesUnanswered() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            View view = card(context(1));
            TeacherQuizResults.QuizResultModel result = new TeacherQuizResults.QuizResultModel();
            result.wrongAnswers = Arrays.asList("First question", "Second question", "Third question");
            result.selectedAnswers = Arrays.asList("A saved selection", "No answer");
            result.correctAnswers = Arrays.asList("First correct answer", null, "Third correct answer");
            CreationMissedQuestionView.bind(view, result, 0, true);
            assertEquals("A saved selection", text(view, R.id.tvReviewSelected));
            assertEquals("First correct answer", text(view, R.id.tvReviewCorrect));
            CreationMissedQuestionView.bind(view, result, 1, true);
            assertEquals("No answer", text(view, R.id.tvReviewSelected));
            assertEquals("Not recorded", text(view, R.id.tvReviewCorrect));
            CreationMissedQuestionView.bind(view, result, 2, true);
            assertEquals("Not recorded", text(view, R.id.tvReviewSelected));
            assertEquals("Third correct answer", text(view, R.id.tvReviewCorrect));
        });
    }

    @Test
    public void rebindingResetsVisibilityAndDoesNotRetainOldAnswers() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            View view = card(context(1));
            TeacherQuizResults.QuizResultModel result = new TeacherQuizResults.QuizResultModel();
            result.wrongAnswers = Arrays.asList("Question");
            result.selectedAnswers = Arrays.asList("Saved answer");
            result.correctAnswers = Arrays.asList("Correct answer");
            CreationMissedQuestionView.bind(view, result, 0, true);
            assertEquals(View.VISIBLE, view.findViewById(R.id.layoutReviewAnswers).getVisibility());
            result.selectedAnswers = null;
            result.correctAnswers = null;
            CreationMissedQuestionView.bind(view, result, 0, false);
            assertEquals(View.GONE, view.findViewById(R.id.layoutReviewAnswers).getVisibility());
            assertEquals("Not recorded", text(view, R.id.tvReviewSelected));
            CreationMissedQuestionView.bind(view, result, 0, true);
            assertEquals("Not recorded", text(view, R.id.tvReviewCorrect));
        });
    }

    @Test
    public void longSavedTextWrapsOnSmallScreenAtLargeFontSize() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = context(1.5f);
            View view = card(context);
            TeacherQuizResults.QuizResultModel result = new TeacherQuizResults.QuizResultModel();
            String longText = "A long teacher-edited question or answer should remain fully readable. "
                    + "The review must display every word, even with larger text on a small screen.";
            result.wrongAnswers = Arrays.asList(longText);
            result.selectedAnswers = Arrays.asList(longText);
            result.correctAnswers = Arrays.asList(longText);
            CreationMissedQuestionView.bind(view, result, 0, true);
            int width = (int) (280 * context.getResources().getDisplayMetrics().density);
            view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            view.layout(0, 0, width, view.getMeasuredHeight());
            for (int id : new int[]{R.id.tvMissedQuestion, R.id.tvReviewSelected, R.id.tvReviewCorrect}) {
                TextView text = view.findViewById(id);
                assertTrue(text.getLineCount() > 1);
                assertEquals(text.length(), text.getLayout().getLineEnd(text.getLineCount() - 1));
                for (int line = 0; line < text.getLineCount(); line++)
                    assertEquals(0, text.getLayout().getEllipsisCount(line));
                assertTrue(text.getHeight() >= text.getLayout().getHeight()
                        + text.getCompoundPaddingTop() + text.getCompoundPaddingBottom());
            }
        });
    }
}