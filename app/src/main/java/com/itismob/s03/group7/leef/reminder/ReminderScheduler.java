package com.itismob.s03.group7.leef.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

/**
 * Sets and cancels the alarm behind each event reminder. LEEF runs on Firebase's free plan, which has
 * no server code to send timed pushes, so the phone schedules the reminder itself. The alarm is
 * "inexact" on purpose (no special permission); Android may deliver it a few minutes late when the
 * phone is dozing.
 */
public final class ReminderScheduler {

    private static final String TAG = "LeefReminders";
    static final String EXTRA_EVENT_ID = "event_id";

    private ReminderScheduler() {
    }

    /**
     * Remembers the reminder and sets its alarm.
     *
     * @return false if the moment to remind has already passed (nothing is scheduled then)
     */
    public static boolean schedule(@NonNull Context context, @NonNull Reminder reminder) {
        Context app = context.getApplicationContext();
        long trigger = reminder.triggerAtMillis();
        if (trigger <= System.currentTimeMillis()) {
            cancel(app, reminder.getEventId());
            return false;
        }
        AlarmManager alarms = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) {
            return false;
        }
        new ReminderStore(app).put(reminder);
        try {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent(app, reminder.getEventId()));
        } catch (SecurityException e) {
            Log.w(TAG, "alarm not allowed", e);
            return false;
        }
        return true;
    }

    /** Removes the reminder and its alarm. */
    public static void cancel(@NonNull Context context, @NonNull String eventId) {
        Context app = context.getApplicationContext();
        AlarmManager alarms = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        if (alarms != null) {
            alarms.cancel(pendingIntent(app, eventId));
        }
        new ReminderStore(app).remove(eventId);
    }

    /** Sets every remembered alarm again (after a restart or an app update); reminders already past are dropped. */
    static void rescheduleAll(@NonNull Context context) {
        ReminderStore store = new ReminderStore(context);
        for (Reminder reminder : store.all()) {
            if (!schedule(context, reminder)) {
                store.remove(reminder.getEventId());
            }
        }
    }

    @NonNull
    private static PendingIntent pendingIntent(@NonNull Context context, @NonNull String eventId) {
        Intent intent = new Intent(context, ReminderReceiver.class).putExtra(EXTRA_EVENT_ID, eventId);
        return PendingIntent.getBroadcast(context, eventId.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
