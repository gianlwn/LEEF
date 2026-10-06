package com.itismob.s03.group7.leef.data;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
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
                .addOnSuccessListener(unused -> callback.onSuccess(profile))
                .addOnFailureListener(error -> {
                    // Most often Firestore security rules not published yet, or no connection.
                    Log.e(TAG, "Saving the user profile failed; removing the new account", error);
                    user.delete().addOnCompleteListener(task -> callback.onFailure(Failure.PROFILE_NOT_SAVED));
                });
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
