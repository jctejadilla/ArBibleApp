package com.example.arbibleapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeacherProposeQuestionActivity extends AppCompatActivity {

    private LinearLayout llStorySelectionContainer, llFormContainer, llOptionsContainer;
    private TextView tvToolbarTitle, tvSelectedStoryHeader, tvSelectedStorySub, tvLabelOriginal, tvLabelProposed;
    private Spinner spOriginalQuestion, spCorrectAnswer;
    private EditText etProposedQuestion, etOptionA, etOptionB, etOptionC, etOptionD;
    private ImageView btnBack;

    private CardView cardStory1, cardStory2, cardStory3, cardStory4, cardStory5;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String selectedStoryTitle = "";
    private int selectedQuizType = 1;

    private static final String[] OPTIONS_KEYS = {
            "Option A (Choice 1)",
            "Option B (Choice 2)",
            "Option C (Choice 3)",
            "Option D (Choice 4)"
    };

    private static final String[] CREATION_QUESTIONS = {
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

    private static final String[] FLOOD_WORDS = {
            "BEAR", "BIRD", "CAT", "COW", "DOG", "DOVE", "FROG", "LION", "PIG", "SHEEP"
    };

    private static final String[] BABEL_FLOORS = {
            "Floor 1 - Foundation Block",
            "Floor 2 - Brick Layer Block",
            "Floor 3 - Mortar & Stone Block",
            "Floor 4 - Tower Scaffold Block",
            "Floor 5 - Middle Level Block",
            "Floor 6 - Upper Structure Block",
            "Floor 7 - Pillar Support Block",
            "Floor 8 - High Window Block",
            "Floor 9 - Tower Balcony Block",
            "Floor 10 - Tower Pinnacle Block"
    };

    private static final String[] EXODUS_STEPS = {
            "Step 1: Slavery in Egypt",
            "Step 2: Moses and Pharaoh (Plagues)",
            "Step 3: Crossing the Red Sea",
            "Step 4: Mt. Sinai and the Commandments",
            "Step 5: Entering the Promised Land"
    };

    private static final String[] LAW_SCENARIOS = {
            "Scenario 1: You find money on the ground that someone dropped.",
            "Scenario 2: Your parents ask you to clean your room.",
            "Scenario 3: Someone offers you an idol or false object to worship.",
            "Scenario 4: Your friend gets a brand new toy that you really want.",
            "Scenario 5: Someone tells a rumor or lie about your classmate.",
            "Scenario 6: You feel angry at your sibling during a game.",
            "Scenario 7: It is the Sabbath day of rest and worship.",
            "Scenario 8: You are tempted to take something that isn't yours.",
            "Scenario 9: You are tempted to use God's name disrespectfully.",
            "Scenario 10: You have a chance to help an elderly neighbor with groceries."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_teacher_propose_question);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle);

        llStorySelectionContainer = findViewById(R.id.llStorySelectionContainer);
        llFormContainer = findViewById(R.id.llFormContainer);
        llOptionsContainer = findViewById(R.id.llOptionsContainer);

        tvSelectedStoryHeader = findViewById(R.id.tvSelectedStoryHeader);
        tvSelectedStorySub = findViewById(R.id.tvSelectedStorySub);
        tvLabelOriginal = findViewById(R.id.tvLabelOriginal);
        tvLabelProposed = findViewById(R.id.tvLabelProposed);

        spOriginalQuestion = findViewById(R.id.spOriginalQuestion);
        etProposedQuestion = findViewById(R.id.etProposedQuestion);
        etOptionA = findViewById(R.id.etOptionA);
        etOptionB = findViewById(R.id.etOptionB);
        etOptionC = findViewById(R.id.etOptionC);
        etOptionD = findViewById(R.id.etOptionD);

        spCorrectAnswer = findViewById(R.id.spCorrectAnswer);

        cardStory1 = findViewById(R.id.cardStory1);
        cardStory2 = findViewById(R.id.cardStory2);
        cardStory3 = findViewById(R.id.cardStory3);
        cardStory4 = findViewById(R.id.cardStory4);
        cardStory5 = findViewById(R.id.cardStory5);

        ArrayAdapter<String> optAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_dark, OPTIONS_KEYS);
        optAdapter.setDropDownViewResource(R.layout.spinner_dropdown_dark);
        spCorrectAnswer.setAdapter(optAdapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            if (llFormContainer.getVisibility() == View.VISIBLE) {
                showStorySelectionStep();
            } else {
                finish();
            }
        });

        cardStory1.setOnClickListener(v -> selectStory("Creation and the Fall", 1));
        cardStory2.setOnClickListener(v -> selectStory("Early Humanity and the Flood", 2));
        cardStory3.setOnClickListener(v -> selectStory("The Tower of Babel and Abraham's Covenant", 3));
        cardStory4.setOnClickListener(v -> selectStory("Slavery in Egypt and the Exodus", 4));
        cardStory5.setOnClickListener(v -> selectStory("The Law and Wandering in the Desert", 5));

        Button btnSubmitProposal = findViewById(R.id.btnSubmitProposal);
        btnSubmitProposal.setOnClickListener(v -> submitProposal());
    }

    private void showStorySelectionStep() {
        llStorySelectionContainer.setVisibility(View.VISIBLE);
        llFormContainer.setVisibility(View.GONE);
        tvToolbarTitle.setText("Select Bible Story Quiz");
    }

    private void selectStory(String storyTitle, int quizType) {
        this.selectedStoryTitle = storyTitle;
        this.selectedQuizType = quizType;

        llStorySelectionContainer.setVisibility(View.GONE);
        llFormContainer.setVisibility(View.VISIBLE);

        tvToolbarTitle.setText("Configure Proposal");
        tvSelectedStoryHeader.setText(storyTitle);

        List<String> originalItems = new ArrayList<>();
        switch (quizType) {
            case 1: // Quiz 1: Creation and the Fall (Multiple Choice)
                originalItems.addAll(Arrays.asList(CREATION_QUESTIONS));
                tvSelectedStorySub.setText("Quiz 1 • Propose Multiple Choice Question Edits:");
                tvLabelOriginal.setText("Select Current Question to Edit");
                tvLabelProposed.setText("Proposed New Question Wording");
                llOptionsContainer.setVisibility(View.VISIBLE);
                etOptionA.setHint("Option A (Choice 1)");
                etOptionB.setHint("Option B (Choice 2)");
                etOptionC.setVisibility(View.VISIBLE);
                etOptionD.setVisibility(View.VISIBLE);
                etOptionC.setHint("Option C (Choice 3)");
                etOptionD.setHint("Option D (Choice 4)");
                break;

            case 2: // Quiz 2: Early Humanity and the Flood (Word Search)
                originalItems.addAll(Arrays.asList(FLOOD_WORDS));
                tvSelectedStorySub.setText("Quiz 2 • Propose Word Search Grid Word Edits:");
                tvLabelOriginal.setText("Select Current Word to Replace");
                tvLabelProposed.setText("Proposed New Word for Grid");
                llOptionsContainer.setVisibility(View.GONE);
                break;

            case 3: // Quiz 3: Tower of Babel & Abraham's Covenant (Tower Blocks)
                originalItems.addAll(Arrays.asList(BABEL_FLOORS));
                tvSelectedStorySub.setText("Quiz 3 • Propose Tower Floor Block Edits:");
                tvLabelOriginal.setText("Select Floor Level / Block");
                tvLabelProposed.setText("Proposed Correct Block / Word");
                llOptionsContainer.setVisibility(View.VISIBLE);
                etOptionA.setHint("Correct Block Label");
                etOptionB.setHint("Odd-One-Out Block Label");
                etOptionC.setVisibility(View.GONE);
                etOptionD.setVisibility(View.GONE);
                break;

            case 4: // Quiz 4: Slavery in Egypt and the Exodus (Image Sequence)
                originalItems.addAll(Arrays.asList(EXODUS_STEPS));
                tvSelectedStorySub.setText("Quiz 4 • Propose Image Sequence Step Edits:");
                tvLabelOriginal.setText("Select Sequence Step");
                tvLabelProposed.setText("Proposed Step Wording / Scene Description");
                llOptionsContainer.setVisibility(View.VISIBLE);
                etOptionA.setHint("Correct Choice Description");
                etOptionB.setHint("Wrong Choice Description");
                etOptionC.setVisibility(View.GONE);
                etOptionD.setVisibility(View.GONE);
                break;

            case 5: // Quiz 5: The Law and Wandering in the Desert (Scenario Decisions)
                originalItems.addAll(Arrays.asList(LAW_SCENARIOS));
                tvSelectedStorySub.setText("Quiz 5 • Propose Scenario Decision Edits:");
                tvLabelOriginal.setText("Select Current Scenario Wording");
                tvLabelProposed.setText("Proposed Scenario Description");
                llOptionsContainer.setVisibility(View.VISIBLE);
                etOptionA.setHint("Choice A (Obey Decision)");
                etOptionB.setHint("Choice B (Disobey Decision)");
                etOptionC.setVisibility(View.GONE);
                etOptionD.setVisibility(View.GONE);
                break;
        }

        ArrayAdapter<String> originalAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_dark, originalItems);
        originalAdapter.setDropDownViewResource(R.layout.spinner_dropdown_dark);
        spOriginalQuestion.setAdapter(originalAdapter);

        // Fetch live approved question configurations from Firestore to include in dropdown
        db.collection("quiz_questions")
                .whereEqualTo("storyTitle", storyTitle)
                .whereEqualTo("approvedByAdmin", true)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    if (querySnapshots != null && !querySnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshots) {
                            String origQ = doc.getString("originalQuestion");
                            String newQ = doc.getString("question");

                            if (newQ != null && !newQ.isEmpty()) {
                                boolean replaced = false;
                                if (origQ != null && !origQ.isEmpty()) {
                                    for (int i = 0; i < originalItems.size(); i++) {
                                        if (originalItems.get(i).equalsIgnoreCase(origQ) || originalItems.get(i).contains(origQ)) {
                                            originalItems.set(i, newQ + " [Approved Edit]");
                                            replaced = true;
                                            break;
                                        }
                                    }
                                }
                                if (!replaced && !originalItems.contains(newQ)) {
                                    originalItems.add(newQ + " [Approved Edit]");
                                }
                            }
                        }
                        originalAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void submitProposal() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        String origQ = spOriginalQuestion.getSelectedItem() != null ? spOriginalQuestion.getSelectedItem().toString() : "";
        String propQ = etProposedQuestion.getText().toString().trim();

        if (origQ.isEmpty() || propQ.isEmpty()) {
            Toast.makeText(this, "Please fill in content fields", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> options = new ArrayList<>();
        if (llOptionsContainer.getVisibility() == View.VISIBLE) {
            String optA = etOptionA.getText().toString().trim();
            String optB = etOptionB.getText().toString().trim();
            String optC = etOptionC.getText().toString().trim();
            String optD = etOptionD.getText().toString().trim();

            if (!optA.isEmpty()) options.add(optA);
            if (!optB.isEmpty()) options.add(optB);
            if (etOptionC.getVisibility() == View.VISIBLE && !optC.isEmpty()) options.add(optC);
            if (etOptionD.getVisibility() == View.VISIBLE && !optD.isEmpty()) options.add(optD);
        }

        int correctIndex = spCorrectAnswer.getSelectedItemPosition();

        db.collection("users").document(uid).get().addOnSuccessListener(userDoc -> {
            String teacherName = "Teacher";
            if (userDoc.exists()) {
                String name = userDoc.getString("username");
                if (name == null || name.isEmpty()) {
                    name = userDoc.getString("fullName");
                }
                if (name != null && !name.isEmpty()) teacherName = name;
            }

            Map<String, Object> proposal = new HashMap<>();
            proposal.put("teacherUid", uid);
            proposal.put("teacherName", teacherName);
            proposal.put("storyTitle", selectedStoryTitle);
            proposal.put("quizType", selectedQuizType);
            proposal.put("originalQuestion", origQ);
            proposal.put("proposedQuestion", propQ);
            proposal.put("options", options);
            proposal.put("correctAnswerIndex", correctIndex);
            proposal.put("status", "PENDING");
            proposal.put("timestamp", Timestamp.now());

            db.collection("question_edit_proposals").add(proposal)
                    .addOnSuccessListener(ref -> {
                        Toast.makeText(this, "Proposal Submitted for Admin Approval!", Toast.LENGTH_LONG).show();
                        NotificationPopupManager.getInstance().showNotification(
                                this,
                                "Proposal Submitted!",
                                "Edit proposal for " + selectedStoryTitle + " sent to Admin.",
                                android.R.drawable.ic_menu_edit
                        );
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error submitting proposal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    @Override
    public void onBackPressed() {
        if (llFormContainer.getVisibility() == View.VISIBLE) {
            showStorySelectionStep();
        } else {
            super.onBackPressed();
        }
    }
}