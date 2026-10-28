package com.itismob.s03.group7.leef.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * One line of the notifications list. There is no server to send notifications on the free plan, so the
 * list is worked out from data the app can already read: the person's registrations, those events, their
 * chats and their ratings. Nothing here is stored in the database.
 */
public final class AppNotification {

    public enum Kind {
        /** "You're registered for ..." (informational, never counted as new). */
        REGISTERED,
        /** The organizer cancelled an event the person registered for. */
        EVENT_CANCELLED,
        /** The organizer changed the details of an event the person registered for. */
        EVENT_CHANGED,
        /** A registered event starts within a day. */
        STARTING_SOON,
        /** A chat has messages the person hasn't read. */
        NEW_MESSAGES,
        /** A registered event is over and the person hasn't rated it. */
        RATE_EVENT
    }

    private final String id;
    private final Kind kind;
    /** The event's title, or the chat's title for {@link Kind#NEW_MESSAGES}. */
    private final String title;
    /** The event this is about; null for a direct chat. */
    @Nullable
    private final String eventId;
    /** The chat to open for {@link Kind#NEW_MESSAGES}, otherwise null. */
    @Nullable
    private final ChatRef chatRef;
    @Nullable
    private final String coverUrl;
    /** Unread messages for {@link Kind#NEW_MESSAGES}; 0 otherwise. */
    private final int count;
    /** When it happened (or became relevant); the list is newest first. */
    private final long timeMillis;
    /** The event's start for {@link Kind#STARTING_SOON}; 0 otherwise. */
    private final long startsAtMillis;

    public AppNotification(@NonNull String id, @NonNull Kind kind, @NonNull String title, @Nullable String eventId,
                           @Nullable ChatRef chatRef, @Nullable String coverUrl, int count, long timeMillis,
                           long startsAtMillis) {
        this.id = id;
        this.kind = kind;
        this.title = title;
        this.eventId = eventId;
        this.chatRef = chatRef;
        this.coverUrl = coverUrl;
        this.count = count;
        this.timeMillis = timeMillis;
        this.startsAtMillis = startsAtMillis;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public Kind getKind() {
        return kind;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @Nullable
    public String getEventId() {
        return eventId;
    }

    @Nullable
    public ChatRef getChatRef() {
        return chatRef;
    }

    @Nullable
    public String getCoverUrl() {
        return coverUrl;
    }

    public int getCount() {
        return count;
    }

    public long getTimeMillis() {
        return timeMillis;
    }

    public long getStartsAtMillis() {
        return startsAtMillis;
    }

    /** True when this happened after the person last opened the list. A confirmation is never "new". */
    public boolean isUnread(long lastReadMillis) {
        return kind != Kind.REGISTERED && timeMillis > lastReadMillis;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AppNotification)) {
            return false;
        }
        AppNotification that = (AppNotification) other;
        return count == that.count && timeMillis == that.timeMillis && startsAtMillis == that.startsAtMillis
                && id.equals(that.id) && kind == that.kind && title.equals(that.title)
                && Objects.equals(eventId, that.eventId) && Objects.equals(chatRef, that.chatRef)
                && Objects.equals(coverUrl, that.coverUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, kind, title, eventId, chatRef, coverUrl, count, timeMillis, startsAtMillis);
    }
}
