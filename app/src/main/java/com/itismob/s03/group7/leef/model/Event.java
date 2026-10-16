package com.itismob.s03.group7.leef.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * An educational event listing, stored in Firestore at {@code events/{eventId}}. Every field here is
 * a database record, never hardcoded in the app. Later screens (Event Details, Create/Post Event)
 * add the remaining MCO1 fields such as the description and attachments.
 */
public final class Event {

    public static final String COLLECTION = "events";

    public static final String FIELD_TITLE = "title";
    public static final String FIELD_CATEGORY = "category";
    public static final String FIELD_STARTS_AT = "startsAt";
    public static final String FIELD_ENDS_AT = "endsAt";
    public static final String FIELD_VENUE = "venue";
    public static final String FIELD_CITY = "city";
    public static final String FIELD_AUDIENCES = "audiences";
    public static final String FIELD_ORGANIZER_NAME = "organizerName";
    public static final String FIELD_TAGS = "tags";
    public static final String FIELD_PRICE = "price";
    public static final String FIELD_COVER_URL = "coverUrl";
    public static final String FIELD_BOOSTED = "boosted";

    private final String id;
    private final String title;
    private final EventCategory category;
    private final long startsAtMillis;
    /** 0 when the event has no separate end (a single moment or a one-day event). */
    private final long endsAtMillis;
    private final String venue;
    /** City or province, used by the Location filter ("Manila"). Empty when unknown. */
    private final String city;
    /** Who the event is for. Empty means open to everyone. */
    private final List<EventAudience> audiences;
    private final String organizerName;
    private final List<String> tags;
    /** Price in pesos; 0 means free. */
    private final double price;
    @Nullable
    private final String coverUrl;
    /** True while an organizer's paid boost is featuring this event. */
    private final boolean boosted;

    public Event(@NonNull String id, @NonNull String title, @NonNull EventCategory category,
                 long startsAtMillis, long endsAtMillis, @NonNull String venue,
                 @NonNull String city, @NonNull List<EventAudience> audiences,
                 @NonNull String organizerName, @NonNull List<String> tags, double price,
                 @Nullable String coverUrl, boolean boosted) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.startsAtMillis = startsAtMillis;
        this.endsAtMillis = endsAtMillis;
        this.venue = venue;
        this.city = city;
        this.audiences = Collections.unmodifiableList(new ArrayList<>(audiences));
        this.organizerName = organizerName;
        this.tags = Collections.unmodifiableList(new ArrayList<>(tags));
        this.price = price;
        this.coverUrl = coverUrl;
        this.boosted = boosted;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @NonNull
    public EventCategory getCategory() {
        return category;
    }

    public long getStartsAtMillis() {
        return startsAtMillis;
    }

    public long getEndsAtMillis() {
        return endsAtMillis;
    }

    @NonNull
    public String getVenue() {
        return venue;
    }

    @NonNull
    public String getCity() {
        return city;
    }

    /** The groups this event is meant for; empty means everyone is welcome. */
    @NonNull
    public List<EventAudience> getAudiences() {
        return audiences;
    }

    @NonNull
    public String getOrganizerName() {
        return organizerName;
    }

    @NonNull
    public List<String> getTags() {
        return tags;
    }

    public double getPrice() {
        return price;
    }

    public boolean isFree() {
        return price <= 0;
    }

    @Nullable
    public String getCoverUrl() {
        return coverUrl;
    }

    public boolean isBoosted() {
        return boosted;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Event)) {
            return false;
        }
        Event event = (Event) other;
        return startsAtMillis == event.startsAtMillis
                && endsAtMillis == event.endsAtMillis
                && Double.compare(price, event.price) == 0
                && boosted == event.boosted
                && id.equals(event.id)
                && title.equals(event.title)
                && category == event.category
                && venue.equals(event.venue)
                && city.equals(event.city)
                && audiences.equals(event.audiences)
                && organizerName.equals(event.organizerName)
                && tags.equals(event.tags)
                && Objects.equals(coverUrl, event.coverUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, category, startsAtMillis, endsAtMillis, venue, city, audiences,
                organizerName, tags, price, coverUrl, boosted);
    }
}
