package com.itismob.s03.group7.leef;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

public class LeefApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // The LEEF design is light-only; keep system dark mode from inverting it.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}
