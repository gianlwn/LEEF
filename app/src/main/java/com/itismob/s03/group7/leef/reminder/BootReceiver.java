package com.itismob.s03.group7.leef.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Sets the reminder alarms again after the phone restarts or the app is updated (Android clears them). */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            ReminderScheduler.rescheduleAll(context);
        }
    }
}
