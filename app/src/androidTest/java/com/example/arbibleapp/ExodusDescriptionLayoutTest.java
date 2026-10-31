package com.example.arbibleapp;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ExodusDescriptionLayoutTest {
    @Test
    public void descriptionsWrapWithoutTruncationOnSmallScreen() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = new ContextThemeWrapper(
                    InstrumentationRegistry.getInstrumentation().getTargetContext(), R.style.Theme_ArBibleApp);
            View root = LayoutInflater.from(context).inflate(R.layout.activity_quiz_exodus, null);
            TextView main = root.findViewById(R.id.tvMainSceneDescription);
            TextView first = root.findViewById(R.id.tvOption1Caption);
            TextView second = root.findViewById(R.id.tvOption2Caption);
            first.setVisibility(View.VISIBLE);
            second.setVisibility(View.VISIBLE);
            int width = (int) (320 * context.getResources().getDisplayMetrics().density);
            int height = (int) (640 * context.getResources().getDisplayMetrics().density);
            for (int step = 0; step < 5; step++) {
                main.setText(ExodusImageDefaults.description(step));
                first.setText(ExodusImageDefaults.caption(step, true));
                second.setText(ExodusImageDefaults.caption(step, false));
                root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
                root.layout(0, 0, width, height);
                for (TextView text : new TextView[]{main, first, second}) {
                    assertNotNull(text.getLayout());
                    int last = text.getLayout().getLineCount() - 1;
                    assertTrue(last >= 0);
                    assertEquals(text.length(), text.getLayout().getLineEnd(last));
                    for (int line = 0; line <= last; line++)
                        assertEquals(0, text.getLayout().getEllipsisCount(line));
                    assertTrue(text.getHeight() >= text.getLayout().getHeight()
                            + text.getCompoundPaddingTop() + text.getCompoundPaddingBottom());
                }
                assertEquals("What happens next?",
                        ((TextView) root.findViewById(R.id.tvPrompt)).getText().toString());
            }
        });
    }
}