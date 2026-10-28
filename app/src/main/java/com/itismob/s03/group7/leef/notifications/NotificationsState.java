package com.itismob.s03.group7.leef.notifications;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.itismob.s03.group7.leef.model.AppNotification;

import java.util.Collections;
import java.util.List;

/** Everything the Notifications screen draws at one moment. */
public final class NotificationsState {

    public enum Status {
        LOADING,
        ERROR,
        EMPTY,
        CONTENT
    }

    private final Status status;
    private final List<AppNotification> notifications;
    private final long lastReadMillis;
    @StringRes
    private final int errorRes;

    NotificationsState(@NonNull Status status, @NonNull List<AppNotification> notifications, long lastReadMillis,
                       @StringRes int errorRes) {
        this.status = status;
        this.notifications = Collections.unmodifiableList(notifications);
        this.lastReadMillis = lastReadMillis;
        this.errorRes = errorRes;
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    /** Newest first. */
    @NonNull
    public List<AppNotification> getNotifications() {
        return notifications;
    }

    /** When the list was last opened before this visit; rows newer than this get the "new" dot. */
    public long getLastReadMillis() {
        return lastReadMillis;
    }

    @StringRes
    public int getErrorRes() {
        return errorRes;
    }
}
