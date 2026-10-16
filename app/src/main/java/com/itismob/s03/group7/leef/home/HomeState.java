package com.itismob.s03.group7.leef.home;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.itismob.s03.group7.leef.data.EventRepository;
import com.itismob.s03.group7.leef.model.EventCategory;

import java.util.Collections;
import java.util.List;

/** Everything the Home feed needs to draw itself, so the fragment only has to render it. */
public final class HomeState {

    public enum Status {
        /** First load, nothing to show yet. */
        LOADING,
        /** There are events to show. */
        CONTENT,
        /** No events exist at all. */
        EMPTY,
        /** Events exist, but none in the selected category. */
        EMPTY_FILTER,
        /** The first load failed. */
        ERROR
    }

    private final Status status;
    private final List<EventItem> featured;
    private final List<EventItem> upcoming;
    @Nullable
    private final EventCategory selectedCategory;
    private final boolean refreshing;
    @Nullable
    private final EventRepository.Failure failure;

    public HomeState(@NonNull Status status, @NonNull List<EventItem> featured, @NonNull List<EventItem> upcoming,
                     @Nullable EventCategory selectedCategory, boolean refreshing,
                     @Nullable EventRepository.Failure failure) {
        this.status = status;
        this.featured = Collections.unmodifiableList(featured);
        this.upcoming = Collections.unmodifiableList(upcoming);
        this.selectedCategory = selectedCategory;
        this.refreshing = refreshing;
        this.failure = failure;
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    @NonNull
    public List<EventItem> getFeatured() {
        return featured;
    }

    @NonNull
    public List<EventItem> getUpcoming() {
        return upcoming;
    }

    /** The category chip that is selected, or null for "All". */
    @Nullable
    public EventCategory getSelectedCategory() {
        return selectedCategory;
    }

    /** True while a pull-to-refresh is in progress. */
    public boolean isRefreshing() {
        return refreshing;
    }

    @Nullable
    public EventRepository.Failure getFailure() {
        return failure;
    }
}
