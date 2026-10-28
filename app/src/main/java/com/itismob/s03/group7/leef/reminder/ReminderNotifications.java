package com.itismob.s03.group7.leef.reminder;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.itismob.s03.group7.leef.MainActivity;
import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.checkin.CheckInActivity;
import com.itismob.s03.group7.leef.details.EventDetailsActivity;

/** The "Event Reminder" notification and the notification channel it uses. */
public final class ReminderNotifications {

    private static final String CHANNEL_ID = "event_reminders";

    private ReminderNotifications() {
    }

    /** Creates the channel (Android 8+). Safe to call every time the app starts. */
    public static void ensureChannel(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                context.getString(R.string.reminder_channel_name), NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(context.getString(R.string.reminder_channel_description));
        manager.createNotificationChannel(channel);
    }

    /** Whether the person has allowed LEEF to show notifications (always true before Android 13). */
    public static boolean canNotify(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        return manager == null || manager.areNotificationsEnabled();
    }

    /**
     * Shows the reminder. Tapping it opens the check-in ticket for an event the person registered for, or
     * the event page for one they only saved (Home underneath, so Back works).
     */
    static void show(@NonNull Context context, @NonNull Reminder reminder) {
        if (!canNotify(context)) {
            return;
        }
        ensureChannel(context);
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }

        PendingIntent tap = TaskStackBuilder.create(context)
                .addNextIntent(new Intent(context, MainActivity.class))
                .addNextIntent(reminder.isRegistered()
                        ? CheckInActivity.createIntent(context, reminder.getEventId())
                        : EventDetailsActivity.createIntent(context, reminder.getEventId()))
                .getPendingIntent(reminder.getEventId().hashCode(),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String text = bodyText(context, reminder);
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(context, R.color.leef_green_700))
                .setContentTitle(context.getString(reminder.isRegistered()
                        ? R.string.reminder_title : R.string.reminder_title_saved))
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(tap)
                .setAutoCancel(true)
                .build();
        manager.notify(reminder.getEventId().hashCode(), notification);
    }

    @NonNull
    static String bodyText(@NonNull Context context, @NonNull Reminder reminder) {
        boolean hasPlace = !reminder.getPlace().isEmpty();
        if (reminder.isDateOnly()) {
            return hasPlace
                    ? context.getString(R.string.reminder_text_today_place, reminder.getTitle(), reminder.getPlace())
                    : context.getString(R.string.reminder_text_today, reminder.getTitle());
        }
        return hasPlace
                ? context.getString(R.string.reminder_text_place, reminder.getTitle(), reminder.getPlace())
                : context.getString(R.string.reminder_text, reminder.getTitle());
    }
}
