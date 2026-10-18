package com.itismob.s03.group7.leef.reminder;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The reminders that currently have an alarm set, in a small private preferences file. Android forgets
 * alarms when the phone restarts, so this list lets {@link BootReceiver} set them again without needing
 * the internet. It holds nothing sensitive: the event's id, title, place and start time.
 */
final class ReminderStore {

    private static final String TAG = "LeefReminders";
    private static final String PREFS = "leef_reminders";

    private static final String KEY_UID = "uid";
    private static final String KEY_TITLE = "title";
    private static final String KEY_PLACE = "place";
    private static final String KEY_STARTS_AT = "startsAt";

    private final SharedPreferences prefs;

    ReminderStore(@NonNull Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    void put(@NonNull Reminder reminder) {
        try {
            JSONObject json = new JSONObject()
                    .put(KEY_UID, reminder.getUid())
                    .put(KEY_TITLE, reminder.getTitle())
                    .put(KEY_PLACE, reminder.getPlace())
                    .put(KEY_STARTS_AT, reminder.getStartsAtMillis());
            prefs.edit().putString(reminder.getEventId(), json.toString()).apply();
        } catch (JSONException e) {
            Log.w(TAG, "couldn't remember reminder", e);
        }
    }

    void remove(@NonNull String eventId) {
        prefs.edit().remove(eventId).apply();
    }

    @Nullable
    Reminder get(@NonNull String eventId) {
        return parse(eventId, prefs.getString(eventId, null));
    }

    @NonNull
    List<Reminder> all() {
        List<Reminder> reminders = new ArrayList<>();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            if (entry.getValue() instanceof String) {
                Reminder reminder = parse(entry.getKey(), (String) entry.getValue());
                if (reminder != null) {
                    reminders.add(reminder);
                }
            }
        }
        return reminders;
    }

    @Nullable
    private static Reminder parse(@NonNull String eventId, @Nullable String raw) {
        if (raw == null) {
            return null;
        }
        try {
            JSONObject json = new JSONObject(raw);
            return new Reminder(eventId, json.getString(KEY_UID), json.getString(KEY_TITLE),
                    json.optString(KEY_PLACE, ""), json.getLong(KEY_STARTS_AT));
        } catch (JSONException e) {
            Log.w(TAG, "ignoring unreadable reminder " + eventId, e);
            return null;
        }
    }
}
