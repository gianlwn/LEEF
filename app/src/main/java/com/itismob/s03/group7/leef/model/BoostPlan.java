package com.itismob.s03.group7.leef.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.itismob.s03.group7.leef.R;

/**
 * How long an organizer can boost (promote) an event, and what it costs. The prices are the ones in
 * the project proposal. A boost features the event on the Home screen and lists it first in Search.
 * To change a price, change it here <em>and</em> in {@code firestore.rules} (the rules check it).
 */
public enum BoostPlan {
    DAY("day", 1, 599, R.string.boost_plan_day, R.string.boost_plan_day_hint),
    THREE_DAYS("three_days", 3, 899, R.string.boost_plan_three_days, R.string.boost_plan_three_days_hint),
    WEEK("week", 7, 1299, R.string.boost_plan_week, R.string.boost_plan_week_hint);

    public static final long DAY_MILLIS = 24L * 60 * 60 * 1000;
    /** A boost can never run more than this far into the future, however often it is extended. */
    public static final int MAX_DAYS_AHEAD = 30;

    private final String value;
    private final int days;
    private final int price;
    private final int labelRes;
    private final int hintRes;

    BoostPlan(@NonNull String value, int days, int price, @StringRes int labelRes, @StringRes int hintRes) {
        this.value = value;
        this.days = days;
        this.price = price;
        this.labelRes = labelRes;
        this.hintRes = hintRes;
    }

    /** How the plan is stored in Firestore. */
    @NonNull
    public String getValue() {
        return value;
    }

    public int getDays() {
        return days;
    }

    public long getDurationMillis() {
        return days * DAY_MILLIS;
    }

    /** Price in whole pesos. */
    public int getPrice() {
        return price;
    }

    @StringRes
    public int getLabelRes() {
        return labelRes;
    }

    @StringRes
    public int getHintRes() {
        return hintRes;
    }

    @Nullable
    public static BoostPlan fromValue(@Nullable String value) {
        for (BoostPlan plan : values()) {
            if (plan.value.equals(value)) {
                return plan;
            }
        }
        return null;
    }
}
