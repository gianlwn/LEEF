package com.itismob.s03.group7.leef.messages;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.itismob.s03.group7.leef.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** One line of a chat: a day heading, or a message (mine or someone else's). */
final class ChatRow {

    enum Type {
        DAY,
        MINE,
        OTHER
    }

    final Type type;
    /** Unique within the chat; lets the list animate and keep its place. */
    final String id;
    @Nullable
    final String dayText;
    @Nullable
    final ChatMessage message;
    /** In an event chat, show who wrote someone else's message (only on the first of a run). */
    final boolean showSender;

    private ChatRow(@NonNull Type type, @NonNull String id, @Nullable String dayText,
                    @Nullable ChatMessage message, boolean showSender) {
        this.type = type;
        this.id = id;
        this.dayText = dayText;
        this.message = message;
        this.showSender = showSender;
    }

    /** The rows for a chat: messages oldest first with a heading whenever the day changes. */
    @NonNull
    static List<ChatRow> build(@NonNull Context context, @NonNull List<ChatMessage> messages,
                               @NonNull String myUid, boolean groupChat) {
        List<ChatRow> rows = new ArrayList<>();
        ChatMessage previous = null;
        for (ChatMessage message : messages) {
            boolean newDay = previous == null || !ChatFormat.sameDay(previous.getSentAtMillis(), message.getSentAtMillis());
            if (newDay) {
                rows.add(new ChatRow(Type.DAY, "day_" + message.getSentAtMillis() / 86_400_000L + "_" + message.getId(),
                        ChatFormat.dayHeading(context, message.getSentAtMillis()), null, false));
            }
            boolean mine = message.getSenderId().equals(myUid);
            boolean firstOfRun = newDay || !previous.getSenderId().equals(message.getSenderId());
            rows.add(new ChatRow(mine ? Type.MINE : Type.OTHER, message.getId(), null, message,
                    groupChat && !mine && firstOfRun));
            previous = message;
        }
        return rows;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ChatRow)) {
            return false;
        }
        ChatRow that = (ChatRow) other;
        return type == that.type && showSender == that.showSender && id.equals(that.id)
                && Objects.equals(dayText, that.dayText) && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, id, dayText, message, showSender);
    }
}
