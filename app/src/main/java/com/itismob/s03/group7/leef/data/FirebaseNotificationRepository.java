package com.itismob.s03.group7.leef.data;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.itismob.s03.group7.leef.model.AppNotification;
import com.itismob.s03.group7.leef.model.ChatRef;
import com.itismob.s03.group7.leef.model.Conversation;
import com.itismob.s03.group7.leef.model.Event;
import com.itismob.s03.group7.leef.model.Feedback;
import com.itismob.s03.group7.leef.model.Registration;
import com.itismob.s03.group7.leef.model.UserProfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * {@link NotificationRepository} backed by Cloud Firestore (free Spark plan). Nothing is stored for the
 * notifications themselves: the list is worked out on the phone from what the person can already read,
 * and the only thing saved is when they last opened it, at {@code users/{uid}/notificationState/read}.
 */
public final class FirebaseNotificationRepository implements NotificationRepository {

    private static final String TAG = "LeefNotifications";
    private static final String STATE_COLLECTION = "notificationState";
    private static final String STATE_DOCUMENT = "read";
    private static final String FIELD_LAST_READ_AT = "lastReadAt";

    /** A registered event counts as "starting soon" this long before it begins. */
    private static final long SOON_MILLIS = TimeUnit.HOURS.toMillis(24);
    /** Events that ended longer ago than this are no longer offered for rating. */
    private static final long RATE_WINDOW_MILLIS = TimeUnit.DAYS.toMillis(30);
    private static final int MAX_RATE_CHECKS = 5;
    private static final int MAX_ITEMS = 40;

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private final RegistrationRepository registrations;
    private final EventRepository events;
    private final MessageRepository messages;
    private final FeedbackRepository feedback;

    public FirebaseNotificationRepository() {
        this(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance(), new FirebaseRegistrationRepository(),
                new FirebaseEventRepository(), new FirebaseMessageRepository(), new FirebaseFeedbackRepository());
    }

    FirebaseNotificationRepository(@NonNull FirebaseAuth auth, @NonNull FirebaseFirestore firestore,
                                   @NonNull RegistrationRepository registrations, @NonNull EventRepository events,
                                   @NonNull MessageRepository messages, @NonNull FeedbackRepository feedback) {
        this.auth = auth;
        this.firestore = firestore;
        this.registrations = registrations;
        this.events = events;
        this.messages = messages;
        this.feedback = feedback;
    }

