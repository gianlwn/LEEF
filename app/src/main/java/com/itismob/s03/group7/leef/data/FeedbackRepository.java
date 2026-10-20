package com.itismob.s03.group7.leef.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.itismob.s03.group7.leef.model.Feedback;
import com.itismob.s03.group7.leef.model.FeedbackTag;

import java.util.List;

/** Event feedback the screens rely on; the Firebase code lives in {@link FirebaseFeedbackRepository}. */
public interface FeedbackRepository {

    /** Why feedback couldn't be loaded or saved. */
    enum Failure {
        SIGNED_OUT,
        NETWORK,
        /** The database rules refused it (not registered, the event hasn't started, or rules not published). */
        PERMISSION,
        UNKNOWN
    }

    interface MineCallback {
        /** @param feedback the person's feedback for the event, or null if they haven't given any */
        void onLoaded(@Nullable Feedback feedback);

        void onFailure(@NonNull Failure failure);
    }

    interface ListCallback {
        void onLoaded(@NonNull List<Feedback> feedback);

        void onFailure(@NonNull Failure failure);
    }

    interface DoneCallback {
        void onDone();

        void onFailure(@NonNull Failure failure);
    }

    /** The signed-in person's feedback for one event, if any. */
    void loadMine(@NonNull String eventId, @NonNull MineCallback callback);

    /** Sends (or replaces) the signed-in person's feedback for an event. */
    void submit(@NonNull String eventId, int rating, @NonNull List<FeedbackTag> tags, @NonNull String comment,
                @Nullable String photoUrl, @NonNull DoneCallback callback);

    /** Everyone's feedback for an event, newest first. Only the event's organizer is allowed to ask. */
    void loadForEvent(@NonNull String eventId, @NonNull ListCallback callback);
}
