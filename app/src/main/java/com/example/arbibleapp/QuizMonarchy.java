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
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
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

public class QuizMonarchy extends AppCompatActivity {
    private MonarchySequenceGame game;
    private java.util.List<MonarchyContentBank.Scenario> content = MonarchyContentBank.defaults();
    private boolean contentReady, restoringContent;
    private java.io.File snapshotFile() {
        return new java.io.File(getFilesDir(), "monarchy-attempt-" + attemptId + ".bin");
    }
    private void startContent(java.util.List<MonarchyContentBank.Scenario> bank, boolean restored) {
        if (isFinishing() || isDestroyed()) return;
        content = bank; contentReady = true; restoringContent = false;
        if (!restored) startedAt = System.currentTimeMillis();
        render();
    }
    private void persistContent(java.util.List<MonarchyContentBank.Scenario> bank) {
        LawImageCodec.WORKER.execute(() -> {
            try {
                try (java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(
                        new java.io.FileOutputStream(snapshotFile()))) {
                    out.writeObject(new ArrayList<>(bank));
                }
                runOnUiThread(() -> startContent(bank, false));
            } catch (Exception error) {
                runOnUiThread(() -> contentError("Could not save this attempt. Please reopen the quiz."));
            }
        });
    }
    private void contentError(String message) {
        if (!isFinishing() && !isDestroyed()) scenarioTitle.setText(message);
    }
    @SuppressWarnings("unchecked")
    private void loadContent(Bundle state) {
        submit.setEnabled(false); next.setEnabled(false); undo.setEnabled(false); reset.setEnabled(false);
        grid.removeAllViews(); scenarioTitle.setText("Loading...");
        if (state != null && state.getBoolean("contentReady")) {
            restoringContent = true;
            LawImageCodec.WORKER.execute(() -> {
                try (java.io.ObjectInputStream in = new java.io.ObjectInputStream(
                        new java.io.FileInputStream(snapshotFile()))) {
                    java.util.List<MonarchyContentBank.Scenario> bank =
                            (java.util.List<MonarchyContentBank.Scenario>) in.readObject();
                    runOnUiThread(() -> startContent(bank, true));
                } catch (Exception error) {
                    runOnUiThread(() -> contentError("This attempt could not be restored. Please reopen the quiz."));
                }
            });
            return;
        }
        db.collection("quiz_questions").whereEqualTo("storyTitle", MonarchyScenarioBank.STORY_TITLE)
                .whereEqualTo("approvedByAdmin", true).get().addOnSuccessListener(result -> {
                    if (!isFinishing() && !isDestroyed()) persistContent(MonarchyContentBank.load(result.getDocuments()));
                }).addOnFailureListener(error -> {
                    if (isFinishing() || isDestroyed()) return;
                    android.widget.Toast.makeText(this, "Could not load edits. Using default scenarios.", android.widget.Toast.LENGTH_LONG).show();
                    persistContent(MonarchyContentBank.defaults());
                });
    }
    private long startedAt, completedAt;
    private String attemptId, studentUid;
    private boolean completedOnSunday, submitting, saveFailed;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private TextView scenarioTitle, selectedCount;
    private LinearLayout grid;
    private Button submit, next, undo, reset;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_quiz_monarchy);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        game = state == null ? new MonarchySequenceGame(new Random().nextLong())
                : (MonarchySequenceGame) state.getSerializable("game");
        if (game == null) game = new MonarchySequenceGame(new Random().nextLong());
        startedAt = state == null ? System.currentTimeMillis() : state.getLong("startedAt");
        completedAt = state == null ? 0 : state.getLong("completedAt");
        completedOnSunday = state != null && state.getBoolean("completedOnSunday");
        attemptId = state == null ? UUID.randomUUID().toString() : state.getString("attemptId");
        studentUid = state == null
                ? (FirebaseAuth.getInstance().getCurrentUser() == null ? null
                    : FirebaseAuth.getInstance().getCurrentUser().getUid())
                : state.getString("studentUid");
        saveFailed = completedAt != 0;
        scenarioTitle = findViewById(R.id.tvMonarchyScenario);
        selectedCount = findViewById(R.id.tvMonarchySelected);
        grid = findViewById(R.id.monarchyGrid);
        submit = findViewById(R.id.btnMonarchySubmit);
        next = findViewById(R.id.btnMonarchyNext);
        undo = findViewById(R.id.btnMonarchyUndo);
        reset = findViewById(R.id.btnMonarchyReset);
        undo.setOnClickListener(v -> { if (!submitting && game.round().undo()) render(); });
        reset.setOnClickListener(v -> { if (!submitting && game.round().reset()) render(); });
        submit.setOnClickListener(v -> {
            if (!submitting && game.round().submit()) {
                render();
                grid.announceForAccessibility(getString(R.string.monarchy_round_score, game.round().score()));
            }
        });
        next.setOnClickListener(v -> {
            if (!contentReady || submitting) return;
            if (game.complete()) saveResult();
            else if (game.next()) {
                render();
                findViewById(R.id.main).post(() ->
                        ((ScrollView) findViewById(R.id.main)).fullScroll(View.FOCUS_UP));
            }
        });
        findViewById(R.id.btnMonarchyBack).setOnClickListener(v -> confirmExit());
        findViewById(R.id.btnMonarchyInstructions).setOnClickListener(v -> {
            Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.dialog_instructions);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
            
            TextView tvTitle = dialog.findViewById(R.id.tvInstructionTitle);
            TextView tvMessage = dialog.findViewById(R.id.tvInstructionMessage);
            
            tvTitle.setText(R.string.monarchy_instructions_title);
            tvMessage.setText(R.string.monarchy_instructions);
            
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
            session.put("storyTitle", MonarchyScenarioBank.STORY_TITLE);
            session.put("timestamp", Timestamp.now());
            session.put("attemptId", attemptId);
            db.collection("quiz_sessions").document(studentUid).set(session);
        }
    }

    private void render() {
        if (!contentReady) return;
        MonarchySequenceGame.Round round = game.round();
        scenarioTitle.setText(content.get(game.current).title);
        selectedCount.setText(getString(R.string.monarchy_selected, round.selected.size()));
        grid.removeAllViews();
        for (int rowIndex = 0; rowIndex < 2; rowIndex++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            grid.addView(row, new LinearLayout.LayoutParams(-1, -2));
            for (int column = 0; column < 3; column++) {
                int scene = round.grid.get(rowIndex * 3 + column);
                int order = round.selected.indexOf(scene);
                boolean chosen = order >= 0;
                boolean right = chosen && order == scene;
                CardView card = new CardView(this);
                card.setTag(scene);
                card.setRadius(dp(12));
                card.setCardElevation(dp(2));
                card.setCardBackgroundColor(Color.parseColor(round.submitted
                        ? (right ? "#DFF3E4" : "#FCE5E5") : chosen ? "#DDECE5" : "#FFFFFF"));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
                params.setMargins(dp(3), dp(4), dp(3), dp(4));
                row.addView(card, params);
                LinearLayout content = new LinearLayout(this);
                content.setOrientation(LinearLayout.VERTICAL);
                content.setPadding(dp(5), dp(6), dp(5), dp(8));
                card.addView(content);
                TextView badge = text(round.submitted ? (right ? "+" : "x")
                        : chosen ? String.valueOf(order + 1) : "—", 23);
                badge.setSingleLine(true);
                content.addView(badge, new LinearLayout.LayoutParams(-1, -2));
                ImageView image = new ImageView(this);
                image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                MonarchyContentBank.Scene configured = this.content.get(game.current).scenes.get(scene);
                if (configured.bytes == null) MonarchyImages.show(image, configured.key);
                else LawImageCodec.display(image, configured.bytes, 0);
                content.addView(image, new LinearLayout.LayoutParams(-1, dp(96)));
                String caption = configured.caption;
                TextView label = text(caption, 14);
                label.setLines(4);
                label.setEllipsize(android.text.TextUtils.TruncateAt.END);
                content.addView(label, new LinearLayout.LayoutParams(-1, -2));
                badge.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                card.setContentDescription(chosen
                        ? getString(round.submitted
                                ? (right ? R.string.monarchy_ordered_correct : R.string.monarchy_ordered_wrong)
                                : R.string.monarchy_ordered, order + 1, caption)
                        : getString(R.string.monarchy_unselected, caption));
                card.setFocusable(true);
                card.setOnClickListener(v -> {
                    if (!submitting && game.round().select(scene)) render();
                });
                card.setClickable(!submitting && !round.submitted && !chosen);
            }
        }
        boolean editable = !round.submitted && !submitting;
        undo.setEnabled(editable && !round.selected.isEmpty());
        reset.setEnabled(editable && !round.selected.isEmpty());
        findViewById(R.id.monarchyEditControls).setVisibility(round.submitted ? View.GONE : View.VISIBLE);
        submit.setVisibility(round.submitted ? View.GONE : View.VISIBLE);
        submit.setEnabled(editable && round.selected.size() == MonarchySequenceGame.SCENES);
        next.setVisibility(round.submitted ? View.VISIBLE : View.GONE);
        next.setEnabled(!submitting);
        for (Button button : new Button[]{undo, reset, submit, next}) {
            button.setAlpha(button.isEnabled() ? 1f : 0.45f);
        }
        next.setText(submitting ? R.string.monarchy_saving : saveFailed ? R.string.monarchy_retry
                : game.complete() ? R.string.monarchy_finish : R.string.monarchy_next);
    }

    private TextView text(String value, int size) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setGravity(Gravity.CENTER);
        text.setTextColor(Color.parseColor("#1D4A4B"));
        return text;
    }

    private void saveResult() {
        if (submitting || !game.complete()) return;
        if (completedAt == 0) {
            completedAt = System.currentTimeMillis();
            completedOnSunday = JudgesRewardPolicy.isSunday(completedAt, TimeZone.getDefault());
        }
        if (studentUid == null || FirebaseAuth.getInstance().getCurrentUser() == null
                || !studentUid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            showSaveMessage(R.string.monarchy_sign_in);
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
            intent.putExtra("TOTAL", MonarchySequenceGame.TOTAL);
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
            showSaveMessage(R.string.monarchy_save_error);
        });
    }

    private Map<String, Object> buildResult() {
        ArrayList<MonarchySequenceResult> sequences = new ArrayList<>();
        ArrayList<String> wrong = new ArrayList<>(), selected = new ArrayList<>(), correct = new ArrayList<>();
        for (int i = 0; i < MonarchySequenceGame.SCENARIOS; i++) {
            MonarchySequenceResult snapshot = MonarchySequenceResult.from(game, i, content);
            sequences.add(snapshot);
            if (snapshot.correctPositions < MonarchySequenceGame.SCENES) {
                wrong.add(snapshot.title);
                selected.add(MonarchySequenceResult.orderedText(snapshot.selectedCaptions));
                correct.add(MonarchySequenceResult.orderedText(snapshot.correctCaptions));
            }
        }
        long seconds = Math.max(0, (completedAt - startedAt) / 1000);
        Map<String, Object> result = new HashMap<>();
        result.put("studentUid", studentUid);
        result.put("storyTitle", MonarchyScenarioBank.STORY_TITLE);
        result.put("score", game.score());
        result.put("totalQuestions", MonarchySequenceGame.TOTAL);
        result.put("xp", game.xp(completedOnSunday));
        result.put("timestamp", new Timestamp(new Date(completedAt)));
        result.put("duration", String.format(Locale.getDefault(), "%dm %02ds", seconds / 60, seconds % 60));
        result.put("date", new SimpleDateFormat("MMM dd (EEE)", Locale.getDefault()).format(new Date(completedAt)));
        result.put("isSunday", completedOnSunday);
        result.put("sequences", sequences);
        result.put("wrongAnswers", wrong);
        result.put("selectedAnswers", selected);
        result.put("correctAnswers", correct);
        return result;
    }

    private void showSaveMessage(int message) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
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
