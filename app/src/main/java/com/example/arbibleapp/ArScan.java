package com.example.arbibleapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;

import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.CameraConfig;
import com.google.ar.core.CameraConfigFilter;
import com.google.ar.core.Config;
import com.google.ar.core.Pose;
import com.google.ar.core.TrackingState;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import io.github.sceneview.ar.ArSceneView;
import io.github.sceneview.ar.node.ArModelNode;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import kotlin.Unit;

public class ArScan extends AppCompatActivity {
    private ArSceneView sceneView;
    private ArModelNode modelNode;
    private ExtendedFloatingActionButton btnProceed;
    
    private List<String> clipModels = new ArrayList<>(Arrays.asList("adam1.glb"));
    private int currentClipIndex = 0;
    
    private boolean isModelAnchored = false;
    private boolean isModelReady = false;
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Camera permission is required for AR", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_ar_scan);

        sceneView = findViewById(R.id.sceneView);
        btnProceed = findViewById(R.id.btnProceed);

        btnProceed.setOnClickListener(v -> {
            Toast.makeText(this, "Proceeding to Quiz...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ArScan.this, QuizAdamEve.class);
            startActivity(intent);
            finish();
        });

        try {
            for (String model : clipModels) {
                InputStream is = getAssets().open(model);
                is.close();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Some model files NOT found in assets!", Toast.LENGTH_LONG).show();
        }

        checkCameraPermission();

        updateButtonConstraints(getResources().getConfiguration().orientation);

        getLifecycle().addObserver(sceneView);

        modelNode = new ArModelNode(sceneView.getEngine());
        modelNode.setVisible(false);
        sceneView.addChild(modelNode);

        loadClip(0);

        sceneView.configureSession((session, config) -> {
            config.setFocusMode(Config.FocusMode.AUTO);
            config.setDepthMode(Config.DepthMode.DISABLED);

            AugmentedImageDatabase database = new AugmentedImageDatabase(session);
            try (InputStream is = getAssets().open("marker1.jpg")) {
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                database.addImage("marker1", bitmap, 0.2f);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error loading marker", Toast.LENGTH_SHORT).show();
            }
            config.setAugmentedImageDatabase(database);

            return Unit.INSTANCE;
        });

        sceneView.setOnArSessionCreated(session -> {
            CameraConfigFilter filter = new CameraConfigFilter(session);
            filter.setTargetFps(EnumSet.of(CameraConfig.TargetFps.TARGET_FPS_30));
            List<CameraConfig> configs = session.getSupportedCameraConfigs(filter);
            if (!configs.isEmpty()) {
                session.setCameraConfig(configs.get(0));
            }
            return Unit.INSTANCE;
        });
        
        sceneView.getOnAugmentedImageUpdate().add(augmentedImage -> {
            if (augmentedImage.getTrackingState() == TrackingState.TRACKING && !isModelAnchored && isModelReady) {
                Pose liftPose = augmentedImage.getCenterPose().compose(Pose.makeTranslation(0, 0.1f, 0));
                modelNode.setAnchor(augmentedImage.createAnchor(liftPose));
                modelNode.setVisible(true);
                isModelAnchored = true;
                updateButtonsVisibility();
                Toast.makeText(this, "Marker Found - Model Placed!", Toast.LENGTH_SHORT).show();
            }
            return Unit.INSTANCE;
        });
    }

    private void loadClip(int index) {
        if (index >= clipModels.size()) return;

        isModelReady = false;
        btnProceed.setVisibility(View.GONE);

        String modelFile = clipModels.get(index);

        modelNode.loadModelGlbAsync(
                modelFile,
                true,
                1.5f,
                null,
                ex -> {
                    Toast.makeText(this, "Load Error: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                    return Unit.INSTANCE;
                },
                instance -> {
                    isModelReady = true;
                    if (isModelAnchored) {
                        modelNode.setVisible(true);
                        updateButtonsVisibility();
                    }

                    modelNode.playAnimation(0, false);
                    
                    final long delayMillis = (instance.getAnimator().getAnimationCount() > 0)
                            ? (long) (instance.getAnimator().getAnimationDuration(0) * 1000)
                            : 10000;

                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        if (currentClipIndex < clipModels.size() - 1) {
                            currentClipIndex++;
                            loadClip(currentClipIndex);
                        } else {
                            updateButtonsVisibility();
                        }
                    }, delayMillis);

                    return Unit.INSTANCE;
                }
        );
    }

    private void updateButtonsVisibility() {
        if (currentClipIndex >= clipModels.size() - 1) {
            btnProceed.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateButtonConstraints(newConfig.orientation);
    }

    private void updateButtonConstraints(int orientation) {
        ConstraintLayout layout = findViewById(R.id.main_layout);
        if (layout == null) return;

        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(layout);

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            constraintSet.connect(R.id.btnProceed, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 64);
            constraintSet.connect(R.id.btnProceed, ConstraintSet.START, ConstraintSet.GONE, ConstraintSet.START, 0);
        } else {
            constraintSet.connect(R.id.btnProceed, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0);
            constraintSet.connect(R.id.btnProceed, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0);
        }
        constraintSet.applyTo(layout);
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }
}
