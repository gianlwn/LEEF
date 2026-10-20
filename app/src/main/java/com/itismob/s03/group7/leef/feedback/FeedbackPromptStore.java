package com.itismob.s03.group7.leef.feedback;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;

/** Remembers which events this phone has already asked the person to rate, so each is asked only once. */
final class FeedbackPromptStore {

    private static final String PREFS = "leef_feedback_prompts";
    private static final String KEY_PROMPTED = "prompted";

    private final SharedPreferences prefs;

    FeedbackPromptStore(@NonNull Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    boolean wasPrompted(@NonNull String uid, @NonNull String eventId) {
        return prefs.getStringSet(KEY_PROMPTED, new HashSet<>()).contains(key(uid, eventId));
    }

    void markPrompted(@NonNull String uid, @NonNull String eventId) {
        // getStringSet returns a set that must not be modified, so work on a copy.
        Set<String> updated = new HashSet<>(prefs.getStringSet(KEY_PROMPTED, new HashSet<>()));
        updated.add(key(uid, eventId));
        prefs.edit().putStringSet(KEY_PROMPTED, updated).apply();
    }

    private static String key(String uid, String eventId) {
        return uid + "|" + eventId;
    }
}
