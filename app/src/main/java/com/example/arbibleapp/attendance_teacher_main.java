package com.example.arbibleapp;

import android.app.AlertDialog;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class attendance_teacher_main extends AppCompatActivity {

    private ImageView btnBack;
    private Button btnScanQR, btnFilterAll, btnFilterPresent, btnFilterAbsent;
    private TextView tvPresent, tvAbsent, tvRate;
    private EditText etSearchStudents;
    private RecyclerView rvStudents;
    private EnrolledStudentAdapter adapter;
    private List<EnrolledStudent> enrolledList = new ArrayList<>();
    private String currentFilter = "All";
    
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String todayDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_attendance_teacher_main);
        
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupListeners();
        fetchEnrolledStudents();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnScanQR = findViewById(R.id.btnScanQR);
        btnFilterAll = findViewById(R.id.btnFilterAll);
        btnFilterPresent = findViewById(R.id.btnFilterPresent);
        btnFilterAbsent = findViewById(R.id.btnFilterAbsent);
        tvPresent = findViewById(R.id.tvPresent);
        tvAbsent = findViewById(R.id.tvAbsent);
        tvRate = findViewById(R.id.tvRate);
        etSearchStudents = findViewById(R.id.etSearchStudents);
        rvStudents = findViewById(R.id.rvStudents);
        TextView tvDateToday = findViewById(R.id.tvDateToday);

        String formattedDate = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(new Date());
        tvDateToday.setText(formattedDate);

        rvStudents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EnrolledStudentAdapter(new ArrayList<>());
        rvStudents.setAdapter(adapter);

        updateFilterButtonsUI();
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnScanQR.setOnClickListener(v -> startActivity(new Intent(this, Attendance.class)));
        findViewById(R.id.navHome).setOnClickListener(v -> startActivity(new Intent(this, TeacherDashboard.class)));
        findViewById(R.id.navStudentsTab).setOnClickListener(v -> startActivity(new Intent(this, ClassManagement.class)));
        findViewById(R.id.navAnalyticsTab).setOnClickListener(v -> startActivity(new Intent(this, AnalyticsActivity.class)));

        btnFilterAll.setOnClickListener(v -> {
            currentFilter = "All";
            updateFilterButtonsUI();
            filterList();
        });

        btnFilterPresent.setOnClickListener(v -> {
            currentFilter = "Present";
            updateFilterButtonsUI();
            filterList();
        });

        btnFilterAbsent.setOnClickListener(v -> {
            currentFilter = "Absent";
            updateFilterButtonsUI();
            filterList();
        });

        etSearchStudents.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updateFilterButtonsUI() {
        int forestGreen = Color.parseColor("#1B4235");
        int presentGreen = Color.parseColor("#2E7D32");
        int absentRed = ContextCompat.getColor(this, R.color.red);
        int white = ContextCompat.getColor(this, R.color.white);

        boolean allSelected = currentFilter.equals("All");
        boolean presentSelected = currentFilter.equals("Present");
        boolean absentSelected = currentFilter.equals("Absent");

        btnFilterAll.setBackgroundTintList(ColorStateList.valueOf(allSelected ? forestGreen : white));
        btnFilterAll.setTextColor(allSelected ? white : forestGreen);
        btnFilterPresent.setBackgroundTintList(ColorStateList.valueOf(presentSelected ? presentGreen : white));
        btnFilterPresent.setTextColor(presentSelected ? white : presentGreen);
        btnFilterAbsent.setBackgroundTintList(ColorStateList.valueOf(absentSelected ? absentRed : white));
        btnFilterAbsent.setTextColor(absentSelected ? white : absentRed);
    }
    private void filterList() {
        String searchQuery = etSearchStudents.getText().toString().toLowerCase();
        List<EnrolledStudent> filteredList = new ArrayList<>();
        int presentCount = 0;
        int absentCount = 0;

        for (EnrolledStudent student : enrolledList) {
            boolean matchesSearch = student.name.toLowerCase().contains(searchQuery);
            boolean matchesFilter = currentFilter.equals("All") || 
                                   (currentFilter.equals("Present") && student.isPresent) ||
                                   (currentFilter.equals("Absent") && !student.isPresent);

            if (student.isPresent) presentCount++; else absentCount++;

            if (matchesSearch && matchesFilter) {
                filteredList.add(student);
            }
        }

        btnFilterAll.setText("All (" + enrolledList.size() + ")");
        btnFilterPresent.setText("Present (" + presentCount + ")");
        btnFilterAbsent.setText("Absent (" + absentCount + ")");

        adapter.updateList(filteredList);
    }

    private void fetchEnrolledStudents() {
        if (mAuth.getCurrentUser() == null) return;
        String teacherUid = mAuth.getCurrentUser().getUid();

        db.collection("enrollments")
                .whereEqualTo("teacherUid", teacherUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    enrolledList.clear();
                    
                    HashSet<String> addedUids = new HashSet<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String studentUid = doc.getString("studentUid");
                        String studentName = doc.getString("studentName");
                        
                        if (studentUid != null && !addedUids.contains(studentUid)) {
                            addedUids.add(studentUid);
                            enrolledList.add(new EnrolledStudent(studentUid, studentName));
                        } else if (studentUid != null && addedUids.contains(studentUid)) {
                            // If it's a duplicate enrollment document in the database, delete it to keep it clean
                            doc.getReference().delete();
                        }
                    }

                    enrolledList.sort((s1, s2) -> {
                        String name1 = s1.name != null ? s1.name : "";
                        String name2 = s2.name != null ? s2.name : "";
                        return name1.compareToIgnoreCase(name2);
                    });

                    checkAttendanceForToday();
                });
    }

    private void checkAttendanceForToday() {
        if (mAuth.getCurrentUser() == null) return;
        String teacherUid = mAuth.getCurrentUser().getUid();

        db.collection("attendance")
                .whereEqualTo("date", todayDate)
                .whereEqualTo("teacherUid", teacherUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int presentCount = 0;
                    HashSet<String> processedAttendanceUids = new HashSet<>();
                    for (EnrolledStudent student : enrolledList) {
                        student.isPresent = false;
                        student.time = "--:--";
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            String studentUid = doc.getString("studentUid");
                            if (studentUid != null && studentUid.equals(student.uid)) {
                                if (processedAttendanceUids.contains(studentUid)) {
                                    // Duplicate attendance record found, delete it
                                    doc.getReference().delete();
                                    continue;
                                }
                                processedAttendanceUids.add(studentUid);
                                
                                student.isPresent = true;
                                String rawTime = doc.getString("timestamp");
                                if (rawTime != null) {
                                    try {
                                        SimpleDateFormat from24 = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                                        SimpleDateFormat to12 = new SimpleDateFormat("h:mm a", Locale.getDefault());
                                        Date timeDate = from24.parse(rawTime);
                                        if (timeDate != null) {
                                            student.time = to12.format(timeDate);
                                        }
                                    } catch (Exception e) {
                                        student.time = rawTime;
                                    }
                                }
                                presentCount++;
                            }
                        }
                    }
                    updateStats(presentCount);
                    filterList();
                });
    }

    private void updateStats(int present) {
        int total = enrolledList.size();
        int absent = total - present;
        tvPresent.setText(String.valueOf(present));
        tvAbsent.setText(String.valueOf(absent));
        if (total > 0) {
            int rate = (present * 100) / total;
            tvRate.setText(rate + "%");
        } else {
            tvRate.setText("0%");
        }
    }

    private void promptDeleteAttendance(EnrolledStudent student) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Attendance")
                .setMessage("Are you sure you want to delete today's attendance record for " + student.name + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("attendance")
                            .whereEqualTo("date", todayDate)
                            .whereEqualTo("studentUid", student.uid)
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                    doc.getReference().delete();
                                }
                                Toast.makeText(this, "Attendance deleted for " + student.name, Toast.LENGTH_SHORT).show();
                                checkAttendanceForToday(); // Refresh the list
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete attendance", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchEnrolledStudents();
    }

    private static class EnrolledStudent {
        String uid, name, time;
        boolean isPresent;
        EnrolledStudent(String uid, String name) {
            this.uid = uid;
            this.name = name;
        }
    }

    private class EnrolledStudentAdapter extends RecyclerView.Adapter<EnrolledStudentAdapter.ViewHolder> {
        List<EnrolledStudent> list;
        EnrolledStudentAdapter(List<EnrolledStudent> list) { this.list = list; }

        public void updateList(List<EnrolledStudent> newList) {
            this.list = newList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_attendance, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            EnrolledStudent student = list.get(position);
            holder.name.setText(student.name);
            holder.time.setText(student.time);
            if (student.isPresent) {
                holder.status.setText("Status: Present");
                holder.status.setTextColor(Color.parseColor("#1B4235"));
                holder.indicator.setBackgroundColor(ContextCompat.getColor(attendance_teacher_main.this, R.color.primary_green));
                holder.btnDelete.setVisibility(View.VISIBLE);
                holder.btnDelete.setOnClickListener(v -> promptDeleteAttendance(student));
            } else {
                holder.status.setText("Status: Absent");
                holder.status.setTextColor(ContextCompat.getColor(attendance_teacher_main.this, R.color.red));
                holder.indicator.setBackgroundColor(ContextCompat.getColor(attendance_teacher_main.this, R.color.red));
                holder.btnDelete.setVisibility(View.GONE);
                holder.btnDelete.setOnClickListener(null);
            }
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView name, status, time;
            View indicator;
            ImageView btnDelete;
            ViewHolder(View v) {
                super(v);
                name = v.findViewById(R.id.tvStudentName);
                status = v.findViewById(R.id.tvAttendanceStatus);
                time = v.findViewById(R.id.tvTime);
                indicator = v.findViewById(R.id.viewStudentStatus);
                btnDelete = v.findViewById(R.id.ivDeleteAttendance);
            }
        }
    }
}