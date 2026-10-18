package com.itismob.s03.group7.leef.messages;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.data.FirebaseMessageRepository;
import com.itismob.s03.group7.leef.data.FirebaseProfileRepository;
import com.itismob.s03.group7.leef.data.MessageRepository;
import com.itismob.s03.group7.leef.data.ProfileRepository;
import com.itismob.s03.group7.leef.model.ChatMessage;
import com.itismob.s03.group7.leef.model.ChatRef;
import com.itismob.s03.group7.leef.model.UserProfile;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen 10 - one chat. Listens to the chat's messages live, sends new ones under the person's full
 * name, and marks the chat read.
 */
public class ChatViewModel extends ViewModel {

    private final MessageRepository messages;
    private final ProfileRepository profiles;
    private final ChatRef ref;
    /** The event's title (event chat) or the other person's name (direct chat). */
    private final String title;

    private final MutableLiveData<ChatState> state = new MutableLiveData<>();
    private final MutableLiveData<Integer> message = new MutableLiveData<>();

    private List<ChatMessage> latest = new ArrayList<>();
    private boolean loaded;
    @Nullable
    private MessageRepository.Failure failure;
    @Nullable
    private MessageRepository.Subscription subscription;
    @Nullable
    private String myName;

    ChatViewModel(@NonNull MessageRepository messages, @NonNull ProfileRepository profiles,
                  @NonNull ChatRef ref, @NonNull String title) {
        this.messages = messages;
        this.profiles = profiles;
        this.ref = ref;
        this.title = title;
        publish();
        subscribe();
        loadName(null);
    }

    @NonNull
    public LiveData<ChatState> getState() {
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

    /** "Try again" after the chat couldn't be opened. */
    public void retry() {
        loaded = false;
        failure = null;
        publish();
        subscribe();
    }

    /** Remembers that the person has seen everything up to now. */
    public void markRead() {
        messages.markRead(ref);
    }

    /** Sends a message under the person's full name. */
    public void send(@NonNull String text) {
        String body = text.trim();
        if (body.isEmpty()) {
            return;
        }
        if (myName == null) {
            // The profile hadn't loaded yet (or failed): try again, then send.
            loadName(() -> sendNow(body));
            return;
        }
        sendNow(body);
    }

    private void sendNow(@NonNull String body) {
        if (myName == null) {
            return;
        }
        messages.sendMessage(ref, title, myName, title, body, new MessageRepository.DoneCallback() {
            @Override
            public void onDone() {
                // The message is already on screen.
            }

            @Override
            public void onFailure(@NonNull MessageRepository.Failure reason) {
                message.setValue(reason == MessageRepository.Failure.NETWORK
                        ? R.string.chat_send_offline : R.string.chat_send_failed);
            }
        });
    }

    private void loadName(@Nullable Runnable afterwards) {
        profiles.loadProfile(new ProfileRepository.LoadCallback() {
            @Override
            public void onLoaded(@NonNull UserProfile profile) {
                myName = profile.getFullName();
                if (afterwards != null) {
                    afterwards.run();
                }
            }

            @Override
            public void onMissing() {
                failedName(afterwards);
            }

            @Override
            public void onFailure(@NonNull ProfileRepository.Failure reason) {
                failedName(afterwards);
            }
        });
    }

    private void failedName(@Nullable Runnable afterwards) {
        if (afterwards != null) {
            message.setValue(R.string.chat_send_no_profile);
        }
    }

    private void subscribe() {
        if (subscription != null) {
            subscription.cancel();
        }
        subscription = messages.listenMessages(ref, new MessageRepository.MessagesListener() {
            @Override
            public void onMessages(@NonNull List<ChatMessage> result) {
                latest = result;
                loaded = true;
                failure = null;
                publish();
            }

            @Override
            public void onFailure(@NonNull MessageRepository.Failure reason) {
                loaded = false;
                failure = reason;
                publish();
            }
        });
    }

    private void publish() {
        ChatState.Status status = failure != null ? ChatState.Status.ERROR
                : loaded ? ChatState.Status.CONTENT : ChatState.Status.LOADING;
        state.setValue(new ChatState(status, latest, failure));
    }

    @Override
    protected void onCleared() {
        if (subscription != null) {
            subscription.cancel();
        }
    }

    /** Creates the view model for one chat. */
    static final class Factory implements ViewModelProvider.Factory {
        private final ChatRef ref;
        private final String title;

        Factory(@NonNull ChatRef ref, @NonNull String title) {
            this.ref = ref;
            this.title = title;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new ChatViewModel(new FirebaseMessageRepository(), new FirebaseProfileRepository(), ref, title);
        }
    }
}
