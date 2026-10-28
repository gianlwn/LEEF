package com.itismob.s03.group7.leef.organizer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.data.AttendeeRepository;
import com.itismob.s03.group7.leef.data.EventRepository;
import com.itismob.s03.group7.leef.data.FirebaseAttendeeRepository;
import com.itismob.s03.group7.leef.data.FirebaseEventRepository;
import com.itismob.s03.group7.leef.home.FeedChange;
import com.itismob.s03.group7.leef.model.Attendee;
import com.itismob.s03.group7.leef.model.Event;

import java.util.List;

/** Loads an organizer's event and how many people registered and checked in. */
public class ManageEventViewModel extends ViewModel {

    private final EventRepository events;
    private final AttendeeRepository attendees;
    private final String eventId;
    @Nullable
    private final String userId;

    private final MutableLiveData<ManageEventState> state = new MutableLiveData<>();
    private final MutableLiveData<Integer> message = new MutableLiveData<>();

    @Nullable
    private Event event;
    @Nullable
    private EventRepository.Failure failure;
    private boolean countsKnown;
    private int people;
    private int checkedInPeople;
    private boolean loading;
    private boolean cancelling;
    /** Set once this screen cancelled the event, so it shows as cancelled before the reload comes back. */
    private boolean cancelledHere;

    ManageEventViewModel(@NonNull EventRepository events, @NonNull AttendeeRepository attendees,
                         @NonNull String eventId, @Nullable String userId) {
        this.events = events;
        this.attendees = attendees;
        this.eventId = eventId;
        this.userId = userId;
        publish();
        load();
    }

    @NonNull
    public LiveData<ManageEventState> getState() {
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

    public void retry() {
        load();
    }

    /** Cancels the event (it stays on record, marked cancelled). The screen asks the person first. */
    public void cancelEvent() {
        if (cancelling || event == null) {
            return;
        }
        cancelling = true;
        publish();
        events.cancelEvent(eventId, new EventRepository.DoneCallback() {
            @Override
            public void onDone() {
                cancelling = false;
                cancelledHere = true;
                FeedChange.bump();
                publish();
                message.setValue(R.string.manage_cancel_done);
                load();
            }

            @Override
            public void onFailure(@NonNull EventRepository.Failure failure) {
                cancelling = false;
                publish();
                message.setValue(cancelFailureMessage(failure));
            }
        });
    }

    private static int cancelFailureMessage(@NonNull EventRepository.Failure failure) {
        switch (failure) {
            case NETWORK:
                return R.string.manage_cancel_offline;
            case PERMISSION:
                return R.string.edit_save_denied;
            case LOCKED:
                return R.string.manage_cancel_locked;
            default:
                return R.string.manage_cancel_failed;
        }
    }

    /** Picks up a boost, a check-in or a new registration when the screen comes back. */
    public void reload() {
        if (event != null) {
            load();
        }
    }

    @SuppressWarnings("unchecked")
    private void load() {
        if (loading) {
            return;
        }
        loading = true;
        failure = null;
        publish();

        final int[] pending = {2};
        final Event[] loadedEvent = {null};
        final EventRepository.Failure[] eventFailure = {null};
        final List<Attendee>[] loadedAttendees = new List[]{null};

        Runnable finish = () -> {
            if (--pending[0] > 0) {
                return;
            }
            loading = false;
            if (loadedEvent[0] != null) {
                event = loadedEvent[0];
                failure = null;
                if (loadedAttendees[0] != null) {
                    countsKnown = true;
                    people = 0;
                    checkedInPeople = 0;
                    for (Attendee attendee : loadedAttendees[0]) {
                        people += attendee.getQuantity();
                        if (attendee.isCheckedIn()) {
                            checkedInPeople += attendee.getQuantity();
                        }
                    }
                }
            } else if (event == null) {
                failure = eventFailure[0] == null ? EventRepository.Failure.UNKNOWN : eventFailure[0];
            }
            publish();
        };

        events.loadEvent(eventId, new EventRepository.EventCallback() {
            @Override
            public void onLoaded(@NonNull Event result) {
                loadedEvent[0] = result;
                finish.run();
            }

            @Override
            public void onFailure(@NonNull EventRepository.Failure reason) {
                eventFailure[0] = reason;
                finish.run();
            }
        });
        attendees.loadAttendees(eventId, new AttendeeRepository.ListCallback() {
            @Override
            public void onLoaded(@NonNull List<Attendee> result) {
                loadedAttendees[0] = result;
                finish.run();
            }

            @Override
            public void onFailure(@NonNull AttendeeRepository.Failure reason) {
                // The counts show as unknown; the rest of the screen still works.
                finish.run();
            }
        });
    }

    private void publish() {
        ManageEventState.Status status;
        if (event != null) {
            status = userId != null && userId.equals(event.getOrganizerId())
                    ? ManageEventState.Status.CONTENT : ManageEventState.Status.NOT_ORGANIZER;
        } else if (loading || failure == null) {
            status = ManageEventState.Status.LOADING;
        } else if (failure == EventRepository.Failure.NOT_FOUND) {
            status = ManageEventState.Status.NOT_FOUND;
        } else {
            status = ManageEventState.Status.ERROR;
        }
        boolean cancelled = event != null && (event.isCancelled() || cancelledHere);
        state.setValue(new ManageEventState(status, event, failure, countsKnown, people, checkedInPeople,
                cancelled, cancelling));
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
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            return (T) new ManageEventViewModel(new FirebaseEventRepository(), new FirebaseAttendeeRepository(),
                    eventId, user == null ? null : user.getUid());
        }
    }
}
