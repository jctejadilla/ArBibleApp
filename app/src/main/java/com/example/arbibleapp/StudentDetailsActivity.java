package com.example.arbibleapp;

import android.app.Dialog;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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

        Button btnRemoveStudent = findViewById(R.id.btnRemoveStudent);
        btnRemoveStudent.setOnClickListener(v -> promptRemoveStudent());

        tvName.setText(studentName);
        tvXP.setText("0");
        tvRank.setText("#--");
        tvQuizAvg.setText("0%");
        tvAttendance.setText("0/0");
        tvStories.setText("0");
        tvAvgSession.setText("0s");

        if (studentName != null && studentName.length() >= 1) {
            tvInitials.setText(studentName.substring(0, Math.min(2, studentName.length())).toUpperCase());
        }
        tvId.setText("ID: #" + studentUid.substring(0, Math.min(6, studentUid.length())).toUpperCase());
    }

    private void promptRemoveStudent() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_remove_student);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvRemoveMessage = dialog.findViewById(R.id.tvRemoveMessage);
        tvRemoveMessage.setText("Are you sure you want to remove " + studentName + " from your class? This action cannot be undone.");

        Button btnCancel = dialog.findViewById(R.id.btnCancelRemove);
        Button btnConfirm = dialog.findViewById(R.id.btnConfirmRemove);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            removeStudentFromClass();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void removeStudentFromClass() {
        if (teacherUid == null || studentUid == null) return;
        db.collection("enrollments")
            .whereEqualTo("teacherUid", teacherUid)
            .whereEqualTo("studentUid", studentUid)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    doc.getReference().delete();
                }
                Toast.makeText(this, studentName + " removed from class.", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Failed to remove student", Toast.LENGTH_SHORT).show());
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

        loadPerformanceOverview();
    }

    private void loadPerformanceOverview() {
        db.collection("quiz_results")
                .whereEqualTo("studentUid", studentUid)
                .get()
                .addOnSuccessListener(query -> {
                    Set<String> completedStories = new HashSet<>();
                    double totalPercentage = 0;
                    int validQuizzes = 0;
                    long totalSessionSeconds = 0;
                    int validSessions = 0;

                    for (DocumentSnapshot document : query) {
                        Boolean isSunday = document.getBoolean("isSunday");
                        if (Boolean.FALSE.equals(isSunday)) continue;

                        String storyTitle = document.getString("storyTitle");
                        if (storyTitle != null && !storyTitle.trim().isEmpty()) {
                            completedStories.add(storyTitle.trim());
                        }

                        Long score = document.getLong("score");
                        Long totalQuestions = document.getLong("totalQuestions");
                        if (score != null && totalQuestions != null
                                && totalQuestions > 0 && score >= 0 && score <= totalQuestions) {
                            totalPercentage += (score.doubleValue() / totalQuestions.doubleValue()) * 100.0;
                            validQuizzes++;
                        }

                        Long durationSeconds = parseDurationSeconds(document.getString("duration"));
                        if (durationSeconds != null) {
                            totalSessionSeconds += durationSeconds;
                            validSessions++;
                        }
                    }

                    int averagePercentage = validQuizzes == 0
                            ? 0 : (int) (totalPercentage / validQuizzes);
                    tvQuizAvg.setText(averagePercentage + "%");
                    tvStories.setText(String.valueOf(completedStories.size()));

                    if (validSessions == 0) {
                        tvAvgSession.setText("0s");
                    } else {
                        long averageSeconds = Math.round((double) totalSessionSeconds / validSessions);
                        if (averageSeconds >= 60) {
                            tvAvgSession.setText((averageSeconds / 60) + "m " + (averageSeconds % 60) + "s");
                        } else {
                            tvAvgSession.setText(averageSeconds + "s");
                        }
                    }
                });

        Calendar today = Calendar.getInstance();
        Calendar firstDayOfMonth = (Calendar) today.clone();
        firstDayOfMonth.set(Calendar.DAY_OF_MONTH, 1);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        dateFormat.setLenient(false);
        String monthStart = dateFormat.format(firstDayOfMonth.getTime());
        String todayString = dateFormat.format(today.getTime());
        int totalSundays = countSundaysInMonth(today);
        tvAttendance.setText("0/" + totalSundays);

        db.collection("attendance")
                .whereEqualTo("studentUid", studentUid)
                .whereGreaterThanOrEqualTo("date", monthStart)
                .whereLessThanOrEqualTo("date", todayString)
                .get()
                .addOnSuccessListener(query -> {
                    Set<String> attendedSundays = new HashSet<>();
                    for (DocumentSnapshot document : query) {
                        String date = document.getString("date");
                        if (isSundayDate(date)) attendedSundays.add(date);
                    }
                    tvAttendance.setText(attendedSundays.size() + "/" + totalSundays);
                });
    }

    private int countSundaysInMonth(Calendar currentDate) {
        Calendar day = (Calendar) currentDate.clone();
        day.set(Calendar.DAY_OF_MONTH, 1);
        int todayDate = currentDate.get(Calendar.DAY_OF_MONTH);
        int sundayCount = 0;
        for (int date = 1; date <= todayDate; date++) {
            day.set(Calendar.DAY_OF_MONTH, date);
            if (day.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) sundayCount++;
        }
        return sundayCount == 0 ? 1 : sundayCount; // Avoid division by 0 / "0/0"
    }

    private boolean isSundayDate(String value) {
        if (value == null) return false;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        dateFormat.setLenient(false);
        try {
            Date parsedDate = dateFormat.parse(value);
            if (parsedDate == null || !value.equals(dateFormat.format(parsedDate))) return false;
            Calendar date = Calendar.getInstance();
            date.setTime(parsedDate);
            return date.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
        } catch (ParseException ignored) {
            return false;
        }
    }

    private Long parseDurationSeconds(String duration) {
        if (duration == null) return null;
        String trimmedDuration = duration.trim();
        if (!trimmedDuration.matches("\\d+m\\s+\\d+s")) return null;

        String[] parts = trimmedDuration.split("\\s+");
        try {
            long minutes = Long.parseLong(parts[0].substring(0, parts[0].length() - 1));
            long seconds = Long.parseLong(parts[1].substring(0, parts[1].length() - 1));
            if (seconds >= 60 || minutes > (Long.MAX_VALUE - seconds) / 60) return null;
            return minutes * 60 + seconds;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void loadRecentQuizzes() {
        db.collection("quiz_results")
                .whereEqualTo("studentUid", studentUid)
                .get()
                .addOnSuccessListener(query -> {
                    quizResults.clear();
                    List<DocumentSnapshot> docs = new ArrayList<>(query.getDocuments());
                    
                    docs.sort((d1, d2) -> {
                        Timestamp t1 = d1.getTimestamp("timestamp");
                        Timestamp t2 = d2.getTimestamp("timestamp");
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
