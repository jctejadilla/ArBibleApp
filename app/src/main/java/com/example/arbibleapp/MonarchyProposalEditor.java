package com.example.arbibleapp;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.*;

/** Scenario drafts keep image actions attached to stable scene IDs while rows move. */
final class MonarchyProposalEditor {
    private final AppCompatActivity activity;
    private final LinearLayout rows;
    private final Spinner selector;
    private final EditText title;
    private final Button submit;
    private final ActivityResultLauncher<String> picker;
    private List<MonarchyContentBank.Scenario> bank;
    private final List<Draft> draft = new ArrayList<>();
    private boolean active, ready, processing, submitting;
    private int version, pickerVersion, index;
    private Draft picked;
    private static final class Draft {
        MonarchyContentBank.Scene scene;
        String caption, action = "KEEP";
        byte[] bytes;
        EditText input;
        Draft(MonarchyContentBank.Scene scene) { this.scene = scene; caption = scene.caption; }
    }
    MonarchyProposalEditor(AppCompatActivity activity, LinearLayout form, Spinner selector, EditText title, Button submit) {
        this.activity = activity; this.selector = selector; this.title = title; this.submit = submit;
        rows = new LinearLayout(activity); rows.setOrientation(LinearLayout.VERTICAL);
        form.addView(rows, form.indexOfChild(submit) < 0 ? form.getChildCount() : form.indexOfChild(submit));
        rows.setVisibility(View.GONE);
        picker = activity.registerForActivityResult(new ActivityResultContracts.GetContent(), this::picked);
    }
    void deactivate() { active = false; ready = false; version++; rows.setVisibility(View.GONE); selector.setEnabled(true); title.setEnabled(true); }
    void load() {
        active = true; ready = false; processing = false; submitting = false; version++;
        final int request = version;
        rows.removeAllViews(); rows.setVisibility(View.VISIBLE);
        selector.setOnItemSelectedListener(null);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, R.layout.spinner_item_dark, MonarchyScenarioBank.TITLES);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_dark);
        selector.setAdapter(adapter);
        submit.setEnabled(false);
        title.setText("");
        FirebaseFirestore.getInstance().collection("quiz_questions")
                .whereEqualTo("storyTitle", MonarchyScenarioBank.STORY_TITLE)
                .whereEqualTo("approvedByAdmin", true).get().addOnSuccessListener(result -> {
                    if (!valid(request)) return;
                    bank = MonarchyContentBank.load(result.getDocuments());
                    ready = true;
                    selector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override public void onItemSelected(AdapterView<?> p, View v, int position, long id) { if (active && ready) select(position); }
                        @Override public void onNothingSelected(AdapterView<?> p) { }
                    });
                    select(Math.max(0, selector.getSelectedItemPosition()));
                }).addOnFailureListener(error -> {
                    if (valid(request)) message("Could not load current scenarios. Select Monarchy again to retry.");
                });
    }
    private boolean valid(int generation) {
        return active && generation == version && !activity.isFinishing() && !activity.isDestroyed();
    }
    private void select(int position) {
        version++; index = position; processing = false;
        draft.clear();
        MonarchyContentBank.Scenario scenario = bank.get(position);
        title.setText(scenario.title);
        for (MonarchyContentBank.Scene scene : scenario.scenes) draft.add(new Draft(scene));
        render();
    }
    private void capture() {
        for (Draft d : draft) if (d.input != null) d.caption = d.input.getText().toString().trim();
    }
    private void render() {
        rows.removeAllViews();
        TextView help = new TextView(activity);
        help.setText("Correct order: arrange the six scenes from first to last. Pictures stay with their scenes.");
        help.setTextColor(activity.getResources().getColor(R.color.text_grey, null));
        help.setTextSize(13f);
        help.setPadding(0, 0, 0, dp(16));
        rows.addView(help);
        for (int i = 0; i < draft.size(); i++) {
            final int position = i; Draft d = draft.get(i);
            
            // Header for Scene
            TextView number = new TextView(activity); 
            number.setText("Scene " + (i + 1)); 
            number.setTextColor(activity.getResources().getColor(R.color.text_dark, null));
            number.setTextSize(14f);
            number.setTypeface(null, Typeface.BOLD);
            number.setPadding(0, dp(8), 0, dp(4));
            rows.addView(number);
            
            // Image Preview
            ImageView image = new ImageView(activity); 
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            rows.addView(image, new LinearLayout.LayoutParams(-1, dp(150)));
            byte[] bytes = "REPLACE".equals(d.action) ? d.bytes : "KEEP".equals(d.action) ? d.scene.bytes : null;
            if (bytes == null) MonarchyImages.show(image, d.scene.key);
            else LawImageCodec.display(image, bytes, 0);
            
            // Caption EditText
            d.input = new EditText(activity); 
            d.input.setHint("Scene description (max 300 characters)");
            d.input.setText(d.caption); 
            d.input.setEnabled(!submitting);
            d.input.setTextColor(activity.getResources().getColor(R.color.primary_green, null));
            d.input.setHintTextColor(Color.parseColor("#882E7D32"));
            d.input.setTextSize(14f);
            d.input.setBackgroundResource(R.drawable.bg_outlined_edittext);
            d.input.setPadding(dp(16), dp(16), dp(16), dp(16));
            d.input.setMinHeight(dp(52));
            LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(-1, -2);
            inputParams.setMargins(0, dp(8), 0, dp(4));
            rows.addView(d.input, inputParams);
            
            // Status Text
            TextView status = new TextView(activity);
            status.setText("REPLACE".equals(d.action) ? "Uploaded replacement" : "DEFAULT".equals(d.action) ? "Original picture" : "Current approved picture");
            status.setTextColor(activity.getResources().getColor(R.color.text_grey, null));
            status.setTextSize(12f);
            status.setPadding(0, 0, 0, dp(8));
            rows.addView(status);
            
            // Buttons
            LinearLayout images = new LinearLayout(activity); 
            rows.addView(images);
            button(images, "Upload", !processing && !submitting, () -> {
                capture(); picked = d; pickerVersion = version; picker.launch("image/*");
            }, false);
            button(images, "Original", !processing && !submitting, () -> {
                capture(); d.action = "DEFAULT"; d.bytes = null; render();
            }, false);
            button(images, "Keep current", !processing && !submitting, () -> {
                capture(); d.action = "KEEP"; d.bytes = null; render();
            }, false);
            
            LinearLayout move = new LinearLayout(activity); 
            LinearLayout.LayoutParams moveParams = new LinearLayout.LayoutParams(-1, -2);
            moveParams.setMargins(0, dp(4), 0, dp(24));
            rows.addView(move, moveParams);
            button(move, "Move up", i > 0 && !processing && !submitting, () -> move(position, position - 1), true);
            button(move, "Move down", i < 5 && !processing && !submitting, () -> move(position, position + 1), true);
        }
        submit.setEnabled(ready && !processing && !submitting);
        selector.setEnabled(!submitting); title.setEnabled(!submitting);
    }
    private void move(int from, int to) { capture(); Collections.swap(draft, from, to); render(); }
    private void button(LinearLayout parent, String label, boolean enabled, Runnable action, boolean primary) {
        AppCompatButton button = new AppCompatButton(activity);
        button.setText(label); 
        button.setEnabled(enabled);
        button.setAllCaps(false);
        button.setTextSize(14f);
        button.setTextColor(activity.getResources().getColor(android.R.color.white, null));
        button.setBackgroundResource(R.drawable.bg_button_next);
        button.setBackgroundTintList(ColorStateList.valueOf(
                activity.getResources().getColor(primary ? R.color.primary_green : R.color.badge_red, null)
        ));
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(48), 1);
        params.setMargins(dp(4), 0, dp(4), 0);
        parent.addView(button, params);
        button.setOnClickListener(v -> action.run());
    }
    private void picked(Uri uri) {
        if (uri == null || !valid(pickerVersion) || picked == null) return;
        final int request = version; final Draft target = picked;
        capture(); processing = true; render();
        LawImageCodec.WORKER.execute(() -> {
            try {
                byte[] bytes = LawImageCodec.prepare(activity.getContentResolver(), uri, MonarchyContentBank.IMAGE_LIMIT);
                activity.runOnUiThread(() -> {
                    if (!valid(request)) return;
                    capture(); processing = false; target.action = "REPLACE"; target.bytes = bytes; render();
                });
            } catch (Exception | OutOfMemoryError error) {
                activity.runOnUiThread(() -> {
                    if (!valid(request)) return;
                    capture(); processing = false; render(); message("Could not prepare image. Choose another picture.");
                });
            }
        });
    }
    void submit() {
        if (!active || !ready || processing || submitting) return;
        if (FirebaseAuth.getInstance().getCurrentUser() == null) { message("Not authenticated"); return; }
        capture();
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Map<String,Object> proposal = new HashMap<>();
        proposal.put("storyTitle", MonarchyScenarioBank.STORY_TITLE);
        proposal.put("quizType", 7); proposal.put("itemIndex", index); proposal.put("operation", "EDIT");
        proposal.put("originalQuestion", bank.get(index).title);
        proposal.put("proposedQuestion", title.getText().toString().trim());
        List<Map<String,Object>> scenes = new ArrayList<>();
        for (Draft d : draft) {
            Map<String,Object> scene = new HashMap<>();
            scene.put("key", d.scene.key); scene.put("caption", d.caption); scene.put("imageAction", d.action);
            if ("REPLACE".equals(d.action)) scene.put("imageBytes", Blob.fromBytes(d.bytes));
            scenes.add(scene);
        }
        proposal.put("scenes", scenes);
        Map<String,Object> validation = new HashMap<>(proposal);
        validation.put("approvedByAdmin", true); validation.put("question", proposal.get("proposedQuestion"));
        if (MonarchyContentBank.parse(validation) == null) {
            message("Enter a title of 1-120 characters and six descriptions of 1-300 characters."); return;
        }
        proposal.put("teacherUid", uid); proposal.put("options", new ArrayList<String>());
        proposal.put("correctAnswerIndex", 0); proposal.put("status", "PENDING"); proposal.put("timestamp", Timestamp.now());
        submitting = true; render();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(uid).get().continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();
            String name = task.getResult().getString("username");
            if (name == null || name.isEmpty()) name = task.getResult().getString("fullName");
            proposal.put("teacherName", name == null || name.isEmpty() ? "Teacher" : name);
            return db.collection("question_edit_proposals").add(proposal);
        }).addOnSuccessListener(ref -> {
            if (activity.isFinishing() || activity.isDestroyed()) return;
            message("Proposal submitted for admin approval."); activity.finish();
        }).addOnFailureListener(error -> {
            submitting = false;
            if (active && !activity.isDestroyed()) { render(); message("Could not submit proposal. Please retry."); }
        });
    }
    private void message(String text) { Toast.makeText(activity, text, Toast.LENGTH_LONG).show(); }
    private int dp(int n) { return Math.round(n * activity.getResources().getDisplayMetrics().density); }
}
