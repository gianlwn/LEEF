package com.itismob.s03.group7.leef.notifications;

import android.content.Context;
import android.text.format.DateUtils;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.model.AppNotification;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** The words and icon for each kind of notification. */
final class NotificationText {

    private NotificationText() {
    }

    @DrawableRes
    static int iconFor(@NonNull AppNotification.Kind kind) {
        switch (kind) {
            case REGISTERED:
                return R.drawable.ic_notif_check;
            case EVENT_CANCELLED:
                return R.drawable.ic_close;
            case EVENT_CHANGED:
                return R.drawable.ic_edit;
            case STARTING_SOON:
                return R.drawable.ic_event_date;
            case NEW_MESSAGES:
                return R.drawable.ic_nav_messages;
            case RATE_EVENT:
            default:
                return R.drawable.ic_star_outline;
        }
    }

    @NonNull
    static String headline(@NonNull Context context, @NonNull AppNotification item) {
        switch (item.getKind()) {
            case REGISTERED:
                return context.getString(R.string.notif_registered_headline);
            case EVENT_CANCELLED:
                return context.getString(R.string.notif_cancelled_headline);
            case EVENT_CHANGED:
                return context.getString(R.string.notif_changed_headline);
            case STARTING_SOON:
                return context.getString(R.string.notif_soon_headline);
            case NEW_MESSAGES:
                return context.getResources().getQuantityString(R.plurals.notif_messages_headline,
                        item.getCount(), item.getCount());
            case RATE_EVENT:
            default:
                return context.getString(R.string.notif_rate_headline);
        }
    }

    @NonNull
    static String body(@NonNull Context context, @NonNull AppNotification item) {
        String title = item.getTitle();
        switch (item.getKind()) {
            case REGISTERED:
                return context.getString(R.string.notif_registered_body, title);
            case EVENT_CANCELLED:
                return context.getString(R.string.notif_cancelled_body, title);
            case EVENT_CHANGED:
                return context.getString(R.string.notif_changed_body, title);
            case STARTING_SOON:
                return context.getString(R.string.notif_soon_body, title, startLabel(context, item.getStartsAtMillis()));
            case NEW_MESSAGES:
                return context.getString(R.string.notif_messages_body, title);
            case RATE_EVENT:
            default:
                return context.getString(R.string.notif_rate_body, title);
        }
    }

    /** "today at 3:00 PM", "tomorrow at 9:00 AM" or "Nov 3 at 9:00 AM". */
    @NonNull
    private static String startLabel(@NonNull Context context, long startsAtMillis) {
        String clock = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date(startsAtMillis));
        if (DateUtils.isToday(startsAtMillis)) {
            return context.getString(R.string.notif_start_today, clock);
        }
        if (DateUtils.isToday(startsAtMillis - DateUtils.DAY_IN_MILLIS)) {
            return context.getString(R.string.notif_start_tomorrow, clock);
        }
        String day = new SimpleDateFormat("MMM d", Locale.getDefault()).format(new Date(startsAtMillis));
        return context.getString(R.string.notif_start_on_day, day, clock);
    }

    /** "3:42 PM" for today, "Yesterday", otherwise "Oct 21". */
    @NonNull
    static String timeLabel(@NonNull Context context, long millis) {
        if (millis <= 0) {
            return "";
        }
        if (DateUtils.isToday(millis)) {
            return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date(millis));
        }
        if (DateUtils.isToday(millis + DateUtils.DAY_IN_MILLIS)) {
            return context.getString(R.string.notif_yesterday);
        }
        return new SimpleDateFormat("MMM d", Locale.getDefault()).format(new Date(millis));
    }
}
