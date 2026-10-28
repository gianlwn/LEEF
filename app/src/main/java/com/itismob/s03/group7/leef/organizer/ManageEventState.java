package com.itismob.s03.group7.leef.organizer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.itismob.s03.group7.leef.data.EventRepository;
import com.itismob.s03.group7.leef.model.Event;

/** Everything the Manage Event screen draws at one moment. */
public final class ManageEventState {

    public enum Status {
        LOADING,
        ERROR,
        NOT_FOUND,
        /** Somebody other than the organizer opened the screen. */
        NOT_ORGANIZER,
        CONTENT
    }

    private final Status status;
    @Nullable
    private final Event event;
    @Nullable
    private final EventRepository.Failure failure;
    /** False when the registrations couldn't be read, so the counts are unknown. */
    private final boolean countsKnown;
    private final int people;
    private final int checkedInPeople;
    private final boolean cancelled;
    /** True while a cancellation is being saved. */
    private final boolean busy;

    ManageEventState(@NonNull Status status, @Nullable Event event, @Nullable EventRepository.Failure failure,
                     boolean countsKnown, int people, int checkedInPeople, boolean cancelled, boolean busy) {
        this.status = status;
        this.event = event;
        this.failure = failure;
        this.countsKnown = countsKnown;
        this.people = people;
        this.checkedInPeople = checkedInPeople;
        this.cancelled = cancelled;
        this.busy = busy;
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    @Nullable
    public Event getEvent() {
        return event;
    }

    @Nullable
    public EventRepository.Failure getFailure() {
        return failure;
    }

    public boolean isCountsKnown() {
        return countsKnown;
    }

    /** People registered, counting each person of a Group ticket. */
    public int getPeople() {
        return people;
    }

    public int getCheckedInPeople() {
        return checkedInPeople;
    }

    /** True once the event is cancelled (also right after the organizer cancelled it here). */
    public boolean isCancelled() {
        return cancelled;
    }

    public boolean isBusy() {
        return busy;
    }
}
