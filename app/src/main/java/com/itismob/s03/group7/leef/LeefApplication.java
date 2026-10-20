package com.itismob.s03.group7.leef;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.itismob.s03.group7.leef.reminder.ReminderNotifications;
import com.itismob.s03.group7.leef.ui.ConnectionMonitor;
import com.itismob.s03.group7.leef.ui.OfflineBanner;

public class LeefApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // The LEEF design is light-only; keep system dark mode from inverting it.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        // The channel the event reminders use (Android 8+).
        ReminderNotifications.ensureChannel(this);
        // Cover every screen with a "No internet connection" page while the phone is offline.
        ConnectionMonitor.start(this);
        OfflineBanner.install(this);
    }
}
