package com.example.arbibleapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashSet;
import java.util.Set;

public class StudentProfile extends AppCompatActivity {

    private ImageView btnBack;
    private LinearLayout navHome, navStories, navLeaderboard, navProfile;
    private Button btnLogout;
    private TextView tvStudentName, tvQuizCount, tvAttendanceCount, tvBadgesSummary, tvBadgesCount;
    private TextView tvLevelLabel, tvXPProgressText;
    private ProgressBar pbXPLevel;
    
    private View badgeWorshipper, badgeDisciple, badgeFaithfulness, badgeGodsHouse, badgeSuperFaithful;
    private View badgeTryer, badgeLearner, badgeScholar, badgeExplorer, badgeChampion;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private static final int XP_PER_LEVEL = 250;

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

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupNavigation();
        fetchUserData();
        loadAchievements();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnLogout = findViewById(R.id.btnLogout);
        tvStudentName = findViewById(R.id.tvStudentName);
        tvQuizCount = findViewById(R.id.tvQuizCount);
        tvAttendanceCount = findViewById(R.id.tvAttendanceCount);
        tvBadgesSummary = findViewById(R.id.tvBadgesSummary);
        tvBadgesCount = findViewById(R.id.tvBadgesCount);
        tvLevelLabel = findViewById(R.id.tvLevelLabel);
        tvXPProgressText = findViewById(R.id.tvXPProgressText);
        pbXPLevel = findViewById(R.id.pbXPLevel);

        badgeWorshipper = findViewById(R.id.badgeWorshipper);
        badgeDisciple = findViewById(R.id.badgeDisciple);
        badgeFaithfulness = findViewById(R.id.badgeFaithfulness);
        badgeGodsHouse = findViewById(R.id.badgeGodsHouse);
        badgeSuperFaithful = findViewById(R.id.badgeSuperFaithful);

        setupBadgeUI(badgeWorshipper,"Best Worshipper", R.drawable.ic_worshippper);
        setupBadgeUI(badgeDisciple, "Growing Disciple", R.drawable.ic_growing_disciple);
        setupBadgeUI(badgeFaithfulness, "Faithfulness", R.drawable.ic_faithfulness);
        setupBadgeUI(badgeGodsHouse, "God’s House", R.drawable.ic_gods_house);
        setupBadgeUI(badgeSuperFaithful, "Super Faithful", R.drawable.ic_super_faithfull);

        badgeTryer = findViewById(R.id.badgeTryer);
        badgeLearner = findViewById(R.id.badgeLearner);
        badgeScholar = findViewById(R.id.badgeScholar);
        badgeExplorer = findViewById(R.id.badgeExplorer);
        badgeChampion = findViewById(R.id.badgeChampion);

