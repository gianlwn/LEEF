package com.itismob.s03.group7.leef.organizer;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.itismob.s03.group7.leef.model.Event;

import java.util.Collections;
import java.util.List;

/** Everything the Posted Events screen draws at one moment. */
public final class PostedEventsState {

    public enum Status {
        LOADING,
        ERROR,
        EMPTY,
        CONTENT
    }

    private final Status status;
    private final List<Event> events;
    @StringRes
    private final int errorRes;

    PostedEventsState(@NonNull Status status, @NonNull List<Event> events, @StringRes int errorRes) {
        this.status = status;
        this.events = Collections.unmodifiableList(events);
        this.errorRes = errorRes;
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    /** The organizer's events: ones not over yet (soonest first), then ones that ended (latest first). */
    @NonNull
    public List<Event> getEvents() {
        return events;
    }

    @StringRes
    public int getErrorRes() {
        return errorRes;
    }
}
