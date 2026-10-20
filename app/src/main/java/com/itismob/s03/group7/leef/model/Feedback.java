package com.itismob.s03.group7.leef.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One person's feedback on one event, stored in Firestore at {@code events/{eventId}/feedback/{uid}}
 * (so each person has a single piece of feedback per event, which they can change). Only the person and
 * the event's organizer can read it.
 */
public final class Feedback {

    public static final String COLLECTION = "feedback";

    public static final String FIELD_USER_ID = "userId";
    public static final String FIELD_RATING = "rating";
    public static final String FIELD_TAGS = "tags";
    public static final String FIELD_COMMENT = "comment";
    public static final String FIELD_PHOTO_URL = "photoUrl";
    public static final String FIELD_SUBMITTED_AT = "submittedAt";

    public static final int MIN_RATING = 1;
    public static final int MAX_RATING = 5;
    public static final int MAX_COMMENT_LENGTH = 300;

    private final int rating;
    private final List<FeedbackTag> tags;
    private final String comment;
    @Nullable
    private final String photoUrl;
    private final long submittedAtMillis;

    public Feedback(int rating, @NonNull List<FeedbackTag> tags, @NonNull String comment,
                    @Nullable String photoUrl, long submittedAtMillis) {
        this.rating = rating;
        this.tags = Collections.unmodifiableList(new ArrayList<>(tags));
        this.comment = comment;
        this.photoUrl = photoUrl;
        this.submittedAtMillis = submittedAtMillis;
    }

    /** Stars given, 1 to 5. */
    public int getRating() {
        return rating;
    }

    @NonNull
    public List<FeedbackTag> getTags() {
        return tags;
    }

    /** What the person wrote; empty when they only gave stars. */
    @NonNull
    public String getComment() {
        return comment;
    }

    @Nullable
    public String getPhotoUrl() {
        return photoUrl;
    }

    /** When it was last sent (0 while the server time hasn't been filled in yet). */
    public long getSubmittedAtMillis() {
        return submittedAtMillis;
    }
}
