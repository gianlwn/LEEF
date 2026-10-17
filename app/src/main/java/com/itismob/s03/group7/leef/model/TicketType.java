package com.itismob.s03.group7.leef.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.itismob.s03.group7.leef.R;

/**
 * The ticket types from MCO1 (Early Bird, Regular, Group 5+). What each one costs always comes from
 * the event's own pricing fields; this enum only names them.
 */
public enum TicketType {
    EARLY_BIRD("earlyBird", R.string.ticket_early_bird),
    REGULAR("regular", R.string.ticket_regular),
    GROUP("group", R.string.ticket_group);

    /** The fewest people a Group ticket covers. */
    public static final int GROUP_MIN_SIZE = 5;
    /** The most people one Group registration can cover. */
    public static final int GROUP_MAX_SIZE = 50;

    private final String value;
    private final int labelRes;

    TicketType(@NonNull String value, @StringRes int labelRes) {
        this.value = value;
        this.labelRes = labelRes;
    }

    /** How the ticket type is stored in Firestore. */
    @NonNull
    public String getValue() {
        return value;
    }

    @StringRes
    public int getLabelRes() {
        return labelRes;
    }

    @Nullable
    public static TicketType fromValue(@Nullable String value) {
        for (TicketType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
