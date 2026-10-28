package com.itismob.s03.group7.leef.feedback;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.data.EventRepository;
import com.itismob.s03.group7.leef.data.FeedbackRepository;
import com.itismob.s03.group7.leef.data.FirebaseEventRepository;
import com.itismob.s03.group7.leef.data.FirebaseFeedbackRepository;
import com.itismob.s03.group7.leef.data.FirebaseRegistrationRepository;
import com.itismob.s03.group7.leef.data.RegistrationRepository;
import com.itismob.s03.group7.leef.model.Event;
import com.itismob.s03.group7.leef.model.Feedback;
import com.itismob.s03.group7.leef.model.Registration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * After an event is over the app asks the person to rate it (MCO1, Screen 12). There is no server to
 * push a message on the free plan, so the check runs when the app opens: it finds the most recent event
 * the person registered for that ended in the last 30 days, that they haven't rated, and that this phone
 * hasn't asked about yet, and offers a short dialog. Each event is asked about only once; the person can
 * always rate later from the event's page.
 */
public final class FeedbackPrompter {

    private static final long WINDOW_MILLIS = TimeUnit.DAYS.toMillis(30);
    private static final int MAX_CANDIDATES = 5;

    private final Activity activity;
    private final FeedbackPromptStore store;
    private final RegistrationRepository registrations = new FirebaseRegistrationRepository();
    private final EventRepository events = new FirebaseEventRepository();
    private final FeedbackRepository feedback = new FirebaseFeedbackRepository();
    private String uid = "";

    private FeedbackPrompter(@NonNull Activity activity) {
        this.activity = activity;
        this.store = new FeedbackPromptStore(activity);
    }

    /** Looks for an event to ask about and, if there is one, shows the dialog. Safe to call on every launch. */
    public static void run(@NonNull Activity activity) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        FeedbackPrompter prompter = new FeedbackPrompter(activity);
        prompter.uid = user.getUid();
        prompter.start();
    }

    private void start() {
        registrations.loadRegistrations(new RegistrationRepository.ListCallback() {
            @Override
            public void onLoaded(@NonNull List<Registration> loaded) {
                long now = System.currentTimeMillis();
                List<Registration> recent = new ArrayList<>();
                for (Registration registration : loaded) {
                    long start = registration.getEventStartsAtMillis();
                    if (start < now && now - start < WINDOW_MILLIS
                            && !store.wasPrompted(uid, registration.getEventId())) {
                        recent.add(registration);
                    }
                }
                if (recent.isEmpty()) {
                    return;
                }
                // Newest first, and not too many lookups.
                Collections.sort(recent, (a, b) ->
                        Long.compare(b.getEventStartsAtMillis(), a.getEventStartsAtMillis()));
                if (recent.size() > MAX_CANDIDATES) {
                    recent = new ArrayList<>(recent.subList(0, MAX_CANDIDATES));
                }
                loadEvents(recent);
            }

            @Override
            public void onFailure(@NonNull RegistrationRepository.Failure failure) {
                // Quietly skip: this is a nicety, not something to interrupt the person about.
            }
        });
    }

    private void loadEvents(@NonNull List<Registration> candidates) {
        Set<String> ids = new HashSet<>();
        for (Registration registration : candidates) {
            ids.add(registration.getEventId());
        }
        events.loadEventsByIds(ids, new EventRepository.EventsCallback() {
            @Override
            public void onLoaded(@NonNull List<Event> loaded) {
                long now = System.currentTimeMillis();
                List<Event> ended = new ArrayList<>();
                for (Registration registration : candidates) {
                    for (Event event : loaded) {
                        if (event.getId().equals(registration.getEventId()) && event.hasEnded(now)
                                && !event.isCancelled()) {
                            ended.add(event);
                        }
                    }
                }
                checkNext(ended, 0);
            }

            @Override
            public void onFailure(@NonNull EventRepository.Failure failure) {
            }
        });
    }

    /** Walks the list until it finds an event with no feedback yet. */
    private void checkNext(@NonNull List<Event> ended, int index) {
        if (index >= ended.size()) {
            return;
        }
        Event event = ended.get(index);
        feedback.loadMine(event.getId(), new FeedbackRepository.MineCallback() {
            @Override
            public void onLoaded(Feedback mine) {
                if (mine != null) {
                    store.markPrompted(uid, event.getId());
                    checkNext(ended, index + 1);
                } else {
                    show(event);
                }
            }

            @Override
            public void onFailure(@NonNull FeedbackRepository.Failure failure) {
            }
        });
    }

    private void show(@NonNull Event event) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        store.markPrompted(uid, event.getId());
        new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.feedback_prompt_title, event.getTitle()))
                .setMessage(R.string.feedback_prompt_body)
                .setPositiveButton(R.string.feedback_prompt_rate, (dialog, which) ->
                        activity.startActivity(FeedbackActivity.createIntent(activity, event.getId())))
                .setNegativeButton(R.string.feedback_prompt_later, null)
                .show();
    }
}
