package com.itismob.s03.group7.leef.messages;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.itismob.s03.group7.leef.data.MessageRepository;
import com.itismob.s03.group7.leef.model.Conversation;

import java.util.Collections;
import java.util.List;

/** Everything the Messages list draws at one moment. */
public final class MessagesState {

    public enum Status {
        LOADING,
        /** Couldn't load for the first time; see {@link #getFailure()}. */
        ERROR,
        CONTENT
    }

    private final Status status;
    private final boolean refreshing;
    private final boolean hasAny;
    private final String query;
    private final List<Conversation> visible;
    @Nullable
    private final MessageRepository.Failure failure;

    MessagesState(@NonNull Status status, boolean refreshing, boolean hasAny, @NonNull String query,
                  @NonNull List<Conversation> visible, @Nullable MessageRepository.Failure failure) {
        this.status = status;
        this.refreshing = refreshing;
        this.hasAny = hasAny;
        this.query = query;
        this.visible = Collections.unmodifiableList(visible);
        this.failure = failure;
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    public boolean isRefreshing() {
        return refreshing;
    }

    /** False when the person has no conversations at all (as opposed to the search matching none). */
    public boolean hasAny() {
        return hasAny;
    }

    @NonNull
    public String getQuery() {
        return query;
    }

    /** The conversations that match the search, newest first. */
    @NonNull
    public List<Conversation> getVisible() {
        return visible;
    }

    @Nullable
    public MessageRepository.Failure getFailure() {
        return failure;
    }
}