        setupBadgeUI(badgeTryer,  "Tryer", R.drawable.ic_tryer);
        setupBadgeUI(badgeLearner, "Learner", R.drawable.ic_little_learner);
        setupBadgeUI(badgeScholar, "Scholar", R.drawable.ic_growing_scholar);
        setupBadgeUI(badgeExplorer, "Explorer", R.drawable.ic_bible_explorer);
        setupBadgeUI(badgeChampion,  "Champion", R.drawable.ic_quiz_champion);
    }

    private void setupBadgeUI(View badgeView, String name, int imageRes) {
        if (badgeView != null) {
            TextView tvIcon = badgeView.findViewById(R.id.tvBadgeIcon);
            ImageView ivIcon = badgeView.findViewById(R.id.ivBadgeIcon);
            TextView tvName = badgeView.findViewById(R.id.tvBadgeName);

            tvName.setText(name);

            if (imageRes != 0) {
                tvIcon.setVisibility(View.GONE);
                ivIcon.setVisibility(View.VISIBLE);
                ivIcon.setImageResource(imageRes);
            } else {
                tvIcon.setVisibility(View.VISIBLE);
                ivIcon.setVisibility(View.GONE);
            }
        }
    }

    private void setupNavigation() {
        btnBack.setOnClickListener(v -> finish());

        navHome = findViewById(R.id.navHome);
        navStories = findViewById(R.id.navStories);
        navLeaderboard = findViewById(R.id.navLeaderboard);
        navProfile = findViewById(R.id.navProfile);

        navHome.setOnClickListener(v -> startActivity(new Intent(this, StudentDashboard.class)));
        navStories.setOnClickListener(v -> startActivity(new Intent(this, BibleStories.class)));
        navLeaderboard.setOnClickListener(v -> startActivity(new Intent(this, Leaderboards.class)));

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(StudentProfile.this, login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void fetchUserData() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String username = documentSnapshot.getString("username");
                if (username == null || username.isEmpty()) {
                    username = documentSnapshot.getString("fullName");
                }
                tvStudentName.setText(username != null ? username : "Student");

                long totalXP = documentSnapshot.contains("totalXP") ? documentSnapshot.getLong("totalXP") : 0;
                updateLevelUI(totalXP);
            }
        });
    }

    private void updateLevelUI(long totalXP) {
        int level = (int) (totalXP / XP_PER_LEVEL) + 1;
        if (level > 10) level = 10;

        long currentLevelXP = totalXP % XP_PER_LEVEL;
        
        String levelName;
        switch (level) {
            case 1: levelName = "Beginner"; break;
            case 2: levelName = "Learner"; break;
            case 3: levelName = "Explorer"; break;
            case 4: levelName = "Scholar"; break;
            case 5: levelName = "Faithful"; break;
            case 6: levelName = "Disciple"; break;
            case 7: levelName = "Servant"; break;
            case 8: levelName = "Warrior"; break;
            case 9: levelName = "Ambassador"; break;
            case 10: levelName = "Bible Master"; break;
            default: levelName = "Bible Master"; break;
        }

        tvLevelLabel.setText("Level " + level + " - " + levelName);
        
        if (level < 10) {
            pbXPLevel.setMax(XP_PER_LEVEL);
            pbXPLevel.setProgress((int) currentLevelXP);
            tvXPProgressText.setText(currentLevelXP + " / " + XP_PER_LEVEL + " XP");
        } else {
            pbXPLevel.setMax(XP_PER_LEVEL);
            pbXPLevel.setProgress(XP_PER_LEVEL);
            tvXPProgressText.setText("Max Level (Bible Master)");
        }
    }

    private void loadAchievements() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("attendance").whereEqualTo("studentUid", uid).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Set<String> uniqueDates = new HashSet<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String date = doc.getString("date");
                        if (date != null) uniqueDates.add(date);
                    }
                    int attendanceCount = uniqueDates.size();
                    tvAttendanceCount.setText(String.valueOf(attendanceCount));
                    updateAttendanceBadges(attendanceCount);
                });

        db.collection("quiz_results").whereEqualTo("studentUid", uid).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Set<String> uniqueStories = new HashSet<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String title = doc.getString("storyTitle");
                        if (title != null) uniqueStories.add(title);
                    }
                    int quizCount = uniqueStories.size();
                    tvQuizCount.setText(String.valueOf(quizCount));
                    updateQuizBadges(quizCount);
                });
    }

    private void updateAttendanceBadges(int count) {
        setUnlocked(badgeWorshipper, count >= 1);
        setUnlocked(badgeDisciple, count >= 2);
        setUnlocked(badgeFaithfulness, count >= 3);
        setUnlocked(badgeGodsHouse, count >= 4);
        setUnlocked(badgeSuperFaithful, count >= 5);
        updateBadgeTotals();
    }

    private void updateQuizBadges(int count) {
        setUnlocked(badgeTryer, count >= 1);
        setUnlocked(badgeLearner, count >= 2);
        setUnlocked(badgeScholar, count >= 3);
        setUnlocked(badgeExplorer, count >= 4);
        setUnlocked(badgeChampion, count >= 5);
        updateBadgeTotals();
    }

    private void setUnlocked(View badgeView, boolean unlocked) {
        if (badgeView != null) {
            badgeView.setAlpha(unlocked ? 1.0f : 0.2f);
        }
    }

    private void updateBadgeTotals() {
        int count = 0;
        View[] allBadges = {badgeWorshipper, badgeDisciple, badgeFaithfulness, badgeGodsHouse, badgeSuperFaithful,
                           badgeTryer, badgeLearner, badgeScholar, badgeExplorer, badgeChampion};
        
        for (View b : allBadges) {
            if (b != null && b.getAlpha() == 1.0f) {
                count++;
            }
        }
        
        tvBadgesCount.setText(count + " of 10");
        tvBadgesSummary.setText(count + "/10");
    }
}
