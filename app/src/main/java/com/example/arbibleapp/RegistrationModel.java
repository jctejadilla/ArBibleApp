package com.example.arbibleapp;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.json.JSONObject;

public class RegistrationModel extends AndroidViewModel {
    final MutableLiveData<Integer> changes = new MutableLiveData<>(0);
    boolean busy, complete;
    String error;
    String existingEmail;
    interface Backend {
        String uid();
        Task<String> create(String email, String password);
        Task<Void> save(String uid, Map<String,Object> data);
    }
    private final Backend backend;
    private final SharedPreferences pending;
    public RegistrationModel(Application app) {
        this(app, new Backend() {
            public String uid() { FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser(); return u == null ? null : u.getUid(); }
            public Task<String> create(String email, String password) {
                return FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).continueWith(task -> {
                    if (!task.isSuccessful()) throw task.getException(); return task.getResult().getUser().getUid();
                });
            }
            public Task<Void> save(String uid, Map<String,Object> data) {
                DocumentReference ref = FirebaseFirestore.getInstance().collection("users").document(uid);
                return FirebaseFirestore.getInstance().runTransaction(transaction -> {
                    VerifiedAccountFlow.requireUser(uid);
                    if (!transaction.get(ref).exists()) transaction.set(ref, data);
                    return null;
                });
            }
        }, createEncryptedPrefs(app));
    }
    
    private static SharedPreferences createEncryptedPrefs(Application app) {
        try {
            MasterKey masterKey = new MasterKey.Builder(app)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            return EncryptedSharedPreferences.create(
                    app,
                    "pending-registration-secure",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            return app.getSharedPreferences("pending-registration", 0);
        }
    }
    
    RegistrationModel(Application app, Backend backend, SharedPreferences pending) {
        super(app); this.backend = backend; this.pending = pending;
    }
    boolean hasPending() {
        return backend.uid() != null && pending.contains(backend.uid());
    }
    void register(String name, String email, String password, String gender, String role) {
        if (busy || complete) return;
        if (!isRegistrableRole(role)) { fail("Choose Student or Teacher to register."); return; }
        if (hasPending()) { saveProfile(); return; }
        busy = true; error = null; existingEmail = null; changed();
        backend.create(email, password).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                    busy = false; existingEmail = email; changed();
                } else fail("Could not create account: " + task.getException().getMessage());
                return;
            }
            String uid = task.getResult();
            Map<String,Object> data = new HashMap<>();
            data.put("uid", uid); data.put("username", name); data.put("email", email);
            data.put("gender", gender); data.put("userType", role); data.put("totalXP", 0); data.put("level", 1);
            pending.edit().putString(uid, new JSONObject(data).toString()).apply();
            saveProfile();
        });
    }
    void retry() { if (!busy && hasPending()) saveProfile(); }
    private void saveProfile() {
        String uid = backend.uid();
        if (uid == null) { fail("Please sign in again."); return; }
        Map<String,Object> data = new HashMap<>();
        try {
            JSONObject json = new JSONObject(pending.getString(uid, ""));
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) { String key = keys.next(); data.put(key, json.get(key)); }
        } catch (Exception e) { fail("Registration details are unavailable. Please contact your administrator."); return; }
        if (!isRegistrableRole(data.get("userType") instanceof String ? (String) data.get("userType") : null)) {
            fail("This pending registration cannot create an Admin account. Sign in with your existing Admin account.");
            return;
        }
        busy = true; error = null; changed();
        backend.save(uid, data).addOnCompleteListener(task -> {
            busy = false;
            if (!task.isSuccessful()) { fail("Account created, but profile setup failed. Tap Retry profile setup."); return; }
            if (!uid.equals(backend.uid())) { fail("Please sign in again."); return; }
            pending.edit().remove(uid).apply(); complete = true; changed();
        });
    }
    private static boolean isRegistrableRole(String role) {
        return "Student".equals(role) || "Teacher".equals(role);
    }
    private void fail(String message) { busy = false; error = message; changed(); }
    private void changed() { changes.setValue(changes.getValue() + 1); }
}
