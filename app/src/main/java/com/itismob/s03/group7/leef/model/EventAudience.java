package com.itismob.s03.group7.leef.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.itismob.s03.group7.leef.R;

/**
 * Who an event is meant for (the "age group" filter in the MCO1 scope of work). A fixed option list,
 * not a database record, so it lives in the app. {@link #getValue()} is what an event stores in
 * Firestore; an event with no audiences is open to everyone.
 */
public enum EventAudience {
    STUDENTS("students", R.string.audience_students),
    TEACHERS("teachers", R.string.audience_teachers),
    PARENTS("parents", R.string.audience_parents),
    FREELANCERS("freelancers", R.string.audience_freelancers),
    SCHOOL_ADMINS("school_admins", R.string.audience_school_admins);

    private final String value;
    private final int labelRes;

    EventAudience(@NonNull String value, @StringRes int labelRes) {
        this.value = value;
        this.labelRes = labelRes;
    }

    /** Stable identifier saved in Firestore ("students", "school_admins", ...). */
    @NonNull
    public String getValue() {
        return value;
    }

    @StringRes
    public int getLabelRes() {
        return labelRes;
    }

    /** The audience saved under {@code value}, or null if it is unknown. */
    @Nullable
    public static EventAudience fromValue(@Nullable String value) {
        for (EventAudience audience : values()) {
            if (audience.value.equals(value)) {
                return audience;
            }
        }
        return null;
    }
}
