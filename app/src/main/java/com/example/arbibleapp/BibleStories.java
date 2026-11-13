package com.example.arbibleapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

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
    private ConstraintLayout cardCreationFall, cardFlood, cardBabelAbraham, cardExodus, cardLawDesert,
            cardJudges, cardMonarchy, cardDivisionExile, cardReturnMessiah,
            cardBirthMinistry;

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
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        etSearch = findViewById(R.id.etSearch);
        containerStories = findViewById(R.id.containerStories);

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
        if (cardJudges != null) cardJudges.setOnClickListener(v -> openStoryReview("Entering the Promised Land and the Judges", QuizJudges.class));
        if (cardMonarchy != null) cardMonarchy.setOnClickListener(v -> openStoryReview("The Monarchy: Saul, David, and Solomon", QuizMonarchy.class));
        if (cardDivisionExile != null) cardDivisionExile.setOnClickListener(v -> openStoryReview("Division of the Kingdom and Exile", QuizDivision.class));
        if (cardReturnMessiah != null) cardReturnMessiah.setOnClickListener(v -> openStoryReview("Return from Exile and Anticipation of the Messiah", QuizReturn.class));
        if (cardBirthMinistry != null) cardBirthMinistry.setOnClickListener(v -> openStoryReview("The Birth and Ministry of Jesus", QuizBirth.class));

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
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
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

    private void applyFilters() {
        String query = etSearch.getText() != null ? etSearch.getText().toString().toLowerCase().trim() : "";
        List<View> matches = new ArrayList<>();

        checkAndAddMatch(matches, cardCreationFall, "Creation and the Fall", query);
        checkAndAddMatch(matches, cardFlood, "Early Humanity and the Flood", query);
        checkAndAddMatch(matches, cardBabelAbraham, "The Tower of Babel and Abraham's Covenant", query);
        checkAndAddMatch(matches, cardExodus, "Slavery in Egypt and the Exodus", query);
        checkAndAddMatch(matches, cardLawDesert, "The Law and Wandering in the Desert", query);
        checkAndAddMatch(matches, cardJudges, "Entering the Promised Land and the Judges", query);
        checkAndAddMatch(matches, cardMonarchy, "The Monarchy: Saul, David, and Solomon", query);
        checkAndAddMatch(matches, cardDivisionExile, "Division of the Kingdom and Exile", query);
        checkAndAddMatch(matches, cardReturnMessiah, "Return from Exile and Anticipation of the Messiah", query);
        checkAndAddMatch(matches, cardBirthMinistry, "The Birth and Ministry of Jesus", query);

        setVisibleCards(matches.toArray(new View[0]));
    }

    private void checkAndAddMatch(List<View> matches, View card, String title, String query) {
        if (card != null && (query.isEmpty() || title.toLowerCase().contains(query))) {
            matches.add(card);
        }
    }
}
