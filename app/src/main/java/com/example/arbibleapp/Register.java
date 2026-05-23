package com.example.arbibleapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        MaterialButtonToggleGroup toggleUserType = findViewById(R.id.toggleUserType);
        EditText etUsername = findViewById(R.id.etRegisterUsername);
        EditText etEmail = findViewById(R.id.etRegisterEmail);
        EditText etPassword = findViewById(R.id.etRegisterPassword);
        EditText etConfirmPassword = findViewById(R.id.etConfirmPassword);
        RadioGroup rgGender = findViewById(R.id.rgGender);
        Button btnRegister = findViewById(R.id.btnRegister);
        TextView tvLogin = findViewById(R.id.tvLogin);

        btnRegister.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();
            
            String userType = "Student";
            if (toggleUserType.getCheckedButtonId() == R.id.btnTeacherRegister) {
                userType = "Teacher";
            }

            int selectedGenderId = rgGender.getCheckedRadioButtonId();
            String gender = "";
            if (selectedGenderId != -1) {
                RadioButton rbSelected = findViewById(selectedGenderId);
                gender = rbSelected.getText().toString();
            }

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || gender.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!email.endsWith("@gmail.com")) {
                Toast.makeText(this, "Please use a Gmail account", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(this, "Password should be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            registerUser(username, email, password, gender, userType);
        });

        tvLogin.setOnClickListener(v -> {
            finish();
        });
    }

    private void registerUser(String username, String email, String password, String gender, String userType) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToFirestore(user.getUid(), username, email, password, gender, userType);
                        }
                    } else {
                        Toast.makeText(Register.this, "Authentication failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToFirestore(String uid, String username, String email, String password, String gender, String userType) {
        Map<String, Object> user = new HashMap<>();
        user.put("uid", uid);
        user.put("username", username);
        user.put("email", email);
        user.put("password", password);
        user.put("gender", gender);
        user.put("userType", userType);
        user.put("totalXP", 0);

        db.collection("users").document(uid)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(Register.this, "Account Created Successfully!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(Register.this, login.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Register.this, "Error storing data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
