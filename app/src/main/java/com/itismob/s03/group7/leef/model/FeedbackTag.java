package com.itismob.s03.group7.leef.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.itismob.s03.group7.leef.R;

/**
 * The quick "what stood out" chips on the feedback form (Very helpful, Well organized...). A fixed option
 * list from the wireframe, not a database record, so it lives in the app. {@link #getValue()} is what
 * a feedback document stores.
 */
public enum FeedbackTag {
    VERY_HELPFUL("very_helpful", R.string.feedback_tag_very_helpful),
    WELL_ORGANIZED("well_organized", R.string.feedback_tag_well_organized),
    GREAT_SPEAKERS("great_speakers", R.string.feedback_tag_great_speakers),
    LEARNED_A_LOT("learned_a_lot", R.string.feedback_tag_learned_a_lot),
    MET_NEW_PEOPLE("met_new_people", R.string.feedback_tag_met_new_people),
    WOULD_RECOMMEND("would_recommend", R.string.feedback_tag_would_recommend);

    private final String value;
    private final int labelRes;

    FeedbackTag(@NonNull String value, @StringRes int labelRes) {
        this.value = value;
        this.labelRes = labelRes;
    }

    /** Stable identifier saved in Firestore ("very_helpful", ...). */
    @NonNull
    public String getValue() {
        return value;
    }

    @StringRes
    public int getLabelRes() {
        return labelRes;
    }

    /** The tag saved under {@code value}, or null if it is unknown. */
    @Nullable
    public static FeedbackTag fromValue(@Nullable String value) {
        for (FeedbackTag tag : values()) {
            if (tag.value.equals(value)) {
                return tag;
            }
        }
        return null;
    }
}
