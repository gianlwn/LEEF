package com.itismob.s03.group7.leef.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * One registration as the event's organizer sees it: who is coming, with which ticket, and whether
 * they have been checked in. Built from {@code registrations/{uid}_{eventId}}; it carries the name
 * (never an email or any payment detail).
 */
public final class Attendee {

    private final String documentId;
    private final String eventId;
    /** The registrant's name, or empty for a registration made before names were kept. */
    private final String name;
    private final TicketType ticketType;
    private final int quantity;
    private final String confirmationCode;
    /** When the person checked in at the venue; 0 if they haven't. */
    private final long checkedInAtMillis;

    public Attendee(@NonNull String documentId, @NonNull String eventId, @NonNull String name,
                    @NonNull TicketType ticketType, int quantity, @NonNull String confirmationCode,
                    long checkedInAtMillis) {
        this.documentId = documentId;
        this.eventId = eventId;
        this.name = name;
        this.ticketType = ticketType;
        this.quantity = quantity;
        this.confirmationCode = confirmationCode;
        this.checkedInAtMillis = checkedInAtMillis;
    }

    @NonNull
    public String getDocumentId() {
        return documentId;
    }

    @NonNull
    public String getEventId() {
        return eventId;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public TicketType getTicketType() {
        return ticketType;
    }

    /** How many people the registration covers (more than 1 for a Group ticket). */
    public int getQuantity() {
        return quantity;
    }

    @NonNull
    public String getConfirmationCode() {
        return confirmationCode;
    }

    public long getCheckedInAtMillis() {
        return checkedInAtMillis;
    }

    public boolean isCheckedIn() {
        return checkedInAtMillis > 0;
    }

    /** The same registration, marked as checked in at this time. */
    @NonNull
    public Attendee withCheckedIn(long millis) {
        return new Attendee(documentId, eventId, name, ticketType, quantity, confirmationCode, millis);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Attendee)) {
            return false;
        }
        Attendee a = (Attendee) other;
        return quantity == a.quantity
                && checkedInAtMillis == a.checkedInAtMillis
                && documentId.equals(a.documentId)
                && eventId.equals(a.eventId)
                && name.equals(a.name)
                && ticketType == a.ticketType
                && confirmationCode.equals(a.confirmationCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(documentId, eventId, name, ticketType, quantity, confirmationCode, checkedInAtMillis);
    }
}
