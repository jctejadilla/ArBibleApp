package com.example.arbibleapp;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class EmailVerificationModel extends AndroidViewModel {
    interface Backend {
        String uid(); String email(); Task<Void> send(); Task<Class<?>> check(); void signOut();
    }
    final MutableLiveData<Integer> changes = new MutableLiveData<>(0);
    private final Backend backend;
    private final SharedPreferences preferences;
    boolean busy, finished;
    Class<?> destination;
    String message = "Open the link in your email, then return here. Check your spam folder too.";
    public EmailVerificationModel(Application app) {
        this(app, new Backend() {
            public String uid() { FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser(); return u == null ? null : u.getUid(); }
            public String email() { FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser(); return u == null ? "" : u.getEmail(); }
            public Task<Void> send() { return FirebaseAuth.getInstance().getCurrentUser().sendEmailVerification(); }
            public Task<Class<?>> check() { return VerifiedAccountFlow.destination(app, FirebaseAuth.getInstance().getCurrentUser()); }
            public void signOut() { FirebaseAuth.getInstance().signOut(); }
        }, app.getSharedPreferences("email-verification", 0));
    }
    EmailVerificationModel(Application app, Backend backend, SharedPreferences preferences) {
        super(app); this.backend = backend; this.preferences = preferences;
    }
    String email() { return backend.email(); }
    long remainingSeconds() {
        long delay = preferences.getLong("sent-" + backend.uid(), 0) + 60000 - System.currentTimeMillis();
        return Math.max(0, Math.min(60, (delay + 999) / 1000));
    }
    void initialSend() {
        if (!preferences.getBoolean("initial-" + backend.uid(), false)) {
            preferences.edit().putBoolean("initial-" + backend.uid(), true).apply(); send();
        }
    }
    void send() {
        if (busy || finished || remainingSeconds() > 0) return;
        String uid = backend.uid();
        if (uid == null) { leave(); return; }
        busy = true; message = "Sending verification email?"; notifyChange();
        backend.send().addOnCompleteListener(task -> {
            if (finished) return;
            if (!uid.equals(backend.uid())) { leave(); return; }
            busy = false;
            if (task.isSuccessful()) {
                preferences.edit().putLong("sent-" + uid, System.currentTimeMillis()).apply();
                message = "Verification email sent. Open the link in your inbox or spam folder.";
            } else {
                if (task.getException() instanceof com.google.firebase.auth.FirebaseAuthInvalidUserException) { leave(); return; }
                message = "Could not send the email. Check your connection and tap Resend email to retry.";
            }
            notifyChange();
        });
    }
    void check() {
        if (busy || finished) return;
        String uid = backend.uid(); if (uid == null) { leave(); return; }
        busy = true; message = "Checking verification?"; notifyChange();
        backend.check().addOnCompleteListener(task -> {
            if (finished) return;
            if (!uid.equals(backend.uid())) { leave(); return; }
            busy = false;
            if (task.isSuccessful() && task.getResult() != EmailVerificationActivity.class) {
                finished = true; destination = task.getResult();
            } else if (task.isSuccessful()) message = "Your email is not verified yet. Open the email link, then try again.";
            else {
                Exception error = task.getException();
                if (error instanceof com.google.firebase.auth.FirebaseAuthInvalidUserException) { leave(); return; }
                message = error instanceof IllegalStateException ? error.getMessage()
                        : "Could not check verification. Check your connection and try again.";
            }
            notifyChange();
        });
    }
    void leave() { backend.signOut(); finished = true; busy = false; destination = login.class; notifyChange(); }
    private void notifyChange() { changes.setValue(changes.getValue() + 1); }
}
