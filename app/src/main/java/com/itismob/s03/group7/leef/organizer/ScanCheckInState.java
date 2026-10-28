package com.itismob.s03.group7.leef.organizer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.itismob.s03.group7.leef.model.Attendee;

/** What the Check-in screen shows: whether a ticket is being looked up and how the last one went. */
public final class ScanCheckInState {

    /** The outcome of the last ticket. */
    public enum Result {
        /** Nothing checked yet. */
        NONE,
        CHECKED_IN,
        ALREADY_CHECKED_IN,
        /** No registration for this event has that code. */
        NOT_FOUND,
        /** A LEEF ticket, but for another event. */
        WRONG_EVENT,
        /** The text isn't a LEEF ticket code. */
        UNREADABLE,
        /** The lookup itself failed (offline, rules); see {@link #getErrorRes()}. */
        ERROR
    }

    private final boolean busy;
    private final Result result;
    @Nullable
    private final Attendee attendee;
    @StringRes
    private final int errorRes;

    ScanCheckInState(boolean busy, @NonNull Result result, @Nullable Attendee attendee, @StringRes int errorRes) {
        this.busy = busy;
        this.result = result;
        this.attendee = attendee;
        this.errorRes = errorRes;
    }

    public boolean isBusy() {
        return busy;
    }

    @NonNull
    public Result getResult() {
        return result;
    }

    /** The registration the result is about, for checked in and already checked in. */
    @Nullable
    public Attendee getAttendee() {
        return attendee;
    }

    @StringRes
    public int getErrorRes() {
        return errorRes;
    }
}
