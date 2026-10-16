package com.itismob.s03.group7.leef.home;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.itismob.s03.group7.leef.model.Event;

/** An event as shown on a Home card: the event plus whether the person has saved it (the heart). */
public final class EventItem {

    private final Event event;
    private final boolean saved;

    public EventItem(@NonNull Event event, boolean saved) {
        this.event = event;
        this.saved = saved;
    }

    @NonNull
    public Event getEvent() {
        return event;
    }

    public boolean isSaved() {
        return saved;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof EventItem)) {
            return false;
        }
        EventItem item = (EventItem) other;
        return saved == item.saved && event.equals(item.event);
    }

    @Override
    public int hashCode() {
        return 31 * event.hashCode() + (saved ? 1 : 0);
    }
}
