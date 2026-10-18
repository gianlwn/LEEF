package com.itismob.s03.group7.leef.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/** Runs when a reminder's alarm goes off and shows the notification (only for the signed-in account). */
public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String eventId = intent.getStringExtra(ReminderScheduler.EXTRA_EVENT_ID);
        if (eventId == null) {
            return;
        }
        ReminderStore store = new ReminderStore(context);
        Reminder reminder = store.get(eventId);
        if (reminder == null) {
            return;
        }
        store.remove(eventId);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || !user.getUid().equals(reminder.getUid())) {
            // Someone else is signed in now (or nobody is): this reminder isn't theirs to see.
            return;
        }
        ReminderNotifications.show(context, reminder);
    }
}
