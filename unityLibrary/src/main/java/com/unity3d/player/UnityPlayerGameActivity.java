package com.unity3d.player;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import androidx.core.view.ViewCompat;

import com.google.androidgamesdk.GameActivity;
import android.app.Activity;
import android.util.Log;

public class UnityPlayerGameActivity extends GameActivity implements IUnityPlayerLifecycleEvents, IUnityPermissionRequestSupport, IUnityPlayerSupport
{
    class GameActivitySurfaceView extends InputEnabledSurfaceView
    {
        GameActivity mGameActivity;
        public GameActivitySurfaceView(GameActivity activity) {
            super(activity);
            mGameActivity = activity;
        }

        // Reroute motion events from captured pointer to normal events
        // Otherwise when doing Cursor.lockState = CursorLockMode.Locked from C# the touch and mouse events will stop working
        @Override public boolean onCapturedPointerEvent(MotionEvent event) {
            return mGameActivity.onTouchEvent(event);
        }
    }

    protected UnityPlayerForGameActivity mUnityPlayer;
    private OnBackInvokedCallback backInvokedCallback;
    protected String updateUnityCommandLineArguments(String cmdLine)
    {
        return cmdLine;
    }

    static
    {
        System.loadLibrary("game");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            backInvokedCallback = this::handleBackPressed;
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT + 2, backInvokedCallback);
        }
        notifyArScan("attachUnityActivity", new Class<?>[]{Activity.class}, this);
    }

    @Override
    public UnityPlayerForGameActivity getUnityPlayerConnection() {
        return mUnityPlayer;
    }

    // Soft keyboard relies on inset listener for listening to various events - keyboard opened/closed/text entered.
    private void applyInsetListener(SurfaceView surfaceView)
    {
        surfaceView.getViewTreeObserver().addOnGlobalLayoutListener(
                () -> onApplyWindowInsets(surfaceView, ViewCompat.getRootWindowInsets(getWindow().getDecorView())));
    }

    @Override protected InputEnabledSurfaceView createSurfaceView() {
        return new GameActivitySurfaceView(this);
    }

    @Override protected void onCreateSurfaceView() {
        super.onCreateSurfaceView();
        FrameLayout frameLayout = findViewById(contentViewId);

        // The workaround is not needed for Android 11 or above starting with GameActivity 4.3.0-alpha01
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q)
            applyInsetListener(mSurfaceView);

        mSurfaceView.setId(UnityPlayerForGameActivity.getUnityViewIdentifier(this));

        String cmdLine = updateUnityCommandLineArguments(getIntent().getStringExtra("unity"));
        getIntent().putExtra("unity", cmdLine);
        // Unity requires access to frame layout for setting the static splash screen.
        // Note: we cannot initialize in onCreate (after super.onCreate), because game activity native thread would be already started and unity runtime initialized
        //       we also cannot initialize before super.onCreate since frameLayout is not yet available.
        mUnityPlayer = new UnityPlayerForGameActivity(this, frameLayout, mSurfaceView, this);
    }

    @Override
    public void onUnityPlayerUnloaded() {
        moveTaskToBack(true);
    }

    @Override
    public void onUnityPlayerQuitted() {
    }

    // Route both legacy and predictive Android Back through the app bridge.
    @Override public void onBackPressed()
    {
        handleBackPressed();
    }

    private void handleBackPressed() {
        if (!exitThroughArScan()) finish();
    }

    @Override protected void onDestroy ()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && backInvokedCallback != null) {
            getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backInvokedCallback);
            backInvokedCallback = null;
        }
        notifyArScan("detachUnityActivity", new Class<?>[]{Activity.class}, this);
        if (mUnityPlayer != null) mUnityPlayer.destroy();
        super.onDestroy();
    }

    @Override protected void onStop()
    {
        // Note: we want Java onStop callbacks to be processed before the native part processes the onStop callback
        if (mUnityPlayer != null) mUnityPlayer.onStop();
        super.onStop();
    }

    @Override protected void onStart()
    {
        // Note: we want Java onStart callbacks to be processed before the native part processes the onStart callback
        if (mUnityPlayer != null) mUnityPlayer.onStart();
        super.onStart();
    }

    // Pause Unity
    @Override protected void onPause()
    {
        // Note: we want Java onPause callbacks to be processed before the native part processes the onPause callback
        if (mUnityPlayer != null) mUnityPlayer.onPause();
        super.onPause();
    }

    // Resume Unity
    @Override protected void onResume()
    {
        // Note: we want Java onResume callbacks to be processed before the native part processes the onResume callback
        if (mUnityPlayer != null) mUnityPlayer.onResume();
        super.onResume();
    }

    // Configuration changes are used by Video playback logic in Unity
    @Override public void onConfigurationChanged(Configuration newConfig)
    {
        if (mUnityPlayer != null) mUnityPlayer.configurationChanged(newConfig);
        super.onConfigurationChanged(newConfig);
    }

    // Notify Unity of the focus change.
    @Override public void onWindowFocusChanged(boolean hasFocus)
    {
        if (mUnityPlayer != null) mUnityPlayer.windowFocusChanged(hasFocus);
        super.onWindowFocusChanged(hasFocus);
    }

    @Override protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        // To support deep linking, we need to make sure that the client can get access to
        // the last sent intent. The clients access this through a JNI api that allows them
        // to get the intent set on launch. To update that after launch we have to manually
        // replace the intent with the one caught here.
        setIntent(intent);
        if (mUnityPlayer != null) mUnityPlayer.newIntent(intent);
    }

    private boolean exitThroughArScan() {
        try {
            Class<?> bridge = Class.forName("com.example.arbibleapp.ArScan");
            bridge.getMethod("exitUnityAR").invoke(null);
            return true;
        } catch (ReflectiveOperationException error) {
            Log.w("UnityPlayerGameActivity", "AR exit bridge unavailable", error);
            return false;
        }
    }

    private void notifyArScan(String method, Class<?>[] parameterTypes, Object... arguments) {
        try {
            Class.forName("com.example.arbibleapp.ArScan").getMethod(method, parameterTypes)
                    .invoke(null, arguments);
        } catch (ReflectiveOperationException error) {
            Log.w("UnityPlayerGameActivity", "AR lifecycle bridge unavailable: " + method, error);
        }
    }

    @Override
    public void requestPermissions(PermissionRequest request)
    {
        if (mUnityPlayer != null) mUnityPlayer.addPermissionRequest(request);
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (mUnityPlayer != null) mUnityPlayer.permissionResponse(this, requestCode, permissions, grantResults);
    }

    @Override public void onMultiWindowModeChanged(boolean isInMultiWindowMode, Configuration newConfig)
    {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig);
        if (mUnityPlayer != null) mUnityPlayer.onMultiWindowModeChanged(isInMultiWindowMode, newConfig);
    }

}
