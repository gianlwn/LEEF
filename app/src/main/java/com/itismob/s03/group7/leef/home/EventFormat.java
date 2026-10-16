package com.itismob.s03.group7.leef.home;

import android.content.Context;

import androidx.annotation.NonNull;

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

    /** "Free" or the price in pesos ("₱500", "₱249.50"). */
    @NonNull
    public static String priceText(@NonNull Context context, @NonNull Event event) {
        if (event.isFree()) {
            return context.getString(R.string.event_price_free);
        }
        double price = event.getPrice();
        boolean whole = price == Math.floor(price);
        String amount = whole
                ? String.format(Locale.getDefault(), "%,d", (long) price)
                : String.format(Locale.getDefault(), "%,.2f", price);
        return context.getString(R.string.event_price_format, amount);
    }
}
