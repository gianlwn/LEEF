package com.itismob.s03.group7.leef.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/** One row of the Messages list: a chat, its latest message and how many messages are unread. */
public final class Conversation {

    private final ChatRef ref;
    /** The event's title (event chat) or the other person's name (direct chat). */
    private final String title;
    @Nullable
    private final String coverUrl;
    @Nullable
    private final String lastMessage;
    @Nullable
    private final String lastSenderId;
    @Nullable
    private final String lastSenderName;
    /** When the latest message was sent, or 0 if nobody has written yet. */
    private final long lastMessageAtMillis;
    private final int unreadCount;

    public Conversation(@NonNull ChatRef ref, @NonNull String title, @Nullable String coverUrl,
                        @Nullable String lastMessage, @Nullable String lastSenderId,
                        @Nullable String lastSenderName, long lastMessageAtMillis, int unreadCount) {
        this.ref = ref;
        this.title = title;
        this.coverUrl = coverUrl;
        this.lastMessage = lastMessage;
        this.lastSenderId = lastSenderId;
        this.lastSenderName = lastSenderName;
        this.lastMessageAtMillis = lastMessageAtMillis;
        this.unreadCount = unreadCount;
    }

    /** The same conversation with the event's photo filled in. */
    @NonNull
    public Conversation withCover(@Nullable String url) {
        return new Conversation(ref, title, url, lastMessage, lastSenderId, lastSenderName,
                lastMessageAtMillis, unreadCount);
    }

    @NonNull
    public ChatRef getRef() {
        return ref;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @Nullable
    public String getCoverUrl() {
        return coverUrl;
    }

    @Nullable
    public String getLastMessage() {
        return lastMessage;
    }

    @Nullable
    public String getLastSenderId() {
        return lastSenderId;
    }

    @Nullable
    public String getLastSenderName() {
        return lastSenderName;
    }

    public long getLastMessageAtMillis() {
        return lastMessageAtMillis;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public boolean hasMessages() {
        return lastMessage != null && lastMessageAtMillis > 0;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Conversation)) {
            return false;
        }
        Conversation that = (Conversation) other;
        return lastMessageAtMillis == that.lastMessageAtMillis && unreadCount == that.unreadCount
                && ref.equals(that.ref) && title.equals(that.title)
                && Objects.equals(coverUrl, that.coverUrl) && Objects.equals(lastMessage, that.lastMessage)
                && Objects.equals(lastSenderId, that.lastSenderId)
                && Objects.equals(lastSenderName, that.lastSenderName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ref, title, coverUrl, lastMessage, lastSenderId, lastSenderName,
                lastMessageAtMillis, unreadCount);
    }
}
