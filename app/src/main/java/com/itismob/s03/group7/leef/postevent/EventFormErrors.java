package com.itismob.s03.group7.leef.postevent;

import androidx.annotation.StringRes;

/** The problems found with the form, one string resource per field (0 = that field is fine). */
public final class EventFormErrors {

    @StringRes
    public int title;
    @StringRes
    public int category;
    @StringRes
    public int start;
    @StringRes
    public int end;
    @StringRes
    public int venue;
    @StringRes
    public int price;
    @StringRes
    public int earlyPrice;
    @StringRes
    public int earlyUntil;
    @StringRes
    public int groupPrice;

    public boolean hasAny() {
        return title != 0 || category != 0 || start != 0 || end != 0 || venue != 0 || price != 0
                || earlyPrice != 0 || earlyUntil != 0 || groupPrice != 0;
    }
}
