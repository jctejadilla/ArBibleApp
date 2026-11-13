package com.example.arbibleapp;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.text.TextUtils;
import android.widget.LinearLayout;
import android.widget.TextView;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;

public class QuizReturn extends AppCompatActivity {
    private ReturnMatchingGame game;
    private java.util.List<ReturnContentBank.Item> content = ReturnContentBank.defaults();
    private android.graphics.Bitmap[] preparedImages;
    private boolean contentReady, restoringContent, foreground;
    private java.io.File snapshotFile() {
        return new java.io.File(getFilesDir(), "return-attempt-" + attemptId + ".bin");
    }
    private void startContent(java.util.List<ReturnContentBank.Item> bank,
                              android.graphics.Bitmap[] images, boolean restored) {
        if (isFinishing() || isDestroyed()) return;
        content = bank; preparedImages = images; contentReady = true; restoringContent = false;
        if (!restored) startedAt = System.currentTimeMillis();
        game.resolveMismatch(System.currentTimeMillis());
        feedback.setText(game.complete() ? R.string.return_complete : game.second != -1
                ? R.string.return_mismatch : game.first != -1 ? R.string.return_choose_second : R.string.return_prompt);
        if (foreground) { render(false); scheduleMismatch(); }
    }
    private void persistContent(java.util.List<ReturnContentBank.Item> bank) {
        LawImageCodec.WORKER.execute(() -> {
            try {
                android.graphics.Bitmap[] images = ReturnImages.prepare(this, bank);
                try (java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(
                        new java.io.FileOutputStream(snapshotFile()))) { out.writeObject(new ArrayList<>(bank)); }
                runOnUiThread(() -> startContent(bank, images, false));
            } catch (Exception | OutOfMemoryError error) {
                runOnUiThread(() -> contentError("Could not prepare this attempt. Please reopen the quiz."));
            }
        });
    }
    private void contentError(String message) {
        if (!isFinishing() && !isDestroyed()) feedback.setText(message);
    }
    @SuppressWarnings("unchecked")
    private void loadContent(Bundle state) {
        finishButton.setEnabled(false); finishButton.setVisibility(View.GONE);
        for (CardView card : cards) card.setClickable(false);
        feedback.setText("Loading pictures...");
        if (state != null && state.getBoolean("contentReady")) {
            restoringContent = true;
            LawImageCodec.WORKER.execute(() -> {
                try (java.io.ObjectInputStream in = new java.io.ObjectInputStream(new java.io.FileInputStream(snapshotFile()))) {
                    java.util.List<ReturnContentBank.Item> bank = (java.util.List<ReturnContentBank.Item>)in.readObject();
                    android.graphics.Bitmap[] images = ReturnImages.prepare(this, bank);
                    runOnUiThread(() -> startContent(bank, images, true));
                } catch (Exception | OutOfMemoryError error) {
                    runOnUiThread(() -> contentError("This attempt could not be restored. Please reopen the quiz."));
                }
            });
            return;
        }
        db.collection("quiz_questions").whereEqualTo("storyTitle", ReturnMatchingGame.STORY_TITLE)
                .whereEqualTo("approvedByAdmin", true).get().addOnSuccessListener(result -> {
                    if (!isFinishing() && !isDestroyed()) persistContent(ReturnContentBank.load(result.getDocuments()));
                }).addOnFailureListener(error -> {
                    if (isFinishing() || isDestroyed()) return;
                    android.widget.Toast.makeText(this, "Could not load edits. Using default pictures.", android.widget.Toast.LENGTH_LONG).show();
                    persistContent(ReturnContentBank.defaults());
                });
    }
    private long startedAt, completedAt;
    private String attemptId, studentUid;
    private boolean completedOnSunday, submitting, saveFailed;
    private int animations;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final CardView[] cards = new CardView[12];
    private final ImageView[] pictures = new ImageView[12];
    private final TextView[] captions = new TextView[12];
    private final TextView[] cardBacks = new TextView[12];
    private final boolean[] showing = new boolean[12];
    private TextView feedback;
    private Button finishButton;
    private final Runnable hideMismatch = () -> {
        if (!contentReady || !foreground) return;
        if (game.resolveMismatch(System.currentTimeMillis())) {
            feedback.setText(R.string.return_prompt);
            render(true);
        } else scheduleMismatch();
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_quiz_return);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        game = state == null ? new ReturnMatchingGame(new Random().nextLong())
                : (ReturnMatchingGame) state.getSerializable("game");
        if (game == null) game = new ReturnMatchingGame(new Random().nextLong());
        startedAt = state == null ? System.currentTimeMillis() : state.getLong("startedAt");
        completedAt = state == null ? 0 : state.getLong("completedAt");
        completedOnSunday = state != null && state.getBoolean("completedOnSunday");
        attemptId = state == null ? UUID.randomUUID().toString() : state.getString("attemptId");
        studentUid = state == null
                ? (FirebaseAuth.getInstance().getCurrentUser() == null ? null
                    : FirebaseAuth.getInstance().getCurrentUser().getUid())
                : state.getString("studentUid");
        saveFailed = completedAt != 0;
        feedback = findViewById(R.id.tvReturnFeedback);
        finishButton = findViewById(R.id.btnReturnFinish);
        buildBoard();
        if (game.complete()) feedback.setText(R.string.return_complete);
        else if (game.second != -1) feedback.setText(R.string.return_mismatch);
        else if (game.first != -1) feedback.setText(R.string.return_choose_second);
        findViewById(R.id.btnReturnBack).setOnClickListener(v -> confirmExit());
        findViewById(R.id.btnReturnInstructions).setOnClickListener(v -> {
            Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.dialog_instructions);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
            
            TextView tvTitle = dialog.findViewById(R.id.tvInstructionTitle);
            TextView tvMessage = dialog.findViewById(R.id.tvInstructionMessage);
            
            tvTitle.setText(R.string.return_instructions_title);
            tvMessage.setText(R.string.return_instructions);
            
            dialog.findViewById(R.id.btnInstructionGotIt).setOnClickListener(dv -> dialog.dismiss());
            dialog.show();
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { confirmExit(); }
        });
        finishButton.setOnClickListener(v -> saveResult());
        loadContent(state);
        if (state == null && studentUid != null) {
            Map<String, Object> session = new HashMap<>();
            session.put("studentUid", studentUid);
            session.put("storyTitle", ReturnMatchingGame.STORY_TITLE);
            session.put("timestamp", Timestamp.now());
            session.put("attemptId", attemptId);
            db.collection("quiz_sessions").document(studentUid).set(session);
        }
    }

    private void buildBoard() {
        LinearLayout board = findViewById(R.id.returnBoard);
        for (int rowIndex = 0; rowIndex < 4; rowIndex++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            board.addView(row, new LinearLayout.LayoutParams(-1, -2));
            for (int column = 0; column < 3; column++) {
                int index = rowIndex * 3 + column;
                CardView card = new CardView(this);
                cards[index] = card;
                card.setId(View.generateViewId());
                card.setRadius(dp(12));
                card.setCardElevation(dp(2));
                card.setCameraDistance(dp(8000));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
                params.setMargins(dp(4), dp(4), dp(4), dp(4));
                row.addView(card, params);
                LinearLayout content = new LinearLayout(this);
                content.setOrientation(LinearLayout.VERTICAL);
                content.setGravity(Gravity.CENTER);
                content.setPadding(dp(4), dp(8), dp(4), dp(8));
                card.addView(content);
                FrameLayout pictureArea = new FrameLayout(this);
                content.addView(pictureArea, new LinearLayout.LayoutParams(-1, dp(82)));
                pictures[index] = new ImageView(this);
                pictures[index].setScaleType(ImageView.ScaleType.FIT_CENTER);
                pictures[index].setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                pictureArea.addView(pictures[index], new FrameLayout.LayoutParams(-1, -1));
                cardBacks[index] = new TextView(this);
                cardBacks[index].setText("?");
                cardBacks[index].setTextSize(34);
                cardBacks[index].setTextColor(Color.parseColor("#1D4A4B"));
                cardBacks[index].setGravity(Gravity.CENTER);
                cardBacks[index].setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                pictureArea.addView(cardBacks[index], new FrameLayout.LayoutParams(-1, -1));
                captions[index] = new TextView(this);
                captions[index].setGravity(Gravity.CENTER);
                captions[index].setTextSize(13);
                captions[index].setLines(3);
                captions[index].setEllipsize(TextUtils.TruncateAt.END);
                captions[index].setTextColor(Color.parseColor("#1D4A4B"));
                captions[index].setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                content.addView(captions[index], new LinearLayout.LayoutParams(-1, -2));
                card.setOnClickListener(v -> select(index));
            }
        }
    }

    private void select(int index) {
        if (!contentReady || submitting || animations > 0) return;
        ReturnMatchingGame.Selection result = game.select(index, System.currentTimeMillis());
        if (result == ReturnMatchingGame.Selection.IGNORED) return;
        int message = result == ReturnMatchingGame.Selection.FIRST ? R.string.return_choose_second
                : result == ReturnMatchingGame.Selection.MISMATCH ? R.string.return_mismatch
                : game.complete() ? R.string.return_complete : R.string.return_match;
        feedback.setText(message);
        render(true);
        scheduleMismatch();
    }

    private void render(boolean animate) {
        if (!contentReady) return;
        for (int i = 0; i < cards.length; i++) {
            boolean face = game.faceUp(i);
            if (animate && showing[i] != face) {
                final int index = i;
                animations++;
                cards[i].animate().rotationY(90).setDuration(ReturnMatchingGame.FLIP_MS / 2)
                        .withEndAction(() -> {
                            bindCard(index);
                            cards[index].setRotationY(-90);
                            cards[index].animate().rotationY(0).setDuration(ReturnMatchingGame.FLIP_MS / 2)
                                    .withEndAction(() -> {
                                        animations--;
                                        updateInteraction();
                                    }).start();
                        }).start();
            } else {
                cards[i].setRotationY(0);
                bindCard(i);
            }
            showing[i] = face;
        }
        finishButton.setVisibility(game.complete() ? View.VISIBLE : View.GONE);
        finishButton.setText(submitting ? R.string.return_saving
                : saveFailed ? R.string.return_retry : R.string.return_finish);
        updateInteraction();
    }

    private void bindCard(int index) {
        boolean face = game.faceUp(index);
        boolean matched = game.matched[index];
        cards[index].setCardBackgroundColor(Color.parseColor(matched ? "#DFF3E4" : face ? "#FFFFFF" : "#C4DED1"));
        pictures[index].setVisibility(face ? View.VISIBLE : View.INVISIBLE);
        cardBacks[index].setVisibility(face ? View.INVISIBLE : View.VISIBLE);
        if (face) pictures[index].setImageBitmap(preparedImages[game.deck[index]]);
        else pictures[index].setImageDrawable(null);
        captions[index].setText(matched
                ? getString(R.string.return_matched_label, content.get(game.deck[index]).label)
                : "");
        cards[index].setContentDescription(face
                ? getString(matched ? R.string.return_matched_card : R.string.return_revealed_card,
                        index + 1, content.get(game.deck[index]).label)
                : getString(R.string.return_hidden_card, index + 1));
    }

    private void updateInteraction() {
        boolean ready = contentReady && !submitting && animations == 0 && game.second == -1;
        for (int i = 0; i < cards.length; i++) {
            cards[i].setClickable(ready && !game.matched[i] && i != game.first);
            cards[i].setFocusable(true);
        }
        finishButton.setEnabled(contentReady && game.complete() && !submitting && animations == 0);
    }

    private void scheduleMismatch() {
        handler.removeCallbacks(hideMismatch);
        if (contentReady && foreground && game.second != -1)
            handler.postDelayed(hideMismatch, Math.max(0, game.hideAt - System.currentTimeMillis()));
    }

    @Override protected void onStart() {
        super.onStart();
        foreground = true;
        if (game != null && contentReady) {
            if (game.resolveMismatch(System.currentTimeMillis())) feedback.setText(R.string.return_prompt);
            render(false); scheduleMismatch();
        }
    }

    @Override protected void onStop() {
        foreground = false;
        handler.removeCallbacks(hideMismatch);
        for (CardView card : cards) if (card != null) card.animate().cancel();
        animations = 0;
        super.onStop();
    }

    private void saveResult() {
        if (!contentReady || submitting || animations > 0 || !game.complete()) return;
        if (completedAt == 0) {
            completedAt = System.currentTimeMillis();
            completedOnSunday = JudgesRewardPolicy.isSunday(completedAt, TimeZone.getDefault());
        }
        if (studentUid == null || FirebaseAuth.getInstance().getCurrentUser() == null
                || !studentUid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            feedback.setText(R.string.return_sign_in);
            return;
        }
        submitting = true;
        render(false);
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
            Long value = user.getLong("totalXP");
            long oldXP = value == null ? 0 : value;
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
            intent.putExtra("TOTAL", ReturnMatchingGame.PAIRS);
            Object xp = saved.get("xp");
            intent.putExtra("XP", Boolean.TRUE.equals(saved.get("isSunday")) && xp instanceof Number
                    ? ((Number) xp).intValue() : 0);
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
            render(false);
            feedback.setText(R.string.return_save_error);
        });
    }

    private Map<String, Object> buildResult() {
        ArrayList<String> wrong = new ArrayList<>(), selected = new ArrayList<>(), correct = new ArrayList<>();
        ArrayList<String> selectedKeys = new ArrayList<>(), correctKeys = new ArrayList<>();
        for (ReturnMatchingGame.Miss miss : game.misses) {
            wrong.add("Move " + miss.move + ": Find the identical picture for "
                    + content.get(miss.firstKey).label);
            selected.add(content.get(miss.secondKey).label);
            correct.add(content.get(miss.firstKey).label);
            selectedKeys.add(content.get(miss.secondKey).imageKey());
            correctKeys.add(content.get(miss.firstKey).imageKey());
        }
        long seconds = Math.max(0, (completedAt - startedAt) / 1000);
        Map<String, Object> result = new HashMap<>();
        result.put("studentUid", studentUid);
        result.put("storyTitle", ReturnMatchingGame.STORY_TITLE);
        result.put("score", game.score());
        result.put("xp", JudgesRewardPolicy.xp(game.score(), completedOnSunday));
        result.put("totalQuestions", ReturnMatchingGame.PAIRS);
        result.put("moves", game.moves);
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
        state.putSerializable("game", game);
        state.putLong("startedAt", startedAt);
        state.putLong("completedAt", completedAt);
        state.putBoolean("completedOnSunday", completedOnSunday);
        state.putString("attemptId", attemptId);
        state.putString("studentUid", studentUid);
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
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
