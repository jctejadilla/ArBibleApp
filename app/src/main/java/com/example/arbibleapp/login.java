package com.example.arbibleapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButtonToggleGroup;

public class login extends AppCompatActivity {

    private String userType = "Student"; // Default

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

        MaterialButtonToggleGroup toggleGroup = findViewById(R.id.toggleUserType);
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegister = findViewById(R.id.tvRegister);

        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnStudent) {
                    userType = "Student";
                } else if (checkedId == R.id.btnTeacher) {
                    userType = "Teacher";
                }
            }
        });

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            Toast.makeText(this, "Logging in as " + userType, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(login.this, StudentDashboard.class);
            startActivity(intent);
            finish();

            /*if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            } else {
                // Perform login logic based on userType
                Toast.makeText(this, "Logging in as " + userType, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(login.this, StudentDashboard.class);
                startActivity(intent);
                finish();
            }*/
        });

        tvRegister.setOnClickListener(v -> {
            // Intent to Register Activity (to be created)
            Toast.makeText(this, "Redirecting to Register...", Toast.LENGTH_SHORT).show();
        });
    }
}