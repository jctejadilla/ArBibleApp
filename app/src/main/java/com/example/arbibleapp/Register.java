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


public class Register extends AppCompatActivity {

    private RegistrationModel registration;
    private FirebaseAuth mAuth;
    private androidx.appcompat.app.AlertDialog recoveryDialog;

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

        MaterialButtonToggleGroup toggleUserType = findViewById(R.id.toggleUserType);
        EditText etUsername = findViewById(R.id.etRegisterUsername);
        EditText etEmail = findViewById(R.id.etRegisterEmail);
        EditText etPassword = findViewById(R.id.etRegisterPassword);
        EditText etConfirmPassword = findViewById(R.id.etConfirmPassword);
        RadioGroup rgGender = findViewById(R.id.rgGender);
        Button btnRegister = findViewById(R.id.btnRegister);
        TextView tvLogin = findViewById(R.id.tvLogin);

        registration = new androidx.lifecycle.ViewModelProvider(this).get(RegistrationModel.class);
        if (savedInstanceState != null && registration.existingEmail == null)
            registration.existingEmail = savedInstanceState.getString("existingEmail");
        etPassword.setSaveEnabled(false); etConfirmPassword.setSaveEnabled(false);
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (!registration.busy) { FirebaseAuth.getInstance().signOut(); finish(); }
            }
        });
        registration.changes.observe(this, ignored -> {
            btnRegister.setEnabled(!registration.busy);
            tvLogin.setEnabled(!registration.busy);
            btnRegister.setText(registration.hasPending() ? "Retry profile setup" : "Register");
            if (registration.error != null) {
                Toast.makeText(this, registration.error, Toast.LENGTH_LONG).show(); registration.error = null;
            }
            showExistingAccount();
            if (registration.complete) VerifiedAccountFlow.open(this, EmailVerificationActivity.class);
        });
        btnRegister.setText(registration.hasPending() ? "Retry profile setup" : "Register");
        btnRegister.setOnClickListener(v -> {
            if (registration.busy) return;
            if (registration.hasPending()) { registration.retry(); return; }
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

            registration.register(username, email, password, gender, userType);
        });

        tvLogin.setOnClickListener(v -> {
            mAuth.signOut();
            finish();
        });
    }

    private void showExistingAccount() {
        if (registration.existingEmail == null || (recoveryDialog != null && recoveryDialog.isShowing())) return;
        final String email = registration.existingEmail;
        recoveryDialog = new androidx.appcompat.app.AlertDialog.Builder(this)
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
