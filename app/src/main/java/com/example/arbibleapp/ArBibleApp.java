package com.example.arbibleapp;

import android.app.Application;

public class ArBibleApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationPopupManager.getInstance().init(this);
    }
}