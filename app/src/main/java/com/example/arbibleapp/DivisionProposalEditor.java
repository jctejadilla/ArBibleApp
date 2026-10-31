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

final class DivisionProposalEditor {
    private final AppCompatActivity activity;
    private final Spinner selector;
    private final EditText characterName, objectName;
    private final LinearLayout container;
    private final Button submit;
    private final ImageView[] previews = new ImageView[2];
    private final TextView[] statuses = new TextView[2];
    private final Button[] uploads = new Button[2], originals = new Button[2], keeps = new Button[2];
    private final String[] actions = {"KEEP", "KEEP"};
    private final byte[][] images = new byte[2][];
    private final ActivityResultLauncher<String> picker;
    private List<DivisionContentBank.Pair> bank;
    private int version, pickerVersion, pickedRole, index;
    private boolean active, ready, processing, submitting;
    DivisionProposalEditor(AppCompatActivity activity, LinearLayout form, Spinner selector, EditText characterName, Button submit) {
        this.activity = activity; this.selector = selector; this.characterName = characterName; this.submit = submit;
        container = new LinearLayout(activity); container.setOrientation(LinearLayout.VERTICAL);
        form.addView(container, form.indexOfChild(submit));
        objectName = new EditText(activity); objectName.setHint("Matching object name (max 80 characters)"); 
        objectName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        objectName.setTextColor(ContextCompat.getColor(activity, R.color.primary_green));
        objectName.setHintTextColor(Color.parseColor("#882E7D32"));
        objectName.setTextSize(14f);
        int pad = dp(16);
        objectName.setPadding(pad, pad, pad, pad);
        objectName.setMinHeight(dp(52));
        objectName.setBackgroundResource(R.drawable.bg_outlined_edittext);
        objectName.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(-1, -2);
        alp.topMargin = dp(8);
        container.addView(objectName, alp);
        picker = activity.registerForActivityResult(new ActivityResultContracts.GetContent(), this::picked);
        for (int role = 0; role < 2; role++) {
            final int target = role;
            TextView label = new TextView(activity); label.setText(role == 0 ? "Character picture" : "Object picture"); 
            label.setTextColor(ContextCompat.getColor(activity, R.color.text_dark));
            label.setTypeface(null, Typeface.BOLD);
            label.setTextSize(14f);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(-1, -2);
            tlp.topMargin = dp(16);
            container.addView(label, tlp);
            
            previews[role] = new ImageView(activity); previews[role].setScaleType(ImageView.ScaleType.FIT_CENTER);
            LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(-1, dp(160));
            plp.topMargin = dp(8);
            container.addView(previews[role], plp);
            
            statuses[role] = new TextView(activity); 
            statuses[role].setTextColor(ContextCompat.getColor(activity, R.color.text_dark));
            container.addView(statuses[role]);
            
            LinearLayout buttons = new LinearLayout(activity); 
            buttons.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, -2);
            blp.topMargin = dp(8);
            blp.bottomMargin = dp(16);
            container.addView(buttons, blp);
            uploads[role] = button(buttons, "Upload", () -> {
                if (!ready || processing || submitting) return;
                pickerVersion = version; pickedRole = target; picker.launch("image/*");
            });
            originals[role] = button(buttons, "Original", () -> imageAction(target, "DEFAULT"));
            keeps[role] = button(buttons, "Keep current", () -> imageAction(target, "KEEP"));
        }
        container.setVisibility(View.GONE);
    }
    private Button button(LinearLayout row, String label, Runnable action) {
        Button b = new Button(activity);
        b.setText(label); b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(activity, R.color.primary_green)));
        b.setBackgroundResource(R.drawable.bg_button_next);
        row.addView(b, new LinearLayout.LayoutParams(0, dp(48), 1)); 
        ((LinearLayout.LayoutParams)b.getLayoutParams()).setMargins(dp(4), 0, dp(4), 0);
        b.setOnClickListener(v -> action.run()); return b;
    }
    void deactivate() {
        active = false; ready = false; version++; container.setVisibility(View.GONE);
        selector.setEnabled(true); characterName.setEnabled(true);
        for (ImageView preview : previews) preview.setTag(null);
    }
    void load() {
        active = true; ready = false; processing = false; submitting = false; version++;
        final int request = version;
        container.setVisibility(View.VISIBLE); selector.setOnItemSelectedListener(null);
        List<String> labels = new ArrayList<>();
        for (DivisionContentBank.Pair pair : DivisionContentBank.defaults()) labels.add(label(pair));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, R.layout.spinner_item_dark, labels);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_dark); selector.setAdapter(adapter);
        characterName.setText(""); objectName.setText("");
        for (int role = 0; role < 2; role++) { previews[role].setImageDrawable(null); statuses[role].setText("Loading current picture..."); }
        controls();
        FirebaseFirestore.getInstance().collection("quiz_questions")
                .whereEqualTo("storyTitle", DivisionPairBank.STORY_TITLE).whereEqualTo("approvedByAdmin", true)
                .get().addOnSuccessListener(result -> {
                    if (!valid(request)) return;
                    bank = DivisionContentBank.load(result.getDocuments());
                    for (int i = 0; i < 9; i++) labels.set(i, label(bank.get(i)));
                    adapter.notifyDataSetChanged(); ready = true;
                    selector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override public void onItemSelected(AdapterView<?> p, View v, int position, long id) { if (active && ready) select(position); }
                        @Override public void onNothingSelected(AdapterView<?> p) { }
                    });
                    select(Math.max(0, selector.getSelectedItemPosition()));
                }).addOnFailureListener(error -> {
                    if (valid(request)) message("Could not load current pairs. Select Division again to retry.");
                });
    }
    private String label(DivisionContentBank.Pair pair) { return "Round " + (pair.index / 3 + 1) + ": " + pair.characterName + " - " + pair.objectName; }
    private boolean valid(int request) { return active && request == version && !activity.isFinishing() && !activity.isDestroyed(); }
    private void select(int selected) {
        index = selected; version++; processing = false;
        characterName.setText(bank.get(index).characterName); objectName.setText(bank.get(index).objectName);
        for (int role = 0; role < 2; role++) { actions[role] = "KEEP"; images[role] = null; preview(role); }
        controls();
    }
    private void imageAction(int role, String action) {
        if (!active || !ready || submitting) return;
        version++; processing = false; actions[role] = action; images[role] = null;
        preview(0); preview(1); controls();
    }
    private void preview(int role) {
        byte[] bytes = "REPLACE".equals(actions[role]) ? images[role]
                : "KEEP".equals(actions[role]) ? bank.get(index).images[role] : null;
        LawImageCodec.display(previews[role], bytes, DivisionImages.resource(index, role));
        statuses[role].setText("REPLACE".equals(actions[role]) ? "Uploaded replacement"
                : "DEFAULT".equals(actions[role]) ? "Original picture" : "Current approved picture");
    }
    private void controls() {
        submit.setEnabled(active && ready && !processing && !submitting);
        selector.setEnabled(!submitting); characterName.setEnabled(!submitting); objectName.setEnabled(!submitting);
        for (int role = 0; role < 2; role++) {
            uploads[role].setEnabled(ready && !processing && !submitting);
            originals[role].setEnabled(ready && !submitting); keeps[role].setEnabled(ready && !submitting);
        }
    }
    private void picked(Uri uri) {
        if (uri == null || !valid(pickerVersion) || !ready) return;
        final int request = version, role = pickedRole;
        processing = true; statuses[role].setText("Preparing image..."); controls();
        LawImageCodec.WORKER.execute(() -> {
            try {
                byte[] bytes = LawImageCodec.prepare(activity.getContentResolver(), uri);
                activity.runOnUiThread(() -> {
                    if (!valid(request)) return;
                    processing = false; actions[role] = "REPLACE"; images[role] = bytes; preview(role); controls();
                });
            } catch (Exception | OutOfMemoryError error) {
                activity.runOnUiThread(() -> {
                    if (!valid(request)) return;
                    processing = false; preview(role); controls(); message("Could not prepare image. Choose another picture.");
                });
            }
        });
    }
    void submit() {
        if (!active || !ready || processing || submitting) return;
        if (FirebaseAuth.getInstance().getCurrentUser() == null) { message("Not authenticated"); return; }
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Map<String,Object> proposal = new HashMap<>();
        proposal.put("storyTitle", DivisionPairBank.STORY_TITLE); proposal.put("quizType", 8);
        proposal.put("operation", "EDIT"); proposal.put("itemIndex", index);
        proposal.put("originalQuestion", label(bank.get(index)));
        proposal.put("proposedQuestion", characterName.getText().toString().trim());
        proposal.put("objectName", objectName.getText().toString().trim());
        for (int role = 0; role < 2; role++) {
            proposal.put("image" + (role + 1) + "Action", actions[role]);
            if ("REPLACE".equals(actions[role])) proposal.put("image" + (role + 1) + "Bytes", Blob.fromBytes(images[role]));
        }
        Map<String,Object> validation = new HashMap<>(proposal);
        validation.put("approvedByAdmin", true); validation.put("question", proposal.get("proposedQuestion"));
        DivisionContentBank.Pair pair = DivisionContentBank.parse(validation);
        if (pair == null || !DivisionContentBank.unique(bank, pair)) {
            message("Use names of 1-80 characters with no duplicate character or object names in this round."); return;
        }
        proposal.put("teacherUid", uid); proposal.put("options", new ArrayList<String>());
        proposal.put("correctAnswerIndex", 0); proposal.put("status", "PENDING"); proposal.put("timestamp", Timestamp.now());
        final int request = version;
        submitting = true; controls();
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
