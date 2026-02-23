package com.example.arbibleapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.assets.RenderableSource;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.Scene;

import java.util.Collection;

public class ArScan extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 1001;
    private ArSceneView arSceneView;
    private ModelRenderable modelRenderable;
    private boolean modelPlaced = false;
    private boolean isSessionSetup = false;
    private boolean installRequested = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ar_scan);

        arSceneView = findViewById(R.id.arSceneView);

        if (checkCameraPermission()) {
            setupSession();
        } else {
            requestCameraPermission();
        }
        
        loadModel();

        Scene scene = arSceneView.getScene();
        scene.addOnUpdateListener(frameTime -> {
            Frame frame = arSceneView.getArFrame();
            if (frame == null) return;

            Collection<AugmentedImage> images = frame.getUpdatedTrackables(AugmentedImage.class);

            for (AugmentedImage image : images) {
                if (image.getTrackingState() == TrackingState.TRACKING) {
                    if (!modelPlaced && modelRenderable != null) {
                        Anchor anchor = image.createAnchor(image.getCenterPose());
                        placeModel(anchor, image);
                        modelPlaced = true;
                        Toast.makeText(this, "Marker detected!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupSession();
                resumeArSceneView();
            } else {
                Toast.makeText(this, "Camera permission is required for AR", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void setupSession() {
        if (isSessionSetup) return;

        try {
            switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                case INSTALL_REQUESTED:
                    installRequested = true;
                    return;
                case INSTALLED:
                    break;
            }

            Session session = new Session(this);
            Config config = new Config(session);
            config.setFocusMode(Config.FocusMode.AUTO);
            config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);

            AugmentedImageDatabase db = new AugmentedImageDatabase(session);
            
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.marker1);
            if (bitmap != null) {
                db.addImage("marker1", bitmap);
            } else {
                Log.e("ArScan", "Could not load marker1 resource");
            }

            config.setAugmentedImageDatabase(db);
            session.configure(config);
            arSceneView.setupSession(session);
            isSessionSetup = true;
        } catch (UnavailableArcoreNotInstalledException | UnavailableUserDeclinedInstallationException e) {
            Toast.makeText(this, "Please install ARCore", Toast.LENGTH_LONG).show();
            finish();
        } catch (UnavailableApkTooOldException e) {
            Toast.makeText(this, "Please update ARCore", Toast.LENGTH_LONG).show();
            finish();
        } catch (UnavailableSdkTooOldException e) {
            Toast.makeText(this, "Please update this app", Toast.LENGTH_LONG).show();
            finish();
        } catch (UnavailableDeviceNotCompatibleException e) {
            Toast.makeText(this, "This device does not support AR", Toast.LENGTH_LONG).show();
            finish();
        } catch (Exception e) {
            Log.e("ArScan", "Failed to setup AR session", e);
            finish();
        }
    }

    private void loadModel() {
        ModelRenderable.builder()
                .setSource(this, RenderableSource.builder()
                        .setSource(this, Uri.parse("model.glb"), RenderableSource.SourceType.GLB)
                        .setRecenterMode(RenderableSource.RecenterMode.CENTER)
                        .build())
                .setRegistryId("model.glb")
                .build()
                .thenAccept(renderable -> {
                    modelRenderable = renderable;
                    Log.d("ArScan", "Model loaded successfully");
                })
                .exceptionally(throwable -> {
                    Log.e("ArScan", "Error loading model: " + throwable.getMessage());
                    return null;
                });
    }

    private void placeModel(Anchor anchor, AugmentedImage image) {
        Log.d("ArScan", "Placing model on marker: " + image.getName());
        
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(arSceneView.getScene());

        Node modelNode = new Node();
        modelNode.setParent(anchorNode);
        modelNode.setRenderable(modelRenderable);

        modelNode.setLocalPosition(new Vector3(0.0f, 0.05f, 0.0f));
        modelNode.setLocalScale(new Vector3(0.07f, 0.07f, 0.07f));
        
        Log.d("ArScan", "Model node attached to scene at: " + anchor.getPose().toString());
    }

    private void resumeArSceneView() {
        if (arSceneView != null && isSessionSetup) {
            try {
                arSceneView.resume();
            } catch (Exception e) {
                Log.e("ArScan", "Error resuming ArSceneView", e);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkCameraPermission()) {
            setupSession();
            resumeArSceneView();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (arSceneView != null) {
            arSceneView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (arSceneView != null) {
            arSceneView.destroy();
        }
    }
}
