package com.itismob.s03.group7.leef.data;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.itismob.s03.group7.leef.model.Attendee;
import com.itismob.s03.group7.leef.model.Registration;
import com.itismob.s03.group7.leef.model.TicketType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** {@link AttendeeRepository} backed by Cloud Firestore (free Spark plan). */
public final class FirebaseAttendeeRepository implements AttendeeRepository {

    private static final String TAG = "LeefAttendees";

    /** Set on a registration by the organizer when the person is checked in. */
    public static final String FIELD_ATTENDEE_NAME = "attendeeName";
    public static final String FIELD_CHECKED_IN_AT = "checkedInAt";
    public static final String FIELD_CHECKED_IN_BY = "checkedInBy";

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    public FirebaseAttendeeRepository() {
        this(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance());
    }

    FirebaseAttendeeRepository(@NonNull FirebaseAuth auth, @NonNull FirebaseFirestore firestore) {
        this.auth = auth;
        this.firestore = firestore;
    }

    @Override
    public void loadAttendees(@NonNull String eventId, @NonNull ListCallback callback) {
        if (auth.getCurrentUser() == null) {
            callback.onFailure(Failure.SIGNED_OUT);
            return;
        }
        firestore.collection(Registration.COLLECTION)
                .whereEqualTo(Registration.FIELD_EVENT_ID, eventId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Attendee> attendees = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        Attendee attendee = toAttendee(document);
                        if (attendee != null) {
                            attendees.add(attendee);
                        }
                    }
                    callback.onLoaded(attendees);
                })
                .addOnFailureListener(error -> {
                    Log.w(TAG, "loading attendees failed", error);
                    callback.onFailure(toFailure(error));
                });
    }

    @Override
    public void checkIn(@NonNull String eventId, @NonNull String confirmationCode, @NonNull CheckInCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onFailure(Failure.SIGNED_OUT);
            return;
        }
        firestore.collection(Registration.COLLECTION)
                .whereEqualTo(Registration.FIELD_EVENT_ID, eventId)
                .whereEqualTo(Registration.FIELD_CONFIRMATION_CODE, confirmationCode)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onResult(Outcome.NOT_FOUND, null);
                        return;
                    }
                    markCheckedIn(snapshot.getDocuments().get(0).getReference(), user, callback);
                })
                .addOnFailureListener(error -> {
                    Log.w(TAG, "looking up the ticket failed", error);
                    callback.onFailure(toFailure(error));
                });
    }

    /** A transaction so a ticket scanned twice at once is only marked once, and being offline fails right away. */
    private void markCheckedIn(@NonNull DocumentReference reference, @NonNull FirebaseUser user,
                               @NonNull CheckInCallback callback) {
        final Outcome[] outcome = {Outcome.CHECKED_IN};
        firestore.runTransaction(transaction -> {
            DocumentSnapshot document = transaction.get(reference);
            if (document.getTimestamp(FIELD_CHECKED_IN_AT) != null) {
                outcome[0] = Outcome.ALREADY_CHECKED_IN;
                return toAttendee(document);
            }
            outcome[0] = Outcome.CHECKED_IN;
            Map<String, Object> changes = new HashMap<>();
            changes.put(FIELD_CHECKED_IN_AT, FieldValue.serverTimestamp());
            changes.put(FIELD_CHECKED_IN_BY, user.getUid());
            transaction.update(reference, changes);
            Attendee attendee = toAttendee(document);
            return attendee == null ? null : attendee.withCheckedIn(System.currentTimeMillis());
        }).addOnSuccessListener(attendee -> callback.onResult(outcome[0], attendee))
                .addOnFailureListener(error -> {
                    Log.w(TAG, "checking in failed", error);
                    callback.onFailure(toFailure(error));
                });
    }

    @Nullable
    private static Attendee toAttendee(@NonNull DocumentSnapshot document) {
        String eventId = document.getString(Registration.FIELD_EVENT_ID);
        TicketType ticket = TicketType.fromValue(document.getString(Registration.FIELD_TICKET_TYPE));
        String code = document.getString(Registration.FIELD_CONFIRMATION_CODE);
        if (eventId == null || ticket == null || code == null) {
            Log.w(TAG, "Ignoring incomplete registration " + document.getId());
            return null;
        }
        String name = document.getString(FIELD_ATTENDEE_NAME);
        Long quantity = document.getLong(Registration.FIELD_QUANTITY);
        Timestamp checkedInAt = document.getTimestamp(FIELD_CHECKED_IN_AT);
        return new Attendee(document.getId(), eventId, name == null ? "" : name.trim(), ticket,
                quantity == null ? 1 : Math.max(1, quantity.intValue()), code,
                checkedInAt == null ? 0 : checkedInAt.toDate().getTime());
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
