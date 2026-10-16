package com.itismob.s03.group7.leef.home;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.data.EventRepository;
import com.itismob.s03.group7.leef.data.FirebaseEventRepository;
import com.itismob.s03.group7.leef.model.Event;
import com.itismob.s03.group7.leef.model.EventCategory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Screen 4 - Home / Event Feed. Loads upcoming events and the person's saved events, applies the
 * selected category chip, and splits the result into Featured (boosted) and Upcoming sections.
 */
public class HomeViewModel extends ViewModel {

    /** How many upcoming events the feed asks for in one go. */
    private static final int FEED_LIMIT = 40;
    private static final int FEATURED_LIMIT = 6;

    private final EventRepository events;

    private final MutableLiveData<HomeState> state = new MutableLiveData<>();
    private final MutableLiveData<Integer> message = new MutableLiveData<>();

    private List<Event> loadedEvents = Collections.emptyList();
    private final Set<String> savedIds = new HashSet<>();
    @Nullable
    private EventCategory selectedCategory;

    private boolean loadedOnce;
    private boolean loading;
    private boolean refreshing;
    @Nullable
    private EventRepository.Failure failure;

    public HomeViewModel() {
        this(new FirebaseEventRepository());
    }

    HomeViewModel(@NonNull EventRepository events) {
        this.events = events;
        publish();
        load(false);
    }

    @NonNull
    public LiveData<HomeState> getState() {
        return state;
    }

    /** A short message for a toast (a string resource id), e.g. "Couldn't save that event". Call {@link #messageHandled()} after showing it. */
    @NonNull
    public LiveData<Integer> getMessage() {
        return message;
    }

    public void messageHandled() {
        message.setValue(null);
    }

    /** Pull-to-refresh. */
    public void refresh() {
        load(true);
    }

    /** "Try again" after the first load failed. */
    public void retry() {
        load(false);
    }

    /** Selects a category chip, or "All" when {@code category} is null. */
    public void selectCategory(@Nullable EventCategory category) {
        if (selectedCategory == category) {
            return;
        }
        selectedCategory = category;
        publish();
    }

    /** Taps the heart on an event: saves it, or removes it from the saved events. */
    public void toggleSaved(@NonNull Event event) {
        String id = event.getId();
        boolean nowSaved = !savedIds.contains(id);
        applySaved(id, nowSaved);
        publish();
        events.setEventSaved(id, nowSaved, new EventRepository.DoneCallback() {
            @Override
            public void onDone() {
                // Already shown; nothing to do.
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

        // Two reads run side by side; the feed is redrawn once both have answered.
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
                    // Keep showing what we had and just say the refresh didn't work.
                    message.setValue(R.string.home_refresh_failed);
                }
            }
            publish();
        };

        events.loadUpcomingEvents(FEED_LIMIT, new EventRepository.EventsCallback() {
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
                // Hearts are optional: the feed still works without them.
                finish.run();
            }
        });
    }

    private void publish() {
        List<EventItem> featured = new ArrayList<>();
        List<EventItem> upcoming = new ArrayList<>();
        for (Event event : loadedEvents) {
            if (selectedCategory != null && event.getCategory() != selectedCategory) {
                continue;
            }
            EventItem item = new EventItem(event, savedIds.contains(event.getId()));
            if (event.isBoosted() && featured.size() < FEATURED_LIMIT) {
                featured.add(item);
            } else {
                upcoming.add(item);
            }
        }

        HomeState.Status status;
        if (!loadedOnce && loading) {
            status = HomeState.Status.LOADING;
        } else if (!loadedOnce) {
            status = failure != null ? HomeState.Status.ERROR : HomeState.Status.LOADING;
        } else if (loadedEvents.isEmpty()) {
            status = HomeState.Status.EMPTY;
        } else if (featured.isEmpty() && upcoming.isEmpty()) {
            status = HomeState.Status.EMPTY_FILTER;
        } else {
            status = HomeState.Status.CONTENT;
        }
        state.setValue(new HomeState(status, featured, upcoming, selectedCategory, refreshing, failure));
    }

    /** The string to show for a failed first load. */
    @StringRes
    public static int errorMessageFor(@Nullable EventRepository.Failure failure) {
        if (failure == EventRepository.Failure.NETWORK) {
            return R.string.home_error_network;
        }
        return R.string.home_error_generic;
    }
}
