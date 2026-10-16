package com.itismob.s03.group7.leef.data;

import androidx.annotation.NonNull;

import com.itismob.s03.group7.leef.model.Event;

import java.util.List;
import java.util.Set;

/**
 * Event data the screens rely on. Screens depend on this interface only, so the Firebase code stays
 * in one place ({@link FirebaseEventRepository}).
 */
public interface EventRepository {

    /** Why events couldn't be loaded or saved. */
    enum Failure {
        SIGNED_OUT,
        NETWORK,
        /** The database rules refused the request (for example the rules haven't been published). */
        PERMISSION,
        UNKNOWN
    }

    interface EventsCallback {
        void onLoaded(@NonNull List<Event> events);

        void onFailure(@NonNull Failure failure);
    }

    interface SavedIdsCallback {
        void onLoaded(@NonNull Set<String> eventIds);

        void onFailure(@NonNull Failure failure);
    }

    interface DoneCallback {
        void onDone();

        void onFailure(@NonNull Failure failure);
    }

    /**
     * Events that haven't finished their first day yet (today onwards), soonest first.
     *
     * @param limit the most events to return
     */
    void loadUpcomingEvents(int limit, @NonNull EventsCallback callback);

    /** Ids of the events the signed-in person has saved (the heart on an event card). */
    void loadSavedEventIds(@NonNull SavedIdsCallback callback);

    /** Saves or un-saves an event for the signed-in person. */
    void setEventSaved(@NonNull String eventId, boolean saved, @NonNull DoneCallback callback);
}
