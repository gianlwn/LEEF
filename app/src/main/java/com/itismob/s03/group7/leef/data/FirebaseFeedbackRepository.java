package com.itismob.s03.group7.leef.data;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior;
import com.itismob.s03.group7.leef.model.Event;
import com.itismob.s03.group7.leef.model.Feedback;
import com.itismob.s03.group7.leef.model.FeedbackTag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link FeedbackRepository} backed by Cloud Firestore (free Spark plan). Feedback lives in
 * {@code events/{eventId}/feedback/{uid}}.
 */
public final class FirebaseFeedbackRepository implements FeedbackRepository {

    private static final String TAG = "LeefFeedback";

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    public FirebaseFeedbackRepository() {
        this(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance());
    }

    FirebaseFeedbackRepository(@NonNull FirebaseAuth auth, @NonNull FirebaseFirestore firestore) {
        this.auth = auth;
        this.firestore = firestore;
    }

    @Override
    public void loadMine(@NonNull String eventId, @NonNull MineCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onFailure(Failure.SIGNED_OUT);
            return;
        }
        reference(eventId, user.getUid()).get()
                .addOnSuccessListener(document -> callback.onLoaded(document.exists() ? toFeedback(document) : null))
                .addOnFailureListener(error -> {
                    Log.w(TAG, "loading feedback failed", error);
                    callback.onFailure(toFailure(error));
                });
    }

    @Override
    public void submit(@NonNull String eventId, int rating, @NonNull List<FeedbackTag> tags,
                       @NonNull String comment, @Nullable String photoUrl, @NonNull DoneCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onFailure(Failure.SIGNED_OUT);
            return;
        }
        Map<String, Object> document = new HashMap<>();
        document.put(Feedback.FIELD_USER_ID, user.getUid());
        document.put(Feedback.FIELD_RATING, rating);
        List<String> tagValues = new ArrayList<>();
        for (FeedbackTag tag : tags) {
            tagValues.add(tag.getValue());
        }
        document.put(Feedback.FIELD_TAGS, tagValues);
        if (!comment.isEmpty()) {
            document.put(Feedback.FIELD_COMMENT, comment);
        }
        if (photoUrl != null && !photoUrl.isEmpty()) {
            document.put(Feedback.FIELD_PHOTO_URL, photoUrl);
        }
        document.put(Feedback.FIELD_SUBMITTED_AT, FieldValue.serverTimestamp());

        DocumentReference reference = reference(eventId, user.getUid());
        DocumentReference eventReference = firestore.collection(Event.COLLECTION).document(eventId);
        // A transaction (not a plain set) so being offline fails right away instead of queueing a write.
        // It also keeps the event's running tally of ratings (how many, and the total of the stars) in step
        // with this person's rating, which is what lets everyone see an average without reading anyone's
        // feedback. The database rules check that the tally changes by exactly this person's rating.
        firestore.runTransaction(transaction -> {
            DocumentSnapshot event = transaction.get(eventReference);
            DocumentSnapshot before = transaction.get(reference);
            Long oldCount = event.getLong(Event.FIELD_RATING_COUNT);
            Long oldSum = event.getLong(Event.FIELD_RATING_SUM);
            boolean hadRating = before.exists();
            Long oldRating = hadRating ? before.getLong(Feedback.FIELD_RATING) : null;
            long count;
            long sum;
            if (hadRating && oldCount == null) {
                // Rated before ratings were tallied: this rating becomes the first one counted.
                count = 1;
                sum = rating;
            } else {
                count = (oldCount == null ? 0 : oldCount) + (hadRating ? 0 : 1);
                sum = (oldSum == null ? 0 : oldSum) + rating - (oldRating == null ? 0 : oldRating);
            }
            Map<String, Object> tally = new HashMap<>();
            tally.put(Event.FIELD_RATING_COUNT, count);
            tally.put(Event.FIELD_RATING_SUM, sum);
            transaction.set(reference, document);
            transaction.update(eventReference, tally);
            return null;
        }).addOnSuccessListener(unused -> callback.onDone())
                .addOnFailureListener(error -> {
                    Log.w(TAG, "sending feedback failed", error);
                    callback.onFailure(toFailure(error));
                });
    }

    @Override
    public void loadForEvent(@NonNull String eventId, @NonNull ListCallback callback) {
        if (auth.getCurrentUser() == null) {
            callback.onFailure(Failure.SIGNED_OUT);
            return;
        }
        firestore.collection(Event.COLLECTION).document(eventId).collection(Feedback.COLLECTION)
                .limit(200)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Feedback> result = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        Feedback feedback = toFeedback(document);
                        if (feedback != null) {
                            result.add(feedback);
                        }
                    }
                    Collections.sort(result, (a, b) -> Long.compare(b.getSubmittedAtMillis(), a.getSubmittedAtMillis()));
                    callback.onLoaded(result);
                })
                .addOnFailureListener(error -> {
                    Log.w(TAG, "loading event feedback failed", error);
                    callback.onFailure(toFailure(error));
                });
    }

    @NonNull
    private DocumentReference reference(@NonNull String eventId, @NonNull String uid) {
        return firestore.collection(Event.COLLECTION).document(eventId)
                .collection(Feedback.COLLECTION).document(uid);
    }

    @Nullable
    private static Feedback toFeedback(@NonNull DocumentSnapshot document) {
        Long rating = document.getLong(Feedback.FIELD_RATING);
        if (rating == null || rating < Feedback.MIN_RATING || rating > Feedback.MAX_RATING) {
            return null;
        }
        List<FeedbackTag> tags = new ArrayList<>();
        Object rawTags = document.get(Feedback.FIELD_TAGS);
        if (rawTags instanceof List) {
            for (Object value : (List<?>) rawTags) {
                FeedbackTag tag = value instanceof String ? FeedbackTag.fromValue((String) value) : null;
                if (tag != null && !tags.contains(tag)) {
                    tags.add(tag);
                }
            }
        }
        String comment = document.getString(Feedback.FIELD_COMMENT);
        String photoUrl = document.getString(Feedback.FIELD_PHOTO_URL);
        Timestamp submitted = document.getTimestamp(Feedback.FIELD_SUBMITTED_AT, ServerTimestampBehavior.ESTIMATE);
        return new Feedback(rating.intValue(), tags, comment == null ? "" : comment.trim(),
                photoUrl == null || photoUrl.trim().isEmpty() ? null : photoUrl.trim(),
                submitted == null ? 0 : submitted.toDate().getTime());
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
