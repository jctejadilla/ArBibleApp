package com.example.arbibleapp;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class login extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

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
        db = FirebaseFirestore.getInstance();

        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegister = findViewById(R.id.tvRegister);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            String uid = mAuth.getCurrentUser().getUid();
                            String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

                            Map<String, Object> updates = new HashMap<>();
                            updates.put("password", password);
                            if (deviceId != null) {
                                updates.put("activeDeviceId", deviceId);
                            }

                            db.collection("users").document(uid)
                                    .set(updates, SetOptions.merge())
                                    .addOnCompleteListener(updateTask -> {
                                        if (isFinishing()) return;
                                        
                                        db.collection("users").document(uid).get()
                                                .addOnSuccessListener(documentSnapshot -> {
                                                    if (isFinishing()) return;
                                                    
                                                    if (documentSnapshot.exists()) {
                                                        String userType = documentSnapshot.getString("userType");
                                                        Intent intent;
                                                        if ("Teacher".equals(userType)) {
                                                            intent = new Intent(login.this, TeacherDashboard.class);
                                                        } else {
                                                            intent = new Intent(login.this, StudentDashboard.class);
                                                        }
                                                        startActivity(intent);
                                                        finish();
                                                    } else {
                                                        Toast.makeText(login.this, "User data not found", Toast.LENGTH_SHORT).show();
                                                    }
                                                })
                                                .addOnFailureListener(e -> {
                                                    if (isFinishing()) return;
                                                    Toast.makeText(login.this, "Error fetching user data", Toast.LENGTH_SHORT).show();
                                                });
                                    });
                        } else {
                            Toast.makeText(login.this, "Login failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(login.this, Register.class);
            startActivity(intent);
        });

        tvForgotPassword.setOnClickListener(v->{
            Intent intent = new Intent(login.this, ForgotPassword.class);
            startActivity(intent);
        });
    }
}
