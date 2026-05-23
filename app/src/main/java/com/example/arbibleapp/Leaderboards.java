package com.example.arbibleapp;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Leaderboards extends AppCompatActivity {

    private ImageView btnBack;
    private MaterialButtonToggleGroup toggleGroup;
    private Button btnGlobal, btnByStory, btnByClass;
    private LinearLayout layoutLeaderboardList;
    private TextView tvUserRank, tvUserPoints, tvXPToRankup, tvSubtitle;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String userClassTeacherUid = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_leaderboards);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        btnBack = findViewById(R.id.btnBack);
        toggleGroup = findViewById(R.id.toggleGroupLeaderboard);
        btnGlobal = findViewById(R.id.btnGlobal);
        btnByStory = findViewById(R.id.btnByStory);
        btnByClass = findViewById(R.id.btnByClass);
        
        layoutLeaderboardList = findViewById(R.id.layoutLeaderboardList);
        tvUserRank = findViewById(R.id.tvUserRank);
        tvUserPoints = findViewById(R.id.tvUserPoints);
        tvXPToRankup = findViewById(R.id.tvXPToRankup);
        tvSubtitle = findViewById(R.id.tvSubtitle);

        btnBack.setOnClickListener(v -> finish());
        
        setupNavigation();
        setupResetNotice();

        checkEnrollmentAndSetupToggle();
    }

    private void setupResetNotice() {
        TextView tvResetNotice = findViewById(R.id.tvResetNotice);
        java.util.Calendar cal = java.util.Calendar.getInstance();

        cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
        while (cal.get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.SUNDAY) {
            cal.add(java.util.Calendar.DAY_OF_MONTH, 1);
        }
        
        java.util.Calendar now = java.util.Calendar.getInstance();
        if (now.after(cal)) {
            cal.add(java.util.Calendar.MONTH, 1);
            cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
            while (cal.get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.SUNDAY) {
                cal.add(java.util.Calendar.DAY_OF_MONTH, 1);
            }
        }

        long diff = cal.getTimeInMillis() - now.getTimeInMillis();
        long days = diff / (24 * 60 * 60 * 1000);
        
        if (days == 0) {
            tvResetNotice.setText("Resets Today!");
        } else {
            tvResetNotice.setText("Resets in " + days + "d");
        }
    }

    private void setupNavigation() {
        // Teacher Nav
        findViewById(R.id.navHomeTeacher).setOnClickListener(v -> {
            startActivity(new Intent(this, TeacherDashboard.class));
            finish();
        });
        findViewById(R.id.navStudentsTab).setOnClickListener(v -> {
            startActivity(new Intent(this, ClassManagement.class));
            finish();
        });
        findViewById(R.id.navAttendanceTab).setOnClickListener(v -> {
            startActivity(new Intent(this, attendance_teacher_main.class));
            finish();
        });
        findViewById(R.id.navAnalyticsTab).setOnClickListener(v -> {
            startActivity(new Intent(this, AnalyticsActivity.class));
            finish();
        });

        // Student Nav
        findViewById(R.id.navHomeStudent).setOnClickListener(v -> {
            startActivity(new Intent(this, StudentDashboard.class));
            finish();
        });
        findViewById(R.id.navStories).setOnClickListener(v -> {
            startActivity(new Intent(this, BibleStories.class));
            finish();
        });
        findViewById(R.id.navProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, StudentProfile.class));
            finish();
        });
    }

    private void checkEnrollmentAndSetupToggle() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users").document(uid).get().addOnSuccessListener(userDoc -> {
            if (userDoc.exists()) {
                String type = userDoc.getString("userType");
                if ("Teacher".equals(type)) {
                    userClassTeacherUid = uid;
                    btnByClass.setVisibility(View.VISIBLE);
                    findViewById(R.id.cardSummary).setVisibility(View.GONE);
                    findViewById(R.id.bottomNavTeacher).setVisibility(View.VISIBLE);
                    findViewById(R.id.bottomNavStudent).setVisibility(View.GONE);
                    setupToggleListeners();
                } else {
                    findViewById(R.id.cardSummary).setVisibility(View.VISIBLE);
                    findViewById(R.id.bottomNavTeacher).setVisibility(View.GONE);
                    findViewById(R.id.bottomNavStudent).setVisibility(View.VISIBLE);
                    db.collection("enrollments").whereEqualTo("studentUid", uid).limit(1).get()
                        .addOnSuccessListener(enrollments -> {
                            if (!enrollments.isEmpty()) {
                                userClassTeacherUid = enrollments.getDocuments().get(0).getString("teacherUid");
                                btnByClass.setVisibility(View.VISIBLE);
                            } else {
                                btnByClass.setVisibility(View.GONE);
                            }
                            setupToggleListeners();
                        });
                }
            }
        });
    }

    private void setupToggleListeners() {
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;

            layoutLeaderboardList.removeAllViews();

            if (checkedId == R.id.btnGlobal) {
                tvSubtitle.setText("Global Top Performers");
                loadGlobalLeaderboard();
            } else if (checkedId == R.id.btnByStory) {
                tvSubtitle.setText("Global Story Rankings");
                loadGlobalStoryLeaderboard();
            } else if (checkedId == R.id.btnByClass) {
                tvSubtitle.setText("My Class Rankings");
                if (userClassTeacherUid != null) {
                    loadClassLeaderboard(userClassTeacherUid);
                } else {
                    showEmptyState("Not enrolled in a class");
                }
            }
        });
        toggleGroup.check(R.id.btnGlobal);
    }

    private void loadGlobalLeaderboard() {
        db.collection("users")
                .whereEqualTo("userType", "Student")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    processXPLeaderboard(queryDocumentSnapshots.getDocuments());
                });
    }

    private void loadClassLeaderboard(String teacherUid) {
        db.collection("enrollments").whereEqualTo("teacherUid", teacherUid).get()
            .addOnSuccessListener(enrollSnapshots -> {
                List<String> studentUids = new ArrayList<>();
                for (DocumentSnapshot doc : enrollSnapshots) {
                    String sUid = doc.getString("studentUid");
                    if (sUid != null) studentUids.add(sUid);
                }

                if (studentUids.isEmpty()) {
                    showEmptyState("No students in this class");
                    return;
                }

                db.collection("users").whereIn("uid", studentUids).get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        processXPLeaderboard(queryDocumentSnapshots.getDocuments());
                    });
            });
    }

    private void processXPLeaderboard(List<DocumentSnapshot> documents) {
        layoutLeaderboardList.removeAllViews();
        List<DocumentSnapshot> sortedDocs = new ArrayList<>(documents);

        Collections.sort(sortedDocs, (d1, d2) -> {
            long xp1 = d1.contains("totalXP") ? d1.getLong("totalXP") : 0;
            long xp2 = d2.contains("totalXP") ? d2.getLong("totalXP") : 0;
            return Long.compare(xp2, xp1);
        });

        String currentUid = mAuth.getCurrentUser().getUid();
        tvUserRank.setText("-");
        tvUserPoints.setText("0");
        tvXPToRankup.setText("-");

        for (int i = 0; i < sortedDocs.size(); i++) {
            DocumentSnapshot doc = sortedDocs.get(i);
            int rank = i + 1;
            String username = doc.getString("username");
            if (username == null) username = doc.getString("fullName");
            if (username == null) username = "Student";
            long xp = doc.contains("totalXP") ? doc.getLong("totalXP") : 0;

            addLeaderboardItem(rank, username, xp, doc.getId().equals(currentUid), false);

            if (doc.getId().equals(currentUid)) {
                tvUserRank.setText(rank + getRankSuffix(rank));
                tvUserPoints.setText(String.valueOf(xp));
                if (i > 0) {
                    long aboveXP = sortedDocs.get(i - 1).contains("totalXP") ? sortedDocs.get(i - 1).getLong("totalXP") : 0;
                    tvXPToRankup.setText(String.valueOf((aboveXP - xp) + 10));
                } else {
                    tvXPToRankup.setText("Top Rank!");
                }
            }
        }
        
        if (sortedDocs.isEmpty()) {
            showEmptyState("No data found");
        }
    }

    private void loadGlobalStoryLeaderboard() {
        db.collection("quiz_results")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    processStoryLeaderboard(queryDocumentSnapshots.getDocuments());
                });
    }

    private void processStoryLeaderboard(List<DocumentSnapshot> results) {
        layoutLeaderboardList.removeAllViews();
        if (results.isEmpty()) {
            showEmptyState("No results found");
            return;
        }

        List<DocumentSnapshot> sortedResults = new ArrayList<>(results);
        Collections.sort(sortedResults, (r1, r2) -> {
            String s1 = r1.getString("storyTitle");
            String s2 = r2.getString("storyTitle");
            if (s1 == null) s1 = "";
            if (s2 == null) s2 = "";
            int titleCompare = s1.compareTo(s2);
            if (titleCompare != 0) return titleCompare;
            
            long sc1 = r1.contains("score") ? r1.getLong("score") : 0;
            long sc2 = r2.contains("score") ? r2.getLong("score") : 0;
            return Long.compare(sc2, sc1);
        });

        String currentUid = mAuth.getCurrentUser().getUid();
        Map<String, DocumentSnapshot> bestScoresPerUser = new LinkedHashMap<>();
        for (DocumentSnapshot doc : sortedResults) {
            String story = doc.getString("storyTitle");
            String sUid = doc.getString("studentUid");
            if (story == null || sUid == null) continue;
            String key = sUid + "_" + story;
            if (!bestScoresPerUser.containsKey(key)) {
                bestScoresPerUser.put(key, doc);
            }
        }
        renderLeaderboard(bestScoresPerUser.values(), currentUid);
    }

    private void renderLeaderboard(Collection<DocumentSnapshot> sortedResults, String currentUid) {
        String lastStory = "";
        int rankInStory = 0;

        for (DocumentSnapshot doc : sortedResults) {
            String story = doc.getString("storyTitle");
            if (story == null) story = "Unknown Story";

            if (!story.equals(lastStory)) {
                addStoryHeader(story);
                lastStory = story;
                rankInStory = 1;
            } else {
                rankInStory++;
            }

            String name = doc.getString("username");
            if (name == null || name.isEmpty()) name = "Student";
            long score = doc.contains("score") ? doc.getLong("score") : 0;
            boolean isMe = currentUid.equals(doc.getString("studentUid"));

            addLeaderboardItem(rankInStory, name, score, isMe, true);
        }
    }

    private void showEmptyState(String message) {
        layoutLeaderboardList.removeAllViews();
        TextView tvEmpty = new TextView(this);
        tvEmpty.setText(message);
        tvEmpty.setGravity(android.view.Gravity.CENTER);
        tvEmpty.setPadding(0, 50, 0, 0);
        layoutLeaderboardList.addView(tvEmpty);
    }

    private void addStoryHeader(String storyTitle) {
        TextView tvHeader = new TextView(this);
        tvHeader.setText(storyTitle);
        tvHeader.setTextSize(18);
        tvHeader.setGravity(android.view.Gravity.CENTER);
        tvHeader.setPadding(0, 0, 0, 24);
        tvHeader.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
        tvHeader.setTypeface(null, Typeface.BOLD);
        layoutLeaderboardList.addView(tvHeader);
    }

    private String getRankSuffix(int rank) {
        if (rank >= 11 && rank <= 13) return "th";
        switch (rank % 10) {
            case 1: return "st";
            case 2: return "nd";
            case 3: return "rd";
            default: return "th";
        }
    }

    private void addLeaderboardItem(int rank, String name, long val, boolean isCurrentUser, boolean isScore) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_leaderboard, layoutLeaderboardList, false);
        
        TextView tvRank = view.findViewById(R.id.tvRank);
        ImageView ivRankIcon = view.findViewById(R.id.ivRankIcon);
        TextView tvUsername = view.findViewById(R.id.tvUsername);
        TextView tvVal = view.findViewById(R.id.tvXP);
        TextView tvPointsLabel = view.findViewById(R.id.tvPoints);

        tvUsername.setText(name);
        tvPointsLabel.setText(isScore ? " Score" : " XP");
        tvVal.setText(String.valueOf(val));

        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) tvUsername.getLayoutParams();

        if (rank == 1) {
            tvRank.setVisibility(View.GONE);
            ivRankIcon.setVisibility(View.VISIBLE);
            ivRankIcon.setImageResource(R.drawable.ic_gold);
            params.startToStart = -1;
            params.startToEnd = R.id.ivRankIcon;
        } else if (rank == 2) {
            tvRank.setVisibility(View.GONE);
            ivRankIcon.setVisibility(View.VISIBLE);
            ivRankIcon.setImageResource(R.drawable.ic_silver);
            params.startToStart = -1;
            params.startToEnd = R.id.ivRankIcon;
        } else if (rank == 3) {
            tvRank.setVisibility(View.GONE);
            ivRankIcon.setVisibility(View.VISIBLE);
            ivRankIcon.setImageResource(R.drawable.ic_bronze);
            params.startToStart = -1;
            params.startToEnd = R.id.ivRankIcon;
        } else {
            tvRank.setVisibility(View.VISIBLE);
            ivRankIcon.setVisibility(View.GONE);
            tvRank.setText(String.valueOf(rank));
            params.startToStart = -1;
            params.startToEnd = R.id.tvRank;
        }
        tvUsername.setLayoutParams(params);

        if (isCurrentUser) {
            view.setBackgroundResource(R.drawable.bg_rounded_card);
            tvUsername.setTextColor(getColor(R.color.white));
            tvVal.setTextColor(getColor(R.color.white));
            tvPointsLabel.setTextColor(getColor(R.color.white));
            view.setAlpha(0.9f);
        } else {
            view.setBackgroundResource(R.drawable.bg_rounded_card1);
            tvUsername.setTextColor(getColor(R.color.text_dark));
            tvVal.setTextColor(getColor(R.color.text_dark));
            tvPointsLabel.setTextColor(getColor(R.color.text_grey));
            view.setAlpha(0.9f);
        }

        layoutLeaderboardList.addView(view);
    }
}
