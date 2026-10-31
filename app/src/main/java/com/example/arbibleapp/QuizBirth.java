package com.example.arbibleapp;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
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

public class QuizBirth extends AppCompatActivity {
    private BirthWordGame game;
    private java.util.List<BirthContentBank.Item> content = BirthContentBank.defaults();
    private android.graphics.Bitmap[] preparedImages;
    private boolean contentReady, restoringContent;
    private java.io.File snapshotFile() {
        return new java.io.File(getFilesDir(), "birth-attempt-" + attemptId + ".bin");
    }
    private void startContent(java.util.List<BirthContentBank.Item> bank,
                              android.graphics.Bitmap[] images, boolean restored) {
        if (isFinishing() || isDestroyed()) return;
        content = bank; preparedImages = images; contentReady = true; restoringContent = false;
        if (!restored) {
            String[] answers = new String[BirthWordGame.TOTAL];
            for (int i = 0; i < answers.length; i++) answers[i] = bank.get(i).answer;
            game = new BirthWordGame(new Random().nextLong(), answers);
            startedAt = System.currentTimeMillis();
        }
        render();
    }
    private void persistContent(java.util.List<BirthContentBank.Item> bank) {
        LawImageCodec.WORKER.execute(() -> {
            try {
                android.graphics.Bitmap[] images = BirthImages.prepare(this, bank);
                try (java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(
                        new java.io.FileOutputStream(snapshotFile()))) { out.writeObject(new ArrayList<>(bank)); }
                runOnUiThread(() -> startContent(bank, images, false));
            } catch (Exception | OutOfMemoryError error) {
                runOnUiThread(() -> contentError("Could not prepare this attempt. Please reopen the quiz."));
            }
        });
    }
    private void contentError(String message) {
        if (!isFinishing() && !isDestroyed()) Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
    @SuppressWarnings("unchecked")
    private void loadContent(Bundle state) {
        submit.setEnabled(false); next.setEnabled(false); clear.setEnabled(false);
        picture.setContentDescription("Loading pictures...");
        if (state != null && state.getBoolean("contentReady")) {
            restoringContent = true;
            LawImageCodec.WORKER.execute(() -> {
                try (java.io.ObjectInputStream in = new java.io.ObjectInputStream(new java.io.FileInputStream(snapshotFile()))) {
                    java.util.List<BirthContentBank.Item> bank = (java.util.List<BirthContentBank.Item>)in.readObject();
                    android.graphics.Bitmap[] images = BirthImages.prepare(this, bank);
                    runOnUiThread(() -> startContent(bank, images, true));
                } catch (Exception | OutOfMemoryError error) {
                    runOnUiThread(() -> contentError("This attempt could not be restored. Please reopen the quiz."));
                }
            });
            return;
        }
        db.collection("quiz_questions").whereEqualTo("storyTitle", BirthWordBank.STORY_TITLE)
                .whereEqualTo("approvedByAdmin", true).get().addOnSuccessListener(result -> {
                    if (!isFinishing() && !isDestroyed()) persistContent(BirthContentBank.load(result.getDocuments()));
                }).addOnFailureListener(error -> {
                    if (isFinishing() || isDestroyed()) return;
                    android.widget.Toast.makeText(this, "Could not load edits. Using default pictures.", android.widget.Toast.LENGTH_LONG).show();
                    persistContent(BirthContentBank.defaults());
                });
    }

    private long startedAt, completedAt;
    private String attemptId, studentUid;
    private boolean completedOnSunday, submitting, saveFailed;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private LinearLayout slots, tiles;
    private ImageView picture;
    private Button submit, next, clear;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_quiz_birth);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        game = state == null ? new BirthWordGame(new Random().nextLong())
                : (BirthWordGame) state.getSerializable("game");
        if (game == null) game = new BirthWordGame(new Random().nextLong());
        startedAt = state == null ? System.currentTimeMillis() : state.getLong("startedAt");
        completedAt = state == null ? 0 : state.getLong("completedAt");
        completedOnSunday = state != null && state.getBoolean("completedOnSunday");
        attemptId = state == null ? UUID.randomUUID().toString() : state.getString("attemptId");
        studentUid = state == null
                ? (FirebaseAuth.getInstance().getCurrentUser() == null ? null
                    : FirebaseAuth.getInstance().getCurrentUser().getUid())
                : state.getString("studentUid");
        picture = findViewById(R.id.ivBirthPicture);
        slots = findViewById(R.id.birthSlots);
        tiles = findViewById(R.id.birthTiles);
        submit = findViewById(R.id.btnBirthSubmit);
        next = findViewById(R.id.btnBirthNext);
        clear = findViewById(R.id.btnBirthClear);
        clear.setOnClickListener(v -> {
            if (contentReady && !submitting) { game.round().clear(); render(); }
        });
        submit.setOnClickListener(v -> {
            if (contentReady && !submitting && game.round().submit()) {
                render();
                
                // Announce whether the user was right or wrong for accessibility users
                if (game.round().correct()) {
                    picture.announceForAccessibility("Correct! " + game.round().answer);
                } else {
                    picture.announceForAccessibility("Incorrect.");
                }
                
                findViewById(R.id.scrollView).post(() ->
                        ((ScrollView) findViewById(R.id.scrollView)).fullScroll(View.FOCUS_DOWN));
            }
        });
        next.setOnClickListener(v -> {
            if (!contentReady || submitting) return;
            if (game.complete()) saveResult();
            else if (game.next()) {
                render();
                findViewById(R.id.scrollView).post(() ->
                        ((ScrollView) findViewById(R.id.scrollView)).fullScroll(View.FOCUS_UP));
            }
        });
        findViewById(R.id.btnBirthBack).setOnClickListener(v -> confirmExit());
        findViewById(R.id.btnBirthInstructions).setOnClickListener(v -> {
            Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.dialog_instructions);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
            
            TextView tvTitle = dialog.findViewById(R.id.tvInstructionTitle);
            TextView tvMessage = dialog.findViewById(R.id.tvInstructionMessage);
            
            tvTitle.setText(R.string.birth_title);
            tvMessage.setText(R.string.birth_instructions);
            
            dialog.findViewById(R.id.btnInstructionGotIt).setOnClickListener(dv -> dialog.dismiss());
            dialog.show();
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { confirmExit(); }
        });
        loadContent(state);
        if (state == null && studentUid != null) {
            Map<String, Object> session = new HashMap<>();
            session.put("studentUid", studentUid);
            session.put("storyTitle", BirthWordBank.STORY_TITLE);
            session.put("timestamp", Timestamp.now());
            session.put("attemptId", attemptId);
            db.collection("quiz_sessions").document(studentUid).set(session);
        }
    }

    private void render() {
        if (!contentReady) return;
        BirthWordGame.Round round = game.round();
        picture.setImageBitmap(preparedImages[game.contentIndices[game.current]]);
        picture.setContentDescription(getString(R.string.birth_picture));
        slots.removeAllViews();
        String entered = round.entered();
        for (int i = 0; i < round.answer.length(); i++) {
            final int position = i;
            Button slot = letter(String.valueOf(entered.charAt(i)));
            slot.setTag(i);
            
            int bgColor = Color.WHITE;
            int textColor = Color.parseColor("#1D4A4B");
            
            if (round.submitted) {
                if (round.correct()) {
                    bgColor = ContextCompat.getColor(this, R.color.primary_green);
                    textColor = Color.WHITE;
                } else {
                    bgColor = ContextCompat.getColor(this, R.color.red);
                    textColor = Color.WHITE;
                }
            } else if (round.hints[i]) {
                bgColor = Color.parseColor("#DDECE5");
            }
            
            slot.setBackgroundTintList(ColorStateList.valueOf(bgColor));
            slot.setTextColor(textColor);
            
            slot.setContentDescription(getString(round.hints[i] ? R.string.birth_hint_slot
                    : R.string.birth_answer_slot, i + 1, String.valueOf(entered.charAt(i))));
            slot.setOnClickListener(v -> {
                if (contentReady && !submitting && game.round().remove(position)) render();
            });
            slot.setEnabled(!submitting && !round.submitted && !round.hints[i] && round.slots[i] >= 0);
            slots.addView(slot, weighted());
        }
        tiles.removeAllViews();
        for (int r = 0; r < 2; r++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            tiles.addView(row, new LinearLayout.LayoutParams(-1, -2));
            for (int c = 0; c < 5; c++) {
                final int tile = r * 5 + c;
                Button button = letter(String.valueOf(round.tiles.get(tile)));
                button.setTag(tile);
                button.setContentDescription(getString(R.string.birth_tile, tile + 1,
                        String.valueOf(round.tiles.get(tile))));
                boolean used = round.used(tile);
                button.setAlpha(used ? 0.35f : 1f);
                button.setEnabled(!submitting && !round.submitted && !used);
                button.setOnClickListener(v -> {
                    if (contentReady && !submitting && game.round().select(tile)) render();
                });
                row.addView(button, weighted());
            }
        }
        clear.setVisibility(round.submitted ? View.GONE : View.VISIBLE);
        boolean hasLetters = false;
        for (int slot : round.slots) if (slot >= 0) hasLetters = true;
        clear.setEnabled(!submitting && !round.submitted && hasLetters);
        submit.setVisibility(round.submitted ? View.GONE : View.VISIBLE);
        submit.setEnabled(!submitting && !round.submitted && round.full());
        next.setVisibility(round.submitted ? View.VISIBLE : View.GONE);
        next.setEnabled(!submitting);
        next.setText(submitting ? R.string.birth_saving : saveFailed ? R.string.birth_retry
                : game.complete() ? R.string.birth_finish : R.string.birth_next);
    }

    private LinearLayout.LayoutParams weighted() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
        params.setMargins(dp(2), dp(3), dp(2), dp(3));
        return params;
    }

    private Button letter(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(22);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(dp(48));
        button.setMinimumHeight(dp(48));
        button.setPadding(0, dp(4), 0, dp(4));
        button.setTextColor(Color.parseColor("#1D4A4B"));
        button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#DDECE5")));
        return button;
    }

    private void saveResult() {
        if (!contentReady || submitting || !game.complete()) return;
        if (completedAt == 0) {
            completedAt = System.currentTimeMillis();
            completedOnSunday = JudgesRewardPolicy.isSunday(completedAt, TimeZone.getDefault());
        }
        if (studentUid == null || FirebaseAuth.getInstance().getCurrentUser() == null
                || !studentUid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            Toast.makeText(this, R.string.birth_sign_in, Toast.LENGTH_SHORT).show();
            return;
        }
        submitting = true;
        render();
        DocumentReference resultRef = db.collection("quiz_results").document(attemptId);
        DocumentReference userRef = db.collection("users").document(studentUid);
        DocumentReference sessionRef = db.collection("quiz_sessions").document(studentUid);
        final Map<String, Object> result = buildResult();
        final int awardedXP = game.xp(completedOnSunday);
        db.runTransaction(transaction -> {
            DocumentSnapshot existing = transaction.get(resultRef);
            if (existing.exists()) return existing.getData();
            DocumentSnapshot user = transaction.get(userRef);
            DocumentSnapshot session = transaction.get(sessionRef);
            String username = user.getString("username");
            if (username == null || username.trim().isEmpty()) username = user.getString("fullName");
            result.put("username", username == null ? "Unknown" : username);
            Long xp = user.getLong("totalXP");
            long oldXP = xp == null ? 0 : xp;
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
            intent.putExtra("TOTAL", BirthWordGame.TOTAL);
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
            render();
            Toast.makeText(this, R.string.birth_save_error, Toast.LENGTH_SHORT).show();
        });
    }

    private Map<String, Object> buildResult() {
        ArrayList<BirthWordResult> words = new ArrayList<>();
        ArrayList<String> wrong = new ArrayList<>(), selected = new ArrayList<>(), correct = new ArrayList<>();
        ArrayList<String> imageKeys = new ArrayList<>();
        for (int i = 0; i < BirthWordGame.TOTAL; i++) {
            BirthWordResult snapshot = BirthWordResult.from(game, i, content.get(game.contentIndices[i]));
            words.add(snapshot);
            if (!snapshot.correct) {
                wrong.add("Picture " + (i + 1));
                selected.add(snapshot.selectedWord);
                correct.add(snapshot.expectedWord + " (" + snapshot.displayName + ")");
                imageKeys.add(snapshot.imageKey);
            }
        }
        long seconds = Math.max(0, (completedAt - startedAt) / 1000);
        Map<String, Object> result = new HashMap<>();
        result.put("studentUid", studentUid);
        result.put("storyTitle", BirthWordBank.STORY_TITLE);
        result.put("score", game.score());
        result.put("totalQuestions", BirthWordGame.TOTAL);
        result.put("xp", game.xp(completedOnSunday));
        result.put("timestamp", new Timestamp(new Date(completedAt)));
        result.put("duration", String.format(Locale.getDefault(), "%dm %02ds", seconds / 60, seconds % 60));
        result.put("date", new SimpleDateFormat("MMM dd (EEE)", Locale.getDefault()).format(new Date(completedAt)));
        result.put("isSunday", completedOnSunday);
        result.put("wordAnswers", words);
        result.put("correctImageKeys", imageKeys);
        result.put("wrongAnswers", wrong);
        result.put("selectedAnswers", selected);
        result.put("correctAnswers", correct);
        return result;
    }

    private void confirmExit() {
        if (submitting) return;
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_confirm_exit);
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.findViewById(R.id.btnExitStay).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btnExitLeave).setOnClickListener(v -> { dialog.dismiss(); finish(); });
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
