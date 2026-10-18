package com.itismob.s03.group7.leef.model;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * Which chat a screen is talking about: the group chat of an event (everyone registered for it), or a
 * direct chat with one other person. Only the ids are kept here; where the chat lives in Firestore is
 * the repository's business.
 */
public final class ChatRef {

    public enum Kind {
        /** The group chat of one event. */
        EVENT,
        /** A private chat between two people. */
        DIRECT
    }

    private final Kind kind;
    private final String targetId;

    private ChatRef(@NonNull Kind kind, @NonNull String targetId) {
        this.kind = kind;
        this.targetId = targetId;
    }

    /** The chat of the event with this id. */
    @NonNull
    public static ChatRef event(@NonNull String eventId) {
        return new ChatRef(Kind.EVENT, eventId);
    }

    /** A direct chat with the person who has this user id. */
    @NonNull
    public static ChatRef direct(@NonNull String otherUserId) {
        return new ChatRef(Kind.DIRECT, otherUserId);
    }

    @NonNull
    public Kind getKind() {
        return kind;
    }

    /** The event id (event chat) or the other person's user id (direct chat). */
    @NonNull
    public String getTargetId() {
        return targetId;
    }

    public boolean isEvent() {
        return kind == Kind.EVENT;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ChatRef)) {
            return false;
        }
        ChatRef that = (ChatRef) other;
        return kind == that.kind && targetId.equals(that.targetId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, targetId);
    }
}
