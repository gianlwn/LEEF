package com.itismob.s03.group7.leef.data;

import androidx.annotation.NonNull;

import com.itismob.s03.group7.leef.model.BoostPlan;
import com.itismob.s03.group7.leef.model.BoostReceipt;
import com.itismob.s03.group7.leef.model.PaymentMethod;

/** Saving a paid boost. Screens depend on this interface only; the Firebase code is in one place. */
public interface BoostRepository {

    /** Why a boost couldn't be saved. */
    enum Failure {
        SIGNED_OUT,
        NETWORK,
        /** The database rules refused the request (for example the rules haven't been published). */
        PERMISSION,
        /** The event doesn't exist (any more). */
        NOT_FOUND,
        /** The signed-in person didn't post this event. */
        NOT_ORGANIZER,
        /** The event has already started. */
        STARTED,
        /** The organizer cancelled the event, so it can't be promoted. */
        CANCELLED,
        /** The event is already boosted so far ahead that this would go past the limit. */
        TOO_LONG,
        UNKNOWN
    }

    interface SaveCallback {
        void onSaved(@NonNull BoostReceipt receipt);

        void onFailure(@NonNull Failure failure);
    }

    /**
     * Records the paid boost and features the event. If the event is already boosted, the new time is
     * added after the current end, so nothing already paid for is lost.
     */
    void saveBoost(@NonNull String eventId, @NonNull BoostPlan plan, @NonNull PaymentMethod paidWith,
                   @NonNull String confirmationCode, @NonNull SaveCallback callback);
}
