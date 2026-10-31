package com.example.arbibleapp;

import android.content.Context;
import android.content.res.Configuration;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Arrays;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class JudgesMissedQuestionViewTest {
    private Context context() {
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Configuration config = new Configuration(target.getResources().getConfiguration());
        config.fontScale = 1.5f;
        return new ContextThemeWrapper(target.createConfigurationContext(config), R.style.Theme_ArBibleApp);
    }

    @Test public void reviewUsesRecordedAnswersAndHidesImagesUntilRequested() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = context();
            View card = LayoutInflater.from(context).inflate(R.layout.item_creation_missed_question, null);
            TeacherQuizResults.QuizResultModel result = new TeacherQuizResults.QuizResultModel();
            result.wrongAnswers = Arrays.asList("Unscramble: JOHSUA");
            result.selectedAnswers = Arrays.asList("Samson");
            result.correctAnswers = Arrays.asList("Joshua");
            result.selectedImageKeys = Arrays.asList("samson");
            result.correctImageKeys = Arrays.asList("joshua");
            JudgesMissedQuestionView.bind(card, result, 0, false);
            assertEquals(View.GONE, card.findViewById(R.id.layoutReviewAnswers).getVisibility());
            assertEquals(View.GONE, card.findViewById(R.id.ivReviewCorrect).getVisibility());
            JudgesMissedQuestionView.bind(card, result, 0, true);
            assertEquals("Samson", ((TextView) card.findViewById(R.id.tvReviewSelected)).getText().toString());
            assertEquals("Joshua", ((TextView) card.findViewById(R.id.tvReviewCorrect)).getText().toString());
            assertEquals(View.VISIBLE, card.findViewById(R.id.ivReviewSelected).getVisibility());
            assertNotNull(((ImageView) card.findViewById(R.id.ivReviewCorrect)).getDrawable());
            result.selectedImageKeys = Arrays.asList("unknown");
            result.correctImageKeys = null;
            JudgesMissedQuestionView.bind(card, result, 0, true);
            assertEquals(View.GONE, card.findViewById(R.id.ivReviewSelected).getVisibility());
            assertEquals(View.GONE, card.findViewById(R.id.ivReviewCorrect).getVisibility());
            assertEquals("Joshua", ((TextView) card.findViewById(R.id.tvReviewCorrect)).getText().toString());
        });
    }

    @Test public void allChoiceImagesExistAndGameplayFitsNarrowLargeTextLayout() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = context();
            for (String key : JudgesQuizGame.KEYS) assertNotEquals(0, JudgesImages.resource(context, key));
            assertEquals(0, JudgesImages.resource(context, "unknown"));
            View layout = LayoutInflater.from(context).inflate(R.layout.activity_quiz_judges, null);
            TextView scrambled = layout.findViewById(R.id.tvJudgesScramble);
            scrambled.setText("TURMPET");
            int width = (int) (320 * context.getResources().getDisplayMetrics().density);
            layout.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            layout.layout(0, 0, width, layout.getMeasuredHeight());
            assertEquals(scrambled.length(),
                    scrambled.getLayout().getLineEnd(scrambled.getLineCount() - 1));
            for (int line = 0; line < scrambled.getLineCount(); line++)
                assertEquals(0, scrambled.getLayout().getEllipsisCount(line));
        });
    }
}
