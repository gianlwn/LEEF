package com.itismob.s03.group7.leef.organizer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.itismob.s03.group7.leef.data.AttendeeRepository;
import com.itismob.s03.group7.leef.data.FirebaseAttendeeRepository;
import com.itismob.s03.group7.leef.model.Attendee;

import java.util.Locale;

/**
 * Checks tickets in at the door. A ticket arrives either as the text of a scanned QR code
 * ("LEEF:LEEF-7KQ2MD:eventId") or as a confirmation code typed by hand ("LEEF-7KQ2MD" or just
 * "7KQ2MD").
 */
public class ScanCheckInViewModel extends ViewModel {

    private static final String QR_PREFIX = "LEEF:";
    private static final String CODE_PREFIX = "LEEF-";

    private final String eventId;
    private final AttendeeRepository repository;
    private final MutableLiveData<ScanCheckInState> state = new MutableLiveData<>();

    ScanCheckInViewModel(@NonNull String eventId, @NonNull AttendeeRepository repository) {
        this.eventId = eventId;
        this.repository = repository;
        state.setValue(new ScanCheckInState(false, ScanCheckInState.Result.NONE, null, 0));
    }

    @NonNull
    public LiveData<ScanCheckInState> getState() {
        return state;
    }

    /** Checks in a scanned QR code or a typed confirmation code. */
    public void submit(@Nullable String raw) {
        ScanCheckInState current = state.getValue();
        if (current != null && current.isBusy()) {
            return;
        }
        String text = raw == null ? "" : raw.trim();
        String code;
        if (text.startsWith(QR_PREFIX)) {
            String rest = text.substring(QR_PREFIX.length());
            int split = rest.lastIndexOf(':');
            if (split <= 0) {
                show(ScanCheckInState.Result.UNREADABLE);
                return;
            }
            if (!rest.substring(split + 1).equals(eventId)) {
                show(ScanCheckInState.Result.WRONG_EVENT);
                return;
            }
            code = rest.substring(0, split);
        } else {
            code = normalizeCode(text);
        }
        if (code == null || !code.startsWith(CODE_PREFIX) || code.length() <= CODE_PREFIX.length()) {
            show(ScanCheckInState.Result.UNREADABLE);
            return;
        }

        state.setValue(new ScanCheckInState(true, ScanCheckInState.Result.NONE, null, 0));
        repository.checkIn(eventId, code, new AttendeeRepository.CheckInCallback() {
            @Override
            public void onResult(@NonNull AttendeeRepository.Outcome outcome, @Nullable Attendee attendee) {
                ScanCheckInState.Result result;
                switch (outcome) {
                    case CHECKED_IN:
                        result = ScanCheckInState.Result.CHECKED_IN;
                        break;
                    case ALREADY_CHECKED_IN:
                        result = ScanCheckInState.Result.ALREADY_CHECKED_IN;
                        break;
                    default:
                        result = ScanCheckInState.Result.NOT_FOUND;
                        break;
                }
                state.setValue(new ScanCheckInState(false, result, attendee, 0));
            }

            @Override
            public void onFailure(@NonNull AttendeeRepository.Failure failure) {
                state.setValue(new ScanCheckInState(false, ScanCheckInState.Result.ERROR, null,
                        OrganizerText.checkInError(failure)));
            }
        });
    }

    private void show(@NonNull ScanCheckInState.Result result) {
        state.setValue(new ScanCheckInState(false, result, null, 0));
    }

    /** "7kq2md", "LEEF 7KQ2MD" and "leef-7kq2md" all become "LEEF-7KQ2MD"; anything else comes back as typed. */
    @NonNull
    static String normalizeCode(@NonNull String typed) {
        String compact = typed.toUpperCase(Locale.ROOT).replaceAll("[\\s-]", "");
        if (compact.startsWith("LEEF")) {
            compact = compact.substring(4);
        }
        return compact.matches("[A-Z0-9]{6}") ? CODE_PREFIX + compact : typed.toUpperCase(Locale.ROOT);
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
            return (T) new ScanCheckInViewModel(eventId, new FirebaseAttendeeRepository());
        }
    }
}
