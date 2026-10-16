package com.itismob.s03.group7.leef.data;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.itismob.s03.group7.leef.model.Event;
import com.itismob.s03.group7.leef.model.EventAudience;
import com.itismob.s03.group7.leef.model.EventCategory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link EventRepository} backed by Cloud Firestore (free Spark plan).
 *
 * <p>Events live in {@code events/{eventId}}; a person's saved events live in
 * {@code users/{uid}/savedEvents/{eventId}}.
 */
public final class FirebaseEventRepository implements EventRepository {

    private static final String TAG = "LeefEvents";
    private static final String SAVED_COLLECTION = "savedEvents";
    private static final String FIELD_SAVED_AT = "savedAt";

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    public FirebaseEventRepository() {
        this(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance());
    }

    FirebaseEventRepository(@NonNull FirebaseAuth auth, @NonNull FirebaseFirestore firestore) {
        this.auth = auth;
        this.firestore = firestore;
    }

    @Override
    public void loadUpcomingEvents(int limit, @NonNull EventsCallback callback) {
        if (auth.getCurrentUser() == null) {
            callback.onFailure(Failure.SIGNED_OUT);
            return;
        }
        Query query = firestore.collection(Event.COLLECTION)
                .whereGreaterThanOrEqualTo(Event.FIELD_STARTS_AT, new Timestamp(startOfTodayMillis() / 1000, 0))
                .orderBy(Event.FIELD_STARTS_AT)
                .limit(limit);
        query.get()
                .addOnSuccessListener(snapshot -> {
                    List<Event> events = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        Event event = toEvent(document);
                        if (event != null) {
                            events.add(event);
                        }
                    }
                    callback.onLoaded(events);
                })
                .addOnFailureListener(error -> {
                    Log.w(TAG, "loading events failed", error);
                    callback.onFailure(toFailure(error));
                });
    }

    @Override
    public void loadSavedEventIds(@NonNull SavedIdsCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onFailure(Failure.SIGNED_OUT);
            return;
        }
        savedCollection(user).get()
                .addOnSuccessListener(snapshot -> {
                    Set<String> ids = new HashSet<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        ids.add(document.getId());
                    }
                    callback.onLoaded(ids);
                })
                .addOnFailureListener(error -> {
                    Log.w(TAG, "loading saved events failed", error);
                    callback.onFailure(toFailure(error));
                });
    }

    @Override
    public void setEventSaved(@NonNull String eventId, boolean saved, @NonNull DoneCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onFailure(Failure.SIGNED_OUT);
            return;
        }
        com.google.android.gms.tasks.Task<Void> task;
        if (saved) {
            Map<String, Object> document = new HashMap<>();
            document.put(FIELD_SAVED_AT, FieldValue.serverTimestamp());
            task = savedCollection(user).document(eventId).set(document);
        } else {
            task = savedCollection(user).document(eventId).delete();
        }
        task.addOnSuccessListener(unused -> callback.onDone())
                .addOnFailureListener(error -> {
                    Log.w(TAG, "saving event failed", error);
                    callback.onFailure(toFailure(error));
                });
    }

    private com.google.firebase.firestore.CollectionReference savedCollection(@NonNull FirebaseUser user) {
        return firestore.collection("users").document(user.getUid()).collection(SAVED_COLLECTION);
    }

    /** Midnight at the start of today on this phone, so today's events still count as upcoming. */
    private static long startOfTodayMillis() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /** Builds an {@link Event} from a document, or null if it is missing something every event needs. */
    @Nullable
    private static Event toEvent(@NonNull DocumentSnapshot document) {
        String title = document.getString(Event.FIELD_TITLE);
        EventCategory category = EventCategory.fromValue(document.getString(Event.FIELD_CATEGORY));
        Timestamp startsAt = document.getTimestamp(Event.FIELD_STARTS_AT);
        if (title == null || title.trim().isEmpty() || category == null || startsAt == null) {
            Log.w(TAG, "Skipping event " + document.getId() + ": needs title, category and startsAt");
            return null;
        }
        Timestamp endsAt = document.getTimestamp(Event.FIELD_ENDS_AT);
        String venue = document.getString(Event.FIELD_VENUE);
        String city = document.getString(Event.FIELD_CITY);
        String organizerName = document.getString(Event.FIELD_ORGANIZER_NAME);
        Double price = document.getDouble(Event.FIELD_PRICE);
        Boolean boosted = document.getBoolean(Event.FIELD_BOOSTED);

        List<String> tags = new ArrayList<>();
        Object rawTags = document.get(Event.FIELD_TAGS);
        if (rawTags instanceof List) {
            for (Object tag : (List<?>) rawTags) {
                if (tag instanceof String && !((String) tag).trim().isEmpty()) {
                    tags.add(((String) tag).trim());
                }
            }
        }
        List<EventAudience> audiences = new ArrayList<>();
        Object rawAudiences = document.get(Event.FIELD_AUDIENCES);
        if (rawAudiences instanceof List) {
            for (Object value : (List<?>) rawAudiences) {
                EventAudience audience = value instanceof String ? EventAudience.fromValue((String) value) : null;
                if (audience != null && !audiences.contains(audience)) {
                    audiences.add(audience);
                }
            }
        }
        String coverUrl = document.getString(Event.FIELD_COVER_URL);
        return new Event(
                document.getId(),
                title.trim(),
                category,
                startsAt.toDate().getTime(),
                endsAt == null ? 0 : endsAt.toDate().getTime(),
                venue == null ? "" : venue.trim(),
                city == null ? "" : city.trim(),
                audiences,
                organizerName == null ? "" : organizerName.trim(),
                tags,
                price == null ? 0 : price,
                coverUrl == null || coverUrl.trim().isEmpty() ? null : coverUrl.trim(),
                boosted != null && boosted);
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
