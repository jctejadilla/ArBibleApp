package com.example.arbibleapp;

import android.content.Intent;
import android.os.Bundle;
import android.app.Dialog;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.view.ViewGroup;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class TeacherProfile extends AppCompatActivity {

    ImageView btnBack;
    Button btnLogout;
    TextView tvTeacherName;

    private EditText etUsername;
    private RadioGroup rgGender;
    private RadioButton rbMale, rbFemale;
    private TextView tvRoleLabel;
    private Button btnSaveChanges, btnCancelChanges, btnChangeEmail, btnChangePassword;
    
    private String originalUsername = "";
    private String originalGender = "";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_teacher_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupNavigation();
        fetchTeacherData();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnLogout = findViewById(R.id.btnLogout);

        tvTeacherName = findViewById(R.id.tvTeacherLabel);

        etUsername = findViewById(R.id.etUsername);
        rgGender = findViewById(R.id.rgGender);
        rbMale = findViewById(R.id.rbMale);
        rbFemale = findViewById(R.id.rbFemale);
        tvRoleLabel = findViewById(R.id.tvRoleLabel);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        btnCancelChanges = findViewById(R.id.btnCancelChanges);
        btnChangeEmail = findViewById(R.id.btnChangeEmail);
        btnChangePassword = findViewById(R.id.btnChangePassword);
    }

    private void setupNavigation() {
        btnBack.setOnClickListener(v -> finish());

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(TeacherProfile.this, login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
        
        btnCancelChanges.setOnClickListener(v -> restoreOriginalAccountDetails());
        btnSaveChanges.setOnClickListener(v -> saveAccountDetails());
        btnChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(TeacherProfile.this, ForgotPassword.class);
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
                    tvTeacherName.setText(newUsername);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save details", Toast.LENGTH_SHORT).show());
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
                            Intent intent = new Intent(TeacherProfile.this, login.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        });
            }).addOnFailureListener(e -> Toast.makeText(this, "Failed to change email: " + e.getMessage(), Toast.LENGTH_SHORT).show())).addOnFailureListener(e -> Toast.makeText(this, "Authentication failed. Check your password.", Toast.LENGTH_SHORT).show());
        });
        
        dialog.show();
    }

    private void fetchTeacherData() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String name = doc.getString("username");
                if (name == null) name = doc.getString("fullName");
                
                originalUsername = name != null ? name : "";
                if (etUsername != null) {
                    etUsername.setText(originalUsername);
                }
                
                originalGender = doc.getString("gender");
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
                
                if (tvRoleLabel != null && mAuth.getCurrentUser().getEmail() != null) {
                    tvRoleLabel.setText(mAuth.getCurrentUser().getEmail());
                }

                tvTeacherName.setText(originalUsername.isEmpty() ? "Teacher" : originalUsername);
            }
        });
    }
}
