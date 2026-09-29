package com.example.arbibleapp;

import android.graphics.Color;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.CornerFamily;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class QuizBabel extends AppCompatActivity {

    private LinearLayout towerContainer;
    private Button btnSubmit;
    private ImageView btnBack;
    private TextView tvTimer;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private CountDownTimer countDownTimer;
    private boolean isTimeUp = false;
    private long startTime;
    private long timeLeftInMillis = 60000;
    private int score = 0;
    private int floorsCleared = 0;
    private int floorWrongGuesses = 0;
    private final int TOTAL_FLOORS = 5;
    private int currentFloorIndex = 0;

    private final int[] babelImages = {
            R.drawable.correct1,
            R.drawable.correct2,
            R.drawable.correct3,
            R.drawable.correct4,
            R.drawable.correct5,
            R.drawable.correct6,
            R.drawable.correct7,
            R.drawable.correct8,
            R.drawable.correct9,
            R.drawable.correct10,
    };
    private final int[] oddOneOutImages = {
            R.drawable.odd1,
            R.drawable.odd2,
            R.drawable.odd3,
            R.drawable.odd4,
            R.drawable.odd5,
    };

    private List<LinearLayout> floorLayouts = new ArrayList<>();
    private boolean isSubmittingResult = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_quiz_babel);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        towerContainer = findViewById(R.id.towerContainer);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnBack = findViewById(R.id.btnBack);
        tvTimer = findViewById(R.id.tvTimer);

        btnBack.setOnClickListener(v -> finish());

        View btnInstructions = findViewById(R.id.btnInstructions);
        if (btnInstructions != null) {
            btnInstructions.setOnClickListener(v -> showInstructionsDialog());
        }

        btnSubmit.setOnClickListener(v -> saveResults());

        setupTower();
        startTimer();
        startTime = System.currentTimeMillis();
        startQuizSession();
    }

    private void startQuizSession() {
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            Map<String, Object> session = new HashMap<>();
            session.put("studentUid", uid);
            session.put("storyTitle", "The Tower of Babel");
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

    private void setupTower() {
        towerContainer.removeAllViews();
        floorLayouts.clear();

        List<Integer> masterBabel = new ArrayList<>();
        for(int img : babelImages) masterBabel.add(img);
        Collections.shuffle(masterBabel);

        List<Integer> masterOdd = new ArrayList<>();
        for(int img : oddOneOutImages) masterOdd.add(img);
        Collections.shuffle(masterOdd);

        for (int f = TOTAL_FLOORS - 1; f >= 0; f--) {
            LinearLayout floorRow = new LinearLayout(this);
            floorRow.setOrientation(LinearLayout.HORIZONTAL);
            floorRow.setGravity(Gravity.CENTER);
            floorRow.setPadding(2, 12, 2, 8);
            
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            floorRow.setLayoutParams(rowParams);

            List<BlockData> blocksInFloor = new ArrayList<>();

            blocksInFloor.add(new BlockData(masterBabel.get(f * 2), true, f));
            blocksInFloor.add(new BlockData(masterBabel.get(f * 2 + 1), true, f));
            blocksInFloor.add(new BlockData(masterOdd.get(f), false, f));

            Collections.shuffle(blocksInFloor);

            for (BlockData data : blocksInFloor) {
                FrameLayout container = new FrameLayout(this);
                int size = (int) (100 * getResources().getDisplayMetrics().density);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
                params.setMargins(8, 0, 8, 0);
                container.setLayoutParams(params);
                container.setBackgroundResource(R.drawable.bg_white_card);
                container.setElevation(4f);
                container.setClipToOutline(true);

                ShapeableImageView blockView = new ShapeableImageView(this);
                FrameLayout.LayoutParams imgParams = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                );
                blockView.setLayoutParams(imgParams);
                blockView.setImageResource(data.imageRes);
                

                blockView.setPadding(30, 30, 30, 30);
                blockView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                

                float imgRadius = 16 * getResources().getDisplayMetrics().density;
                blockView.setShapeAppearanceModel(blockView.getShapeAppearanceModel()
                        .toBuilder()
                        .setAllCorners(CornerFamily.ROUNDED, imgRadius)
                        .build());

                container.setOnClickListener(v -> {
                    if (isTimeUp) return;
                    handleBlockClick(data, container);
                });

                container.addView(blockView);
                floorRow.addView(container);
            }

            if (f > 0) {
                floorRow.setAlpha(0.3f);
            }

            towerContainer.addView(floorRow);
            floorLayouts.add(0, floorRow);
        }
    }

    private void handleBlockClick(BlockData data, View view) {
        if (data.floorIndex != currentFloorIndex) {
            Toast.makeText(this, "Complete the bottom floor first!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!data.isBabel) {
            floorsCleared++;
            int floorPoints = Math.max(0, 15 - (floorWrongGuesses * 5));
            score += floorPoints;

            if (view.getBackground() != null) {
                view.getBackground().mutate().setTint(Color.parseColor("#4CAF50")); 
            }

            LinearLayout currentFloor = floorLayouts.get(currentFloorIndex);
            for(int i=0; i<currentFloor.getChildCount(); i++) {
                currentFloor.getChildAt(i).setClickable(false);
            }

            currentFloorIndex++;
            floorWrongGuesses = 0;

            if (currentFloorIndex < TOTAL_FLOORS) {
                Toast.makeText(this, "Floor " + currentFloorIndex + " complete! +" + floorPoints + " XP", Toast.LENGTH_SHORT).show();
                floorLayouts.get(currentFloorIndex).setAlpha(1.0f);
            } else {
                Toast.makeText(this, "Tower Complete! Total XP: " + score, Toast.LENGTH_SHORT).show();
                if (countDownTimer != null) countDownTimer.cancel();
                btnSubmit.setVisibility(View.VISIBLE);
                btnSubmit.setEnabled(true);
            }
        } else {
            floorWrongGuesses++;

            if (view.getBackground() != null) {
                view.getBackground().mutate().setTint(Color.parseColor("#EF5350")); // Material Red
            }

            view.setClickable(false);
            Toast.makeText(this, "That belongs to Babel! -5 XP potential.", Toast.LENGTH_SHORT).show();
        }
    }

    private void pauseTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
    }

    private void resumeTimer() {
        if (timeLeftInMillis <= 0) return;
        startTimerWithDuration(timeLeftInMillis);
    }

    private void startTimer() {
        startTimerWithDuration(60000);
    }

    private void startTimerWithDuration(long durationMs) {
        if (countDownTimer != null) countDownTimer.cancel();
        countDownTimer = new CountDownTimer(durationMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                int seconds = (int) (millisUntilFinished / 1000);
                String timeLeftFormatted = String.format(Locale.getDefault(), "00:%02d", seconds);
                tvTimer.setText(timeLeftFormatted);
                if (seconds <= 10) tvTimer.setTextColor(Color.RED);
            }

            @Override
            public void onFinish() {
                timeLeftInMillis = 0;
                tvTimer.setText("00:00");
                isTimeUp = true;
                handleTimeUp();
            }
        }.start();
    }

    private void handleTimeUp() {
        Toast.makeText(this, "Time's up!", Toast.LENGTH_LONG).show();
        btnSubmit.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(true);
        btnSubmit.setText("Time's Up! Submit Results");
    }

    private void showInstructionsDialog() {
        pauseTimer();
        new AlertDialog.Builder(this)
                .setTitle("ℹ️ Quiz 3 Instructions")
                .setMessage("Tap the correct tower block on each floor to build the tower step-by-step!\n\nWatch out for the odd-one-out block on every floor to reach the top!")
                .setPositiveButton("Got It!", (dialog, which) -> resumeTimer())
                .setOnCancelListener(dialog -> resumeTimer())
                .show();
    }

    private void saveResults() {
        if (isSubmittingResult) return;
        isSubmittingResult = true;

        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        int totalXP = score;

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
            result.put("storyTitle", "The Tower of Babel");
            result.put("score", floorsCleared);
            result.put("xp", totalXP);
            result.put("totalQuestions", TOTAL_FLOORS);
            result.put("timestamp", Timestamp.now());
            result.put("duration", durationString);
            result.put("date", dateString);
            result.put("wrongAnswers", new ArrayList<String>());
            result.put("correctAnswers", new ArrayList<String>());
            result.put("selectedAnswers", new ArrayList<String>());
            result.put("isSunday", isSunday);

            db.collection("quiz_results").add(result)
                    .addOnSuccessListener(dr -> {
                        boolean leveledUp = false;
                        int newLevel = 1;

                        if (totalXP > 0) {
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
                        returnToDashboard(isSunday, floorsCleared, TOTAL_FLOORS, totalXP, leveledUp, newLevel);
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
        intent.putExtra("XP", xp);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
        if (!isChangingConfigurations()) {
            endQuizSession();
        }
    }

    private static class BlockData {
        int imageRes;
        boolean isBabel;
        int floorIndex;

        BlockData(int imageRes, boolean isBabel, int floorIndex) {
            this.imageRes = imageRes;
            this.isBabel = isBabel;
            this.floorIndex = floorIndex;
        }
    }
}
