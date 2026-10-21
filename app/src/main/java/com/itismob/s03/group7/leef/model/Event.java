package com.itismob.s03.group7.leef.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * An educational event listing, stored in Firestore at {@code events/{eventId}}. Every field here is
 * a database record, never hardcoded in the app. Create/Post Event (Screen 11) writes them from the app.
 *
 * <p>Build one with {@link Builder}; only the id, title, category and start time are required.
 */
public final class Event {

    public static final String COLLECTION = "events";

    public static final String FIELD_TITLE = "title";
    public static final String FIELD_CATEGORY = "category";
    public static final String FIELD_STARTS_AT = "startsAt";
    public static final String FIELD_ENDS_AT = "endsAt";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_VENUE = "venue";
    public static final String FIELD_ADDRESS = "address";
    public static final String FIELD_CITY = "city";
    public static final String FIELD_AUDIENCES = "audiences";
    public static final String FIELD_ORGANIZER_ID = "organizerId";
    public static final String FIELD_CREATED_AT = "createdAt";
    public static final String FIELD_ORGANIZER_NAME = "organizerName";
    public static final String FIELD_ORGANIZER_LOGO_URL = "organizerLogoUrl";
    public static final String FIELD_TAGS = "tags";
    public static final String FIELD_PRICE = "price";
    public static final String FIELD_EARLY_BIRD_PRICE = "earlyBirdPrice";
    public static final String FIELD_EARLY_BIRD_UNTIL = "earlyBirdUntil";
    public static final String FIELD_GROUP_PRICE = "groupPrice";
    public static final String FIELD_COVER_URL = "coverUrl";
    public static final String FIELD_BOOSTED = "boosted";
    public static final String FIELD_BOOSTED_UNTIL = "boostedUntil";

    private final String id;
    private final String title;
    private final EventCategory category;
    private final long startsAtMillis;
    /** 0 when the event has no separate end (a single moment or a one-day event). */
    private final long endsAtMillis;
    private final String description;
    private final String venue;
    /** Street or area under the venue name ("Katipunan, Quezon City"). Empty when unknown. */
    private final String address;
    /** City or province, used by the Location filter ("Manila"). Empty when unknown. */
    private final String city;
    /** Who the event is for. Empty means open to everyone. */
    private final List<EventAudience> audiences;
    /** Account that posted the event (Create/Post Event); empty for events added by hand. */
    private final String organizerId;
    private final String organizerName;
    @Nullable
    private final String organizerLogoUrl;
    private final List<String> tags;
    /** Regular price in pesos; 0 means free. */
    private final double price;
    /** Discounted price while the early-bird offer lasts; negative when there is no offer. */
    private final double earlyBirdPrice;
    /** When the early-bird offer ends; 0 when there is no offer. */
    private final long earlyBirdUntilMillis;
    /** Price per person for a group of {@link TicketType#GROUP_MIN_SIZE} or more; negative when the event has no group rate. */
    private final double groupPrice;
    @Nullable
    private final String coverUrl;
    /** True when the event has been boosted (see {@link #isBoosted()} for whether the boost still runs). */
    private final boolean boosted;
    /** When the paid boost ends; 0 when there is no end (for example an event boosted by hand in the console). */
    private final long boostedUntilMillis;

