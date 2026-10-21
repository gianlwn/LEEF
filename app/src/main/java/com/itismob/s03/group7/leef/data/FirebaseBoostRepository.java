package com.itismob.s03.group7.leef.data;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.itismob.s03.group7.leef.model.BoostPlan;
import com.itismob.s03.group7.leef.model.BoostReceipt;
import com.itismob.s03.group7.leef.model.Event;
import com.itismob.s03.group7.leef.model.PaymentMethod;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link BoostRepository} backed by Cloud Firestore (free Spark plan). One transaction writes both the
 * receipt (boosts/{id}) and the event's {@code boosted} / {@code boostedUntil}, so they can't disagree.
 * Card details and mobile numbers are never stored.
 */
public final class FirebaseBoostRepository implements BoostRepository {

    private static final String TAG = "LeefBoosts";

    /** Thrown inside the transaction to say why it was stopped. */
    private static final class Refused extends RuntimeException {
        final Failure failure;

        Refused(@NonNull Failure failure) {
            super(failure.name());
            this.failure = failure;
        }
    }

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    public FirebaseBoostRepository() {
        this(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance());
    }

    FirebaseBoostRepository(@NonNull FirebaseAuth auth, @NonNull FirebaseFirestore firestore) {
        this.auth = auth;
        this.firestore = firestore;
    }

    @Override
    public void saveBoost(@NonNull String eventId, @NonNull BoostPlan plan, @NonNull PaymentMethod paidWith,
                          @NonNull String confirmationCode, @NonNull SaveCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onFailure(Failure.SIGNED_OUT);
            return;
        }
        DocumentReference eventRef = firestore.collection(Event.COLLECTION).document(eventId);
        DocumentReference boostRef = firestore.collection(BoostReceipt.COLLECTION).document();

        // A transaction (not two plain writes) so being offline fails right away and both documents
        // change together or not at all.
        firestore.runTransaction(transaction -> {
            DocumentSnapshot event = transaction.get(eventRef);
            if (!event.exists()) {
                throw new Refused(Failure.NOT_FOUND);
            }
            if (!user.getUid().equals(event.getString(Event.FIELD_ORGANIZER_ID))) {
                throw new Refused(Failure.NOT_ORGANIZER);
            }
            Timestamp startsAt = event.getTimestamp(Event.FIELD_STARTS_AT);
            long now = System.currentTimeMillis();
            if (startsAt == null || startsAt.toDate().getTime() <= now) {
                throw new Refused(Failure.STARTED);
            }
            String title = event.getString(Event.FIELD_TITLE);

            long base = now;
            Boolean boosted = event.getBoolean(Event.FIELD_BOOSTED);
            Timestamp current = event.getTimestamp(Event.FIELD_BOOSTED_UNTIL);
            if (boosted != null && boosted && current != null && current.toDate().getTime() > now) {
                base = current.toDate().getTime();
            }
            long until = base + plan.getDurationMillis();
            if (until - now > BoostPlan.MAX_DAYS_AHEAD * BoostPlan.DAY_MILLIS) {
                throw new Refused(Failure.TOO_LONG);
            }

            Map<String, Object> eventChanges = new HashMap<>();
            eventChanges.put(Event.FIELD_BOOSTED, true);
            eventChanges.put(Event.FIELD_BOOSTED_UNTIL, new Timestamp(new Date(until)));
            transaction.update(eventRef, eventChanges);

            Map<String, Object> receipt = new HashMap<>();
            receipt.put(BoostReceipt.FIELD_USER_ID, user.getUid());
            receipt.put(BoostReceipt.FIELD_EVENT_ID, eventId);
            receipt.put(BoostReceipt.FIELD_EVENT_TITLE, title == null ? "" : title);
            receipt.put(BoostReceipt.FIELD_PLAN, plan.getValue());
            receipt.put(BoostReceipt.FIELD_DAYS, plan.getDays());
            receipt.put(BoostReceipt.FIELD_AMOUNT, plan.getPrice());
            receipt.put(BoostReceipt.FIELD_PAYMENT_METHOD, paidWith.getValue());
            receipt.put(BoostReceipt.FIELD_CONFIRMATION_CODE, confirmationCode);
            receipt.put(BoostReceipt.FIELD_BOOSTED_UNTIL, new Timestamp(new Date(until)));
            receipt.put(BoostReceipt.FIELD_CREATED_AT, FieldValue.serverTimestamp());
            transaction.set(boostRef, receipt);

            return new BoostReceipt(eventId, title == null ? "" : title, plan, plan.getPrice(), paidWith,
                    confirmationCode, until);
        }).addOnSuccessListener(callback::onSaved)
                .addOnFailureListener(error -> {
                    Log.w(TAG, "saving the boost failed", error);
                    callback.onFailure(toFailure(error));
                });
    }

    @NonNull
    private static Failure toFailure(@NonNull Exception error) {
        Throwable cause = error;
        while (cause != null) {
            if (cause instanceof Refused) {
                return ((Refused) cause).failure;
            }
            cause = cause.getCause();
        }
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
