package com.itismob.s03.group7.leef.feedback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.itismob.s03.group7.leef.model.Event;

/** What the Event Feedback screen is showing. */
public final class FeedbackState {

    public enum Status {
        LOADING,
        /** Something couldn't be loaded (offline or similar); Try again. */
        ERROR,
        /** The person can't rate this event (not registered, not over yet, or gone). */
        UNAVAILABLE,
        FORM,
        /** The feedback was sent. */
        DONE
    }

    private final Status status;
    @Nullable
    private final Event event;
    private final boolean posting;
    /** True when the person already rated this event and is changing their feedback. */
    private final boolean editing;
    @StringRes
    private final int titleRes;
    @StringRes
    private final int bodyRes;

    FeedbackState(@NonNull Status status, @Nullable Event event, boolean posting, boolean editing,
                  @StringRes int titleRes, @StringRes int bodyRes) {
        this.status = status;
        this.event = event;
        this.posting = posting;
        this.editing = editing;
        this.titleRes = titleRes;
        this.bodyRes = bodyRes;
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    @Nullable
    public Event getEvent() {
        return event;
    }

    public boolean isPosting() {
        return posting;
    }

    public boolean isEditing() {
        return editing;
    }

    /** Heading for {@link Status#UNAVAILABLE} (0 for the other states). */
    @StringRes
    public int getTitleRes() {
        return titleRes;
    }

    /** Message for {@link Status#UNAVAILABLE} and {@link Status#ERROR}. */
    @StringRes
    public int getBodyRes() {
        return bodyRes;
    }
}
