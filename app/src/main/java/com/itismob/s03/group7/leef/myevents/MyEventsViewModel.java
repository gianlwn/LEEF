package com.itismob.s03.group7.leef.myevents;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.data.EventRepository;
import com.itismob.s03.group7.leef.data.FirebaseEventRepository;
import com.itismob.s03.group7.leef.data.FirebaseRegistrationRepository;
import com.itismob.s03.group7.leef.data.RegistrationRepository;
import com.itismob.s03.group7.leef.model.Event;
import com.itismob.s03.group7.leef.model.Registration;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Screen 8 - My Events. Combines the events the person registered for with the ones they saved, and
 * splits them into Upcoming and Past. A registered event shows once, as Registered (or Group registered).
 */
public class MyEventsViewModel extends ViewModel {

    private final EventRepository events;
    private final RegistrationRepository registrations;

    private final MutableLiveData<MyEventsState> state = new MutableLiveData<>();
    private final MutableLiveData<Integer> message = new MutableLiveData<>();

    private List<MyEventItem> upcoming = new ArrayList<>();
    private List<MyEventItem> past = new ArrayList<>();
    private boolean showPast;
    private boolean loadedOnce;
    private boolean loading;
    private boolean refreshing;
    @Nullable
    private EventRepository.Failure failure;

    public MyEventsViewModel() {
        this(new FirebaseEventRepository(), new FirebaseRegistrationRepository());
    }

    MyEventsViewModel(@NonNull EventRepository events, @NonNull RegistrationRepository registrations) {
        this.events = events;
        this.registrations = registrations;
        publish();
        load(false);
    }

    @NonNull
    public LiveData<MyEventsState> getState() {
        return state;
    }

    /** A toast message (string resource id). Call {@link #messageHandled()} after showing it. */
    @NonNull
    public LiveData<Integer> getMessage() {
        return message;
    }

    public void messageHandled() {
        message.setValue(null);
    }

    public void selectTab(boolean past) {
        if (showPast != past) {
            showPast = past;
            publish();
        }
    }

    /** Pull to refresh. */
    public void refresh() {
        load(true);
    }

    /** "Try again" after a failed first load. */
    public void retry() {
        load(false);
    }

    /** Quietly picks up new registrations and hearts when the tab comes back into view. */
    public void reload() {
        if (loadedOnce) {
            load(false);
        }
    }

    @SuppressWarnings("unchecked")
    private void load(boolean userInitiated) {
        if (loading) {
            if (userInitiated) {
                refreshing = true;
                publish();
            }
            return;
        }
        loading = true;
        refreshing = userInitiated;
        failure = null;
        publish();

        final int[] pending = {2};
        final List<Registration>[] loadedRegistrations = new List[]{null};
        final Set<String>[] savedIds = new Set[]{new HashSet<String>()};
        final RegistrationRepository.Failure[] registrationFailure = {null};

        Runnable next = () -> {
            if (--pending[0] > 0) {
                return;
            }
            if (loadedRegistrations[0] == null) {
                fail(registrationFailure[0] == RegistrationRepository.Failure.NETWORK
                        ? EventRepository.Failure.NETWORK
                        : registrationFailure[0] == RegistrationRepository.Failure.PERMISSION
                        ? EventRepository.Failure.PERMISSION : EventRepository.Failure.UNKNOWN);
                return;
            }
            loadEvents(loadedRegistrations[0], savedIds[0]);
        };

        registrations.loadRegistrations(new RegistrationRepository.ListCallback() {
            @Override
            public void onLoaded(@NonNull List<Registration> result) {
                loadedRegistrations[0] = result;
                next.run();
            }

            @Override
            public void onFailure(@NonNull RegistrationRepository.Failure reason) {
                registrationFailure[0] = reason;
                next.run();
            }
        });
        events.loadSavedEventIds(new EventRepository.SavedIdsCallback() {
            @Override
            public void onLoaded(@NonNull Set<String> ids) {
                savedIds[0] = ids;
                next.run();
            }

            @Override
            public void onFailure(@NonNull EventRepository.Failure reason) {
                // Registrations still show without the saved ones.
                next.run();
            }
        });
    }

    private void loadEvents(@NonNull List<Registration> registered, @NonNull Set<String> saved) {
        Map<String, Registration> byId = new HashMap<>();
        for (Registration registration : registered) {
            byId.put(registration.getEventId(), registration);
        }
        Set<String> ids = new HashSet<>(byId.keySet());
        ids.addAll(saved);

        events.loadEventsByIds(ids, new EventRepository.EventsCallback() {
            @Override
            public void onLoaded(@NonNull List<Event> found) {
                Map<String, Event> eventsById = new HashMap<>();
                for (Event event : found) {
                    eventsById.put(event.getId(), event);
                }
                build(ids, byId, eventsById);
                loading = false;
                refreshing = false;
                loadedOnce = true;
                failure = null;
                publish();
            }

            @Override
            public void onFailure(@NonNull EventRepository.Failure reason) {
                fail(reason);
            }
        });
    }

    private void build(@NonNull Set<String> ids, @NonNull Map<String, Registration> registered,
                       @NonNull Map<String, Event> eventsById) {
        long now = System.currentTimeMillis();
        long startOfToday = startOfDay(now);
        List<MyEventItem> nextUpcoming = new ArrayList<>();
        List<MyEventItem> nextPast = new ArrayList<>();
        for (String id : ids) {
            Registration registration = registered.get(id);
            Event event = eventsById.get(id);
            if (event == null && registration == null) {
                continue; // A saved event that has since been removed.
            }
            String title = event != null ? event.getTitle() : registration.getEventTitle();
            long startsAt = event != null ? event.getStartsAtMillis() : registration.getEventStartsAtMillis();
            boolean cancelled = event != null && event.isCancelled();
            // A cancelled event is never upcoming: it moves to Past and says Cancelled.
            boolean isUpcoming = !cancelled && (event != null ? !event.hasEnded(now) : startsAt >= startOfToday);
            MyEventItem.Kind kind = cancelled ? MyEventItem.Kind.CANCELLED : MyEventItem.kindOf(registration);
            MyEventItem item = new MyEventItem(id, title, startsAt, event, kind, isUpcoming);
            (isUpcoming ? nextUpcoming : nextPast).add(item);
        }
        Collections.sort(nextUpcoming, Comparator.comparingLong(MyEventItem::getStartsAtMillis));
        Collections.sort(nextPast, (a, b) -> Long.compare(b.getStartsAtMillis(), a.getStartsAtMillis()));
        upcoming = nextUpcoming;
        past = nextPast;
    }

    private void fail(@NonNull EventRepository.Failure reason) {
        loading = false;
        boolean wasRefreshing = refreshing;
        refreshing = false;
        if (loadedOnce) {
            // Keep what is on screen; only say so if the person asked for the refresh.
            if (wasRefreshing) {
                message.setValue(reason == EventRepository.Failure.NETWORK
                        ? R.string.my_events_refresh_offline : R.string.my_events_refresh_failed);
            }
        } else {
            failure = reason;
        }
        publish();
    }

    private void publish() {
        MyEventsState.Status status;
        if (loadedOnce) {
            status = MyEventsState.Status.CONTENT;
        } else if (failure != null) {
            status = MyEventsState.Status.ERROR;
        } else {
            status = MyEventsState.Status.LOADING;
        }
        state.setValue(new MyEventsState(status, showPast, refreshing, upcoming, past, failure));
    }

    private static long startOfDay(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
