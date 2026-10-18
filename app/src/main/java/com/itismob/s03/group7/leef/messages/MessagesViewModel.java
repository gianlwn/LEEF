package com.itismob.s03.group7.leef.messages;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.data.EventRepository;
import com.itismob.s03.group7.leef.data.FirebaseEventRepository;
import com.itismob.s03.group7.leef.data.FirebaseMessageRepository;
import com.itismob.s03.group7.leef.data.FirebaseRegistrationRepository;
import com.itismob.s03.group7.leef.data.MessageRepository;
import com.itismob.s03.group7.leef.data.RegistrationRepository;
import com.itismob.s03.group7.leef.model.Conversation;
import com.itismob.s03.group7.leef.model.Event;
import com.itismob.s03.group7.leef.model.Registration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Screen 10 - Messages. Lists the chat of every event the person registered for and their direct chats,
 * newest message first, and filters them as they type in the search box.
 */
public class MessagesViewModel extends ViewModel {

    private final MessageRepository messages;
    private final RegistrationRepository registrations;
    private final EventRepository events;

    private final MutableLiveData<MessagesState> state = new MutableLiveData<>();
    private final MutableLiveData<Integer> message = new MutableLiveData<>();

    private List<Conversation> all = new ArrayList<>();
    private String query = "";
    private boolean loadedOnce;
    private boolean loading;
    private boolean refreshing;
    @Nullable
    private MessageRepository.Failure failure;

    public MessagesViewModel() {
        this(new FirebaseMessageRepository(), new FirebaseRegistrationRepository(), new FirebaseEventRepository());
    }

    MessagesViewModel(@NonNull MessageRepository messages, @NonNull RegistrationRepository registrations,
                      @NonNull EventRepository events) {
        this.messages = messages;
        this.registrations = registrations;
        this.events = events;
        publish();
        load(false);
    }

    @NonNull
    public LiveData<MessagesState> getState() {
        return state;
    }

    /** A toast message (string resource id). Call {@link #messageHandled()} after showing it. */
    @NonNull
    public LiveData<Integer> getMessage() {
        return message;
    }

    public void messageHandled() {
        message.setValue(null);
    }

    /** Filters the list by what the person typed. */
    public void setQuery(@NonNull String text) {
        String trimmed = text.trim();
        if (!trimmed.equals(query)) {
            query = trimmed;
            publish();
        }
    }

    /** Pull to refresh. */
    public void refresh() {
        load(true);
    }

    /** "Try again" after a failed first load. */
    public void retry() {
        load(false);
    }

    /** Quietly picks up new messages when the tab comes back into view. */
    public void reload() {
        if (loadedOnce) {
            load(false);
        }
    }

    private void load(boolean userInitiated) {
        if (loading) {
            if (userInitiated) {
                refreshing = true;
                publish();
            }
            return;
        }
        loading = true;
        refreshing = userInitiated;
        failure = null;
        publish();

        registrations.loadRegistrations(new RegistrationRepository.ListCallback() {
            @Override
            public void onLoaded(@NonNull List<Registration> registered) {
                loadConversations(registered);
            }

            @Override
            public void onFailure(@NonNull RegistrationRepository.Failure reason) {
                fail(reason == RegistrationRepository.Failure.NETWORK ? MessageRepository.Failure.NETWORK
                        : reason == RegistrationRepository.Failure.PERMISSION ? MessageRepository.Failure.PERMISSION
                        : MessageRepository.Failure.UNKNOWN);
            }
        });
    }

    private void loadConversations(@NonNull List<Registration> registered) {
        // Organizers also get the chat of every event they posted. A failure here only hides those chats.
        events.loadPostedEvents(new EventRepository.EventsCallback() {
            @Override
            public void onLoaded(@NonNull List<Event> posted) {
                loadConversations(registered, posted);
            }

            @Override
            public void onFailure(@NonNull EventRepository.Failure reason) {
                loadConversations(registered, new ArrayList<>());
            }
        });
    }

