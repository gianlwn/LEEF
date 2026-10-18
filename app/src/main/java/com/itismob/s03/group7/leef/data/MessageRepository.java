package com.itismob.s03.group7.leef.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.itismob.s03.group7.leef.model.ChatMessage;
import com.itismob.s03.group7.leef.model.ChatRef;
import com.itismob.s03.group7.leef.model.Conversation;

import java.util.List;
import java.util.Map;

/** Chats the Messaging screens rely on; the Firebase code lives in {@link FirebaseMessageRepository}. */
public interface MessageRepository {

    /** Why chats couldn't be loaded or a message couldn't be sent. */
    enum Failure {
        SIGNED_OUT,
        NETWORK,
        /** The database rules refused the request (not registered for the event, or rules not published). */
        PERMISSION,
        UNKNOWN
    }

    interface ConversationsCallback {
        void onLoaded(@NonNull List<Conversation> conversations);

        void onFailure(@NonNull Failure failure);
    }

    interface MessagesListener {
        /** The latest messages, oldest first. Called again whenever something changes. */
        void onMessages(@NonNull List<ChatMessage> messages);

        void onFailure(@NonNull Failure failure);
    }

    interface DoneCallback {
        void onDone();

        void onFailure(@NonNull Failure failure);
    }

    /** Stops a live listener. */
    interface Subscription {
        void cancel();
    }

    /** The signed-in person's user id, or null when signed out. */
    @Nullable
    String getSignedInUid();

    /**
     * The person's conversations: the chat of every event they registered for (even with no messages yet)
     * plus their direct chats, each with its latest message and unread count.
     *
     * @param registeredEvents event id to event title, for the events the person registered for
     */
    void loadConversations(@NonNull Map<String, String> registeredEvents, @NonNull ConversationsCallback callback);

    /** Live messages of one chat (the latest 200), oldest first. */
    @NonNull
    Subscription listenMessages(@NonNull ChatRef ref, @NonNull MessagesListener listener);

    /**
     * Sends a message and updates the chat's latest-message summary.
     *
     * @param chatTitle  the event's title (event chat); ignored for a direct chat
     * @param senderName the sender's full name
     * @param otherName  the other person's name (direct chat); ignored for an event chat
     */
    void sendMessage(@NonNull ChatRef ref, @NonNull String chatTitle, @NonNull String senderName,
                     @NonNull String otherName, @NonNull String text, @NonNull DoneCallback callback);

    /** Remembers that the person has read the chat up to now (clears its unread badge). */
    void markRead(@NonNull ChatRef ref);
}
