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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashSet;
import java.util.Set;

public class StudentDashboard extends AppCompatActivity {

    ConstraintLayout arScan, bibleStories, leaderboard, attendance;
    View viewProfile;
    LinearLayout navHome, navStories, navLeaderboard, navProfile;
    TextView tvStudentName, tvXPValue, tvStreakLabel, tvStreakValue;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_student_dashboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvStudentName = findViewById(R.id.tvStudentName);
        tvXPValue = findViewById(R.id.tvXPValue);
        tvStreakLabel = findViewById(R.id.tvStreakLabel);
        tvStreakValue = findViewById(R.id.tvStreakValue);

        arScan = findViewById(R.id.arScan);
        bibleStories = findViewById(R.id.bibleStories);
        attendance = findViewById(R.id.attendance);

        viewProfile = findViewById(R.id.viewProfile);

        navHome = findViewById(R.id.navHome);
        navStories = findViewById(R.id.navStories);
        navLeaderboard = findViewById(R.id.navLeaderboard);
        navProfile = findViewById(R.id.navProfile);

        fetchUserData();
        fetchAttendanceData();

        arScan.setOnClickListener(v -> startActivity(new Intent(StudentDashboard.this, ArScan.class)));
        bibleStories.setOnClickListener(v -> startActivity(new Intent(StudentDashboard.this, BibleStories.class)));
        attendance.setOnClickListener(v -> {
            Intent intent = new Intent(StudentDashboard.this, Attendance.class);
            intent.putExtra("userType", "Student");
            startActivity(intent);
        });

        viewProfile.setOnClickListener(v -> startActivity(new Intent(StudentDashboard.this, StudentProfile.class)));

        navStories.setOnClickListener(v->{ startActivity(new Intent(this, BibleStories.class));});
        navLeaderboard.setOnClickListener(v->{ startActivity(new Intent(this, Leaderboards.class));});
        navProfile.setOnClickListener(v->{ startActivity(new Intent(this, StudentProfile.class));});

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
                                tvStudentName.setText(username);
                            }

                            long totalXP = documentSnapshot.contains("totalXP") ? documentSnapshot.getLong("totalXP") : 0;
                            tvXPValue.setText(String.format("%,d", totalXP));
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error fetching user data", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void fetchAttendanceData() {
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            db.collection("attendance").whereEqualTo("studentUid", uid).get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        Set<String> uniqueDates = new HashSet<>();
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String date = doc.getString("date");
                            if (date != null) uniqueDates.add(date);
                        }
                        int attendanceCount = uniqueDates.size();
                        tvStreakLabel.setText("Total Attendance");

                        if(attendanceCount <= 1) {
                            tvStreakValue.setText(attendanceCount + " Sundays");
                        } else {
                            tvStreakValue.setText(attendanceCount + " Sundays 🔥");
                        }
                    });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchUserData();
        fetchAttendanceData();
    }
}