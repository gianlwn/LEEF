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
import com.itismob.s03.group7.leef.model.PaymentMethod;
import com.itismob.s03.group7.leef.model.Registration;
import com.itismob.s03.group7.leef.model.TicketType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link RegistrationRepository} backed by Cloud Firestore (free Spark plan). Only the confirmation
 * code and payment method are kept; card details and mobile numbers are never stored.
 */
public final class FirebaseRegistrationRepository implements RegistrationRepository {

    private static final String TAG = "LeefRegistrations";

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    public FirebaseRegistrationRepository() {
        this(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance());
    }

    FirebaseRegistrationRepository(@NonNull FirebaseAuth auth, @NonNull FirebaseFirestore firestore) {
        this.auth = auth;
        this.firestore = firestore;
    }

    @Override
    public void loadRegistration(@NonNull String eventId, @NonNull LoadCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onFailure(Failure.SIGNED_OUT);
            return;
        }
        firestore.collection(Registration.COLLECTION)
                .document(Registration.documentId(user.getUid(), eventId))
                .get()
                .addOnSuccessListener(document -> callback.onLoaded(document.exists() ? toRegistration(document) : null))
                .addOnFailureListener(error -> {
                    Log.w(TAG, "loading registration failed", error);
                    callback.onFailure(toFailure(error));
                });
    }

    @Override
    public void loadRegistrations(@NonNull ListCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onFailure(Failure.SIGNED_OUT);
            return;
        }
        firestore.collection(Registration.COLLECTION)
                .whereEqualTo(Registration.FIELD_USER_ID, user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Registration> registrations = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        Registration registration = toRegistration(document);
                        if (registration != null) {
                            registrations.add(registration);
                        }
                    }
                    callback.onLoaded(registrations);
                })
                .addOnFailureListener(error -> {
                    Log.w(TAG, "loading registrations failed", error);
                    callback.onFailure(toFailure(error));
                });
    }

    @Override
    public void saveRegistration(@NonNull Registration registration, @NonNull SaveCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onFailure(Failure.SIGNED_OUT);
            return;
        }
        Map<String, Object> document = new HashMap<>();
        document.put(Registration.FIELD_USER_ID, user.getUid());
        document.put(Registration.FIELD_EVENT_ID, registration.getEventId());
        document.put(Registration.FIELD_EVENT_TITLE, registration.getEventTitle());
        document.put(Registration.FIELD_EVENT_STARTS_AT, new Timestamp(new Date(registration.getEventStartsAtMillis())));
        document.put(Registration.FIELD_TICKET_TYPE, registration.getTicketType().getValue());
        document.put(Registration.FIELD_QUANTITY, registration.getQuantity());
        document.put(Registration.FIELD_AMOUNT, registration.getAmount());
        if (registration.getPaymentMethod() != null) {
            document.put(Registration.FIELD_PAYMENT_METHOD, registration.getPaymentMethod().getValue());
        }
        document.put(Registration.FIELD_CONFIRMATION_CODE, registration.getConfirmationCode());
        document.put(Registration.FIELD_STATUS, Registration.STATUS_CONFIRMED);
        document.put(Registration.FIELD_REGISTERED_AT, FieldValue.serverTimestamp());

        firestore.collection(Registration.COLLECTION)
                .document(Registration.documentId(user.getUid(), registration.getEventId()))
                .set(document)
                .addOnSuccessListener(unused -> callback.onSaved())
                .addOnFailureListener(error -> {
                    Log.w(TAG, "saving registration failed", error);
                    callback.onFailure(toFailure(error));
                });
    }

    @Nullable
    private static Registration toRegistration(@NonNull DocumentSnapshot document) {
        String eventId = document.getString(Registration.FIELD_EVENT_ID);
        String title = document.getString(Registration.FIELD_EVENT_TITLE);
        Timestamp startsAt = document.getTimestamp(Registration.FIELD_EVENT_STARTS_AT);
        TicketType ticket = TicketType.fromValue(document.getString(Registration.FIELD_TICKET_TYPE));
        if (eventId == null || title == null || startsAt == null || ticket == null) {
            Log.w(TAG, "Ignoring incomplete registration " + document.getId());
            return null;
        }
        Double amount = document.getDouble(Registration.FIELD_AMOUNT);
        Long quantity = document.getLong(Registration.FIELD_QUANTITY);
        String code = document.getString(Registration.FIELD_CONFIRMATION_CODE);
        return new Registration(eventId, title, startsAt.toDate().getTime(), ticket,
                quantity == null ? 1 : quantity.intValue(), amount == null ? 0 : amount,
                PaymentMethod.fromValue(document.getString(Registration.FIELD_PAYMENT_METHOD)),
                code == null ? "" : code);
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
