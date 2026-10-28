package com.itismob.s03.group7.leef.data;

import androidx.annotation.NonNull;

import com.itismob.s03.group7.leef.model.Event;
import com.itismob.s03.group7.leef.model.EventDraft;

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
        /** The event doesn't exist (any more), or its document is incomplete. */
        NOT_FOUND,
        /**
         * The event can't be changed or cancelled any more: it isn't the signed-in person's, it has
         * started, or it was already cancelled.
         */
        LOCKED,
        UNKNOWN
    }

    interface EventsCallback {
        void onLoaded(@NonNull List<Event> events);

        void onFailure(@NonNull Failure failure);
    }

    interface EventCallback {
        void onLoaded(@NonNull Event event);

        void onFailure(@NonNull Failure failure);
    }

    interface SavedIdsCallback {
        void onLoaded(@NonNull Set<String> eventIds);

        void onFailure(@NonNull Failure failure);
    }

    interface PostedCallback {
        /** The new event is saved and live; {@code eventId} is its document id. */
        void onPosted(@NonNull String eventId);

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

    /** One event by its id, with every detail (Event Details). */
    void loadEvent(@NonNull String eventId, @NonNull EventCallback callback);

    /**
     * The events with these ids (My Events). An id with no event, or an incomplete document, is simply
     * missing from the result.
     */
    void loadEventsByIds(@NonNull Set<String> eventIds, @NonNull EventsCallback callback);

    /** Ids of the events the signed-in person has saved (the heart on an event card). */
    void loadSavedEventIds(@NonNull SavedIdsCallback callback);

    /** Saves or un-saves an event for the signed-in person. */
    void setEventSaved(@NonNull String eventId, boolean saved, @NonNull DoneCallback callback);

    /**
     * Publishes a new event for the signed-in organizer (Create/Post Event). The event is stamped with
     * the person's uid. Fails with {@link Failure#NETWORK} when offline instead of queueing the write.
     */
    void postEvent(@NonNull EventDraft draft, @NonNull PostedCallback callback);

    /** Every event the signed-in person has posted (at most 50), past ones included. */
    void loadPostedEvents(@NonNull EventsCallback callback);

    /**
     * Replaces the details of an event the signed-in person posted (Edit Event). Fails with
     * {@link Failure#LOCKED} when it has started or was cancelled, {@link Failure#NOT_FOUND} when it is
     * gone, and {@link Failure#NETWORK} when offline instead of queueing the change.
     */
    void updateEvent(@NonNull String eventId, @NonNull EventDraft draft, @NonNull DoneCallback callback);

    /**
     * Cancels an event the signed-in person posted. The event stays on record, marked cancelled, so the
     * people who registered can see what happened. Fails like {@link #updateEvent}.
     */
    void cancelEvent(@NonNull String eventId, @NonNull DoneCallback callback);
}
