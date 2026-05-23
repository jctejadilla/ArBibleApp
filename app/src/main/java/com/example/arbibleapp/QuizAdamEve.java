package com.example.arbibleapp;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class QuizAdamEve extends AppCompatActivity {

    private TextView tvQuestion, tvQuestionCount, tvTimer;
    private RadioGroup rgOptions;
    private RadioButton rbOptionA, rbOptionB, rbOptionC, rbOptionD;
    private Button btnNext, btnPrevious;
    private ImageView btnBack;
    private ProgressBar progressBar;

    private CountDownTimer countDownTimer;
    private static final long QUIZ_TIME = 60000; // 1 minute

    private static final String PREFS_NAME = "QuizProgress";
    private static final String KEY_INDEX = "AdamEve_Index";
    private static final String KEY_SELECTIONS = "AdamEve_Selections";

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (!isChangingConfigurations()) {
            endQuizSession();
        }
    }

    private static class QuestionModel {
        String question;
        String[] options;
        int correctAnswerIndex;

        QuestionModel(String question, String[] options, int correctAnswerIndex) {
            this.question = question;
            String correctOptionText = options[correctAnswerIndex];
            List<String> optionsList = new ArrayList<>(Arrays.asList(options));
            Collections.shuffle(optionsList);
            this.options = optionsList.toArray(new String[0]);
            for (int i = 0; i < this.options.length; i++) {
                if (this.options[i].equals(correctOptionText)) {
                    this.correctAnswerIndex = i;
                    break;
                }
            }
        }
    }

    private List<QuestionModel> questionList = new ArrayList<>();
    private int[] userSelections;

    private int currentQuestionIndex = 0;
    private boolean isQuizFinished = false;
    private long startTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_quiz_adam_eve);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvQuestion = findViewById(R.id.tvQuestion);
        tvQuestionCount = findViewById(R.id.tvQuestionCount);
        rgOptions = findViewById(R.id.rgOptions);
        rbOptionA = findViewById(R.id.rbOptionA);
        rbOptionB = findViewById(R.id.rbOptionB);
        rbOptionC = findViewById(R.id.rbOptionC);
        rbOptionD = findViewById(R.id.rbOptionD);
        btnNext = findViewById(R.id.btnNext);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);
        tvTimer = findViewById(R.id.tvTimer);

        setupQuestions();
        userSelections = new int[questionList.size()];
        Arrays.fill(userSelections, -1);

        btnBack.setOnClickListener(v -> finish());

        clearProgress();
        loadQuestion();
        startTimer();
        startTime = System.currentTimeMillis();
        startQuizSession();

        btnNext.setOnClickListener(v -> {
            int selectedId = rgOptions.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(this, "Please select an answer", Toast.LENGTH_SHORT).show();
                return;
            }

            RadioButton selectedRadioButton = findViewById(selectedId);
            userSelections[currentQuestionIndex] = rgOptions.indexOfChild(selectedRadioButton);

            if (currentQuestionIndex < questionList.size() - 1) {
                currentQuestionIndex++;
                loadQuestion();
            } else {
                saveQuizResult();
            }
        });

        btnPrevious.setOnClickListener(v -> {
            int selectedId = rgOptions.getCheckedRadioButtonId();
            if (selectedId != -1) {
                RadioButton selectedRadioButton = findViewById(selectedId);
                userSelections[currentQuestionIndex] = rgOptions.indexOfChild(selectedRadioButton);
            }

            if (currentQuestionIndex > 0) {
                currentQuestionIndex--;
                loadQuestion();
            }
        });
    }

    private void setupQuestions() {
        String[] questions = {
                "Who were the first man and woman created by God?",
                "Where did Adam and Eve live at the beginning?",
                "What was the name of the garden?",
                "Who created Adam and Eve?",
                "What did God tell Adam and Eve not to eat?",
                "Which animal tricked Eve?",
                "What did the serpent say would happen if Eve ate the fruit?",
                "Why did God create Eve?",
                "What did Adam and Eve use to cover themselves?",
                "What is one important lesson from the story?"
        };

        String[][] options = {
                {"Noah and his wife", "Abraham and Sarah", "Adam and Eve", "Moses and Miriam"},
                {"In a castle", "In a garden", "In a city", "On a mountain"},
                {"Garden of Peace", "Garden of Life", "Garden of Eden", "Garden of Hope"},
                {"An angel", "A king", "God", "A farmer"},
                {"Apples", "Grapes", "Bread", "Fruit from the Tree of Knowledge"},
                {"Lion", "Monkey", "Dove", "Serpent (snake)"},
                {"She would get sick", "She would fall asleep", "She would disappear", "She would be like God, knowing good and evil"},
                {"To work in the garden", "To name the animals", "To be a helper and companion for Adam", "To build a house"},
                {"Animal skin", "Grass", "Blankets", "Leaves"},
                {"Always hide", "Animals can talk", "We should obey God", "Gardens are dangerous"}
        };

        int[] correctAnswers = {2, 1, 2, 2, 3, 3, 3, 2, 3, 2};

        for (int i = 0; i < questions.length; i++) {
            questionList.add(new QuestionModel(questions[i], options[i], correctAnswers[i]));
        }
        Collections.shuffle(questionList);
    }

    private void loadQuestion() {
        rgOptions.clearCheck();
        QuestionModel currentQuestion = questionList.get(currentQuestionIndex);
        tvQuestion.setText(currentQuestion.question);
        rbOptionA.setText(currentQuestion.options[0]);
        rbOptionB.setText(currentQuestion.options[1]);
        rbOptionC.setText(currentQuestion.options[2]);
        rbOptionD.setText(currentQuestion.options[3]);
        tvQuestionCount.setText("Question " + (currentQuestionIndex + 1) + " of " + questionList.size());
        
        progressBar.setMax(questionList.size());
        progressBar.setProgress(currentQuestionIndex + 1);

        if (currentQuestionIndex == 0) {
            btnPrevious.setVisibility(View.INVISIBLE);
        } else {
            btnPrevious.setVisibility(View.VISIBLE);
        }

        if (currentQuestionIndex == questionList.size() - 1) {
            btnNext.setText("Submit");
        } else {
            btnNext.setText("Next");
        }

        if (userSelections[currentQuestionIndex] != -1) {
            ((RadioButton) rgOptions.getChildAt(userSelections[currentQuestionIndex])).setChecked(true);
        }
    }

    private void clearProgress() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().remove(KEY_INDEX).remove(KEY_SELECTIONS).apply();
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(QUIZ_TIME, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000);
                String timeLeftFormatted = String.format(java.util.Locale.getDefault(), "00:%02d", seconds);
                tvTimer.setText(timeLeftFormatted);

                if (seconds <= 10) {
                    tvTimer.setTextColor(Color.RED);
                }
            }

            @Override
            public void onFinish() {
                tvTimer.setText("00:00");
                Toast.makeText(QuizAdamEve.this, "Time's up!", Toast.LENGTH_SHORT).show();
                saveQuizResult();
            }
        }.start();
    }

    private void startQuizSession() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String uid = auth.getCurrentUser().getUid();
            Map<String, Object> session = new HashMap<>();
            session.put("studentUid", uid);
            session.put("storyTitle", "Creation and the Fall");
            session.put("timestamp", com.google.firebase.Timestamp.now());

            FirebaseFirestore.getInstance().collection("quiz_sessions").document(uid).set(session);
        }
    }

    private void endQuizSession() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String uid = auth.getCurrentUser().getUid();
            FirebaseFirestore.getInstance().collection("quiz_sessions").document(uid).delete();
        }
    }

    private void saveQuizResult() {
        if (countDownTimer != null) countDownTimer.cancel();
        int tempScore = 0;
        for (int i = 0; i < questionList.size(); i++) {
            if (userSelections[i] == questionList.get(i).correctAnswerIndex) {
                tempScore++;
            }
        }
        final int finalScore = tempScore;
        final int xpGained = finalScore * 10;

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() != null) {
            String uid = auth.getCurrentUser().getUid();

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
                result.put("storyTitle", "Creation and the Fall");
                result.put("score", finalScore);
                result.put("xp", xpGained);
                result.put("totalQuestions", questionList.size());
                result.put("timestamp", com.google.firebase.Timestamp.now());
                result.put("duration", durationString);
                result.put("date", dateString);

                db.collection("quiz_results").add(result)
                        .addOnSuccessListener(documentReference -> {
                            db.collection("users").document(uid)
                                    .update("totalXP", FieldValue.increment(xpGained));

                            endQuizSession();
                            isQuizFinished = true;
                            clearProgress();
                            Toast.makeText(this, "Quiz Finished! Score: " + finalScore + "/" + questionList.size() + " (" + xpGained + " XP)", Toast.LENGTH_LONG).show();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Error saving result", Toast.LENGTH_SHORT).show();
                            finish();
                        });
            });
        } else {
            endQuizSession();
            isQuizFinished = true;
            clearProgress();
            Toast.makeText(this, "Quiz Finished! Score: " + finalScore + "/" + questionList.size(), Toast.LENGTH_LONG).show();
            finish();
        }
    }
}
