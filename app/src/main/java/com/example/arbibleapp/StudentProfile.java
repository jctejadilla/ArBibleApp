package com.example.arbibleapp;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StudentProfile extends AppCompatActivity {

    private ImageView btnBack;
    private Button btnLogout;
    private TextView tvStudentName, tvQuizCount, tvAttendanceCount, tvBadgesSummary, tvBadgesCount, tvStoriesCount;
    private TextView tvLevelLabel, tvXPProgressText;
    private ProgressBar pbXPLevel;
    
    private View badgeWorshipper, badgeDisciple, badgeFaithfulness, badgeGodsHouse, badgeSuperFaithful;
    private View badgeTryer, badgeLearner, badgeScholar, badgeExplorer, badgeChampion;

    private EditText etUsername;
    private RadioGroup rgGender;
    private RadioButton rbMale, rbFemale;
    private TextView tvCurrentEmail;
    private Button btnSaveChanges, btnCancelChanges, btnChangeEmail, btnChangePassword;

    private String originalUsername = "";
    private String originalGender = "";

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
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);

            View topBar = findViewById(R.id.topBar);
            if (topBar != null) {
                topBar.setPadding(
                        topBar.getPaddingLeft(),
                        systemBars.top + 12,
                        topBar.getPaddingRight(),
                        topBar.getPaddingBottom()
                );
            }
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupNavigation();
        fetchUserData();
        loadAchievements();
        loadQuizzes();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnLogout = findViewById(R.id.btnLogout);
        tvStudentName = findViewById(R.id.tvStudentName);
        tvStoriesCount = findViewById(R.id.tvStoriesCount);
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
        
        etUsername = findViewById(R.id.etUsername);
        rgGender = findViewById(R.id.rgGender);
        rbMale = findViewById(R.id.rbMale);
        rbFemale = findViewById(R.id.rbFemale);
        tvCurrentEmail = findViewById(R.id.tvCurrentEmail);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        btnCancelChanges = findViewById(R.id.btnCancelChanges);
        btnChangeEmail = findViewById(R.id.btnChangeEmail);
        btnChangePassword = findViewById(R.id.btnChangePassword);
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

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(StudentProfile.this, login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
        
        btnCancelChanges.setOnClickListener(v -> restoreOriginalAccountDetails());
        btnSaveChanges.setOnClickListener(v -> saveAccountDetails());
        btnChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(StudentProfile.this, ForgotPassword.class);
            if (mAuth.getCurrentUser() != null) {
                intent.putExtra("email", mAuth.getCurrentUser().getEmail());
            }
            startActivity(intent);
        });
        btnChangeEmail.setOnClickListener(v -> promptChangeEmail());
    }

    private void restoreOriginalAccountDetails() {
        if (etUsername != null) etUsername.setText(originalUsername);
        if (originalGender.equalsIgnoreCase("Male")) {
            rbMale.setChecked(true);
        } else if (originalGender.equalsIgnoreCase("Female")) {
            rbFemale.setChecked(true);
        } else {
            rgGender.clearCheck();
        }
    }

    private void saveAccountDetails() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        
        String newUsername = etUsername.getText().toString().trim();
        String tempGender;
        if (rbMale.isChecked()) tempGender = "Male";
        else if (rbFemale.isChecked()) tempGender = "Female";
        else tempGender = "";
        
        final String newGender = tempGender;
        
        if (newUsername.isEmpty()) {
            Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        
        db.collection("users").document(uid)
                .update("username", newUsername, "gender", newGender)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Account details saved", Toast.LENGTH_SHORT).show();
                    originalUsername = newUsername;
                    originalGender = newGender;
                    updateHeaderUsername(newUsername);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save details", Toast.LENGTH_SHORT).show());
    }

    private void updateHeaderUsername(String username) {
        if (username != null && !username.isEmpty()) {
            if (!username.startsWith("Explorer") && !username.endsWith("!")) {
                tvStudentName.setText("Explorer " + username + "!");
            } else {
                tvStudentName.setText(username);
            }
        }
    }

    private void promptChangeEmail() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_change_email);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        
        EditText etCurrentPassword = dialog.findViewById(R.id.etCurrentPassword);
        EditText etNewEmail = dialog.findViewById(R.id.etNewEmail);
        Button btnCancel = dialog.findViewById(R.id.btnCancelEmailChange);
        Button btnConfirm = dialog.findViewById(R.id.btnConfirmEmailChange);
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnConfirm.setOnClickListener(v -> {
            String password = etCurrentPassword.getText().toString().trim();
            String newEmail = etNewEmail.getText().toString().trim();
            
            if (password.isEmpty() || newEmail.isEmpty()) {
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String currentEmail = user.getEmail();
            if (currentEmail == null) {
                Toast.makeText(this, "Current email not found", Toast.LENGTH_SHORT).show();
                return;
            }
            
            AuthCredential credential = EmailAuthProvider.getCredential(currentEmail, password);
            user.reauthenticate(credential).addOnSuccessListener(aVoid -> user.verifyBeforeUpdateEmail(newEmail).addOnSuccessListener(aVoid2 -> {
                Toast.makeText(this, "Verification email sent to " + newEmail + ". You will now be logged out.", Toast.LENGTH_LONG).show();
                db.collection("users").document(user.getUid())
                        .update("email", newEmail)
                        .addOnSuccessListener(aVoid3 -> {
                            dialog.dismiss();
                            mAuth.signOut();
                            Intent intent = new Intent(StudentProfile.this, login.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        });
            }).addOnFailureListener(e -> Toast.makeText(this, "Failed to change email: " + e.getMessage(), Toast.LENGTH_SHORT).show())).addOnFailureListener(e -> Toast.makeText(this, "Authentication failed. Check your password.", Toast.LENGTH_SHORT).show());
        });
        
        dialog.show();
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
                
                originalUsername = username != null ? username : "";
                if (etUsername != null) {
                    etUsername.setText(originalUsername);
                }
                
                originalGender = documentSnapshot.getString("gender");
                if (originalGender == null) originalGender = "";
                
                ImageView ivAvatar = findViewById(R.id.ivAvatar);
                
                if (originalGender.equalsIgnoreCase("Male")) {
                    rbMale.setChecked(true);
                    if (ivAvatar != null) ivAvatar.setImageResource(R.drawable.ic_def_avatar);
                } else if (originalGender.equalsIgnoreCase("Female")) {
                    rbFemale.setChecked(true);
                    if (ivAvatar != null) ivAvatar.setImageResource(R.drawable.ic_def_avatar_fem);
                } else {
                    if (ivAvatar != null) ivAvatar.setImageResource(R.drawable.ic_def_avatar);
                }
                
                if (tvCurrentEmail != null && mAuth.getCurrentUser().getEmail() != null) {
                    tvCurrentEmail.setText(mAuth.getCurrentUser().getEmail());
                }

                updateHeaderUsername(originalUsername);

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
            tvXPProgressText.setText(currentLevelXP + "/" + XP_PER_LEVEL + " XP");
        } else {
            pbXPLevel.setMax(XP_PER_LEVEL);
            pbXPLevel.setProgress(XP_PER_LEVEL);
            tvXPProgressText.setText("Max Level");
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
                    
                    int streak = calculateSundayStreak(uniqueDates);
                    TextView tvStreakValue = findViewById(R.id.tvStreakValue);
                    if (tvStreakValue != null) {
                        tvStreakValue.setText(streak + " Sunday Streak 🔥");
                    }

                    updateAttendanceBadges(attendanceCount);
                });
    }

    private int calculateSundayStreak(Set<String> uniqueDates) {
        if (uniqueDates == null || uniqueDates.isEmpty()) return 0;
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<LocalDate> sundays = new ArrayList<>();
        for (String d : uniqueDates) {
            try {
                LocalDate date = LocalDate.parse(d, formatter);
                if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                    sundays.add(date);
                }
            } catch (Exception e) {
                // ignore
            }
        }
        
        if (sundays.isEmpty()) return 0;
        sundays.sort(Collections.reverseOrder());
        
        LocalDate today = LocalDate.now();
        LocalDate thisSunday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate lastSunday = thisSunday.minusWeeks(1);
        
        LocalDate mostRecentSunday = sundays.get(0);
        if (!mostRecentSunday.equals(thisSunday) && !mostRecentSunday.equals(lastSunday)) {
            return 0;
        }
        
        int streak = 1;
        for (int i = 1; i < sundays.size(); i++) {
            if (sundays.get(i - 1).minusWeeks(1).equals(sundays.get(i))) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    private void loadQuizzes() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        
        db.collection("quiz_results").whereEqualTo("studentUid", uid).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Set<String> uniqueStories = new HashSet<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Boolean isSunday = doc.getBoolean("isSunday");
                        if (isSunday != null && !isSunday) continue; // Skip trial quizzes
                        
                        String title = doc.getString("storyTitle");
                        if (title != null) uniqueStories.add(title);
                    }
                    int quizCount = uniqueStories.size();
                    tvQuizCount.setText(String.valueOf(quizCount));
                    if (tvStoriesCount != null) {
                        tvStoriesCount.setText(String.valueOf(quizCount));
                    }
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
