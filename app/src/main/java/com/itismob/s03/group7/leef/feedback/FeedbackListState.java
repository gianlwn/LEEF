package com.itismob.s03.group7.leef.feedback;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.itismob.s03.group7.leef.model.Feedback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** What the organizer's feedback list is showing. */
public final class FeedbackListState {

    public enum Status {
        LOADING, ERROR, EMPTY, CONTENT
    }

    private final Status status;
    private final List<Feedback> feedback;
    private final double average;
    @StringRes
    private final int errorRes;

    FeedbackListState(@NonNull Status status, @NonNull List<Feedback> feedback, double average,
                      @StringRes int errorRes) {
        this.status = status;
        this.feedback = Collections.unmodifiableList(new ArrayList<>(feedback));
        this.average = average;
        this.errorRes = errorRes;
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    /** Newest first. */
    @NonNull
    public List<Feedback> getFeedback() {
        return feedback;
    }

    /** Mean of all the ratings, 0 when there are none. */
    public double getAverage() {
        return average;
    }

    @StringRes
    public int getErrorRes() {
        return errorRes;
    }
}
