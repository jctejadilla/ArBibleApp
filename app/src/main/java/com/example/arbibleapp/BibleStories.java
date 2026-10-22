package com.example.arbibleapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Color;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class BibleStories extends AppCompatActivity {

    private ImageView btnBack;
    private EditText etSearch;
    private View containerStories;
    private TextView btnAll, btnOldTestament, btnNewTestament;
    private ConstraintLayout cardCreationFall, cardFlood, cardBabelAbraham, cardExodus, cardLawDesert,
            cardJudges, cardMonarchy, cardDivisionExile, cardReturnMessiah,
            cardBirthMinistry, cardConflictDeath, cardResurrectionAscension;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bible_stories);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);

            View topBar = findViewById(R.id.topBar);
            if (topBar != null) {
                topBar.setPadding(
                        topBar.getPaddingLeft(),
                        systemBars.top + 12,
                        topBar.getPaddingRight(),
                        topBar.getPaddingBottom()
                );
            }
            return insets;
        });

        initViews();
        setupListeners();
        setupSearch();
        setupCategories();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        etSearch = findViewById(R.id.etSearch);
        containerStories = findViewById(R.id.containerStories);
        btnAll = findViewById(R.id.btnAll);
        btnOldTestament = findViewById(R.id.btnOldTestament);
        btnNewTestament = findViewById(R.id.btnNewTestament);

        cardCreationFall = findViewById(R.id.cardCreationFall);
        cardFlood = findViewById(R.id.cardFlood);
        cardBabelAbraham = findViewById(R.id.cardBabelAbraham);
        cardExodus = findViewById(R.id.cardExodus);
        cardLawDesert = findViewById(R.id.cardLawDesert);
        cardJudges = findViewById(R.id.cardJudges);
        cardMonarchy = findViewById(R.id.cardMonarchy);
        cardDivisionExile = findViewById(R.id.cardDivisionExile);
        cardReturnMessiah = findViewById(R.id.cardReturnMessiah);
        cardBirthMinistry = findViewById(R.id.cardBirthMinistry);
        cardConflictDeath = findViewById(R.id.cardConflictDeath);
        cardResurrectionAscension = findViewById(R.id.cardResurrectionAscension);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        if (cardCreationFall != null) {
            cardCreationFall.setOnClickListener(v -> openStoryReview("Creation and the Fall", QuizAdamEve.class));
        }

        if (cardFlood != null) {
            cardFlood.setOnClickListener(v -> openStoryReview("Early Humanity and the Flood", QuizFlood.class));
        }

        if (cardBabelAbraham != null) {
            cardBabelAbraham.setOnClickListener(v -> openStoryReview("The Tower of Babel", QuizBabel.class));
        }

        if (cardExodus != null) {
            cardExodus.setOnClickListener(v -> openStoryReview("Slavery in Egypt and the Exodus", QuizExodus.class));
        }

        if (cardLawDesert != null) {
            cardLawDesert.setOnClickListener(v -> openStoryReview("The Law and Wandering in the Desert", QuizLaw.class));
        }

        // Setup other cards similarly if quizzes exist
        if (cardJudges != null) cardJudges.setOnClickListener(v -> openStoryReview("Entering the Promised Land and the Judges", null));
        if (cardMonarchy != null) cardMonarchy.setOnClickListener(v -> openStoryReview("The Monarchy: Saul, David, and Solomon", null));
        if (cardDivisionExile != null) cardDivisionExile.setOnClickListener(v -> openStoryReview("Division of the Kingdom and Exile", null));
        if (cardReturnMessiah != null) cardReturnMessiah.setOnClickListener(v -> openStoryReview("Return from Exile and Anticipation of the Messiah", null));
        if (cardBirthMinistry != null) cardBirthMinistry.setOnClickListener(v -> openStoryReview("The Birth and Ministry of Jesus", null));
        if (cardConflictDeath != null) cardConflictDeath.setOnClickListener(v -> openStoryReview("Conflict with the Pharisees and Jesus's Death", null));
        if (cardResurrectionAscension != null) cardResurrectionAscension.setOnClickListener(v -> openStoryReview("Resurrection and Ascension", null));
    }

    private void openStoryReview(String title, Class<?> quizClass) {
        Intent intent = new Intent(BibleStories.this, StudentQuizReviewActivity.class);
        intent.putExtra("storyTitle", title);
        if (quizClass != null) {
            intent.putExtra("quizClass", quizClass.getName());
        }
        startActivity(intent);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStories(s.toString().toLowerCase());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupCategories() {
        btnAll.setOnClickListener(v -> {
            updateCategoryUI(btnAll);
            showAllStories();
        });

        btnOldTestament.setOnClickListener(v -> {
            updateCategoryUI(btnOldTestament);
            showOldTestament();
        });

        btnNewTestament.setOnClickListener(v -> {
            updateCategoryUI(btnNewTestament);
            showNewTestament();
        });
    }

    private void updateCategoryUI(TextView selectedBtn) {
        resetCategoryButton(btnAll);
        resetCategoryButton(btnOldTestament);
        resetCategoryButton(btnNewTestament);

        selectedBtn.setBackgroundResource(R.drawable.bg_pill_btn_yellow);
        selectedBtn.setTextColor(Color.parseColor("#1D3B2E"));
    }

    private void resetCategoryButton(TextView btn) {
        btn.setBackgroundResource(R.drawable.bg_cream_task_card);
        btn.setTextColor(Color.parseColor("#4A6956"));
    }

    private void setVisibleCards(View... cards) {
        if (containerStories instanceof ViewGroup) {
            ViewGroup grid = (ViewGroup) containerStories;
            grid.removeAllViews();
            for (View card : cards) {
                if (card != null) {
                    card.setVisibility(View.VISIBLE);
                    grid.addView(card);
                }
            }
        }
    }

    private void showAllStories() {
        setVisibleCards(
                cardCreationFall, cardFlood, cardBabelAbraham, cardExodus,
                cardLawDesert, cardJudges, cardMonarchy, cardDivisionExile,
                cardReturnMessiah, cardBirthMinistry, cardConflictDeath, cardResurrectionAscension
        );
    }

    private void showOldTestament() {
        setVisibleCards(
                cardCreationFall, cardFlood, cardBabelAbraham, cardExodus,
                cardLawDesert, cardJudges, cardMonarchy, cardDivisionExile, cardReturnMessiah
        );
    }

    private void showNewTestament() {
        setVisibleCards(
                cardBirthMinistry, cardConflictDeath, cardResurrectionAscension
        );
    }

    private void filterStories(String query) {
        if (query == null || query.trim().isEmpty()) {
            showAllStories();
            return;
        }
        String q = query.toLowerCase().trim();
        List<View> matches = new ArrayList<>();
        checkAndAddMatch(matches, cardCreationFall, "Creation and the Fall", q);
        checkAndAddMatch(matches, cardFlood, "Early Humanity and the Flood", q);
        checkAndAddMatch(matches, cardBabelAbraham, "The Tower of Babel and Abraham's Covenant", q);
        checkAndAddMatch(matches, cardExodus, "Slavery in Egypt and the Exodus", q);
        checkAndAddMatch(matches, cardLawDesert, "The Law and Wandering in the Desert", q);
        checkAndAddMatch(matches, cardJudges, "Entering the Promised Land and the Judges", q);
        checkAndAddMatch(matches, cardMonarchy, "The Monarchy: Saul, David, and Solomon", q);
        checkAndAddMatch(matches, cardDivisionExile, "Division of the Kingdom and Exile", q);
        checkAndAddMatch(matches, cardReturnMessiah, "Return from Exile and Anticipation of the Messiah", q);
        checkAndAddMatch(matches, cardBirthMinistry, "The Birth and Ministry of Jesus", q);
        checkAndAddMatch(matches, cardConflictDeath, "Conflict with the Pharisees and Jesus's Death", q);
        checkAndAddMatch(matches, cardResurrectionAscension, "Resurrection and Ascension", q);

        setVisibleCards(matches.toArray(new View[0]));
    }

    private void checkAndAddMatch(List<View> matches, View card, String title, String query) {
        if (card != null && title.toLowerCase().contains(query)) {
            matches.add(card);
        }
    }
}