    private Event(@NonNull Builder b) {
        this.id = b.id;
        this.title = b.title;
        this.category = b.category;
        this.startsAtMillis = b.startsAtMillis;
        this.endsAtMillis = b.endsAtMillis;
        this.description = b.description;
        this.venue = b.venue;
        this.address = b.address;
        this.city = b.city;
        this.audiences = Collections.unmodifiableList(new ArrayList<>(b.audiences));
        this.organizerId = b.organizerId;
        this.organizerName = b.organizerName;
        this.organizerLogoUrl = b.organizerLogoUrl;
        this.tags = Collections.unmodifiableList(new ArrayList<>(b.tags));
        this.price = b.price;
        this.earlyBirdPrice = b.earlyBirdPrice;
        this.earlyBirdUntilMillis = b.earlyBirdUntilMillis;
        this.groupPrice = b.groupPrice;
        this.coverUrl = b.coverUrl;
        this.boosted = b.boosted;
        this.boostedUntilMillis = b.boostedUntilMillis;
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
    public String getDescription() {
        return description;
    }

    @NonNull
    public String getVenue() {
        return venue;
    }

    @NonNull
    public String getAddress() {
        return address;
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

    /** Uid of the account that posted this event, or empty when it was added by hand. */
    @NonNull
    public String getOrganizerId() {
        return organizerId;
    }

    @NonNull
    public String getOrganizerName() {
        return organizerName;
    }

    @Nullable
    public String getOrganizerLogoUrl() {
        return organizerLogoUrl;
    }

    @NonNull
    public List<String> getTags() {
        return tags;
    }

    /** The regular price in pesos (0 = free). */
    public double getPrice() {
        return price;
    }

    public boolean isFree() {
        return price <= 0;
    }

    /** True when the event has an early-bird price and a date it runs until. */
    public boolean hasEarlyBird() {
        return earlyBirdPrice >= 0 && earlyBirdUntilMillis > 0;
    }

    public double getEarlyBirdPrice() {
        return earlyBirdPrice;
    }

    public long getEarlyBirdUntilMillis() {
        return earlyBirdUntilMillis;
    }

    /** True while the early-bird price can still be paid ({@code nowMillis} is before the cut-off). */
    public boolean isEarlyBirdActive(long nowMillis) {
        return hasEarlyBird() && nowMillis < earlyBirdUntilMillis;
    }

    /** What registering costs right now: the early-bird price while it lasts, otherwise the regular price. */
    public double currentPrice(long nowMillis) {
        return isEarlyBirdActive(nowMillis) ? earlyBirdPrice : price;
    }

    /** True when the organizer offers a per-person group rate (Group 5+). */
    public boolean hasGroupPrice() {
        return groupPrice >= 0;
    }

    public double getGroupPrice() {
        return groupPrice;
    }

    @Nullable
    public String getCoverUrl() {
        return coverUrl;
    }

    /** True while a boost is featuring this event: boosted, and the paid time hasn't run out. */
    public boolean isBoosted() {
        return isBoosted(System.currentTimeMillis());
    }

    public boolean isBoosted(long nowMillis) {
        return boosted && (boostedUntilMillis == 0 || boostedUntilMillis > nowMillis);
    }

    /** When the boost ends, or 0 when the event was never given a timed boost. */
    public long getBoostedUntilMillis() {
        return boostedUntilMillis;
    }

    /** True once the event has begun (a boost can only be bought before this). */
    public boolean hasStarted(long nowMillis) {
        return nowMillis >= startsAtMillis;
    }

    /**
     * True once the event is over: after its end, or, when it has no end time, after the day it starts.
     */
    public boolean hasEnded(long nowMillis) {
        if (endsAtMillis > startsAtMillis) {
            return nowMillis > endsAtMillis;
        }
        java.util.Calendar endOfStartDay = java.util.Calendar.getInstance();
        endOfStartDay.setTimeInMillis(startsAtMillis);
        endOfStartDay.set(java.util.Calendar.HOUR_OF_DAY, 23);
        endOfStartDay.set(java.util.Calendar.MINUTE, 59);
        endOfStartDay.set(java.util.Calendar.SECOND, 59);
        return nowMillis > endOfStartDay.getTimeInMillis();
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
                && Double.compare(earlyBirdPrice, event.earlyBirdPrice) == 0
                && earlyBirdUntilMillis == event.earlyBirdUntilMillis
                && Double.compare(groupPrice, event.groupPrice) == 0
                && boosted == event.boosted
                && boostedUntilMillis == event.boostedUntilMillis
                && id.equals(event.id)
                && title.equals(event.title)
                && category == event.category
                && description.equals(event.description)
                && venue.equals(event.venue)
                && address.equals(event.address)
                && city.equals(event.city)
                && audiences.equals(event.audiences)
                && organizerId.equals(event.organizerId)
                && organizerName.equals(event.organizerName)
                && Objects.equals(organizerLogoUrl, event.organizerLogoUrl)
                && tags.equals(event.tags)
                && Objects.equals(coverUrl, event.coverUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, category, startsAtMillis, endsAtMillis, description, venue, address,
                city, audiences, organizerId, organizerName, organizerLogoUrl, tags, price, earlyBirdPrice,
                earlyBirdUntilMillis, groupPrice, coverUrl, boosted, boostedUntilMillis);
    }

    /** Collects an event's fields; anything not set stays empty (or free, or "no early bird"). */
    public static final class Builder {
        private final String id;
        private final String title;
        private final EventCategory category;
        private final long startsAtMillis;
        private long endsAtMillis;
        private String description = "";
        private String venue = "";
        private String address = "";
        private String city = "";
        private List<EventAudience> audiences = Collections.emptyList();
        private String organizerId = "";
        private String organizerName = "";
        @Nullable
        private String organizerLogoUrl;
        private List<String> tags = Collections.emptyList();
        private double price;
        private double earlyBirdPrice = -1;
        private long earlyBirdUntilMillis;
        private double groupPrice = -1;
        @Nullable
        private String coverUrl;
        private boolean boosted;
        private long boostedUntilMillis;

        public Builder(@NonNull String id, @NonNull String title, @NonNull EventCategory category,
                       long startsAtMillis) {
            this.id = id;
            this.title = title;
            this.category = category;
            this.startsAtMillis = startsAtMillis;
        }

        @NonNull
        public Builder endsAt(long millis) {
            this.endsAtMillis = millis;
            return this;
        }

        @NonNull
        public Builder description(@NonNull String value) {
            this.description = value;
            return this;
        }

        @NonNull
        public Builder venue(@NonNull String value) {
            this.venue = value;
            return this;
        }

        @NonNull
        public Builder address(@NonNull String value) {
            this.address = value;
            return this;
        }

        @NonNull
        public Builder city(@NonNull String value) {
            this.city = value;
            return this;
        }

        @NonNull
        public Builder audiences(@NonNull List<EventAudience> value) {
            this.audiences = value;
            return this;
        }

        @NonNull
        public Builder organizerId(@NonNull String value) {
            this.organizerId = value;
            return this;
        }

        @NonNull
        public Builder organizerName(@NonNull String value) {
            this.organizerName = value;
            return this;
        }

        @NonNull
        public Builder organizerLogoUrl(@Nullable String value) {
            this.organizerLogoUrl = value;
            return this;
        }

        @NonNull
        public Builder tags(@NonNull List<String> value) {
            this.tags = value;
            return this;
        }

        @NonNull
        public Builder price(double value) {
            this.price = value;
            return this;
        }

        @NonNull
        public Builder earlyBird(double earlyPrice, long untilMillis) {
            this.earlyBirdPrice = earlyPrice;
            this.earlyBirdUntilMillis = untilMillis;
            return this;
        }

        @NonNull
        public Builder groupPrice(double perPerson) {
            this.groupPrice = perPerson;
            return this;
        }

        @NonNull
        public Builder coverUrl(@Nullable String value) {
            this.coverUrl = value;
            return this;
        }

        @NonNull
        public Builder boosted(boolean value) {
            this.boosted = value;
            return this;
        }

        @NonNull
        public Builder boostedUntil(long millis) {
            this.boostedUntilMillis = millis;
            return this;
        }

        @NonNull
        public Event build() {
            return new Event(this);
        }
    }
}
