package com.example.arbibleapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ClassManagement extends AppCompatActivity {

    private EditText etSearch;
    private ImageView btnBack;
    private RecyclerView rvStudents;
    private StudentDirectoryAdapter adapter;
    private List<StudentStatusModel> studentList = new ArrayList<>();
    private List<StudentStatusModel> displayedList = new ArrayList<>();
    
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Spinner spinnerFilter;
    private Button btnEnrollQR;

    private final androidx.activity.result.ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if(result.getContents() != null) {
                    enrollStudentByUid(result.getContents());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_class_management);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        etSearch = findViewById(R.id.etSearchAllStudents);
        rvStudents = findViewById(R.id.rvStudentResults);
        btnEnrollQR = findViewById(R.id.btnEnrollQR);
        btnBack = findViewById(R.id.btnBack);
        spinnerFilter = findViewById(R.id.spinnerFilter);

        btnEnrollQR.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setPrompt("Scan Student QR Code to Enroll");
            options.setBeepEnabled(true);
            options.setOrientationLocked(true);
            options.setCameraId(0);
            barcodeLauncher.launch(options);
        });

        btnBack.setOnClickListener(v -> finish());

        setupFilterSpinner();

        rvStudents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StudentDirectoryAdapter(displayedList);
        rvStudents.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadEnrolledStudents();

        setupBottomNav();
    }

    private void setupBottomNav() {
        findViewById(R.id.navHome).setOnClickListener(v -> {
            startActivity(new Intent(this, TeacherDashboard.class));
            finish();
        });
        findViewById(R.id.navAttendanceTab).setOnClickListener(v -> {
            startActivity(new Intent(this, attendance_teacher_main.class));
            finish();
        });
        findViewById(R.id.navAnalyticsTab).setOnClickListener(v -> {
            startActivity(new Intent(this, AnalyticsActivity.class));
            finish();
        });
    }

    private void setupFilterSpinner() {
        String[] items = {"All Students", "Excellent", "Good", "Average", "Needs Attention"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, items);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerFilter.setAdapter(spinnerAdapter);
        
        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilters();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadEnrolledStudents() {
        if (mAuth.getCurrentUser() == null) return;
        String teacherUid = mAuth.getCurrentUser().getUid();

        db.collection("enrollments")
                .whereEqualTo("teacherUid", teacherUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    studentList.clear();
                    if (queryDocumentSnapshots.isEmpty()) {
                        applyFilters();
                        return;
                    }
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String studentUid = doc.getString("studentUid");
                        fetchStudentDetails(studentUid);
                    }
                });
    }

    private void fetchStudentDetails(String studentUid) {
        db.collection("users")
                .document(studentUid)
                .get()
                .addOnSuccessListener(doc -> {

                if (doc.exists()) {
                    String studentName = doc.getString("username");
                    if (studentName == null) studentName = doc.getString("fullName");
                    if (studentName == null) studentName = "Unknown Student";

                    String email = doc.getString("email");
                    if (email == null) email = "No email provided";

                    Long xpLong = doc.getLong("totalXP");
                    long xp = xpLong != null ? xpLong : 0;

                    StudentStatusModel student = new StudentStatusModel(studentUid, studentName, email, xp);
                    studentList.add(student);

                    studentList.sort((s1, s2) ->
                            s1.name.compareToIgnoreCase(s2.name)
                    );

                    fetchStudentStats(student);
                }
        });
    }

    private void fetchStudentStats(StudentStatusModel student) {
        // Fetch Quiz Average
        db.collection("quiz_results").whereEqualTo("studentUid", student.uid).get()
                .addOnSuccessListener(results -> {
                    if (!results.isEmpty()) {
                        double totalPercentage = 0;
                        int count = results.size();
                        for (DocumentSnapshot d : results) {
                            Long score = d.getLong("score");
                            Long totalQuestions = d.getLong("totalQuestions");
                            
                            if (score != null && totalQuestions != null && totalQuestions > 0) {
                                totalPercentage += (score.doubleValue() / totalQuestions.doubleValue()) * 100.0;
                            } else if (score != null) {
                                totalPercentage += (score.doubleValue() / 5.0) * 100.0;
                            }
                        }
                        int avg = (int) (totalPercentage / count);
                        student.quizAvg = Math.min(100, avg);
                        student.storiesCompleted = count;
                    }
                    
                    // Fetch Attendance Rate and Latest Attendance
                    db.collection("attendance").whereEqualTo("studentUid", student.uid).get()
                            .addOnSuccessListener(attendance -> {
                                student.attendedCount = attendance.size();
                                
                                if (!attendance.isEmpty()) {
                                    List<DocumentSnapshot> docs = new ArrayList<>(attendance.getDocuments());
                                    docs.sort((d1, d2) -> {
                                        String date1 = d1.getString("date");
                                        String date2 = d2.getString("date");
                                        if (date1 == null) date1 = "";
                                        if (date2 == null) date2 = "";
                                        return date2.compareTo(date1);
                                    });
                                    
                                    String rawDate = docs.get(0).getString("date");
                                    if (rawDate != null) {
                                        try {
                                            SimpleDateFormat fromSdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                            SimpleDateFormat toSdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
                                            Date dateObj = fromSdf.parse(rawDate);
                                            if (dateObj != null) {
                                                student.latestAttendanceDate = toSdf.format(dateObj);
                                            }
                                        } catch (Exception e) {
                                            student.latestAttendanceDate = rawDate;
                                        }
                                    }
                                }
                                
                                // Calculate Sundays in current month
                                Calendar cal = Calendar.getInstance();
                                int month = cal.get(Calendar.MONTH);
                                int year = cal.get(Calendar.YEAR);
                                cal.set(year, month, 1);
                                int totalSundays = 0;
                                while (cal.get(Calendar.MONTH) == month) {
                                    if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                                        totalSundays++;
                                    }
                                    cal.add(Calendar.DAY_OF_MONTH, 1);
                                }
                                student.totalSundays = totalSundays;
                                applyFilters();
                            });
                });
    }

    private void applyFilters() {
        String query = etSearch.getText().toString().toLowerCase();
        String filter = spinnerFilter.getSelectedItem() != null ? spinnerFilter.getSelectedItem().toString() : "All Students";
        
        displayedList.clear();
        for (StudentStatusModel s : studentList) {
            boolean matchesSearch = s.name.toLowerCase().contains(query);
            boolean matchesFilter = filter.equals("All Students") || s.getPerformance().equals(filter);
            
            if (matchesSearch && matchesFilter) {
                displayedList.add(s);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void enrollStudentByUid(String studentUid) {
        if (mAuth.getCurrentUser() == null) return;
        String teacherUid = mAuth.getCurrentUser().getUid();

        db.collection("enrollments")
                .whereEqualTo("teacherUid", teacherUid)
                .whereEqualTo("studentUid", studentUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "Student already enrolled", Toast.LENGTH_SHORT).show();
                    } else {
                        db.collection("users").document(studentUid).get().addOnSuccessListener(userDoc -> {
                            if (userDoc.exists()) {
                                String studentName = userDoc.getString("username");
                                if (studentName == null) studentName = userDoc.getString("fullName");

                                Map<String, Object> enrollment = new HashMap<>();
                                enrollment.put("teacherUid", teacherUid);
                                enrollment.put("studentUid", studentUid);
                                enrollment.put("studentName", studentName);
                                enrollment.put("enrolledAt", com.google.firebase.Timestamp.now());

                                String finalName = studentName;
                                db.collection("enrollments").add(enrollment).addOnSuccessListener(ref -> {
                                    Toast.makeText(this, "Enrolled " + finalName, Toast.LENGTH_SHORT).show();
                                    loadEnrolledStudents();
                                });
                            } else {
                                Toast.makeText(this, "Invalid Student QR", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
    }

    private static class StudentStatusModel {
        String uid, name, email, latestAttendanceDate;
        long xp;
        int quizAvg = 0;
        int attendedCount = 0;
        int totalSundays = 4;
        int storiesCompleted = 0;

        StudentStatusModel(String uid, String name, String email, long xp) {
            this.uid = uid;
            this.name = name;
            this.email = email;
            this.xp = xp;
            this.latestAttendanceDate = "No attendance yet";
        }

        String getPerformance() {
            if (quizAvg >= 90) return "Excellent";
            if (quizAvg >= 75) return "Good";
            if (quizAvg >= 50) return "Average";
            return "Needs Attention";
        }
    }

    private class StudentDirectoryAdapter extends RecyclerView.Adapter<StudentDirectoryAdapter.ViewHolder> {
        List<StudentStatusModel> students;
        StudentDirectoryAdapter(List<StudentStatusModel> students) { this.students = students; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_directory, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            StudentStatusModel s = students.get(position);
            holder.name.setText(s.name);
            holder.xp.setText(String.valueOf(s.xp));
            holder.quizAvg.setText(s.quizAvg + "%");
            holder.attendance.setText(s.attendedCount + "/" + s.totalSundays);
            holder.stories.setText(s.storiesCompleted + " stories completed");
            
            String performance = s.getPerformance();
            holder.badge.setText(performance);
            
            int badgeColor, textColor;
            if (performance.equals("Excellent")) {
                badgeColor = Color.parseColor("#E8F5E9");
                textColor = Color.parseColor("#2E7D32");
            } else if (performance.equals("Good")) {
                badgeColor = Color.parseColor("#E3F2FD");
                textColor = Color.parseColor("#1565C0");
            } else if (performance.equals("Average")) {
                badgeColor = Color.parseColor("#FFF3E0");
                textColor = Color.parseColor("#EF6C00");
            } else {
                badgeColor = Color.parseColor("#FFEBEE");
                textColor = Color.parseColor("#C62828");
            }
            
            holder.badge.getBackground().setTint(badgeColor);
            holder.badge.setTextColor(textColor);
            
            holder.lastActive.setText("Last Attended: " + s.latestAttendanceDate);
            
            if (s.name.length() >= 2) {
                holder.initials.setText(s.name.substring(0, 2).toUpperCase());
            } else {
                holder.initials.setText(s.name.toUpperCase());
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), StudentDetailsActivity.class);
                intent.putExtra("studentUid", s.uid);
                intent.putExtra("studentName", s.name);
                intent.putExtra("teacherUid", mAuth.getCurrentUser().getUid());
                v.getContext().startActivity(intent);
            });
        }

        @Override
        public int getItemCount() { return students.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView name, xp, quizAvg, attendance, stories, badge, initials, lastActive;
            ViewHolder(View v) {
                super(v);
                name = v.findViewById(R.id.tvStudentName);
                xp = v.findViewById(R.id.tvXPValue);
                quizAvg = v.findViewById(R.id.tvQuizAvg);
                attendance = v.findViewById(R.id.tvAttendanceRate);
                stories = v.findViewById(R.id.tvStoriesCount);
                badge = v.findViewById(R.id.tvPerformanceBadge);
                initials = v.findViewById(R.id.tvAvatarInitials);
                lastActive = v.findViewById(R.id.tvLastActive);
            }
        }
    }
}