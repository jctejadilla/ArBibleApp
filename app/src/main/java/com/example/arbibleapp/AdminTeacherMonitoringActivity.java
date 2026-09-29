package com.example.arbibleapp;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminTeacherMonitoringActivity extends AppCompatActivity {

    private RecyclerView rvTeachers;
    private TextView tvEmptyState;
    private FirebaseFirestore db;
    private TeacherAdapter adapter;
    private final List<TeacherModel> teacherList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_teacher_monitoring);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        tvEmptyState = findViewById(R.id.tvEmptyState);
        rvTeachers = findViewById(R.id.rvTeachers);
        rvTeachers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TeacherAdapter(teacherList);
        rvTeachers.setAdapter(adapter);

        loadTeachers();
    }

    private void loadTeachers() {
        db.collection("users")
                .whereEqualTo("userType", "Teacher")
                .get()
                .addOnSuccessListener(snapshots -> {
                    teacherList.clear();
                    for (DocumentSnapshot doc : snapshots) {
                        String uid = doc.getId();
                        String name = doc.getString("username");
                        if (name == null || name.isEmpty()) {
                            name = doc.getString("fullName");
                        }
                        String email = doc.getString("email");

                        teacherList.add(new TeacherModel(uid, name != null ? name : "Teacher", email != null ? email : "No email"));
                    }

                    if (teacherList.isEmpty()) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                        rvTeachers.setVisibility(View.GONE);
                    } else {
                        tvEmptyState.setVisibility(View.GONE);
                        rvTeachers.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private static class TeacherModel {
        String uid, name, email;
        boolean isExpanded = false;
        List<StudentModel> students = new ArrayList<>();

        TeacherModel(String uid, String name, String email) {
            this.uid = uid;
            this.name = name;
            this.email = email;
        }
    }

    private static class StudentModel {
        String name, email;
        long level, totalXP;

        StudentModel(String name, String email, long level, long totalXP) {
            this.name = name;
            this.email = email;
            this.level = level;
            this.totalXP = totalXP;
        }
    }

    private class TeacherAdapter extends RecyclerView.Adapter<TeacherAdapter.ViewHolder> {
        private final List<TeacherModel> list;

        TeacherAdapter(List<TeacherModel> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_teacher, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TeacherModel teacher = list.get(position);
            holder.tvTeacherName.setText(teacher.name);
            holder.tvTeacherEmail.setText(teacher.email);

            loadStudentsForTeacher(teacher, holder);

            holder.llStudentHeader.setOnClickListener(v -> {
                teacher.isExpanded = !teacher.isExpanded;
                holder.tvToggleExpand.setText(teacher.isExpanded ? "Hide Students" : "Tap to View Students");
                holder.llStudentsContainer.setVisibility(teacher.isExpanded ? View.VISIBLE : View.GONE);
            });
        }

        private void loadStudentsForTeacher(TeacherModel teacher, ViewHolder holder) {
            db.collection("enrollments")
                    .whereEqualTo("teacherUid", teacher.uid)
                    .get()
                    .addOnSuccessListener(enrollSnapshots -> {
                        teacher.students.clear();
                        if (enrollSnapshots == null || enrollSnapshots.isEmpty()) {
                            holder.tvStudentCountBadge.setText("0 Students");
                            holder.llStudentsContainer.removeAllViews();
                            TextView tvEmpty = new TextView(AdminTeacherMonitoringActivity.this);
                            tvEmpty.setText("No QR registered students yet");
                            tvEmpty.setTextColor(Color.GRAY);
                            tvEmpty.setPadding(16, 16, 16, 16);
                            holder.llStudentsContainer.addView(tvEmpty);
                            return;
                        }

                        int totalEnrolled = enrollSnapshots.size();
                        final int[] loadedCount = {0};

                        for (DocumentSnapshot enrollDoc : enrollSnapshots) {
                            String studentUid = enrollDoc.getString("studentUid");
                            if (studentUid != null) {
                                db.collection("users").document(studentUid).get().addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {
                                        String name = userDoc.getString("username");
                                        if (name == null || name.isEmpty()) {
                                            name = userDoc.getString("fullName");
                                        }
                                        String email = userDoc.getString("email");
                                        Long levelLong = userDoc.contains("level") ? userDoc.getLong("level") : 1;
                                        Long xpLong = userDoc.contains("totalXP") ? userDoc.getLong("totalXP") : 0;

                                        teacher.students.add(new StudentModel(
                                                name != null ? name : "Student",
                                                email != null ? email : "",
                                                levelLong != null ? levelLong : 1,
                                                xpLong != null ? xpLong : 0
                                        ));
                                    }
                                    loadedCount[0]++;
                                    if (loadedCount[0] >= totalEnrolled) {
                                        renderStudentList(teacher, holder);
                                    }
                                }).addOnFailureListener(e -> {
                                    loadedCount[0]++;
                                    if (loadedCount[0] >= totalEnrolled) {
                                        renderStudentList(teacher, holder);
                                    }
                                });
                            } else {
                                loadedCount[0]++;
                                if (loadedCount[0] >= totalEnrolled) {
                                    renderStudentList(teacher, holder);
                                }
                            }
                        }
                    });
        }

        private void renderStudentList(TeacherModel teacher, ViewHolder holder) {
            holder.tvStudentCountBadge.setText(teacher.students.size() + " Students");

            holder.llStudentsContainer.removeAllViews();
            if (teacher.students.isEmpty()) {
                TextView tvEmpty = new TextView(AdminTeacherMonitoringActivity.this);
                tvEmpty.setText("No QR registered students found");
                tvEmpty.setTextColor(Color.GRAY);
                tvEmpty.setPadding(16, 16, 16, 16);
                holder.llStudentsContainer.addView(tvEmpty);
            } else {
                for (StudentModel s : teacher.students) {
                    View studentView = LayoutInflater.from(AdminTeacherMonitoringActivity.this)
                            .inflate(android.R.layout.simple_list_item_2, holder.llStudentsContainer, false);
                    TextView text1 = studentView.findViewById(android.R.id.text1);
                    TextView text2 = studentView.findViewById(android.R.id.text2);

                    text1.setText("• " + s.name + " (" + s.email + ")");
                    text1.setTextColor(Color.DKGRAY);
                    text1.setTextSize(14f);

                    text2.setText("Level " + s.level + " • " + s.totalXP + " XP");
                    text2.setTextColor(Color.GRAY);
                    text2.setTextSize(12f);

                    holder.llStudentsContainer.addView(studentView);
                }
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTeacherName, tvTeacherEmail, tvStudentCountBadge, tvToggleExpand;
            LinearLayout llStudentHeader, llStudentsContainer;

            ViewHolder(View v) {
                super(v);
                tvTeacherName = v.findViewById(R.id.tvTeacherName);
                tvTeacherEmail = v.findViewById(R.id.tvTeacherEmail);
                tvStudentCountBadge = v.findViewById(R.id.tvStudentCountBadge);
                tvToggleExpand = v.findViewById(R.id.tvToggleExpand);
                llStudentHeader = v.findViewById(R.id.llStudentHeader);
                llStudentsContainer = v.findViewById(R.id.llStudentsContainer);
            }
        }
    }
}