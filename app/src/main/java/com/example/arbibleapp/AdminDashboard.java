package com.example.arbibleapp;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.facebook.shimmer.ShimmerFrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class AdminDashboard extends AppCompatActivity {

    private TextView tvAdminName, tvTeacherCountVal, tvStudentCountVal, tvPendingProposalsVal;
    private CardView cardTeacherMonitoring, cardQuestionApproval, cardLeaderboards, cardApprovalHistory;
    private ShimmerFrameLayout shimmerViewContainer;
    private View btnLogout;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration proposalListener;

    @Override protected void onResume() {
        super.onResume(); VerifiedAccountFlow.guard(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!VerifiedAccountFlow.guard(this)) return;
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_dashboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupClickListeners();
        fetchAdminProfile();
        loadSystemStatistics();
    }

    private void initViews() {
        tvAdminName = findViewById(R.id.tvAdminName);
        tvTeacherCountVal = findViewById(R.id.tvTeacherCountVal);
        tvStudentCountVal = findViewById(R.id.tvStudentCountVal);
        tvPendingProposalsVal = findViewById(R.id.tvPendingProposalsVal);
        shimmerViewContainer = findViewById(R.id.shimmerViewContainer);
        if (shimmerViewContainer != null) shimmerViewContainer.startShimmer();

        btnLogout = findViewById(R.id.btnLogout);

        cardTeacherMonitoring = findViewById(R.id.cardTeacherMonitoring);
        cardQuestionApproval = findViewById(R.id.cardQuestionApproval);
        cardLeaderboards = findViewById(R.id.cardLeaderboards);
        cardApprovalHistory = findViewById(R.id.cardApprovalHistory);
    }

    private void setupClickListeners() {
        cardTeacherMonitoring.setOnClickListener(v -> startActivity(new Intent(this, AdminTeacherMonitoringActivity.class)));
        cardQuestionApproval.setOnClickListener(v -> startActivity(new Intent(this, AdminQuestionApprovalActivity.class)));
        cardLeaderboards.setOnClickListener(v -> startActivity(new Intent(this, Leaderboards.class)));

        if (cardApprovalHistory != null) {
            cardApprovalHistory.setOnClickListener(v -> startActivity(new Intent(this, AdminApprovalHistoryActivity.class)));
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> showLogoutConfirmation());
        }
    }

    private void showLogoutConfirmation() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_confirm_exit);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        ((ImageView) dialog.findViewById(R.id.ivExitIcon)).setImageResource(R.drawable.ic_power_off);
        ((TextView) dialog.findViewById(R.id.tvExitTitle)).setText(R.string.logout_dialog_title);
        ((TextView) dialog.findViewById(R.id.tvExitMessage)).setText(R.string.logout_dialog_message);
        Button stayButton = (Button) dialog.findViewById(R.id.btnExitStay);
        Button logoutButton = (Button) dialog.findViewById(R.id.btnExitLeave);
        stayButton.setText(R.string.logout_dialog_cancel);
        logoutButton.setText(R.string.logout_dialog_confirm);
        stayButton.setOnClickListener(v -> dialog.dismiss());
        logoutButton.setOnClickListener(v -> {
            dialog.dismiss();
            performLogout();
        });
        dialog.show();
    }

    private void performLogout() {
        mAuth.signOut();
        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    private void fetchAdminProfile() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String username = doc.getString("username");
                if (username == null || username.isEmpty()) {
                    username = doc.getString("fullName");
                }
                tvAdminName.setText(username != null ? username : "Admin");
            }
        });
    }

    private void loadSystemStatistics() {
        // 1. Teachers Count
        db.collection("users")
                .whereEqualTo("userType", "Teacher")
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (tvTeacherCountVal != null) {
                        tvTeacherCountVal.setText(String.valueOf(snapshots.size()));
                    }
                });

        // 2. Students Count
        db.collection("users")
                .whereEqualTo("userType", "Student")
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (tvStudentCountVal != null) {
                        tvStudentCountVal.setText(String.valueOf(snapshots.size()));
                    }
                });

        // 3. Pending Proposals Count (Real-time)
        if (proposalListener != null) proposalListener.remove();
        proposalListener = db.collection("question_edit_proposals")
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    if (tvPendingProposalsVal != null) {
                        tvPendingProposalsVal.setText(String.valueOf(snapshots.size()));
                    }
                    if (shimmerViewContainer != null) {
                        shimmerViewContainer.stopShimmer();
                        shimmerViewContainer.setShimmer(null);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (proposalListener != null) {
            proposalListener.remove();
        }
    }
}