package com.itismob.s03.group7.leef.data;

import androidx.annotation.NonNull;

import com.itismob.s03.group7.leef.model.UserProfile;
import com.itismob.s03.group7.leef.model.UserRole;

/** Reads the signed-in person's LEEF profile (the {@code users/{uid}} document). */
public interface ProfileRepository {

    enum Failure {
        SIGNED_OUT,
        NETWORK,
        UNKNOWN
    }

    interface LoadCallback {
        void onLoaded(@NonNull UserProfile profile);

        /** The account exists but has no LEEF profile (for example a Google sign-in that stopped part-way). */
        void onMissing();

        void onFailure(@NonNull Failure failure);
    }

    void loadProfile(@NonNull LoadCallback callback);

    /** Why saving the changes to a profile failed. */
    enum SaveFailure {
        SIGNED_OUT,
        NETWORK,
        /** The database rules refused the change (for example the latest rules aren't published yet). */
        PERMISSION,
        UNKNOWN
    }

    interface SaveCallback {
        void onSaved();

        void onFailure(@NonNull SaveFailure failure);
    }

    /**
     * Changes the signed-in person's name, school or organization and role. The email can't be changed
     * here. Fails right away when offline instead of queueing the change.
     */
    void updateProfile(@NonNull String fullName, @NonNull String schoolOrganization, @NonNull UserRole role,
                       @NonNull SaveCallback callback);
}
