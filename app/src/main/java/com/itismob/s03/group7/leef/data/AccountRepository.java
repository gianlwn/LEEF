package com.itismob.s03.group7.leef.data;

import androidx.annotation.NonNull;

import com.itismob.s03.group7.leef.model.UserProfile;
import com.itismob.s03.group7.leef.model.UserRole;

/**
 * Account operations the screens rely on. Screens depend on this interface only, so the Firebase
 * details stay in one place ({@link FirebaseAccountRepository}).
 */
public interface AccountRepository {

    /** Why creating an account failed, in terms a screen can explain to the person. */
    enum Failure {
        EMAIL_IN_USE,
        INVALID_EMAIL,
        WEAK_PASSWORD,
        NETWORK,
        PROFILE_NOT_SAVED,
        UNKNOWN
    }

    interface RegisterCallback {
        void onSuccess(@NonNull UserProfile profile);

        void onFailure(@NonNull Failure failure);
    }

    /**
     * Creates the sign-in account and the user's profile, and leaves the person signed in. If the
     * profile can't be saved, the half-created account is removed so they can simply try again.
     * Results arrive on the main thread.
     */
    void register(@NonNull String fullName, @NonNull String schoolOrganization, @NonNull String email,
                  @NonNull UserRole role, @NonNull String password, @NonNull RegisterCallback callback);
}
