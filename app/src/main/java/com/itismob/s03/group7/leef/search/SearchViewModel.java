package com.itismob.s03.group7.leef.search;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.data.EventRepository;
import com.itismob.s03.group7.leef.data.FirebaseEventRepository;
import com.itismob.s03.group7.leef.home.EventItem;
import com.itismob.s03.group7.leef.model.Event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Screen 5 - Search & Filter Events. Loads the upcoming events once and filters them on the phone
 * (Firestore has no full-text search), so results and the "Show N Events" count update instantly as
 * the keyword or a filter changes.
 *
 * <p>The Filters sheet edits a separate <em>draft</em> so that backing out of it leaves the results
 * alone; "Show N Events" copies the draft into the live filters.
 */
public class SearchViewModel extends ViewModel {

    /** The most upcoming events searched. */
    private static final int SEARCH_LIMIT = 200;

    private final EventRepository events;

    private final MutableLiveData<SearchState> state = new MutableLiveData<>();
    private final MutableLiveData<SearchFilters> draft = new MutableLiveData<>(SearchFilters.NONE);
    private final MutableLiveData<Integer> message = new MutableLiveData<>();

    private List<Event> loadedEvents = Collections.emptyList();
    private final Set<String> savedIds = new HashSet<>();
    private SearchFilters filters = SearchFilters.NONE;

    private boolean loadedOnce;
    private boolean loading;
    private boolean refreshing;
    @Nullable
    private EventRepository.Failure failure;

    public SearchViewModel() {
        this(new FirebaseEventRepository());
    }

    SearchViewModel(@NonNull EventRepository events) {
        this.events = events;
        publish();
        load(false);
    }

    @NonNull
    public LiveData<SearchState> getState() {
        return state;
    }

    /** The filters being edited in the Filters sheet. */
    @NonNull
    public LiveData<SearchFilters> getDraft() {
        return draft;
    }

    /** A toast message (string resource id). Call {@link #messageHandled()} after showing it. */
    @NonNull
    public LiveData<Integer> getMessage() {
        return message;
    }

    public void messageHandled() {
        message.setValue(null);
    }

    public void refresh() {
        load(true);
    }

    public void retry() {
        load(false);
    }

    /** The keyword box changed. */
    public void setKeyword(@NonNull String keyword) {
        SearchFilters next = filters.withKeyword(keyword);
        if (next.equals(filters)) {
            return;
        }
        filters = next;
        publish();
    }

    /** Opens the Filters sheet: the draft starts as what is applied now. */
    public void beginEditingFilters() {
        draft.setValue(filters);
    }

    public void updateDraft(@NonNull SearchFilters next) {
        if (!next.equals(draft.getValue())) {
            draft.setValue(next);
        }
    }

    /** "Show N Events": the draft becomes the live filters. */
    public void applyDraft() {
        SearchFilters edited = draft.getValue();
        if (edited != null && !edited.equals(filters)) {
            filters = edited;
            publish();
        }
    }

    /** Clears every filter from the sheet but keeps the typed keyword. */
    public void clearSheetFilters() {
        filters = filters.withoutSheetFilters();
        publish();
    }

    /** How many loaded events pass {@code candidate}; drives the live count on the sheet's button. */
    public int countMatching(@NonNull SearchFilters candidate) {
        int count = 0;
        for (Event event : loadedEvents) {
            if (candidate.matches(event)) {
                count++;
            }
        }
        return count;
    }

    /** True once the first load has finished and the count means something. */
    public boolean isLoaded() {
        return loadedOnce;
    }

    /**
     * Re-reads which events are saved. Called when the screen comes back (for example from Event
     * Details, where the heart can change) so the hearts here stay right.
     */
    public void reloadSaved() {
        if (!loadedOnce || loading) {
            return;
        }
        events.loadSavedEventIds(new EventRepository.SavedIdsCallback() {
            @Override
            public void onLoaded(@NonNull Set<String> ids) {
                if (!ids.equals(savedIds)) {
                    savedIds.clear();
                    savedIds.addAll(ids);
                    publish();
                }
            }

            @Override
            public void onFailure(@NonNull EventRepository.Failure reason) {
                // Keep the hearts we have.
            }
        });
    }

    public void toggleSaved(@NonNull Event event) {
        String id = event.getId();
        boolean nowSaved = !savedIds.contains(id);
        applySaved(id, nowSaved);
        publish();
        events.setEventSaved(id, nowSaved, new EventRepository.DoneCallback() {
            @Override
            public void onDone() {
                // Already shown.
            }

            @Override
            public void onFailure(@NonNull EventRepository.Failure reason) {
                applySaved(id, !nowSaved);
                publish();
                message.setValue(nowSaved ? R.string.home_save_failed : R.string.home_unsave_failed);
            }
        });
    }

    private void applySaved(@NonNull String id, boolean saved) {
        if (saved) {
            savedIds.add(id);
        } else {
            savedIds.remove(id);
        }
    }

    private void load(boolean fromPull) {
        if (loading) {
            return;
        }
        loading = true;
        refreshing = fromPull;
        failure = null;
        publish();

        final int[] pending = {2};
        final List<Event>[] loaded = new List[]{null};
        final Set<String>[] saved = new Set[]{null};
        final EventRepository.Failure[] eventsFailure = {null};

        Runnable finish = () -> {
            if (--pending[0] > 0) {
                return;
            }
            loading = false;
            refreshing = false;
            if (loaded[0] != null) {
                loadedEvents = loaded[0];
                loadedOnce = true;
                if (saved[0] != null) {
                    savedIds.clear();
                    savedIds.addAll(saved[0]);
                }
                failure = null;
            } else {
                failure = eventsFailure[0];
                if (loadedOnce) {
                    message.setValue(R.string.home_refresh_failed);
                }
            }
            publish();
            // The Filters sheet may be open and counting; nudge it to recount.
            SearchFilters current = draft.getValue();
            if (current != null) {
                draft.setValue(current);
            }
        };

        events.loadUpcomingEvents(SEARCH_LIMIT, new EventRepository.EventsCallback() {
            @Override
            public void onLoaded(@NonNull List<Event> result) {
                loaded[0] = result;
                finish.run();
            }

            @Override
            public void onFailure(@NonNull EventRepository.Failure reason) {
                eventsFailure[0] = reason;
                finish.run();
            }
        });
        events.loadSavedEventIds(new EventRepository.SavedIdsCallback() {
            @Override
            public void onLoaded(@NonNull Set<String> ids) {
                saved[0] = ids;
                finish.run();
            }

            @Override
            public void onFailure(@NonNull EventRepository.Failure reason) {
                finish.run();
            }
        });
    }

    private void publish() {
        // Boosted events come first (still soonest first within each group).
        long now = System.currentTimeMillis();
        List<EventItem> boosted = new ArrayList<>();
        List<EventItem> results = new ArrayList<>();
        for (Event event : loadedEvents) {
            if (filters.matches(event)) {
                EventItem item = new EventItem(event, savedIds.contains(event.getId()));
                (event.isBoosted(now) ? boosted : results).add(item);
            }
        }
        boosted.addAll(results);
        results = boosted;
        SearchState.Status status;
        if (loadedOnce) {
            status = SearchState.Status.CONTENT;
        } else if (!loading && failure != null) {
            status = SearchState.Status.ERROR;
        } else {
            status = SearchState.Status.LOADING;
        }
        state.setValue(new SearchState(status, results, filters, loadedEvents.size(), refreshing, failure));
    }
}
