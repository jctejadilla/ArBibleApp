package com.example.arbibleapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class ArScan extends AppCompatActivity {

    private static Context appContext;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    launchUnityAR();
                } else {
                    Toast.makeText(this, "Kailangan ng Camera Permission para sa AR", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appContext = getApplicationContext();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchUnityAR();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchUnityAR() {
        try {
            appContext = getApplicationContext();
            Intent intent = new Intent();
            intent.setClassName(getPackageName(), "com.unity3d.player.UnityPlayerGameActivity");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to start AR: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    // 1. DONE BUTTON -> PUMUNTA SA QUIZ
    public static void finishARAndOpenQuiz() {
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                if (appContext != null) {
                    Intent quizIntent = new Intent(appContext, QuizAdamEve.class);
                    quizIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    appContext.startActivity(quizIntent);
                }
            } catch (Exception e) {
                Log.e("ArScan", "Error opening quiz: " + e.getMessage());
            }
        });
    }

    // 2. BACK BUTTON -> BUMALIK SA DASHBOARD
    // BACK BUTTON: Gamitin ang tamang Android system back trigger o unload
    public static void exitUnityAR() {
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                if (unityActivityRef != null) {
                    // I-trigger ang native back press ng mismong Unity Activity
                    unityActivityRef.onBackPressed();
                } else if (appContext != null) {
                    Intent mainIntent = new Intent(appContext, StudentDashboard.class);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    appContext.startActivity(mainIntent);
                }
            } catch (Exception e) {
                Log.e("ArScan", "Error exiting AR: " + e.getMessage());
            }
        });
    }
}