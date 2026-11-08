package com.example.arbibleapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;


public class ForgotPassword extends AppCompatActivity {

    private EditText etEmail;
    private Button btnResetPassword;
    private ImageView btnBack;
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();


        etEmail = findViewById(R.id.etEmail);
        
        etEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String currentEmail = etEmail.getText().toString().trim();
                if (!currentEmail.isEmpty() && !currentEmail.endsWith("@gmail.com") && !currentEmail.endsWith("gmail.com")) {
                    if (currentEmail.endsWith("@")) {
                        etEmail.setText(currentEmail + "gmail.com");
                    } else if (!currentEmail.contains("@")) {
                        etEmail.setText(currentEmail + "@gmail.com");
                    }
                } else if (!currentEmail.isEmpty() && currentEmail.endsWith("gmail.com") && !currentEmail.endsWith("@gmail.com")) {
                     etEmail.setText(currentEmail.substring(0, currentEmail.length() - 9) + "@gmail.com");
                }
            }
        });
        if (savedInstanceState == null) etEmail.setText(getIntent().getStringExtra(VerifiedAccountFlow.EXTRA_EMAIL));
        btnResetPassword = findViewById(R.id.btnResetPassword);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        btnResetPassword.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(ForgotPassword.this, "Please enter your email", Toast.LENGTH_SHORT).show();
                return;
            }

            btnResetPassword.setEnabled(false);
            sendResetEmail(email);
        });
    }

    private void sendResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(this, task -> {
                    btnResetPassword.setEnabled(true);
                    if (task.isSuccessful()) {
                        Toast.makeText(ForgotPassword.this, "If this email has an account, a reset link has been sent. Check your inbox and spam folder.", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(ForgotPassword.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
