package com.itismob.s03.group7.leef.details;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.itismob.s03.group7.leef.data.EventRepository;
import com.itismob.s03.group7.leef.model.Event;

/** Everything the Event Details screen draws at one moment. */
public final class EventDetailsState {

    public enum Status {
        LOADING,
        /** The event couldn't be loaded; see {@link #getFailure()}. */
        ERROR,
        /** The event doesn't exist (any more). */
        NOT_FOUND,
        CONTENT
    }

    private final Status status;
    @Nullable
    private final Event event;
    private final boolean saved;
    private final boolean registered;
    @Nullable
    private final EventRepository.Failure failure;

    EventDetailsState(@NonNull Status status, @Nullable Event event, boolean saved, boolean registered,
                      @Nullable EventRepository.Failure failure) {
        this.status = status;
        this.event = event;
        this.saved = saved;
        this.registered = registered;
        this.failure = failure;
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    /** The event once loaded, otherwise null. */
    @Nullable
    public Event getEvent() {
        return event;
    }

    /** Whether the person has saved this event (the heart). */
    public boolean isSaved() {
        return saved;
    }

    /** Whether the person has already registered for this event. */
    public boolean isRegistered() {
        return registered;
    }

    @Nullable
    public EventRepository.Failure getFailure() {
        return failure;
    }
}
