package com.example.arbibleapp;

import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class QuizExodus extends AppCompatActivity {

    private ImageView btnBack, ivMainScene, ivOption1, ivOption2;
    private CardView cardOption1, cardOption2;
    private ProgressBar quizProgress;
    private TextView tvTimer, tvQuestionCount;

    private List<SequenceStep> steps = new ArrayList<>();
    private int currentStepIndex = 0;
    private int score = 0;
    private long startTime;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private CountDownTimer questionTimer;
    private final long TIME_LIMIT = 15000;

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
        updateUI();
        startTime = System.currentTimeMillis();
        startQuizSession();
    }

    private void startQuizSession() {
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            Map<String, Object> session = new HashMap<>();
            session.put("studentUid", uid);
            session.put("storyTitle", "Slavery in Egypt and the Exodus");
            session.put("timestamp", com.google.firebase.Timestamp.now());

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

        btnBack.setOnClickListener(v -> finish());

        cardOption1.setOnClickListener(v -> handleChoice(0));
        cardOption2.setOnClickListener(v -> handleChoice(1));
    }

    private void setupSequence() {
        // 1. Slavery -> Baby Moses
        steps.add(new SequenceStep(
                R.drawable.ic_slavery,
                R.drawable.ic_birth,
                R.drawable.ic_adam,
                0
        ));

        // 2. Moses grown up -> Burning Bush / Confront Pharaoh
        steps.add(new SequenceStep(
                R.drawable.ic_birth,
                R.drawable.ic_conflict,
                R.drawable.ic_noah,
                0
        ));

        // 3. Pharaoh refuses -> Plagues
        steps.add(new SequenceStep(
                R.drawable.ic_conflict,
                R.drawable.ic_land, // Placeholder for exodus start
                R.drawable.ic_babel,
                0
        ));

        // 4. Red Sea Crossing -> Wilderness
        steps.add(new SequenceStep(
                R.drawable.ic_land,
                R.drawable.ic_law, // Placeholder for Sinai
                R.drawable.ic_gold,
                0
        ));

        // 5. Sinai -> Promised Land
        steps.add(new SequenceStep(
                R.drawable.ic_law,
                R.drawable.ic_land, // Correct: Entering land
                R.drawable.ic_monarchy,
                0
        ));

        quizProgress.setMax(steps.size());
    }

    private void updateUI() {
        if (currentStepIndex >= steps.size()) {
            finishQuiz();
            return;
        }

        tvQuestionCount.setText((currentStepIndex + 1) + "/" + steps.size());

        SequenceStep step = steps.get(currentStepIndex);
        ivMainScene.setImageResource(step.mainSceneRes);
        
        if (Math.random() > 0.5) {
            ivOption1.setImageResource(step.correctRes);
            ivOption2.setImageResource(step.wrongRes);
            step.correctOptionIndex = 0;
        } else {
            ivOption1.setImageResource(step.wrongRes);
            ivOption2.setImageResource(step.correctRes);
            step.correctOptionIndex = 1;
        }

        quizProgress.setProgress(currentStepIndex);
        cardOption1.setCardBackgroundColor(Color.WHITE);
        cardOption2.setCardBackgroundColor(Color.WHITE);
        
        startTimer();
    }

    private void startTimer() {
        if (questionTimer != null) {
            questionTimer.cancel();
        }
        
        questionTimer = new CountDownTimer(TIME_LIMIT, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000);
                String timeLeftFormatted = String.format(java.util.Locale.getDefault(), "00:%02d", seconds);
                tvTimer.setText(timeLeftFormatted);
                if (seconds <= 5) {
                    tvTimer.setTextColor(Color.RED);
                } else {
                    tvTimer.setTextColor(Color.parseColor("#1D4A4B"));
                }
            }

            @Override
            public void onFinish() {
                tvTimer.setText("00:00");
                Toast.makeText(QuizExodus.this, "Time's Up!", Toast.LENGTH_SHORT).show();
                currentStepIndex++;
                updateUI();
            }
        }.start();
    }

    private void handleChoice(int index) {
        if (questionTimer != null) {
            questionTimer.cancel();
        }
        
        SequenceStep step = steps.get(currentStepIndex);
        if (index == step.correctOptionIndex) {
            score++;
            Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Incorrect!", Toast.LENGTH_SHORT).show();
        }

        currentStepIndex++;
        updateUI();
    }

    private void finishQuiz() {
        if (questionTimer != null) {
            questionTimer.cancel();
        }
        saveResults();
    }

    private void saveResults() {
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
            String durationString = String.format(java.util.Locale.getDefault(), "%dm %02ds", minutes, seconds);
            String dateString = new SimpleDateFormat("MMM dd (EEE)", Locale.getDefault()).format(new Date());

            Map<String, Object> result = new HashMap<>();
            result.put("studentUid", uid);
            result.put("username", username);
            result.put("storyTitle", "Slavery in Egypt and the Exodus");
            result.put("score", score);
            result.put("xp", totalXP);
            result.put("totalQuestions", steps.size());
            result.put("timestamp", com.google.firebase.Timestamp.now());
            result.put("duration", durationString);
            result.put("date", dateString);

            db.collection("quiz_results").add(result)
                    .addOnSuccessListener(dr -> {
                        db.collection("users").document(uid).update("totalXP", FieldValue.increment(totalXP));
                        endQuizSession();
                        Toast.makeText(this, "Sequence Complete! Gained " + totalXP + " XP", Toast.LENGTH_LONG).show();
                        finish();
                    });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (questionTimer != null) {
            questionTimer.cancel();
        }
        if (!isChangingConfigurations()) {
            endQuizSession();
        }
    }

    private static class SequenceStep {
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
