package com.itismob.s03.group7.leef.data;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.itismob.s03.group7.leef.model.UserProfile;
import com.itismob.s03.group7.leef.model.UserRole;

import java.util.HashMap;
import java.util.Map;

/** {@link ProfileRepository} backed by Cloud Firestore. */
public final class FirebaseProfileRepository implements ProfileRepository {

    private static final String TAG = "LeefProfile";

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    public FirebaseProfileRepository() {
        this(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance());
    }

    FirebaseProfileRepository(@NonNull FirebaseAuth auth, @NonNull FirebaseFirestore firestore) {
        this.auth = auth;
        this.firestore = firestore;
    }

    @Override
    public void loadProfile(@NonNull LoadCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onFailure(Failure.SIGNED_OUT);
            return;
        }
        firestore.collection(UserProfile.COLLECTION).document(user.getUid()).get()
                .addOnSuccessListener(document -> deliver(user, document, callback))
                .addOnFailureListener(error -> {
                    Log.w(TAG, "loading the profile failed", error);
                    boolean offline = error instanceof FirebaseFirestoreException
                            && ((FirebaseFirestoreException) error).getCode()
                            == FirebaseFirestoreException.Code.UNAVAILABLE;
                    callback.onFailure(offline ? Failure.NETWORK : Failure.UNKNOWN);
                });
    }

    @Override
    public void updateProfile(@NonNull String fullName, @NonNull String schoolOrganization, @NonNull UserRole role,
                              @NonNull SaveCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onFailure(SaveFailure.SIGNED_OUT);
            return;
        }
        Map<String, Object> changes = new HashMap<>();
        changes.put(UserProfile.FIELD_FULL_NAME, fullName.trim());
        changes.put(UserProfile.FIELD_SCHOOL_ORGANIZATION, schoolOrganization.trim());
        changes.put(UserProfile.FIELD_ROLE, role.getValue());

        DocumentReference reference = firestore.collection(UserProfile.COLLECTION).document(user.getUid());
        // A transaction (not a plain update) so being offline fails right away instead of queueing the change.
        firestore.runTransaction(transaction -> {
            transaction.update(reference, changes);
            return null;
        }).addOnSuccessListener(unused -> callback.onSaved())
                .addOnFailureListener(error -> {
                    Log.w(TAG, "saving the profile failed", error);
                    callback.onFailure(toSaveFailure(error));
                });
    }

    @NonNull
    private static SaveFailure toSaveFailure(@NonNull Exception error) {
        if (error instanceof FirebaseFirestoreException) {
            switch (((FirebaseFirestoreException) error).getCode()) {
                case UNAVAILABLE:
                case DEADLINE_EXCEEDED:
                    return SaveFailure.NETWORK;
                case PERMISSION_DENIED:
                    return SaveFailure.PERMISSION;
                default:
                    return SaveFailure.UNKNOWN;
            }
        }
        return SaveFailure.UNKNOWN;
    }

    private static void deliver(@NonNull FirebaseUser user, @NonNull DocumentSnapshot document,
                                @NonNull LoadCallback callback) {
        if (!document.exists()) {
            callback.onMissing();
            return;
        }
        String fullName = document.getString(UserProfile.FIELD_FULL_NAME);
        String school = document.getString(UserProfile.FIELD_SCHOOL_ORGANIZATION);
        String email = document.getString(UserProfile.FIELD_EMAIL);
        UserRole role = UserRole.fromValue(document.getString(UserProfile.FIELD_ROLE));
        if (fullName == null || school == null || email == null || role == null) {
            Log.w(TAG, "Profile document is incomplete");
            callback.onFailure(Failure.UNKNOWN);
            return;
        }
        callback.onLoaded(new UserProfile(user.getUid(), fullName, school, email, role));
    }
}
