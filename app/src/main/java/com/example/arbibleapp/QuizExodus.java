package com.example.arbibleapp;

import android.app.Dialog;
import android.graphics.Color;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class QuizExodus extends AppCompatActivity {

    private ImageView btnBack, ivMainScene, ivOption1, ivOption2;
    private CardView cardOption1, cardOption2;
    private ProgressBar quizProgress;
    private TextView tvTimer, tvQuestionCount, tvPrompt, tvOption1Caption, tvOption2Caption;
    private boolean quizReady = false;

    private List<SequenceStep> steps = new ArrayList<>();
    private List<String> wrongAnswers = new ArrayList<>();
    private List<String> correctAnswers = new ArrayList<>();
    private List<String> selectedAnswers = new ArrayList<>();
    private int currentStepIndex = 0;
    private int score = 0;
    private boolean isSubmittingResult = false;
    private long startTime;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private CountDownTimer questionTimer;
    private final long TIME_LIMIT = 15000;
    private long timeLeftInMillis = TIME_LIMIT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_quiz_exodus);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupSequence();
        cardOption1.setEnabled(false);
        cardOption2.setEnabled(false);
        findViewById(R.id.btnInstructions).setEnabled(false);
        tvTimer.setText("Loading...");
        loadApprovedOverrides();
    }

    private void loadApprovedOverrides() {
        db.collection("quiz_questions")
                .whereEqualTo("storyTitle", "Slavery in Egypt and the Exodus")
                .whereEqualTo("approvedByAdmin", true)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (isFinishing() || isDestroyed()) return;
                    Map<Integer, QuizCaptionOverride> edits =
                            QuizCaptionOverride.select(snapshots.getDocuments(), 4);
                    for (Map.Entry<Integer, QuizCaptionOverride> entry : edits.entrySet()) {
                        SequenceStep step = steps.get(entry.getKey());
                        QuizCaptionOverride edit = entry.getValue();
                        step.description = edit.question;
                        step.correctCaption = edit.optionA;
                        step.wrongCaption = edit.optionB;
                        step.mainBytes = ExodusImageDefaults.bytes(edit.image1);
                        step.correctBytes = ExodusImageDefaults.bytes(edit.image2);
                        step.wrongBytes = ExodusImageDefaults.bytes(edit.image3);
                    }
                    startLoadedQuiz();
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(this, "Could not load edits. Using default content.", Toast.LENGTH_SHORT).show();
                    startLoadedQuiz();
                });
    }

    private void startLoadedQuiz() {
        quizReady = true;
        cardOption1.setEnabled(true);
        cardOption2.setEnabled(true);
        findViewById(R.id.btnInstructions).setEnabled(true);
        startTime = System.currentTimeMillis();
        startQuizSession();
        updateUI();
    }

    private void setCaption(TextView view, String caption) {
        view.setText(caption);
        view.setVisibility(caption.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void startQuizSession() {
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            Map<String, Object> session = new HashMap<>();
            session.put("studentUid", uid);
            session.put("storyTitle", "Slavery in Egypt and the Exodus");
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
        ivMainScene = findViewById(R.id.ivMainScene);
        ivOption1 = findViewById(R.id.ivOption1);
        ivOption2 = findViewById(R.id.ivOption2);
        cardOption1 = findViewById(R.id.option1);
        cardOption2 = findViewById(R.id.option2);
        quizProgress = findViewById(R.id.quizProgress);
        tvTimer = findViewById(R.id.tvTimer);
        tvQuestionCount = findViewById(R.id.tvQuestionCount);
        tvPrompt = findViewById(R.id.tvPrompt);
        tvOption1Caption = findViewById(R.id.tvOption1Caption);
        tvOption2Caption = findViewById(R.id.tvOption2Caption);

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

        cardOption1.setOnClickListener(v -> handleChoice(0));
        cardOption2.setOnClickListener(v -> handleChoice(1));
        tvOption1Caption.setOnClickListener(v -> cardOption1.performClick());
        tvOption2Caption.setOnClickListener(v -> cardOption2.performClick());
    }

    private void setupSequence() {
        for (int step = 0; step < 5; step++) {
            SequenceStep scenario = new SequenceStep(ExodusImageDefaults.image(step, 0),
                    ExodusImageDefaults.image(step, 1), ExodusImageDefaults.image(step, 2), 0);
            scenario.description = ExodusImageDefaults.description(step);
            scenario.correctCaption = ExodusImageDefaults.caption(step, true);
            scenario.wrongCaption = ExodusImageDefaults.caption(step, false);
            steps.add(scenario);
        }

        quizProgress.setMax(steps.size());
    }

    private void updateUI() {
        if (currentStepIndex >= steps.size()) {
            finishQuiz();
            return;
        }

        tvQuestionCount.setText((currentStepIndex + 1) + "/" + steps.size());

        SequenceStep step = steps.get(currentStepIndex);
        LawImageCodec.display(ivMainScene, step.mainBytes, step.mainSceneRes);
        ((TextView) findViewById(R.id.tvMainSceneDescription)).setText(step.description);
        tvPrompt.setText("What happens next?");
        
        if (Math.random() > 0.5) {
            LawImageCodec.display(ivOption1, step.correctBytes, step.correctRes);
            LawImageCodec.display(ivOption2, step.wrongBytes, step.wrongRes);
            setCaption(tvOption1Caption, step.correctCaption);
            setCaption(tvOption2Caption, step.wrongCaption);
            step.correctOptionIndex = 0;
        } else {
            LawImageCodec.display(ivOption1, step.wrongBytes, step.wrongRes);
            LawImageCodec.display(ivOption2, step.correctBytes, step.correctRes);
            setCaption(tvOption1Caption, step.wrongCaption);
            setCaption(tvOption2Caption, step.correctCaption);
            step.correctOptionIndex = 1;
        }

        quizProgress.setProgress(currentStepIndex);
        cardOption1.setCardBackgroundColor(Color.WHITE);
        cardOption2.setCardBackgroundColor(Color.WHITE);
        
        startTimer();
    }

    private void pauseTimer() {
        if (questionTimer != null) questionTimer.cancel();
    }

    private void resumeTimer() {
        if (!quizReady || timeLeftInMillis <= 0) return;
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
                String timeLeftFormatted = String.format(Locale.getDefault(), "00:%02d", seconds);
                tvTimer.setText(timeLeftFormatted);
                if (seconds <= 5) {
                    tvTimer.setTextColor(Color.RED);
                } else {
                    tvTimer.setTextColor(Color.parseColor("#1D4A4B"));
                }
            }

            @Override
            public void onFinish() {
                timeLeftInMillis = 0;
                tvTimer.setText("00:00");
                Toast.makeText(QuizExodus.this, "Time's Up!", Toast.LENGTH_SHORT).show();
                currentStepIndex++;
                updateUI();
            }
        }.start();
    }

    private void handleChoice(int index) {
        if (!quizReady || currentStepIndex >= steps.size()) return;
        
        quizReady = false; // Prevent multiple clicks while showing feedback
        
        if (questionTimer != null) {
            questionTimer.cancel();
        }
        
        SequenceStep step = steps.get(currentStepIndex);
        boolean correct = index == step.correctOptionIndex;
        
        CardView selectedCard = (index == 0) ? cardOption1 : cardOption2;
        int colorRes = correct ? R.color.primary_green : R.color.red;
        selectedCard.setCardBackgroundColor(ContextCompat.getColor(this, colorRes));

        if (correct) {
            score++;
        } else {
            wrongAnswers.add("Step " + (currentStepIndex + 1) + " of the Exodus sequence");
            correctAnswers.add("Selected the wrong event in the timeline");
            selectedAnswers.add("Wrong choice made");
        }

        // Delay 1 second to show the color, then reset and move on
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            cardOption1.setCardBackgroundColor(Color.WHITE);
            cardOption2.setCardBackgroundColor(Color.WHITE);
            currentStepIndex++;
            updateUI();
        }, 1000);
    }

    private void finishQuiz() {
        if (questionTimer != null) {
            questionTimer.cancel();
        }
        saveResults();
    }

    private void showInstructionsDialog() {
        pauseTimer();
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_instructions);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        TextView tvTitle = dialog.findViewById(R.id.tvInstructionTitle);
        TextView tvMessage = dialog.findViewById(R.id.tvInstructionMessage);
        
        tvTitle.setText("Quiz 4 Instructions");
        tvMessage.setText("Review each story scene and tap the image choice that correctly continues the Exodus timeline!\n\nFollow Moses from Egypt across the Red Sea to the Promised Land!");
        
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
        int totalXP = score * 10; // 10 points each correct guess

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
            result.put("storyTitle", "Slavery in Egypt and the Exodus");
            result.put("score", score);
            result.put("xp", totalXP);
            result.put("totalQuestions", steps.size());
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
                        returnToDashboard(isSunday, score, steps.size(), totalXP, leveledUp, newLevel);
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
        intent.putExtra("TOTAL", total);
        intent.putExtra("XP", xp);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (ivMainScene != null) ivMainScene.setTag(null);
        if (ivOption1 != null) ivOption1.setTag(null);
        if (ivOption2 != null) ivOption2.setTag(null);
        super.onDestroy();
        if (questionTimer != null) {
            questionTimer.cancel();
        }
        if (!isChangingConfigurations()) {
            endQuizSession();
        }
    }

    private static class SequenceStep {
        String description = "", correctCaption = "", wrongCaption = "";
        byte[] mainBytes, correctBytes, wrongBytes;
        int mainSceneRes;
        int correctRes;
        int wrongRes;
        int correctOptionIndex;

        SequenceStep(int mainSceneRes, int correctRes, int wrongRes, int correctOptionIndex) {
            this.mainSceneRes = mainSceneRes;
            this.correctRes = correctRes;
            this.wrongRes = wrongRes;
            this.correctOptionIndex = correctOptionIndex;
        }
    }
}
