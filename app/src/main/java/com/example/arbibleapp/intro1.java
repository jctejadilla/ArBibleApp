package com.example.arbibleapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class intro1 extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            checkUserTypeAndRedirect(currentUser.getUid());
            return; // Exit onCreate to prevent showing intro
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_intro1);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button btnNext = findViewById(R.id.btnNext);
        btnNext.setOnClickListener(v -> {
            Intent intent = new Intent(intro1.this, intro2.class);
            startActivity(intent);
        });

        TextView tvSkip = findViewById(R.id.tvSkip);
        tvSkip.setOnClickListener(v -> {
            Intent intent = new Intent(intro1.this, login.class);
            startActivity(intent);
            finish();
        });
    }

    private void checkUserTypeAndRedirect(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String userType = documentSnapshot.getString("userType");
                        Intent intent;
                        if ("Teacher".equals(userType)) {
                            intent = new Intent(intro1.this, TeacherDashboard.class);
                        } else {
                            intent = new Intent(intro1.this, StudentDashboard.class);
                        }
                        startActivity(intent);
                        finish();
                    } else {
                        mAuth.signOut();
                        startActivity(new Intent(intro1.this, login.class));
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(intro1.this, "Error checking user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(intro1.this, login.class));
                    finish();
                });
    }
}