    @Override
    public void loadNotifications(@NonNull LoadCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onFailure(Failure.SIGNED_OUT);
            return;
        }
        registrations.loadRegistrations(new RegistrationRepository.ListCallback() {
            @Override
            public void onLoaded(@NonNull List<Registration> loaded) {
                new Work(user.getUid(), loaded, callback).start();
            }

            @Override
            public void onFailure(@NonNull RegistrationRepository.Failure failure) {
                Log.w(TAG, "loading registrations for notifications failed: " + failure);
                callback.onFailure(failure == RegistrationRepository.Failure.NETWORK ? Failure.NETWORK
                        : failure == RegistrationRepository.Failure.PERMISSION ? Failure.PERMISSION
                        : Failure.UNKNOWN);
            }
        });
    }

    @Override
    public void markAllRead(@NonNull DoneCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onFailure(Failure.SIGNED_OUT);
            return;
        }
        DocumentReference reference = stateReference(user.getUid());
        Map<String, Object> document = new HashMap<>();
        document.put(FIELD_LAST_READ_AT, FieldValue.serverTimestamp());
        // A transaction (not a plain set) so being offline fails right away instead of queueing.
        firestore.runTransaction(transaction -> {
            transaction.set(reference, document);
            return null;
        }).addOnSuccessListener(unused -> callback.onDone())
                .addOnFailureListener(error -> {
                    Log.w(TAG, "marking notifications read failed", error);
                    callback.onFailure(toFailure(error));
                });
    }

    @NonNull
    private DocumentReference stateReference(@NonNull String uid) {
        return firestore.collection(UserProfile.COLLECTION).document(uid)
                .collection(STATE_COLLECTION).document(STATE_DOCUMENT);
    }

    /**
     * One load. The registrations are already known; the rest (when the list was last opened, the events,
     * the chats, the ratings) is read in parallel, and anything that fails only leaves its own lines out.
     */
    private final class Work {
        private final String uid;
        private final List<Registration> registered;
        private final LoadCallback callback;

        private long lastRead;
        private final Map<String, Event> eventsById = new HashMap<>();
        private final List<Conversation> conversations = new ArrayList<>();
        private final Set<String> needsRating = new HashSet<>();
        private int pending = 3;

        Work(@NonNull String uid, @NonNull List<Registration> registered, @NonNull LoadCallback callback) {
            this.uid = uid;
            this.registered = registered;
            this.callback = callback;
        }

        void start() {
            readLastOpened();
            loadEventsThenRatings();
            loadChats();
        }

        private synchronized void branchDone() {
            if (--pending > 0) {
                return;
            }
            callback.onLoaded(assemble(), lastRead);
        }

        private void readLastOpened() {
            stateReference(uid).get()
                    .addOnSuccessListener(document -> {
                        Timestamp readAt = document.getTimestamp(FIELD_LAST_READ_AT);
                        lastRead = readAt == null ? 0 : readAt.toDate().getTime();
                        branchDone();
                    })
                    .addOnFailureListener(error -> {
                        Log.w(TAG, "reading the last-opened time failed", error);
                        branchDone();
                    });
        }

        private void loadEventsThenRatings() {
            Set<String> ids = new HashSet<>();
            for (Registration registration : registered) {
                ids.add(registration.getEventId());
            }
            if (ids.isEmpty()) {
                branchDone();
                return;
            }
            events.loadEventsByIds(ids, new EventRepository.EventsCallback() {
                @Override
                public void onLoaded(@NonNull List<Event> found) {
                    for (Event event : found) {
                        eventsById.put(event.getId(), event);
                    }
                    checkRatings();
                }

                @Override
                public void onFailure(@NonNull EventRepository.Failure failure) {
                    Log.w(TAG, "loading events for notifications failed: " + failure);
                    branchDone();
                }
            });
        }

        /** Looks for recent events that ended without a rating from this person. */
        private void checkRatings() {
            long now = System.currentTimeMillis();
            List<Event> candidates = new ArrayList<>();
            for (Event event : eventsById.values()) {
                long end = endOf(event);
                if (!event.isCancelled() && event.hasEnded(now) && now - end < RATE_WINDOW_MILLIS) {
                    candidates.add(event);
                }
            }
            Collections.sort(candidates, (a, b) -> Long.compare(endOf(b), endOf(a)));
            if (candidates.size() > MAX_RATE_CHECKS) {
                candidates = new ArrayList<>(candidates.subList(0, MAX_RATE_CHECKS));
            }
            if (candidates.isEmpty()) {
                branchDone();
                return;
            }
            final int[] left = {candidates.size()};
            for (Event event : candidates) {
                feedback.loadMine(event.getId(), new FeedbackRepository.MineCallback() {
                    @Override
                    public void onLoaded(@Nullable Feedback mine) {
                        if (mine == null) {
                            synchronized (Work.this) {
                                needsRating.add(event.getId());
                            }
                        }
                        ratingChecked();
                    }

                    @Override
                    public void onFailure(@NonNull FeedbackRepository.Failure failure) {
                        ratingChecked();
                    }

                    private void ratingChecked() {
                        boolean last;
                        synchronized (Work.this) {
                            last = --left[0] == 0;
                        }
                        if (last) {
                            branchDone();
                        }
                    }
                });
            }
        }

        private void loadChats() {
            // Organizers also have the chat of every event they posted (the Messages tab lists them too).
            events.loadPostedEvents(new EventRepository.EventsCallback() {
                @Override
                public void onLoaded(@NonNull List<Event> posted) {
                    loadChats(posted);
                }

                @Override
                public void onFailure(@NonNull EventRepository.Failure failure) {
                    loadChats(new ArrayList<>());
                }
            });
        }

        private void loadChats(@NonNull List<Event> posted) {
            Map<String, String> titles = new HashMap<>();
            for (Registration registration : registered) {
                titles.put(registration.getEventId(), registration.getEventTitle());
            }
            for (Event event : posted) {
                titles.put(event.getId(), event.getTitle());
            }
            messages.loadConversations(titles, new MessageRepository.ConversationsCallback() {
                @Override
                public void onLoaded(@NonNull List<Conversation> result) {
                    synchronized (Work.this) {
                        conversations.addAll(result);
                    }
                    branchDone();
                }

                @Override
                public void onFailure(@NonNull MessageRepository.Failure failure) {
                    Log.w(TAG, "loading chats for notifications failed: " + failure);
                    branchDone();
                }
            });
        }

        @NonNull
        private List<AppNotification> assemble() {
            long now = System.currentTimeMillis();
            List<AppNotification> items = new ArrayList<>();
            for (Registration registration : registered) {
                String eventId = registration.getEventId();
                Event event = eventsById.get(eventId);
                String title = event != null ? event.getTitle() : registration.getEventTitle();
                String cover = event == null ? null : event.getCoverUrl();

                items.add(new AppNotification("registered_" + eventId, AppNotification.Kind.REGISTERED, title,
                        eventId, null, cover, 0, registration.getRegisteredAtMillis(), 0));
                if (event == null) {
                    continue;
                }
                if (event.isCancelled()) {
                    long when = event.getCancelledAtMillis() > 0 ? event.getCancelledAtMillis()
                            : Math.max(event.getUpdatedAtMillis(), 1);
                    items.add(new AppNotification("cancelled_" + eventId, AppNotification.Kind.EVENT_CANCELLED,
                            title, eventId, null, cover, 0, when, 0));
                    continue;
                }
                boolean ended = event.hasEnded(now);
                if (!ended && event.getUpdatedAtMillis() > registration.getRegisteredAtMillis()) {
                    items.add(new AppNotification("changed_" + eventId, AppNotification.Kind.EVENT_CHANGED,
                            title, eventId, null, cover, 0, event.getUpdatedAtMillis(), 0));
                }
                long startsAt = event.getStartsAtMillis();
                if (startsAt > now && startsAt - now <= SOON_MILLIS) {
                    items.add(new AppNotification("soon_" + eventId, AppNotification.Kind.STARTING_SOON, title,
                            eventId, null, cover, 0, startsAt - SOON_MILLIS, startsAt));
                }
                if (needsRating.contains(eventId)) {
                    items.add(new AppNotification("rate_" + eventId, AppNotification.Kind.RATE_EVENT, title,
                            eventId, null, cover, 0, endOf(event), 0));
                }
            }
            for (Conversation conversation : conversations) {
                if (conversation.getUnreadCount() <= 0 || !conversation.hasMessages()) {
                    continue;
                }
                ChatRef ref = conversation.getRef();
                Event event = ref.isEvent() ? eventsById.get(ref.getTargetId()) : null;
                items.add(new AppNotification("messages_" + ref.getKind() + "_" + ref.getTargetId(),
                        AppNotification.Kind.NEW_MESSAGES, conversation.getTitle(),
                        ref.isEvent() ? ref.getTargetId() : null, ref,
                        event != null ? event.getCoverUrl() : conversation.getCoverUrl(),
                        conversation.getUnreadCount(), conversation.getLastMessageAtMillis(), 0));
            }
            Collections.sort(items, (a, b) -> Long.compare(b.getTimeMillis(), a.getTimeMillis()));
            return items.size() > MAX_ITEMS ? new ArrayList<>(items.subList(0, MAX_ITEMS)) : items;
        }
    }

    /** When the event is over: its end time, or the end of its start day when it has none. */
    private static long endOf(@NonNull Event event) {
        if (event.getEndsAtMillis() > event.getStartsAtMillis()) {
            return event.getEndsAtMillis();
        }
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTimeInMillis(event.getStartsAtMillis());
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23);
        calendar.set(java.util.Calendar.MINUTE, 59);
        calendar.set(java.util.Calendar.SECOND, 59);
        return calendar.getTimeInMillis();
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
