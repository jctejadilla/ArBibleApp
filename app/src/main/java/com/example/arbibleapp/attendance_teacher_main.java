package com.example.arbibleapp;

import android.content.Intent;
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
        int primaryGreen = ContextCompat.getColor(this, R.color.primary_green);
        int white = ContextCompat.getColor(this, R.color.white);

        btnFilterAll.setBackgroundTintList(android.content.res.ColorStateList.valueOf(currentFilter.equals("All") ? primaryGreen : white));
        btnFilterAll.setTextColor(currentFilter.equals("All") ? white : primaryGreen);

        btnFilterPresent.setBackgroundTintList(android.content.res.ColorStateList.valueOf(currentFilter.equals("Present") ? primaryGreen : white));
        btnFilterPresent.setTextColor(currentFilter.equals("Present") ? white : primaryGreen);

        btnFilterAbsent.setBackgroundTintList(android.content.res.ColorStateList.valueOf(currentFilter.equals("Absent") ? primaryGreen : white));
        btnFilterAbsent.setTextColor(currentFilter.equals("Absent") ? white : primaryGreen);
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

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String studentUid = doc.getString("studentUid");
                        String studentName = doc.getString("studentName");
                        enrolledList.add(new EnrolledStudent(studentUid, studentName));
                    }

                    enrolledList.sort((s1, s2) ->
                            s1.name.compareToIgnoreCase(s2.name)
                    );

                    checkAttendanceForToday();
                });
    }

    private void checkAttendanceForToday() {
        db.collection("attendance")
                .whereEqualTo("date", todayDate)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int presentCount = 0;
                    for (EnrolledStudent student : enrolledList) {
                        student.isPresent = false;
                        student.time = "--:--";
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            if (doc.getString("studentUid").equals(student.uid)) {
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
                                break;
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
                holder.indicator.setBackgroundColor(ContextCompat.getColor(attendance_teacher_main.this, R.color.primary_green));
            } else {
                holder.status.setText("Status: Absent");
                holder.indicator.setBackgroundColor(ContextCompat.getColor(attendance_teacher_main.this, R.color.red));
            }
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView name, status, time;
            View indicator;
            ViewHolder(View v) {
                super(v);
                name = v.findViewById(R.id.tvStudentName);
                status = v.findViewById(R.id.tvAttendanceStatus);
                time = v.findViewById(R.id.tvTime);
                indicator = v.findViewById(R.id.viewStudentStatus);
            }
        }
    }
}