package com.itismob.s03.group7.leef.data;

import androidx.annotation.NonNull;

import com.itismob.s03.group7.leef.model.AppNotification;

import java.util.List;

/**
 * The notifications list and when the person last opened it; the Firebase code lives in
 * {@link FirebaseNotificationRepository}.
 */
public interface NotificationRepository {

    enum Failure {
        SIGNED_OUT,
        NETWORK,
        /** The database rules refused the request (for example the rules haven't been published). */
        PERMISSION,
        UNKNOWN
    }

    interface LoadCallback {
        /**
         * @param notifications newest first
         * @param lastReadMillis when the person last opened the list, 0 if never
         */
        void onLoaded(@NonNull List<AppNotification> notifications, long lastReadMillis);

        void onFailure(@NonNull Failure failure);
    }

    interface DoneCallback {
        void onDone();

        void onFailure(@NonNull Failure failure);
    }

    /** Works out the signed-in person's notifications from their registrations, events, chats and ratings. */
    void loadNotifications(@NonNull LoadCallback callback);

    /** Remembers that the person opened the list just now (clears the badge on the bell). */
    void markAllRead(@NonNull DoneCallback callback);
}
