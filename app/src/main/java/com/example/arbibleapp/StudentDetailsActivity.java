package com.example.arbibleapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class StudentDetailsActivity extends AppCompatActivity {

    private String studentUid, studentName, teacherUid;
    private FirebaseFirestore db;

    private TextView tvName, tvId, tvXP, tvRank, tvQuizAvg, tvAttendance, tvStories, tvAvgSession, tvInitials;
    private RecyclerView rvQuizzes;
    private QuizResultAdapter adapter;
    private List<QuizResultModel> quizResults = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_student_details);

        studentUid = getIntent().getStringExtra("studentUid");
        studentName = getIntent().getStringExtra("studentName");
        teacherUid = getIntent().getStringExtra("teacherUid");

        db = FirebaseFirestore.getInstance();

        initViews();
        loadStudentData();
        loadRecentQuizzes();
    }

    private void initViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        tvName = findViewById(R.id.tvStudentName);
        tvId = findViewById(R.id.tvStudentId);
        tvXP = findViewById(R.id.tvTotalXP);
        tvRank = findViewById(R.id.tvClassRank);
        tvQuizAvg = findViewById(R.id.tvQuizAvg);
        tvAttendance = findViewById(R.id.tvAttendanceRate);
        tvStories = findViewById(R.id.tvStoriesCount);
        tvAvgSession = findViewById(R.id.tvAvgSession);
        tvInitials = findViewById(R.id.tvAvatarInitials);

        rvQuizzes = findViewById(R.id.rvRecentQuizzes);
        rvQuizzes.setLayoutManager(new LinearLayoutManager(this));
        adapter = new QuizResultAdapter(quizResults);
        rvQuizzes.setAdapter(adapter);

        tvName.setText(studentName);
        tvXP.setText("0");
        tvRank.setText("#--");
        tvQuizAvg.setText("0%");
        tvAttendance.setText("0/4");
        tvStories.setText("0");
        tvAvgSession.setText("0m");

        if (studentName != null && studentName.length() >= 1) {
            tvInitials.setText(studentName.substring(0, Math.min(2, studentName.length())).toUpperCase());
        }
        tvId.setText("ID: #" + studentUid.substring(0, Math.min(6, studentUid.length())).toUpperCase());
    }

    private void loadStudentData() {
        db.collection("users").document(studentUid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Long studentXP = doc.getLong("totalXP");
                long xpValue = studentXP != null ? studentXP : 0;
                tvXP.setText(String.valueOf(xpValue));
                
                // Calculate Class Rank based on ENROLLED students only
                if (xpValue <= 0) {
                    tvRank.setText("#--");
                } else if (teacherUid != null) {
                    db.collection("enrollments")
                        .whereEqualTo("teacherUid", teacherUid)
                        .get()
                        .addOnSuccessListener(enrollments -> {
                            List<String> enrolledStudentUids = new ArrayList<>();
                            for (DocumentSnapshot enrollment : enrollments) {
                                String uid = enrollment.getString("studentUid");
                                if (uid != null) enrolledStudentUids.add(uid);
                            }

                            if (enrolledStudentUids.isEmpty()) {
                                tvRank.setText("#1");
                                return;
                            }

                            db.collection("users")
                                .whereIn("uid", enrolledStudentUids)
                                .get()
                                .addOnSuccessListener(users -> {
                                    int rank = 1;
                                    for (DocumentSnapshot u : users) {
                                        Long otherXP = u.getLong("totalXP");
                                        if (otherXP != null && otherXP > xpValue) {
                                            rank++;
                                        }
                                    }
                                    tvRank.setText("#" + rank);
                                })
                                .addOnFailureListener(e -> tvRank.setText("#--"));
                        })
                        .addOnFailureListener(e -> tvRank.setText("#--"));
                } else {
                    tvRank.setText("#--");
                }
            }
        });

        db.collection("quiz_results").whereEqualTo("studentUid", studentUid).get().addOnSuccessListener(query -> {
            if (!query.isEmpty()) {
                double totalPercentage = 0;
                long totalSeconds = 0;
                int count = query.size();
                for (DocumentSnapshot d : query) {
                    Long score = d.getLong("score");
                    Long totalQuestions = d.getLong("totalQuestions");
                    
                    if (score != null && totalQuestions != null && totalQuestions > 0) {
                        totalPercentage += (score.doubleValue() / totalQuestions.doubleValue()) * 100.0;
                    } else if (score != null) {
                        totalPercentage += (score.doubleValue() / 5.0) * 100.0;
                    }

                    String duration = d.getString("duration");
                    if (duration != null && duration.contains("m") && duration.contains("s")) {
                        try {
                            String[] parts = duration.split(" ");
                            int m = Integer.parseInt(parts[0].replace("m", ""));
                            int s = Integer.parseInt(parts[1].replace("s", ""));
                            totalSeconds += (m * 60L) + s;
                        } catch (Exception ignored) {}
                    }
                }
                int avg = (int) (totalPercentage / count);
                if (avg > 100) avg = 100;
                tvQuizAvg.setText(avg + "%");
                tvStories.setText(String.valueOf(count));

                long avgSec = totalSeconds / count;
                if (avgSec >= 60) {
                    tvAvgSession.setText((avgSec / 60) + "m " + (avgSec % 60) + "s");
                } else {
                    tvAvgSession.setText(avgSec + "s");
                }
            } else {
                tvQuizAvg.setText("0%");
                tvStories.setText("0");
                tvAvgSession.setText("0s");
            }
        });

        Calendar cal = Calendar.getInstance();
        int month = cal.get(Calendar.MONTH);
        int year = cal.get(Calendar.YEAR);
        cal.set(year, month, 1);
        int totalSundays = 0;
        while (cal.get(Calendar.MONTH) == month) {
            if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                totalSundays++;
            }
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        final int finalTotalSundays = totalSundays;

        db.collection("attendance").whereEqualTo("studentUid", studentUid).get().addOnSuccessListener(query -> {
            int attended = query.size();
            tvAttendance.setText(attended + "/" + finalTotalSundays);
        });
    }

    private void loadRecentQuizzes() {
        db.collection("quiz_results")
                .whereEqualTo("studentUid", studentUid)
                .get()
                .addOnSuccessListener(query -> {
                    quizResults.clear();
                    List<DocumentSnapshot> docs = new ArrayList<>(query.getDocuments());
                    
                    docs.sort((d1, d2) -> {
                        com.google.firebase.Timestamp t1 = d1.getTimestamp("timestamp");
                        com.google.firebase.Timestamp t2 = d2.getTimestamp("timestamp");
                        if (t1 == null || t2 == null) return 0;
                        return t2.compareTo(t1);
                    });

                    for (int i = 0; i < Math.min(docs.size(), 5); i++) {
                        DocumentSnapshot doc = docs.get(i);
                        String duration = doc.getString("duration");
                        if (duration == null) duration = "N/A";
                        
                        quizResults.add(new QuizResultModel(
                                doc.getString("storyTitle"),
                                doc.getLong("score"),
                                doc.getLong("totalQuestions"),
                                doc.getString("date"),
                                duration
                        ));
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private static class QuizResultModel {
        String title, date, duration;
        Long score, totalQuestions;
        QuizResultModel(String title, Long score, Long totalQuestions, String date, String duration) {
            this.title = title;
            this.score = score;
            this.totalQuestions = totalQuestions;
            this.date = date;
            this.duration = duration;
        }
    }

    private class QuizResultAdapter extends RecyclerView.Adapter<QuizResultAdapter.ViewHolder> {
        List<QuizResultModel> results;
        QuizResultAdapter(List<QuizResultModel> results) { this.results = results; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_quiz_result, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            QuizResultModel r = results.get(position);
            holder.title.setText(r.title);
            
            long score = r.score != null ? r.score : 0;
            long total = r.totalQuestions != null && r.totalQuestions > 0 ? r.totalQuestions : 5;
            
            int percent = (int) ((score * 100) / total);
            holder.pb.setProgress(percent);
            holder.percent.setText(percent + "%");
            holder.meta.setText((r.date != null ? r.date : "Recent") + "  •  " + r.duration);
        }

        @Override
        public int getItemCount() { return results.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title, percent, meta;
            ProgressBar pb;
            ViewHolder(View v) {
                super(v);
                title = v.findViewById(R.id.tvStoryTitle);
                percent = v.findViewById(R.id.tvScorePercent);
                meta = v.findViewById(R.id.tvQuizMeta);
                pb = v.findViewById(R.id.pbScore);
            }
        }
    }
}
