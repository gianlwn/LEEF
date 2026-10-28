package com.itismob.s03.group7.leef.organizer;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.itismob.s03.group7.leef.model.Attendee;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Everything the Attendees screen draws at one moment. */
public final class AttendeesState {

    public enum Status {
        LOADING,
        ERROR,
        EMPTY,
        CONTENT
    }

    private final Status status;
    private final List<Attendee> attendees;
    private final Set<String> busyIds;
    @StringRes
    private final int errorRes;

    AttendeesState(@NonNull Status status, @NonNull List<Attendee> attendees, @NonNull Set<String> busyIds,
                   @StringRes int errorRes) {
        this.status = status;
        this.attendees = Collections.unmodifiableList(attendees);
        this.busyIds = Collections.unmodifiableSet(new HashSet<>(busyIds));
        this.errorRes = errorRes;
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    @NonNull
    public List<Attendee> getAttendees() {
        return attendees;
    }

    /** Registrations being checked in right now (their button is disabled). */
    @NonNull
    public Set<String> getBusyIds() {
        return busyIds;
    }

    @StringRes
    public int getErrorRes() {
        return errorRes;
    }

    /** Everyone coming, counting each person of a Group ticket. */
    public int getPeople() {
        int total = 0;
        for (Attendee attendee : attendees) {
            total += attendee.getQuantity();
        }
        return total;
    }

    /** How many of them have been checked in. */
    public int getCheckedInPeople() {
        int total = 0;
        for (Attendee attendee : attendees) {
            if (attendee.isCheckedIn()) {
                total += attendee.getQuantity();
            }
        }
        return total;
    }
}
