package com.itismob.s03.group7.leef.home;

/**
 * A tiny counter that goes up whenever the list of events changes inside this app (for example when
 * an organizer posts one). Home remembers the number it last loaded at and quietly reloads when it differs,
 * so a new event shows without a pull to refresh.
 */
public final class FeedChange {

    private static volatile int version;

    private FeedChange() {
    }

    public static int version() {
        return version;
    }

    public static void bump() {
        version++;
    }
}
