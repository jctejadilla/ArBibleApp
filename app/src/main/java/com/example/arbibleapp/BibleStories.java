package com.example.arbibleapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class BibleStories extends AppCompatActivity {

    private ImageView btnBack;
    private EditText etSearch;
    private LinearLayout containerStories;
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
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
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
            cardCreationFall.setOnClickListener(v -> {
                Intent intent = new Intent(BibleStories.this, QuizAdamEve.class);
                startActivity(intent);
            });
        }

        if (cardFlood != null) {
            cardFlood.setOnClickListener(v -> {
                Intent intent = new Intent(BibleStories.this, QuizFlood.class);
                startActivity(intent);
            });
        }

        if (cardBabelAbraham != null) {
            cardBabelAbraham.setOnClickListener(v -> {
                Intent intent = new Intent(BibleStories.this, QuizBabel.class);
                startActivity(intent);
            });
        }

        if (cardExodus != null) {
            cardExodus.setOnClickListener(v -> {
                Intent intent = new Intent(BibleStories.this, QuizExodus.class);
                startActivity(intent);
            });
        }

        if (cardLawDesert != null) {
            cardLawDesert.setOnClickListener(v -> {
                Intent intent = new Intent(BibleStories.this, QuizLaw.class);
                startActivity(intent);
            });
        }
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

        selectedBtn.setBackgroundResource(R.drawable.bg_button_next);
        selectedBtn.setTextColor(getResources().getColor(R.color.white));
    }

    private void resetCategoryButton(TextView btn) {
        btn.setBackgroundResource(R.drawable.bg_search_bar);
        btn.setTextColor(getResources().getColor(R.color.text_grey));
    }

    private void showAllStories() {
        setAllVisibility(View.VISIBLE);
    }

    private void showOldTestament() {
        setAllVisibility(View.GONE);
        if (cardCreationFall != null) cardCreationFall.setVisibility(View.VISIBLE);
        if (cardFlood != null) cardFlood.setVisibility(View.VISIBLE);
        if (cardBabelAbraham != null) cardBabelAbraham.setVisibility(View.VISIBLE);
        if (cardExodus != null) cardExodus.setVisibility(View.VISIBLE);
        if (cardLawDesert != null) cardLawDesert.setVisibility(View.VISIBLE);
        if (cardJudges != null) cardJudges.setVisibility(View.VISIBLE);
        if (cardMonarchy != null) cardMonarchy.setVisibility(View.VISIBLE);
        if (cardDivisionExile != null) cardDivisionExile.setVisibility(View.VISIBLE);
        if (cardReturnMessiah != null) cardReturnMessiah.setVisibility(View.VISIBLE);
    }

    private void showNewTestament() {
        setAllVisibility(View.GONE);
        if (cardBirthMinistry != null) cardBirthMinistry.setVisibility(View.VISIBLE);
        if (cardConflictDeath != null) cardConflictDeath.setVisibility(View.VISIBLE);
        if (cardResurrectionAscension != null) cardResurrectionAscension.setVisibility(View.VISIBLE);
    }

    private void setAllVisibility(int visibility) {
        if (cardCreationFall != null) cardCreationFall.setVisibility(visibility);
        if (cardFlood != null) cardFlood.setVisibility(visibility);
        if (cardBabelAbraham != null) cardBabelAbraham.setVisibility(visibility);
        if (cardExodus != null) cardExodus.setVisibility(visibility);
        if (cardLawDesert != null) cardLawDesert.setVisibility(visibility);
        if (cardJudges != null) cardJudges.setVisibility(visibility);
        if (cardMonarchy != null) cardMonarchy.setVisibility(visibility);
        if (cardDivisionExile != null) cardDivisionExile.setVisibility(visibility);
        if (cardReturnMessiah != null) cardReturnMessiah.setVisibility(visibility);
        if (cardBirthMinistry != null) cardBirthMinistry.setVisibility(visibility);
        if (cardConflictDeath != null) cardConflictDeath.setVisibility(visibility);
        if (cardResurrectionAscension != null) cardResurrectionAscension.setVisibility(visibility);
    }

    private void filterStories(String query) {
        handleCardVisibility(cardCreationFall, "Creation and the Fall", query);
        handleCardVisibility(cardFlood, "Early Humanity and the Flood", query);
        handleCardVisibility(cardBabelAbraham, "The Tower of Babel and Abraham's Covenant", query);
        handleCardVisibility(cardExodus, "Slavery in Egypt and the Exodus", query);
        handleCardVisibility(cardLawDesert, "The Law and Wandering in the Desert", query);
        handleCardVisibility(cardJudges, "Entering the Promised Land and the Judges", query);
        handleCardVisibility(cardMonarchy, "The Monarchy: Saul, David, and Solomon", query);
        handleCardVisibility(cardDivisionExile, "Division of the Kingdom and Exile", query);
        handleCardVisibility(cardReturnMessiah, "Return from Exile and Anticipation of the Messiah", query);
        handleCardVisibility(cardBirthMinistry, "The Birth and Ministry of Jesus", query);
        handleCardVisibility(cardConflictDeath, "Conflict with the Pharisees and Jesus's Death", query);
        handleCardVisibility(cardResurrectionAscension, "Resurrection and Ascension", query);
    }

    private void handleCardVisibility(View card, String title, String query) {
        if (card == null) return;
        if (title.toLowerCase().contains(query)) {
            card.setVisibility(View.VISIBLE);
        } else {
            card.setVisibility(View.GONE);
        }
    }
}
