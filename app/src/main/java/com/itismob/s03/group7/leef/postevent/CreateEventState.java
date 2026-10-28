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
        /** Edit mode: the event can't be changed (it isn't theirs, has started, or was cancelled). */
        LOCKED,
        FORM
    }

    private final Status status;
    private final boolean posting;
    private final boolean editing;
    private final boolean pricesLocked;
    private final int registeredPeople;
    private final String organizerName;
    @StringRes
    private final int errorRes;

    CreateEventState(@NonNull Status status, boolean posting, boolean editing, boolean pricesLocked,
                     int registeredPeople, @Nullable String organizerName, @StringRes int errorRes) {
        this.status = status;
        this.posting = posting;
        this.editing = editing;
        this.pricesLocked = pricesLocked;
        this.registeredPeople = registeredPeople;
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

    /** True when an existing event is being edited instead of a new one created. */
    public boolean isEditing() {
        return editing;
    }

    /**
     * True when the prices can't be changed because people already registered (or because that couldn't
     * be checked). Everything else stays editable.
     */
    public boolean isPricesLocked() {
        return pricesLocked;
    }

    /** How many people registered; only meaningful when {@link #isPricesLocked()} came from real counts. */
    public int getRegisteredPeople() {
        return registeredPeople;
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
