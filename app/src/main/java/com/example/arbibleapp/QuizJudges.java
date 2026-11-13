package com.example.arbibleapp;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class QuizJudges extends AppCompatActivity {
    private JudgesQuizGame game;
    private List<JudgesContentBank.Item> content = JudgesContentBank.defaults();
    private boolean contentReady, restoringContent;
    private File snapshotFile() {
        return new File(getFilesDir(), "judges-attempt-" + attemptId + ".bin");
    }
    private void startContent(List<JudgesContentBank.Item> items, Bundle state) {
        if (isFinishing() || isDestroyed()) return;
        content = items;
        String[] words = new String[items.size()];
        for (int i = 0; i < words.length; i++) words[i] = items.get(i).word;
        game = new JudgesQuizGame(seed, words);
        if (state != null) {
            int[] saved = state.getIntArray("selections");
            if (saved != null && saved.length == game.selections.length)
                System.arraycopy(saved, 0, game.selections, 0, saved.length);
        } else startedAt = System.currentTimeMillis();
        restoringContent = false;
        contentReady = true;
        render();
    }
    private void persistContent(List<JudgesContentBank.Item> items) {
        LawImageCodec.WORKER.execute(() -> {
            try (ObjectOutputStream output = new ObjectOutputStream(
                    new FileOutputStream(snapshotFile()))) {
                output.writeObject(new ArrayList<>(items));
                output.flush();
                runOnUiThread(() -> startContent(items, null));
            } catch (IOException error) {
                runOnUiThread(() -> contentError("Could not save this attempt. Please reopen the quiz."));
            }
        });
    }
    private void contentError(String message) {
        if (isFinishing() || isDestroyed()) return;
        feedback.setText(message);
        next.setEnabled(false);
    }
    @SuppressWarnings("unchecked")
    private void loadContent(Bundle state) {
        next.setEnabled(false);
        choices.removeAllViews();
        scramble.setText("Loading...");
        if (state != null && state.getBoolean("contentReady")) {
            restoringContent = true;
            LawImageCodec.WORKER.execute(() -> {
                try (ObjectInputStream input = new ObjectInputStream(
                        new FileInputStream(snapshotFile()))) {
                    List<JudgesContentBank.Item> items =
                            (List<JudgesContentBank.Item>) input.readObject();
                    runOnUiThread(() -> startContent(items, state));
                } catch (Exception error) {
                    runOnUiThread(() -> contentError("This attempt could not be restored. Please reopen the quiz."));
                }
            });
            return;
        }
        db.collection("quiz_questions").whereEqualTo("storyTitle", JudgesQuizGame.STORY_TITLE)
                .whereEqualTo("approvedByAdmin", true).get()
                .addOnSuccessListener(result -> {
                    if (!isFinishing() && !isDestroyed()) persistContent(JudgesContentBank.load(result.getDocuments()));
                }).addOnFailureListener(error -> {
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(this, "Could not load edits. Using default content.", Toast.LENGTH_LONG).show();
                    persistContent(JudgesContentBank.defaults());
                });
    }
    private String label(int index) { return content.get(index).word; }
    private long seed, startedAt, completedAt;
    private int roundIndex;
    private String attemptId, studentUid;
    private boolean submitting, saveFailed, completedOnSunday;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private TextView scramble, feedback;
    private ProgressBar progress;
    private LinearLayout choices;
    private Button next;
    private TextView tvJudgesProgressText;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_quiz_judges);
        tvJudgesProgressText = findViewById(R.id.tvJudgesProgressText);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        seed = state == null ? new Random().nextLong() : state.getLong("seed");
        startedAt = state == null ? System.currentTimeMillis() : state.getLong("startedAt");
        completedAt = state == null ? 0 : state.getLong("completedAt");
        completedOnSunday = state != null && state.getBoolean("completedOnSunday",
                JudgesRewardPolicy.isSunday(completedAt, TimeZone.getDefault()));
        attemptId = state == null ? UUID.randomUUID().toString() : state.getString("attemptId");
        studentUid = state == null
                ? (FirebaseAuth.getInstance().getCurrentUser() == null ? null
                    : FirebaseAuth.getInstance().getCurrentUser().getUid())
                : state.getString("studentUid");
        game = new JudgesQuizGame(seed);
        if (state != null) {
            roundIndex = state.getInt("roundIndex");
            int[] saved = state.getIntArray("selections");
            if (saved != null && saved.length == game.selections.length)
                System.arraycopy(saved, 0, game.selections, 0, saved.length);
            saveFailed = completedAt != 0;
        }
        scramble = findViewById(R.id.tvJudgesScramble);
        feedback = findViewById(R.id.tvJudgesFeedback);
        progress = findViewById(R.id.judgesProgress);
        choices = findViewById(R.id.judgesChoices);
        next = findViewById(R.id.btnJudgesNext);
        findViewById(R.id.btnJudgesBack).setOnClickListener(v -> confirmExit());
        findViewById(R.id.btnJudgesInstructions).setOnClickListener(v -> {
            Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.dialog_instructions);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
            
            TextView tvTitle = dialog.findViewById(R.id.tvInstructionTitle);
            TextView tvMessage = dialog.findViewById(R.id.tvInstructionMessage);
            
            tvTitle.setText(R.string.judges_instructions_title);
            tvMessage.setText(R.string.judges_instructions);
            
            dialog.findViewById(R.id.btnInstructionGotIt).setOnClickListener(dv -> dialog.dismiss());
            dialog.show();
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { confirmExit(); }
        });
        next.setOnClickListener(v -> {
            if (!contentReady || submitting || game.selections[roundIndex] == -1) return;
            if (roundIndex == game.rounds.size() - 1) saveResult();
            else {
                roundIndex++;
                render();
                findViewById(R.id.judgesScroll).post(() ->
                        ((ScrollView) findViewById(R.id.judgesScroll)).fullScroll(View.FOCUS_UP));
            }
        });
        loadContent(state);
        if (state == null && studentUid != null) {
            Map<String, Object> session = new HashMap<>();
            session.put("studentUid", studentUid);
            session.put("storyTitle", JudgesQuizGame.STORY_TITLE);
            session.put("timestamp", Timestamp.now());
            session.put("attemptId", attemptId);
            db.collection("quiz_sessions").document(studentUid).set(session);
        }
    }

    private void render() {
        tvJudgesProgressText.setText((roundIndex + 1) + "/" + game.rounds.size());
        JudgesQuizGame.Round round = game.rounds.get(roundIndex);
        boolean answered = game.selections[roundIndex] != -1;
        scramble.setText(round.scrambled);
        scramble.setContentDescription(getString(R.string.judges_scrambled_description,
                round.scrambled.replace("", " ").trim()));
        boolean sunday = completedAt == 0
                ? JudgesRewardPolicy.isSunday(System.currentTimeMillis(), TimeZone.getDefault())
                : completedOnSunday;
        progress.setContentDescription(sunday
                ? getString(R.string.judges_progress, roundIndex + 1, game.rounds.size(),
                        JudgesRewardPolicy.xp(game.score(), true))
                : getString(R.string.judges_trial_progress, roundIndex + 1, game.rounds.size(),
                        game.score()));
        progress.setProgress(roundIndex + (answered ? 1 : 0));
        choices.removeAllViews();
        for (int rowIndex = 0; rowIndex < 2; rowIndex++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            choices.addView(row, new LinearLayout.LayoutParams(-1, -2));
            for (int column = 0; column < 2; column++) {
                int choice = round.choices.get(rowIndex * 2 + column);
                CardView card = new CardView(this);
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(0, -2, 1);
                cardParams.setMargins(dp(4), dp(4), dp(4), dp(4));
                row.addView(card, cardParams);
                card.setRadius(dp(16));
                card.setCardElevation(dp(2));
                boolean correct = answered && choice == round.answer;
                boolean wrong = answered && choice == game.selections[roundIndex] && !correct;
                card.setCardBackgroundColor(Color.parseColor(correct ? "#DFF3E4" : wrong ? "#FCE5E5" : "#FFFFFF"));
                LinearLayout content = new LinearLayout(this);
                content.setOrientation(LinearLayout.VERTICAL);
                content.setPadding(dp(8), dp(8), dp(8), dp(8));
                card.addView(content);
                ImageView image = new ImageView(this);
                image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                if (this.content.get(choice).imageBytes == null) JudgesImages.show(image, JudgesQuizGame.KEYS[choice]);
                else LawImageCodec.display(image, this.content.get(choice).imageBytes, 0);
                image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                content.addView(image, new LinearLayout.LayoutParams(-1, dp(144)));
                String status = correct ? getString(R.string.judges_correct_choice, label(choice))
                        : wrong ? getString(R.string.judges_wrong_choice, label(choice)) : "";
                if (!status.isEmpty()) {
                    TextView caption = new TextView(this);
                    caption.setText(status);
                    caption.setTextSize(16);
                    caption.setTextColor(Color.parseColor("#1D4A4B"));
                    caption.setGravity(Gravity.CENTER);
                    content.addView(caption, new LinearLayout.LayoutParams(-1, -2));
                }
                card.setContentDescription(status.isEmpty() ? this.content.get(choice).description : status);
                card.setFocusable(!answered);
                card.setClickable(!answered);
                if (!answered) card.setOnClickListener(v -> {
                    if (submitting || !game.select(roundIndex, choice)) return;
                    render();
                    feedback.announceForAccessibility(feedback.getText());
                });
            }
        }
        if (answered) {
            feedback.setText(getString(game.selections[roundIndex] == round.answer
                    ? R.string.judges_correct : R.string.judges_incorrect,
                    content.get(round.answer).word));
        } else feedback.setText("");
        next.setEnabled(answered && !submitting);
        next.setText(submitting ? R.string.judges_saving : saveFailed ? R.string.judges_retry
                : roundIndex == game.rounds.size() - 1 ? R.string.judges_finish : R.string.judges_next);
    }

    private void saveResult() {
        if (submitting || !game.complete()) return;
        if (completedAt == 0) {
            completedAt = System.currentTimeMillis();
            completedOnSunday = JudgesRewardPolicy.isSunday(completedAt, TimeZone.getDefault());
        }
        if (studentUid == null || FirebaseAuth.getInstance().getCurrentUser() == null
                || !studentUid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            feedback.setText(R.string.judges_sign_in);
            return;
        }
        submitting = true;
        render();
        DocumentReference resultRef = db.collection("quiz_results").document(attemptId);
        DocumentReference userRef = db.collection("users").document(studentUid);
        DocumentReference sessionRef = db.collection("quiz_sessions").document(studentUid);
        final Map<String, Object> result = buildResult();
        final int awardedXP = JudgesRewardPolicy.xp(game.score(), completedOnSunday);
        db.runTransaction(transaction -> {
            DocumentSnapshot existing = transaction.get(resultRef);
            if (existing.exists()) return existing.getData();
            DocumentSnapshot user = transaction.get(userRef);
            DocumentSnapshot session = transaction.get(sessionRef);
            String username = user.getString("username");
            if (username == null || username.trim().isEmpty()) username = user.getString("fullName");
            result.put("username", username == null ? "Unknown" : username);
            Long xpValue = user.getLong("totalXP");
            long oldXP = xpValue == null ? 0 : xpValue;
            long newXP = oldXP + awardedXP;
            int oldLevel = (int) Math.min(10, oldXP / 250 + 1);
            int newLevel = (int) Math.min(10, newXP / 250 + 1);
            result.put("leveledUp", completedOnSunday && newLevel > oldLevel);
            result.put("newLevel", newLevel);
            transaction.set(resultRef, result);
            if (completedOnSunday && awardedXP > 0) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("totalXP", newXP);
                updates.put("level", newLevel);
                transaction.set(userRef, updates, SetOptions.merge());
            }
            if (attemptId.equals(session.getString("attemptId"))) transaction.delete(sessionRef);
            return result;
        }).addOnSuccessListener(saved -> {
            if (isFinishing() || isDestroyed()) return;
            Intent intent = new Intent(this, StudentDashboard.class);
            intent.putExtra("SHOW_QUIZ_RESULT", true);
            intent.putExtra("IS_SUNDAY", Boolean.TRUE.equals(saved.get("isSunday")));
            intent.putExtra("SCORE", game.score());
            intent.putExtra("TOTAL", game.rounds.size());
            Object savedXP = saved.get("xp");
            intent.putExtra("XP", Boolean.TRUE.equals(saved.get("isSunday")) && savedXP instanceof Number
                    ? ((Number) savedXP).intValue() : 0);
            intent.putExtra("LEVELED_UP", Boolean.TRUE.equals(saved.get("leveledUp")));
            Object level = saved.get("newLevel");
            intent.putExtra("NEW_LEVEL", level instanceof Number ? ((Number) level).intValue() : 1);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        }).addOnFailureListener(error -> {
            if (isFinishing() || isDestroyed()) return;
            submitting = false;
            saveFailed = true;
            render();
            feedback.setText(R.string.judges_save_error);
        });
    }

    private Map<String, Object> buildResult() {
        ArrayList<String> wrong = new ArrayList<>(), selected = new ArrayList<>(), correct = new ArrayList<>();
        ArrayList<String> selectedKeys = new ArrayList<>(), correctKeys = new ArrayList<>();
        for (int i = 0; i < game.rounds.size(); i++) {
            JudgesQuizGame.Round round = game.rounds.get(i);
            if (game.selections[i] != round.answer) {
                wrong.add("Unscramble: " + round.scrambled);
                selected.add(label(game.selections[i]));
                correct.add(label(round.answer));
                selectedKeys.add(content.get(game.selections[i]).imageKey());
                correctKeys.add(content.get(round.answer).imageKey());
            }
        }
        long seconds = Math.max(0, (completedAt - startedAt) / 1000);
        Map<String, Object> result = new HashMap<>();
        result.put("studentUid", studentUid);
        result.put("storyTitle", JudgesQuizGame.STORY_TITLE);
        result.put("score", game.score());
        result.put("xp", JudgesRewardPolicy.xp(game.score(), completedOnSunday));
        result.put("totalQuestions", game.rounds.size());
        result.put("timestamp", new Timestamp(new Date(completedAt)));
        result.put("duration", String.format(Locale.getDefault(), "%dm %02ds", seconds / 60, seconds % 60));
        result.put("date", new SimpleDateFormat("MMM dd (EEE)", Locale.getDefault()).format(new Date(completedAt)));
        result.put("isSunday", completedOnSunday);
        result.put("wrongAnswers", wrong);
        result.put("selectedAnswers", selected);
        result.put("correctAnswers", correct);
        result.put("selectedImageKeys", selectedKeys);
        result.put("correctImageKeys", correctKeys);
        return result;
    }

    private void confirmExit() {
        if (submitting) return;
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_confirm_exit);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        dialog.findViewById(R.id.btnExitStay).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btnExitLeave).setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });
        
        dialog.show();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putBoolean("contentReady", contentReady || restoringContent);
        state.putLong("seed", seed);
        state.putLong("startedAt", startedAt);
        state.putLong("completedAt", completedAt);
        state.putBoolean("completedOnSunday", completedOnSunday);
        state.putString("attemptId", attemptId);
        state.putString("studentUid", studentUid);
        state.putInt("roundIndex", roundIndex);
        state.putIntArray("selections", game.selections);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) LawImageCodec.WORKER.execute(() -> snapshotFile().delete());
        if (isFinishing() && studentUid != null) {
            DocumentReference session = db.collection("quiz_sessions").document(studentUid);
            db.runTransaction(transaction -> {
                DocumentSnapshot current = transaction.get(session);
                if (attemptId.equals(current.getString("attemptId"))) transaction.delete(session);
                return null;
            });
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
