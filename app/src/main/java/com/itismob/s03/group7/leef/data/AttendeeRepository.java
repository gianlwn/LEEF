package com.itismob.s03.group7.leef.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.itismob.s03.group7.leef.model.Attendee;

import java.util.List;

/** What an organizer can do with the people registered for their event. Firebase code lives in {@link FirebaseAttendeeRepository}. */
public interface AttendeeRepository {

    enum Failure {
        SIGNED_OUT,
        NETWORK,
        /** The database rules refused the request (not the organizer, or the rules aren't published). */
        PERMISSION,
        UNKNOWN
    }

    /** What happened when a ticket code was checked in. */
    enum Outcome {
        CHECKED_IN,
        ALREADY_CHECKED_IN,
        /** No registration for this event has that code. */
        NOT_FOUND
    }

    interface ListCallback {
        void onLoaded(@NonNull List<Attendee> attendees);

        void onFailure(@NonNull Failure failure);
    }

    interface CheckInCallback {
        /** @param attendee the registration, or null when the outcome is {@link Outcome#NOT_FOUND} */
        void onResult(@NonNull Outcome outcome, @Nullable Attendee attendee);

        void onFailure(@NonNull Failure failure);
    }

    /** Everyone registered for the event (the signed-in person must be its organizer). */
    void loadAttendees(@NonNull String eventId, @NonNull ListCallback callback);

    /** Marks the registration with this confirmation code as checked in. Checking in twice changes nothing. */
    void checkIn(@NonNull String eventId, @NonNull String confirmationCode, @NonNull CheckInCallback callback);
}
