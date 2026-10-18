package com.itismob.s03.group7.leef.model;

import androidx.annotation.NonNull;

import java.util.Objects;

/** One message in a chat, stored in Firestore under the chat's {@code messages} collection. */
public final class ChatMessage {

    public static final String COLLECTION = "messages";

    public static final String FIELD_SENDER_ID = "senderId";
    public static final String FIELD_SENDER_NAME = "senderName";
    public static final String FIELD_TEXT = "text";
    public static final String FIELD_SENT_AT = "sentAt";

    /** The longest message a person can send. */
    public static final int MAX_LENGTH = 1000;

    private final String id;
    private final String senderId;
    private final String senderName;
    private final String text;
    private final long sentAtMillis;

    public ChatMessage(@NonNull String id, @NonNull String senderId, @NonNull String senderName,
                       @NonNull String text, long sentAtMillis) {
        this.id = id;
        this.senderId = senderId;
        this.senderName = senderName;
        this.text = text;
        this.sentAtMillis = sentAtMillis;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getSenderId() {
        return senderId;
    }

    @NonNull
    public String getSenderName() {
        return senderName;
    }

    @NonNull
    public String getText() {
        return text;
    }

    public long getSentAtMillis() {
        return sentAtMillis;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ChatMessage)) {
            return false;
        }
        ChatMessage that = (ChatMessage) other;
        return sentAtMillis == that.sentAtMillis && id.equals(that.id) && senderId.equals(that.senderId)
                && senderName.equals(that.senderName) && text.equals(that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, senderId, senderName, text, sentAtMillis);
    }
}
