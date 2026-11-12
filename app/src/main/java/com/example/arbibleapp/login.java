package com.example.arbibleapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.example.arbibleapp.databinding.ActivityLoginBinding;

public class login extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        binding.etEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String currentEmail = binding.etEmail.getText().toString().trim();
                if (!currentEmail.isEmpty() && !currentEmail.endsWith("@gmail.com") && !currentEmail.endsWith("gmail.com")) {
                    if (currentEmail.endsWith("@")) {
                        binding.etEmail.setText(currentEmail + "gmail.com");
                    } else if (!currentEmail.contains("@")) {
                        binding.etEmail.setText(currentEmail + "@gmail.com");
                    }
                } else if (!currentEmail.isEmpty() && currentEmail.endsWith("gmail.com") && !currentEmail.endsWith("@gmail.com")) {
                     binding.etEmail.setText(currentEmail.substring(0, currentEmail.length() - 9) + "@gmail.com");
                }
            }
        });

        LoginModel model = new ViewModelProvider(this).get(LoginModel.class);
        binding.etPassword.setSaveEnabled(false);
        if (savedInstanceState == null) binding.etEmail.setText(getIntent().getStringExtra(VerifiedAccountFlow.EXTRA_EMAIL));
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (!model.busy) { FirebaseAuth.getInstance().signOut(); finish(); }
            }
        });
        model.changes.observe(this, ignored -> {
            binding.btnLogin.setEnabled(!model.busy); 
            binding.tvRegister.setEnabled(!model.busy); 
            binding.tvForgotPassword.setEnabled(!model.busy);
            if (model.error != null) { Toast.makeText(this, model.error, Toast.LENGTH_LONG).show(); model.error = null; }
            if (model.destination != null) VerifiedAccountFlow.open(this, model.destination);
        });
        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            model.signIn(email, password);
        });

        binding.tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(login.this, Register.class);
            startActivity(intent);
        });

        binding.tvForgotPassword.setOnClickListener(v->{
            Intent intent = new Intent(login.this, ForgotPassword.class)
                    .putExtra(VerifiedAccountFlow.EXTRA_EMAIL, binding.etEmail.getText().toString().trim());
            startActivity(intent);
        });
    }
}
