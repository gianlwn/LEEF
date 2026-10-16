package com.itismob.s03.group7.leef.search;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.itismob.s03.group7.leef.model.Event;
import com.itismob.s03.group7.leef.model.EventAudience;
import com.itismob.s03.group7.leef.model.EventCategory;

import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

/**
 * What the person typed and picked on the Search & Filter screen: a keyword plus the MCO1 filters
 * (category, date range, location, audience / age group and price). Immutable; each {@code with...}
 * call returns a changed copy. {@link #matches(Event)} decides whether an event passes.
 */
public final class SearchFilters {

    /** The Free / Paid price filter from the MCO1 scope of work. */
    public enum Price {
        ALL, FREE, PAID
    }

    public static final SearchFilters NONE =
            new SearchFilters("", null, 0, 0, "", null, Price.ALL);

    private final String keyword;
    @Nullable
    private final EventCategory category;
    /** Start of the first day wanted, or 0 for no lower limit. */
    private final long fromDayMillis;
    /** Start of the last day wanted, or 0 for no upper limit. */
    private final long toDayMillis;
    private final String city;
    @Nullable
    private final EventAudience audience;
    private final Price price;

    private SearchFilters(@NonNull String keyword, @Nullable EventCategory category, long fromDayMillis,
                          long toDayMillis, @NonNull String city, @Nullable EventAudience audience,
                          @NonNull Price price) {
        this.keyword = keyword;
        this.category = category;
        this.fromDayMillis = fromDayMillis;
        this.toDayMillis = toDayMillis;
        this.city = city;
        this.audience = audience;
        this.price = price;
    }

    @NonNull
    public String getKeyword() {
        return keyword;
    }

    @Nullable
    public EventCategory getCategory() {
        return category;
    }

    public long getFromDayMillis() {
        return fromDayMillis;
    }

    public long getToDayMillis() {
        return toDayMillis;
    }

    @NonNull
    public String getCity() {
        return city;
    }

    @Nullable
    public EventAudience getAudience() {
        return audience;
    }

    @NonNull
    public Price getPrice() {
        return price;
    }

    @NonNull
    public SearchFilters withKeyword(@NonNull String value) {
        return new SearchFilters(value.trim(), category, fromDayMillis, toDayMillis, city, audience, price);
    }

    @NonNull
    public SearchFilters withCategory(@Nullable EventCategory value) {
        return new SearchFilters(keyword, value, fromDayMillis, toDayMillis, city, audience, price);
    }

    /** @param dayMillis local start of the first day wanted, or 0 to clear it */
    @NonNull
    public SearchFilters withFromDay(long dayMillis) {
        return new SearchFilters(keyword, category, dayMillis, toDayMillis, city, audience, price);
    }

    /** @param dayMillis local start of the last day wanted, or 0 to clear it */
    @NonNull
    public SearchFilters withToDay(long dayMillis) {
        return new SearchFilters(keyword, category, fromDayMillis, dayMillis, city, audience, price);
    }

    @NonNull
    public SearchFilters withCity(@NonNull String value) {
        return new SearchFilters(keyword, category, fromDayMillis, toDayMillis, value.trim(), audience, price);
    }

    @NonNull
    public SearchFilters withAudience(@Nullable EventAudience value) {
        return new SearchFilters(keyword, category, fromDayMillis, toDayMillis, city, value, price);
    }

    @NonNull
    public SearchFilters withPrice(@NonNull Price value) {
        return new SearchFilters(keyword, category, fromDayMillis, toDayMillis, city, audience, value);
    }

    /** Same keyword, every filter from the Filters sheet cleared ("Clear all"). */
    @NonNull
    public SearchFilters withoutSheetFilters() {
        return NONE.withKeyword(keyword);
    }

