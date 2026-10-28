package com.itismob.s03.group7.leef.reminder;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.itismob.s03.group7.leef.data.EventRepository;
import com.itismob.s03.group7.leef.data.FirebaseEventRepository;
import com.itismob.s03.group7.leef.data.FirebaseRegistrationRepository;
import com.itismob.s03.group7.leef.data.RegistrationRepository;
import com.itismob.s03.group7.leef.model.Event;
import com.itismob.s03.group7.leef.model.Registration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Makes sure every event the person registered for or saved (hearted) has its reminder. Runs when the
 * app opens, so a new phone, a reinstall or a registration made elsewhere still gets reminders, and again
 * whenever a heart is tapped. Reminders for saved events that were un-hearted, cancelled or removed are
 * dropped, and other people's reminders left on the phone are removed.
 */
public final class ReminderSync {

    /** Set the first time the app opens, so a heart tap can ask for a sync without carrying a Context. */
    @Nullable
    private static Context appContext;
    /** Counts syncs; only the newest one is allowed to change reminders (a quick heart then un-heart). */
    private static int generation;

    private ReminderSync() {
    }

    /** Syncs again after a heart changed. Does nothing before the app has opened. */
    public static void refresh() {
        if (appContext != null) {
            run(appContext);
        }
    }

    public static void run(@NonNull Context context) {
        Context app = context.getApplicationContext();
        appContext = app;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        String uid = user.getUid();
        dropOtherAccounts(app, uid);

        final int mine = ++generation;
        new FirebaseRegistrationRepository().loadRegistrations(new RegistrationRepository.ListCallback() {
            @Override
            public void onLoaded(@NonNull List<Registration> registrations) {
                if (mine == generation) {
                    loadSaved(app, uid, registrations, mine);
                }
            }

            @Override
            public void onFailure(@NonNull RegistrationRepository.Failure failure) {
                // Offline: the reminders already set stay as they are.
            }
        });
    }

    private static void dropOtherAccounts(@NonNull Context context, @NonNull String uid) {
        for (Reminder reminder : new ReminderStore(context).all()) {
            if (!reminder.getUid().equals(uid)) {
                ReminderScheduler.cancel(context, reminder.getEventId());
            }
        }
    }

    private static void loadSaved(@NonNull Context context, @NonNull String uid,
                                  @NonNull List<Registration> registrations, int mine) {
        new FirebaseEventRepository().loadSavedEventIds(new EventRepository.SavedIdsCallback() {
            @Override
            public void onLoaded(@NonNull Set<String> eventIds) {
                plan(context, uid, registrations, eventIds, mine);
            }

            @Override
            public void onFailure(@NonNull EventRepository.Failure failure) {
                // The saved list is unknown, so saved reminders are left alone.
                plan(context, uid, registrations, null, mine);
            }
        });
    }

    /** @param saved the ids of the events the person hearted, or null when that couldn't be loaded */
    private static void plan(@NonNull Context context, @NonNull String uid,
                             @NonNull List<Registration> registrations, @Nullable Set<String> saved, int mine) {
        if (mine != generation) {
            return;
        }
        long now = System.currentTimeMillis();
        Map<String, Registration> upcoming = new HashMap<>();
        for (Registration registration : registrations) {
            Reminder probe = new Reminder(registration.getEventId(), uid, registration.getEventTitle(), "",
                    registration.getEventStartsAtMillis());
            if (probe.triggerAtMillis() > now) {
                upcoming.put(registration.getEventId(), registration);
            }
        }
        Set<String> ids = new HashSet<>(upcoming.keySet());
        if (saved != null) {
            ids.addAll(saved);
        }
        if (ids.isEmpty()) {
            dropSavedReminders(context, uid, new HashSet<>(), saved != null);
            return;
        }
        // The venue is on the event, not on the registration, so look the events up to name the place.
        new FirebaseEventRepository().loadEventsByIds(ids, new EventRepository.EventsCallback() {
            @Override
            public void onLoaded(@NonNull List<Event> events) {
                if (mine != generation) {
                    return;
                }
                Map<String, Event> byId = new HashMap<>();
                for (Event event : events) {
                    byId.put(event.getId(), event);
                }
                scheduleAll(context, uid, upcoming, saved, byId);
            }

            @Override
            public void onFailure(@NonNull EventRepository.Failure failure) {
                if (mine == generation) {
                    // Without the events nothing about saved ones is known; keep registrations on time.
                    scheduleAll(context, uid, upcoming, null, new HashMap<>());
                }
            }
        });
    }

    private static void scheduleAll(@NonNull Context context, @NonNull String uid,
                                    @NonNull Map<String, Registration> upcoming, @Nullable Set<String> saved,
                                    @NonNull Map<String, Event> events) {
        Set<String> keep = new HashSet<>();
        for (Registration registration : upcoming.values()) {
            Event event = events.get(registration.getEventId());
            if (event != null && event.isCancelled()) {
                ReminderScheduler.cancel(context, registration.getEventId());
                continue;
            }
            long startsAt = event != null ? event.getStartsAtMillis() : registration.getEventStartsAtMillis();
            String title = event != null ? event.getTitle() : registration.getEventTitle();
            if (ReminderScheduler.schedule(context, new Reminder(registration.getEventId(), uid, title,
                    event == null ? "" : placeOf(event), startsAt))) {
                keep.add(registration.getEventId());
            }
        }
        if (saved == null) {
            return;
        }
        for (String id : saved) {
            Event event = events.get(id);
            if (upcoming.containsKey(id) || event == null || event.isCancelled()) {
                continue;
            }
            // Saved only (not registered): a reminder that opens the event page, not a ticket.
            if (ReminderScheduler.schedule(context, new Reminder(id, uid, event.getTitle(), placeOf(event),
                    event.getStartsAtMillis(), false))) {
                keep.add(id);
            }
        }
        dropSavedReminders(context, uid, keep, true);
    }

    /**
     * Removes the reminders that exist only because of a heart (the event was un-hearted, cancelled or
     * deleted). Reminders for registrations are never removed here, so a registration made while this
     * ran can't lose its reminder.
     */
    private static void dropSavedReminders(@NonNull Context context, @NonNull String uid,
                                           @NonNull Set<String> keep, boolean savedKnown) {
        if (!savedKnown) {
            return;
        }
        for (Reminder reminder : new ReminderStore(context).all()) {
            if (reminder.getUid().equals(uid) && !reminder.isRegistered() && !keep.contains(reminder.getEventId())) {
                ReminderScheduler.cancel(context, reminder.getEventId());
            }
        }
    }

    /** The venue, or the address, or the city: whichever the event has first. */
    @NonNull
    public static String placeOf(@NonNull Event event) {
        if (!event.getVenue().isEmpty()) {
            return event.getVenue();
        }
        return !event.getAddress().isEmpty() ? event.getAddress() : event.getCity();
    }
}
