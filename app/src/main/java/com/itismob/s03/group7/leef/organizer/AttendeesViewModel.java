package com.itismob.s03.group7.leef.organizer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.data.AttendeeRepository;
import com.itismob.s03.group7.leef.data.FirebaseAttendeeRepository;
import com.itismob.s03.group7.leef.model.Attendee;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Organizer's list of who registered, with a button to check each person in by hand. */
public class AttendeesViewModel extends ViewModel {

    private final String eventId;
    private final AttendeeRepository repository;

    private final MutableLiveData<AttendeesState> state = new MutableLiveData<>();
    private final MutableLiveData<Integer> message = new MutableLiveData<>();

    private List<Attendee> attendees = new ArrayList<>();
    private final Set<String> busy = new HashSet<>();
    private boolean loaded;
    private boolean loading;
    private int errorRes;

    AttendeesViewModel(@NonNull String eventId, @NonNull AttendeeRepository repository) {
        this.eventId = eventId;
        this.repository = repository;
        publish();
        load();
    }

    @NonNull
    public LiveData<AttendeesState> getState() {
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

    /** Quietly picks up check-ins done on the scanner screen when this screen comes back. */
    public void reload() {
        if (loaded) {
            load();
        }
    }

    public void checkIn(@NonNull Attendee attendee) {
        if (attendee.isCheckedIn() || !busy.add(attendee.getDocumentId())) {
            return;
        }
        publish();
        repository.checkIn(eventId, attendee.getConfirmationCode(), new AttendeeRepository.CheckInCallback() {
            @Override
            public void onResult(@NonNull AttendeeRepository.Outcome outcome, @Nullable Attendee result) {
                busy.remove(attendee.getDocumentId());
                if (result != null) {
                    replace(result);
                }
                publish();
            }

            @Override
            public void onFailure(@NonNull AttendeeRepository.Failure failure) {
                busy.remove(attendee.getDocumentId());
                message.setValue(OrganizerText.checkInError(failure));
                publish();
            }
        });
    }

    private void replace(@NonNull Attendee updated) {
        List<Attendee> next = new ArrayList<>(attendees);
        for (int i = 0; i < next.size(); i++) {
            if (next.get(i).getDocumentId().equals(updated.getDocumentId())) {
                next.set(i, updated);
            }
        }
        attendees = next;
    }

    private void load() {
        if (loading) {
            return;
        }
        loading = true;
        repository.loadAttendees(eventId, new AttendeeRepository.ListCallback() {
            @Override
            public void onLoaded(@NonNull List<Attendee> result) {
                loading = false;
                loaded = true;
                List<Attendee> sorted = new ArrayList<>(result);
                Collections.sort(sorted, (a, b) -> displayName(a).compareToIgnoreCase(displayName(b)));
                attendees = sorted;
                publish();
            }

            @Override
            public void onFailure(@NonNull AttendeeRepository.Failure failure) {
                loading = false;
                if (!loaded) {
                    errorRes = OrganizerText.loadError(failure);
                }
                publish();
            }
        });
    }

    /** Sorting key: the name, with unnamed (older) registrations last. */
    @NonNull
    private static String displayName(@NonNull Attendee attendee) {
        return attendee.getName().isEmpty() ? "￿" + attendee.getConfirmationCode()
                : attendee.getName().toLowerCase(Locale.ROOT);
    }

    private void publish() {
        AttendeesState.Status status;
        if (loaded) {
            status = attendees.isEmpty() ? AttendeesState.Status.EMPTY : AttendeesState.Status.CONTENT;
        } else if (!loading && errorRes != 0) {
            status = AttendeesState.Status.ERROR;
        } else {
            status = AttendeesState.Status.LOADING;
        }
        state.setValue(new AttendeesState(status, attendees, busy, errorRes));
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
            return (T) new AttendeesViewModel(eventId, new FirebaseAttendeeRepository());
        }
    }
}
