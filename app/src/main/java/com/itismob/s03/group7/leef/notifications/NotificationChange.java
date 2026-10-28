package com.itismob.s03.group7.leef.notifications;

/**
 * A tiny counter that goes up when something changes the notifications list inside this app (the list was
 * just opened, a message was read, an event was changed). The bell on Home remembers the number it last
 * counted at and quietly recounts when it differs.
 */
public final class NotificationChange {

    private static volatile int version;

    private NotificationChange() {
    }

    public static int version() {
        return version;
    }

    public static void bump() {
        version++;
    }
}
