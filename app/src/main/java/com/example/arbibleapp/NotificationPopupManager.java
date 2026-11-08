package com.example.arbibleapp;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class NotificationPopupManager implements Application.ActivityLifecycleCallbacks {

    private static NotificationPopupManager instance;

    private WeakReference<Activity> currentActivityRef;
    private final Map<Activity, View> attachedPopupViews = new HashMap<>();

    private static class PopupItem {
        final String title;
        final String desc;
        final int iconRes;
        final long durationMs;

        PopupItem(String title, String desc, int iconRes, long durationMs) {
            this.title = title;
            this.desc = desc;
            this.iconRes = iconRes;
            this.durationMs = durationMs;
        }
    }

    private final Queue<PopupItem> notificationQueue = new LinkedList<>();
    private PopupItem currentItem;
    private boolean isShowing = false;
    private long notificationEndTime = 0;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable dismissRunnable;

    private NotificationPopupManager() {}

    public static synchronized NotificationPopupManager getInstance() {
        if (instance == null) {
            instance = new NotificationPopupManager();
        }
        return instance;
    }

    public void init(Application application) {
        application.registerActivityLifecycleCallbacks(this);
    }

    public void showNotification(Context context, String title, String desc, int iconRes) {
        showNotification(context, title, desc, iconRes, 4000);
    }

    public void showNotification(Context context, String title, String desc, int iconRes, long durationMs) {
        // Prevent duplicate notifications in queue
        if (currentItem != null && currentItem.title.equals(title) && currentItem.desc.equals(desc)) {
            return;
        }
        for (PopupItem q : notificationQueue) {
            if (q.title.equals(title) && q.desc.equals(desc)) {
                return;
            }
        }

        notificationQueue.offer(new PopupItem(title, desc, iconRes, durationMs));

        if (!isShowing) {
            processNextNotification();
        }
    }

    private void processNextNotification() {
        if (notificationQueue.isEmpty()) {
            isShowing = false;
            currentItem = null;
            dismissAllPopupsWithFade(null);
            return;
        }

        isShowing = true;
        currentItem = notificationQueue.poll();
        this.notificationEndTime = System.currentTimeMillis() + currentItem.durationMs;

        Activity activity = currentActivityRef != null ? currentActivityRef.get() : null;
        if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
            displayPopupOnActivity(activity, currentItem, currentItem.durationMs, true);
        } else {
            if (dismissRunnable != null) handler.removeCallbacks(dismissRunnable);
            dismissRunnable = this::onCurrentNotificationFinished;
            handler.postDelayed(dismissRunnable, currentItem.durationMs);
        }
    }

    private void displayPopupOnActivity(Activity activity, PopupItem item, long remainingMs, boolean animateFadeIn) {
        ViewGroup root = activity.findViewById(android.R.id.content);
        if (root == null) return;

        View popupView = attachedPopupViews.get(activity);
        if (popupView == null) {
            popupView = LayoutInflater.from(activity).inflate(R.layout.layout_quiz_notification, root, false);
            
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
            );
            params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            params.topMargin = (int) (48 * activity.getResources().getDisplayMetrics().density);

            root.addView(popupView, params);
            attachedPopupViews.put(activity, popupView);
        }

        CardView cardNotif = popupView.findViewById(R.id.cardQuizNotification);
        
        setupSwipeToDismiss(popupView);

        TextView tvTitle = popupView.findViewById(R.id.tvNotificationTitle);
        TextView tvDesc = popupView.findViewById(R.id.tvNotificationDesc);
        ImageView ivIcon = popupView.findViewById(R.id.ivNotificationIcon);

        if (tvTitle != null) tvTitle.setText(item.title);
        if (tvDesc != null) tvDesc.setText(item.desc);
        if (ivIcon != null && item.iconRes != 0) ivIcon.setImageResource(item.iconRes);

        if (cardNotif != null) {
            cardNotif.setVisibility(View.VISIBLE);
        }

        if (animateFadeIn) {
            popupView.setAlpha(0f);
            popupView.animate()
                    .alpha(1.0f)
                    .setDuration(300)
                    .start();
        } else {
            popupView.setAlpha(1.0f);
        }

        if (dismissRunnable != null) {
            handler.removeCallbacks(dismissRunnable);
        }
        dismissRunnable = this::onCurrentNotificationFinished;
        handler.postDelayed(dismissRunnable, remainingMs);
    }

    private void setupSwipeToDismiss(View popupView) {
        popupView.setOnTouchListener(new View.OnTouchListener() {
            private float startX;
            private float initialTranslationX;
            private boolean isSwiping;
            private final int swipeThreshold = (int) (100 * popupView.getContext().getResources().getDisplayMetrics().density);

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getRawX();
                        initialTranslationX = v.getTranslationX();
                        isSwiping = false;
                        v.animate().cancel(); // Stop any running animations
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float deltaX = event.getRawX() - startX;
                        if (Math.abs(deltaX) > 20) {
                            isSwiping = true;
                            v.getParent().requestDisallowInterceptTouchEvent(true);
                        }
                        if (isSwiping) {
                            v.setTranslationX(initialTranslationX + deltaX);
                            // Calculate fade based on distance
                            float alpha = 1.0f - Math.min(1.0f, Math.abs(deltaX) / (v.getWidth() * 1.5f));
                            v.setAlpha(alpha);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (isSwiping) {
                            float finalDeltaX = event.getRawX() - startX;
                            if (Math.abs(finalDeltaX) > swipeThreshold) {
                                // Swiped far enough, dismiss it
                                float direction = Math.signum(finalDeltaX);
                                float screenWidth = v.getContext().getResources().getDisplayMetrics().widthPixels;
                                v.animate()
                                        .translationX(direction * screenWidth)
                                        .alpha(0f)
                                        .setDuration(200)
                                        .withEndAction(() -> {
                                            if (dismissRunnable != null) {
                                                handler.removeCallbacks(dismissRunnable);
                                            }
                                            onCurrentNotificationFinished();
                                        })
                                        .start();
                            } else {
                                // Snap back to center
                                v.animate()
                                        .translationX(0f)
                                        .alpha(1f)
                                        .setDuration(200)
                                        .start();
                            }
                        } else {
                            // It was just a click
                            v.performClick();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void onCurrentNotificationFinished() {
        dismissAllPopupsWithFade(() -> {
            handler.postDelayed(this::processNextNotification, 150);
        });
    }

    private void removePopupFromActivity(Activity activity) {
        View popupView = attachedPopupViews.remove(activity);
        if (popupView != null && popupView.getParent() != null) {
            ((ViewGroup) popupView.getParent()).removeView(popupView);
        }
    }

    private void dismissAllPopupsWithFade(@Nullable Runnable onComplete) {
        if (attachedPopupViews.isEmpty()) {
            notificationEndTime = 0;
            if (onComplete != null) onComplete.run();
            return;
        }

        int totalViews = attachedPopupViews.size();
        final int[] completedCount = {0};

        for (Map.Entry<Activity, View> entry : new HashMap<>(attachedPopupViews).entrySet()) {
            Activity act = entry.getKey();
            View v = entry.getValue();
            if (v != null && v.getParent() != null) {
                v.animate()
                        .alpha(0f)
                        .setDuration(250)
                        .withEndAction(() -> {
                            removePopupFromActivity(act);
                            completedCount[0]++;
                            if (completedCount[0] >= totalViews) {
                                notificationEndTime = 0;
                                if (onComplete != null) onComplete.run();
                            }
                        })
                        .start();
            } else {
                removePopupFromActivity(act);
                completedCount[0]++;
                if (completedCount[0] >= totalViews) {
                    notificationEndTime = 0;
                    if (onComplete != null) onComplete.run();
                }
            }
        }
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        currentActivityRef = new WeakReference<>(activity);

        long remainingMs = notificationEndTime - System.currentTimeMillis();
        if (isShowing && currentItem != null && remainingMs > 300) {
            displayPopupOnActivity(activity, currentItem, remainingMs, false);
        }
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        currentActivityRef = new WeakReference<>(activity);

        long remainingMs = notificationEndTime - System.currentTimeMillis();
        if (isShowing && currentItem != null && remainingMs > 300) {
            displayPopupOnActivity(activity, currentItem, remainingMs, false);
        }
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        Activity current = currentActivityRef != null ? currentActivityRef.get() : null;
        if (current != activity) {
            removePopupFromActivity(activity);
        }
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        removePopupFromActivity(activity);
        if (currentActivityRef != null && currentActivityRef.get() == activity) {
            currentActivityRef = null;
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}

    @Override
    public void onActivityPaused(@NonNull Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
}