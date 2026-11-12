package com.example.arbibleapp;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import com.facebook.shimmer.ShimmerFrameLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TeacherDashboard extends AppCompatActivity {

    CardView quizResult, leaderboard, cardProposeQuestion, cardEnrollStudent;
    LinearLayout navHome, navStudentsTab, navAttendanceTab, navAnalyticsTab;
    TextView tvTeacherName, tvTotalStudents, tvQuizSubmissions, tvAttendanceRate, tvAvgQuiz, tvResetBadge;
    ImageView btnSettings;
    ShimmerFrameLayout shimmerViewContainer;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration ssoListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!VerifiedAccountFlow.guard(this)) return;
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_teacher_dashboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);

            View topBar = findViewById(R.id.topBar);
            if (topBar != null) {
                topBar.setPadding(
                        topBar.getPaddingLeft(),
                        topBar.getPaddingTop(),
                        topBar.getPaddingRight(),
                        topBar.getPaddingBottom()
                );
            }

            View bottomNav = findViewById(R.id.bottomNav);
            if (bottomNav != null) {
                bottomNav.setPadding(
                        bottomNav.getPaddingLeft(),
                        bottomNav.getPaddingTop(),
                        bottomNav.getPaddingRight(),
                        systemBars.bottom
                );
            }
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvTeacherName = findViewById(R.id.tvTeacherName);
        
        tvTotalStudents = findViewById(R.id.tvTotalStudentsValue);
        tvQuizSubmissions = findViewById(R.id.tvQuizSubmissionsValue);
        tvAttendanceRate = findViewById(R.id.tvAttendanceRateValue);
        tvAvgQuiz = findViewById(R.id.tvAvgQuizValue);
        tvResetBadge = findViewById(R.id.tvResetBadge);
        shimmerViewContainer = findViewById(R.id.shimmerViewContainer);
        if (shimmerViewContainer != null) shimmerViewContainer.startShimmer();

        quizResult = findViewById(R.id.cardQuizResults);
        leaderboard = findViewById(R.id.cardLeaderboard);
        cardProposeQuestion = findViewById(R.id.cardProposeQuestion);
        cardEnrollStudent = findViewById(R.id.cardEnrollStudent);

        btnSettings = findViewById(R.id.btnSettings);

        navHome = findViewById(R.id.navHome);
        navStudentsTab = findViewById(R.id.navStudentsTab);
        navAttendanceTab = findViewById(R.id.navAttendanceTab);
        navAnalyticsTab = findViewById(R.id.navAnalyticsTab);

        fetchUserData();

        if (quizResult != null) quizResult.setOnClickListener(v -> startActivity(new Intent(TeacherDashboard.this, TeacherQuizResults.class)));
        if (leaderboard != null) leaderboard.setOnClickListener(v -> startActivity(new Intent(TeacherDashboard.this, Leaderboards.class)));
        if (cardProposeQuestion != null) cardProposeQuestion.setOnClickListener(v -> startActivity(new Intent(TeacherDashboard.this, TeacherProposeQuestionActivity.class)));
        if (cardEnrollStudent != null) cardEnrollStudent.setOnClickListener(v -> {
            Intent intent = new Intent(TeacherDashboard.this, ClassManagement.class);
            intent.putExtra(ClassManagement.EXTRA_OPEN_ENROLLMENT_SCANNER, true);
            startActivity(intent);
        });

        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> startActivity(new Intent(TeacherDashboard.this, TeacherProfile.class)));
        }

        if (navStudentsTab != null) navStudentsTab.setOnClickListener(v->{ startActivity(new Intent(TeacherDashboard.this, ClassManagement.class));});
        if (navAttendanceTab != null) navAttendanceTab.setOnClickListener(v->{ startActivity(new Intent(TeacherDashboard.this, attendance_teacher_main.class));});
        if (navAnalyticsTab != null) navAnalyticsTab.setOnClickListener(v->{ startActivity(new Intent(TeacherDashboard.this, AnalyticsActivity.class));});
    }

    private void fetchUserData() {
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (isFinishing()) return;
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            if (username == null || username.isEmpty()) {
                                username = documentSnapshot.getString("fullName");
                            }
                            
                            if (username != null && !username.isEmpty() && tvTeacherName != null) {
                                tvTeacherName.setText(username);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (isFinishing()) return;
                        Toast.makeText(this, "Error fetching user data", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void fetchMonthlyOverview() {
        if (mAuth.getCurrentUser() == null) return;
        if (tvQuizSubmissions != null) tvQuizSubmissions.setText("...");
        String teacherUid = mAuth.getCurrentUser().getUid();

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, 1);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        String nextMonth = new SimpleDateFormat("MMMM d", Locale.getDefault()).format(cal.getTime());
        if (tvResetBadge != null) tvResetBadge.setText("Resets " + nextMonth);

        db.collection("enrollments")
                .whereEqualTo("teacherUid", teacherUid)
                .get(Source.SERVER)
                .addOnSuccessListener(enrollments -> {
                    if (isFinishing() || isDestroyed()) return;
                    Set<String> uniqueStudentUids = new HashSet<>();
                    for (DocumentSnapshot enrollment : enrollments) {
                        String studentUid = enrollment.getString("studentUid");
                        if (studentUid != null && !studentUid.trim().isEmpty()) {
                            uniqueStudentUids.add(studentUid.trim());
                        }
                    }
                    if (uniqueStudentUids.isEmpty()) {
                        updateEmptyDashboard();
                        return;
                    }
                    fetchValidStudents(new ArrayList<>(uniqueStudentUids));
                })
                .addOnFailureListener(error -> showSubmissionUnavailable());
    }

    private void fetchValidStudents(List<String> rawStudentUids) {
        List<Task<QuerySnapshot>> userQueries = new ArrayList<>();
        final int maxUidsPerQuery = 30;
        for (int start = 0; start < rawStudentUids.size(); start += maxUidsPerQuery) {
            int end = Math.min(start + maxUidsPerQuery, rawStudentUids.size());
            userQueries.add(db.collection("users")
                    .whereIn("uid", rawStudentUids.subList(start, end))
                    .get(Source.SERVER));
        }

        Tasks.whenAllSuccess(userQueries)
                .addOnSuccessListener(queryResults -> {
                    if (isFinishing() || isDestroyed()) return;
                    Set<String> validUids = new HashSet<>();
                    for (Object queryResult : queryResults) {
                        QuerySnapshot users = (QuerySnapshot) queryResult;
                        for (DocumentSnapshot user : users.getDocuments()) {
                            if (user.exists()) validUids.add(user.getId());
                        }
                    }
                    List<String> validStudentUids = new ArrayList<>(validUids);
                    int validStudentCount = validStudentUids.size();
                    if (tvTotalStudents != null) {
                        tvTotalStudents.setText(String.valueOf(validStudentCount));
                    }
                    if (validStudentCount == 0) {
                        updateEmptyDashboard();
                        return;
                    }
                    fetchQuizAndAttendanceData(validStudentUids, validStudentCount);
                })
                .addOnFailureListener(error -> showSubmissionUnavailable());
    }

    private void showSubmissionUnavailable() {
        if (isFinishing() || isDestroyed()) return;
        if (tvQuizSubmissions != null) tvQuizSubmissions.setText("N/A");
        stopDashboardShimmer();
    }

    private void updateEmptyDashboard() {
        if (tvTotalStudents != null) tvTotalStudents.setText("0");
        if (tvQuizSubmissions != null) tvQuizSubmissions.setText("0");
        if (tvAvgQuiz != null) tvAvgQuiz.setText("0%");
        if (tvAttendanceRate != null) tvAttendanceRate.setText("0%");

        stopDashboardShimmer();
    }

    private void stopDashboardShimmer() {
        if (shimmerViewContainer == null) return;
        shimmerViewContainer.stopShimmer();
        shimmerViewContainer.setShimmer(null);
    }

    private void fetchMonthlySubmissionCount(List<String> studentUids) {
        Calendar start = Calendar.getInstance();
        start.set(Calendar.DAY_OF_MONTH, 1);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.MONTH, 1);

        Set<String> enrolledUids = new HashSet<>(studentUids);
        db.collection("quiz_results")
                .whereGreaterThanOrEqualTo("timestamp", new Timestamp(start.getTime()))
                .whereLessThan("timestamp", new Timestamp(end.getTime()))
                .get(Source.SERVER)
                .addOnSuccessListener(results -> {
                    if (isFinishing() || isDestroyed()) return;
                    int submissionCount = 0;
                    for (DocumentSnapshot result : results) {
                        if (enrolledUids.contains(result.getString("studentUid"))) {
                            submissionCount++;
                        }
                    }
                    if (tvQuizSubmissions != null) {
                        tvQuizSubmissions.setText(String.format(Locale.getDefault(), "%,d", submissionCount));
                    }
                })
                .addOnFailureListener(error -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (tvQuizSubmissions != null) tvQuizSubmissions.setText("N/A");
                    stopDashboardShimmer();
                });
    }

    private List<Task<QuerySnapshot>> createStudentQueries(String collection, List<String> studentUids) {
        List<Task<QuerySnapshot>> requests = new ArrayList<>();
        final int maxUidsPerQuery = 30;
        for (int start = 0; start < studentUids.size(); start += maxUidsPerQuery) {
            int end = Math.min(start + maxUidsPerQuery, studentUids.size());
            requests.add(db.collection(collection)
                    .whereIn("studentUid", studentUids.subList(start, end))
                    .get(Source.SERVER));
        }
        return requests;
    }

    private void fetchQuizAndAttendanceData(List<String> studentUids, int studentCount) {
        fetchMonthlySubmissionCount(studentUids);

        Tasks.whenAllSuccess(createStudentQueries("quiz_results", studentUids))
                .addOnSuccessListener(queryResults -> {
                    if (isFinishing() || isDestroyed()) return;
                    double totalPercentage = 0;
                    int validResults = 0;
                    for (Object queryResult : queryResults) {
                        QuerySnapshot results = (QuerySnapshot) queryResult;
                        for (DocumentSnapshot result : results.getDocuments()) {
                            Boolean isSunday = result.getBoolean("isSunday");
                            if (isSunday != null && !isSunday) continue;

                            Long score = result.getLong("score");
                            Long total = result.getLong("totalQuestions");
                            if (score != null && total != null && total > 0) {
                                totalPercentage += (score.doubleValue() / total.doubleValue()) * 100.0;
                                validResults++;
                            }
                        }
                    }
                    if (tvAvgQuiz != null) {
                        tvAvgQuiz.setText(validResults > 0
                                ? (int) (totalPercentage / validResults) + "%"
                                : "0%");
                    }
                })
                .addOnFailureListener(error -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (tvAvgQuiz != null) tvAvgQuiz.setText("N/A");
                    stopDashboardShimmer();
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
        Tasks.whenAllSuccess(createStudentQueries("attendance", studentUids))
                .addOnSuccessListener(queryResults -> {
                    if (isFinishing() || isDestroyed()) return;
                    int totalAttendance = 0;
                    String monthPrefix = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
                    for (Object queryResult : queryResults) {
                        QuerySnapshot attendanceDocs = (QuerySnapshot) queryResult;
                        for (DocumentSnapshot attendance : attendanceDocs.getDocuments()) {
                            String date = attendance.getString("date");
                            if (date != null && date.startsWith(monthPrefix)) {
                                totalAttendance++;
                            }
                        }
                    }
                    int rate = (totalAttendance * 100) / (studentCount * finalSundays);
                    if (tvAttendanceRate != null) {
                        tvAttendanceRate.setText(Math.min(100, rate) + "%");
                    }
                    stopDashboardShimmer();
                })
                .addOnFailureListener(error -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (tvAttendanceRate != null) tvAttendanceRate.setText("N/A");
                    stopDashboardShimmer();
                });
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (!VerifiedAccountFlow.guard(this)) return;
        fetchMonthlyOverview();
        checkSingleSignOn();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (ssoListener != null) {
            ssoListener.remove();
            ssoListener = null;
        }
    }

    private void checkSingleSignOn() {
        if (mAuth.getCurrentUser() == null) return;

        String currentDeviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        String uid = mAuth.getCurrentUser().getUid();

        if (ssoListener != null) ssoListener.remove();

        ssoListener = db.collection("users").document(uid).addSnapshotListener((snapshot, e) -> {
            if (e != null || snapshot == null || !snapshot.exists()) return;

            String activeId = snapshot.getString("activeDeviceId");
            if (activeId != null && !activeId.equals(currentDeviceId)) {
                if (ssoListener != null) ssoListener.remove();
                mAuth.signOut();

                Toast.makeText(this, "Logged in from another device. Session ended.", Toast.LENGTH_LONG).show();

                Intent intent = new Intent(this, login.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ssoListener != null) ssoListener.remove();
    }
}
