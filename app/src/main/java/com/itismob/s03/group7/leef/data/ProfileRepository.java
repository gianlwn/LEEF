package com.itismob.s03.group7.leef.data;

import androidx.annotation.NonNull;

import com.itismob.s03.group7.leef.model.UserProfile;

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
}
