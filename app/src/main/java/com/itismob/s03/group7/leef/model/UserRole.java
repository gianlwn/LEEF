package com.itismob.s03.group7.leef.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.itismob.s03.group7.leef.R;

/**
 * The account roles from the MCO1 scope of work. This is a fixed option list (not a database
 * record), so it lives in the app. {@link #getValue()} is what gets stored on the user profile.
 */
public enum UserRole {
    STUDENT("student", R.string.role_student),
    TEACHER("teacher", R.string.role_teacher),
    ORGANIZER("organizer", R.string.role_organizer),
    PARENT("parent", R.string.role_parent),
    FREELANCER("freelancer", R.string.role_freelancer);

    private final String value;
    private final int labelRes;

    UserRole(@NonNull String value, @StringRes int labelRes) {
        this.value = value;
        this.labelRes = labelRes;
    }

    /** Stable identifier saved in Firestore ("student", "teacher", ...). */
    @NonNull
    public String getValue() {
        return value;
    }

    @StringRes
    public int getLabelRes() {
        return labelRes;
    }

    /** The role saved under {@code value}, or null if it is unknown. */
    @Nullable
    public static UserRole fromValue(@Nullable String value) {
        for (UserRole role : values()) {
            if (role.value.equals(value)) {
                return role;
            }
        }
        return null;
    }
}
