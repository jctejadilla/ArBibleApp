package com.example.arbibleapp;

import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
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
    private int score = 0; // Tracks total points/XP
    private int floorsCleared = 0;
    private int floorWrongGuesses = 0;
    private final int TOTAL_FLOORS = 5;
    private int currentFloorIndex = 0;

    private final int[] babelImages = {
            R.drawable.ic_babel,
            R.drawable.ic_law,
            R.drawable.ic_gold,
            R.drawable.ic_land,
            R.drawable.ic_monarchy
    };
    private final int[] oddOneOutImages = {
            R.drawable.ic_adam,
            R.drawable.ic_noah
    };

    private List<LinearLayout> floorLayouts = new ArrayList<>();

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

    private void setupTower() {
        towerContainer.removeAllViews();
        floorLayouts.clear();
        Random random = new Random();

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
            List<Integer> selectedBabel = new ArrayList<>();

            for(int img : babelImages) selectedBabel.add(img);

            Collections.shuffle(selectedBabel);

            blocksInFloor.add(new BlockData(selectedBabel.get(0), true, f));
            blocksInFloor.add(new BlockData(selectedBabel.get(1), true, f));

            int oddImg = oddOneOutImages[random.nextInt(oddOneOutImages.length)];
            blocksInFloor.add(new BlockData(oddImg, false, f));

            Collections.shuffle(blocksInFloor);

            for (BlockData data : blocksInFloor) {
                ImageView blockView = new ImageView(this);
                int size = (int) (100 * getResources().getDisplayMetrics().density);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
                params.setMargins(8, 0, 8, 0);
                blockView.setLayoutParams(params);
                
                blockView.setImageResource(data.imageRes);
                blockView.setBackgroundResource(R.drawable.bg_white_card);
                blockView.setPadding(15, 15, 15, 15);
                blockView.setElevation(4f);

                blockView.setOnClickListener(v -> {
                    if (isTimeUp) return;
                    handleBlockClick(data, blockView);
                });

                floorRow.addView(blockView);
            }

            if (f > 0) {
                floorRow.setAlpha(0.3f);
            }

            towerContainer.addView(floorRow);
            floorLayouts.add(0, floorRow);
        }
    }

    private void handleBlockClick(BlockData data, ImageView view) {
        if (data.floorIndex != currentFloorIndex) {
            Toast.makeText(this, "Complete the bottom floor first!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!data.isBabel) {
            floorsCleared++;
            int floorPoints = Math.max(0, 15 - (floorWrongGuesses * 5));
            score += floorPoints;
            view.setBackgroundColor(Color.GREEN);

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
            view.setBackgroundColor(Color.RED);
            view.setClickable(false);
            Toast.makeText(this, "That belongs to Babel! -5 XP potential.", Toast.LENGTH_SHORT).show();
        }
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000);
                String timeLeftFormatted = String.format(java.util.Locale.getDefault(), "00:%02d", seconds);
                tvTimer.setText(timeLeftFormatted);
                if (seconds <= 10) tvTimer.setTextColor(Color.RED);
            }

            @Override
            public void onFinish() {
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

    private void saveResults() {
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
            String durationString = String.format(java.util.Locale.getDefault(), "%dm %02ds", minutes, seconds);
            String dateString = new SimpleDateFormat("MMM dd (EEE)", Locale.getDefault()).format(new Date());

            Map<String, Object> result = new HashMap<>();
            result.put("studentUid", uid);
            result.put("username", username);
            result.put("storyTitle", "The Tower of Babel");
            result.put("score", floorsCleared);
            result.put("xp", totalXP);
            result.put("totalQuestions", TOTAL_FLOORS);
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
