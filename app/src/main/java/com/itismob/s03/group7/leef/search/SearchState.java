package com.itismob.s03.group7.leef.search;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.itismob.s03.group7.leef.data.EventRepository;
import com.itismob.s03.group7.leef.home.EventItem;

import java.util.Collections;
import java.util.List;

/** Everything the Search & Filter screen draws at one moment. */
public final class SearchState {

    public enum Status {
        /** The first load is still running. */
        LOADING,
        /** The first load failed. */
        ERROR,
        /** Events are loaded; {@link #getResults()} may still be empty. */
        CONTENT
    }

    private final Status status;
    private final List<EventItem> results;
    private final SearchFilters filters;
    /** How many upcoming events were loaded before any keyword or filter. */
    private final int totalLoaded;
    private final boolean refreshing;
    @Nullable
    private final EventRepository.Failure failure;

    SearchState(@NonNull Status status, @NonNull List<EventItem> results, @NonNull SearchFilters filters,
                int totalLoaded, boolean refreshing, @Nullable EventRepository.Failure failure) {
        this.status = status;
        this.results = Collections.unmodifiableList(results);
        this.filters = filters;
        this.totalLoaded = totalLoaded;
        this.refreshing = refreshing;
        this.failure = failure;
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    @NonNull
    public List<EventItem> getResults() {
        return results;
    }

    @NonNull
    public SearchFilters getFilters() {
        return filters;
    }

    public int getTotalLoaded() {
        return totalLoaded;
    }

    public boolean isRefreshing() {
        return refreshing;
    }

    @Nullable
    public EventRepository.Failure getFailure() {
        return failure;
    }
}
