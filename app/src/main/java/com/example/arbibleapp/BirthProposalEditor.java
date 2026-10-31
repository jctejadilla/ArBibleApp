package com.example.arbibleapp;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.*;

final class BirthProposalEditor {
    private final AppCompatActivity activity;
    private final Spinner selector;
    private final EditText label, answer;
    private final LinearLayout container;
    private final ImageView preview;
    private final TextView status;
    private final Button submit, upload, original, keep;
    private final ActivityResultLauncher<String> picker;
    private List<BirthContentBank.Item> bank;
    private String action = "KEEP";
    private byte[] imageBytes;
    private int index, version, pickerVersion;
    private boolean active, ready, processing, submitting;
    BirthProposalEditor(AppCompatActivity activity, LinearLayout form, Spinner selector, EditText label, Button submit) {
        this.activity = activity; this.selector = selector; this.label = label; this.submit = submit;
        container = new LinearLayout(activity); container.setOrientation(LinearLayout.VERTICAL);
        form.addView(container, form.indexOfChild(submit));
        TextView help = new TextView(activity);
        help.setText("This picture will appear on the selected word question.");
        help.setTextColor(ContextCompat.getColor(activity, R.color.text_grey));
        help.setTextSize(13f);
        container.addView(help);
        TextView answerLabel = new TextView(activity);
        answerLabel.setText("Answer word (3-10 English letters, for example JOHN)"); 
        answerLabel.setTextColor(ContextCompat.getColor(activity, R.color.text_dark));
        answerLabel.setTypeface(null, Typeface.BOLD);
        answerLabel.setTextSize(14f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(16);
        container.addView(answerLabel, lp);
        answer = new EditText(activity); answer.setSingleLine(true);
        answer.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        answer.setTextColor(ContextCompat.getColor(activity, R.color.primary_green));
        answer.setHintTextColor(Color.parseColor("#882E7D32"));
        answer.setTextSize(14f);
        int pad = dp(16);
        answer.setPadding(pad, pad, pad, pad);
        answer.setMinHeight(dp(52));
        answer.setBackgroundResource(R.drawable.bg_outlined_edittext);
        answer.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(-1, -2);
        alp.topMargin = dp(8);
        container.addView(answer, alp);
        preview = new ImageView(activity); preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(-1, dp(180));
        plp.topMargin = dp(16);
        container.addView(preview, plp);
        status = new TextView(activity); 
        status.setTextColor(ContextCompat.getColor(activity, R.color.text_dark));
        container.addView(status);
        picker = activity.registerForActivityResult(new ActivityResultContracts.GetContent(), this::picked);
        LinearLayout buttons = new LinearLayout(activity); 
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, -2);
        blp.topMargin = dp(8);
        blp.bottomMargin = dp(16);
        container.addView(buttons, blp);
        upload = button(buttons, "Upload", () -> {
            if (!ready || processing || submitting) return;
            pickerVersion = version; picker.launch("image/*");
        });
        original = button(buttons, "Original", () -> imageAction("DEFAULT"));
        keep = button(buttons, "Keep current", () -> imageAction("KEEP"));
        container.setVisibility(View.GONE);
    }
    private Button button(LinearLayout row, String text, Runnable callback) {
        Button b = new Button(activity);
        b.setText(text); b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(activity, R.color.primary_green)));
        b.setBackgroundResource(R.drawable.bg_button_next);
        row.addView(b, new LinearLayout.LayoutParams(0, dp(48), 1)); 
        ((LinearLayout.LayoutParams)b.getLayoutParams()).setMargins(dp(4), 0, dp(4), 0);
        b.setOnClickListener(v -> callback.run()); return b;
    }
    void deactivate() {
        active = false; ready = false; version++; container.setVisibility(View.GONE);
        selector.setEnabled(true); label.setEnabled(true); preview.setTag(null);
    }
    void load() {
        active = true; ready = false; processing = false; submitting = false; version++;
        final int request = version;
        container.setVisibility(View.VISIBLE); selector.setOnItemSelectedListener(null);
        List<String> labels = new ArrayList<>(Arrays.asList(BirthWordBank.NAMES));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, R.layout.spinner_item_dark, labels);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_dark); selector.setAdapter(adapter);
        label.setText(""); preview.setImageDrawable(null); status.setText("Loading current picture..."); controls();
        FirebaseFirestore.getInstance().collection("quiz_questions")
                .whereEqualTo("storyTitle", BirthWordBank.STORY_TITLE).whereEqualTo("approvedByAdmin", true)
                .get().addOnSuccessListener(result -> {
                    if (!valid(request)) return;
                    bank = BirthContentBank.load(result.getDocuments());
                    for (int i = 0; i < 10; i++) labels.set(i, bank.get(i).label);
                    adapter.notifyDataSetChanged(); ready = true;
                    selector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override public void onItemSelected(AdapterView<?> p, View v, int position, long id) { if (active && ready) select(position); }
                        @Override public void onNothingSelected(AdapterView<?> p) { }
                    });
                    select(Math.max(0, selector.getSelectedItemPosition()));
                }).addOnFailureListener(error -> {
                    if (valid(request)) message("Could not load current items. Select Birth again to retry.");
                });
    }
    private boolean valid(int request) { return active && request == version && !activity.isFinishing() && !activity.isDestroyed(); }
    private void select(int position) {
        index = position; version++; processing = false;
        label.setText(bank.get(index).label); answer.setText(bank.get(index).answer); action = "KEEP"; imageBytes = null; preview(); controls();
    }
    private void imageAction(String value) {
        if (!active || !ready || submitting) return;
        version++; processing = false; action = value; imageBytes = null; preview(); controls();
    }
    private void preview() {
        byte[] bytes = "REPLACE".equals(action) ? imageBytes : "KEEP".equals(action) ? bank.get(index).imageBytes : null;
        if (bytes == null) { preview.setTag(null); BirthImages.show(preview, BirthWordBank.KEYS[index]); }
        else LawImageCodec.display(preview, bytes, 0);
        status.setText("REPLACE".equals(action) ? "Uploaded replacement for this item"
                : "DEFAULT".equals(action) ? "Original picture for this item" : "Current approved picture for this item");
    }
    private void controls() {
        submit.setEnabled(active && ready && !processing && !submitting);
        selector.setEnabled(!submitting); label.setEnabled(!submitting); answer.setEnabled(ready && !submitting);
        upload.setEnabled(ready && !processing && !submitting);
        original.setEnabled(ready && !submitting); keep.setEnabled(ready && !submitting);
    }
    private void picked(Uri uri) {
        if (uri == null || !valid(pickerVersion) || !ready) return;
        final int request = version;
        processing = true; status.setText("Preparing image..."); controls();
        LawImageCodec.WORKER.execute(() -> {
            try {
                byte[] bytes = LawImageCodec.prepare(activity.getContentResolver(), uri);
                activity.runOnUiThread(() -> {
                    if (!valid(request)) return;
                    processing = false; action = "REPLACE"; imageBytes = bytes; preview(); controls();
                });
            } catch (Exception | OutOfMemoryError error) {
                activity.runOnUiThread(() -> {
                    if (!valid(request)) return;
                    processing = false; preview(); controls(); message("Could not prepare image. Choose another picture.");
                });
            }
        });
    }
    void submit() {
        if (!active || !ready || processing || submitting) return;
        if (FirebaseAuth.getInstance().getCurrentUser() == null) { message("Not authenticated"); return; }
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Map<String,Object> proposal = new HashMap<>();
        proposal.put("storyTitle", BirthWordBank.STORY_TITLE); proposal.put("quizType", 10);
        proposal.put("operation", "EDIT"); proposal.put("itemIndex", index);
        proposal.put("originalQuestion", bank.get(index).label); proposal.put("proposedQuestion", label.getText().toString().trim());
        proposal.put("answerWord", answer.getText().toString().trim().toUpperCase(Locale.ROOT));
        proposal.put("originalAnswerWord", bank.get(index).answer);
        proposal.put("imageAction", action);
        if ("REPLACE".equals(action)) proposal.put("imageBytes", Blob.fromBytes(imageBytes));
        Map<String,Object> validation = new HashMap<>(proposal);
        validation.put("approvedByAdmin", true); validation.put("question", proposal.get("proposedQuestion"));
        BirthContentBank.Item item = BirthContentBank.parse(validation);
        if (item == null || !BirthContentBank.unique(bank, item)) { message("Use a name of 1-80 characters and a unique answer of 3-10 English letters."); return; }
        proposal.put("teacherUid", uid); proposal.put("options", new ArrayList<String>());
        proposal.put("correctAnswerIndex", 0); proposal.put("status", "PENDING"); proposal.put("timestamp", Timestamp.now());
        final int request = version; submitting = true; controls();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(uid).get().continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();
            String name = task.getResult().getString("username");
            if (name == null || name.isEmpty()) name = task.getResult().getString("fullName");
            proposal.put("teacherName", name == null || name.isEmpty() ? "Teacher" : name);
            return db.collection("question_edit_proposals").add(proposal);
        }).addOnSuccessListener(ref -> {
            if (!valid(request)) return;
            message("Proposal submitted for admin approval."); activity.finish();
        }).addOnFailureListener(error -> {
            if (!valid(request)) return;
            submitting = false; controls(); message("Could not submit proposal. Please retry.");
        });
    }
    private void message(String text) { Toast.makeText(activity, text, Toast.LENGTH_LONG).show(); }
    private int dp(int value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }
}
