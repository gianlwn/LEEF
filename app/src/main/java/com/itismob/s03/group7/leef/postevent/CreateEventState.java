package com.itismob.s03.group7.leef.postevent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

/** What Create Event is showing: the person's profile loading, a problem with it, or the form. */
public final class CreateEventState {

    public enum Status {
        /** Checking who is signed in. */
        LOADING,
        /** The profile could not be read (offline or similar). */
        ERROR,
        /** The signed-in account is not an organizer, so it can't post events. */
        FORBIDDEN,
        FORM
    }

    private final Status status;
    private final boolean posting;
    private final String organizerName;
    @StringRes
    private final int errorRes;

    CreateEventState(@NonNull Status status, boolean posting, @Nullable String organizerName,
                     @StringRes int errorRes) {
        this.status = status;
        this.posting = posting;
        this.organizerName = organizerName == null ? "" : organizerName;
        this.errorRes = errorRes;
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    /** True while the event is being saved (the button shows a spinner and the form is locked). */
    public boolean isPosting() {
        return posting;
    }

    /** The school or organization the event will be posted under. */
    @NonNull
    public String getOrganizerName() {
        return organizerName;
    }

    /** The message for {@link Status#ERROR}. */
    @StringRes
    public int getErrorRes() {
        return errorRes;
    }
}
