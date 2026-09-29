package com.example.arbibleapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
    private ListenerRegistration proposalListener;

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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (proposalListener != null) proposalListener.remove();
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
                                ts
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
        // 1. Commit to live quiz questions collection
        Map<String, Object> liveQuestion = new HashMap<>();
        liveQuestion.put("storyTitle", proposal.storyTitle);
        liveQuestion.put("originalQuestion", proposal.originalQuestion);
        liveQuestion.put("question", proposal.proposedQuestion);
        liveQuestion.put("options", proposal.options);
        liveQuestion.put("correctAnswerIndex", proposal.correctAnswerIndex);
        liveQuestion.put("approvedByAdmin", true);
        liveQuestion.put("timestamp", Timestamp.now());

        db.collection("quiz_questions").add(liveQuestion)
                .addOnSuccessListener(ref -> {
                    // 2. Mark proposal APPROVED
                    db.collection("question_edit_proposals").document(proposal.id)
                            .update("status", "APPROVED")
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Question Proposal Approved!", Toast.LENGTH_SHORT).show();
                                NotificationPopupManager.getInstance().showNotification(
                                        this,
                                        "Proposal Approved!",
                                        "Question edit proposal for " + proposal.storyTitle + " was approved.",
                                        android.R.drawable.checkbox_on_background
                                );
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error approving proposal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void disapproveProposal(ProposalModel proposal) {
        db.collection("question_edit_proposals").document(proposal.id)
                .update("status", "REJECTED")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Proposal Disapproved", Toast.LENGTH_SHORT).show();
                    NotificationPopupManager.getInstance().showNotification(
                            this,
                            "Proposal Rejected",
                            "Question edit proposal for " + proposal.storyTitle + " was rejected.",
                            android.R.drawable.ic_menu_close_clear_cancel
                    );
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error updating status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private static class ProposalModel {
        String id, teacherName, storyTitle, originalQuestion, proposedQuestion;
        List<String> options;
        int correctAnswerIndex;
        Timestamp timestamp;

        ProposalModel(String id, String teacherName, String storyTitle, String originalQuestion, String proposedQuestion, List<String> options, int correctAnswerIndex, Timestamp timestamp) {
            this.id = id;
            this.teacherName = teacherName;
            this.storyTitle = storyTitle;
            this.originalQuestion = originalQuestion;
            this.proposedQuestion = proposedQuestion;
            this.options = options;
            this.correctAnswerIndex = correctAnswerIndex;
            this.timestamp = timestamp;
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
            holder.tvProposedOptions.setText(optsBuilder.toString());

            if (p.timestamp != null) {
                String timeStr = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(p.timestamp.toDate());
                holder.tvProposalTime.setText(timeStr);
            } else {
                holder.tvProposalTime.setText("Just now");
            }

            holder.btnApprove.setOnClickListener(v -> approveProposal(p));
            holder.btnDisapprove.setOnClickListener(v -> disapproveProposal(p));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvStoryBadge, tvTeacherName, tvOriginalQuestion, tvProposedQuestion, tvProposedOptions, tvProposalTime;
            Button btnApprove, btnDisapprove;

            ViewHolder(View v) {
                super(v);
                tvStoryBadge = v.findViewById(R.id.tvStoryBadge);
                tvTeacherName = v.findViewById(R.id.tvTeacherName);
                tvOriginalQuestion = v.findViewById(R.id.tvOriginalQuestion);
                tvProposedQuestion = v.findViewById(R.id.tvProposedQuestion);
                tvProposedOptions = v.findViewById(R.id.tvProposedOptions);
                tvProposalTime = v.findViewById(R.id.tvProposalTime);
                btnApprove = v.findViewById(R.id.btnApprove);
                btnDisapprove = v.findViewById(R.id.btnDisapprove);
            }
        }
    }
}