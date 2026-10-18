package com.itismob.s03.group7.leef.checkin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.itismob.s03.group7.leef.data.EventRepository;
import com.itismob.s03.group7.leef.model.Event;
import com.itismob.s03.group7.leef.model.Registration;

/** Everything the Check-in screen draws at one moment. */
public final class CheckInState {

    public enum Status {
        LOADING,
        /** Couldn't load; see {@link #getFailure()}. */
        ERROR,
        /** The person hasn't registered for this event, so there is no ticket. */
        NOT_REGISTERED,
        CONTENT
    }

    private final Status status;
    @Nullable
    private final Registration registration;
    @Nullable
    private final Event event;
    @Nullable
    private final String name;
    private final boolean ended;
    @Nullable
    private final EventRepository.Failure failure;

    CheckInState(@NonNull Status status, @Nullable Registration registration, @Nullable Event event,
                 @Nullable String name, boolean ended, @Nullable EventRepository.Failure failure) {
        this.status = status;
        this.registration = registration;
        this.event = event;
        this.name = name;
        this.ended = ended;
        this.failure = failure;
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    @Nullable
    public Registration getRegistration() {
        return registration;
    }

    /** The event, or null if it was removed (the registration still has its title and start). */
    @Nullable
    public Event getEvent() {
        return event;
    }

    /** The attendee's full name, or null if the profile couldn't be read. */
    @Nullable
    public String getName() {
        return name;
    }

    /** True once the event is over, so the QR code is no longer shown. */
    public boolean isEnded() {
        return ended;
    }

    @Nullable
    public EventRepository.Failure getFailure() {
        return failure;
    }
}
