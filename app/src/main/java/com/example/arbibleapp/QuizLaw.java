package com.example.arbibleapp;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;
import com.example.arbibleapp.LawScenarioBank.Scenario;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class QuizLaw extends AppCompatActivity {

    private ImageView btnBack, ivScenario;
    private TextView tvScenarioDesc, tvTimer, tvScenarioCount;
    private CardView swipeCard;
    private ProgressBar quizProgress;

    private List<Scenario> scenarios = new ArrayList<>();
    private List<String> wrongAnswers = new ArrayList<>();
    private List<String> correctAnswers = new ArrayList<>();
    private List<String> selectedAnswers = new ArrayList<>();
    private int currentScenarioIndex = 0;
    private int score = 0;
    private boolean isSubmittingResult = false;
    private boolean quizReady = false, transitioning = false;
    private long startTime;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private CountDownTimer questionTimer;
    private final long TIME_LIMIT = 15000;
    private long timeLeftInMillis = TIME_LIMIT;

    private float dX, initialX;
    private final float SWIPE_THRESHOLD = 300f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_quiz_law);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        findViewById(R.id.btnInstructions).setEnabled(false);
        tvTimer.setText("Loading...");
        loadApprovedOverrides();
    }

    private void startQuizSession() {
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            Map<String, Object> session = new HashMap<>();
            session.put("studentUid", uid);
            session.put("storyTitle", "The Law and Wandering in the Desert");
            session.put("timestamp", Timestamp.now());

            db.collection("quiz_sessions").document(uid).set(session);
        }
    }

    private void endQuizSession() {
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            db.collection("quiz_sessions").document(uid).delete();
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        ivScenario = findViewById(R.id.ivScenario);
        tvScenarioCount = findViewById(R.id.tvScenarioCount);
        tvScenarioDesc = findViewById(R.id.tvScenarioDesc);
        tvTimer = findViewById(R.id.tvTimer);
        swipeCard = findViewById(R.id.swipeCard);
        quizProgress = findViewById(R.id.quizProgress);

        btnBack.setOnClickListener(v -> confirmExit());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                confirmExit();
            }
        });

        View btnInstructions = findViewById(R.id.btnInstructions);
        if (btnInstructions != null) {
            btnInstructions.setOnClickListener(v -> showInstructionsDialog());
        }
        setupSwipeListener();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupSwipeListener() {
        swipeCard.setOnTouchListener((v, event) -> {
            if (!quizReady || transitioning || isSubmittingResult) return true;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    dX = v.getX() - event.getRawX();
                    initialX = v.getX();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float newX = event.getRawX() + dX;
                    v.setX(newX);
                    float rotation = (newX - initialX) / 10f;
                    v.setRotation(rotation);
                    return true;
                case MotionEvent.ACTION_UP:
                    if (v.getX() > initialX + SWIPE_THRESHOLD) {
                        handleSwipe(true);
                    } else if (v.getX() < initialX - SWIPE_THRESHOLD) {
                        handleSwipe(false);
                    } else {
                        v.animate().x(initialX).rotation(0).setDuration(200).start();
                    }
                    return true;
            }
            return false;
        });
    }

    private void handleSwipe(boolean swipedRight) {
        if (!quizReady || transitioning || isSubmittingResult || currentScenarioIndex >= scenarios.size()) return;
        transitioning = true;
        if (questionTimer != null) questionTimer.cancel();
        
        Scenario current = scenarios.get(currentScenarioIndex);
        if (swipedRight == current.isGood) {
            score++;
            Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Incorrect!", Toast.LENGTH_SHORT).show();
            wrongAnswers.add(current.description);
            correctAnswers.add(current.isGood ? "Follows Law" : "Breaks Law");
            selectedAnswers.add(swipedRight ? "Follows Law" : "Breaks Law");
        }

        float targetX = swipedRight ? 2000f : -2000f;
        swipeCard.animate().x(targetX).setDuration(300).withEndAction(() -> {
            currentScenarioIndex++;
            swipeCard.setX(initialX);
            swipeCard.setRotation(0);
            updateUI();
        }).start();
    }

    private void loadApprovedOverrides() {
        db.collection("quiz_questions")
                .whereEqualTo("storyTitle", LawScenarioBank.STORY_TITLE)
                .whereEqualTo("approvedByAdmin", true)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (isFinishing() || isDestroyed()) return;
                    startLoadedQuiz(LawScenarioBank.load(snapshots.getDocuments()));
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(this, "Could not load edits. Using default scenarios.", Toast.LENGTH_SHORT).show();
                    startLoadedQuiz(LawScenarioBank.defaults());
                });
    }

    private void startLoadedQuiz(List<Scenario> bank) {
        scenarios = LawScenarioBank.selectQuiz(bank, new Random());
        quizProgress.setMax(scenarios.size());
        swipeCard.post(() -> {
            if (isFinishing() || isDestroyed()) return;
            initialX = swipeCard.getX();
            quizReady = true;
            findViewById(R.id.btnInstructions).setEnabled(true);
            startTime = System.currentTimeMillis();
            startQuizSession();
            updateUI();
        });
    }

    private void updateUI() {
        transitioning = false;
        if (currentScenarioIndex >= scenarios.size()) {
            finishQuiz();
            return;
        }

        Scenario current = scenarios.get(currentScenarioIndex);
        LawImageCodec.display(ivScenario, current.imageBytes, current.imageRes);
        tvScenarioDesc.setText(current.description);
        tvScenarioCount.setText(currentScenarioIndex + 1 + " / " + scenarios.size());
        quizProgress.setProgress(currentScenarioIndex);

        startTimer();
    }

    private void pauseTimer() {
        if (questionTimer != null) questionTimer.cancel();
    }

    private void resumeTimer() {
        if (!quizReady || transitioning || isSubmittingResult || timeLeftInMillis <= 0) return;
        startTimerWithDuration(timeLeftInMillis);
    }

    private void startTimer() {
        startTimerWithDuration(TIME_LIMIT);
    }

    private void startTimerWithDuration(long durationMs) {
        if (questionTimer != null) questionTimer.cancel();
        questionTimer = new CountDownTimer(durationMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                int seconds = (int) (millisUntilFinished / 1000);
                String time = String.format(Locale.getDefault(), "00:%02d", seconds);
                tvTimer.setText(time);
                if (seconds <= 5) tvTimer.setTextColor(Color.RED);
                else tvTimer.setTextColor(Color.parseColor("#1D4A4B"));
            }

            @Override
            public void onFinish() {
                if (!quizReady || transitioning || isSubmittingResult || currentScenarioIndex >= scenarios.size()) return;
                transitioning = true;
                Scenario current = scenarios.get(currentScenarioIndex);
                wrongAnswers.add(current.description);
                correctAnswers.add(current.isGood ? "Follows Law" : "Breaks Law");
                selectedAnswers.add("No answer (time expired)");
                timeLeftInMillis = 0;
                tvTimer.setText("00:00");

                Toast.makeText(QuizLaw.this, "You ran out of time", Toast.LENGTH_SHORT).show();

                float targetX = 0f;

                swipeCard.animate()
                        .x(targetX)
                        .rotation(0)
                        .setDuration(200)
                        .withEndAction(() -> {
                            currentScenarioIndex++;
                            swipeCard.setX(initialX);
                            swipeCard.setRotation(0);
                            updateUI();
                        })
                        .start();
            }
        }.start();
    }

    private void finishQuiz() {
        quizReady = false;
        if (questionTimer != null) questionTimer.cancel();
        saveResults();
    }

    private void showInstructionsDialog() {
        if (!quizReady || transitioning || isSubmittingResult) return;
        pauseTimer();
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_instructions);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        TextView tvTitle = dialog.findViewById(R.id.tvInstructionTitle);
        TextView tvMessage = dialog.findViewById(R.id.tvInstructionMessage);
        
        tvTitle.setText("Quiz 5 Instructions");
        tvMessage.setText("Read each scenario. Swipe right if the action Follows Law, or left if it Breaks Law.");
        
        dialog.findViewById(R.id.btnInstructionGotIt).setOnClickListener(v -> {
            dialog.dismiss();
            resumeTimer();
        });
        dialog.setOnCancelListener(d -> resumeTimer());
        
        dialog.show();
    }
    
    private void confirmExit() {
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

    private void saveResults() {
        if (isSubmittingResult) return;
        isSubmittingResult = true;

        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        int totalXP = score * 20;

        db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            String username = "Unknown";
            if (documentSnapshot.exists()) {
                username = documentSnapshot.getString("username");
                if (username == null || username.isEmpty()) {
                    username = documentSnapshot.getString("fullName");
                }
            }

            long endTime = System.currentTimeMillis();
            long durationMillis = endTime - startTime;
            int minutes = (int) (durationMillis / (1000 * 60));
            int seconds = (int) ((durationMillis / 1000) % 60);
            String durationString = String.format(Locale.getDefault(), "%dm %02ds", minutes, seconds);
            String dateString = new SimpleDateFormat("MMM dd (EEE)", Locale.getDefault()).format(new Date());

            Calendar cal = Calendar.getInstance();
            boolean isSunday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;

            Map<String, Object> result = new HashMap<>();
            result.put("studentUid", uid);
            result.put("username", username);
            result.put("storyTitle", "The Law and Wandering in the Desert");
            result.put("score", score);
            result.put("xp", totalXP);
            result.put("totalQuestions", scenarios.size());
            result.put("timestamp", Timestamp.now());
            result.put("duration", durationString);
            result.put("date", dateString);
            result.put("wrongAnswers", wrongAnswers);
            result.put("correctAnswers", correctAnswers);
            result.put("selectedAnswers", selectedAnswers);
            result.put("isSunday", isSunday);

            db.collection("quiz_results").add(result)
                    .addOnSuccessListener(dr -> {
                        boolean leveledUp = false;
                        int newLevel = 1;

                        if (isSunday && totalXP > 0) {
                            Long currentXpLong = (documentSnapshot.exists() && documentSnapshot.contains("totalXP")) ? documentSnapshot.getLong("totalXP") : null;
                            long currentXP = currentXpLong != null ? currentXpLong : 0;
                            int oldLevel = (int) (currentXP / 250) + 1;
                            long newXP = currentXP + totalXP;
                            newLevel = (int) (newXP / 250) + 1;
                            if (newLevel > 10) newLevel = 10;
                            leveledUp = newLevel > oldLevel;

                            Map<String, Object> updates = new HashMap<>();
                            updates.put("totalXP", newXP);
                            updates.put("level", newLevel);

                            db.collection("users").document(uid).update(updates);
                        }
                        endQuizSession();
                        returnToDashboard(isSunday, score, scenarios.size(), totalXP, leveledUp, newLevel);
                    });
        });
    }

    private void returnToDashboard(boolean isSunday, int score, int total, int xp, boolean leveledUp, int newLevel) {
        Intent intent = new Intent(this, StudentDashboard.class);
        intent.putExtra("SHOW_QUIZ_RESULT", true);
        intent.putExtra("IS_SUNDAY", isSunday);
        intent.putExtra("SCORE", score);
        intent.putExtra("TOTAL", total);
        intent.putExtra("XP", xp);
        intent.putExtra("LEVELED_UP", leveledUp);
        intent.putExtra("NEW_LEVEL", newLevel);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        quizReady = false;
        swipeCard.animate().cancel();
        ivScenario.setTag(null);
        if (questionTimer != null) questionTimer.cancel();
        if (!isChangingConfigurations()) {
            endQuizSession();
        }
    }

}
