package com.example.arbibleapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class intro1 extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    
    private ViewPager2 viewPager;
    private Button btnNext;
    private TextView tvSkip;
    private View dot1, dot2, dot3;

    private static class IntroSlide {
        int iconRes;
        String title, description;
        IntroSlide(int iconRes, String title, String description) {
            this.iconRes = iconRes;
            this.title = title;
            this.description = description;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            checkUserTypeAndRedirect(currentUser.getUid());
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_intro1);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        viewPager = findViewById(R.id.viewPager);
        btnNext = findViewById(R.id.btnNext);
        tvSkip = findViewById(R.id.tvSkip);
        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);
        dot3 = findViewById(R.id.dot3);
        
        List<IntroSlide> slides = new ArrayList<>();
        slides.add(new IntroSlide(R.drawable.into1_logo, "AR Bible Experience", "Scan pages and bring Bible stories to life with augmented reality 3D animations"));
        slides.add(new IntroSlide(R.drawable.intro2_logo, "Interactive Stories", "Explore Bible stories with engaging animations, audio narration, and fun characters"));
        slides.add(new IntroSlide(R.drawable.intro3_logo, "Quizzes & Attendance", "Test your knowledge with fun quizzes and track your learning journey with badges"));

        IntroAdapter adapter = new IntroAdapter(slides);
        viewPager.setAdapter(adapter);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateIndicators(position);
                if (position == slides.size() - 1) {
                    btnNext.setText("Get Started");
                } else {
                    btnNext.setText("Next");
                }
            }
        });

        btnNext.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() < slides.size() - 1) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, true);
            } else {
                startActivity(new Intent(intro1.this, login.class));
                finish();
            }
        });

        tvSkip.setOnClickListener(v -> {
            startActivity(new Intent(intro1.this, login.class));
            finish();
        });
    }
    
    private void updateIndicators(int position) {
        dot1.setAlpha(position == 0 ? 1f : 0.2f);
        dot1.getLayoutParams().width = position == 0 ? dpToPx(32) : dpToPx(8);
        dot1.requestLayout();
        
        dot2.setAlpha(position == 1 ? 1f : 0.2f);
        dot2.getLayoutParams().width = position == 1 ? dpToPx(32) : dpToPx(8);
        dot2.requestLayout();
        
        dot3.setAlpha(position == 2 ? 1f : 0.2f);
        dot3.getLayoutParams().width = position == 2 ? dpToPx(32) : dpToPx(8);
        dot3.requestLayout();
    }
    
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    private void checkUserTypeAndRedirect(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String userType = documentSnapshot.getString("userType");
                        Intent intent;
                        if ("Admin".equals(userType)) {
                            intent = new Intent(intro1.this, AdminDashboard.class);
                        } else if ("Teacher".equals(userType)) {
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
    
    private static class IntroAdapter extends RecyclerView.Adapter<IntroAdapter.IntroViewHolder> {
        private final List<IntroSlide> slides;

        IntroAdapter(List<IntroSlide> slides) {
            this.slides = slides;
        }

        @NonNull
        @Override
        public IntroViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_intro_slide, parent, false);
            return new IntroViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull IntroViewHolder holder, int position) {
            IntroSlide slide = slides.get(position);
            holder.ivIllustration.setImageResource(slide.iconRes);
            holder.tvTitle.setText(slide.title);
            holder.tvDescription.setText(slide.description);
        }

        @Override
        public int getItemCount() {
            return slides.size();
        }

        static class IntroViewHolder extends RecyclerView.ViewHolder {
            ImageView ivIllustration;
            TextView tvTitle, tvDescription;

            IntroViewHolder(@NonNull View itemView) {
                super(itemView);
                ivIllustration = itemView.findViewById(R.id.viewIllustration);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvDescription = itemView.findViewById(R.id.tvDescription);
            }
        }
    }
}
