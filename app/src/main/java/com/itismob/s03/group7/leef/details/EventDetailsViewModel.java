package com.itismob.s03.group7.leef.details;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.data.EventRepository;
import com.itismob.s03.group7.leef.data.FirebaseEventRepository;
import com.itismob.s03.group7.leef.data.FirebaseRegistrationRepository;
import com.itismob.s03.group7.leef.data.RegistrationRepository;
import com.itismob.s03.group7.leef.model.Event;
import com.itismob.s03.group7.leef.model.Registration;

import java.util.Set;

/**
 * Screen 6 - Event Details. Loads one event by id, whether the person has saved it (the heart) and
 * whether they have registered for it, and handles the heart.
 */
public class EventDetailsViewModel extends ViewModel {

    private final EventRepository events;
    private final RegistrationRepository registrations;
    private final String eventId;

    private final MutableLiveData<EventDetailsState> state = new MutableLiveData<>();
    private final MutableLiveData<Integer> message = new MutableLiveData<>();

    @Nullable
    private Event event;
    private boolean saved;
    private boolean registered;
    private boolean loading;
    @Nullable
    private EventRepository.Failure failure;

    EventDetailsViewModel(@NonNull EventRepository events, @NonNull RegistrationRepository registrations,
                          @NonNull String eventId) {
        this.events = events;
        this.registrations = registrations;
        this.eventId = eventId;
        load();
    }

    @NonNull
    public LiveData<EventDetailsState> getState() {
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

    /** "Try again" after a failed load. */
    public void retry() {
        load();
    }

    /** Called when Registration & Payment finishes, so the button changes without another load. */
    public void markRegistered() {
        registered = true;
        publish();
    }

    /** Taps the heart: saves the event, or removes it from the saved events. */
    public void toggleSaved() {
        if (event == null) {
            return;
        }
        boolean nowSaved = !saved;
        saved = nowSaved;
        publish();
        events.setEventSaved(eventId, nowSaved, new EventRepository.DoneCallback() {
            @Override
            public void onDone() {
                // Already shown.
            }

            @Override
            public void onFailure(@NonNull EventRepository.Failure reason) {
                saved = !nowSaved;
                publish();
                message.setValue(nowSaved ? R.string.home_save_failed : R.string.home_unsave_failed);
            }
        });
    }

    private void load() {
        if (loading) {
            return;
        }
        loading = true;
        failure = null;
        publish();

        final int[] pending = {3};
        final Event[] loaded = {null};
        final boolean[] savedFlag = {false};
        final boolean[] registeredFlag = {false};
        final EventRepository.Failure[] eventFailure = {null};

        Runnable finish = () -> {
            if (--pending[0] > 0) {
                return;
            }
            loading = false;
            if (loaded[0] != null) {
                event = loaded[0];
                saved = savedFlag[0];
                registered = registeredFlag[0];
                failure = null;
            } else {
                failure = eventFailure[0] == null ? EventRepository.Failure.UNKNOWN : eventFailure[0];
            }
            publish();
        };

        events.loadEvent(eventId, new EventRepository.EventCallback() {
            @Override
            public void onLoaded(@NonNull Event result) {
                loaded[0] = result;
                finish.run();
            }

            @Override
            public void onFailure(@NonNull EventRepository.Failure reason) {
                eventFailure[0] = reason;
                finish.run();
            }
        });
        events.loadSavedEventIds(new EventRepository.SavedIdsCallback() {
            @Override
            public void onLoaded(@NonNull Set<String> ids) {
                savedFlag[0] = ids.contains(eventId);
                finish.run();
            }

            @Override
            public void onFailure(@NonNull EventRepository.Failure reason) {
                // The heart is optional: the details still show without it.
                finish.run();
            }
        });
        registrations.loadRegistration(eventId, new RegistrationRepository.LoadCallback() {
            @Override
            public void onLoaded(@Nullable Registration registration) {
                registeredFlag[0] = registration != null;
                finish.run();
            }

            @Override
            public void onFailure(@NonNull RegistrationRepository.Failure reason) {
                // Optional too: Register Now still works, and the registration screen checks again.
                finish.run();
            }
        });
    }

    private void publish() {
        EventDetailsState.Status status;
        if (event != null) {
            status = EventDetailsState.Status.CONTENT;
        } else if (loading || failure == null) {
            status = EventDetailsState.Status.LOADING;
        } else if (failure == EventRepository.Failure.NOT_FOUND) {
            status = EventDetailsState.Status.NOT_FOUND;
        } else {
            status = EventDetailsState.Status.ERROR;
        }
        state.setValue(new EventDetailsState(status, event, saved, registered, failure));
    }

    /** Creates the view model for one event id. */
    static final class Factory implements ViewModelProvider.Factory {
        private final String eventId;

        Factory(@NonNull String eventId) {
            this.eventId = eventId;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new EventDetailsViewModel(new FirebaseEventRepository(),
                    new FirebaseRegistrationRepository(), eventId);
        }
    }
}
