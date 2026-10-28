package com.itismob.s03.group7.leef.myevents;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.itismob.s03.group7.leef.model.Event;
import com.itismob.s03.group7.leef.model.Registration;
import com.itismob.s03.group7.leef.model.TicketType;

import java.util.Objects;

/**
 * One row on My Events: an event the person registered for or saved. A registration keeps its own copy
 * of the title and start, so the row still works if the event itself has been removed.
 */
public final class MyEventItem {

    /** What the status pill says. */
    public enum Kind {
        REGISTERED,
        /** Registered with a Group (5+) ticket. */
        GROUP_REGISTERED,
        /** Saved with the heart but not registered. */
        SAVED,
        /** The organizer cancelled the event (whether or not the person registered). */
        CANCELLED
    }

    private final String eventId;
    private final String title;
    private final long startsAtMillis;
    @Nullable
    private final Event event;
    private final Kind kind;
    private final boolean upcoming;

    MyEventItem(@NonNull String eventId, @NonNull String title, long startsAtMillis, @Nullable Event event,
                @NonNull Kind kind, boolean upcoming) {
        this.eventId = eventId;
        this.title = title;
        this.startsAtMillis = startsAtMillis;
        this.event = event;
        this.kind = kind;
        this.upcoming = upcoming;
    }

    @NonNull
    static Kind kindOf(@Nullable Registration registration) {
        if (registration == null) {
            return Kind.SAVED;
        }
        return registration.getTicketType() == TicketType.GROUP ? Kind.GROUP_REGISTERED : Kind.REGISTERED;
    }

    @NonNull
    public String getEventId() {
        return eventId;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    public long getStartsAtMillis() {
        return startsAtMillis;
    }

    /** The full event, or null when it has been removed (a registration is still listed). */
    @Nullable
    public Event getEvent() {
        return event;
    }

    @NonNull
    public Kind getKind() {
        return kind;
    }

    /** True until the event is over; upcoming rows go on the Upcoming tab, the rest on Past. */
    public boolean isUpcoming() {
        return upcoming;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MyEventItem)) {
            return false;
        }
        MyEventItem item = (MyEventItem) other;
        return startsAtMillis == item.startsAtMillis
                && upcoming == item.upcoming
                && kind == item.kind
                && eventId.equals(item.eventId)
                && title.equals(item.title)
                && Objects.equals(event, item.event);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, title, startsAtMillis, event, kind, upcoming);
    }
}
