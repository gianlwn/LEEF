package com.itismob.s03.group7.leef.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A person's registration for an event, stored in Firestore at {@code registrations/{uid}_{eventId}}
 * (so each person can register for an event only once). It keeps a copy of the event's title and start
 * so My Events (Screen 8) can list it without loading every event.
 */
public final class Registration {

    public static final String COLLECTION = "registrations";

    public static final String FIELD_USER_ID = "userId";
    public static final String FIELD_EVENT_ID = "eventId";
    public static final String FIELD_EVENT_TITLE = "eventTitle";
    public static final String FIELD_EVENT_STARTS_AT = "eventStartsAt";
    public static final String FIELD_TICKET_TYPE = "ticketType";
    public static final String FIELD_QUANTITY = "quantity";
    public static final String FIELD_AMOUNT = "amount";
    public static final String FIELD_PAYMENT_METHOD = "paymentMethod";
    public static final String FIELD_CONFIRMATION_CODE = "confirmationCode";
    public static final String FIELD_REGISTERED_AT = "registeredAt";
    public static final String FIELD_STATUS = "status";

    public static final String STATUS_CONFIRMED = "confirmed";

    private final String eventId;
    private final String eventTitle;
    private final long eventStartsAtMillis;
    private final TicketType ticketType;
    private final int quantity;
    /** Total paid in pesos (0 for a free event). */
    private final double amount;
    /** Null for a free registration, which has no payment. */
    @Nullable
    private final PaymentMethod paymentMethod;
    private final String confirmationCode;

    public Registration(@NonNull String eventId, @NonNull String eventTitle, long eventStartsAtMillis,
                        @NonNull TicketType ticketType, int quantity, double amount,
                        @Nullable PaymentMethod paymentMethod, @NonNull String confirmationCode) {
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.eventStartsAtMillis = eventStartsAtMillis;
        this.ticketType = ticketType;
        this.quantity = quantity;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.confirmationCode = confirmationCode;
    }

    @NonNull
    public String getEventId() {
        return eventId;
    }

    @NonNull
    public String getEventTitle() {
        return eventTitle;
    }

    public long getEventStartsAtMillis() {
        return eventStartsAtMillis;
    }

    @NonNull
    public TicketType getTicketType() {
        return ticketType;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getAmount() {
        return amount;
    }

    @Nullable
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    @NonNull
    public String getConfirmationCode() {
        return confirmationCode;
    }

    /** The document id: one registration per person per event. */
    @NonNull
    public static String documentId(@NonNull String userId, @NonNull String eventId) {
        return userId + "_" + eventId;
    }
}
