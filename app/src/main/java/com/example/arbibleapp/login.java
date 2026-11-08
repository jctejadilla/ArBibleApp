package com.example.arbibleapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;


public class login extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        EditText etEmail = findViewById(R.id.etEmail);
        
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
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegister = findViewById(R.id.tvRegister);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);

        LoginModel model = new ViewModelProvider(this).get(LoginModel.class);
        etPassword.setSaveEnabled(false);
        if (savedInstanceState == null) etEmail.setText(getIntent().getStringExtra(VerifiedAccountFlow.EXTRA_EMAIL));
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (!model.busy) { FirebaseAuth.getInstance().signOut(); finish(); }
            }
        });
        model.changes.observe(this, ignored -> {
            btnLogin.setEnabled(!model.busy); tvRegister.setEnabled(!model.busy); tvForgotPassword.setEnabled(!model.busy);
            if (model.error != null) { Toast.makeText(this, model.error, Toast.LENGTH_LONG).show(); model.error = null; }
            if (model.destination != null) VerifiedAccountFlow.open(this, model.destination);
        });
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            model.signIn(email, password);
        });

        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(login.this, Register.class);
            startActivity(intent);
        });

        tvForgotPassword.setOnClickListener(v->{
            Intent intent = new Intent(login.this, ForgotPassword.class)
                    .putExtra(VerifiedAccountFlow.EXTRA_EMAIL, etEmail.getText().toString().trim());
            startActivity(intent);
        });
    }
}
