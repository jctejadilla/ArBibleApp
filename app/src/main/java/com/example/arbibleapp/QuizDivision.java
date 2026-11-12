package com.example.arbibleapp;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
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

import android.content.ClipData;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

public class QuizDivision extends AppCompatActivity {
    private DivisionMatchingGame game;
    private java.util.List<DivisionContentBank.Pair> content = DivisionContentBank.defaults();
    private boolean contentReady, restoringContent;
    private java.io.File snapshotFile() {
        return new java.io.File(getFilesDir(), "division-attempt-" + attemptId + ".bin");
    }
    private void startContent(java.util.List<DivisionContentBank.Pair> bank, boolean restored) {
        if (isFinishing() || isDestroyed()) return;
        content = bank; contentReady = true; restoringContent = false;
        if (!restored) startedAt = System.currentTimeMillis();
        render();
    }
    private void persistContent(java.util.List<DivisionContentBank.Pair> bank) {
        LawImageCodec.WORKER.execute(() -> {
            try {
                try (java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(
                        new java.io.FileOutputStream(snapshotFile()))) { out.writeObject(new ArrayList<>(bank)); }
                runOnUiThread(() -> startContent(bank, false));
            } catch (Exception error) {
                runOnUiThread(() -> contentError("Could not save this attempt. Please reopen the quiz."));
            }
        });
    }
    private void contentError(String message) {
        if (!isFinishing() && !isDestroyed()) ((TextView)findViewById(R.id.tvDivisionRound)).setText(message);
    }
    @SuppressWarnings("unchecked")
    private void loadContent(Bundle state) {
        submit.setEnabled(false); next.setEnabled(false); reset.setEnabled(false);
        rows.removeAllViews(); tray.removeAllViews();
        ((TextView)findViewById(R.id.tvDivisionRound)).setText("Loading...");
        if (state != null && state.getBoolean("contentReady")) {
            restoringContent = true;
            LawImageCodec.WORKER.execute(() -> {
                try (java.io.ObjectInputStream in = new java.io.ObjectInputStream(new java.io.FileInputStream(snapshotFile()))) {
                    java.util.List<DivisionContentBank.Pair> bank = (java.util.List<DivisionContentBank.Pair>)in.readObject();
                    runOnUiThread(() -> startContent(bank, true));
                } catch (Exception error) {
                    runOnUiThread(() -> contentError("This attempt could not be restored. Please reopen the quiz."));
                }
            });
            return;
        }
        db.collection("quiz_questions").whereEqualTo("storyTitle", DivisionPairBank.STORY_TITLE)
                .whereEqualTo("approvedByAdmin", true).get().addOnSuccessListener(result -> {
                    if (!isFinishing() && !isDestroyed()) persistContent(DivisionContentBank.load(result.getDocuments()));
                }).addOnFailureListener(error -> {
                    if (isFinishing() || isDestroyed()) return;
                    android.widget.Toast.makeText(this, "Could not load edits. Using default pairs.", android.widget.Toast.LENGTH_LONG).show();
                    persistContent(DivisionContentBank.defaults());
                });
    }
    private long startedAt, completedAt;
    private String attemptId, studentUid;
    private boolean completedOnSunday, submitting, saveFailed;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private LinearLayout rows, tray;
    private Button submit, next, reset;
    private int cardSize;
    private boolean dragActive;
    private int scrollStep;
    private View dragSource;
    private ScrollView characterScroll;
    private DivisionDragLayout dragRoot;
    private final Runnable edgeScroll = new Runnable() {
        @Override public void run() {
            if (!dragActive || scrollStep == 0) return;
            characterScroll.scrollBy(0, scrollStep);
            characterScroll.postDelayed(this, 16);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_quiz_division);
        View root = findViewById(R.id.main);
        dragRoot = (DivisionDragLayout) root;
        characterScroll = findViewById(R.id.divisionScroll);
        dragRoot.observer = this::observeDrag;
        dragRoot.setOnDragListener((v, event) -> event.getLocalState() instanceof DragToken
                && event.getAction() != DragEvent.ACTION_DROP);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        game = state == null ? new DivisionMatchingGame(new Random().nextLong())
                : (DivisionMatchingGame) state.getSerializable("game");
        if (game == null) game = new DivisionMatchingGame(new Random().nextLong());
        startedAt = state == null ? System.currentTimeMillis() : state.getLong("startedAt");
        completedAt = state == null ? 0 : state.getLong("completedAt");
        completedOnSunday = state != null && state.getBoolean("completedOnSunday");
        attemptId = state == null ? UUID.randomUUID().toString() : state.getString("attemptId");
        studentUid = state == null
                ? (FirebaseAuth.getInstance().getCurrentUser() == null ? null
                    : FirebaseAuth.getInstance().getCurrentUser().getUid())
                : state.getString("studentUid");
        saveFailed = completedAt != 0;
        rows = findViewById(R.id.divisionRows);
        tray = findViewById(R.id.divisionTray);
        submit = findViewById(R.id.btnDivisionSubmit);
        next = findViewById(R.id.btnDivisionNext);
        reset = findViewById(R.id.btnDivisionReset);
        cardSize = dp(104);
        root.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            int available = r - l - v.getPaddingLeft() - v.getPaddingRight() - dp(48);
            int size = Math.min(dp(104), Math.max(1, available / 3));
            if (size != cardSize) { cardSize = size; render(); }
        });
        reset.setOnClickListener(v -> {
            if (!submitting && game.round().reset()) { game.selected = -1; render(); }
        });
        submit.setOnClickListener(v -> {
            if (!submitting && game.round().submit()) {
                game.selected = -1;
                render();
                rows.announceForAccessibility(game.round().score() + " of 3 correct matches");
            }
        });
        next.setOnClickListener(v -> {
            if (!contentReady || submitting) return;
            if (game.complete()) saveResult();
            else if (game.next()) {
                render();
                ((ScrollView) findViewById(R.id.divisionScroll)).fullScroll(View.FOCUS_UP);
            }
        });
        findViewById(R.id.btnDivisionBack).setOnClickListener(v -> confirmExit());
        findViewById(R.id.btnDivisionInstructions).setOnClickListener(v -> {
            Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.dialog_instructions);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
            
            TextView tvTitle = dialog.findViewById(R.id.tvInstructionTitle);
            TextView tvMessage = dialog.findViewById(R.id.tvInstructionMessage);
            
            tvTitle.setText("Divided Kingdom Quiz");
            tvMessage.setText("Match each character with their object. Press and move an object to drag it into a space, or tap an object and then a space. You can move objects between spaces or return them to the tray. Fill all three spaces, then Submit. Each correct pair earns one point and 10 XP on Sunday. Weekday trials are recorded with no XP. There is no time limit.");
            
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
            session.put("storyTitle", DivisionPairBank.STORY_TITLE);
            session.put("timestamp", Timestamp.now());
            session.put("attemptId", attemptId);
            db.collection("quiz_sessions").document(studentUid).set(session);
        }
    }

    private boolean editable() { return contentReady && !submitting && !game.round().submitted; }

    private void render() {
        if (!contentReady || dragActive) return;
        DivisionMatchingGame.Round round = game.round();
        ((TextView) findViewById(R.id.tvDivisionRound)).setText("Round " + (game.current + 1) + "/3");
        rows.removeAllViews();
        for (int position = 0; position < 3; position++) {
            final int target = position;
            int character = round.characters.get(position);
            int object = round.slots[position];
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER);
            row.setPadding(0, dp(4), 0, dp(4));
            rows.addView(row, new LinearLayout.LayoutParams(-1, -2));
            LinearLayout left = column();
            row.addView(left, new LinearLayout.LayoutParams(0, -2, 1));
            left.addView(configuredPicture(character, 0,
                    content.get(character).characterName, false, (int) (cardSize * 1.2)));
            left.addView(text(content.get(character).characterName, 14));
            TextView arrow = text("\u2192", 24);
            row.addView(arrow, new LinearLayout.LayoutParams(dp(32), -2));
            LinearLayout right = column();
            row.addView(right, new LinearLayout.LayoutParams(0, -2, 1));
            String label = object < 0 ? "Drop here" : content.get(object).objectName;
            CardView slot = configuredPicture(object, 1,
                    "Match for " + content.get(character).characterName + ": " + label, true, (int) (cardSize * 1.2));
            slot.setTag("slot_" + position);
            right.addView(slot);
            right.addView(text(label, 14));
            if (round.submitted) {
                boolean correct = character == object;
                slot.setCardBackgroundColor(Color.parseColor(correct ? "#DFF3E4" : "#FCE5E5"));
            } else {
                if (object == game.selected && object >= 0) slot.setCardBackgroundColor(Color.parseColor("#CDE8DC"));
                slot.setOnClickListener(v -> {
                    if (!editable()) return;
                    if (game.selected >= 0) {
                        game.round().assign(game.selected, target);
                        game.selected = -1;
                    } else if (game.round().slots[target] >= 0) {
                        game.selected = game.round().slots[target];
                    }
                    render();
                });
                if (object >= 0) draggable(slot, object);
                dropTarget(slot, target);
            }
        }
        tray.removeAllViews();
        for (int object : round.objects) {
            LinearLayout item = column();
            tray.addView(item, new LinearLayout.LayoutParams(0, -2, 1));
            boolean available = round.location(object) < 0;
            CardView card = configuredPicture(available ? object : -1, 1,
                    available ? "Select " + content.get(object).objectName : "Object placed", available, cardSize);
            item.addView(card);
            TextView label = text(available ? content.get(object).objectName : "Placed", 13);
            label.setMinLines(2);
            item.addView(label);
            if (available && editable()) {
                if (game.selected == object) card.setCardBackgroundColor(Color.parseColor("#CDE8DC"));
                card.setOnClickListener(v -> {
                    game.selected = game.selected == object ? -1 : object;
                    render();
                });
                draggable(card, object);
            } else if (!available) {
                card.setAlpha(0.35f);
                card.setOnClickListener(v -> returnSelected());
            }
        }
        dropTarget(tray, -1);
        tray.setOnClickListener(v -> returnSelected());
        tray.setContentDescription("Object tray. Tap to return selected object.");
        tray.setVisibility(round.submitted ? View.GONE : View.VISIBLE);
        findViewById(R.id.tvDivisionTray).setVisibility(round.submitted ? View.GONE : View.VISIBLE);
        reset.setVisibility(round.submitted ? View.GONE : View.VISIBLE);
        reset.setEnabled(editable());
        submit.setVisibility(round.submitted ? View.GONE : View.VISIBLE);
        submit.setEnabled(editable() && round.full());
        next.setVisibility(round.submitted ? View.VISIBLE : View.GONE);
        next.setEnabled(!submitting);
        next.setText(submitting ? "Saving..." : saveFailed ? "Retry Save" : game.complete() ? "Finish" : "Next Round");
        for (Button button : new Button[]{reset, submit, next}) button.setAlpha(button.isEnabled() ? 1f : 0.45f);
    }

    private void returnSelected() {
        if (editable() && game.selected >= 0) {
            game.round().remove(game.selected);
            game.selected = -1;
            render();
        }
    }

    private final class DragToken {
        final int object, round;
        DragToken(int object) { this.object = object; this.round = game.current; }
    }

    private boolean beginDrag(View view, int object) {
        if (!editable() || dragActive) return false;
        dragActive = true;
        dragSource = view;
        view.cancelLongPress();
        boolean started = view.startDragAndDrop(
                ClipData.newPlainText("object", DivisionPairBank.OBJECT_KEYS[object]),
                new View.DragShadowBuilder(view), new DragToken(object), 0);
        if (!started) finishDrag();
        else view.setAlpha(0.5f);
        return started;
    }

    private void draggable(View view, int object) {
        view.setOnLongClickListener(v -> beginDrag(v, object));
        view.setOnTouchListener(new View.OnTouchListener() {
            float downX, downY;
            boolean started;
            @Override public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        if (!editable()) return false;
                        downX = event.getX();
                        downY = event.getY();
                        started = false;
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        return false; // Keep native click and long-press handling.
                    case MotionEvent.ACTION_MOVE:
                        int slop = ViewConfiguration.get(v.getContext()).getScaledTouchSlop();
                        if (!started && !dragActive &&
                                Math.hypot(event.getX() - downX, event.getY() - downY) > slop) {
                            started = beginDrag(v, object);
                        }
                        return started || dragActive;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        if (started || dragActive) { v.setPressed(false); return true; }
                        return false;
                    default: return false;
                }
            }
        });
    }

    private void observeDrag(DragEvent event) {
        if (!(event.getLocalState() instanceof DragToken)) return;
        if (event.getAction() == DragEvent.ACTION_DRAG_ENDED) {
            // Dispatch END to all old targets before replacing any views.
            dragRoot.post(this::finishDrag);
            return;
        }
        if (event.getAction() != DragEvent.ACTION_DRAG_LOCATION || !dragActive) return;
        int[] rootLocation = new int[2], scrollLocation = new int[2];
        dragRoot.getLocationOnScreen(rootLocation);
        characterScroll.getLocationOnScreen(scrollLocation);
        float x = rootLocation[0] + event.getX() - scrollLocation[0];
        float y = rootLocation[1] + event.getY() - scrollLocation[1];
        int step = 0;
        if (x >= 0 && x <= characterScroll.getWidth() && y >= 0 && y <= characterScroll.getHeight()) {
            if (y < dp(48)) step = -dp(6);
            else if (y > characterScroll.getHeight() - dp(48)) step = dp(6);
        }
        if (step != scrollStep) {
            characterScroll.removeCallbacks(edgeScroll);
            scrollStep = step;
            if (step != 0) characterScroll.post(edgeScroll);
        }
    }

    private void finishDrag() {
        characterScroll.removeCallbacks(edgeScroll);
        scrollStep = 0;
        if (dragSource != null) {
            dragSource.setAlpha(1f);
            dragSource.setPressed(false);
            if (dragSource.getParent() != null)
                dragSource.getParent().requestDisallowInterceptTouchEvent(false);
        }
        dragSource = null;
        if (!dragActive) return;
        dragActive = false;
        if (!isDestroyed()) render();
    }

    private void dropTarget(View view, int target) {
        view.setOnDragListener((v, event) -> {
            if (!(event.getLocalState() instanceof DragToken)) return false;
            DragToken token = (DragToken) event.getLocalState();
            if (event.getAction() == DragEvent.ACTION_DRAG_ENDED) {
                v.setAlpha(1f);
                return true;
            }
            if (!editable() || token.round != game.current || !game.round().objects.contains(token.object)) return false;
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED: return true;
                case DragEvent.ACTION_DRAG_ENTERED:
                    v.setAlpha(0.65f);
                    return true;
                case DragEvent.ACTION_DRAG_EXITED: v.setAlpha(1f); return true;
                case DragEvent.ACTION_DROP:
                    if (target < 0) game.round().remove(token.object);
                    else game.round().assign(token.object, target);
                    game.selected = -1;
                    return true;
                default: return true;
            }
        });
    }

    private LinearLayout column() {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);
        return column;
    }
    private CardView configuredPicture(int index, int role, String description, boolean interactive, int size) {
        CardView card = picture(index < 0 ? 0 : DivisionImages.resource(index, role), description, interactive, size);
        if (index >= 0 && content.get(index).images[role] != null)
            DivisionImages.show((ImageView)card.getChildAt(0), content.get(index), role);
        return card;
    }
    private CardView picture(int resource, String description, boolean interactive, int size) {
        CardView card = new CardView(this);
        card.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        card.setRadius(dp(12));
        card.setCardElevation(dp(2));
        card.setCardBackgroundColor(Color.WHITE);
        card.setContentDescription(description);
        card.setFocusable(interactive);
        if (resource != 0) {
            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setPadding(dp(4), dp(4), dp(4), dp(4));
            image.setImageResource(resource);
            image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            card.addView(image, new FrameLayout.LayoutParams(-1, -1));
        } else {
            TextView placeholder = text("+", 28);
            placeholder.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            card.addView(placeholder, new FrameLayout.LayoutParams(-1, -1));
        }
        return card;
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
            intent.putExtra("TOTAL", DivisionMatchingGame.TOTAL);
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
        ArrayList<DivisionPairResult> pairs = new ArrayList<>();
        ArrayList<String> wrong = new ArrayList<>(), selected = new ArrayList<>(), correct = new ArrayList<>();
        for (int round = 0; round < DivisionMatchingGame.ROUNDS; round++) {
            for (int row = 0; row < DivisionMatchingGame.PAIRS; row++) {
                DivisionPairResult pair = DivisionPairResult.from(game, round, row, content);
                pairs.add(pair);
                if (!pair.correct) {
                    wrong.add("Round " + pair.round + ": " + pair.characterName);
                    selected.add(pair.selectedObjectName);
                    correct.add(pair.correctObjectName);
                }
            }
        }
        long seconds = Math.max(0, (completedAt - startedAt) / 1000);
        Map<String, Object> result = new HashMap<>();
        result.put("studentUid", studentUid);
        result.put("storyTitle", DivisionPairBank.STORY_TITLE);
        result.put("score", game.score());
        result.put("totalQuestions", DivisionMatchingGame.TOTAL);
        result.put("xp", game.xp(completedOnSunday));
        result.put("timestamp", new Timestamp(new Date(completedAt)));
        result.put("duration", String.format(Locale.getDefault(), "%dm %02ds", seconds / 60, seconds % 60));
        result.put("date", new SimpleDateFormat("MMM dd (EEE)", Locale.getDefault()).format(new Date(completedAt)));
        result.put("isSunday", completedOnSunday);
        result.put("matchedPairs", pairs);
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
        characterScroll.removeCallbacks(edgeScroll);
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
