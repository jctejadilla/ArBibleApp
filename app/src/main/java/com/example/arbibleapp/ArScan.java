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
import java.util.Collections;
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
    
    private String currentMarkerName = null;
    private int currentClipIndex = 0;
    private int currentSessionId = 0;

    private final List<String> adamClips = Arrays.asList("adam1.glb");
    private final List<String> floodClips = Arrays.asList("babel1.glb");
    private final List<String> babelClips = Arrays.asList("babel1.glb");

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
            Class<?> targetQuiz;
            if ("markerFlood".equals(currentMarkerName)) {
                targetQuiz = QuizFlood.class;
            } else if ("markerBabel".equals(currentMarkerName)) {
                targetQuiz = QuizBabel.class;
            } else {
                targetQuiz = QuizAdamEve.class;
            }
            Toast.makeText(this, "Proceeding to Quiz...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ArScan.this, targetQuiz);
            startActivity(intent);
            finish();
        });

        checkCameraPermission();

        updateButtonConstraints(getResources().getConfiguration().orientation);

        getLifecycle().addObserver(sceneView);

        modelNode = new ArModelNode(sceneView.getEngine());
        modelNode.setVisible(false);
        sceneView.addChild(modelNode);

        sceneView.configureSession((session, config) -> {
            config.setFocusMode(Config.FocusMode.AUTO);
            config.setDepthMode(Config.DepthMode.DISABLED);

            AugmentedImageDatabase database = new AugmentedImageDatabase(session);
            addMarkerToDatabase(database, "markerAdam", "markerAdam.jpg");
            addMarkerToDatabase(database, "markerFlood", "markerFlood.png");
            addMarkerToDatabase(database, "markerBabel", "marketBabel.png");
            
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
            String name = augmentedImage.getName();
            if (augmentedImage.getTrackingState() == TrackingState.TRACKING) {
                if (currentMarkerName == null || !currentMarkerName.equals(name)) {
                    currentMarkerName = name;
                    currentClipIndex = 0;
                    currentSessionId++;
                    
                    sceneView.removeChild(modelNode);
                    modelNode = new ArModelNode(sceneView.getEngine());
                    modelNode.setVisible(false);
                    sceneView.addChild(modelNode);

                    loadClip(0, augmentedImage, currentSessionId);
                } else {
                    modelNode.setVisible(true);
                }
            } else if (name.equals(currentMarkerName)) {
                modelNode.setVisible(false);
            }
            return Unit.INSTANCE;
        });
    }

    private void addMarkerToDatabase(AugmentedImageDatabase database, String name, String fileName) {
        try (InputStream is = getAssets().open(fileName)) {
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            database.addImage(name, bitmap, 0.2f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<String> getCurrentClips() {
        if ("markerAdam".equals(currentMarkerName)) return adamClips;
        if ("markerFlood".equals(currentMarkerName)) return floodClips;
        if ("markerBabel".equals(currentMarkerName)) return babelClips;
        return new ArrayList<>();
    }
    
    private void loadClip(int index, AugmentedImage augmentedImage, int sessionId) {
        if (sessionId != currentSessionId) return;

        List<String> clips = getCurrentClips();
        if (index >= clips.size()) {
            btnProceed.setVisibility(View.VISIBLE);
            return;
        }

        String modelFile = clips.get(index);
        
        if (index == 0) {
            btnProceed.setVisibility(View.GONE);
            modelNode.setVisible(false);
        }

        modelNode.loadModelGlbAsync(
                modelFile,
                true,
                0.30f,
                null,
                ex -> {
                    if (sessionId == currentSessionId) {
                        Toast.makeText(this, "Load Error: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    return Unit.INSTANCE;
                },
                instance -> {
                    if (sessionId != currentSessionId) return Unit.INSTANCE;

                    if (augmentedImage != null) {
                        Pose centerPose = augmentedImage.getCenterPose();
                        modelNode.setAnchor(augmentedImage.createAnchor(centerPose));
                    }
                    
                    modelNode.setVisible(true);
                    modelNode.playAnimation(0, false);
                    
                    if (index == 0) {
                        Toast.makeText(this, "Story Found!", Toast.LENGTH_SHORT).show();
                    }

                    long animTime = (instance.getAnimator().getAnimationCount() > 0)
                            ? (long) (instance.getAnimator().getAnimationDuration(0) * 1000)
                            : 0;
                    final long finalDuration = Math.max(animTime, 20000);

                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        if (sessionId == currentSessionId && currentClipIndex == index) {
                            currentClipIndex++;
                            loadClip(currentClipIndex, null, sessionId);
                        }
                    }, finalDuration);

                    return Unit.INSTANCE;
                }
        );
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
