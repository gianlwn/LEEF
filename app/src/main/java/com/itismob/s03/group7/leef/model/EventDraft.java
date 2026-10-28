package com.itismob.s03.group7.leef.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A new event as the organizer filled it in on Create Event, before it is saved. The repository adds
 * the organizer's uid and the creation time when it writes {@link #toDocument()} to Firestore.
 */
public final class EventDraft {

    private final String title;
    private final String description;
    private final EventCategory category;
    private final long startsAtMillis;
    private final long endsAtMillis;
    private final String venue;
    private final String city;
    private final List<EventAudience> audiences;
    private final List<String> tags;
    private final double price;
    private final double earlyBirdPrice;
    private final long earlyBirdUntilMillis;
    private final double groupPrice;
    @Nullable
    private final String coverUrl;
    private final String organizerName;

    /**
     * @param endsAtMillis         0 when the event has no separate end time
     * @param earlyBirdPrice       negative when there is no early-bird offer
     * @param earlyBirdUntilMillis 0 when there is no early-bird offer
     * @param groupPrice           negative when there is no group rate
     */
    public EventDraft(@NonNull String title, @NonNull String description, @NonNull EventCategory category,
                      long startsAtMillis, long endsAtMillis, @NonNull String venue, @NonNull String city,
                      @NonNull List<EventAudience> audiences, @NonNull List<String> tags, double price,
                      double earlyBirdPrice, long earlyBirdUntilMillis, double groupPrice,
                      @Nullable String coverUrl, @NonNull String organizerName) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.startsAtMillis = startsAtMillis;
        this.endsAtMillis = endsAtMillis;
        this.venue = venue;
        this.city = city;
        this.audiences = Collections.unmodifiableList(new ArrayList<>(audiences));
        this.tags = Collections.unmodifiableList(new ArrayList<>(tags));
        this.price = price;
        this.earlyBirdPrice = earlyBirdPrice;
        this.earlyBirdUntilMillis = earlyBirdUntilMillis;
        this.groupPrice = groupPrice;
        this.coverUrl = coverUrl;
        this.organizerName = organizerName;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    /**
     * The Firestore document for this event. Optional fields are left out when empty, and a new event is
     * never boosted (a boost is a paid feature, not something an organizer can switch on here).
     */
    @NonNull
    public Map<String, Object> toDocument() {
        Map<String, Object> document = new HashMap<>();
        document.put(Event.FIELD_TITLE, title);
        document.put(Event.FIELD_CATEGORY, category.getValue());
        document.put(Event.FIELD_STARTS_AT, timestamp(startsAtMillis));
        if (endsAtMillis > startsAtMillis) {
            document.put(Event.FIELD_ENDS_AT, timestamp(endsAtMillis));
        }
        if (!description.isEmpty()) {
            document.put(Event.FIELD_DESCRIPTION, description);
        }
        if (!venue.isEmpty()) {
            document.put(Event.FIELD_VENUE, venue);
        }
        if (!city.isEmpty()) {
            document.put(Event.FIELD_CITY, city);
        }
        List<String> audienceValues = new ArrayList<>();
        for (EventAudience audience : audiences) {
            audienceValues.add(audience.getValue());
        }
        document.put(Event.FIELD_AUDIENCES, audienceValues);
        document.put(Event.FIELD_TAGS, new ArrayList<>(tags));
        document.put(Event.FIELD_PRICE, price);
        if (earlyBirdPrice >= 0 && earlyBirdUntilMillis > 0) {
            document.put(Event.FIELD_EARLY_BIRD_PRICE, earlyBirdPrice);
            document.put(Event.FIELD_EARLY_BIRD_UNTIL, timestamp(earlyBirdUntilMillis));
        }
        if (groupPrice >= 0) {
            document.put(Event.FIELD_GROUP_PRICE, groupPrice);
        }
        if (coverUrl != null && !coverUrl.isEmpty()) {
            document.put(Event.FIELD_COVER_URL, coverUrl);
        }
        document.put(Event.FIELD_ORGANIZER_NAME, organizerName);
        document.put(Event.FIELD_BOOSTED, false);
        return document;
    }

    /**
     * The changes that turn an existing event into this version of it (Edit Event). Every field an
     * organizer can edit is listed: a value when there is one, otherwise {@link FieldValue#delete()} so
     * an optional field the organizer cleared (end time, description, early bird, ...) really goes away.
     * The organizer, creation time, boost and cancellation fields are never touched.
     */
    @NonNull
    public Map<String, Object> toUpdate() {
        Map<String, Object> changes = new HashMap<>(toDocument());
        changes.remove(Event.FIELD_ORGANIZER_NAME);
        changes.remove(Event.FIELD_BOOSTED);
        String[] optional = {Event.FIELD_ENDS_AT, Event.FIELD_DESCRIPTION, Event.FIELD_VENUE, Event.FIELD_CITY,
                Event.FIELD_EARLY_BIRD_PRICE, Event.FIELD_EARLY_BIRD_UNTIL, Event.FIELD_GROUP_PRICE,
                Event.FIELD_COVER_URL};
        for (String field : optional) {
            if (!changes.containsKey(field)) {
                changes.put(field, FieldValue.delete());
            }
        }
        return changes;
    }

    @NonNull
    private static Timestamp timestamp(long millis) {
        return new Timestamp(new Date(millis));
    }
}
