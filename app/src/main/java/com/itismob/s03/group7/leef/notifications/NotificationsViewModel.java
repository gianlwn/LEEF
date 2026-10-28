package com.itismob.s03.group7.leef.notifications;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.data.FirebaseNotificationRepository;
import com.itismob.s03.group7.leef.data.NotificationRepository;
import com.itismob.s03.group7.leef.model.AppNotification;

import java.util.ArrayList;
import java.util.List;

/** Loads the notifications and marks them read once they have been shown. */
public class NotificationsViewModel extends ViewModel {

    private final NotificationRepository notifications;
    private final MutableLiveData<NotificationsState> state = new MutableLiveData<>();

    private List<AppNotification> loaded = new ArrayList<>();
    /** The "last opened" time from the first load of this visit, so the dots stay until the screen is closed. */
    private long shownLastRead;
    private boolean loadedOnce;
    private boolean loading;
    @StringRes
    private int errorRes;

    public NotificationsViewModel() {
        this(new FirebaseNotificationRepository());
    }

    NotificationsViewModel(@NonNull NotificationRepository notifications) {
        this.notifications = notifications;
        publish();
        load();
    }

    @NonNull
    public LiveData<NotificationsState> getState() {
        return state;
    }

    public void retry() {
        load();
    }

    /** Picks up new items when the screen comes back (for example after reading a chat). */
    public void reload() {
        if (loadedOnce) {
            load();
        }
    }

    @StringRes
    static int errorMessageFor(@NonNull NotificationRepository.Failure failure) {
        switch (failure) {
            case NETWORK:
                return R.string.notif_error_network;
            case PERMISSION:
                return R.string.notif_error_permission;
            default:
                return R.string.notif_error_generic;
        }
    }

    private void load() {
        if (loading) {
            return;
        }
        loading = true;
        errorRes = 0;
        publish();
        notifications.loadNotifications(new NotificationRepository.LoadCallback() {
            @Override
            public void onLoaded(@NonNull List<AppNotification> result, long lastReadMillis) {
                loading = false;
                boolean first = !loadedOnce;
                loadedOnce = true;
                loaded = result;
                if (first) {
                    shownLastRead = lastReadMillis;
                }
                publish();
                if (first) {
                    markRead();
                }
            }

            @Override
            public void onFailure(@NonNull NotificationRepository.Failure failure) {
                loading = false;
                if (!loadedOnce) {
                    errorRes = errorMessageFor(failure);
                }
                publish();
            }
        });
    }

    /** Remembers that the list was opened, which clears the badge on the bell. A failure here is not worth showing. */
    private void markRead() {
        notifications.markAllRead(new NotificationRepository.DoneCallback() {
            @Override
            public void onDone() {
                NotificationChange.bump();
            }

            @Override
            public void onFailure(@NonNull NotificationRepository.Failure failure) {
                // The dots simply show again next time.
            }
        });
    }

    private void publish() {
        NotificationsState.Status status;
        if (loadedOnce) {
            status = loaded.isEmpty() ? NotificationsState.Status.EMPTY : NotificationsState.Status.CONTENT;
        } else if (!loading && errorRes != 0) {
            status = NotificationsState.Status.ERROR;
        } else {
            status = NotificationsState.Status.LOADING;
        }
        state.setValue(new NotificationsState(status, loaded, shownLastRead, errorRes));
    }
}
