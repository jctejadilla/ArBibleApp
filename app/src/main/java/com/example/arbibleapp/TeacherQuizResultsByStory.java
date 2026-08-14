package com.example.arbibleapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class TeacherQuizResultsByStory extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvTotalStories, tvSubmissions, tvAvgScore;
    private RecyclerView rvByStory;
    private StoryGroupAdapter adapter;
    private final List<Object> displayList = new ArrayList<>();
    
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_teacher_quiz_results_by_story);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        btnBack = findViewById(R.id.btnBack);
        tvTotalStories = findViewById(R.id.tvTotalStories);
        tvSubmissions = findViewById(R.id.tvSubmissions);
        tvAvgScore = findViewById(R.id.tvAvgScore);
        rvByStory = findViewById(R.id.rvQuizResultsByStory);

        btnBack.setOnClickListener(v -> finish());

        rvByStory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StoryGroupAdapter(displayList);
        rvByStory.setAdapter(adapter);

        loadQuizResults();
        setupTabs();
    }

    private void setupTabs() {
        TextView tabAll = findViewById(R.id.tabAll);
        if (tabAll != null) {
            tabAll.setOnClickListener(v -> {
                Intent intent = new Intent(this, TeacherQuizResults.class);
                startActivity(intent);
                finish();
                overridePendingTransition(0, 0);
            });
        }
    }

    private void loadQuizResults() {
        if (mAuth.getCurrentUser() == null) return;
        String teacherUid = mAuth.getCurrentUser().getUid();

        db.collection("enrollments")
                .whereEqualTo("teacherUid", teacherUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        updateSummary(new ArrayList<>());
                        return;
                    }

                    List<String> studentUids = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String uid = doc.getString("studentUid");
                        if (uid != null) studentUids.add(uid);
                    }

                    if (!studentUids.isEmpty()) {
                        fetchQuizResultsForStudents(studentUids);
                    } else {
                        updateSummary(new ArrayList<>());
                    }
                });
    }

    private void fetchQuizResultsForStudents(List<String> studentUids) {
        db.collection("quiz_results")
                .whereIn("studentUid", studentUids)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<TeacherQuizResults.QuizResultModel> results = new ArrayList<>();
                    // Use TreeMap to keep story titles sorted
                    Map<String, List<TeacherQuizResults.QuizResultModel>> grouped = new TreeMap<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        TeacherQuizResults.QuizResultModel result = doc.toObject(TeacherQuizResults.QuizResultModel.class);
                        if (result != null) {
                            results.add(result);
                            String title = result.storyTitle != null ? result.storyTitle : "General Quiz";
                            if (!grouped.containsKey(title)) {
                                grouped.put(title, new ArrayList<>());
                            }
                            grouped.get(title).add(result);
                        }
                    }

                    displayList.clear();
                    for (Map.Entry<String, List<TeacherQuizResults.QuizResultModel>> entry : grouped.entrySet()) {
                        String title = entry.getKey();
                        List<TeacherQuizResults.QuizResultModel> storyResults = entry.getValue();
                        
                        // Sort results within each group by timestamp descending
                        Collections.sort(storyResults, (r1, r2) -> {
                            if (r1.timestamp == null || r2.timestamp == null) return 0;
                            return r2.timestamp.compareTo(r1.timestamp);
                        });

                        displayList.add(new StoryHeader(title, storyResults.size()));
                        displayList.addAll(storyResults);
                    }

                    adapter.notifyDataSetChanged();
                    updateSummary(results);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateSummary(List<TeacherQuizResults.QuizResultModel> results) {
        if (tvSubmissions != null) tvSubmissions.setText(String.valueOf(results.size()));
        Set<String> distinctStories = new HashSet<>();
        int totalScore = 0;
        int totalPossible = 0;
        for (TeacherQuizResults.QuizResultModel r : results) {
            if (r.storyTitle != null) distinctStories.add(r.storyTitle);
            totalScore += r.score;
            totalPossible += r.totalQuestions;
        }
        if (tvTotalStories != null) tvTotalStories.setText(String.valueOf(distinctStories.size()));
        if (tvAvgScore != null) {
            if (totalPossible > 0) {
                int avg = (int) (((double) totalScore / totalPossible) * 100);
                tvAvgScore.setText(String.format(Locale.getDefault(), "%d%%", avg));
            } else {
                tvAvgScore.setText("0%");
            }
        }
    }

    static class StoryHeader {
        String title;
        int count;
        StoryHeader(String title, int count) { this.title = title; this.count = count; }
    }

    private static class StoryGroupAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_HEADER = 0;
        private static final int TYPE_ITEM = 1;
        private final List<Object> list;

        StoryGroupAdapter(List<Object> list) { this.list = list; }

        @Override
        public int getItemViewType(int position) {
            return list.get(position) instanceof StoryHeader ? TYPE_HEADER : TYPE_ITEM;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_HEADER) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_story_group_header, parent, false);
                return new HeaderViewHolder(v);
            } else {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_story_submission, parent, false);
                return new ItemViewHolder(v);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (getItemViewType(position) == TYPE_HEADER) {
                StoryHeader h = (StoryHeader) list.get(position);
                HeaderViewHolder vh = (HeaderViewHolder) holder;
                vh.tvTitle.setText(h.title);
                vh.tvCount.setText(String.format(Locale.getDefault(), "%d submissions", h.count));
            } else {
                TeacherQuizResults.QuizResultModel r = (TeacherQuizResults.QuizResultModel) list.get(position);
                ItemViewHolder vh = (ItemViewHolder) holder;
                vh.tvName.setText(r.username != null ? r.username : "Unknown");
                vh.tvDate.setText(r.date != null ? r.date : "Recently");
                vh.tvScore.setText(String.format(Locale.getDefault(), "%d/%d", r.score, r.totalQuestions));
                
                String name = r.username != null ? r.username : "ST";
                if (name.length() >= 2) {
                    vh.tvInitials.setText(name.substring(0, 2).toUpperCase());
                } else {
                    vh.tvInitials.setText(name.toUpperCase());
                }

                double percent = r.totalQuestions > 0 ? (double) r.score / r.totalQuestions : 0;
                if (percent >= 0.8) {
                    vh.tvScore.setTextColor(Color.parseColor("#2E7D32"));
                } else if (percent >= 0.5) {
                    vh.tvScore.setTextColor(Color.parseColor("#EF6C00"));
                } else {
                    vh.tvScore.setTextColor(Color.parseColor("#C62828"));
                }
            }
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class HeaderViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvCount;
            HeaderViewHolder(View v) {
                super(v);
                tvTitle = v.findViewById(R.id.tvStoryName);
                tvCount = v.findViewById(R.id.tvSubmissionCount);
            }
        }

        static class ItemViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvDate, tvScore, tvInitials;
            ItemViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvStudentName);
                tvDate = v.findViewById(R.id.tvDate);
                tvScore = v.findViewById(R.id.tvScoreBadge);
                tvInitials = v.findViewById(R.id.tvAvatarInitials);
            }
        }
    }
}