    /** How many of the sheet's filters are on (the keyword isn't counted). */
    public int sheetFilterCount() {
        int count = 0;
        if (category != null) {
            count++;
        }
        if (fromDayMillis > 0 || toDayMillis > 0) {
            count++;
        }
        if (!city.isEmpty()) {
            count++;
        }
        if (audience != null) {
            count++;
        }
        if (price != Price.ALL) {
            count++;
        }
        return count;
    }

    public boolean hasSheetFilters() {
        return sheetFilterCount() > 0;
    }

    /** True when the event passes the keyword and every filter that is on. */
    public boolean matches(@NonNull Event event) {
        if (category != null && event.getCategory() != category) {
            return false;
        }
        if (price == Price.FREE && !event.isFree()) {
            return false;
        }
        if (price == Price.PAID && event.isFree()) {
            return false;
        }
        if (audience != null && !event.getAudiences().isEmpty() && !event.getAudiences().contains(audience)) {
            // An event with no audience listed is open to everyone, so it always passes.
            return false;
        }
        if (!matchesDates(event) || !matchesCity(event)) {
            return false;
        }
        return matchesKeyword(event);
    }

    /** The event overlaps the chosen days (an event that runs over several days counts if any day is inside). */
    private boolean matchesDates(@NonNull Event event) {
        long start = event.getStartsAtMillis();
        long end = Math.max(start, event.getEndsAtMillis());
        if (fromDayMillis > 0 && end < fromDayMillis) {
            return false;
        }
        return toDayMillis <= 0 || start < endOfDay(toDayMillis);
    }

    private boolean matchesCity(@NonNull Event event) {
        if (city.isEmpty()) {
            return true;
        }
        String wanted = city.toLowerCase(Locale.ROOT);
        return event.getCity().toLowerCase(Locale.ROOT).contains(wanted)
                || event.getVenue().toLowerCase(Locale.ROOT).contains(wanted);
    }

    /** Every word typed has to appear somewhere in the title, organizer, place, tags or category. */
    private boolean matchesKeyword(@NonNull Event event) {
        if (keyword.isEmpty()) {
            return true;
        }
        StringBuilder haystack = new StringBuilder();
        haystack.append(event.getTitle()).append(' ')
                .append(event.getOrganizerName()).append(' ')
                .append(event.getVenue()).append(' ')
                .append(event.getCity()).append(' ')
                .append(event.getCategory().getValue().replace('_', ' '));
        for (String tag : event.getTags()) {
            haystack.append(' ').append(tag);
        }
        String text = haystack.toString().toLowerCase(Locale.ROOT);
        for (String word : keyword.toLowerCase(Locale.ROOT).split("\\s+")) {
            if (!word.isEmpty() && !text.contains(word)) {
                return false;
            }
        }
        return true;
    }

    /** The moment the day that starts at {@code dayStartMillis} ends (the start of the next day). */
    private static long endOfDay(long dayStartMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dayStartMillis);
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        return calendar.getTimeInMillis();
    }

    /** The date picker speaks UTC midnight; this turns its pick into midnight on this phone. */
    public static long utcPickToLocalDay(long utcMillis) {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(utcMillis);
        Calendar local = Calendar.getInstance();
        local.clear();
        local.set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH));
        return local.getTimeInMillis();
    }

    /** The reverse of {@link #utcPickToLocalDay(long)}, to pre-select a day in the date picker. */
    public static long localDayToUtcPick(long localMillis) {
        Calendar local = Calendar.getInstance();
        local.setTimeInMillis(localMillis);
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.clear();
        utc.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH));
        return utc.getTimeInMillis();
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SearchFilters)) {
            return false;
        }
        SearchFilters f = (SearchFilters) other;
        return fromDayMillis == f.fromDayMillis && toDayMillis == f.toDayMillis
                && keyword.equals(f.keyword) && category == f.category && city.equals(f.city)
                && audience == f.audience && price == f.price;
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyword, category, fromDayMillis, toDayMillis, city, audience, price);
    }
}
