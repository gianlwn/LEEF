package com.itismob.s03.group7.leef.messages;

import android.content.Context;

import androidx.annotation.NonNull;

import com.itismob.s03.group7.leef.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Times and names as the Messaging screens show them. */
final class ChatFormat {

    private ChatFormat() {
    }

    /** "10:24 AM" for a message. */
    @NonNull
    static String clockTime(long millis) {
        return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date(millis));
    }

    /** The time on a Messages list row: the clock today, "Yesterday", then "Sep 24" (with the year if older). */
    @NonNull
    static String listTime(@NonNull Context context, long millis) {
        int days = daysAgo(millis);
        if (days <= 0) {
            return clockTime(millis);
        }
        if (days == 1) {
            return context.getString(R.string.chat_yesterday);
        }
        return dateText(millis);
    }

    /** The heading between days inside a chat: "Today", "Yesterday" or "Sat, Sep 24". */
    @NonNull
    static String dayHeading(@NonNull Context context, long millis) {
        int days = daysAgo(millis);
        if (days <= 0) {
            return context.getString(R.string.chat_today);
        }
        if (days == 1) {
            return context.getString(R.string.chat_yesterday);
        }
        String pattern = sameYear(millis) ? "EEE, MMM d" : "EEE, MMM d, yyyy";
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date(millis));
    }

    /** True when both moments are on the same calendar day. */
    static boolean sameDay(long first, long second) {
        Calendar a = Calendar.getInstance();
        a.setTimeInMillis(first);
        Calendar b = Calendar.getInstance();
        b.setTimeInMillis(second);
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    /** "Ateneo Innovation Center" becomes "AI"; a single word gives one letter. */
    @NonNull
    static String initialsOf(@NonNull String name) {
        String[] words = name.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        if (words.length > 0 && !words[0].isEmpty()) {
            initials.appendCodePoint(words[0].codePointAt(0));
        }
        if (words.length > 1 && !words[1].isEmpty()) {
            initials.appendCodePoint(words[1].codePointAt(0));
        }
        return initials.toString().toUpperCase(Locale.getDefault());
    }

    /** "John Reyes" becomes "John". */
    @NonNull
    static String firstName(@NonNull String name) {
        String trimmed = name.trim();
        int space = trimmed.indexOf(' ');
        return space < 0 ? trimmed : trimmed.substring(0, space);
    }

    @NonNull
    private static String dateText(long millis) {
        String pattern = sameYear(millis) ? "MMM d" : "MMM d, yyyy";
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date(millis));
    }

    private static boolean sameYear(long millis) {
        Calendar then = Calendar.getInstance();
        then.setTimeInMillis(millis);
        return then.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR);
    }

    /** Whole calendar days between that moment and today (0 = today, 1 = yesterday). */
    private static int daysAgo(long millis) {
        Calendar then = Calendar.getInstance();
        then.setTimeInMillis(millis);
        Calendar today = Calendar.getInstance();
        if (then.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
            return today.get(Calendar.DAY_OF_YEAR) - then.get(Calendar.DAY_OF_YEAR);
        }
        // Different years: fall back to a rough day count (only 0 / 1 / many matter).
        then.set(Calendar.HOUR_OF_DAY, 0);
        then.set(Calendar.MINUTE, 0);
        then.set(Calendar.SECOND, 0);
        then.set(Calendar.MILLISECOND, 0);
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        return (int) Math.round((today.getTimeInMillis() - then.getTimeInMillis()) / 86_400_000.0);
    }
}
