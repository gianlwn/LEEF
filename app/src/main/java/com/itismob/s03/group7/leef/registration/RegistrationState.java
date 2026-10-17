package com.itismob.s03.group7.leef.registration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.itismob.s03.group7.leef.data.EventRepository;
import com.itismob.s03.group7.leef.model.Event;
import com.itismob.s03.group7.leef.model.PaymentMethod;
import com.itismob.s03.group7.leef.model.Registration;
import com.itismob.s03.group7.leef.model.TicketType;

import java.util.Collections;
import java.util.List;

/** Everything the Registration & Payment screen draws at one moment. */
public final class RegistrationState {

    public enum Status {
        LOADING,
        /** Couldn't load the event; see {@link #getFailure()}. */
        ERROR,
        /** The event doesn't exist (any more). */
        NOT_FOUND,
        /** The event is over, so nobody can register. */
        CLOSED,
        /** Choosing a ticket and a way to pay. */
        FORM,
        /** Registered: the confirmation is showing. */
        DONE
    }

    private final Status status;
    @Nullable
    private final Event event;
    @Nullable
    private final EventRepository.Failure failure;
    private final List<TicketType> tickets;
    private final TicketType ticket;
    private final int groupSize;
    private final PaymentMethod method;
    private final boolean paying;
    @Nullable
    private final Registration registration;
    private final boolean alreadyRegistered;

    RegistrationState(@NonNull Status status, @Nullable Event event, @Nullable EventRepository.Failure failure,
                      @NonNull List<TicketType> tickets, @NonNull TicketType ticket, int groupSize,
                      @NonNull PaymentMethod method, boolean paying, @Nullable Registration registration,
                      boolean alreadyRegistered) {
        this.status = status;
        this.event = event;
        this.failure = failure;
        this.tickets = Collections.unmodifiableList(tickets);
        this.ticket = ticket;
        this.groupSize = groupSize;
        this.method = method;
        this.paying = paying;
        this.registration = registration;
        this.alreadyRegistered = alreadyRegistered;
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    @Nullable
    public Event getEvent() {
        return event;
    }

    @Nullable
    public EventRepository.Failure getFailure() {
        return failure;
    }

    /** The ticket types on offer, in display order. */
    @NonNull
    public List<TicketType> getTickets() {
        return tickets;
    }

    @NonNull
    public TicketType getTicket() {
        return ticket;
    }

    /** How many people a Group ticket covers (5 or more). */
    public int getGroupSize() {
        return groupSize;
    }

    @NonNull
    public PaymentMethod getMethod() {
        return method;
    }

    /** True while a payment is being processed. */
    public boolean isPaying() {
        return paying;
    }

    /** The finished registration once {@link Status#DONE}. */
    @Nullable
    public Registration getRegistration() {
        return registration;
    }

    /** True when the person had already registered before opening the screen. */
    public boolean isAlreadyRegistered() {
        return alreadyRegistered;
    }

    public int getQuantity() {
        return TicketPricing.quantity(ticket, groupSize);
    }

    /** Price per person for the chosen ticket. */
    public double priceOf(@NonNull TicketType type) {
        return event == null ? 0 : TicketPricing.unitPrice(event, type);
    }

    /** What the person pays in total, in pesos. */
    public double getTotal() {
        return event == null ? 0 : TicketPricing.total(event, ticket, groupSize);
    }

    /** True when there is nothing to pay, so no payment method is needed. */
    public boolean isFree() {
        return getTotal() <= 0;
    }
}
