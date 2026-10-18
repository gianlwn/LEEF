package com.itismob.s03.group7.leef.data;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.itismob.s03.group7.leef.model.ChatMessage;
import com.itismob.s03.group7.leef.model.ChatRef;
import com.itismob.s03.group7.leef.model.Conversation;
import com.itismob.s03.group7.leef.model.UserProfile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link MessageRepository} backed by Cloud Firestore (free Spark plan).
 *
 * <ul>
 *   <li>{@code eventChats/{eventId}}: the event chat's latest-message summary; its messages are in
 *       {@code eventChats/{eventId}/messages}. Everyone registered for the event can read and write.</li>
 *   <li>{@code directChats/{uidA_uidB}} (ids sorted): a private chat between two people, with the same
 *       kind of summary and a {@code messages} collection.</li>
 *   <li>{@code users/{uid}/chatReads/{event_id | dm_id}}: when this person last read each chat, used for
 *       the unread badges.</li>
 * </ul>
 */
public final class FirebaseMessageRepository implements MessageRepository {

    private static final String TAG = "LeefMessages";

    private static final String EVENT_CHATS = "eventChats";
    private static final String DIRECT_CHATS = "directChats";
    private static final String CHAT_READS = "chatReads";

    private static final String FIELD_EVENT_TITLE = "eventTitle";
    private static final String FIELD_MEMBERS = "members";
    private static final String FIELD_NAMES = "names";
    private static final String FIELD_LAST_MESSAGE = "lastMessage";
    private static final String FIELD_LAST_SENDER_ID = "lastSenderId";
    private static final String FIELD_LAST_SENDER_NAME = "lastSenderName";
    private static final String FIELD_LAST_MESSAGE_AT = "lastMessageAt";
    private static final String FIELD_LAST_READ_AT = "lastReadAt";

    /** How many of the latest messages a chat shows. */
    private static final int MESSAGE_LIMIT = 200;
    /** The summary line in the list is kept short. */
    private static final int PREVIEW_MAX = 200;

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    public FirebaseMessageRepository() {
        this(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance());
    }

    FirebaseMessageRepository(@NonNull FirebaseAuth auth, @NonNull FirebaseFirestore firestore) {
        this.auth = auth;
        this.firestore = firestore;
    }

    @Nullable
    @Override
    public String getSignedInUid() {
        FirebaseUser user = auth.getCurrentUser();
        return user == null ? null : user.getUid();
    }

    // ---------------------------------------------------------------- list

    /** A conversation before its unread count is known. */
    private static final class Draft {
        final ChatRef ref;
        final String title;
        final String lastMessage;
        final String lastSenderId;
        final String lastSenderName;
        final long lastAt;
        final boolean unread;
        final Timestamp readAt;
        int unreadCount;

        Draft(ChatRef ref, String title, DocumentSnapshot chat, String myUid, Timestamp readAt) {
            this.ref = ref;
            this.title = title;
            this.lastMessage = chat == null ? null : chat.getString(FIELD_LAST_MESSAGE);
            this.lastSenderId = chat == null ? null : chat.getString(FIELD_LAST_SENDER_ID);
            this.lastSenderName = chat == null ? null : chat.getString(FIELD_LAST_SENDER_NAME);
            Timestamp at = chat == null ? null : chat.getTimestamp(FIELD_LAST_MESSAGE_AT);
            this.lastAt = at == null ? 0 : at.toDate().getTime();
            this.readAt = readAt;
            this.unread = lastMessage != null && lastAt > 0 && !myUid.equals(lastSenderId)
                    && (readAt == null || at.compareTo(readAt) > 0);
        }
    }

    @Override
    public void loadConversations(@NonNull Map<String, String> registeredEvents,
                                  @NonNull ConversationsCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onFailure(Failure.SIGNED_OUT);
            return;
        }
        String uid = user.getUid();

        final List<DocumentSnapshot> directChats = new ArrayList<>();
        final Map<String, DocumentSnapshot> eventChats = new HashMap<>();
        final Map<String, Timestamp> reads = new HashMap<>();
        final Exception[] error = {null};
        final int[] pending = {2 + registeredEvents.size()};

        Runnable done = () -> {
            if (--pending[0] > 0) {
                return;
            }
            if (error[0] != null) {
                callback.onFailure(toFailure(error[0]));
                return;
            }
            assemble(uid, registeredEvents, eventChats, directChats, reads, callback);
        };

