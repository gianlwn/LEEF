package com.itismob.s03.group7.leef.reminder;

import androidx.annotation.NonNull;

import java.util.Calendar;

/**
 * One event reminder: which person it is for, what to say, and when to show it. Reminders are kept on
 * the phone (see {@link ReminderStore}) because they exist only to set an alarm.
 */
public final class Reminder {

    /** How long before the start the reminder goes off ("starts in 1 hour"). */
    public static final long LEAD_MILLIS = 60L * 60L * 1000L;

    /** For an event with no start time (it starts at midnight), the reminder goes off at 8:00 AM that day. */
    private static final int DATE_ONLY_REMINDER_HOUR = 8;

    private final String eventId;
    private final String uid;
    private final String title;
    private final String place;
    private final long startsAtMillis;
    private final boolean registered;

    /** A reminder for an event the person registered for. */
    public Reminder(@NonNull String eventId, @NonNull String uid, @NonNull String title,
                    @NonNull String place, long startsAtMillis) {
        this(eventId, uid, title, place, startsAtMillis, true);
    }

    /**
     * @param registered true for an event the person registered for (the reminder opens their ticket),
     *                   false for an event they only saved with the heart (it opens the event page)
     */
    public Reminder(@NonNull String eventId, @NonNull String uid, @NonNull String title,
                    @NonNull String place, long startsAtMillis, boolean registered) {
        this.registered = registered;
        this.eventId = eventId;
        this.uid = uid;
        this.title = title;
        this.place = place;
        this.startsAtMillis = startsAtMillis;
    }

    @NonNull
    public String getEventId() {
        return eventId;
    }

    /** The signed-in account the reminder belongs to. */
    @NonNull
    public String getUid() {
        return uid;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    /** The venue (or address, or city) to mention, or empty when unknown. */
    @NonNull
    public String getPlace() {
        return place;
    }

    public long getStartsAtMillis() {
        return startsAtMillis;
    }

    /** False when the person only saved the event (hearted it) and has no ticket for it. */
    public boolean isRegistered() {
        return registered;
    }

    /** True when the event's start time is unknown (it starts at exactly midnight). */
    public boolean isDateOnly() {
        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(startsAtMillis);
        return start.get(Calendar.HOUR_OF_DAY) == 0 && start.get(Calendar.MINUTE) == 0;
    }

    /** When the notification should appear. */
    public long triggerAtMillis() {
        if (isDateOnly()) {
            Calendar morning = Calendar.getInstance();
            morning.setTimeInMillis(startsAtMillis);
            morning.set(Calendar.HOUR_OF_DAY, DATE_ONLY_REMINDER_HOUR);
            return morning.getTimeInMillis();
        }
        return startsAtMillis - LEAD_MILLIS;
    }
}
