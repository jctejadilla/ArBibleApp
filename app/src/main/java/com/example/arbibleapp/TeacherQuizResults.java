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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TeacherQuizResults extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvTotalStories, tvSubmissions, tvAvgScore;
    private RecyclerView rvQuizResults;
    private QuizResultAdapter adapter;
    private final List<QuizResultModel> quizResultsList = new ArrayList<>();
    
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_teacher_quiz_results);
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
        rvQuizResults = findViewById(R.id.rvQuizResults);

        btnBack.setOnClickListener(v -> finish());

        rvQuizResults.setLayoutManager(new LinearLayoutManager(this));
        adapter = new QuizResultAdapter(quizResultsList);
        rvQuizResults.setAdapter(adapter);

        loadQuizResults();
        
        setupTabs();
    }

    private void setupTabs() {
        TextView tabByStory = findViewById(R.id.tabByStory);
        if (tabByStory != null) {
            tabByStory.setOnClickListener(v -> {
                Intent intent = new Intent(this, TeacherQuizResultsByStory.class);
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
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load enrollments", Toast.LENGTH_SHORT).show());
    }

    private void fetchQuizResultsForStudents(List<String> studentUids) {
        db.collection("quiz_results")
                .whereIn("studentUid", studentUids)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    quizResultsList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        QuizResultModel result = doc.toObject(QuizResultModel.class);
                        if (result != null) {
                            quizResultsList.add(result);
                        }
                    }
                    
                    // Sort locally by timestamp descending
                    Collections.sort(quizResultsList, (r1, r2) -> {
                        if (r1.timestamp == null || r2.timestamp == null) return 0;
                        return r2.timestamp.compareTo(r1.timestamp);
                    });
                    
                    adapter.notifyDataSetChanged();
                    updateSummary(quizResultsList);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading results: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateSummary(List<QuizResultModel> results) {
        if (tvSubmissions != null) tvSubmissions.setText(String.valueOf(results.size()));
        
        Set<String> distinctStories = new HashSet<>();
        int totalScore = 0;
        int totalPossible = 0;
        
        for (QuizResultModel r : results) {
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

    public static class QuizResultModel {
        public String studentUid, username, storyTitle, duration, date;
        public int score, totalQuestions;
        public com.google.firebase.Timestamp timestamp;

        public QuizResultModel() {}
    }

    private static class QuizResultAdapter extends RecyclerView.Adapter<QuizResultAdapter.ViewHolder> {
        private final List<QuizResultModel> list;
        QuizResultAdapter(List<QuizResultModel> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_teacher_quiz_result, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            QuizResultModel r = list.get(position);
            holder.tvName.setText(r.username != null ? r.username : "Unknown");
            
            if (r.studentUid != null && r.studentUid.length() >= 7) {
                holder.tvId.setText(r.studentUid.substring(0, 7).toUpperCase());
            } else {
                holder.tvId.setText("STUDENT");
            }
            
            holder.tvStory.setText(r.storyTitle != null ? r.storyTitle : "General Quiz");
            holder.tvScore.setText(String.format(Locale.getDefault(), "%d/%d", r.score, r.totalQuestions));
            holder.tvDate.setText(r.date != null ? r.date : "Recently");
            holder.tvDuration.setText(r.duration != null ? r.duration : "--");
            
            String name = r.username != null ? r.username : "ST";
            if (name.length() >= 2) {
                holder.tvInitials.setText(name.substring(0, 2).toUpperCase());
            } else {
                holder.tvInitials.setText(name.toUpperCase());
            }
            
            double percent = r.totalQuestions > 0 ? (double) r.score / r.totalQuestions : 0;
            if (percent >= 0.8) {
                holder.tvScore.setTextColor(Color.parseColor("#2E7D32"));
            } else if (percent >= 0.5) {
                holder.tvScore.setTextColor(Color.parseColor("#EF6C00"));
            } else {
                holder.tvScore.setTextColor(Color.parseColor("#C62828"));
            }
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvId, tvStory, tvScore, tvDate, tvDuration, tvInitials;
            ViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvStudentName);
                tvId = v.findViewById(R.id.tvStudentId);
                tvStory = v.findViewById(R.id.tvStoryTitle);
                tvScore = v.findViewById(R.id.tvScoreBadge);
                tvDate = v.findViewById(R.id.tvDate);
                tvDuration = v.findViewById(R.id.tvDuration);
                tvInitials = v.findViewById(R.id.tvAvatarInitials);
            }
        }
    }
}