    private void loadConversations(@NonNull List<Registration> registered, @NonNull List<Event> posted) {
        Map<String, String> titles = new HashMap<>();
        for (Registration registration : registered) {
            titles.put(registration.getEventId(), registration.getEventTitle());
        }
        final Map<String, String> covers = new HashMap<>();
        for (Event event : posted) {
            titles.put(event.getId(), event.getTitle());
            covers.put(event.getId(), event.getCoverUrl());
        }
        // The covers of events the person registered for (not posted by them) still have to be loaded.
        final java.util.Set<String> missingCovers = new java.util.HashSet<>(titles.keySet());
        missingCovers.removeAll(covers.keySet());

        final int[] pending = {2};
        final List<Conversation>[] conversations = newHolder();
        final MessageRepository.Failure[] chatFailure = {null};

        Runnable next = () -> {
            if (--pending[0] > 0) {
                return;
            }
            if (conversations[0] == null) {
                fail(chatFailure[0] == null ? MessageRepository.Failure.UNKNOWN : chatFailure[0]);
                return;
            }
            List<Conversation> result = new ArrayList<>();
            for (Conversation conversation : conversations[0]) {
                result.add(conversation.getRef().isEvent()
                        ? conversation.withCover(covers.get(conversation.getRef().getTargetId())) : conversation);
            }
            sort(result);
            all = result;
            loading = false;
            refreshing = false;
            loadedOnce = true;
            failure = null;
            publish();
        };

        messages.loadConversations(titles, new MessageRepository.ConversationsCallback() {
            @Override
            public void onLoaded(@NonNull List<Conversation> result) {
                conversations[0] = result;
                next.run();
            }

            @Override
            public void onFailure(@NonNull MessageRepository.Failure reason) {
                chatFailure[0] = reason;
                next.run();
            }
        });
        if (missingCovers.isEmpty()) {
            next.run();
        } else {
            events.loadEventsByIds(missingCovers, new EventRepository.EventsCallback() {
                @Override
                public void onLoaded(@NonNull List<Event> found) {
                    for (Event event : found) {
                        covers.put(event.getId(), event.getCoverUrl());
                    }
                    next.run();
                }

                @Override
                public void onFailure(@NonNull EventRepository.Failure reason) {
                    // The photos are optional: the rows still show with the default art.
                    next.run();
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Conversation>[] newHolder() {
        return new List[]{null};
    }

    /** Newest message first; chats nobody has written in yet come last, by name. */
    private static void sort(@NonNull List<Conversation> list) {
        Collections.sort(list, (a, b) -> {
            if (a.getLastMessageAtMillis() != b.getLastMessageAtMillis()) {
                return Long.compare(b.getLastMessageAtMillis(), a.getLastMessageAtMillis());
            }
            return a.getTitle().compareToIgnoreCase(b.getTitle());
        });
    }

    private void fail(@NonNull MessageRepository.Failure reason) {
        loading = false;
        boolean wasRefreshing = refreshing;
        refreshing = false;
        if (loadedOnce) {
            // Keep what is on screen; only say so if the person asked for the refresh.
            if (wasRefreshing) {
                message.setValue(reason == MessageRepository.Failure.NETWORK
                        ? R.string.messages_refresh_offline : R.string.messages_refresh_failed);
            }
        } else {
            failure = reason;
        }
        publish();
    }

    private void publish() {
        MessagesState.Status status;
        if (loadedOnce) {
            status = MessagesState.Status.CONTENT;
        } else if (failure != null) {
            status = MessagesState.Status.ERROR;
        } else {
            status = MessagesState.Status.LOADING;
        }
        state.setValue(new MessagesState(status, refreshing, !all.isEmpty(), query, filtered(), failure));
    }

    @NonNull
    private List<Conversation> filtered() {
        if (query.isEmpty()) {
            return all;
        }
        String needle = query.toLowerCase(Locale.getDefault());
        List<Conversation> matches = new ArrayList<>();
        for (Conversation conversation : all) {
            if (contains(conversation.getTitle(), needle) || contains(conversation.getLastMessage(), needle)
                    || contains(conversation.getLastSenderName(), needle)) {
                matches.add(conversation);
            }
        }
        return matches;
    }

    private static boolean contains(@Nullable String text, @NonNull String needle) {
        return text != null && text.toLowerCase(Locale.getDefault()).contains(needle);
    }
}
