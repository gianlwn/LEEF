package com.itismob.s03.group7.leef.messages;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.itismob.s03.group7.leef.data.MessageRepository;
import com.itismob.s03.group7.leef.model.ChatMessage;

import java.util.Collections;
import java.util.List;

/** Everything one chat screen draws at one moment. */
public final class ChatState {

    public enum Status {
        LOADING,
        /** Couldn't open the chat; see {@link #getFailure()}. */
        ERROR,
        CONTENT
    }

    private final Status status;
    private final List<ChatMessage> messages;
    @Nullable
    private final MessageRepository.Failure failure;

    ChatState(@NonNull Status status, @NonNull List<ChatMessage> messages,
              @Nullable MessageRepository.Failure failure) {
        this.status = status;
        this.messages = Collections.unmodifiableList(messages);
        this.failure = failure;
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    /** The latest messages, oldest first. */
    @NonNull
    public List<ChatMessage> getMessages() {
        return messages;
    }

    @Nullable
    public MessageRepository.Failure getFailure() {
        return failure;
    }
}
