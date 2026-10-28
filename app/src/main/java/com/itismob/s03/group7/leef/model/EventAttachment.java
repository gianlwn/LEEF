package com.itismob.s03.group7.leef.model;

import androidx.annotation.NonNull;

import java.util.Locale;

/**
 * A file the organizer links to from an event (a flyer, a PDF, a form): a name to show and a web link.
 * Files are links because uploading them needs Firebase Storage, which is not on the free plan.
 */
public final class EventAttachment {

    public static final String FIELD_NAME = "name";
    public static final String FIELD_URL = "url";

    /** The most files one event can link to. */
    public static final int MAX_COUNT = 3;
    public static final int MAX_NAME_LENGTH = 40;
    public static final int MAX_URL_LENGTH = 500;

    private final String name;
    private final String url;

    public EventAttachment(@NonNull String name, @NonNull String url) {
        this.name = name;
        this.url = url;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getUrl() {
        return url;
    }

    /** True for an https link with no spaces, short enough to store. */
    public static boolean isValidUrl(@NonNull String url) {
        return url.toLowerCase(Locale.ROOT).startsWith("https://")
                && url.length() > 12
                && url.length() <= MAX_URL_LENGTH
                && !url.contains(" ");
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof EventAttachment)) {
            return false;
        }
        EventAttachment that = (EventAttachment) other;
        return name.equals(that.name) && url.equals(that.url);
    }

    @Override
    public int hashCode() {
        return 31 * name.hashCode() + url.hashCode();
    }
}
