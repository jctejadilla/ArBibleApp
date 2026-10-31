package com.example.arbibleapp;

import android.app.Activity;
import android.content.Intent;
import android.provider.Settings;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

final class VerifiedAccountFlow {
    static final String EXTRA_EMAIL = "account_email";
    static void open(Activity activity, Class<?> destination) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        activity.startActivity(new Intent(activity, destination)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        activity.finish();
    }
    static boolean guard(Activity activity) {
        if (activity.isFinishing()) return false;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { open(activity, login.class); return false; }
        if (!user.isEmailVerified()) { open(activity, EmailVerificationActivity.class); return false; }
        return true;
    }
    static Class<?> dashboard(String role) {
        if ("Admin".equals(role)) return AdminDashboard.class;
        if ("Teacher".equals(role)) return TeacherDashboard.class;
        if ("Student".equals(role)) return StudentDashboard.class;
        throw new IllegalStateException("Your account profile has no valid role. Please contact your administrator.");
    }
    static Class<?> profileDestination(boolean exists, boolean verified, boolean pending, String role) {
        if (!exists) {
            if (pending) return Register.class;
            throw new IllegalStateException("Your account profile is missing. Please contact your administrator to finish setup.");
        }
        return verified ? dashboard(role) : EmailVerificationActivity.class;
    }
    static Task<Class<?>> destination(android.content.Context context, FirebaseUser user) {
        if (user == null) return Tasks.forException(new IllegalStateException("Please sign in again."));
        String uid = user.getUid();
        return user.reload().continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();
            requireUser(uid);
            return FirebaseFirestore.getInstance().collection("users").document(uid).get();
        }).continueWithTask(profile -> {
            if (!profile.isSuccessful()) throw profile.getException();
            requireUser(uid);
            boolean pending = context.getSharedPreferences("pending-registration", 0).contains(uid);
            Class<?> destination = profileDestination(profile.getResult().exists(), user.isEmailVerified(),
                    pending, profile.getResult().getString("userType"));
            if (destination == Register.class || destination == EmailVerificationActivity.class)
                return Tasks.forResult(destination);
            return user.getIdToken(true).continueWithTask(token -> {
                if (!token.isSuccessful()) throw token.getException();
                requireUser(uid);
                String device = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
                if (device == null) return Tasks.forResult(destination);
                return FirebaseFirestore.getInstance().collection("users").document(uid)
                        .update("activeDeviceId", device).continueWith(update -> {
                            if (!update.isSuccessful()) throw update.getException();
                            requireUser(uid); return destination;
                        });
            });
        });
    }
    static void requireUser(String uid) {
        FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
        if (current == null || !uid.equals(current.getUid())) throw new IllegalStateException("Please sign in again.");
    }
}
