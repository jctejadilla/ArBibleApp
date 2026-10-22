package com.example.arbibleapp;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class StudentDashboard extends AppCompatActivity {

    ConstraintLayout arScan, attendance;
    LinearLayout navHome, navStories, navLeaderboard, navProfile;
    TextView tvStudentName, tvXPValue, tvStreakLabel, tvStreakValue;
    ImageView btnNotifications;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration ssoListener, attendanceListener;
    private boolean isInitialAttendanceLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_student_dashboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);

            View topBar = findViewById(R.id.topBar);
            if (topBar != null) {
                topBar.setPadding(
                        topBar.getPaddingLeft(),
                        systemBars.top + 12,
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

        tvStudentName = findViewById(R.id.tvStudentName);
        tvXPValue = findViewById(R.id.tvXPValue);
        tvStreakLabel = findViewById(R.id.tvStreakLabel);
        tvStreakValue = findViewById(R.id.tvStreakValue);
        btnNotifications = findViewById(R.id.btnNotifications);

        arScan = findViewById(R.id.arScan);
        attendance = findViewById(R.id.attendance);

        navHome = findViewById(R.id.navHome);
        navStories = findViewById(R.id.navStories);
        navLeaderboard = findViewById(R.id.navLeaderboard);
        navProfile = findViewById(R.id.navProfile);

        fetchUserData();
        fetchAttendanceData();

        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> startActivity(new Intent(this, NotificationActivity.class)));
        }

        if (arScan != null) arScan.setOnClickListener(v -> startActivity(new Intent(StudentDashboard.this, ArScan.class)));
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
                                if (!username.startsWith("Explorer") && !username.endsWith("!")) {
                                    tvStudentName.setText("Explorer " + username + "!");
                                } else {
                                    tvStudentName.setText(username);
                                }
                            }

                            long totalXP = documentSnapshot.contains("totalXP") ? documentSnapshot.getLong("totalXP") : 0;
                            
                            int XP_PER_LEVEL = 250;
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

                            if (tvStreakLabel != null) {
                                tvStreakLabel.setText("Level " + level + " - " + levelName);
                            }

                            ProgressBar progressXP = findViewById(R.id.progressXP);
                            if (progressXP != null) {
                                if (level < 10) {
                                    progressXP.setMax(XP_PER_LEVEL);
                                    progressXP.setProgress((int) currentLevelXP);
                                } else {
                                    progressXP.setMax(XP_PER_LEVEL);
                                    progressXP.setProgress(XP_PER_LEVEL);
                                }
                            }

                            if (tvXPValue != null) {
                                if (level < 10) {
                                    tvXPValue.setText(currentLevelXP + "/" + XP_PER_LEVEL + " XP");
                                } else {
                                    tvXPValue.setText("Max Level");
                                }
                            }
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
                        if (tvStreakValue != null) {
                            tvStreakValue.setText(attendanceCount + " Sunday Streak 🔥");
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
        checkForQuizResult(getIntent());
        setupRealtimeAttendanceListener();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        checkForQuizResult(intent);
    }

    private void checkForQuizResult(Intent intent) {
        if (intent != null) {
            if (intent.getBooleanExtra("SHOW_QUIZ_RESULT", false)) {
                boolean isSunday = intent.getBooleanExtra("IS_SUNDAY", false);
                int score = intent.getIntExtra("SCORE", 0);
                int total = intent.getIntExtra("TOTAL", 0);
                int xp = intent.getIntExtra("XP", 0);
                boolean leveledUp = intent.getBooleanExtra("LEVELED_UP", false);
                int newLevel = intent.getIntExtra("NEW_LEVEL", 1);

                showResultNotification(isSunday, score, total, xp, leveledUp, newLevel);
                intent.removeExtra("SHOW_QUIZ_RESULT");
            } else if (intent.getBooleanExtra("SHOW_ATTENDANCE_RESULT", false)) {
                showAttendanceNotification();
                intent.removeExtra("SHOW_ATTENDANCE_RESULT");
            }
        }
    }

    private void showResultNotification(boolean isSunday, int score, int total, int xp, boolean leveledUp, int newLevel) {
        String title = isSunday ? "Quiz Submitted!" : "Trial Finished!";
        String desc = "You scored " + score + "/" + total + "!";
        if (isSunday) desc += " +" + xp + " XP";
        else desc += " (Trial - No XP)";

        NotificationPopupManager.getInstance().showNotification(
                this,
                title,
                desc,
                R.drawable.ic_quiz
        );

        if (leveledUp) {
            String levelTitle = getLevelTitle(newLevel);
            int badgeIcon = getLevelBadgeIcon(newLevel);
            NotificationPopupManager.getInstance().showNotification(
                    this,
                    "🎉 Level Up! Level " + newLevel,
                    "Congratulations! You've reached Level " + newLevel + " (" + levelTitle + ")!",
                    badgeIcon
            );
        }

        if (mAuth.getCurrentUser() != null) {
            checkQuizBadgesAndNotify(mAuth.getCurrentUser().getUid());
        }
    }

    private int getLevelBadgeIcon(int level) {
        switch (level) {
            case 2: return R.drawable.ic_little_learner;
            case 3: return R.drawable.ic_bible_explorer;
            case 4: return R.drawable.ic_growing_scholar;
            case 5: return R.drawable.ic_faithfulness;
            case 6: return R.drawable.ic_growing_disciple;
            case 7: return R.drawable.ic_worshippper;
            case 8: return R.drawable.ic_tryer;
            case 9: return R.drawable.ic_super_faithfull;
            case 10: return R.drawable.ic_quiz_champion;
            default: return R.drawable.ic_quiz_champion;
        }
    }

    private void checkQuizBadgesAndNotify(String uid) {
        db.collection("quiz_results").whereEqualTo("studentUid", uid).get()
                .addOnSuccessListener(querySnapshots -> {
                    Set<String> uniqueStories = new HashSet<>();
                    for (DocumentSnapshot doc : querySnapshots) {
                        String story = doc.getString("storyTitle");
                        if (story != null) uniqueStories.add(story);
                    }
                    int count = uniqueStories.size();
                    String badgeName = null;
                    int badgeIcon = 0;

                    if (count == 1) { badgeName = "Tryer"; badgeIcon = R.drawable.ic_tryer; }
                    else if (count == 2) { badgeName = "Learner"; badgeIcon = R.drawable.ic_little_learner; }
                    else if (count == 3) { badgeName = "Scholar"; badgeIcon = R.drawable.ic_growing_scholar; }
                    else if (count == 4) { badgeName = "Explorer"; badgeIcon = R.drawable.ic_bible_explorer; }
                    else if (count >= 5) { badgeName = "Champion"; badgeIcon = R.drawable.ic_quiz_champion; }

                    if (badgeName != null) {
                        NotificationPopupManager.getInstance().showNotification(
                                this,
                                "🏆 Achievement Unlocked!",
                                "You've earned the " + badgeName + " badge!",
                                badgeIcon
                        );
                    }
                });
    }

    private String getLevelTitle(int level) {
        switch (level) {
            case 1: return "Beginner";
            case 2: return "Learner";
            case 3: return "Explorer";
            case 4: return "Scholar";
            case 5: return "Faithful";
            case 6: return "Disciple";
            case 7: return "Servant";
            case 8: return "Warrior";
            case 9: return "Ambassador";
            case 10: return "Bible Master";
            default: return "Bible Master";
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (ssoListener != null) {
            ssoListener.remove();
            ssoListener = null;
        }
        if (attendanceListener != null) {
            attendanceListener.remove();
            attendanceListener = null;
        }
    }

    private void setupRealtimeAttendanceListener() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        if (attendanceListener != null) attendanceListener.remove();
        isInitialAttendanceLoad = true;

        attendanceListener = db.collection("attendance")
                .whereEqualTo("studentUid", uid)
                .whereEqualTo("date", today)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    
                    if (isInitialAttendanceLoad) {
                        isInitialAttendanceLoad = false;
                        return;
                    }

                    if (!snapshots.isEmpty()) {
                        showAttendanceNotification();
                    }
                });
    }

    private void showAttendanceNotification() {
        NotificationPopupManager.getInstance().showNotification(
                this,
                "Attendance Marked!",
                "Your presence has been recorded for today.",
                R.drawable.ic_streak
        );

        if (mAuth.getCurrentUser() != null) {
            checkAttendanceBadgesAndNotify(mAuth.getCurrentUser().getUid());
        }
    }

    private void checkAttendanceBadgesAndNotify(String uid) {
        db.collection("attendance").whereEqualTo("studentUid", uid).get()
                .addOnSuccessListener(querySnapshots -> {
                    Set<String> uniqueDates = new HashSet<>();
                    for (DocumentSnapshot doc : querySnapshots) {
                        String date = doc.getString("date");
                        if (date != null) uniqueDates.add(date);
                    }
                    int count = uniqueDates.size();
                    String badgeName = null;
                    int badgeIcon = 0;

                    if (count == 1) { badgeName = "Best Worshipper"; badgeIcon = R.drawable.ic_worshippper; }
                    else if (count == 2) { badgeName = "Growing Disciple"; badgeIcon = R.drawable.ic_growing_disciple; }
                    else if (count == 3) { badgeName = "Faithfulness"; badgeIcon = R.drawable.ic_faithfulness; }
                    else if (count == 4) { badgeName = "God’s House"; badgeIcon = R.drawable.ic_gods_house; }
                    else if (count >= 5) { badgeName = "Super Faithful"; badgeIcon = R.drawable.ic_super_faithfull; }

                    if (badgeName != null) {
                        NotificationPopupManager.getInstance().showNotification(
                                this,
                                "🏆 Achievement Unlocked!",
                                "You've earned the " + badgeName + " badge!",
                                badgeIcon
                        );
                    }
                });
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
