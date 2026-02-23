package com.example.arbibleapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class StudentDashboard extends AppCompatActivity {

    ConstraintLayout arScan, bibleStories, leaderboard, attendance;
    View viewProfile;
    LinearLayout navHome, navStories, navLeaderboard, navProfile;


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


        arScan = findViewById(R.id.arScan);
        bibleStories = findViewById(R.id.bibleStories);
        leaderboard = findViewById(R.id.leaderboard);
        attendance = findViewById(R.id.attendance);

        viewProfile = findViewById(R.id.viewProfile);

        navHome = findViewById(R.id.navHome);
        navStories = findViewById(R.id.navStories);
        navLeaderboard = findViewById(R.id.navLeaderboard);
        navProfile = findViewById(R.id.navProfile);


        arScan.setOnClickListener(v -> startActivity(new Intent(StudentDashboard.this, ArScan.class)));
        bibleStories.setOnClickListener(v -> startActivity(new Intent(StudentDashboard.this, BibleStories.class)));
        leaderboard.setOnClickListener(v -> startActivity(new Intent(StudentDashboard.this, Leaderboards.class)));
        attendance.setOnClickListener(v -> startActivity(new Intent(StudentDashboard.this, Attendance.class)));

        viewProfile.setOnClickListener(v -> startActivity(new Intent(StudentDashboard.this, StudentProfile.class)));

        navStories.setOnClickListener(v->{ startActivity(new Intent(this, BibleStories.class));});
        navLeaderboard.setOnClickListener(v->{ startActivity(new Intent(this, Leaderboards.class));});
        navProfile.setOnClickListener(v->{ startActivity(new Intent(this, StudentProfile.class));});

    }
}