package com.itismob.s03.group7.leef.data;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.itismob.s03.group7.leef.model.UserProfile;
import com.itismob.s03.group7.leef.model.UserRole;

/** {@link AccountRepository} backed by Firebase Authentication and Cloud Firestore (free Spark plan). */
public final class FirebaseAccountRepository implements AccountRepository {

    private static final String TAG = "LeefAccounts";

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    public FirebaseAccountRepository() {
        this(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance());
    }

    FirebaseAccountRepository(@NonNull FirebaseAuth auth, @NonNull FirebaseFirestore firestore) {
        this.auth = auth;
        this.firestore = firestore;
    }

    @Override
    public void register(@NonNull String fullName, @NonNull String schoolOrganization, @NonNull String email,
                         @NonNull UserRole role, @NonNull String password, @NonNull RegisterCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        callback.onFailure(Failure.UNKNOWN);
                        return;
                    }
                    UserProfile profile = new UserProfile(user.getUid(), fullName, schoolOrganization, email, role);
                    saveProfile(user, profile, callback);
                })
                .addOnFailureListener(error -> {
                    Log.w(TAG, "createUserWithEmailAndPassword failed", error);
                    callback.onFailure(toFailure(error));
                });
    }

    private void saveProfile(@NonNull FirebaseUser user, @NonNull UserProfile profile,
                             @NonNull RegisterCallback callback) {
        firestore.collection(UserProfile.COLLECTION)
                .document(profile.getUid())
                .set(profile.toNewDocument())
                .addOnSuccessListener(unused -> sendFirstVerificationEmail(user, profile, callback))
                .addOnFailureListener(error -> {
                    // Most often Firestore security rules not published yet, or no connection.
                    Log.e(TAG, "Saving the user profile failed; removing the new account", error);
                    user.delete().addOnCompleteListener(task -> callback.onFailure(Failure.PROFILE_NOT_SAVED));
                });
    }

    /** The account exists at this point; a failed email only means the person has to tap Resend. */
    private void sendFirstVerificationEmail(@NonNull FirebaseUser user, @NonNull UserProfile profile,
                                            @NonNull RegisterCallback callback) {
        user.sendEmailVerification().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "The first verification email could not be sent", task.getException());
            }
            callback.onSuccess(profile, task.isSuccessful());
        });
    }

    @Override
    public void signIn(@NonNull String email, @NonNull String password, @NonNull SignInCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        callback.onFailure(SignInFailure.UNKNOWN);
                        return;
                    }
                    callback.onSignedIn(user.isEmailVerified());
                })
                .addOnFailureListener(error -> {
                    Log.w(TAG, "signInWithEmailAndPassword failed", error);
                    callback.onFailure(toSignInFailure(error));
                });
    }

    @Override
    public void signInWithGoogle(@NonNull String idToken, @NonNull GoogleSignInCallback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        callback.onFailure(GoogleSignInFailure.UNKNOWN);
                        return;
                    }
                    checkForProfile(user, callback);
                })
                .addOnFailureListener(error -> {
                    Log.w(TAG, "signInWithCredential (Google) failed", error);
                    callback.onFailure(toGoogleFailure(error));
                });
    }

    /** A returning person has a profile; a first-timer is sent to fill one in. */
    private void checkForProfile(@NonNull FirebaseUser user, @NonNull GoogleSignInCallback callback) {
        firestore.collection(UserProfile.COLLECTION)
                .document(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        callback.onSignedIn();
                    } else {
                        String name = user.getDisplayName();
                        String email = user.getEmail();
                        callback.onProfileNeeded(user.getUid(), name == null ? "" : name.trim(),
                                email == null ? "" : email);
                    }
                })
                .addOnFailureListener(error -> {
                    Log.w(TAG, "Looking up the Google user's profile failed", error);
                    // Start clean so the person can simply try again.
                    auth.signOut();
                    boolean offline = error instanceof FirebaseFirestoreException
                            && ((FirebaseFirestoreException) error).getCode()
                            == FirebaseFirestoreException.Code.UNAVAILABLE;
                    callback.onFailure(offline ? GoogleSignInFailure.NETWORK : GoogleSignInFailure.UNKNOWN);
                });
    }

    @Override
    public void saveGoogleProfile(@NonNull String fullName, @NonNull String schoolOrganization,
                                  @NonNull UserRole role, @NonNull SaveProfileCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        String email = user == null ? null : user.getEmail();
        if (user == null || email == null) {
            callback.onFailure(ProfileFailure.SIGNED_OUT);
            return;
        }
        UserProfile profile = new UserProfile(user.getUid(), fullName, schoolOrganization,
                email.toLowerCase(java.util.Locale.ROOT), role);
        firestore.collection(UserProfile.COLLECTION)
                .document(profile.getUid())
                .set(profile.toNewDocument())
                .addOnSuccessListener(unused -> callback.onSaved(profile))
                .addOnFailureListener(error -> {
                    Log.w(TAG, "Saving the Google user's profile failed", error);
                    boolean offline = error instanceof FirebaseFirestoreException
                            && ((FirebaseFirestoreException) error).getCode()
                            == FirebaseFirestoreException.Code.UNAVAILABLE;
                    callback.onFailure(offline ? ProfileFailure.NETWORK : ProfileFailure.UNKNOWN);
                });
    }

    @NonNull
    private static GoogleSignInFailure toGoogleFailure(@NonNull Exception error) {
        if (error instanceof FirebaseAuthUserCollisionException) {
            return GoogleSignInFailure.ACCOUNT_EXISTS_WITH_OTHER_METHOD;
        }
        if (error instanceof FirebaseAuthInvalidUserException) {
            return "ERROR_USER_DISABLED".equals(((FirebaseAuthInvalidUserException) error).getErrorCode())
                    ? GoogleSignInFailure.ACCOUNT_DISABLED : GoogleSignInFailure.UNKNOWN;
        }
        if (error instanceof FirebaseNetworkException) {
            return GoogleSignInFailure.NETWORK;
        }
        return GoogleSignInFailure.UNKNOWN;
    }

    @Override
    public void sendPasswordReset(@NonNull String email, @NonNull PasswordResetCallback callback) {
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> callback.onSent())
                .addOnFailureListener(error -> {
                    Log.w(TAG, "sendPasswordResetEmail failed", error);
                    callback.onFailure(toPasswordResetFailure(error));
                });
    }

    @NonNull
    private static SignInFailure toSignInFailure(@NonNull Exception error) {
        if (error instanceof FirebaseAuthInvalidUserException) {
            String code = ((FirebaseAuthInvalidUserException) error).getErrorCode();
            return "ERROR_USER_DISABLED".equals(code)
                    ? SignInFailure.ACCOUNT_DISABLED : SignInFailure.INVALID_CREDENTIALS;
        }
        if (error instanceof FirebaseAuthInvalidCredentialsException) {
            return SignInFailure.INVALID_CREDENTIALS;
        }
        if (error instanceof FirebaseNetworkException) {
            return SignInFailure.NETWORK;
        }
        if (error instanceof FirebaseTooManyRequestsException) {
            return SignInFailure.TOO_MANY_REQUESTS;
        }
        return SignInFailure.UNKNOWN;
    }

    @NonNull
    private static PasswordResetFailure toPasswordResetFailure(@NonNull Exception error) {
        if (error instanceof FirebaseAuthInvalidCredentialsException) {
            return PasswordResetFailure.INVALID_EMAIL;
        }
        // An unknown address is reported as success by Firebase; a disabled one is treated the same.
        if (error instanceof FirebaseAuthInvalidUserException) {
            return PasswordResetFailure.INVALID_EMAIL;
        }
        if (error instanceof FirebaseNetworkException) {
            return PasswordResetFailure.NETWORK;
        }
        if (error instanceof FirebaseTooManyRequestsException) {
            return PasswordResetFailure.TOO_MANY_REQUESTS;
        }
        return PasswordResetFailure.UNKNOWN;
    }

    @Nullable
    @Override
    public String getSignedInUid() {
        FirebaseUser user = auth.getCurrentUser();
        return user == null ? null : user.getUid();
    }

    @Nullable
    @Override
    public String getSignedInDisplayName() {
        FirebaseUser user = auth.getCurrentUser();
        return user == null ? null : user.getDisplayName();
    }

    @Nullable
    @Override
    public String getSignedInEmail() {
        FirebaseUser user = auth.getCurrentUser();
        return user == null ? null : user.getEmail();
    }

    @Override
    public boolean hasPasswordSignIn() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return false;
        }
        for (com.google.firebase.auth.UserInfo info : user.getProviderData()) {
            if (com.google.firebase.auth.EmailAuthProvider.PROVIDER_ID.equals(info.getProviderId())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isSignedIn() {
        return auth.getCurrentUser() != null;
    }

    @Override
    public boolean isSignedInAndVerified() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null && user.isEmailVerified();
    }

    @Override
    public void sendVerificationEmail(@NonNull SendVerificationCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onFailure(VerificationFailure.SIGNED_OUT);
            return;
        }
        user.sendEmailVerification()
                .addOnSuccessListener(unused -> callback.onSent())
                .addOnFailureListener(error -> {
                    Log.w(TAG, "sendEmailVerification failed", error);
                    callback.onFailure(toVerificationFailure(error));
                });
    }

    @Override
    public void checkEmailVerified(@NonNull CheckVerificationCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onFailure(VerificationFailure.SIGNED_OUT);
            return;
        }
        // reload() fetches the latest account state, including whether the link was tapped.
        user.reload()
                .addOnSuccessListener(unused -> {
                    FirebaseUser refreshed = auth.getCurrentUser();
                    callback.onChecked(refreshed != null && refreshed.isEmailVerified());
                })
                .addOnFailureListener(error -> {
                    Log.w(TAG, "Reloading the user failed", error);
                    callback.onFailure(toVerificationFailure(error));
                });
    }

    @Override
    public void signOut() {
        auth.signOut();
    }

    @NonNull
    private static VerificationFailure toVerificationFailure(@NonNull Exception error) {
        if (error instanceof FirebaseNetworkException) {
            return VerificationFailure.NETWORK;
        }
        if (error instanceof FirebaseTooManyRequestsException) {
            return VerificationFailure.TOO_MANY_REQUESTS;
        }
        return VerificationFailure.UNKNOWN;
    }

    @NonNull
    private static Failure toFailure(@NonNull Exception error) {
        if (error instanceof FirebaseAuthUserCollisionException) {
            return Failure.EMAIL_IN_USE;
        }
        // Weak password is a subclass of invalid credentials, so check it first.
        if (error instanceof FirebaseAuthWeakPasswordException) {
            return Failure.WEAK_PASSWORD;
        }
        if (error instanceof FirebaseAuthInvalidCredentialsException) {
            return Failure.INVALID_EMAIL;
        }
        if (error instanceof FirebaseNetworkException) {
            return Failure.NETWORK;
        }
        return Failure.UNKNOWN;
    }
}
