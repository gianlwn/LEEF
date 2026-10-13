package com.itismob.s03.group7.leef.auth;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Remembers that a signed-in Google account still has to finish its LEEF profile, so reopening the
 * app after closing it mid-way returns to that step instead of the app. Tiny and local to the phone.
 */
final class PendingProfileStore {

    private static final String PREFS = "leef_pending_profile";
    private static final String KEY_UID = "uid";

    private PendingProfileStore() {
    }

    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static void markPending(@NonNull Context context, @NonNull String uid) {
        prefs(context).edit().putString(KEY_UID, uid).apply();
    }

    static void clear(@NonNull Context context) {
        prefs(context).edit().remove(KEY_UID).apply();
    }

    /** True if {@code uid} (the signed-in account) is the one that still owes a profile. */
    static boolean isPendingFor(@NonNull Context context, @Nullable String uid) {
        return uid != null && uid.equals(prefs(context).getString(KEY_UID, null));
    }
}
