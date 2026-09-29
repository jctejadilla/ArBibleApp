package com.example.arbibleapp;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminApprovalHistoryActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private TextView tvEmptyState;
    private MaterialButtonToggleGroup toggleFilter;
    private FirebaseFirestore db;
    private HistoryAdapter adapter;

    private final List<HistoryModel> allHistoryList = new ArrayList<>();
    private final List<HistoryModel> filteredList = new ArrayList<>();
    private ListenerRegistration historyListener;

    private String currentFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_approval_history);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        tvEmptyState = findViewById(R.id.tvEmptyState);
        toggleFilter = findViewById(R.id.toggleFilter);

        rvHistory = findViewById(R.id.rvHistory);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(filteredList);
        rvHistory.setAdapter(adapter);

        setupFilterToggle();
        loadApprovalHistory();
    }

    private void setupFilterToggle() {
        toggleFilter.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnFilterAll) {
                currentFilter = "ALL";
            } else if (checkedId == R.id.btnFilterApproved) {
                currentFilter = "APPROVED";
            } else if (checkedId == R.id.btnFilterRejected) {
                currentFilter = "REJECTED";
            }
            applyFilter();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (historyListener != null) historyListener.remove();
    }

    private void loadApprovalHistory() {
        if (historyListener != null) historyListener.remove();

        historyListener = db.collection("question_edit_proposals")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    allHistoryList.clear();
                    for (DocumentSnapshot doc : snapshots) {
                        String status = doc.getString("status");
                        if ("APPROVED".equals(status) || "REJECTED".equals(status)) {
                            String id = doc.getId();
                            String teacherName = doc.getString("teacherName");
                            String story = doc.getString("storyTitle");
                            String origQ = doc.getString("originalQuestion");
                            String propQ = doc.getString("proposedQuestion");
                            Timestamp ts = doc.getTimestamp("timestamp");

                            allHistoryList.add(new HistoryModel(
                                    id,
                                    status,
                                    teacherName != null ? teacherName : "Teacher",
                                    story != null ? story : "Bible Story",
                                    origQ != null ? origQ : "",
                                    propQ != null ? propQ : "",
                                    ts
                            ));
                        }
                    }
                    applyFilter();
                });
    }

    private void applyFilter() {
        filteredList.clear();
        for (HistoryModel item : allHistoryList) {
            if ("ALL".equals(currentFilter) || currentFilter.equals(item.status)) {
                filteredList.add(item);
            }
        }

        if (filteredList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvHistory.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvHistory.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }

    private static class HistoryModel {
        String id, status, teacherName, storyTitle, originalQuestion, proposedQuestion;
        Timestamp timestamp;

        HistoryModel(String id, String status, String teacherName, String storyTitle, String originalQuestion, String proposedQuestion, Timestamp timestamp) {
            this.id = id;
            this.status = status;
            this.teacherName = teacherName;
            this.storyTitle = storyTitle;
            this.originalQuestion = originalQuestion;
            this.proposedQuestion = proposedQuestion;
            this.timestamp = timestamp;
        }
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private final List<HistoryModel> list;

        HistoryAdapter(List<HistoryModel> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_history, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HistoryModel item = list.get(position);
            holder.tvStatusBadge.setText(item.status);

            if ("APPROVED".equals(item.status)) {
                holder.tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            } else {
                holder.tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D32F2F")));
            }

            holder.tvStoryTitle.setText(item.storyTitle);
            holder.tvTeacherName.setText("Proposed by: " + item.teacherName);
            holder.tvOriginalQuestion.setText(item.originalQuestion);
            holder.tvProposedQuestion.setText(item.proposedQuestion);

            if (item.timestamp != null) {
                String timeStr = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(item.timestamp.toDate());
                holder.tvTimestamp.setText(timeStr);
            } else {
                holder.tvTimestamp.setText("Recently");
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvStatusBadge, tvStoryTitle, tvTimestamp, tvTeacherName, tvOriginalQuestion, tvProposedQuestion;

            ViewHolder(View v) {
                super(v);
                tvStatusBadge = v.findViewById(R.id.tvStatusBadge);
                tvStoryTitle = v.findViewById(R.id.tvStoryTitle);
                tvTimestamp = v.findViewById(R.id.tvTimestamp);
                tvTeacherName = v.findViewById(R.id.tvTeacherName);
                tvOriginalQuestion = v.findViewById(R.id.tvOriginalQuestion);
                tvProposedQuestion = v.findViewById(R.id.tvProposedQuestion);
            }
        }
    }
}