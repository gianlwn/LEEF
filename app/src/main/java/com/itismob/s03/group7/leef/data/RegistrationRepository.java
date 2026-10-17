package com.itismob.s03.group7.leef.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.itismob.s03.group7.leef.model.Registration;

/** Registrations the screens rely on; the Firebase code lives in {@link FirebaseRegistrationRepository}. */
public interface RegistrationRepository {

    /** Why a registration couldn't be loaded or saved. */
    enum Failure {
        SIGNED_OUT,
        NETWORK,
        /** The database rules refused the request (for example the rules haven't been published). */
        PERMISSION,
        UNKNOWN
    }

    interface LoadCallback {
        /** @param registration the person's registration for the event, or null if they haven't registered */
        void onLoaded(@Nullable Registration registration);

        void onFailure(@NonNull Failure failure);
    }

    interface SaveCallback {
        void onSaved();

        void onFailure(@NonNull Failure failure);
    }

    /** The signed-in person's registration for one event, if any. */
    void loadRegistration(@NonNull String eventId, @NonNull LoadCallback callback);

    /** Saves the signed-in person's registration. */
    void saveRegistration(@NonNull Registration registration, @NonNull SaveCallback callback);
}
