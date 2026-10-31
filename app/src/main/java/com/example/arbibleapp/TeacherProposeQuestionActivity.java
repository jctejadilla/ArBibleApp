package com.example.arbibleapp;

import android.graphics.Color;
import android.os.Bundle;
import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.firebase.firestore.Blob;

import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import androidx.appcompat.app.AlertDialog;
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
import androidx.core.content.ContextCompat;
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

    private ReturnProposalEditor returnEditor;
    private BirthProposalEditor birthEditor;
    private DivisionProposalEditor divisionEditor;
    private MonarchyProposalEditor monarchyEditor;
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

    private static final String[] CREATION_QUESTIONS = CreationQuestionBank.defaults().stream()
            .map(question -> question.question).toArray(String[]::new);

    private static final String[] FLOOD_WORDS = FloodWordBank.defaults();
    private String[] currentFloodWords = FloodWordBank.defaults();
    private boolean floodWordsReady = false;

    private Map<Integer, QuizBabelOverride> currentBabelEdits;
    private Map<Integer, QuizCaptionOverride> currentExodusEdits;

    private static final String[] BABEL_FLOORS = {
            "Floor 1 - Foundation Block",
            "Floor 2 - Brick Layer Block",
            "Floor 3 - Mortar & Stone Block",
            "Floor 4 - Tower Scaffold Block",
            "Floor 5 - Middle Level Block"
    };

    private static final int[] BABEL_IMAGES = {
            R.drawable.correct1, R.drawable.correct2, R.drawable.correct3, R.drawable.correct4, R.drawable.correct5,
            R.drawable.correct6, R.drawable.correct7, R.drawable.correct8, R.drawable.correct9, R.drawable.correct10
    };

    private static final int[] BABEL_ODD_IMAGES = {
            R.drawable.odd1, R.drawable.odd2, R.drawable.odd3, R.drawable.odd4, R.drawable.odd5
    };

    private static final String[] EXODUS_STEPS = {
            "Step 1: Slavery in Egypt",
            "Step 2: Moses and Pharaoh (Plagues)",
            "Step 3: Crossing the Red Sea",
            "Step 4: Mt. Sinai and the Commandments",
            "Step 5: Entering the Promised Land"
    };

    private List<LawScenarioBank.Scenario> currentLawScenarios = LawScenarioBank.defaults();
    private List<JudgesContentBank.Item> currentJudgesItems = JudgesContentBank.defaults();
    private EditText judgesDescription;
    private Spinner spLawClassification;
    private boolean lawReady = false, restoringQuiz = false;
    private ImageView ivLawImagePreview;
    private String lawImageAction = "KEEP";
    private byte[] lawImageBytes;
    private String babelImage1Action = "KEEP", babelImage2Action = "KEEP", babelImage3Action = "KEEP";
    private byte[] babelImage1Bytes, babelImage2Bytes, babelImage3Bytes;
    private int currentBabelPicker = -1; // 1, 2, or 3
    private Spinner spBabelOddBlock;
    private ImageView ivBabelImage1, ivBabelImage2, ivBabelImage3;
    private boolean imageProcessing = false, submittingProposal = false;
    private int imageVersion = 0, pickerVersion = -1;
    private final ActivityResultLauncher<String> lawImagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::processLawImage);
    private final ActivityResultLauncher<String> babelImagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::processBabelImage);

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
        monarchyEditor = new MonarchyProposalEditor(this, llFormContainer, spOriginalQuestion,
                etProposedQuestion, findViewById(R.id.btnSubmitProposal));
        divisionEditor = new DivisionProposalEditor(this, llFormContainer, spOriginalQuestion,
                etProposedQuestion, findViewById(R.id.btnSubmitProposal));
        returnEditor = new ReturnProposalEditor(this, llFormContainer, spOriginalQuestion,
                etProposedQuestion, findViewById(R.id.btnSubmitProposal));
        birthEditor = new BirthProposalEditor(this, llFormContainer, spOriginalQuestion,
                etProposedQuestion, findViewById(R.id.btnSubmitProposal));
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
        spLawClassification = findViewById(R.id.spLawClassification);
        spBabelOddBlock = findViewById(R.id.spBabelOddBlock);

        ArrayAdapter<String> babelAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_dark, new String[]{"Block 1", "Block 2", "Block 3"});
        babelAdapter.setDropDownViewResource(R.layout.spinner_dropdown_dark);
        spBabelOddBlock.setAdapter(babelAdapter);
        spBabelOddBlock.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (selectedQuizType == 3 && lawReady) {
                    showBabelImage(1);
                    showBabelImage(2);
                    showBabelImage(3);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        ivBabelImage1 = findViewById(R.id.ivBabelImage1);
        ivBabelImage2 = findViewById(R.id.ivBabelImage2);
        ivBabelImage3 = findViewById(R.id.ivBabelImage3);

        setupBabelImageButton(R.id.btnChooseBabel1, R.id.btnDefaultBabel1, 1);
        setupBabelImageButton(R.id.btnChooseBabel2, R.id.btnDefaultBabel2, 2);
        setupBabelImageButton(R.id.btnChooseBabel3, R.id.btnDefaultBabel3, 3);

        ivLawImagePreview = findViewById(R.id.ivLawImagePreview);
        judgesDescription = new EditText(this);
        judgesDescription.setHint("Picture description for accessibility (max 300 characters)");
        judgesDescription.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        judgesDescription.setTextColor(ContextCompat.getColor(this, R.color.primary_green));
        judgesDescription.setHintTextColor(Color.parseColor("#882E7D32"));
        judgesDescription.setTextSize(14f);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        judgesDescription.setPadding(padding, padding, padding, padding);
        judgesDescription.setMinHeight((int) (52 * getResources().getDisplayMetrics().density));
        judgesDescription.setBackgroundResource(R.drawable.bg_outlined_edittext);
        judgesDescription.setGravity(Gravity.TOP);
        judgesDescription.setLines(2);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, (int) (12 * getResources().getDisplayMetrics().density));
        judgesDescription.setLayoutParams(params);
        ((LinearLayout) findViewById(R.id.llLawControls)).addView(judgesDescription, 0);
        findViewById(R.id.btnChooseLawImage).setOnClickListener(v -> {
            if (((selectedQuizType == 5 || selectedQuizType == 6) && !lawReady) || imageProcessing || submittingProposal) return;
            pickerVersion = imageVersion;
            lawImagePicker.launch("image/*");
        });
        findViewById(R.id.btnDefaultLawImage).setOnClickListener(v -> {
            if (((selectedQuizType == 5 || selectedQuizType == 6) && !lawReady) || submittingProposal) return;
            imageVersion++;
            imageProcessing = false;
            lawImageAction = "DEFAULT";
            lawImageBytes = null;
            showLawImage();
            updateImageControls();
        });
        ArrayAdapter<String> lawAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_dark,
                new String[]{"Follows Law (swipe right)", "Breaks Law (swipe left)"});
        lawAdapter.setDropDownViewResource(R.layout.spinner_dropdown_dark);
        spLawClassification.setAdapter(lawAdapter);
        findViewById(R.id.btnRestoreQuizDefaults).setOnClickListener(v -> {
            final int quizType = selectedQuizType;
            new AlertDialog.Builder(this)
                    .setTitle("Reset " + QuizResetPolicy.name(quizType) + " to defaults?")
                    .setMessage("Request admin approval to reset all content changes for " +
                            QuizResetPolicy.story(quizType) + ". Other quizzes, current attempts, and past results will not change.")
                    .setPositiveButton("Send request", (dialog, which) -> requestQuizRestore(quizType))
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        cardStory1 = findViewById(R.id.cardStory1);
        cardStory2 = findViewById(R.id.cardStory2);
        cardStory3 = findViewById(R.id.cardStory3);
        cardStory4 = findViewById(R.id.cardStory4);
        cardStory5 = findViewById(R.id.cardStory5);

        ArrayAdapter<String> optAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_dark, OPTIONS_KEYS);
        optAdapter.setDropDownViewResource(R.layout.spinner_dropdown_dark);
        spCorrectAnswer.setAdapter(optAdapter);
    }

    private void setupBabelImageButton(int chooseId, int defaultId, int babelIndex) {
        findViewById(chooseId).setOnClickListener(v -> {
            if (((selectedQuizType == 3 || selectedQuizType == 4) && !lawReady) || imageProcessing || submittingProposal) return;
            pickerVersion = imageVersion;
            currentBabelPicker = babelIndex;
            babelImagePicker.launch("image/*");
        });
        findViewById(defaultId).setOnClickListener(v -> {
            if (((selectedQuizType == 3 || selectedQuizType == 4) && !lawReady) || submittingProposal) return;
            imageVersion++;
            imageProcessing = false;
            if (babelIndex == 1) { babelImage1Action = "DEFAULT"; babelImage1Bytes = null; }
            else if (babelIndex == 2) { babelImage2Action = "DEFAULT"; babelImage2Bytes = null; }
            else if (babelIndex == 3) { babelImage3Action = "DEFAULT"; babelImage3Bytes = null; }
            showBabelImage(babelIndex);
            updateImageControls();
        });
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            if (llFormContainer.getVisibility() == View.VISIBLE) {
                showStorySelectionStep();
            } else {
                finish();
            }
        });

        findViewById(R.id.cardStory6).setOnClickListener(v -> selectStory(JudgesQuizGame.STORY_TITLE, 6));
        findViewById(R.id.cardStory7).setOnClickListener(v -> selectStory(MonarchyScenarioBank.STORY_TITLE, 7));
        findViewById(R.id.cardStory8).setOnClickListener(v -> selectStory(DivisionPairBank.STORY_TITLE, 8));
        findViewById(R.id.cardStory9).setOnClickListener(v -> selectStory(ReturnMatchingGame.STORY_TITLE, 9));
        findViewById(R.id.cardStory10).setOnClickListener(v -> selectStory(BirthWordBank.STORY_TITLE, 10));
        cardStory1.setOnClickListener(v -> selectStory("Creation and the Fall", 1));
        cardStory2.setOnClickListener(v -> selectStory("Early Humanity and the Flood", 2));
        cardStory3.setOnClickListener(v -> selectStory("The Tower of Babel and Abraham's Covenant", 3));
        cardStory4.setOnClickListener(v -> selectStory("Slavery in Egypt and the Exodus", 4));
        cardStory5.setOnClickListener(v -> selectStory("The Law and Wandering in the Desert", 5));

        Button btnSubmitProposal = findViewById(R.id.btnSubmitProposal);
        btnSubmitProposal.setOnClickListener(v -> submitProposal());
    }

    private void showStorySelectionStep() {
        if (monarchyEditor != null) monarchyEditor.deactivate();
        if (divisionEditor != null) divisionEditor.deactivate();
        if (returnEditor != null) returnEditor.deactivate();
        if (birthEditor != null) birthEditor.deactivate();
        llStorySelectionContainer.setVisibility(View.VISIBLE);
        llFormContainer.setVisibility(View.GONE);
        tvToolbarTitle.setText("Select Bible Story Quiz");
    }

    private void selectStory(String storyTitle, int quizType) {
        monarchyEditor.deactivate();
        divisionEditor.deactivate();
        returnEditor.deactivate();
        birthEditor.deactivate();
        this.selectedStoryTitle = storyTitle;
        this.selectedQuizType = quizType;
        imageVersion++;
        imageProcessing = false;
        lawImageAction = "KEEP";
        lawImageBytes = null;
        babelImage1Action = "KEEP"; babelImage1Bytes = null;
        babelImage2Action = "KEEP"; babelImage2Bytes = null;
        babelImage3Action = "KEEP"; babelImage3Bytes = null;
        if (ivLawImagePreview != null) ivLawImagePreview.setTag(null);
        if (ivBabelImage1 != null) ivBabelImage1.setTag(null);
        if (ivBabelImage2 != null) ivBabelImage2.setTag(null);
        if (ivBabelImage3 != null) ivBabelImage3.setTag(null);
        updateImageControls();
        ((Button) findViewById(R.id.btnRestoreQuizDefaults)).setText(
                "Request reset " + QuizResetPolicy.name(quizType) + " to defaults");
        spOriginalQuestion.setOnItemSelectedListener(null);
        judgesDescription.setVisibility(quizType == 6 ? View.VISIBLE : View.GONE);
        findViewById(R.id.llLawControls).setVisibility(quizType == 5 || quizType == 6 ? View.VISIBLE : View.GONE);
        findViewById(R.id.llBabelControls).setVisibility(quizType == 3 || quizType == 4 ? View.VISIBLE : View.GONE);
        spBabelOddBlock.setVisibility(quizType == 3 ? View.VISIBLE : View.GONE);
        findViewById(R.id.tvBabelOddLabel).setVisibility(quizType == 3 ? View.VISIBLE : View.GONE);
        int[] labels = {R.id.tvThreeImageLabel1, R.id.tvThreeImageLabel2, R.id.tvThreeImageLabel3};
        ImageView[] previews = {ivBabelImage1, ivBabelImage2, ivBabelImage3};
        for (int i = 0; i < 3; i++) {
            String label = quizType == 4 ? ExodusImageDefaults.LABELS[i] : "Block " + (i + 1) + " image";
            ((TextView) findViewById(labels[i])).setText(label);
            previews[i].setContentDescription(label + " preview");
        }
        findViewById(R.id.spLawClassification).setVisibility(quizType == 5 ? View.VISIBLE : View.GONE);
        findViewById(R.id.tvLawClassificationLabel).setVisibility(quizType == 5 ? View.VISIBLE : View.GONE);

        llStorySelectionContainer.setVisibility(View.GONE);
        llFormContainer.setVisibility(View.VISIBLE);

        tvToolbarTitle.setText("Configure Proposal");
        // Image quizzes use fixed choice roles, not a selectable answer index.
        spCorrectAnswer.setVisibility(quizType == 3 || quizType == 4 || quizType == 6 ? View.GONE : View.VISIBLE);
        findViewById(R.id.tvCorrectAnswerLabel).setVisibility(
                quizType == 3 || quizType == 4 || quizType == 6 ? View.GONE : View.VISIBLE);
        tvSelectedStoryHeader.setText(storyTitle);

        for (int id : new int[]{R.id.etExodusDescription1, R.id.etExodusDescription2, R.id.etExodusDescription3})
            findViewById(id).setVisibility(quizType == 4 ? View.VISIBLE : View.GONE);
        tvLabelProposed.setVisibility(View.VISIBLE);
        etProposedQuestion.setVisibility(View.VISIBLE);

        TextView tvOptionsLegend = findViewById(R.id.tvOptionsLegend);
        List<String> originalItems = new ArrayList<>();
        if (quizType == 10) {
            spCorrectAnswer.setVisibility(View.GONE);
            findViewById(R.id.tvCorrectAnswerLabel).setVisibility(View.GONE);
            llOptionsContainer.setVisibility(View.GONE);
            tvSelectedStorySub.setText("Quiz 10 • Propose Picture Word Quiz Edits:");
            if (tvOptionsLegend != null) {
                tvOptionsLegend.setText("• Image limit: Provide a picture for the word to guess.\n• Word length limit: 3-10 characters for the display name.\n• Restrictions: Only English letters (A-Z) allowed for the answer word.");
            }
            tvLabelOriginal.setText("Select Picture Question");
            tvLabelProposed.setText("Display name (max 80 characters)");
            birthEditor.load();
            return;
        }
        if (quizType == 9) {
            spCorrectAnswer.setVisibility(View.GONE);
            findViewById(R.id.tvCorrectAnswerLabel).setVisibility(View.GONE);
            llOptionsContainer.setVisibility(View.GONE);
            tvSelectedStorySub.setText("Quiz 9 • Propose Flip and Match Edits:");
            if (tvOptionsLegend != null) {
                tvOptionsLegend.setText("• Image limit: A single picture serves as a pair of identical cards on the board.\n• Label length limit: Max 80 characters for the label displayed upon match.");
            }
            tvLabelOriginal.setText("Select Picture Pair");
            tvLabelProposed.setText("Subject label (max 80 characters)");
            returnEditor.load();
            return;
        }
        if (quizType == 8) {
            spCorrectAnswer.setVisibility(View.GONE);
            findViewById(R.id.tvCorrectAnswerLabel).setVisibility(View.GONE);
            llOptionsContainer.setVisibility(View.GONE);
            tvSelectedStorySub.setText("Quiz 8 • Propose Pair Matching Edits:");
            if (tvOptionsLegend != null) {
                tvOptionsLegend.setText("• Content limit: Each pair requires 1 Character image and 1 Object image.\n• Name length limit: Max 80 characters per item name.\n• Matching condition: The pair must correspond correctly.");
            }
            tvLabelOriginal.setText("Select Pair by Round");
            tvLabelProposed.setText("Character name (max 80 characters)");
            divisionEditor.load();
            return;
        }
        if (quizType == 7) {
            spCorrectAnswer.setVisibility(View.GONE);
            findViewById(R.id.tvCorrectAnswerLabel).setVisibility(View.GONE);
            llOptionsContainer.setVisibility(View.GONE);
            tvSelectedStorySub.setText("Quiz 7 • Propose Story Sequencing Scenario Edits:");
            if (tvOptionsLegend != null) {
                tvOptionsLegend.setText("• Sequence limit: Exactly 6 scenario scenes representing a timeline.\n• Description length limit: Max 300 characters for each scene.\n• Positioning limit: The 6 elements must be in correct chronological order.");
            }
            tvLabelOriginal.setText("Select King");
            tvLabelProposed.setText("Scenario title (max 120 characters)");
            monarchyEditor.load();
            return;
        }
        switch (quizType) {
            case 1: // Quiz 1: Creation and the Fall (Multiple Choice)
                originalItems.addAll(Arrays.asList(CREATION_QUESTIONS));
                tvSelectedStorySub.setText("Quiz 1 • Propose Multiple Choice Question Edits:");
                if (tvOptionsLegend != null) {
                    tvOptionsLegend.setText("• Choices limit: Exactly 4 options required per question (Options A, B, C, D).\n• Choice length limit: Max 100 characters per option.\n• Correct answer limit: Select 1 correct option index (Choice 1 to 4).");
                }
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
                if (tvOptionsLegend != null) {
                    tvOptionsLegend.setText("• Word count limit: Exactly 1 word replacement per proposal.\n• Character limit: Max 8 uppercase letters (A-Z only, no spaces or special characters).\n• Matrix limit: The grid is 8x8, expanding to 10x10 when needed to fit all words.");
                }
                tvLabelOriginal.setText("Select Current Word to Replace");
                tvLabelProposed.setText("Proposed New Word for Grid");
                llOptionsContainer.setVisibility(View.GONE);
                break;

            case 3: // Quiz 3: Tower of Babel & Abraham's Covenant (Tower Blocks)
                lawReady = false;
                currentBabelEdits = null;
                originalItems.addAll(Arrays.asList(BABEL_FLOORS));
                tvSelectedStorySub.setText("Quiz 3 • Propose Tower Floor Block Edits:");
                if (tvOptionsLegend != null) {
                    tvOptionsLegend.setText("• Image limit: Exactly 3 images per floor.\n• Select which block is the 'odd one out' using the dropdown.");
                }
                tvLabelOriginal.setText("Select Floor Level / Block");
                tvLabelProposed.setVisibility(View.GONE);
                etProposedQuestion.setVisibility(View.GONE);
                llOptionsContainer.setVisibility(View.GONE);
                break;

            case 4: // Quiz 4: Slavery in Egypt and the Exodus (Image Sequence)
                lawReady = false;
                currentExodusEdits = null;
                originalItems.addAll(Arrays.asList(EXODUS_STEPS));
                tvSelectedStorySub.setText("Quiz 4 • Propose Image Sequence Step Edits:");
                if (tvOptionsLegend != null) {
                    tvOptionsLegend.setText("• Choices limit: Exactly 2 options per sequence step.\n• Length limit: Max 60 characters per step choice description.\n• Correct answer limit: Option A = Correct Sequence Step, Option B = Wrong Step.");
                }
                tvLabelOriginal.setText("Select Sequence Step");
                tvLabelProposed.setVisibility(View.GONE);
                etProposedQuestion.setVisibility(View.GONE);
                llOptionsContainer.setVisibility(View.GONE);
                etOptionA.setHint("Correct Choice Description (Option A)");
                etOptionB.setHint("Wrong Choice Description (Option B)");
                etOptionC.setVisibility(View.GONE);
                etOptionD.setVisibility(View.GONE);
                break;

            case 6:
                currentJudgesItems = JudgesContentBank.defaults();
                lawReady = false;
                for (JudgesContentBank.Item item : currentJudgesItems) originalItems.add(item.word);
                tvSelectedStorySub.setText("Quiz 6 • Propose Word Scramble Picture Edits:");
                tvLabelOriginal.setText("Select Character or Object");
                tvLabelProposed.setText("Answer word (4-12 letters, no spaces)");
                if (tvOptionsLegend != null) {
                    tvOptionsLegend.setText("• Keep the character or object group.\n• Use a unique word with different interior letters for an easy scramble.\n• Word length limit: 4-12 letters.");
                }
                llOptionsContainer.setVisibility(View.GONE);
                break;
            case 5:
                currentLawScenarios = LawScenarioBank.defaults();
                lawReady = false;
                for (LawScenarioBank.Scenario scenario : currentLawScenarios) {
                    originalItems.add(scenario.description);
                }
                tvSelectedStorySub.setText("Quiz 5 - Propose scenario classification edits");
                tvLabelOriginal.setText("Select Current Scenario");
                tvLabelProposed.setText("Proposed Scenario Description");
                llOptionsContainer.setVisibility(View.GONE);
                break;
        }

        ArrayAdapter<String> originalAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_dark, originalItems);
        originalAdapter.setDropDownViewResource(R.layout.spinner_dropdown_dark);
        spOriginalQuestion.setAdapter(originalAdapter);
        updateImageControls();
        if (quizType == 3 || quizType == 4) {
            showBabelImage(1);
            showBabelImage(2);
            showBabelImage(3);
        }

        if (quizType == 6) {
            showLawImage();
            spOriginalQuestion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> p, View v, int position, long id) {
                    if (selectedQuizType == 6 && lawReady) showJudgesItem(position);
                }
                @Override public void onNothingSelected(AdapterView<?> p) { }
            });
            db.collection("quiz_questions").whereEqualTo("storyTitle", storyTitle)
                    .whereEqualTo("approvedByAdmin", true).get().addOnSuccessListener(result -> {
                        if (isFinishing() || isDestroyed() || selectedQuizType != 6
                                || spOriginalQuestion.getAdapter() != originalAdapter) return;
                        currentJudgesItems = JudgesContentBank.load(result.getDocuments());
                        for (int i = 0; i < currentJudgesItems.size(); i++) originalItems.set(i, currentJudgesItems.get(i).word);
                        originalAdapter.notifyDataSetChanged();
                        lawReady = true;
                        showJudgesItem(spOriginalQuestion.getSelectedItemPosition());
                    }).addOnFailureListener(error -> {
                        if (!isFinishing() && !isDestroyed() && selectedQuizType == 6 && spOriginalQuestion.getAdapter() == originalAdapter)
                            Toast.makeText(this, "Could not load current content. Select Judges again to retry.", Toast.LENGTH_LONG).show();
                    });
            return;
        }

        if (quizType == 5) {
            spOriginalQuestion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (selectedQuizType == 5 && position >= 0 && position < currentLawScenarios.size()) {
                        showLawScenario(position);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) { }
            });
            db.collection("quiz_questions")
                    .whereEqualTo("storyTitle", LawScenarioBank.STORY_TITLE)
                    .whereEqualTo("approvedByAdmin", true)
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        if (isFinishing() || isDestroyed() || selectedQuizType != 5
                                || spOriginalQuestion.getAdapter() != originalAdapter) return;
                        currentLawScenarios = LawScenarioBank.load(snapshots.getDocuments());
                        for (int i = 0; i < currentLawScenarios.size(); i++) {
                            LawScenarioBank.Scenario scenario = currentLawScenarios.get(i);
                            originalItems.set(i, scenario.description + (scenario.overridden ? " [Approved Edit]" : ""));
                        }
                        originalAdapter.notifyDataSetChanged();
                        lawReady = true;
                        showLawScenario(spOriginalQuestion.getSelectedItemPosition());
                    })
                    .addOnFailureListener(e -> {
                        if (isFinishing() || isDestroyed() || selectedQuizType != 5
                                || spOriginalQuestion.getAdapter() != originalAdapter) return;
                        Toast.makeText(this, "Could not load current scenarios. Select this story again to retry.", Toast.LENGTH_LONG).show();
                    });
            return;
        }

        if (quizType == 2) {
            floodWordsReady = false;
            currentFloodWords = FloodWordBank.defaults();
            db.collection("quiz_questions")
                    .whereEqualTo("storyTitle", storyTitle)
                    .whereEqualTo("approvedByAdmin", true)
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        if (isFinishing() || isDestroyed() || selectedQuizType != 2
                                || spOriginalQuestion.getAdapter() != originalAdapter) return;
                        currentFloodWords = FloodWordBank.load(snapshots.getDocuments());
                        for (int i = 0; i < currentFloodWords.length; i++) {
                            String word = currentFloodWords[i];
                            originalItems.set(i, FLOOD_WORDS[i].equals(word)
                                    ? word : word + " [Approved Edit]");
                        }
                        floodWordsReady = true;
                        originalAdapter.notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> {
                        if (isFinishing() || isDestroyed() || selectedQuizType != 2
                                || spOriginalQuestion.getAdapter() != originalAdapter) return;
                        Toast.makeText(this, "Could not load current words. Select this story again to retry.", Toast.LENGTH_LONG).show();
                    });
            return;
        }

        if (quizType == 3 || quizType == 4) {
            db.collection("quiz_questions")
                    .whereEqualTo("storyTitle", storyTitle)
                    .whereEqualTo("approvedByAdmin", true)
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        if (isFinishing() || isDestroyed() || selectedQuizType != quizType || spOriginalQuestion.getAdapter() != originalAdapter) return;
                        if (quizType == 3) {
                            Map<Integer, QuizBabelOverride> edits = QuizBabelOverride.select(snapshots.getDocuments());
                            currentBabelEdits = edits;
                            for (Map.Entry<Integer, QuizBabelOverride> entry : edits.entrySet()) {
                                int index = entry.getKey();
                                originalItems.set(index, BABEL_FLOORS[index] + " [Approved Edit]");
                            }
                        } else {
                            Map<Integer, QuizCaptionOverride> edits = QuizCaptionOverride.select(snapshots.getDocuments(), quizType);
                            currentExodusEdits = edits;
                            for (Map.Entry<Integer, QuizCaptionOverride> entry : edits.entrySet()) {
                                int index = entry.getKey();
                                originalItems.set(index, EXODUS_STEPS[index] + " - " + entry.getValue().question + " [Approved Edit]");
                            }
                        }
                        originalAdapter.notifyDataSetChanged();

                        if (quizType == 3 || quizType == 4) {
                            lawReady = true;
                            spOriginalQuestion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                    if (selectedQuizType == quizType) {
                                        showBabelFloor(position);
                                    }
                                }
                                @Override
                                public void onNothingSelected(AdapterView<?> parent) { }
                            });
                            showBabelFloor(spOriginalQuestion.getSelectedItemPosition());
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (isFinishing() || isDestroyed() || selectedQuizType != quizType
                                || spOriginalQuestion.getAdapter() != originalAdapter) return;
                        Toast.makeText(this, "Could not load current configuration. Select this story again to retry.",
                                Toast.LENGTH_LONG).show();
                    });
            return;
        }

        db.collection("quiz_questions")
                .whereEqualTo("storyTitle", storyTitle)
                .whereEqualTo("approvedByAdmin", true)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (isFinishing() || isDestroyed() || selectedQuizType != 1
                            || spOriginalQuestion.getAdapter() != originalAdapter) return;
                    List<CreationQuestionBank.QuestionModel> questions =
                            CreationQuestionBank.load(snapshots.getDocuments());
                    for (int i = 0; i < questions.size(); i++) {
                        originalItems.set(i, questions.get(i).question);
                    }
                    originalAdapter.notifyDataSetChanged();
                });
    }
    private void processLawImage(Uri uri) {
        if (uri == null || (selectedQuizType != 5 && selectedQuizType != 6) || pickerVersion != imageVersion || !lawReady) return;
        final int version = imageVersion;
        imageProcessing = true;
        ((TextView) findViewById(R.id.tvLawImageStatus)).setText("Preparing image...");
        updateImageControls();
        LawImageCodec.WORKER.execute(() -> {
            try {
                byte[] bytes = LawImageCodec.prepare(getContentResolver(), uri);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed() || (selectedQuizType != 5 && selectedQuizType != 6) || version != imageVersion) return;
                    imageProcessing = false;
                    lawImageAction = "REPLACE";
                    lawImageBytes = bytes;
                    showLawImage();
                    updateImageControls();
                });
            } catch (Exception | OutOfMemoryError e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed() || version != imageVersion) return;
                    imageProcessing = false;
                    showLawImage();
                    updateImageControls();
                    Toast.makeText(this, "Could not prepare image. Choose another image.", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void processBabelImage(Uri uri) {
        if (uri == null || (selectedQuizType != 3 && selectedQuizType != 4) || pickerVersion != imageVersion || !lawReady) return;
        final int version = imageVersion;
        final int babelIndex = currentBabelPicker;
        imageProcessing = true;
        TextView statusView = babelIndex == 1 ? findViewById(R.id.tvBabelStatus1) : babelIndex == 2 ? findViewById(R.id.tvBabelStatus2) : findViewById(R.id.tvBabelStatus3);
        statusView.setText("Preparing image...");
        updateImageControls();
        LawImageCodec.WORKER.execute(() -> {
            try {
                byte[] bytes = LawImageCodec.prepare(getContentResolver(), uri);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed() || (selectedQuizType != 3 && selectedQuizType != 4) || version != imageVersion) return;
                    imageProcessing = false;
                    if (babelIndex == 1) { babelImage1Action = "REPLACE"; babelImage1Bytes = null; babelImage1Bytes = bytes; }
                    else if (babelIndex == 2) { babelImage2Action = "REPLACE"; babelImage2Bytes = null; babelImage2Bytes = bytes; }
                    else if (babelIndex == 3) { babelImage3Action = "REPLACE"; babelImage3Bytes = null; babelImage3Bytes = bytes; }
                    showBabelImage(babelIndex);
                    updateImageControls();
                });
            } catch (Exception | OutOfMemoryError e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed() || version != imageVersion) return;
                    imageProcessing = false;
                    showBabelImage(babelIndex);
                    updateImageControls();
                    Toast.makeText(this, "Could not prepare image. Choose another image.", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void updateImageControls() {
        boolean ready = lawReady;
        findViewById(R.id.btnSubmitProposal).setEnabled(!imageProcessing && !submittingProposal
                && ((selectedQuizType != 3 && selectedQuizType != 4 && selectedQuizType != 5 && selectedQuizType != 6) || ready));
        for (int id : new int[]{R.id.btnChooseBabel1, R.id.btnChooseBabel2, R.id.btnChooseBabel3})
            findViewById(id).setEnabled(ready && !imageProcessing && !submittingProposal);
        for (int id : new int[]{R.id.btnDefaultBabel1, R.id.btnDefaultBabel2, R.id.btnDefaultBabel3})
            findViewById(id).setEnabled(ready && !submittingProposal);
        findViewById(R.id.btnChooseLawImage).setEnabled(ready && !imageProcessing && !submittingProposal);
        findViewById(R.id.btnDefaultLawImage).setEnabled(ready && !submittingProposal);
    }

    private void showLawImage() {
        if (!lawReady) {
            ivLawImagePreview.setTag(null);
            ivLawImagePreview.setImageDrawable(null);
            ((TextView) findViewById(R.id.tvLawImageStatus)).setText("Current image loading or unavailable");
            return;
        }
        byte[] bytes = null;
        int imageRes = android.R.drawable.ic_menu_gallery;
        int position = spOriginalQuestion.getSelectedItemPosition();

        if (selectedQuizType == 5) {
            if (position >= 0 && position < currentLawScenarios.size()) {
                LawScenarioBank.Scenario scenario = currentLawScenarios.get(position);
                bytes = "REPLACE".equals(lawImageAction) ? lawImageBytes
                        : "KEEP".equals(lawImageAction) ? scenario.imageBytes : null;
                imageRes = scenario.imageRes;
            }
        }

        if (selectedQuizType == 6 && position >= 0 && position < currentJudgesItems.size()) {
            JudgesContentBank.Item item = currentJudgesItems.get(position);
            bytes = "REPLACE".equals(lawImageAction) ? lawImageBytes : "KEEP".equals(lawImageAction) ? item.imageBytes : null;
            imageRes = JudgesImages.resource(this, JudgesQuizGame.KEYS[position]);
        }
        LawImageCodec.display(ivLawImagePreview, bytes, imageRes);
        ((TextView) findViewById(R.id.tvLawImageStatus)).setText(
                "REPLACE".equals(lawImageAction) ? "New image selected"
                        : "DEFAULT".equals(lawImageAction) ? "Use the original scenario image"
                        : "Keep the currently approved image");
    }

    private void showBabelImage(int babelIndex) {
        ImageView preview = babelIndex == 1 ? ivBabelImage1 : babelIndex == 2 ? ivBabelImage2 : ivBabelImage3;
        TextView status = findViewById(babelIndex == 1 ? R.id.tvBabelStatus1 : babelIndex == 2 ? R.id.tvBabelStatus2 : R.id.tvBabelStatus3);
        if (!lawReady || spOriginalQuestion.getSelectedItemPosition() < 0) {
            preview.setTag(null);
            preview.setImageDrawable(null);
            status.setText("Current images loading or unavailable");
            return;
        }
        byte[] bytes = null;
        int imageRes = android.R.drawable.ic_menu_gallery; // Will be set by default logic if needed
        int position = spOriginalQuestion.getSelectedItemPosition();

        QuizBabelOverride edit = (currentBabelEdits != null && position >= 0) ? currentBabelEdits.get(position) : null;
        String action = babelIndex == 1 ? babelImage1Action : babelIndex == 2 ? babelImage2Action : babelImage3Action;
        byte[] rawBytes = babelIndex == 1 ? babelImage1Bytes : babelIndex == 2 ? babelImage2Bytes : babelImage3Bytes;

        LawImageData existingImage = null;
        if (edit != null) {
            existingImage = babelIndex == 1 ? edit.image1 : babelIndex == 2 ? edit.image2 : edit.image3;
        }

        if (selectedQuizType == 4) {
            QuizCaptionOverride exodus = currentExodusEdits == null ? null : currentExodusEdits.get(position);
            existingImage = exodus == null ? null : babelIndex == 1 ? exodus.image1
                    : babelIndex == 2 ? exodus.image2 : exodus.image3;
        }
        bytes = "REPLACE".equals(action) ? rawBytes
                : ("KEEP".equals(action) && existingImage != null && "REPLACE".equals(existingImage.action)) ? existingImage.bytes : null;

        imageRes = selectedQuizType == 4 ? ExodusImageDefaults.image(position, babelIndex - 1)
                : BabelImageDefaults.image(position, babelIndex - 1,
                Math.max(0, spBabelOddBlock.getSelectedItemPosition()));

        ImageView iv = babelIndex == 1 ? ivBabelImage1 : babelIndex == 2 ? ivBabelImage2 : ivBabelImage3;
        LawImageCodec.display(iv, bytes, imageRes);
        TextView tv = babelIndex == 1 ? findViewById(R.id.tvBabelStatus1) : babelIndex == 2 ? findViewById(R.id.tvBabelStatus2) : findViewById(R.id.tvBabelStatus3);
        tv.setText(
                "REPLACE".equals(action) ? "New image selected"
                        : "DEFAULT".equals(action) ? "Use the default image"
                        : "Keep the currently approved image");
    }

    private void showBabelFloor(int position) {
        if (position < 0 || position >= BABEL_FLOORS.length) return;
        QuizBabelOverride edit = currentBabelEdits != null ? currentBabelEdits.get(position) : null;
        if (edit != null) {
            spBabelOddBlock.setSelection(edit.oddBlockIndex);
        } else {
            spBabelOddBlock.setSelection(0);
        }
        if (selectedQuizType == 4) {
            QuizCaptionOverride exodus = currentExodusEdits == null ? null : currentExodusEdits.get(position);
            etProposedQuestion.setText(exodus == null ? ExodusImageDefaults.description(position) : exodus.question);
            etOptionA.setText(exodus == null ? ExodusImageDefaults.caption(position, true) : exodus.optionA);
            etOptionB.setText(exodus == null ? ExodusImageDefaults.caption(position, false) : exodus.optionB);
            ((EditText) findViewById(R.id.etExodusDescription1)).setText(etProposedQuestion.getText());
            ((EditText) findViewById(R.id.etExodusDescription2)).setText(etOptionA.getText());
            ((EditText) findViewById(R.id.etExodusDescription3)).setText(etOptionB.getText());
        }
        imageVersion++;
        imageProcessing = false;
        babelImage1Action = "KEEP"; babelImage1Bytes = null;
        babelImage2Action = "KEEP"; babelImage2Bytes = null;
        babelImage3Action = "KEEP"; babelImage3Bytes = null;
        showBabelImage(1);
        showBabelImage(2);
        showBabelImage(3);
        updateImageControls();
    }

    private void showJudgesItem(int position) {
        if (position < 0 || position >= currentJudgesItems.size()) return;
        JudgesContentBank.Item item = currentJudgesItems.get(position);
        etProposedQuestion.setText(item.word);
        judgesDescription.setText(item.description);
        imageVersion++;
        imageProcessing = false;
        lawImageAction = "KEEP";
        lawImageBytes = null;
        showLawImage();
        updateImageControls();
    }

    private void showLawScenario(int position) {
        if (position < 0 || position >= currentLawScenarios.size()) return;
        LawScenarioBank.Scenario scenario = currentLawScenarios.get(position);
        etProposedQuestion.setText(scenario.description);
        spLawClassification.setSelection(scenario.isGood ? 0 : 1);
        imageVersion++;
        imageProcessing = false;
        lawImageAction = "KEEP";
        lawImageBytes = null;
        showLawImage();
        updateImageControls();
    }

    private void requestQuizRestore(int quizType) {
        if (restoringQuiz || mAuth.getCurrentUser() == null) return;
        restoringQuiz = true;
        findViewById(R.id.btnRestoreQuizDefaults).setEnabled(false);
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(user -> {
            String name = user.getString("username");
            if (name == null || name.isEmpty()) name = user.getString("fullName");
            Map<String, Object> proposal = new HashMap<>();
            proposal.put("teacherUid", uid);
            proposal.put("teacherName", name == null || name.isEmpty() ? "Teacher" : name);
            proposal.put("storyTitle", QuizResetPolicy.story(quizType));
            proposal.put("quizType", quizType);
            proposal.put("operation", "RESTORE_ALL");
            proposal.put("originalQuestion", "All content in " + QuizResetPolicy.name(quizType));
            proposal.put("proposedQuestion", "Restore all " + QuizResetPolicy.name(quizType) + " defaults");
            proposal.put("options", new ArrayList<String>());
            proposal.put("status", "PENDING");
            proposal.put("timestamp", Timestamp.now());
            db.collection("question_edit_proposals").add(proposal)
                    .addOnSuccessListener(ref -> {
                        Toast.makeText(this, "Restore request sent for admin approval.", Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .addOnFailureListener(this::quizRestoreFailed);
        }).addOnFailureListener(this::quizRestoreFailed);
    }

    private void quizRestoreFailed(Exception e) {
        restoringQuiz = false;
        findViewById(R.id.btnRestoreQuizDefaults).setEnabled(true);
        Toast.makeText(this, "Could not submit restore request: " + e.getMessage(), Toast.LENGTH_LONG).show();
    }

    private void submitProposal() {
        if (selectedQuizType == 10) { birthEditor.submit(); return; }
        if (selectedQuizType == 9) { returnEditor.submit(); return; }
        if (selectedQuizType == 8) { divisionEditor.submit(); return; }
        if (selectedQuizType == 7) { monarchyEditor.submit(); return; }
        if (selectedQuizType == 4) {
            etProposedQuestion.setText(((EditText) findViewById(R.id.etExodusDescription1)).getText());
            etOptionA.setText(((EditText) findViewById(R.id.etExodusDescription2)).getText());
            etOptionB.setText(((EditText) findViewById(R.id.etExodusDescription3)).getText());
        }
        if (imageProcessing || submittingProposal) return;
        if ((selectedQuizType == 6 || selectedQuizType == 5 || selectedQuizType == 3 || selectedQuizType == 4) && !lawReady) {
            Toast.makeText(this, "Wait for the current scenarios to load before submitting.", Toast.LENGTH_LONG).show();
            return;
        }
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        String origQ = spOriginalQuestion.getSelectedItem() != null ? spOriginalQuestion.getSelectedItem().toString() : "";
        String rawQuestion = etProposedQuestion.getText().toString().trim();
        final String propQ = selectedQuizType == 2 ? FloodWordBank.normalize(rawQuestion) : selectedQuizType == 3 ? "" : rawQuestion;

        if (selectedQuizType != 3 && (origQ.isEmpty() || propQ.isEmpty())) {
            Toast.makeText(this, selectedQuizType == 2
                    ? "Enter 1-8 letters A-Z, with no spaces or symbols."
                    : "Please fill in content fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedQuizType == 2) {
            if (!floodWordsReady) {
                Toast.makeText(this, "Wait for the current word bank to load before submitting.", Toast.LENGTH_LONG).show();
                return;
            }
            if (FloodWordBank.duplicatesOtherSlot(currentFloodWords,
                    spOriginalQuestion.getSelectedItemPosition(), propQ)) {
                Toast.makeText(this, "That word is already used in another slot.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        final String pictureDescription = judgesDescription.getText().toString().trim();
        if (selectedQuizType == 6 && (!JudgesContentBank.validWord(propQ)
                || !JudgesContentBank.unique(currentJudgesItems, spOriginalQuestion.getSelectedItemPosition(), propQ)
                || pictureDescription.isEmpty() || pictureDescription.length() > 300)) {
            Toast.makeText(this, "Use a unique 4-12 letter word with different interior letters and a picture description of 1-300 characters.", Toast.LENGTH_LONG).show();
            return;
        }
        List<String> options = new ArrayList<>();
        if (selectedQuizType == 4 || llOptionsContainer.getVisibility() == View.VISIBLE) {
            String optA = etOptionA.getText().toString().trim();
            String optB = etOptionB.getText().toString().trim();
            String optC = etOptionC.getText().toString().trim();
            String optD = etOptionD.getText().toString().trim();

            if (selectedQuizType == 1) { // 4 choices required
                if (optA.isEmpty() || optB.isEmpty() || optC.isEmpty() || optD.isEmpty()) {
                    Toast.makeText(this, "Quiz 1 requires all 4 choices (Options A, B, C, D)!", Toast.LENGTH_LONG).show();
                    return;
                }
            } else if (selectedQuizType >= 4 && selectedQuizType <= 5) { // 2 choices required
                if (optA.isEmpty() || optB.isEmpty()) {
                    Toast.makeText(this, "This quiz requires both Option A and Option B choices!", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            if (!optA.isEmpty()) options.add(optA);
            if (!optB.isEmpty()) options.add(optB);
            if (etOptionC.getVisibility() == View.VISIBLE && !optC.isEmpty()) options.add(optC);
            if (etOptionD.getVisibility() == View.VISIBLE && !optD.isEmpty()) options.add(optD);
        }

        final int proposalQuizType = selectedQuizType;
        final String proposalStoryTitle = selectedStoryTitle;
        final int itemIndex = spOriginalQuestion.getSelectedItemPosition();
        final String originalQuestion = proposalQuizType == 1 ? CREATION_QUESTIONS[itemIndex]
                : proposalQuizType == 2 ? FLOOD_WORDS[itemIndex]
                : proposalQuizType == 3 ? BABEL_FLOORS[itemIndex]
                : proposalQuizType == 4 ? EXODUS_STEPS[itemIndex]
                : proposalQuizType == 5 ? LawScenarioBank.defaults().get(itemIndex).description : origQ;
        if (proposalQuizType == 4) {
            int limit = 60;
            if (options.get(0).length() > limit || options.get(1).length() > limit) {
                Toast.makeText(this, "Each caption must be at most " + limit + " characters.", Toast.LENGTH_LONG).show();
                return;
            }
        }
        int correctIndex = proposalQuizType == 3 ? spBabelOddBlock.getSelectedItemPosition()
                : proposalQuizType == 4 ? 0
                : proposalQuizType == 5 ? spLawClassification.getSelectedItemPosition()
                : spCorrectAnswer.getSelectedItemPosition();
        if (proposalQuizType == 5) options.addAll(Arrays.asList("Follows Law", "Breaks Law"));
        final String[] threeActions = {babelImage1Action, babelImage2Action, babelImage3Action};
        final byte[][] threeBytes = {
                babelImage1Bytes == null ? null : babelImage1Bytes.clone(),
                babelImage2Bytes == null ? null : babelImage2Bytes.clone(),
                babelImage3Bytes == null ? null : babelImage3Bytes.clone()
        };
        final String imageAction = lawImageAction;
        final byte[] imageBytes = lawImageBytes == null ? null : lawImageBytes.clone();
        submittingProposal = true;
        updateImageControls();

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
            proposal.put("storyTitle", proposalStoryTitle);
            proposal.put("quizType", proposalQuizType);
            proposal.put("itemIndex", itemIndex);
            proposal.put("operation", "EDIT");
            if (proposalQuizType == 6) proposal.put("description", pictureDescription);
            if (proposalQuizType == 5 || proposalQuizType == 6) {
                proposal.put("imageAction", imageAction);
                if ("REPLACE".equals(imageAction)) proposal.put("imageBytes", Blob.fromBytes(imageBytes));
            } else if (proposalQuizType == 3 || proposalQuizType == 4) {
                for (int i = 0; i < 3; i++) {
                    proposal.put("image" + (i + 1) + "Action", threeActions[i]);
                    if ("REPLACE".equals(threeActions[i]))
                        proposal.put("image" + (i + 1) + "Bytes", Blob.fromBytes(threeBytes[i]));
                }
                if (proposalQuizType == 3) proposal.put("oddBlockIndex", correctIndex);
            }
            proposal.put("originalQuestion", originalQuestion);
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
                    .addOnFailureListener(this::proposalFailed);
        }).addOnFailureListener(this::proposalFailed);
    }

    private void proposalFailed(Exception e) {
        submittingProposal = false;
        updateImageControls();
        Toast.makeText(this, "Error submitting proposal: " + e.getMessage(), Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        if (returnEditor != null) returnEditor.deactivate();
        if (birthEditor != null) birthEditor.deactivate();
        if (divisionEditor != null) divisionEditor.deactivate();
        if (monarchyEditor != null) monarchyEditor.deactivate();
        imageVersion++;
        if (ivLawImagePreview != null) ivLawImagePreview.setTag(null);
        super.onDestroy();
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