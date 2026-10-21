package com.itismob.s03.group7.leef.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

    /** Why an email-verification request failed. */
    enum VerificationFailure {
        NETWORK,
        TOO_MANY_REQUESTS,
        /** Nobody is signed in any more. */
        SIGNED_OUT,
        UNKNOWN
    }

    /** Why signing in failed. */
    enum SignInFailure {
        /** Wrong email or password (deliberately not distinguished, so emails can't be probed). */
        INVALID_CREDENTIALS,
        ACCOUNT_DISABLED,
        NETWORK,
        TOO_MANY_REQUESTS,
        UNKNOWN
    }

    /** Why a password-reset email couldn't be sent. */
    enum PasswordResetFailure {
        INVALID_EMAIL,
        NETWORK,
        TOO_MANY_REQUESTS,
        UNKNOWN
    }

    /** Why signing in with a Google account failed. */
    enum GoogleSignInFailure {
        /** Another sign-in method already owns this email and can't be merged automatically. */
        ACCOUNT_EXISTS_WITH_OTHER_METHOD,
        ACCOUNT_DISABLED,
        NETWORK,
        UNKNOWN
    }

    /** Why a Google user's profile couldn't be saved. */
    enum ProfileFailure {
        SIGNED_OUT,
        NETWORK,
        UNKNOWN
    }

    interface GoogleSignInCallback {
        /** Signed in and the person already has a LEEF profile. */
        void onSignedIn();

        /**
         * Signed in, but this Google account has no LEEF profile yet (first time here).
         *
         * @param suggestedName the name on the Google account, possibly empty
         */
        void onProfileNeeded(@NonNull String uid, @NonNull String suggestedName, @NonNull String email);

        void onFailure(@NonNull GoogleSignInFailure failure);
    }

    interface SaveProfileCallback {
        void onSaved(@NonNull UserProfile profile);

        void onFailure(@NonNull ProfileFailure failure);
    }

    interface SignInCallback {
        void onSignedIn(boolean emailVerified);

        void onFailure(@NonNull SignInFailure failure);
    }

    interface PasswordResetCallback {
        void onSent();

        void onFailure(@NonNull PasswordResetFailure failure);
    }

    interface RegisterCallback {
        /**
         * @param verificationEmailSent false if the account was created but the verification email
         *                              couldn't be sent (the person can resend it)
         */
        void onSuccess(@NonNull UserProfile profile, boolean verificationEmailSent);

        void onFailure(@NonNull Failure failure);
    }

    interface SendVerificationCallback {
        void onSent();

        void onFailure(@NonNull VerificationFailure failure);
    }

    interface CheckVerificationCallback {
        void onChecked(boolean verified);

        void onFailure(@NonNull VerificationFailure failure);
    }

    /**
     * Creates the sign-in account and the user's profile, sends the verification email, and leaves
     * the person signed in. If the profile can't be saved, the half-created account is removed so
     * they can simply try again. Results arrive on the main thread.
     */
    void register(@NonNull String fullName, @NonNull String schoolOrganization, @NonNull String email,
                  @NonNull UserRole role, @NonNull String password, @NonNull RegisterCallback callback);

    /** Signs in with an email and password; reports whether the email has been verified. */
    void signIn(@NonNull String email, @NonNull String password, @NonNull SignInCallback callback);

    /**
     * Emails a password-reset link (free Firebase email). Firebase doesn't reveal whether the
     * address has an account, so success only means "if it exists, a link was sent".
     */
    void sendPasswordReset(@NonNull String email, @NonNull PasswordResetCallback callback);

    /**
     * Signs in with the ID token that Google's account sheet returned. Google accounts are created
     * on first use; the callback says whether the person still needs to fill in their profile.
     */
    void signInWithGoogle(@NonNull String idToken, @NonNull GoogleSignInCallback callback);

    /**
     * Saves the profile for the signed-in Google account (name from Google, plus the school and role
     * the person chose). The email always comes from the account itself.
     */
    void saveGoogleProfile(@NonNull String fullName, @NonNull String schoolOrganization,
                           @NonNull UserRole role, @NonNull SaveProfileCallback callback);

    /** Id of the signed-in account, or null if nobody is signed in. */
    @Nullable
    String getSignedInUid();

    /** Display name on the signed-in account (set for Google accounts), or null. */
    @Nullable
    String getSignedInDisplayName();

    /** Email of the signed-in account, or null if nobody is signed in. */
    @Nullable
    String getSignedInEmail();

    /** True if someone is signed in and their email is already verified (as last known on this phone). */
    boolean isSignedInAndVerified();

    /**
     * True if the signed-in account has a password (it was created with an email and password), as
     * opposed to a Google-only account that has nothing to reset.
     */
    boolean hasPasswordSignIn();

    /** True if someone is signed in, whether or not their email is verified. */
    boolean isSignedIn();

    /** Sends (or resends) the verification email to the signed-in account. */
    void sendVerificationEmail(@NonNull SendVerificationCallback callback);

    /** Asks the server whether the signed-in account's email has been verified yet. */
    void checkEmailVerified(@NonNull CheckVerificationCallback callback);

    void signOut();
}
