package com.itismob.s03.group7.leef.model;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.itismob.s03.group7.leef.R;

/**
 * The event categories from the MCO1 scope of work (seminars, workshops, competitions and
 * school-related activities). This is a fixed option list, not a database record, so it lives in the
 * app. {@link #getValue()} is what an event stores in Firestore.
 */
public enum EventCategory {
    SEMINAR("seminar",
            R.string.category_seminars, R.string.category_badge_seminar,
            R.drawable.ic_cat_seminar, R.color.leef_cat_seminar_bg, R.color.leef_cat_seminar_icon,
            R.color.leef_cat_seminar_badge),
    WORKSHOP("workshop",
            R.string.category_workshops, R.string.category_badge_workshop,
            R.drawable.ic_cat_workshop, R.color.leef_cat_workshop_bg, R.color.leef_cat_workshop_icon,
            R.color.leef_cat_workshop_badge),
    COMPETITION("competition",
            R.string.category_competitions, R.string.category_badge_competition,
            R.drawable.ic_cat_competition, R.color.leef_cat_competition_bg, R.color.leef_cat_competition_icon,
            R.color.leef_cat_competition_badge),
    SCHOOL_ACTIVITY("school_activity",
            R.string.category_school_activities, R.string.category_badge_school_activity,
            R.drawable.ic_cat_school, R.color.leef_cat_school_bg, R.color.leef_cat_school_icon,
            R.color.leef_cat_school_badge);

    private final String value;
    private final int labelRes;
    private final int badgeRes;
    private final int iconRes;
    private final int chipColorRes;
    private final int chipIconColorRes;
    private final int badgeColorRes;

    EventCategory(@NonNull String value, @StringRes int labelRes, @StringRes int badgeRes,
                  @DrawableRes int iconRes, @ColorRes int chipColorRes, @ColorRes int chipIconColorRes,
                  @ColorRes int badgeColorRes) {
        this.value = value;
        this.labelRes = labelRes;
        this.badgeRes = badgeRes;
        this.iconRes = iconRes;
        this.chipColorRes = chipColorRes;
        this.chipIconColorRes = chipIconColorRes;
        this.badgeColorRes = badgeColorRes;
    }

    /** Stable identifier saved in Firestore ("seminar", "workshop", ...). */
    @NonNull
    public String getValue() {
        return value;
    }

    /** Plural label used on the category chips ("Seminars"). */
    @StringRes
    public int getLabelRes() {
        return labelRes;
    }

    /** Short singular label used on the event badge ("SEMINAR"). */
    @StringRes
    public int getBadgeRes() {
        return badgeRes;
    }

    @DrawableRes
    public int getIconRes() {
        return iconRes;
    }

    /** Soft pastel behind the chip's icon when the chip is not selected. */
    @ColorRes
    public int getChipColorRes() {
        return chipColorRes;
    }

    /** Colour of the chip's icon when the chip is not selected. */
    @ColorRes
    public int getChipIconColorRes() {
        return chipIconColorRes;
    }

    /** Solid colour behind the badge (white text on top). */
    @ColorRes
    public int getBadgeColorRes() {
        return badgeColorRes;
    }

    /** The category saved under {@code value}, or null if it is unknown. */
    @Nullable
    public static EventCategory fromValue(@Nullable String value) {
        for (EventCategory category : values()) {
            if (category.value.equals(value)) {
                return category;
            }
        }
        return null;
    }
}
