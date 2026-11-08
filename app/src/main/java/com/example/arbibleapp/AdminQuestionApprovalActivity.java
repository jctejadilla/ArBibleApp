package com.example.arbibleapp;

import android.os.Bundle;
import android.graphics.Bitmap;
import android.widget.ImageView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminQuestionApprovalActivity extends AppCompatActivity {

    private RecyclerView rvProposals;
    private TextView tvEmptyState;
    private FirebaseFirestore db;
    private ProposalAdapter adapter;
    private final List<ProposalModel> proposalList = new ArrayList<>();
    private ListenerRegistration proposalListener, lawImageListener, babelImageListener, exodusImageListener;
    private List<BirthContentBank.Item> currentBirthBank;
    private ListenerRegistration birthImageListener;
    private List<ReturnContentBank.Item> currentReturnBank;
    private ListenerRegistration returnImageListener;
    private List<DivisionContentBank.Pair> currentDivisionBank;
    private ListenerRegistration divisionImageListener;
    private List<MonarchyContentBank.Scenario> currentMonarchyBank;
    private ListenerRegistration monarchyImageListener;
    private List<JudgesContentBank.Item> currentJudgesBank;
    private ListenerRegistration judgesImageListener;
    private List<LawScenarioBank.Scenario> currentLawBank;
    private Map<Integer, QuizBabelOverride> currentBabelBank;
    private Map<Integer, QuizCaptionOverride> currentExodusBank;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_question_approval);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        tvEmptyState = findViewById(R.id.tvEmptyState);
        rvProposals = findViewById(R.id.rvProposals);
        rvProposals.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProposalAdapter(proposalList);
        rvProposals.setAdapter(adapter);

        loadPendingProposals();
        returnImageListener = db.collection("quiz_questions")
                .whereEqualTo("storyTitle", ReturnMatchingGame.STORY_TITLE)
                .whereEqualTo("approvedByAdmin", true).addSnapshotListener((snapshots, error) -> {
                    if (isFinishing() || isDestroyed()) return;
                    currentReturnBank = error == null && snapshots != null
                            ? ReturnContentBank.load(snapshots.getDocuments()) : null;
                    adapter.notifyDataSetChanged();
                });
        birthImageListener = db.collection("quiz_questions")
                .whereEqualTo("storyTitle", BirthWordBank.STORY_TITLE)
                .whereEqualTo("approvedByAdmin", true).addSnapshotListener((snapshots, error) -> {
                    if (isFinishing() || isDestroyed()) return;
                    currentBirthBank = error == null && snapshots != null
                            ? BirthContentBank.load(snapshots.getDocuments()) : null;
                    adapter.notifyDataSetChanged();
                });
        divisionImageListener = db.collection("quiz_questions")
                .whereEqualTo("storyTitle", DivisionPairBank.STORY_TITLE)
                .whereEqualTo("approvedByAdmin", true).addSnapshotListener((snapshots, error) -> {
                    if (isFinishing() || isDestroyed()) return;
                    currentDivisionBank = error == null && snapshots != null
                            ? DivisionContentBank.load(snapshots.getDocuments()) : null;
                    adapter.notifyDataSetChanged();
                });
        monarchyImageListener = db.collection("quiz_questions")
                .whereEqualTo("storyTitle", MonarchyScenarioBank.STORY_TITLE)
                .whereEqualTo("approvedByAdmin", true).addSnapshotListener((snapshots, error) -> {
                    if (isFinishing() || isDestroyed()) return;
                    currentMonarchyBank = error == null && snapshots != null
                            ? MonarchyContentBank.load(snapshots.getDocuments()) : null;
                    adapter.notifyDataSetChanged();
                });
        judgesImageListener = db.collection("quiz_questions")
                .whereEqualTo("storyTitle", JudgesQuizGame.STORY_TITLE)
                .whereEqualTo("approvedByAdmin", true).addSnapshotListener((snapshots, error) -> {
                    if (isFinishing() || isDestroyed()) return;
                    currentJudgesBank = error == null && snapshots != null
                            ? JudgesContentBank.load(snapshots.getDocuments()) : null;
                    adapter.notifyDataSetChanged();
                });
        exodusImageListener = db.collection("quiz_questions")
                .whereEqualTo("storyTitle", QuizResetPolicy.story(4))
                .whereEqualTo("approvedByAdmin", true)
                .addSnapshotListener((snapshots, e) -> {
                    if (isFinishing() || isDestroyed()) return;
                    currentExodusBank = e == null && snapshots != null
                            ? QuizCaptionOverride.select(snapshots.getDocuments(), 4) : null;
                    adapter.notifyDataSetChanged();
                });
        babelImageListener = db.collection("quiz_questions")
                .whereEqualTo("storyTitle", QuizResetPolicy.story(3))
                .whereEqualTo("approvedByAdmin", true)
                .addSnapshotListener((snapshots, e) -> {
                    if (isFinishing() || isDestroyed()) return;
                    currentBabelBank = e == null && snapshots != null
                            ? QuizBabelOverride.select(snapshots.getDocuments()) : null;
                    adapter.notifyDataSetChanged();
                });
        lawImageListener = db.collection("quiz_questions")
                .whereEqualTo("storyTitle", LawScenarioBank.STORY_TITLE)
                .whereEqualTo("approvedByAdmin", true)
                .addSnapshotListener((snapshots, e) -> {
                    if (isFinishing() || isDestroyed()) return;
                    currentLawBank = e == null && snapshots != null
                            ? LawScenarioBank.load(snapshots.getDocuments()) : null;
                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (proposalListener != null) proposalListener.remove();
        if (returnImageListener != null) returnImageListener.remove();
        if (birthImageListener != null) birthImageListener.remove();
        if (divisionImageListener != null) divisionImageListener.remove();
        if (monarchyImageListener != null) monarchyImageListener.remove();
        if (judgesImageListener != null) judgesImageListener.remove();
        if (lawImageListener != null) lawImageListener.remove();
        if (babelImageListener != null) babelImageListener.remove();
        if (exodusImageListener != null) exodusImageListener.remove();
        rvProposals.setAdapter(null);
    }

    private void loadPendingProposals() {
        if (proposalListener != null) proposalListener.remove();

        proposalListener = db.collection("question_edit_proposals")
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    proposalList.clear();
                    for (DocumentSnapshot doc : snapshots) {
                        String id = doc.getId();
                        String teacherName = doc.getString("teacherName");
                        String story = doc.getString("storyTitle");
                        String origQ = doc.getString("originalQuestion");
                        String propQ = doc.getString("proposedQuestion");
                        List<String> options = (List<String>) doc.get("options");
                        Long correctIdxLong = doc.getLong("correctAnswerIndex");
                        Timestamp ts = doc.getTimestamp("timestamp");

                        proposalList.add(new ProposalModel(
                                id,
                                teacherName != null ? teacherName : "Teacher",
                                story != null ? story : "Story Quiz",
                                origQ != null ? origQ : "",
                                propQ != null ? propQ : "",
                                options != null ? options : new ArrayList<>(),
                                correctIdxLong != null ? correctIdxLong.intValue() : 0,
                                ts, doc.getLong("itemIndex"), doc.getString("operation"), doc.getData()
                        ));
                    }

                    if (proposalList.isEmpty()) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                        rvProposals.setVisibility(View.GONE);
                    } else {
                        tvEmptyState.setVisibility(View.GONE);
                        rvProposals.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void approveProposal(ProposalModel proposal) {
        reviewProposal(proposal, true);
    }
    private void reviewProposal(ProposalModel proposal, boolean approve) {
        DocumentReference proposalRef = db.collection("question_edit_proposals").document(proposal.id);
        DocumentReference liveRef = db.collection("quiz_questions").document(proposal.id);
        db.runTransaction(transaction -> {
            DocumentSnapshot pending = transaction.get(proposalRef);
            DocumentSnapshot live = transaction.get(liveRef);
            if (!"PENDING".equals(pending.getString("status")) || live.exists()) return false;
            String story = pending.getString("storyTitle");
            int quizType = QuizResetPolicy.type(story);
            if (quizType == 0) throw new IllegalArgumentException("Unknown quiz.");
            Object declaredType = pending.get("quizType");
            if (declaredType != null && (!(declaredType instanceof Number)
                    || ((Number) declaredType).doubleValue() != quizType)) {
                throw new IllegalArgumentException("Quiz identity does not match the proposal.");
            }
            if (approve) {
                String operation = pending.getString("operation");
                if (operation == null) operation = "EDIT";
                Map<String, Object> content = new HashMap<>();
                content.put("storyTitle", story);
                content.put("operation", operation);
                content.put("quizType", quizType);
                content.put("approvedByAdmin", true);
                content.put("originalQuestion", pending.get("originalQuestion"));
                content.put("question", pending.get("proposedQuestion"));
                content.put("correctAnswerIndex", pending.get("correctAnswerIndex"));
                content.put("options", pending.get("options"));
                if (pending.contains("itemIndex")) content.put("itemIndex", pending.get("itemIndex"));
                if ((quizType == 5 || quizType == 6 || quizType == 9 || quizType == 10) && !"RESTORE_ALL".equals(operation)) {
                    LawImageData image = LawImageData.read(pending.get("imageAction"), pending.get("imageBytes"));
                    if (!image.valid) throw new IllegalArgumentException("Invalid Law image. Ask the teacher to choose it again.");
                    if ("REPLACE".equals(image.action)) {
                        Bitmap decoded = LawImageCodec.decode(image.bytes);
                        if (decoded == null) throw new IllegalArgumentException("Image is unreadable or exceeds 1024 pixels.");
                        decoded.recycle();
                        content.put("imageBytes", pending.get("imageBytes"));
                    }
                    content.put("imageAction", image.action);
                } else if ((quizType == 3 || quizType == 4) && !"RESTORE_ALL".equals(operation)) {
                    for (int slot = 1; slot <= 3; slot++) {
                        String actionKey = "image" + slot + "Action";
                        String bytesKey = "image" + slot + "Bytes";
                        LawImageData image = LawImageData.read(pending.get(actionKey), pending.get(bytesKey));
                        if (!image.valid) throw new IllegalArgumentException("Invalid image. Ask the teacher to choose it again.");
                        if ("REPLACE".equals(image.action)) {
                            Bitmap decoded = LawImageCodec.decode(image.bytes);
                            if (decoded == null) throw new IllegalArgumentException("Image is unreadable or exceeds 1024 pixels.");
                            decoded.recycle();
                            content.put(bytesKey, pending.get(bytesKey));
                        }
                        content.put(actionKey, image.action);
                    }
                    if (quizType == 3 && pending.contains("oddBlockIndex"))
                        content.put("oddBlockIndex", pending.get("oddBlockIndex"));
                }
                if (quizType == 6 && !"RESTORE_ALL".equals(operation)) {
                    content.put("description", pending.get("description"));
                    JudgesContentBank.Item item = JudgesContentBank.parse(content);
                    if (item == null || currentJudgesBank == null
                            || !JudgesContentBank.unique(currentJudgesBank, item.index, item.word))
                        throw new IllegalArgumentException("Invalid or duplicate Judges word. Reload current content and ask the teacher to revise the proposal.");
                }
                if (quizType == 7 && !"RESTORE_ALL".equals(operation)) {
                    content.put("scenes", pending.get("scenes"));
                    MonarchyContentBank.Scenario parsed = MonarchyContentBank.parse(content);
                    if (parsed == null) throw new IllegalArgumentException("Invalid Monarchy scenario or image configuration.");
                    for (MonarchyContentBank.Scene scene : parsed.scenes) {
                        if (scene.bytes == null) continue;
                        Bitmap bitmap = LawImageCodec.decode(scene.bytes);
                        if (bitmap == null) throw new IllegalArgumentException("Unreadable Monarchy picture. Ask the teacher to upload it again.");
                        bitmap.recycle();
                    }
                }
                if (quizType == 8 && !"RESTORE_ALL".equals(operation)) {
                    content.put("objectName", pending.get("objectName"));
                    for (int role = 1; role <= 2; role++) {
                        String action = "image" + role + "Action", bytes = "image" + role + "Bytes";
                        content.put(action, pending.get(action));
                        if (pending.contains(bytes)) content.put(bytes, pending.get(bytes));
                    }
                    DivisionContentBank.Pair pair = DivisionContentBank.parse(content);
                    if (pair == null || currentDivisionBank == null || !DivisionContentBank.unique(currentDivisionBank, pair))
                        throw new IllegalArgumentException("Invalid or duplicate pair names. Reload current content and ask the teacher to revise the proposal.");
                    for (byte[] bytes : pair.images) {
                        if (bytes == null) continue;
                        Bitmap bitmap = LawImageCodec.decode(bytes);
                        if (bitmap == null) throw new IllegalArgumentException("Unreadable Division picture. Ask the teacher to upload it again.");
                        bitmap.recycle();
                    }
                }
                if (quizType == 9 && !"RESTORE_ALL".equals(operation)) {
                    ReturnContentBank.Item item = ReturnContentBank.parse(content);
                    if (item == null || currentReturnBank == null || !ReturnContentBank.unique(currentReturnBank, item))
                        throw new IllegalArgumentException("Invalid or duplicate Return label. Reload current content and ask the teacher to revise the proposal.");
                }
                if (quizType == 10 && !"RESTORE_ALL".equals(operation)) {
                    content.put("answerWord", pending.get("answerWord"));
                    BirthContentBank.Item item = BirthContentBank.parse(content);
                    if (item == null || currentBirthBank == null || !BirthContentBank.unique(currentBirthBank, item))
                        throw new IllegalArgumentException("Invalid or duplicate Birth answer. Reload current content and ask the teacher to revise the proposal.");
                }
                // Validate before replacing this temporary timestamp with server time.
                content.put("timestamp", Timestamp.now());
                if (!"RESTORE_ALL".equals(operation) &&
                        (!"EDIT".equals(operation) || !QuizResetPolicy.validEdit(content, proposal.id, quizType))) {
                    throw new IllegalArgumentException("This proposal cannot be mapped safely. Ask the teacher to submit it again using the current form.");
                }
                content.put("timestamp", FieldValue.serverTimestamp());
                transaction.set(liveRef, content);
            }
            transaction.update(proposalRef, "status", approve ? "APPROVED" : "REJECTED",
                    "reviewedAt", FieldValue.serverTimestamp());
            return true;
        }).addOnSuccessListener(changed -> Toast.makeText(this,
                !changed ? "This proposal was already reviewed."
                        : approve ? ("RESTORE_ALL".equals(proposal.operation)
                                ? "All defaults restored for " + proposal.storyTitle + "." : "Quiz edit approved.")
                        : "Quiz proposal rejected.", Toast.LENGTH_LONG).show())
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Could not review proposal: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void disapproveProposal(ProposalModel proposal) {
        reviewProposal(proposal, false);
    }
    private static class ProposalModel {
        String id, teacherName, storyTitle, originalQuestion, proposedQuestion;
        List<String> options;
        int correctAnswerIndex;
        Timestamp timestamp;
        Long itemIndex;
        String operation;
        LawImageData image;
        LawScenarioBank.Edit lawEdit;
        Map<String, Object> raw;

        ProposalModel(String id, String teacherName, String storyTitle, String originalQuestion, String proposedQuestion, List<String> options, int correctAnswerIndex, Timestamp timestamp, Long itemIndex, String operation, Map<String, Object> raw) {
            this.id = id;
            this.teacherName = teacherName;
            this.storyTitle = storyTitle;
            this.originalQuestion = originalQuestion;
            this.proposedQuestion = proposedQuestion;
            this.options = options;
            this.correctAnswerIndex = correctAnswerIndex;
            this.timestamp = timestamp;
            this.itemIndex = itemIndex;
            this.operation = operation == null ? "EDIT" : operation;
            this.raw = raw;
            this.image = LawImageData.read(raw.get("imageAction"), raw.get("imageBytes"));
            if (LawScenarioBank.STORY_TITLE.equals(storyTitle) && !"RESTORE_ALL".equals(this.operation)) {
                Map<String, Object> preview = new HashMap<>(raw);
                preview.put("approvedByAdmin", true);
                preview.put("question", proposedQuestion);
                lawEdit = LawScenarioBank.parse(preview, id);
            }
        }
    }

    private class ProposalAdapter extends RecyclerView.Adapter<ProposalAdapter.ViewHolder> {
        private final List<ProposalModel> list;

        ProposalAdapter(List<ProposalModel> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_proposal, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ProposalModel p = list.get(position);
            holder.tvStoryBadge.setText(p.storyTitle);
            holder.tvTeacherName.setText("Proposed by: " + p.teacherName);
            holder.tvOriginalQuestion.setText(p.originalQuestion);
            holder.tvProposedQuestion.setText(p.proposedQuestion);

            StringBuilder optsBuilder = new StringBuilder();
            for (int i = 0; i < p.options.size(); i++) {
                String label = (char) ('A' + i) + ") " + p.options.get(i);
                if (i == p.correctAnswerIndex) {
                    label += " [CORRECT]";
                }
                optsBuilder.append(label);
                if (i < p.options.size() - 1) optsBuilder.append("\n");
            }
            holder.tvProposedOptions.setVisibility(View.VISIBLE);
            holder.tvProposedOptions.setText(optsBuilder.toString());
            boolean restoreQuiz = "RESTORE_ALL".equals(p.operation);
            if (restoreQuiz) {
                holder.tvOriginalQuestion.setText("All content in " + p.storyTitle);
                holder.tvProposedQuestion.setText("RESET TO DEFAULTS: " + p.storyTitle);
                holder.tvProposedOptions.setText("Resets all content changes for this quiz only. Later approvals can introduce new edits.");
            } else if (LawScenarioBank.STORY_TITLE.equals(p.storyTitle)) {
                holder.tvProposedOptions.setText("Correct classification: " +
                        (p.correctAnswerIndex == 0 ? "Follows Law (right)" : "Breaks Law (left)"));
            }
            holder.btnApprove.setText(restoreQuiz ? "Approve restore all" : "Approve");
            holder.ivLawProposalImage.setTag(null);
            holder.ivLawProposalImage.setImageDrawable(null);
            holder.ivLawProposalImage.setVisibility(View.GONE);
            holder.tvLawImageAction.setVisibility(View.GONE);
            holder.llBabelProposalImages.setVisibility(View.GONE);
            clearBabelImages(holder);

            holder.monarchyPreview.removeAllViews();
            holder.monarchyPreview.setVisibility(View.GONE);
            if (!restoreQuiz && MonarchyScenarioBank.STORY_TITLE.equals(p.storyTitle)) {
                holder.tvProposedOptions.setText("Proposed correct sequence (first to last)");
                Map<String,Object> preview = new HashMap<>(p.raw);
                preview.put("approvedByAdmin", true); preview.put("question", p.proposedQuestion);
                MonarchyContentBank.Scenario scenario = MonarchyContentBank.parse(preview);
                holder.monarchyPreview.setVisibility(View.VISIBLE);
                if (scenario == null) {
                    TextView error = new TextView(holder.itemView.getContext()); error.setText("Invalid scenario proposal");
                    holder.monarchyPreview.addView(error);
                } else {
                    List<?> scenes = (List<?>)p.raw.get("scenes");
                    for (int i = 0; i < 6; i++) {
                        MonarchyContentBank.Scene scene = scenario.scenes.get(i);
                        Map<?,?> row = (Map<?,?>)scenes.get(i);
                        LawImageData action = LawImageData.read(row.get("imageAction"), row.get("imageBytes"));
                        TextView caption = new TextView(holder.itemView.getContext());
                        caption.setText((i + 1) + ". " + scene.caption + "\nImage: " + action.action);
                        holder.monarchyPreview.addView(caption);
                        ImageView image = new ImageView(holder.itemView.getContext());
                        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        holder.monarchyPreview.addView(image, new LinearLayout.LayoutParams(-1,
                                Math.round(140 * holder.itemView.getResources().getDisplayMetrics().density)));
                        byte[] bytes = scene.bytes;
                        if ("KEEP".equals(action.action)) {
                            if (currentMonarchyBank == null) { caption.append(" (current preview unavailable)"); continue; }
                            bytes = MonarchyContentBank.find(currentMonarchyBank.get(scenario.index), scene.key).bytes;
                        }
                        if (bytes == null) MonarchyImages.show(image, scene.key);
                        else LawImageCodec.display(image, bytes, 0);
                    }
                }
            }
            if (!restoreQuiz && DivisionPairBank.STORY_TITLE.equals(p.storyTitle)) {
                holder.tvProposedOptions.setText("Matching object: " + p.raw.get("objectName"));
                Map<String,Object> preview = new HashMap<>(p.raw);
                preview.put("approvedByAdmin", true); preview.put("question", p.proposedQuestion);
                DivisionContentBank.Pair pair = DivisionContentBank.parse(preview);
                holder.monarchyPreview.setVisibility(View.VISIBLE);
                if (pair == null) {
                    TextView error = new TextView(holder.itemView.getContext()); error.setText("Invalid pair proposal");
                    holder.monarchyPreview.addView(error);
                } else {
                    for (int role = 0; role < 2; role++) {
                        LawImageData action = LawImageData.read(p.raw.get("image" + (role + 1) + "Action"),
                                p.raw.get("image" + (role + 1) + "Bytes"));
                        TextView label = new TextView(holder.itemView.getContext());
                        label.setText((role == 0 ? "Character: " + pair.characterName : "Object: " + pair.objectName)
                                + "\nPicture: " + ("REPLACE".equals(action.action) ? "uploaded replacement"
                                : "DEFAULT".equals(action.action) ? "original" : "keep current"));
                        holder.monarchyPreview.addView(label);
                        ImageView image = new ImageView(holder.itemView.getContext());
                        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        holder.monarchyPreview.addView(image, new LinearLayout.LayoutParams(-1,
                                Math.round(140 * holder.itemView.getResources().getDisplayMetrics().density)));
                        byte[] bytes = pair.images[role];
                        if ("KEEP".equals(action.action)) {
                            if (currentDivisionBank == null) { label.append(" (preview unavailable)"); continue; }
                            bytes = currentDivisionBank.get(pair.index).images[role];
                        }
                        LawImageCodec.display(image, bytes, DivisionImages.resource(pair.index, role));
                    }
                }
            }
            if (!restoreQuiz && ReturnMatchingGame.STORY_TITLE.equals(p.storyTitle)) {
                holder.tvProposedOptions.setText("The same picture is used on both cards in this pair.");
                Map<String,Object> preview = new HashMap<>(p.raw);
                preview.put("approvedByAdmin", true); preview.put("question", p.proposedQuestion);
                ReturnContentBank.Item item = ReturnContentBank.parse(preview);
                holder.tvLawImageAction.setVisibility(View.VISIBLE);
                holder.tvLawImageAction.setText(item == null ? "Invalid Return proposal"
                        : "REPLACE".equals(p.image.action) ? "Picture: uploaded replacement for both cards"
                        : "DEFAULT".equals(p.image.action) ? "Picture: original for both cards" : "Picture: keep current for both cards");
                if (item != null) {
                    byte[] bytes = item.imageBytes;
                    boolean ready = true;
                    if ("KEEP".equals(p.image.action)) {
                        ready = currentReturnBank != null;
                        if (ready) bytes = currentReturnBank.get(item.index).imageBytes;
                        else holder.tvLawImageAction.append(" (preview unavailable)");
                    }
                    if (ready) {
                        holder.ivLawProposalImage.setVisibility(View.VISIBLE);
                        if (bytes == null) ReturnImages.show(holder.ivLawProposalImage, ReturnMatchingGame.KEYS[item.index]);
                        else LawImageCodec.display(holder.ivLawProposalImage, bytes, 0);
                    }
                }
            }
            if (!restoreQuiz && BirthWordBank.STORY_TITLE.equals(p.storyTitle)) {
                holder.tvProposedOptions.setText("Answer: " + p.raw.get("originalAnswerWord") + " ? " + p.raw.get("answerWord"));
                Map<String,Object> preview = new HashMap<>(p.raw);
                preview.put("approvedByAdmin", true); preview.put("question", p.proposedQuestion);
                BirthContentBank.Item item = BirthContentBank.parse(preview);
                holder.tvLawImageAction.setVisibility(View.VISIBLE);
                holder.tvLawImageAction.setText(item == null ? "Invalid Birth proposal"
                        : "REPLACE".equals(p.image.action) ? "Picture: uploaded replacement"
                        : "DEFAULT".equals(p.image.action) ? "Picture: original" : "Picture: keep current");
                if (item != null) {
                    byte[] bytes = item.imageBytes;
                    boolean ready = true;
                    if ("KEEP".equals(p.image.action)) {
                        ready = currentBirthBank != null;
                        if (ready) bytes = currentBirthBank.get(item.index).imageBytes;
                        else holder.tvLawImageAction.append(" (preview unavailable)");
                    }
                    if (ready) {
                        holder.ivLawProposalImage.setVisibility(View.VISIBLE);
                        if (bytes == null) BirthImages.show(holder.ivLawProposalImage, BirthWordBank.KEYS[item.index]);
                        else LawImageCodec.display(holder.ivLawProposalImage, bytes, 0);
                    }
                }
            }
            boolean isLaw = LawScenarioBank.STORY_TITLE.equals(p.storyTitle);
            boolean isBabel = "The Tower of Babel and Abraham's Covenant".equals(p.storyTitle);
            boolean isExodus = "Slavery in Egypt and the Exodus".equals(p.storyTitle);
            if (!restoreQuiz && JudgesQuizGame.STORY_TITLE.equals(p.storyTitle)) {
                Map<String, Object> preview = new HashMap<>(p.raw);
                preview.put("approvedByAdmin", true);
                preview.put("question", p.proposedQuestion);
                JudgesContentBank.Item item = JudgesContentBank.parse(preview);
                holder.tvProposedOptions.setText("Picture description: " + p.raw.get("description"));
                holder.tvLawImageAction.setVisibility(View.VISIBLE);
                holder.tvLawImageAction.setText(item == null ? "Invalid Judges content"
                        : "REPLACE".equals(p.image.action) ? "Image: uploaded replacement"
                        : "DEFAULT".equals(p.image.action) ? "Image: original"
                        : "Image: keep current approved image");
                if (item != null) {
                    byte[] bytes = "REPLACE".equals(p.image.action) ? p.image.bytes : null;
                    boolean ready = !"KEEP".equals(p.image.action) || currentJudgesBank != null;
                    if ("KEEP".equals(p.image.action) && currentJudgesBank != null)
                        bytes = currentJudgesBank.get(item.index).imageBytes;
                    if (ready) {
                        holder.ivLawProposalImage.setVisibility(View.VISIBLE);
                        if (bytes == null) {
                            int resourceId = JudgesImages.resource(holder.itemView.getContext(), JudgesQuizGame.KEYS[item.index]);
                            LawImageCodec.display(holder.ivLawProposalImage, null, resourceId);
                        } else {
                            LawImageCodec.display(holder.ivLawProposalImage, bytes, 0);
                        }
                    } else holder.tvLawImageAction.setText("Image: keep current (preview unavailable)");
                }
            } else if (!restoreQuiz && isLaw) {
                holder.tvLawImageAction.setVisibility(View.VISIBLE);
                holder.tvLawImageAction.setText(!p.image.valid ? "Invalid image - submit a new proposal"
                        : "REPLACE".equals(p.image.action) ? "Image: replace with uploaded image"
                        : "DEFAULT".equals(p.image.action) ? "Image: restore original"
                        : "Image: keep current approved image");

                if (p.lawEdit != null) {
                    LawScenarioBank.Scenario original = LawScenarioBank.defaults().get(p.lawEdit.index);
                    byte[] bytes = "REPLACE".equals(p.image.action) ? p.image.bytes : null;
                    boolean ready = true;
                    if ("KEEP".equals(p.image.action)) {
                        if (currentLawBank == null) {
                            ready = false;
                            holder.tvLawImageAction.setText("Image: keep current (preview not available yet)");
                        } else bytes = currentLawBank.get(p.lawEdit.index).imageBytes;
                    }
                    if (ready) {
                        holder.ivLawProposalImage.setVisibility(View.VISIBLE);
                        if (bytes == null) {
                            LawImageCodec.display(holder.ivLawProposalImage, null, original.imageRes);
                        } else {
                            LawImageCodec.display(holder.ivLawProposalImage, bytes, 0);
                        }
                    }
                }
            } else if (!restoreQuiz && isExodus) {
                holder.llBabelProposalImages.setVisibility(View.VISIBLE);
                holder.tvBabelOddBlock.setText("Exodus scene images");
                Map<String, Object> preview = new HashMap<>(p.raw);
                preview.put("approvedByAdmin", true);
                preview.put("question", p.proposedQuestion);
                QuizCaptionOverride proposal = QuizCaptionOverride.parse(preview, p.id, 4);
                if (proposal == null) {
                    holder.tvBabelOddBlock.setText("Image preview unavailable: invalid step configuration");
                } else {
                    QuizCaptionOverride current = currentExodusBank == null ? null
                            : currentExodusBank.get(proposal.itemIndex);
                    bindExodusImage(holder.ivBabelProposalImage1, holder.tvBabelImage1Action,
                            proposal.itemIndex, 0, proposal.question, proposal.image1, current == null ? null : current.image1);
                    bindExodusImage(holder.ivBabelProposalImage2, holder.tvBabelImage2Action,
                            proposal.itemIndex, 1, proposal.optionA, proposal.image2, current == null ? null : current.image2);
                    bindExodusImage(holder.ivBabelProposalImage3, holder.tvBabelImage3Action,
                            proposal.itemIndex, 2, proposal.optionB, proposal.image3, current == null ? null : current.image3);
                }
            } else if (!restoreQuiz && isBabel) {
                holder.llBabelProposalImages.setVisibility(View.VISIBLE);
                holder.tvProposedOptions.setVisibility(View.GONE);
                Map<String, Object> preview = new HashMap<>(p.raw);
                preview.put("approvedByAdmin", true);
                QuizBabelOverride proposal = QuizBabelOverride.parse(preview, p.id);
                if (proposal == null) {
                    holder.tvBabelOddBlock.setText("Image preview unavailable: invalid floor configuration");
                } else {
                    holder.tvBabelOddBlock.setText("Odd Block: Block " + (proposal.oddBlockIndex + 1));
                    QuizBabelOverride current = currentBabelBank == null ? null
                            : currentBabelBank.get(proposal.itemIndex);
                    bindBabelImage(holder.ivBabelProposalImage1, holder.tvBabelImage1Action,
                            proposal, 0, proposal.image1, current == null ? null : current.image1);
                    bindBabelImage(holder.ivBabelProposalImage2, holder.tvBabelImage2Action,
                            proposal, 1, proposal.image2, current == null ? null : current.image2);
                    bindBabelImage(holder.ivBabelProposalImage3, holder.tvBabelImage3Action,
                            proposal, 2, proposal.image3, current == null ? null : current.image3);
                }
            }

            if (p.timestamp != null) {
                String timeStr = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(p.timestamp.toDate());
                holder.tvProposalTime.setText(timeStr);
            } else {
                holder.tvProposalTime.setText("Just now");
            }

            holder.btnApprove.setOnClickListener(v -> approveProposal(p));
            holder.btnDisapprove.setOnClickListener(v -> disapproveProposal(p));
        }

        private void bindExodusImage(ImageView view, TextView label, int step, int role, String description,
                                     LawImageData image, LawImageData current) {
            String prefix = ExodusImageDefaults.LABELS[role] + "\n" + description + "\nImage: ";
            view.setContentDescription(ExodusImageDefaults.LABELS[role] + " proposal preview");
            if (!image.valid) {
                label.setText(prefix + "invalid image");
                return;
            }
            if ("KEEP".equals(image.action) && currentExodusBank == null) {
                label.setText(prefix + "current image loading or unavailable");
                return;
            }
            label.setText(prefix + ("REPLACE".equals(image.action) ? "uploaded replacement"
                    : "DEFAULT".equals(image.action) ? "original default" : "keep current image"));
            byte[] bytes = "KEEP".equals(image.action)
                    ? ExodusImageDefaults.bytes(current) : ExodusImageDefaults.bytes(image);
            LawImageCodec.display(view, bytes, ExodusImageDefaults.image(step, role));
        }

        private void bindBabelImage(ImageView view, TextView label, QuizBabelOverride proposal,
                                    int block, LawImageData image, LawImageData current) {
            String prefix = "Block " + (block + 1) + ": ";
            view.setContentDescription("Babel block " + (block + 1) + " proposal preview");
            if (!image.valid) {
                label.setText(prefix + "invalid image");
                return;
            }
            if ("KEEP".equals(image.action) && currentBabelBank == null) {
                label.setText(prefix + "current image loading or unavailable");
                return;
            }
            label.setText(prefix + ("REPLACE".equals(image.action) ? "uploaded replacement"
                    : "DEFAULT".equals(image.action) ? "original default" : "keep current image"));
            LawImageCodec.display(view, BabelImageDefaults.bytes(image, current),
                    BabelImageDefaults.image(proposal.itemIndex, block, proposal.oddBlockIndex));
        }

        private void clearBabelImages(ViewHolder holder) {
            for (ImageView view : new ImageView[]{holder.ivBabelProposalImage1,
                    holder.ivBabelProposalImage2, holder.ivBabelProposalImage3}) {
                view.setTag(null);
                view.setImageDrawable(null);
            }
            holder.tvBabelImage1Action.setText("");
            holder.tvBabelImage2Action.setText("");
            holder.tvBabelImage3Action.setText("");
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        @Override
        public void onViewRecycled(@NonNull ViewHolder holder) {
            holder.ivLawProposalImage.setTag(null);
            holder.ivLawProposalImage.setImageDrawable(null);
            clearBabelImages(holder);
            super.onViewRecycled(holder);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            LinearLayout monarchyPreview;
            ImageView ivLawProposalImage;
            TextView tvLawImageAction;
            LinearLayout llBabelProposalImages;
            TextView tvBabelOddBlock, tvBabelImage1Action, tvBabelImage2Action, tvBabelImage3Action;
            ImageView ivBabelProposalImage1, ivBabelProposalImage2, ivBabelProposalImage3;
            TextView tvStoryBadge, tvTeacherName, tvOriginalQuestion, tvProposedQuestion, tvProposedOptions, tvProposalTime;
            Button btnApprove, btnDisapprove;

            ViewHolder(View v) {
                super(v);
                ivLawProposalImage = v.findViewById(R.id.ivLawProposalImage);
                tvLawImageAction = v.findViewById(R.id.tvLawImageAction);
                llBabelProposalImages = v.findViewById(R.id.llBabelProposalImages);
                tvBabelOddBlock = v.findViewById(R.id.tvBabelOddBlock);
                tvBabelImage1Action = v.findViewById(R.id.tvBabelImage1Action);
                ivBabelProposalImage1 = v.findViewById(R.id.ivBabelProposalImage1);
                tvBabelImage2Action = v.findViewById(R.id.tvBabelImage2Action);
                ivBabelProposalImage2 = v.findViewById(R.id.ivBabelProposalImage2);
                tvBabelImage3Action = v.findViewById(R.id.tvBabelImage3Action);
                ivBabelProposalImage3 = v.findViewById(R.id.ivBabelProposalImage3);
                tvStoryBadge = v.findViewById(R.id.tvStoryBadge);
                tvTeacherName = v.findViewById(R.id.tvTeacherName);
                tvOriginalQuestion = v.findViewById(R.id.tvOriginalQuestion);
                tvProposedQuestion = v.findViewById(R.id.tvProposedQuestion);
                tvProposedOptions = v.findViewById(R.id.tvProposedOptions);
                tvProposalTime = v.findViewById(R.id.tvProposalTime);
                btnApprove = v.findViewById(R.id.btnApprove);
                btnDisapprove = v.findViewById(R.id.btnDisapprove);
                monarchyPreview = new LinearLayout(v.getContext());
                monarchyPreview.setOrientation(LinearLayout.VERTICAL);
                LinearLayout parent = (LinearLayout) tvProposedOptions.getParent();
                parent.addView(monarchyPreview, parent.indexOfChild(tvProposedOptions) + 1);
            }
        }
    }
}