package com.itismob.s03.group7.leef.home;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.model.Event;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Turns event data into the short text shown on cards: dates like "Nov 8–9, 2026" and prices like "₱500". */
public final class EventFormat {

    private static final String EN_DASH = "–";

    private EventFormat() {
    }

    /**
     * The event's date: one day ("Oct 18, 2026", optionally with the weekday), or a range
     * ("Nov 8–9, 2026", "Nov 30 – Dec 1, 2026").
     */
    @NonNull
    public static String dateText(@NonNull Event event, boolean withWeekday) {
        Locale locale = Locale.getDefault();
        Date start = new Date(event.getStartsAtMillis());
        Calendar startCal = Calendar.getInstance();
        startCal.setTime(start);

        boolean hasEnd = event.getEndsAtMillis() > event.getStartsAtMillis();
        Calendar endCal = Calendar.getInstance();
        if (hasEnd) {
            endCal.setTimeInMillis(event.getEndsAtMillis());
        }
        boolean sameDay = !hasEnd
                || (startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR)
                && startCal.get(Calendar.DAY_OF_YEAR) == endCal.get(Calendar.DAY_OF_YEAR));

        if (sameDay) {
            String pattern = withWeekday ? "MMM d, yyyy (EEE)" : "MMM d, yyyy";
            return new SimpleDateFormat(pattern, locale).format(start);
        }

        Date end = endCal.getTime();
        boolean sameYear = startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR);
        boolean sameMonth = sameYear && startCal.get(Calendar.MONTH) == endCal.get(Calendar.MONTH);
        if (sameMonth) {
            return new SimpleDateFormat("MMM d", locale).format(start) + EN_DASH
                    + new SimpleDateFormat("d, yyyy", locale).format(end);
        }
        if (sameYear) {
            return new SimpleDateFormat("MMM d", locale).format(start) + " " + EN_DASH + " "
                    + new SimpleDateFormat("MMM d, yyyy", locale).format(end);
        }
        SimpleDateFormat full = new SimpleDateFormat("MMM d, yyyy", locale);
        return full.format(start) + " " + EN_DASH + " " + full.format(end);
    }

    /** "Free" or what registering costs right now in pesos: the early-bird price while it lasts ("₱350"), otherwise the regular one. */
    @NonNull
    public static String priceText(@NonNull Context context, @NonNull Event event) {
        return priceText(context, event.currentPrice(System.currentTimeMillis()));
    }

    /** "Free" for 0 or less, otherwise the amount in pesos ("₱500", "₱249.50"). */
    @NonNull
    public static String priceText(@NonNull Context context, double price) {
        if (price <= 0) {
            return context.getString(R.string.event_price_free);
        }
        boolean whole = price == Math.floor(price);
        String amount = whole
                ? String.format(Locale.getDefault(), "%,d", (long) price)
                : String.format(Locale.getDefault(), "%,.2f", price);
        return context.getString(R.string.event_price_format, amount);
    }

    /**
     * The date line on Event Details: "Sat, Oct 18, 2026" for a one-day event, or the range
     * ("Nov 8–9, 2026") for a longer one.
     */
    @NonNull
    public static String detailDateText(@NonNull Event event) {
        if (isMultiDay(event)) {
            return dateText(event, false);
        }
        return new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
                .format(new Date(event.getStartsAtMillis()));
    }

    /**
     * The time line on Event Details: "9:00 AM – 4:00 PM", "9:00 AM", or "Starts 9:00 AM · Ends 4:00 PM"
     * for a multi-day event. Null when the event only has a date (it starts at midnight and has no end).
     */
    @Nullable
    public static String timeText(@NonNull Context context, @NonNull Event event) {
        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(event.getStartsAtMillis());
        boolean hasEnd = event.getEndsAtMillis() > event.getStartsAtMillis();
        boolean midnight = start.get(Calendar.HOUR_OF_DAY) == 0 && start.get(Calendar.MINUTE) == 0;
        if (midnight && !hasEnd) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat("h:mm a", Locale.getDefault());
        String startText = format.format(new Date(event.getStartsAtMillis()));
        if (!hasEnd) {
            return startText;
        }
        String endText = format.format(new Date(event.getEndsAtMillis()));
        if (isMultiDay(event)) {
            return context.getString(R.string.event_time_multiday, startText, endText);
        }
        return startText + " " + EN_DASH + " " + endText;
    }

    /**
     * The date line on My Events: "Oct 18, 2026 | 9:00 AM", or just the date for a multi-day event or one
     * that starts at midnight. Works from the start time alone when the event itself is gone.
     */
    @NonNull
    public static String dateTimeLine(@Nullable Event event, long startsAtMillis) {
        Locale locale = Locale.getDefault();
        if (event != null && isMultiDay(event)) {
            return dateText(event, false);
        }
        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(startsAtMillis);
        String date = new SimpleDateFormat("MMM d, yyyy", locale).format(new Date(startsAtMillis));
        boolean midnight = start.get(Calendar.HOUR_OF_DAY) == 0 && start.get(Calendar.MINUTE) == 0;
        if (midnight) {
            return date;
        }
        return date + " | " + new SimpleDateFormat("h:mm a", locale).format(new Date(startsAtMillis));
    }

    /** "Oct 1": a short date for captions such as "Early bird (until Oct 1)". */
    @NonNull
    public static String shortDateText(long millis) {
        return new SimpleDateFormat("MMM d", Locale.getDefault()).format(new Date(millis));
    }

    /** The average stars with one decimal ("4.6"; a whole number reads "5.0"). */
    @NonNull
    public static String averageText(@NonNull Event event) {
        return String.format(Locale.getDefault(), "%.1f", event.getAverageRating());
    }

    private static boolean isMultiDay(@NonNull Event event) {
        if (event.getEndsAtMillis() <= event.getStartsAtMillis()) {
            return false;
        }
        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(event.getStartsAtMillis());
        Calendar end = Calendar.getInstance();
        end.setTimeInMillis(event.getEndsAtMillis());
        return start.get(Calendar.YEAR) != end.get(Calendar.YEAR)
                || start.get(Calendar.DAY_OF_YEAR) != end.get(Calendar.DAY_OF_YEAR);
    }
}
