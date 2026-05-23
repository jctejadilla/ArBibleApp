package com.example.arbibleapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TeacherDashboard extends AppCompatActivity {

    ConstraintLayout quizResult, bibleStories, leaderboard, attendance, classManagement;
    View viewProfile;
    LinearLayout navHome, navStudentsTab, navAttendanceTab, navAnalyticsTab;
    TextView tvTeacherName, tvTotalStudents, tvAvgXP, tvAttendanceRate, tvAvgQuiz, tvResetBadge;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_teacher_dashboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvTeacherName = findViewById(R.id.tvTeacherName);
        
        tvTotalStudents = findViewById(R.id.tvTotalStudentsValue);
        tvAvgXP = findViewById(R.id.tvAvgXPValue);
        tvAttendanceRate = findViewById(R.id.tvAttendanceRateValue);
        tvAvgQuiz = findViewById(R.id.tvAvgQuizValue);
        tvResetBadge = findViewById(R.id.tvResetBadge);

        quizResult = findViewById(R.id.cardQuizResults);
        bibleStories = findViewById(R.id.cardBibleStories);
        leaderboard = findViewById(R.id.cardLeaderboard);
        attendance = findViewById(R.id.cardAttendance);
        classManagement = findViewById(R.id.cardClassManagement);

        viewProfile = findViewById(R.id.viewProfile);

        navHome = findViewById(R.id.navHome);
        navStudentsTab = findViewById(R.id.navStudentsTab);
        navAttendanceTab = findViewById(R.id.navAttendanceTab);
        navAnalyticsTab = findViewById(R.id.navAnalyticsTab);

        fetchUserData();
        fetchMonthlyOverview();

        quizResult.setOnClickListener(v -> startActivity(new Intent(TeacherDashboard.this, TeacherQuizResults.class)));
        bibleStories.setOnClickListener(v -> startActivity(new Intent(TeacherDashboard.this, BibleStories.class)));
        leaderboard.setOnClickListener(v -> startActivity(new Intent(TeacherDashboard.this, Leaderboards.class)));
        attendance.setOnClickListener(v -> startActivity(new Intent(TeacherDashboard.this, attendance_teacher_main.class)));
        classManagement.setOnClickListener(v -> startActivity(new Intent(TeacherDashboard.this, ClassManagement.class)));

        viewProfile.setOnClickListener(v -> startActivity(new Intent(TeacherDashboard.this, TeacherProfile.class)));

        navStudentsTab.setOnClickListener(v->{ startActivity(new Intent(TeacherDashboard.this, ClassManagement.class));});
        navAttendanceTab.setOnClickListener(v->{ startActivity(new Intent(TeacherDashboard.this, attendance_teacher_main.class));});
        navAnalyticsTab.setOnClickListener(v->{ startActivity(new Intent(TeacherDashboard.this, AnalyticsActivity.class));});
    }

    private void fetchUserData() {
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            if (username == null || username.isEmpty()) {
                                username = documentSnapshot.getString("fullName");
                            }
                            
                            if (username != null && !username.isEmpty()) {
                                tvTeacherName.setText(username);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error fetching user data", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void fetchMonthlyOverview() {
        if (mAuth.getCurrentUser() == null) return;
        String teacherUid = mAuth.getCurrentUser().getUid();

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, 1);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        String nextMonth = new SimpleDateFormat("MMMM d", Locale.getDefault()).format(cal.getTime());
        tvResetBadge.setText("Resets " + nextMonth);

        db.collection("enrollments")
                .whereEqualTo("teacherUid", teacherUid)
                .get()
                .addOnSuccessListener(enrollments -> {
                    int studentCount = enrollments.size();
                    tvTotalStudents.setText(String.valueOf(studentCount));

                    if (studentCount == 0) {
                        tvAvgXP.setText("0");
                        tvAvgQuiz.setText("0%");
                        tvAttendanceRate.setText("0%");
                        return;
                    }

                    List<String> studentUids = new ArrayList<>();
                    for (DocumentSnapshot doc : enrollments) {
                        studentUids.add(doc.getString("studentUid"));
                    }

                    db.collection("users")
                            .whereIn("uid", studentUids)
                            .get()
                            .addOnSuccessListener(users -> {
                                long totalXP = 0;
                                for (DocumentSnapshot user : users) {
                                    Long xp = user.getLong("totalXP");
                                    if (xp != null) totalXP += xp;
                                }
                                tvAvgXP.setText(String.format(Locale.getDefault(), "%,d", totalXP / studentCount));
                            });

                    db.collection("quiz_results")
                            .whereIn("studentUid", studentUids)
                            .get()
                            .addOnSuccessListener(results -> {
                                if (results.isEmpty()) {
                                    tvAvgQuiz.setText("0%");
                                    return;
                                }
                                double totalPercentage = 0;
                                int validResults = 0;
                                for (DocumentSnapshot res : results) {
                                    Long score = res.getLong("score");
                                    Long total = res.getLong("totalQuestions");
                                    if (score != null && total != null && total > 0) {
                                        totalPercentage += (score.doubleValue() / total.doubleValue()) * 100.0;
                                        validResults++;
                                    }
                                }
                                if (validResults > 0) {
                                    tvAvgQuiz.setText((int)(totalPercentage / validResults) + "%");
                                } else {
                                    tvAvgQuiz.setText("0%");
                                }
                            });

                    Calendar calAtt = Calendar.getInstance();
                    calAtt.set(Calendar.DAY_OF_MONTH, 1);
                    int sundaysSoFar = 0;
                    Calendar today = Calendar.getInstance();
                    while (calAtt.before(today) || calAtt.equals(today)) {
                        if (calAtt.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                            sundaysSoFar++;
                        }
                        calAtt.add(Calendar.DAY_OF_MONTH, 1);
                    }
                    if (sundaysSoFar == 0) sundaysSoFar = 1;

                    final int finalSundays = sundaysSoFar;
                    db.collection("attendance")
                            .whereIn("studentUid", studentUids)
                            .get()
                            .addOnSuccessListener(attendanceDocs -> {
                                int totalAttendance = 0;
                                String monthPrefix = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
                                for (DocumentSnapshot att : attendanceDocs) {
                                    String date = att.getString("date");
                                    if (date != null && date.startsWith(monthPrefix)) {
                                        totalAttendance++;
                                    }
                                }
                                int rate = (totalAttendance * 100) / (studentCount * finalSundays);
                                tvAttendanceRate.setText(Math.min(100, rate) + "%");
                            });
                });
    }
}
