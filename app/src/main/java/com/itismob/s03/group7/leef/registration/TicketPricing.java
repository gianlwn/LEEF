package com.itismob.s03.group7.leef.registration;

import androidx.annotation.NonNull;

import com.itismob.s03.group7.leef.model.Event;
import com.itismob.s03.group7.leef.model.TicketType;

import java.util.ArrayList;
import java.util.List;

/** Which tickets an event offers right now and what they cost. Every price comes from the event itself. */
final class TicketPricing {

    private TicketPricing() {
    }

    /** Early Bird while the offer lasts, Regular always, Group when the organizer set a group rate. */
    @NonNull
    static List<TicketType> available(@NonNull Event event, long nowMillis) {
        List<TicketType> tickets = new ArrayList<>();
        if (event.isEarlyBirdActive(nowMillis)) {
            tickets.add(TicketType.EARLY_BIRD);
        }
        tickets.add(TicketType.REGULAR);
        if (event.hasGroupPrice()) {
            tickets.add(TicketType.GROUP);
        }
        return tickets;
    }

    /** The ticket picked when the screen opens: the cheapest everyday one (Early Bird, else Regular). */
    @NonNull
    static TicketType defaultTicket(@NonNull Event event, long nowMillis) {
        return event.isEarlyBirdActive(nowMillis) ? TicketType.EARLY_BIRD : TicketType.REGULAR;
    }

    /** Price per person in pesos. */
    static double unitPrice(@NonNull Event event, @NonNull TicketType type) {
        switch (type) {
            case EARLY_BIRD:
                return event.getEarlyBirdPrice();
            case GROUP:
                return event.getGroupPrice();
            case REGULAR:
            default:
                return event.getPrice();
        }
    }

    /** How many people the registration covers: the group size for a Group ticket, otherwise one. */
    static int quantity(@NonNull TicketType type, int groupSize) {
        return type == TicketType.GROUP ? groupSize : 1;
    }

    static double total(@NonNull Event event, @NonNull TicketType type, int groupSize) {
        return unitPrice(event, type) * quantity(type, groupSize);
    }
}
