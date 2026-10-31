package com.example.arbibleapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class ArScan extends AppCompatActivity {

    private static Context appContext;
    private static WeakReference<Activity> unityActivity = new WeakReference<>(null);

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            launchUnityAR();
                        } else {
                            Toast.makeText(
                                    this,
                                    "Kailangan ng Camera Permission para sa AR",
                                    Toast.LENGTH_SHORT
                            ).show();
                            finish();
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        appContext = getApplicationContext();

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED) {

            launchUnityAR();

        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchUnityAR() {
        try {
            appContext = getApplicationContext();

            Intent intent = new Intent();
            intent.setClassName(
                    getPackageName(),
                    "com.unity3d.player.UnityPlayerGameActivity"
            );

            startActivity(intent);
            finish();

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Failed to start AR: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();

            Log.e("ArScan", "Failed to start Unity AR", e);
            finish();
        }
    }

    public static void attachUnityActivity(Activity activity) {
        unityActivity = new WeakReference<>(activity);
    }

    public static void detachUnityActivity(Activity activity) {
        Activity current = unityActivity.get();
        if (current == activity) unityActivity.clear();
    }

    // Unity's Done button opens the existing quiz flow, then closes AR.
    public static void finishARAndOpenQuiz() {
        new Handler(Looper.getMainLooper()).post(() -> {
            Activity unity = unityActivity.get();
            try {
                if (unity != null && !unity.isFinishing() && !unity.isDestroyed()) {
                    unity.startActivity(new Intent(unity, QuizAdamEve.class)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    unity.finish();
                } else if (appContext != null) {
                    appContext.startActivity(new Intent(appContext, QuizAdamEve.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
                }
            } catch (Exception e) {
                Log.e("ArScan", "Error opening quiz", e);
            }
        });
    }

    // Unity UI Back closes the AR activity; the existing dashboard is below it.
    public static void exitUnityAR() {
        new Handler(Looper.getMainLooper()).post(() -> {
            Activity unity = unityActivity.get();
            if (unity != null && !unity.isFinishing() && !unity.isDestroyed()) {
                unity.finish();
            } else if (appContext != null) {
                appContext.startActivity(new Intent(appContext, StudentDashboard.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            }
        });
    }

    public static boolean exitUnityAR(Activity activity) {
        if (activity == null || unityActivity.get() != activity
                || activity.isFinishing() || activity.isDestroyed()) return false;
        activity.finish();
        return true;
    }
}
