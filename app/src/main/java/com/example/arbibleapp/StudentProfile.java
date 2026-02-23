package com.example.arbibleapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class StudentProfile extends AppCompatActivity {

    ImageView btnBack;
    LinearLayout navHome, navStories, navLeaderboard, navProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_student_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnBack = findViewById(R.id.btnBack);

        navHome = findViewById(R.id.navHome);
        navStories = findViewById(R.id.navStories);
        navLeaderboard = findViewById(R.id.navLeaderboard);
        navProfile = findViewById(R.id.navProfile);

        btnBack.setOnClickListener(v -> { finish(); });

        navHome.setOnClickListener(v->{ startActivity(new Intent(this, StudentDashboard.class));});
        navStories.setOnClickListener(v->{ startActivity(new Intent(this, BibleStories.class));});
        navLeaderboard.setOnClickListener(v->{ startActivity(new Intent(this, Leaderboards.class));});
        navProfile.setOnClickListener(v->{ startActivity(new Intent(this, StudentProfile.class));});


        LinearLayout navHome = findViewById(R.id.navHome);
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(StudentProfile.this, StudentDashboard.class);
            startActivity(intent);
            finish();
        });
    }
}