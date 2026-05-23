package com.example.arbibleapp;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.MotionEvent;
import android.view.View;
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
import java.util.Collections;
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
    private int currentScenarioIndex = 0;
    private int score = 0;
    private long startTime;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private CountDownTimer questionTimer;
    private final long TIME_LIMIT = 15000;

    private float dX, initialX;
    private final float SWIPE_THRESHOLD = 300f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        setupScenarios();
        updateUI();
        startTime = System.currentTimeMillis();
        startQuizSession();
    }

    private void startQuizSession() {
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            Map<String, Object> session = new HashMap<>();
            session.put("studentUid", uid);
            session.put("storyTitle", "The Law and Wandering in the Desert");
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
        ivScenario = findViewById(R.id.ivScenario);
        tvScenarioCount = findViewById(R.id.tvScenarioCount);
        tvScenarioDesc = findViewById(R.id.tvScenarioDesc);
        tvTimer = findViewById(R.id.tvTimer);
        swipeCard = findViewById(R.id.swipeCard);
        quizProgress = findViewById(R.id.quizProgress);

        btnBack.setOnClickListener(v -> finish());
        setupSwipeListener();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupSwipeListener() {
        swipeCard.setOnTouchListener((v, event) -> {
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
        if (questionTimer != null) questionTimer.cancel();
        
        Scenario current = scenarios.get(currentScenarioIndex);
        if (swipedRight == current.isGood) {
            score++;
            Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Incorrect!", Toast.LENGTH_SHORT).show();
        }

        float targetX = swipedRight ? 2000f : -2000f;
        swipeCard.animate().x(targetX).setDuration(300).withEndAction(() -> {
            currentScenarioIndex++;
            swipeCard.setX(initialX);
            swipeCard.setRotation(0);
            updateUI();
        }).start();
    }

    private void setupScenarios() {
        List<Scenario> scenarioBank = new ArrayList<>();

        scenarioBank.add(new Scenario("Helping a friend who fell down", R.drawable.ic_faithfulness, true));
        scenarioBank.add(new Scenario("Taking a toy without asking", R.drawable.ic_slavery, false));
        scenarioBank.add(new Scenario("Listening to your parents", R.drawable.ic_growing_disciple, true));
        scenarioBank.add(new Scenario("Telling a lie to stay out of trouble", R.drawable.ic_conflict, false));
        scenarioBank.add(new Scenario("Sharing your lunch with someone", R.drawable.ic_growing_scholar, true));
        scenarioBank.add(new Scenario("Cheating during a test", R.drawable.ic_conflict, false));
        scenarioBank.add(new Scenario("Praying before eating", R.drawable.ic_faithfulness, true));
        scenarioBank.add(new Scenario("Bullying a classmate", R.drawable.ic_conflict, false));
        scenarioBank.add(new Scenario("Helping clean the classroom", R.drawable.ic_growing_disciple, true));
        scenarioBank.add(new Scenario("Stealing money from a bag", R.drawable.ic_slavery, false));
        scenarioBank.add(new Scenario("Forgiving someone who hurt you", R.drawable.ic_faithfulness, true));
        scenarioBank.add(new Scenario("Destroying school property", R.drawable.ic_conflict, false));
        scenarioBank.add(new Scenario("Respecting your teacher", R.drawable.ic_growing_scholar, true));
        scenarioBank.add(new Scenario("Ignoring someone who needs help", R.drawable.ic_slavery, false));
        scenarioBank.add(new Scenario("Being honest even when scared",R.drawable.ic_faithfulness,true));

        Collections.shuffle(scenarioBank);
        scenarios.clear();

        for (int i = 0; i < 10; i++) {
            scenarios.add(scenarioBank.get(i));
        }

        quizProgress.setMax(scenarios.size());
    }

    private void updateUI() {
        if (currentScenarioIndex >= scenarios.size()) {
            finishQuiz();
            return;
        }

        Scenario current = scenarios.get(currentScenarioIndex);
        ivScenario.setImageResource(current.imageRes);
        tvScenarioDesc.setText(current.description);
        tvScenarioCount.setText(currentScenarioIndex + 1 + " / " + scenarios.size());
        quizProgress.setProgress(currentScenarioIndex);

        startTimer();
    }

    private void startTimer() {
        if (questionTimer != null) questionTimer.cancel();
        questionTimer = new CountDownTimer(TIME_LIMIT, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000);
                String time = String.format(java.util.Locale.getDefault(), "00:%02d", seconds);
                tvTimer.setText(time);
                if (seconds <= 5) tvTimer.setTextColor(Color.RED);
                else tvTimer.setTextColor(Color.parseColor("#1D4A4B"));
            }

            @Override
            public void onFinish() {
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
        saveResults();
    }

    private void saveResults() {
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
            String durationString = String.format(java.util.Locale.getDefault(), "%dm %02ds", minutes, seconds);
            String dateString = new SimpleDateFormat("MMM dd (EEE)", Locale.getDefault()).format(new Date());

            Map<String, Object> result = new HashMap<>();
            result.put("studentUid", uid);
            result.put("username", username);
            result.put("storyTitle", "The Law and Wandering in the Desert");
            result.put("score", score);
            result.put("xp", totalXP);
            result.put("totalQuestions", scenarios.size());
            result.put("timestamp", com.google.firebase.Timestamp.now());
            result.put("duration", durationString);
            result.put("date", dateString);

            db.collection("quiz_results").add(result)
                    .addOnSuccessListener(dr -> {
                        db.collection("users").document(uid).update("totalXP", FieldValue.increment(totalXP));
                        endQuizSession();
                        Toast.makeText(this, "Quiz Finished! Gained " + totalXP + " XP", Toast.LENGTH_LONG).show();
                        finish();
                    });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (questionTimer != null) questionTimer.cancel();
        if (!isChangingConfigurations()) {
            endQuizSession();
        }
    }

    private static class Scenario {
        String description;
        int imageRes;
        boolean isGood;

        Scenario(String description, int imageRes, boolean isGood) {
            this.description = description;
            this.imageRes = imageRes;
            this.isGood = isGood;
        }
    }
}
