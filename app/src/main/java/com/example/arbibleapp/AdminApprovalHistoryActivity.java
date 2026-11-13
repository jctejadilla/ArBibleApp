package com.example.arbibleapp;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.example.arbibleapp.databinding.ActivityAdminApprovalHistoryBinding;
import com.example.arbibleapp.databinding.ItemAdminHistoryBinding;
import androidx.recyclerview.widget.DiffUtil;

public class AdminApprovalHistoryActivity extends AppCompatActivity {

    private ActivityAdminApprovalHistoryBinding binding;
    private FirebaseFirestore db;
    private HistoryAdapter adapter;

    private final List<HistoryModel> allHistoryList = new ArrayList<>();
    private final List<HistoryModel> filteredList = new ArrayList<>();
    private ListenerRegistration historyListener;

    private String currentFilter = "ALL";
    private int currentLimit = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityAdminApprovalHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        binding.btnBack.setOnClickListener(v -> finish());
        
        binding.rvHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(filteredList);
        binding.rvHistory.setAdapter(adapter);
        
        binding.rvHistory.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (!recyclerView.canScrollVertically(1)) {
                    currentLimit += 20;
                    loadApprovalHistory();
                }
            }
        });

        setupFilterToggle();
        loadApprovalHistory();
    }

    private void setupFilterToggle() {
        binding.toggleFilter.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnFilterAll) {
                currentFilter = "ALL";
            } else if (checkedId == R.id.btnFilterApproved) {
                currentFilter = "APPROVED";
            } else if (checkedId == R.id.btnFilterRejected) {
                currentFilter = "REJECTED";
            }
            applyFilter();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (historyListener != null) historyListener.remove();
    }

    private void loadApprovalHistory() {
        if (historyListener != null) historyListener.remove();

        historyListener = db.collection("question_edit_proposals")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(currentLimit)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    allHistoryList.clear();
                    for (DocumentSnapshot doc : snapshots) {
                        String status = doc.getString("status");
                        if ("APPROVED".equals(status) || "REJECTED".equals(status)) {
                            String id = doc.getId();
                            String teacherName = doc.getString("teacherName");
                            String story = doc.getString("storyTitle");
                            String origQ = doc.getString("originalQuestion");
                            String propQ = doc.getString("proposedQuestion");
                            if ("RESTORE_ALL".equals(doc.getString("operation"))) {
                                origQ = "All content in " + story;
                                propQ = "RESET TO DEFAULTS: " + story;
                            }
                            if ((LawScenarioBank.STORY_TITLE.equals(story) || JudgesQuizGame.STORY_TITLE.equals(story) || ReturnMatchingGame.STORY_TITLE.equals(story) || BirthWordBank.STORY_TITLE.equals(story))
                                    && !"RESTORE_ALL".equals(doc.getString("operation"))) {
                                LawImageData image = LawImageData.read(doc.get("imageAction"), doc.get("imageBytes"));
                                propQ = (propQ == null ? "" : propQ) + "\nImage: " +
                                        ("REPLACE".equals(image.action) ? "uploaded replacement"
                                                : "DEFAULT".equals(image.action) ? "original default" : "keep current");
                            }
                            if (MonarchyScenarioBank.STORY_TITLE.equals(story) && !"RESTORE_ALL".equals(doc.getString("operation"))) {
                                Object rows = doc.get("scenes");
                                if (rows instanceof List) {
                                    StringBuilder summary = new StringBuilder(propQ == null ? "" : propQ);
                                    int position = 1;
                                    for (Object row : (List<?>) rows) if (row instanceof Map) {
                                        Map<?,?> scene = (Map<?,?>) row;
                                        summary.append("\n").append(position++).append(". ").append(scene.get("caption"))
                                                .append(" (image: ").append(scene.get("imageAction")).append(")");
                                    }
                                    propQ = summary.toString();
                                }
                            }
                            if (DivisionPairBank.STORY_TITLE.equals(story) && !"RESTORE_ALL".equals(doc.getString("operation"))) {
                                propQ = (propQ == null ? "" : propQ) + " - " + doc.getString("objectName");
                                for (int role = 1; role <= 2; role++) {
                                    LawImageData image = LawImageData.read(doc.get("image" + role + "Action"), doc.get("image" + role + "Bytes"));
                                    propQ += "\n" + (role == 1 ? "Character picture: " : "Object picture: ")
                                            + ("REPLACE".equals(image.action) ? "uploaded replacement" : "DEFAULT".equals(image.action) ? "original" : "keep current");
                                }
                            }
                            if (BirthWordBank.STORY_TITLE.equals(story) && !"RESTORE_ALL".equals(doc.getString("operation"))) {
                                origQ = (origQ == null ? "" : origQ) + "\nAnswer: " + doc.getString("originalAnswerWord");
                                propQ = (propQ == null ? "" : propQ) + "\nAnswer: " + doc.getString("answerWord");
                            }
                            Timestamp ts = doc.getTimestamp("timestamp");

                            allHistoryList.add(new HistoryModel(
                                    id,
                                    status,
                                    teacherName != null ? teacherName : "Teacher",
                                    story != null ? story : "Bible Story",
                                    origQ != null ? origQ : "",
                                    propQ != null ? propQ : "",
                                    ts,
                                    doc.getData()
                            ));
                        }
                    }
                    applyFilter();
                });
    }

    private void applyFilter() {
        List<HistoryModel> newFilteredList = new ArrayList<>();
        for (HistoryModel item : allHistoryList) {
            if ("ALL".equals(currentFilter) || currentFilter.equals(item.status)) {
                newFilteredList.add(item);
            }
        }

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new HistoryDiffCallback(filteredList, newFilteredList));
        filteredList.clear();
        filteredList.addAll(newFilteredList);
        diffResult.dispatchUpdatesTo(adapter);

        if (filteredList.isEmpty()) {
            binding.tvEmptyState.setVisibility(View.VISIBLE);
            binding.rvHistory.setVisibility(View.GONE);
        } else {
            binding.tvEmptyState.setVisibility(View.GONE);
            binding.rvHistory.setVisibility(View.VISIBLE);
        }
    }

    private static class HistoryDiffCallback extends DiffUtil.Callback {
        private final List<HistoryModel> oldList;
        private final List<HistoryModel> newList;

        public HistoryDiffCallback(List<HistoryModel> oldList, List<HistoryModel> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).id.equals(newList.get(newItemPosition).id);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            HistoryModel oldItem = oldList.get(oldItemPosition);
            HistoryModel newItem = newList.get(newItemPosition);
            return oldItem.status.equals(newItem.status) &&
                   oldItem.storyTitle.equals(newItem.storyTitle) &&
                   oldItem.teacherName.equals(newItem.teacherName);
        }
    }

    private static class HistoryModel {
        String id, status, teacherName, storyTitle, originalQuestion, proposedQuestion;
        Timestamp timestamp;
        Map<String, Object> raw;

        HistoryModel(String id, String status, String teacherName, String storyTitle, String originalQuestion, String proposedQuestion, Timestamp timestamp, Map<String, Object> raw) {
            this.id = id;
            this.status = status;
            this.teacherName = teacherName;
            this.storyTitle = storyTitle;
            this.originalQuestion = originalQuestion;
            this.proposedQuestion = proposedQuestion;
            this.timestamp = timestamp;
            this.raw = raw;
        }
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private final List<HistoryModel> list;

        HistoryAdapter(List<HistoryModel> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemAdminHistoryBinding itemBinding = ItemAdminHistoryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HistoryModel item = list.get(position);
            holder.binding.tvStatusBadge.setText(item.status);

            if ("APPROVED".equals(item.status)) {
                holder.binding.tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            } else {
                holder.binding.tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D32F2F")));
            }

            holder.binding.tvStoryTitle.setText(item.storyTitle);
            holder.binding.tvTeacherName.setText("Proposed by: " + item.teacherName);
            holder.binding.tvOriginalQuestion.setText(item.originalQuestion);
            holder.binding.tvProposedQuestion.setText(item.proposedQuestion);

            if (item.timestamp != null) {
                String timeStr = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(item.timestamp.toDate());
                holder.binding.tvTimestamp.setText(timeStr);
            } else {
                holder.binding.tvTimestamp.setText("Recently");
            }
            
            holder.binding.ivHistoryImagePreview.setVisibility(View.GONE);
            holder.binding.ivHistoryImagePreview.setImageDrawable(null);
            
            boolean isLaw = LawScenarioBank.STORY_TITLE.equals(item.storyTitle);
            boolean isBabel = "The Tower of Babel and Abraham's Covenant".equals(item.storyTitle);
            boolean isExodus = "Slavery in Egypt and the Exodus".equals(item.storyTitle);
            boolean restoreQuiz = "RESTORE_ALL".equals(item.raw.get("operation"));
            
            if (!restoreQuiz) {
                if (JudgesQuizGame.STORY_TITLE.equals(item.storyTitle)) {
                    LawImageData image = LawImageData.read(item.raw.get("imageAction"), item.raw.get("imageBytes"));
                    if ("REPLACE".equals(image.action) && image.bytes != null) {
                        holder.binding.ivHistoryImagePreview.setVisibility(View.VISIBLE);
                        LawImageCodec.display(holder.binding.ivHistoryImagePreview, image.bytes, 0);
                    }
                } else if (isLaw) {
                    LawImageData image = LawImageData.read(item.raw.get("imageAction"), item.raw.get("imageBytes"));
                    if ("REPLACE".equals(image.action) && image.bytes != null) {
                        holder.binding.ivHistoryImagePreview.setVisibility(View.VISIBLE);
                        LawImageCodec.display(holder.binding.ivHistoryImagePreview, image.bytes, 0);
                    }
                } else if (ReturnMatchingGame.STORY_TITLE.equals(item.storyTitle) || BirthWordBank.STORY_TITLE.equals(item.storyTitle)) {
                    LawImageData image = LawImageData.read(item.raw.get("imageAction"), item.raw.get("imageBytes"));
                    if ("REPLACE".equals(image.action) && image.bytes != null) {
                        holder.binding.ivHistoryImagePreview.setVisibility(View.VISIBLE);
                        LawImageCodec.display(holder.binding.ivHistoryImagePreview, image.bytes, 0);
                    }
                }
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ItemAdminHistoryBinding binding;

            ViewHolder(ItemAdminHistoryBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}