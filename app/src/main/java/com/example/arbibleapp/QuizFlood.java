package com.example.arbibleapp;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class QuizFlood extends AppCompatActivity {

    private GridLayout gridLayout, gridLayoutWords;
    private Button btnSubmit;
    private ImageView btnBack;
    private TextView tvTimer;

    private final int GRID_SIZE = 8;
    private char[][] grid = new char[GRID_SIZE][GRID_SIZE];
    private String[] wordBank = {"BEAR", "BIRD", "CAT", "COW", "DOG", "DOVE", "FROG", "LION", "PIG", "SHEEP"};
    
    private List<Cell> selectedCells = new ArrayList<>();
    private List<String> foundWords = new ArrayList<>();
    private Map<String, Integer> colors = new HashMap<>();
    private TextView[][] tvGrid = new TextView[GRID_SIZE][GRID_SIZE];
    private Set<Cell> permanentlySelectedCells = new HashSet<>();

    private int score = 0;
    private long startTime;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private CountDownTimer countDownTimer;
    private boolean isTimeUp = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_quiz_flood);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        gridLayout = findViewById(R.id.gridLayout);
        gridLayoutWords = findViewById(R.id.gridLayoutWords);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnBack = findViewById(R.id.btnBack);
        tvTimer = findViewById(R.id.tvTimer);

        btnBack.setOnClickListener(v -> finish());
        btnSubmit.setOnClickListener(v -> {
            if (countDownTimer != null) countDownTimer.cancel();
            saveResults();
        });
        btnSubmit.setEnabled(false);

        setupColorMap();
        generateGrid();
        setupGrid();
        setupWordBank();
        startTimer();
        startTime = System.currentTimeMillis();
        startQuizSession();
    }

    private void startQuizSession() {
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            Map<String, Object> session = new HashMap<>();
            session.put("studentUid", uid);
            session.put("storyTitle", "Early Humanity and the Flood");
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

    private void startTimer() {
        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000);
                String timeLeftFormatted = String.format("00:%02d", seconds);
                tvTimer.setText(timeLeftFormatted);
                
                if (seconds <= 10) {
                    tvTimer.setTextColor(Color.RED);
                }
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
        btnSubmit.setEnabled(true);
        btnSubmit.setText("Time's Up! Submit Answers");
    }

    private void setupColorMap() {
        colors.put("BEAR", R.color.badge_orange);
        colors.put("BIRD", R.color.badge_blue);
        colors.put("CAT", R.color.badge_purple);
        colors.put("COW", R.color.badge_red);
        colors.put("DOG", R.color.badge_teal);
        colors.put("DOVE", R.color.badge_indigo);
        colors.put("FROG", R.color.badge_pink);
        colors.put("LION", R.color.badge_lime);
        colors.put("PIG", R.color.badge_cyan);
        colors.put("SHEEP", R.color.badge_yellow);
    }

    private void generateGrid() {
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                grid[i][j] = ' ';
            }
        }

        Random random = new Random();
        int[] dr = {0, 0, 1, -1, 1, 1, -1, -1};
        int[] dc = {1, -1, 0, 0, 1, -1, 1, -1};

        for (String word : wordBank) {
            boolean placed = false;
            int attempts = 0;
            while (!placed && attempts < 100) {
                int r = random.nextInt(GRID_SIZE);
                int c = random.nextInt(GRID_SIZE);
                int dir = random.nextInt(8);
                
                if (canPlaceWord(word, r, c, dr[dir], dc[dir])) {
                    placeWord(word, r, c, dr[dir], dc[dir]);
                    placed = true;
                }
                attempts++;
            }
        }

        // Fill remaining spaces
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                if (grid[i][j] == ' ') {
                    grid[i][j] = (char) ('A' + random.nextInt(26));
                }
            }
        }
    }

    private boolean canPlaceWord(String word, int r, int c, int dr, int dc) {
        if (r + dr * (word.length() - 1) < 0 || r + dr * (word.length() - 1) >= GRID_SIZE ||
            c + dc * (word.length() - 1) < 0 || c + dc * (word.length() - 1) >= GRID_SIZE) {
            return false;
        }

        for (int i = 0; i < word.length(); i++) {
            char cellChar = grid[r + dr * i][c + dc * i];
            if (cellChar != ' ' && cellChar != word.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private void placeWord(String word, int r, int c, int dr, int dc) {
        for (int i = 0; i < word.length(); i++) {
            grid[r + dr * i][c + dc * i] = word.charAt(i);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupGrid() {
        gridLayout.removeAllViews();
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                TextView tv = new TextView(this);
                tv.setText(String.valueOf(grid[r][c]));
                tv.setGravity(Gravity.CENTER);
                tv.setTextSize(20);
                tv.setPadding(10, 10, 10, 10);
                tv.setTextColor(Color.BLACK);
                tv.setBackgroundResource(R.drawable.bg_circle_white);
                
                tvGrid[r][c] = tv;
                
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = GridLayout.LayoutParams.WRAP_CONTENT;
                params.columnSpec = GridLayout.spec(c, 1f);
                params.rowSpec = GridLayout.spec(r, 1f);
                tv.setLayoutParams(params);
                
                gridLayout.addView(tv);
            }
        }

        gridLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isTimeUp) return true;
                
                int action = event.getAction();
                float x = event.getX();
                float y = event.getY();

                if (action == MotionEvent.ACTION_DOWN) {
                    v.performClick();
                    selectedCells.clear();
                }

                boolean foundChild = false;
                for (int i = 0; i < gridLayout.getChildCount(); i++) {
                    View child = gridLayout.getChildAt(i);
                    if (x >= child.getLeft() && x <= child.getRight() && y >= child.getTop() && y <= child.getBottom()) {
                        int r = i / GRID_SIZE;
                        int c = i % GRID_SIZE;
                        handleTouch(r, c, action);
                        foundChild = true;
                        break;
                    }
                }

                if (!foundChild && action == MotionEvent.ACTION_UP) {
                    handleTouch(-1, -1, action);
                }

                return true;
            }
        });
    }

    private void handleTouch(int r, int c, int action) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                if (r != -1 && c != -1) {
                    addCellToSelection(r, c);
                }
                break;
            case MotionEvent.ACTION_UP:
                checkWord();
                resetSelectionVisuals();
                selectedCells.clear();
                break;
        }
    }

    private void addCellToSelection(int r, int c) {
        Cell cell = new Cell(r, c, tvGrid[r][c]);
        int index = selectedCells.indexOf(cell);
        
        if (index != -1) {
            if (selectedCells.size() >= 2 && index == selectedCells.size() - 2) {
                Cell removedCell = selectedCells.remove(selectedCells.size() - 1);
                if (!permanentlySelectedCells.contains(removedCell)) {
                    removedCell.tv.setBackgroundResource(R.drawable.bg_circle_white);
                    removedCell.tv.setTextColor(Color.BLACK);
                }
            }
        } else {
            selectedCells.add(cell);
            if (!permanentlySelectedCells.contains(cell)) {
                cell.tv.setBackgroundColor(Color.LTGRAY);
            }
        }
    }

    private void resetSelectionVisuals() {
        for (Cell cell : selectedCells) {
            if (!permanentlySelectedCells.contains(cell)) {
                cell.tv.setBackgroundResource(R.drawable.bg_circle_white);
                cell.tv.setTextColor(Color.BLACK);
            }
        }
    }

    private void setupWordBank() {
        gridLayoutWords.removeAllViews();
        for (String word : wordBank) {
            TextView tv = new TextView(this);
            tv.setText(word);
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(16, 16, 16, 16);
            tv.setBackgroundResource(R.drawable.bg_circle_white);
            tv.setTextColor(Color.BLACK);
            tv.setTextSize(16);
            
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED);
            params.setMargins(8, 8, 8, 8);
            tv.setLayoutParams(params);
            
            gridLayoutWords.addView(tv);
        }
    }

    private boolean checkWord() {
        if (selectedCells.isEmpty()) return false;
        StringBuilder sb = new StringBuilder();
        for (Cell cell : selectedCells) {
            sb.append(grid[cell.r][cell.c]);
        }
        String selectedWord = sb.toString();

        for (String word : wordBank) {
            if (!foundWords.contains(word) && selectedWord.equals(word)) {
                markWordFound(word);
                return true;
            }
        }
        return false;
    }

    private void markWordFound(String word) {
        foundWords.add(word);
        score++;
        Integer colorRes = colors.get(word);
        if (colorRes == null) colorRes = R.color.badge_orange;
        int color = ContextCompat.getColor(this, colorRes);

        for (Cell cell : selectedCells) {
            cell.tv.setBackgroundColor(color);
            cell.tv.setTextColor(Color.BLACK);
            permanentlySelectedCells.add(cell);
        }

        for (int i = 0; i < gridLayoutWords.getChildCount(); i++) {
            TextView tv = (TextView) gridLayoutWords.getChildAt(i);
            if (tv.getText().toString().equals(word)) {
                tv.setBackgroundColor(color);
                tv.setTextColor(Color.BLACK);
                break;
            }
        }
        
        if (foundWords.size() == wordBank.length) {
            if (countDownTimer != null) countDownTimer.cancel();
            btnSubmit.setEnabled(true);
            Toast.makeText(this, "All animals found!", Toast.LENGTH_LONG).show();
        }
    }

    private void saveResults() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        int totalXP = score * 10;

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
            result.put("storyTitle", "Early Humanity and the Flood");
            result.put("score", score);
            result.put("xp", totalXP);
            result.put("totalQuestions", wordBank.length);
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

    private static class Cell {
        int r, c;
        TextView tv;

        Cell(int r, int c, TextView tv) {
            this.r = r;
            this.c = c;
            this.tv = tv;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Cell)) return false;
            Cell cell = (Cell) o;
            return r == cell.r && c == cell.c;
        }

        @Override
        public int hashCode() {
            return 31 * r + c;
        }
    }
}