        firestore.collection(DIRECT_CHATS).whereArrayContains(FIELD_MEMBERS, uid).get()
                .addOnSuccessListener(snapshot -> {
                    directChats.addAll(snapshot.getDocuments());
                    done.run();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "loading direct chats failed", e);
                    error[0] = e;
                    done.run();
                });
        firestore.collection(UserProfile.COLLECTION).document(uid).collection(CHAT_READS).get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        Timestamp readAt = document.getTimestamp(FIELD_LAST_READ_AT);
                        if (readAt != null) {
                            reads.put(document.getId(), readAt);
                        }
                    }
                    done.run();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "loading read marks failed", e);
                    error[0] = e;
                    done.run();
                });
        for (String eventId : registeredEvents.keySet()) {
            firestore.collection(EVENT_CHATS).document(eventId).get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            eventChats.put(eventId, document);
                        }
                        done.run();
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "loading event chat failed", e);
                        error[0] = e;
                        done.run();
                    });
        }
    }

    @SuppressWarnings("unchecked")
    private void assemble(@NonNull String uid, @NonNull Map<String, String> registeredEvents,
                          @NonNull Map<String, DocumentSnapshot> eventChats,
                          @NonNull List<DocumentSnapshot> directChats, @NonNull Map<String, Timestamp> reads,
                          @NonNull ConversationsCallback callback) {
        List<Draft> drafts = new ArrayList<>();
        for (Map.Entry<String, String> event : registeredEvents.entrySet()) {
            String eventId = event.getKey();
            drafts.add(new Draft(ChatRef.event(eventId), event.getValue(), eventChats.get(eventId), uid,
                    reads.get(readKey(ChatRef.event(eventId), uid))));
        }
        for (DocumentSnapshot chat : directChats) {
            Object members = chat.get(FIELD_MEMBERS);
            if (!(members instanceof List)) {
                continue;
            }
            String other = null;
            for (Object member : (List<Object>) members) {
                if (member instanceof String && !uid.equals(member)) {
                    other = (String) member;
                }
            }
            if (other == null) {
                continue;
            }
            Object names = chat.get(FIELD_NAMES);
            String name = names instanceof Map ? (String) ((Map<String, Object>) names).get(other) : null;
            ChatRef ref = ChatRef.direct(other);
            drafts.add(new Draft(ref, name == null || name.isEmpty() ? other : name, chat, uid,
                    reads.get(readKey(ref, uid))));
        }

        // Count the unread messages of the chats that have some.
        final int[] pending = {1};
        Runnable done = () -> {
            if (--pending[0] > 0) {
                return;
            }
            List<Conversation> conversations = new ArrayList<>();
            for (Draft draft : drafts) {
                conversations.add(new Conversation(draft.ref, draft.title, null, draft.lastMessage,
                        draft.lastSenderId, draft.lastSenderName, draft.lastAt, draft.unreadCount));
            }
            callback.onLoaded(conversations);
        };
        for (Draft draft : drafts) {
            if (!draft.unread) {
                continue;
            }
            pending[0]++;
            Query unread = messages(draft.ref, uid);
            if (draft.readAt != null) {
                unread = unread.whereGreaterThan(ChatMessage.FIELD_SENT_AT, draft.readAt);
            }
            unread.count().get(AggregateSource.SERVER)
                    .addOnSuccessListener(result -> {
                        draft.unreadCount = (int) Math.max(1, result.getCount());
                        done.run();
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "counting unread messages failed", e);
                        draft.unreadCount = 1;
                        done.run();
                    });
        }
        done.run();
    }

    // ------------------------------------------------------------ one chat

    @NonNull
    @Override
    public Subscription listenMessages(@NonNull ChatRef ref, @NonNull MessagesListener listener) {
        String uid = getSignedInUid();
        if (uid == null) {
            listener.onFailure(Failure.SIGNED_OUT);
            return () -> { };
        }
        ListenerRegistration registration = messages(ref, uid)
                .orderBy(ChatMessage.FIELD_SENT_AT, Query.Direction.DESCENDING)
                .limit(MESSAGE_LIMIT)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.w(TAG, "listening to messages failed", error);
                        listener.onFailure(toFailure(error));
                        return;
                    }
                    if (snapshot != null) {
                        listener.onMessages(toMessages(snapshot));
                    }
                });
        return registration::remove;
    }

    @NonNull
    private static List<ChatMessage> toMessages(@NonNull QuerySnapshot snapshot) {
        List<ChatMessage> messages = new ArrayList<>();
        for (DocumentSnapshot document : snapshot.getDocuments()) {
            String senderId = document.getString(ChatMessage.FIELD_SENDER_ID);
            String text = document.getString(ChatMessage.FIELD_TEXT);
            if (senderId == null || text == null) {
                continue;
            }
            String senderName = document.getString(ChatMessage.FIELD_SENDER_NAME);
            // A message still being sent has no server time yet; use the phone's estimate.
            Timestamp sentAt = document.getTimestamp(ChatMessage.FIELD_SENT_AT,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);
            long millis = sentAt == null ? System.currentTimeMillis() : sentAt.toDate().getTime();
            messages.add(new ChatMessage(document.getId(), senderId, senderName == null ? "" : senderName,
                    text, millis));
        }
        Collections.reverse(messages); // They were fetched newest first.
        return messages;
    }

    @Override
    public void sendMessage(@NonNull ChatRef ref, @NonNull String chatTitle, @NonNull String senderName,
                            @NonNull String otherName, @NonNull String text, @NonNull DoneCallback callback) {
        String uid = getSignedInUid();
        if (uid == null) {
            callback.onFailure(Failure.SIGNED_OUT);
            return;
        }
        String body = text.trim();
        if (body.isEmpty()) {
            callback.onDone();
            return;
        }
        if (body.length() > ChatMessage.MAX_LENGTH) {
            body = body.substring(0, ChatMessage.MAX_LENGTH);
        }

        DocumentReference chat = chatDocument(ref, uid);
        Map<String, Object> summary = new HashMap<>();
        if (ref.isEvent()) {
            summary.put(FIELD_EVENT_TITLE, chatTitle);
        } else {
            String other = ref.getTargetId();
            String first = uid.compareTo(other) < 0 ? uid : other;
            String second = uid.compareTo(other) < 0 ? other : uid;
            summary.put(FIELD_MEMBERS, Arrays.asList(first, second));
            Map<String, Object> names = new HashMap<>();
            names.put(uid, senderName);
            names.put(other, otherName);
            summary.put(FIELD_NAMES, names);
        }
        summary.put(FIELD_LAST_MESSAGE, preview(body));
        summary.put(FIELD_LAST_SENDER_ID, uid);
        summary.put(FIELD_LAST_SENDER_NAME, senderName);
        summary.put(FIELD_LAST_MESSAGE_AT, FieldValue.serverTimestamp());

        Map<String, Object> message = new HashMap<>();
        message.put(ChatMessage.FIELD_SENDER_ID, uid);
        message.put(ChatMessage.FIELD_SENDER_NAME, senderName);
        message.put(ChatMessage.FIELD_TEXT, body);
        message.put(ChatMessage.FIELD_SENT_AT, FieldValue.serverTimestamp());

        Map<String, Object> read = new HashMap<>();
        read.put(FIELD_LAST_READ_AT, FieldValue.serverTimestamp());

        WriteBatch batch = firestore.batch();
        batch.set(chat, summary, SetOptions.merge());
        batch.set(chat.collection(ChatMessage.COLLECTION).document(), message);
        batch.set(readDocument(ref, uid), read);
        batch.commit()
                .addOnSuccessListener(unused -> callback.onDone())
                .addOnFailureListener(e -> {
                    Log.w(TAG, "sending a message failed", e);
                    callback.onFailure(toFailure(e));
                });
    }

    @Override
    public void markRead(@NonNull ChatRef ref) {
        String uid = getSignedInUid();
        if (uid == null) {
            return;
        }
        Map<String, Object> read = new HashMap<>();
        read.put(FIELD_LAST_READ_AT, FieldValue.serverTimestamp());
        readDocument(ref, uid).set(read)
                .addOnFailureListener(e -> Log.w(TAG, "marking a chat read failed", e));
    }

    // -------------------------------------------------------------- helpers

    @NonNull
    private DocumentReference chatDocument(@NonNull ChatRef ref, @NonNull String uid) {
        if (ref.isEvent()) {
            return firestore.collection(EVENT_CHATS).document(ref.getTargetId());
        }
        return firestore.collection(DIRECT_CHATS).document(directChatId(uid, ref.getTargetId()));
    }

    @NonNull
    private CollectionReference messages(@NonNull ChatRef ref, @NonNull String uid) {
        return chatDocument(ref, uid).collection(ChatMessage.COLLECTION);
    }

    @NonNull
    private DocumentReference readDocument(@NonNull ChatRef ref, @NonNull String uid) {
        return firestore.collection(UserProfile.COLLECTION).document(uid)
                .collection(CHAT_READS).document(readKey(ref, uid));
    }

    /** The two ids in alphabetical order, so both people end up in the same chat. */
    @NonNull
    static String directChatId(@NonNull String uidA, @NonNull String uidB) {
        return uidA.compareTo(uidB) < 0 ? uidA + "_" + uidB : uidB + "_" + uidA;
    }

    @NonNull
    private static String readKey(@NonNull ChatRef ref, @NonNull String uid) {
        return ref.isEvent() ? "event_" + ref.getTargetId() : "dm_" + directChatId(uid, ref.getTargetId());
    }

    /** One line of the message for the list. */
    @NonNull
    private static String preview(@NonNull String text) {
        String line = text.replaceAll("\\s+", " ").trim();
        return line.length() > PREVIEW_MAX ? line.substring(0, PREVIEW_MAX) : line;
    }

    @NonNull
    private static Failure toFailure(@NonNull Exception error) {
        if (error instanceof FirebaseFirestoreException) {
            switch (((FirebaseFirestoreException) error).getCode()) {
                case UNAVAILABLE:
                case DEADLINE_EXCEEDED:
                    return Failure.NETWORK;
                case PERMISSION_DENIED:
                    return Failure.PERMISSION;
                default:
                    return Failure.UNKNOWN;
            }
        }
        return Failure.UNKNOWN;
    }
}
