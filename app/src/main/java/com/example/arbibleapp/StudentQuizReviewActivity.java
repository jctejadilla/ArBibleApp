package com.example.arbibleapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class StudentQuizReviewActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvStoryTitle, tvTotalAttempts, tvBestScore, tvAvgScore;
    private RecyclerView rvAttempts;
    private Button btnTakeQuiz;
    private final List<TeacherQuizResults.QuizResultModel> attemptsList = new ArrayList<>();
    private AttemptsAdapter adapter;
    
    private String storyTitle;
    private Class<?> quizClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_student_quiz_review);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        storyTitle = getIntent().getStringExtra("storyTitle");
        String quizClassName = getIntent().getStringExtra("quizClass");
        try {
            if (quizClassName != null) {
                quizClass = Class.forName(quizClassName);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        btnBack = findViewById(R.id.btnBack);
        tvStoryTitle = findViewById(R.id.tvStoryTitle);
        tvTotalAttempts = findViewById(R.id.tvTotalAttempts);
        tvBestScore = findViewById(R.id.tvBestScore);
        tvAvgScore = findViewById(R.id.tvAvgScore);
        rvAttempts = findViewById(R.id.rvWrongAnswers);
        btnTakeQuiz = findViewById(R.id.btnTakeQuiz);

        tvStoryTitle.setText(storyTitle);
        btnBack.setOnClickListener(v -> finish());
        
        View.OnClickListener retakeAction = v -> {
            if (quizClass != null) {
                Intent intent = new Intent(this, quizClass);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Quiz not available for this story yet", Toast.LENGTH_SHORT).show();
            }
        };

        btnTakeQuiz.setOnClickListener(retakeAction);

        rvAttempts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AttemptsAdapter(attemptsList, retakeAction);
        rvAttempts.setAdapter(adapter);

        loadAttempts();
    }

    private void loadAttempts() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        FirebaseFirestore.getInstance().collection("quiz_results")
                .whereEqualTo("studentUid", uid)
                .whereEqualTo("storyTitle", storyTitle)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    attemptsList.clear();
                    int totalScore = 0;
                    int totalPossible = 0;
                    int maxScore = -1;
                    int maxPossible = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        TeacherQuizResults.QuizResultModel result = doc.toObject(TeacherQuizResults.QuizResultModel.class);
                        if (result != null) {
                            attemptsList.add(result);
                            totalScore += result.score;
                            totalPossible += result.totalQuestions;
                            if (result.score > maxScore) {
                                maxScore = result.score;
                                maxPossible = result.totalQuestions;
                            }
                        }
                    }

                    Collections.sort(attemptsList, (r1, r2) -> {
                        if (r1.timestamp == null || r2.timestamp == null) return 0;
                        return r2.timestamp.compareTo(r1.timestamp);
                    });

                    adapter.notifyDataSetChanged();
                    updateSummary(attemptsList.size(), maxScore, maxPossible, totalScore, totalPossible);
                });
    }

    private void updateSummary(int count, int best, int bestPossible, int total, int totalPossible) {
        tvTotalAttempts.setText(String.valueOf(count));
        if (count > 0) {
            tvBestScore.setText(String.format(Locale.getDefault(), "%d/%d", best, bestPossible));
            int avg = (int) (((double) total / totalPossible) * 100);
            tvAvgScore.setText(String.format(Locale.getDefault(), "%d%%", avg));
        } else {
            tvBestScore.setText("0/0");
            tvAvgScore.setText("0%");
        }
    }

    private static class AttemptsAdapter extends RecyclerView.Adapter<AttemptsAdapter.ViewHolder> {
        private final List<TeacherQuizResults.QuizResultModel> list;
        private final View.OnClickListener retakeListener;
        private int expandedPosition = -1;
        private final Set<Integer> showCorrectSet = new HashSet<>();

        AttemptsAdapter(List<TeacherQuizResults.QuizResultModel> list, View.OnClickListener retakeListener) { 
            this.list = list; 
            this.retakeListener = retakeListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_quiz_attempt_expandable, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TeacherQuizResults.QuizResultModel r = list.get(position);
            String title = String.format(Locale.getDefault(), "%d/%d - %s", 
                    r.score, r.totalQuestions, r.date != null ? r.date : "Recent");
            holder.tvTitle.setText(title);

            String timeStr = "";
            if (r.timestamp != null) {
                timeStr = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(r.timestamp.toDate());
            }
            
            String status = r.isSunday ? "" : "Trial";
            if (!status.isEmpty() && !timeStr.isEmpty()) {
                holder.tvStatus.setText(status + " • " + timeStr);
                holder.tvStatus.setVisibility(View.VISIBLE);
            } else if (!timeStr.isEmpty()) {
                holder.tvStatus.setText(timeStr);
                holder.tvStatus.setVisibility(View.VISIBLE);
            } else if (!status.isEmpty()) {
                holder.tvStatus.setText(status);
                holder.tvStatus.setVisibility(View.VISIBLE);
            } else {
                holder.tvStatus.setVisibility(View.GONE);
            }

            boolean isExpanded = position == expandedPosition;
            holder.layoutWrongAnswers.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            holder.ivExpandIcon.setRotation(isExpanded ? 180f : 0f);

            holder.itemView.setOnClickListener(v -> {
                int previousExpanded = expandedPosition;
                expandedPosition = isExpanded ? -1 : holder.getAdapterPosition();
                notifyItemChanged(previousExpanded);
                notifyItemChanged(expandedPosition);
            });

            holder.btnRetake.setOnClickListener(retakeListener);
            
            boolean showCorrect = showCorrectSet.contains(position);
            holder.btnShowCorrect.setText(showCorrect ? "Hide Correct Answers" : "Show Correct Answers");
            holder.btnShowCorrect.setOnClickListener(v -> {
                if (showCorrect) showCorrectSet.remove(position);
                else showCorrectSet.add(position);
                notifyItemChanged(position);
            });

            holder.containerWrongAnswers.removeAllViews();
            if (r.wrongAnswers == null || r.wrongAnswers.isEmpty()) {
                TextView tv = new TextView(holder.itemView.getContext());
                tv.setText("No recorded mistakes. Perfect!");
                tv.setTextColor(Color.parseColor("#2E7D32"));
                tv.setTextSize(13);
                holder.containerWrongAnswers.addView(tv);
                holder.btnShowCorrect.setVisibility(View.GONE);
            } else {
                holder.btnShowCorrect.setVisibility(View.VISIBLE);
                for (int i = 0; i < r.wrongAnswers.size(); i++) {
                    String wrong = r.wrongAnswers.get(i);
                    
                    LinearLayout itemLayout = new LinearLayout(holder.itemView.getContext());
                    itemLayout.setOrientation(LinearLayout.VERTICAL);
                    itemLayout.setPadding(0, 4, 0, 8);

                    TextView tvWrong = new TextView(holder.itemView.getContext());
                    tvWrong.setText("• " + wrong);
                    tvWrong.setTextColor(Color.parseColor("#1D4A4B"));
                    tvWrong.setTextSize(13);
                    itemLayout.addView(tvWrong);

                    if (showCorrect && r.correctAnswers != null && i < r.correctAnswers.size()) {
                        if (r.selectedAnswers != null && i < r.selectedAnswers.size()) {
                            TextView tvSelected = new TextView(holder.itemView.getContext());
                            tvSelected.setText("  ✘ Your Answer: " + r.selectedAnswers.get(i));
                            tvSelected.setTextColor(Color.parseColor("#C62828"));
                            tvSelected.setTextSize(12);
                            tvSelected.setPadding(20, 2, 0, 0);
                            itemLayout.addView(tvSelected);
                        }

                        TextView tvCorrect = new TextView(holder.itemView.getContext());
                        tvCorrect.setText("  ✔ Correct: " + r.correctAnswers.get(i));
                        tvCorrect.setTextColor(Color.parseColor("#2E7D32"));
                        tvCorrect.setTextSize(12);
                        tvCorrect.setPadding(20, 2, 0, 0);
                        itemLayout.addView(tvCorrect);
                    }
                    
                    holder.containerWrongAnswers.addView(itemLayout);
                }
            }
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvStatus;
            ImageView ivExpandIcon;
            LinearLayout layoutWrongAnswers, containerWrongAnswers;
            Button btnShowCorrect, btnRetake;
            ViewHolder(View v) {
                super(v);
                tvTitle = v.findViewById(R.id.tvAttemptTitle);
                tvStatus = v.findViewById(R.id.tvAttemptStatus);
                ivExpandIcon = v.findViewById(R.id.ivExpandIcon);
                layoutWrongAnswers = v.findViewById(R.id.layoutWrongAnswers);
                containerWrongAnswers = v.findViewById(R.id.containerWrongAnswers);
                btnShowCorrect = v.findViewById(R.id.btnShowCorrect);
                btnRetake = v.findViewById(R.id.btnRetakeCard);
            }
        }
    }
}