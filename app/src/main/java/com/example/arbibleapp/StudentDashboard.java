package com.example.arbibleapp;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
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
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashSet;
import java.util.Set;

public class StudentDashboard extends AppCompatActivity {

    ConstraintLayout arScan, bibleStories, attendance;
    LinearLayout navHome, navStories, navLeaderboard, navProfile;
    TextView tvStudentName, tvXPValue, tvStreakLabel, tvStreakValue;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration ssoListener;

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

        navHome = findViewById(R.id.navHome);
        navStories = findViewById(R.id.navStories);
        navLeaderboard = findViewById(R.id.navLeaderboard);
        navProfile = findViewById(R.id.navProfile);

        fetchUserData();
        fetchAttendanceData();

        if (arScan != null) arScan.setOnClickListener(v -> startActivity(new Intent(StudentDashboard.this, ArScan.class)));
        if (bibleStories != null) bibleStories.setOnClickListener(v -> startActivity(new Intent(StudentDashboard.this, BibleStories.class)));
        if (attendance != null) attendance.setOnClickListener(v -> {
            Intent intent = new Intent(StudentDashboard.this, Attendance.class);
            intent.putExtra("userType", "Student");
            startActivity(intent);
        });

        if (navStories != null) navStories.setOnClickListener(v->{ startActivity(new Intent(this, BibleStories.class));});
        if (navLeaderboard != null) navLeaderboard.setOnClickListener(v->{ startActivity(new Intent(this, Leaderboards.class));});
        if (navProfile != null) navProfile.setOnClickListener(v->{ startActivity(new Intent(this, StudentProfile.class));});
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

                            if (username != null && !username.isEmpty() && tvStudentName != null) {
                                tvStudentName.setText(username);
                            }

                            long totalXP = documentSnapshot.contains("totalXP") ? documentSnapshot.getLong("totalXP") : 0;
                            if (tvXPValue != null) tvXPValue.setText(String.format("%,d", totalXP));
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (isFinishing()) return;
                        Toast.makeText(this, "Error fetching user data", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void fetchAttendanceData() {
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            db.collection("attendance").whereEqualTo("studentUid", uid).get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (isFinishing()) return;
                        Set<String> uniqueDates = new HashSet<>();
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String date = doc.getString("date");
                            if (date != null) uniqueDates.add(date);
                        }
                        int attendanceCount = uniqueDates.size();
                        if (tvStreakLabel != null) tvStreakLabel.setText("Total Attendance");

                        if (tvStreakValue != null) {
                            if (attendanceCount <= 1) {
                                tvStreakValue.setText(attendanceCount + " Sundays");
                            } else {
                                tvStreakValue.setText(attendanceCount + " Sundays 🔥");
                            }
                        }
                    });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchUserData();
        fetchAttendanceData();
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
        if (ssoListener != null) {
            ssoListener.remove();
        }
    }
}
