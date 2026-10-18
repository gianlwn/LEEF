package com.itismob.s03.group7.leef.myevents;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.itismob.s03.group7.leef.data.EventRepository;

import java.util.Collections;
import java.util.List;

/** Everything the My Events screen draws at one moment. */
public final class MyEventsState {

    public enum Status {
        LOADING,
        ERROR,
        CONTENT
    }

    private final Status status;
    private final boolean showPast;
    private final boolean refreshing;
    private final List<MyEventItem> upcoming;
    private final List<MyEventItem> past;
    @Nullable
    private final EventRepository.Failure failure;

    MyEventsState(@NonNull Status status, boolean showPast, boolean refreshing, @NonNull List<MyEventItem> upcoming,
                  @NonNull List<MyEventItem> past, @Nullable EventRepository.Failure failure) {
        this.status = status;
        this.showPast = showPast;
        this.refreshing = refreshing;
        this.upcoming = Collections.unmodifiableList(upcoming);
        this.past = Collections.unmodifiableList(past);
        this.failure = failure;
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    /** True when the Past tab is selected. */
    public boolean isShowPast() {
        return showPast;
    }

    /** True while a pull-to-refresh is running. */
    public boolean isRefreshing() {
        return refreshing;
    }

    /** Events not over yet, soonest first. */
    @NonNull
    public List<MyEventItem> getUpcoming() {
        return upcoming;
    }

    /** Events that are over, most recent first. */
    @NonNull
    public List<MyEventItem> getPast() {
        return past;
    }

    /** The rows for the selected tab. */
    @NonNull
    public List<MyEventItem> getVisible() {
        return showPast ? past : upcoming;
    }

    @Nullable
    public EventRepository.Failure getFailure() {
        return failure;
    }
}
