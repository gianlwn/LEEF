package com.itismob.s03.group7.leef.reminder;

import android.content.Context;

import androidx.annotation.NonNull;

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
 * Makes sure every event the person has registered for has its reminder. Runs when the app opens, so a
 * new phone, a reinstall or a registration made elsewhere still gets reminders. Other people's reminders
 * left on the phone are removed.
 */
public final class ReminderSync {

    private ReminderSync() {
    }

    public static void run(@NonNull Context context) {
        Context app = context.getApplicationContext();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        String uid = user.getUid();
        dropOtherAccounts(app, uid);

        new FirebaseRegistrationRepository().loadRegistrations(new RegistrationRepository.ListCallback() {
            @Override
            public void onLoaded(@NonNull List<Registration> registrations) {
                scheduleUpcoming(app, uid, registrations);
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

    private static void scheduleUpcoming(@NonNull Context context, @NonNull String uid,
                                         @NonNull List<Registration> registrations) {
        long now = System.currentTimeMillis();
        Map<String, Registration> upcoming = new HashMap<>();
        for (Registration registration : registrations) {
            Reminder probe = new Reminder(registration.getEventId(), uid, registration.getEventTitle(), "",
                    registration.getEventStartsAtMillis());
            if (probe.triggerAtMillis() > now) {
                upcoming.put(registration.getEventId(), registration);
            }
        }
        if (upcoming.isEmpty()) {
            return;
        }
        // The venue is on the event, not on the registration, so look the events up to name the place.
        Set<String> ids = new HashSet<>(upcoming.keySet());
        new FirebaseEventRepository().loadEventsByIds(ids, new EventRepository.EventsCallback() {
            @Override
            public void onLoaded(@NonNull List<Event> events) {
                Map<String, Event> byId = new HashMap<>();
                for (Event event : events) {
                    byId.put(event.getId(), event);
                }
                scheduleAll(context, uid, upcoming, byId);
            }

            @Override
            public void onFailure(@NonNull EventRepository.Failure failure) {
                scheduleAll(context, uid, upcoming, new HashMap<>());
            }
        });
    }

    private static void scheduleAll(@NonNull Context context, @NonNull String uid,
                                    @NonNull Map<String, Registration> upcoming, @NonNull Map<String, Event> events) {
        for (Registration registration : upcoming.values()) {
            Event event = events.get(registration.getEventId());
            long startsAt = event != null ? event.getStartsAtMillis() : registration.getEventStartsAtMillis();
            String title = event != null ? event.getTitle() : registration.getEventTitle();
            ReminderScheduler.schedule(context, new Reminder(registration.getEventId(), uid, title,
                    event == null ? "" : placeOf(event), startsAt));
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
