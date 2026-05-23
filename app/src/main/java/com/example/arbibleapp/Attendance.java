package com.example.arbibleapp;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.journeyapps.barcodescanner.CompoundBarcodeView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Attendance extends AppCompatActivity {

    private ImageView btnBack;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CompoundBarcodeView barcodeScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            finish();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

        String passedUserType = getIntent().getStringExtra("userType");
        if ("Student".equals(passedUserType)) {
            setupStudentLayout(uid);
            return;
        }

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String userType = documentSnapshot.getString("userType");
                        if ("Teacher".equals(userType)) {
                            setupTeacherLayout();
                        } else {
                            setupStudentLayout(uid);
                        }
                    } else {
                        Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void setupStudentLayout(String uid) {
        setContentView(R.layout.activity_attendance_student);
        applyWindowInsets(findViewById(R.id.main));

        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        ImageView ivQRCode = findViewById(R.id.ivQRCode);

        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(uid, BarcodeFormat.QR_CODE, 400, 400);
            ivQRCode.setImageBitmap(bitmap);
        } catch (Exception e) {
            Toast.makeText(this, "Error generating QR code", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupTeacherLayout() {
        setContentView(R.layout.activity_attendance_teacher);
        applyWindowInsets(findViewById(R.id.main));

        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        barcodeScanner = findViewById(R.id.barcodeScanner);
        barcodeScanner.setStatusText("");

        Button btnScan = findViewById(R.id.btnScan);

        barcodeScanner.decodeContinuous(result -> {
            barcodeScanner.pause();
            String studentUid = result.getText();
            markAttendance(studentUid);
        });

        btnScan.setOnClickListener(v -> {
            barcodeScanner.resume();
        });
    }

    private void markAttendance(String studentUid) {
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        db.collection("attendance")
                .whereEqualTo("studentUid", studentUid)
                .whereEqualTo("date", date)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "Attendance already recorded for today", Toast.LENGTH_SHORT).show();
                        barcodeScanner.resume();
                    } else {
                        db.collection("users").document(studentUid).get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (documentSnapshot.exists()) {
                                        String username = documentSnapshot.getString("username");
                                        if (username == null) username = documentSnapshot.getString("fullName");
                                        
                                        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

                                        Map<String, Object> attendanceData = new HashMap<>();
                                        attendanceData.put("studentUid", studentUid);
                                        attendanceData.put("username", username);
                                        attendanceData.put("date", date);
                                        attendanceData.put("timestamp", timestamp);

                                        String finalUsername = username;
                                        db.collection("attendance").add(attendanceData)
                                                .addOnSuccessListener(documentReference -> {
                                                    Toast.makeText(this, "Attendance recorded for: " + finalUsername, Toast.LENGTH_LONG).show();
                                                    finish();
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(this, "Failed to record attendance", Toast.LENGTH_SHORT).show();
                                                    barcodeScanner.resume();
                                                });
                                    } else {
                                        Toast.makeText(this, "Invalid QR Code: Student not found", Toast.LENGTH_SHORT).show();
                                        barcodeScanner.resume();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Error fetching student data", Toast.LENGTH_SHORT).show();
                                    barcodeScanner.resume();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking existing records", Toast.LENGTH_SHORT).show();
                    barcodeScanner.resume();
                });
    }

    private void applyWindowInsets(View view) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (barcodeScanner != null) {
            barcodeScanner.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (barcodeScanner != null) {
            barcodeScanner.pause();
        }
    }
}