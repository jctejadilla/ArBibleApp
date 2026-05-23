package com.example.arbibleapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class TeacherProfile extends AppCompatActivity {

    ImageView btnBack;
    LinearLayout navHome, navStudentsTab, navAttendanceTab, navAnalyticsTab;
    Button btnLogout, btnSendEmail;
    EditText etSubject, etMessage;
    TextView tvTeacherName, tvStudentsCount, tvLessonsCount;
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
        setupEmailLogic();
        fetchTeacherData();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnLogout = findViewById(R.id.btnLogout);
        btnSendEmail = findViewById(R.id.btnSendEmail);

        etSubject = findViewById(R.id.etEmailSubject);
        etMessage = findViewById(R.id.etEmailMessage);

        tvTeacherName = findViewById(R.id.tvTeacherLabel);
        tvStudentsCount = findViewById(R.id.tvStudentsCount);
        tvLessonsCount = findViewById(R.id.tvLessonsCount);

        navHome = findViewById(R.id.navHome);
        navStudentsTab = findViewById(R.id.navStudentsTab);
        navAttendanceTab = findViewById(R.id.navAttendanceTab);
        navAnalyticsTab = findViewById(R.id.navAnalyticsTab);
    }

    private void setupNavigation() {
        btnBack.setOnClickListener(v -> finish());

        navHome.setOnClickListener(v -> {
            startActivity(new Intent(this, TeacherDashboard.class));
            finish();
        });
        navStudentsTab.setOnClickListener(v -> {
            startActivity(new Intent(this, ClassManagement.class));
            finish();
        });
        navAttendanceTab.setOnClickListener(v -> {
            startActivity(new Intent(this, attendance_teacher_main.class));
            finish();
        });

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(TeacherProfile.this, login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void setupEmailLogic() {
        btnSendEmail.setOnClickListener(v -> {
            String subject = etSubject.getText().toString().trim();
            String message = etMessage.getText().toString().trim();
            String admin = "persyjak03@gmail.com";

            if (subject.isEmpty() || message.isEmpty()) {
                Toast.makeText(this, "Please fill in subject and message", Toast.LENGTH_SHORT).show();
                return;
            }

            String fullMessage = message + "\n\n---\nSent by Teacher: " + tvTeacherName.getText().toString();

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("message/rfc822");
            intent.setPackage("com.google.android.gm");
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{admin});
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
            intent.putExtra(Intent.EXTRA_TEXT, fullMessage);

            try {
                startActivity(intent);
            } catch (android.content.ActivityNotFoundException ex) {
                Intent chooser = Intent.createChooser(intent, "Send Email");
                startActivity(chooser);
            }
        });
    }

    private void fetchTeacherData() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String name = doc.getString("username");
                if (name == null) name = doc.getString("fullName");
                tvTeacherName.setText(name != null ? name : "Teacher");
            }
        });

        db.collection("enrollments").whereEqualTo("teacherUid", uid).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    tvStudentsCount.setText(String.valueOf(queryDocumentSnapshots.size()));
                });

        tvLessonsCount.setText("5");
    }
}
