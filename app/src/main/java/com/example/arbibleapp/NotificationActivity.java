package com.example.arbibleapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;
    private final List<NotificationModel> notificationList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration quizListener, attendanceFeedListener, userListener;
    private static final int XP_PER_LEVEL = 250;
    
    private SharedPreferences prefs;
    private Set<String> dismissedIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notification);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        prefs = getSharedPreferences("dismissed_notifications", Context.MODE_PRIVATE);
        dismissedIds = new HashSet<>(prefs.getStringSet("ids", new HashSet<>()));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnClearAll).setOnClickListener(v -> clearAllNotifications());

        rvNotifications = findViewById(R.id.rvNotifications);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(notificationList);
        rvNotifications.setAdapter(adapter);

        loadNotifications();
    }

    private void dismissNotification(String id) {
        dismissedIds.add(id);
        prefs.edit().putStringSet("ids", dismissedIds).apply();
        notificationList.removeIf(n -> id.equals(n.id));
        adapter.notifyDataSetChanged();
    }

    private void clearAllNotifications() {
        for (NotificationModel n : notificationList) {
            dismissedIds.add(n.id);
        }
        prefs.edit().putStringSet("ids", dismissedIds).apply();
        notificationList.clear();
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (quizListener != null) quizListener.remove();
        if (attendanceFeedListener != null) attendanceFeedListener.remove();
        if (userListener != null) userListener.remove();
    }

    private void loadNotifications() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        notificationList.clear();

        // 1. Load User Level with Real-time listener
        if (userListener != null) userListener.remove();
        userListener = db.collection("users").document(uid).addSnapshotListener((userDoc, e) -> {
            if (e != null || userDoc == null || !userDoc.exists()) return;

            notificationList.removeIf(n -> "level".equals(n.type));

            Long userXpLong = userDoc.contains("totalXP") ? userDoc.getLong("totalXP") : null;
            Long userLevelLong = userDoc.contains("level") ? userDoc.getLong("level") : null;
            long activeUserXP = userXpLong != null ? userXpLong : 0;
            int storedLevel = userLevelLong != null ? userLevelLong.intValue() : 1;

            int maxLevelForUser = Math.max((int) (activeUserXP / XP_PER_LEVEL) + 1, storedLevel);
            if (maxLevelForUser > 10) maxLevelForUser = 10;

            for (int lvl = 2; lvl <= maxLevelForUser; lvl++) {
                String levelId = "level_" + lvl;
                if (!dismissedIds.contains(levelId)) {
                    notificationList.add(new NotificationModel(
                            levelId,
                            "Level Up!",
                            "Congratulations! You've reached Level " + lvl + " - " + getLevelName(lvl) + "!",
                            R.drawable.ic_quiz_champion,
                            "#FFF9C4",
                            Timestamp.now(),
                            "level"
                    ));
                }
            }
            sortAndRefresh();
        });

        // 2. Load Attendance with Real-time listener
        if (attendanceFeedListener != null) attendanceFeedListener.remove();
        attendanceFeedListener = db.collection("attendance")
                .whereEqualTo("studentUid", uid)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    notificationList.removeIf(n -> "attendance".equals(n.type) || "attendance_badge".equals(n.type));

                    Set<String> uniqueDates = new HashSet<>();
                    Timestamp latestAttendanceTs = null;

                    for (DocumentSnapshot doc : snapshots) {
                        String docId = doc.getId();
                        String date = doc.getString("date");
                        if (date != null) {
                            uniqueDates.add(date);
                            Timestamp ts = doc.getTimestamp("fullTimestamp");
                            if (ts != null && (latestAttendanceTs == null || ts.compareTo(latestAttendanceTs) > 0)) {
                                latestAttendanceTs = ts;
                            }
                            
                            if (!dismissedIds.contains(docId)) {
                                notificationList.add(new NotificationModel(
                                        docId,
                                        "Attendance Marked!",
                                        "Your presence has been recorded for " + date + ".",
                                        R.drawable.ic_streak,
                                        "#E0F2F1",
                                        ts,
                                        "attendance"
                                ));
                            }
                        }
                    }
                    addAttendanceBadges(uniqueDates.size(), latestAttendanceTs);
                    sortAndRefresh();
                });

        // 3. Load Quiz Results with Real-time listener
        if (quizListener != null) quizListener.remove();
        quizListener = db.collection("quiz_results")
                .whereEqualTo("studentUid", uid)
                .addSnapshotListener((results, e) -> {
                    if (e != null || results == null) return;

                    notificationList.removeIf(n -> "quiz".equals(n.type) || "quiz_badge".equals(n.type));

                    List<DocumentSnapshot> sortedResults = new ArrayList<>(results.getDocuments());
                    Collections.sort(sortedResults, (d1, d2) -> {
                        Timestamp t1 = d1.getTimestamp("timestamp");
                        Timestamp t2 = d2.getTimestamp("timestamp");
                        if (t1 == null) return -1;
                        if (t2 == null) return 1;
                        return t1.compareTo(t2);
                    });

                    Set<String> uniqueStories = new HashSet<>();
                    Set<String> processedQuizKeys = new HashSet<>();
                    Timestamp latestQuizTs = null;

                    for (DocumentSnapshot doc : sortedResults) {
                        String docId = doc.getId();
                        String story = doc.getString("storyTitle");
                        Long score = doc.getLong("score");
                        Long total = doc.getLong("totalQuestions");
                        Long xp = doc.getLong("xp");
                        Boolean isSunday = doc.getBoolean("isSunday");
                        Timestamp ts = doc.getTimestamp("timestamp");

                        if (ts != null && (latestQuizTs == null || ts.compareTo(latestQuizTs) > 0)) {
                            latestQuizTs = ts;
                        }

                        if (story != null) uniqueStories.add(story);

                        if (!dismissedIds.contains(docId)) {
                            long timeGroup = ts != null ? (ts.getSeconds() / 10) : 0;
                            String dedupKey = story + "_" + score + "_" + timeGroup;

                            if (!processedQuizKeys.contains(dedupKey)) {
                                processedQuizKeys.add(dedupKey);

                                String typeLabel = (isSunday != null && !isSunday) ? "Trial Completed" : "Quiz Submitted";
                                String xpText = (isSunday != null && isSunday && xp != null) ? " + " + xp + " XP" : " (Trial - No XP)";

                                notificationList.add(new NotificationModel(
                                        docId,
                                        typeLabel,
                                        "You scored " + (score != null ? score : 0) + "/" + (total != null ? total : 0) + " in " + story + "." + xpText,
                                        R.drawable.ic_quiz,
                                        isSunday != null && isSunday ? "#E8F5E9" : "#F5F5F5",
                                        ts,
                                        "quiz"
                                ));
                            }
                        }
                    }

                    addQuizBadges(uniqueStories.size(), latestQuizTs);
                    checkLeaderboardStatus(uid);
                    sortAndRefresh();
                });
    }

    private void checkLeaderboardStatus(String uid) {
        db.collection("users")
                .whereEqualTo("userType", "Student")
                .orderBy("totalXP", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(snapshots -> {
                    notificationList.removeIf(n -> "leaderboard".equals(n.type));
                    int rank = 1;
                    for (DocumentSnapshot doc : snapshots) {
                        if (doc.getId().equals(uid)) {
                            String boardId = "leaderboard_" + rank;
                            if (!dismissedIds.contains(boardId)) {
                                notificationList.add(new NotificationModel(
                                        boardId,
                                        "Leaderboard Alert",
                                        "You are currently ranked #" + rank + " globally! Keep it up!",
                                        android.R.drawable.btn_star_big_on,
                                        "#E1BEE7",
                                        Timestamp.now(),
                                        "leaderboard"
                                ));
                            }
                            sortAndRefresh();
                            break;
                        }
                        rank++;
                    }
                });
    }

    private void addAttendanceBadges(int count, Timestamp ts) {
        if (count >= 1) addBadgeIfEarned("Best Worshipper", R.drawable.ic_worshippper, "#B2DFDB", "attendance_badge", ts);
        if (count >= 2) addBadgeIfEarned("Growing Disciple", R.drawable.ic_growing_disciple, "#B3E5FC", "attendance_badge", ts);
        if (count >= 3) addBadgeIfEarned("Faithfulness", R.drawable.ic_faithfulness, "#FFF9C4", "attendance_badge", ts);
        if (count >= 4) addBadgeIfEarned("God’s House", R.drawable.ic_gods_house, "#FFE0B2", "attendance_badge", ts);
        if (count >= 5) addBadgeIfEarned("Super Faithful", R.drawable.ic_super_faithfull, "#FFCDD2", "attendance_badge", ts);
    }

    private void addQuizBadges(int count, Timestamp ts) {
        if (count >= 1) addBadgeIfEarned("Tryer", R.drawable.ic_tryer, "#F0F4C3", "quiz_badge", ts);
        if (count >= 2) addBadgeIfEarned("Learner", R.drawable.ic_little_learner, "#E1BEE7", "quiz_badge", ts);
        if (count >= 3) addBadgeIfEarned("Scholar", R.drawable.ic_growing_scholar, "#B2EBF2", "quiz_badge", ts);
        if (count >= 4) addBadgeIfEarned("Explorer", R.drawable.ic_bible_explorer, "#C5CAE9", "quiz_badge", ts);
        if (count >= 5) addBadgeIfEarned("Champion", R.drawable.ic_quiz_champion, "#F8BBD0", "quiz_badge", ts);
    }

    private void addBadgeIfEarned(String name, int icon, String color, String type, Timestamp ts) {
        String badgeId = "badge_" + name.replace(" ", "_");
        if (!dismissedIds.contains(badgeId)) {
            boolean alreadyInList = false;
            for (NotificationModel n : notificationList) {
                if (badgeId.equals(n.id)) {
                    alreadyInList = true;
                    break;
                }
            }
            if (!alreadyInList) {
                notificationList.add(new NotificationModel(
                        badgeId,
                        "Achievement Unlocked!",
                        "You've earned the " + name + " badge!",
                        icon,
                        color,
                        ts != null ? ts : Timestamp.now(),
                        type
                ));
            }
        }
    }

    private String getLevelName(int level) {
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

    private void sortAndRefresh() {
        Collections.sort(notificationList, (n1, n2) -> {
            Timestamp t1 = n1.timestamp != null ? n1.timestamp : new Timestamp(0, 0);
            Timestamp t2 = n2.timestamp != null ? n2.timestamp : new Timestamp(0, 0);
            return t2.compareTo(t1); // Descending
        });
        adapter.notifyDataSetChanged();
    }

    private static class NotificationModel {
        String id, title, desc, color, type;
        int iconRes;
        Timestamp timestamp;

        NotificationModel(String id, String title, String desc, int iconRes, String color, Timestamp ts, String type) {
            this.id = id;
            this.title = title;
            this.desc = desc;
            this.iconRes = iconRes;
            this.color = color;
            this.timestamp = ts;
            this.type = type;
        }
    }

    private class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
        private final List<NotificationModel> list;
        NotificationAdapter(List<NotificationModel> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            NotificationModel n = list.get(position);
            holder.tvTitle.setText(n.title);
            holder.tvDesc.setText(n.desc);
            holder.ivIcon.setImageResource(n.iconRes);
            holder.viewIndicator.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(n.color)));
            
            if (n.timestamp != null) {
                String time = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(n.timestamp.toDate());
                holder.tvTime.setText(time);
                holder.tvTime.setVisibility(View.VISIBLE);
            } else {
                holder.tvTime.setVisibility(View.GONE);
            }

            holder.btnDelete.setOnClickListener(v -> dismissNotification(n.id));
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDesc, tvTime;
            ImageView ivIcon, btnDelete;
            View viewIndicator;
            ViewHolder(View v) {
                super(v);
                tvTitle = v.findViewById(R.id.tvNotifTitle);
                tvDesc = v.findViewById(R.id.tvNotifDesc);
                tvTime = v.findViewById(R.id.tvNotifTime);
                ivIcon = v.findViewById(R.id.ivIcon);
                viewIndicator = v.findViewById(R.id.viewIndicator);
                btnDelete = v.findViewById(R.id.btnDelete);
            }
        }
    }
}
