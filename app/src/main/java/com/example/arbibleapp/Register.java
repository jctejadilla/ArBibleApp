package com.example.arbibleapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.example.arbibleapp.databinding.ActivityRegisterBinding;

public class Register extends AppCompatActivity {

    private RegistrationModel registration;
    private FirebaseAuth mAuth;
    private AlertDialog recoveryDialog;
    private ActivityRegisterBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        
        binding.etRegisterEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String currentEmail = binding.etRegisterEmail.getText().toString().trim();
                if (!currentEmail.isEmpty() && !currentEmail.endsWith("@gmail.com") && !currentEmail.endsWith("gmail.com")) {
                    if (currentEmail.endsWith("@")) {
                        binding.etRegisterEmail.setText(currentEmail + "gmail.com");
                    } else if (!currentEmail.contains("@")) {
                        binding.etRegisterEmail.setText(currentEmail + "@gmail.com");
                    }
                } else if (!currentEmail.isEmpty() && currentEmail.endsWith("gmail.com") && !currentEmail.endsWith("@gmail.com")) {
                     binding.etRegisterEmail.setText(currentEmail.substring(0, currentEmail.length() - 9) + "@gmail.com");
                }
            }
        });
        
        binding.etRegisterPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String pass = s.toString();
                binding.tvReqLength.setCompoundDrawablesWithIntrinsicBounds(pass.length() >= 6 ? R.drawable.ic_check_circle : R.drawable.ic_cancel_circle, 0, 0, 0);
                binding.tvReqUpper.setCompoundDrawablesWithIntrinsicBounds(pass.matches(".*[A-Z].*") ? R.drawable.ic_check_circle : R.drawable.ic_cancel_circle, 0, 0, 0);
                binding.tvReqLower.setCompoundDrawablesWithIntrinsicBounds(pass.matches(".*[a-z].*") ? R.drawable.ic_check_circle : R.drawable.ic_cancel_circle, 0, 0, 0);
                binding.tvReqNumber.setCompoundDrawablesWithIntrinsicBounds(pass.matches(".*[0-9].*") ? R.drawable.ic_check_circle : R.drawable.ic_cancel_circle, 0, 0, 0);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        registration = new ViewModelProvider(this).get(RegistrationModel.class);
        if (savedInstanceState != null && registration.existingEmail == null)
            registration.existingEmail = savedInstanceState.getString("existingEmail");
        binding.etRegisterPassword.setSaveEnabled(false); binding.etConfirmPassword.setSaveEnabled(false);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (!registration.busy) { FirebaseAuth.getInstance().signOut(); finish(); }
            }
        });
        registration.changes.observe(this, ignored -> {
            binding.btnRegister.setEnabled(!registration.busy);
            binding.tvLogin.setEnabled(!registration.busy);
            binding.btnRegister.setText(registration.hasPending() ? "Retry profile setup" : "Register");
            if (registration.error != null) {
                Toast.makeText(this, registration.error, Toast.LENGTH_LONG).show(); registration.error = null;
            }
            showExistingAccount();
            if (registration.complete) VerifiedAccountFlow.open(this, EmailVerificationActivity.class);
        });
        binding.btnRegister.setText(registration.hasPending() ? "Retry profile setup" : "Register");
        binding.btnRegister.setOnClickListener(v -> {
            if (registration.busy) return;
            if (registration.hasPending()) { registration.retry(); return; }
            String username = binding.etRegisterUsername.getText().toString().trim();
            String email = binding.etRegisterEmail.getText().toString().trim();
            String password = binding.etRegisterPassword.getText().toString().trim();
            String confirmPassword = binding.etConfirmPassword.getText().toString().trim();
            
            String userType = "Student";
            if (binding.toggleUserType.getCheckedButtonId() == R.id.btnTeacherRegister) {
                userType = "Teacher";
            }

            int selectedGenderId = binding.rgGender.getCheckedRadioButtonId();
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

            if (password.length() < 6 || !password.matches(".*[A-Z].*") || !password.matches(".*[a-z].*") || !password.matches(".*[0-9].*")) {
                Toast.makeText(this, "Password must be at least 6 characters, and contain at least 1 uppercase letter, 1 lowercase letter, and 1 number", Toast.LENGTH_LONG).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            registration.register(username, email, password, gender, userType);
        });

        binding.tvLogin.setOnClickListener(v -> {
            mAuth.signOut();
            finish();
        });
    }

    private void showExistingAccount() {
        if (registration.existingEmail == null || (recoveryDialog != null && recoveryDialog.isShowing())) return;
        final String email = registration.existingEmail;
        recoveryDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.account_exists_title)
                .setMessage(R.string.account_exists_message)
                .setPositiveButton(R.string.account_continue_login, (dialog, which) -> {
                    registration.existingEmail = null;
                    startActivity(new Intent(this, login.class).putExtra(VerifiedAccountFlow.EXTRA_EMAIL, email)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    finish();
                })
                .setNeutralButton(R.string.account_forgot_password, (dialog, which) -> {
                    registration.existingEmail = null;
                    startActivity(new Intent(this, ForgotPassword.class).putExtra(VerifiedAccountFlow.EXTRA_EMAIL, email));
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> registration.existingEmail = null)
                .setOnCancelListener(dialog -> registration.existingEmail = null).show();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("existingEmail", registration.existingEmail);
        super.onSaveInstanceState(state);
    }
    @Override protected void onDestroy() {
        if (recoveryDialog != null) recoveryDialog.dismiss();
        super.onDestroy();
    }
}
