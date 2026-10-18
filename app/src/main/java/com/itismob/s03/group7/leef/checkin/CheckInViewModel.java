package com.itismob.s03.group7.leef.checkin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.itismob.s03.group7.leef.data.EventRepository;
import com.itismob.s03.group7.leef.data.FirebaseEventRepository;
import com.itismob.s03.group7.leef.data.FirebaseProfileRepository;
import com.itismob.s03.group7.leef.data.FirebaseRegistrationRepository;
import com.itismob.s03.group7.leef.data.ProfileRepository;
import com.itismob.s03.group7.leef.data.RegistrationRepository;
import com.itismob.s03.group7.leef.model.Event;
import com.itismob.s03.group7.leef.model.Registration;
import com.itismob.s03.group7.leef.model.UserProfile;

import java.util.Calendar;

/**
 * Screen 9 - Check-in. Loads the person's registration for one event (the ticket), the event (for the
 * place and time) and the person's name. Only the registration is essential: the event and the name
 * are shown when they can be read.
 */
public class CheckInViewModel extends ViewModel {

    private final RegistrationRepository registrations;
    private final EventRepository events;
    private final ProfileRepository profiles;
    private final String eventId;

    private final MutableLiveData<CheckInState> state = new MutableLiveData<>();

    @Nullable
    private Registration registration;
    @Nullable
    private Event event;
    @Nullable
    private String name;
    private boolean loading;
    private boolean loaded;
    @Nullable
    private EventRepository.Failure failure;

    CheckInViewModel(@NonNull RegistrationRepository registrations, @NonNull EventRepository events,
                     @NonNull ProfileRepository profiles, @NonNull String eventId) {
        this.registrations = registrations;
        this.events = events;
        this.profiles = profiles;
        this.eventId = eventId;
        load();
    }

    @NonNull
    public LiveData<CheckInState> getState() {
        return state;
    }

    /** "Try again" after a failed load. */
    public void retry() {
        load();
    }

    private void load() {
        if (loading) {
            return;
        }
        loading = true;
        failure = null;
        publish();

        final int[] pending = {3};
        final Registration[] loadedRegistration = {null};
        final Event[] loadedEvent = {null};
        final String[] loadedName = {null};
        final EventRepository.Failure[] registrationFailure = {null};

        Runnable finish = () -> {
            if (--pending[0] > 0) {
                return;
            }
            loading = false;
            if (registrationFailure[0] != null) {
                failure = registrationFailure[0];
            } else {
                loaded = true;
                registration = loadedRegistration[0];
                event = loadedEvent[0];
                name = loadedName[0];
            }
            publish();
        };

        registrations.loadRegistration(eventId, new RegistrationRepository.LoadCallback() {
            @Override
            public void onLoaded(@Nullable Registration result) {
                loadedRegistration[0] = result;
                finish.run();
            }

            @Override
            public void onFailure(@NonNull RegistrationRepository.Failure reason) {
                registrationFailure[0] = reason == RegistrationRepository.Failure.NETWORK
                        ? EventRepository.Failure.NETWORK : EventRepository.Failure.UNKNOWN;
                finish.run();
            }
        });
        events.loadEvent(eventId, new EventRepository.EventCallback() {
            @Override
            public void onLoaded(@NonNull Event result) {
                loadedEvent[0] = result;
                finish.run();
            }

            @Override
            public void onFailure(@NonNull EventRepository.Failure reason) {
                // The ticket still works without the event's details.
                finish.run();
            }
        });
        profiles.loadProfile(new ProfileRepository.LoadCallback() {
            @Override
            public void onLoaded(@NonNull UserProfile profile) {
                loadedName[0] = profile.getFullName();
                finish.run();
            }

            @Override
            public void onMissing() {
                finish.run();
            }

            @Override
            public void onFailure(@NonNull ProfileRepository.Failure reason) {
                finish.run();
            }
        });
    }

    private void publish() {
        CheckInState.Status status;
        boolean ended = false;
        if (loading || (!loaded && failure == null)) {
            status = CheckInState.Status.LOADING;
        } else if (failure != null) {
            status = CheckInState.Status.ERROR;
        } else if (registration == null) {
            status = CheckInState.Status.NOT_REGISTERED;
        } else {
            status = CheckInState.Status.CONTENT;
            ended = hasEnded(registration, event, System.currentTimeMillis());
        }
        state.setValue(new CheckInState(status, registration, event, name, ended, failure));
    }

    /** Over once the event ends; if the event is gone, once the day it started is over. */
    static boolean hasEnded(@NonNull Registration registration, @Nullable Event event, long nowMillis) {
        if (event != null) {
            return event.hasEnded(nowMillis);
        }
        Calendar endOfStartDay = Calendar.getInstance();
        endOfStartDay.setTimeInMillis(registration.getEventStartsAtMillis());
        endOfStartDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfStartDay.set(Calendar.MINUTE, 59);
        endOfStartDay.set(Calendar.SECOND, 59);
        return nowMillis > endOfStartDay.getTimeInMillis();
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
            return (T) new CheckInViewModel(new FirebaseRegistrationRepository(), new FirebaseEventRepository(),
                    new FirebaseProfileRepository(), eventId);
        }
    }
}
