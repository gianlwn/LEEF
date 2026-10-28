package com.itismob.s03.group7.leef.notifications;

import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.itismob.s03.group7.leef.data.FirebaseNotificationRepository;
import com.itismob.s03.group7.leef.data.NotificationRepository;
import com.itismob.s03.group7.leef.model.AppNotification;

import java.util.List;

/** Counts the new notifications for the badge on the bell on Home. */
public class NotificationBadgeViewModel extends ViewModel {

    /** Don't recount more often than this when nothing inside the app changed. */
    private static final long MIN_GAP_MILLIS = 60_000L;

    private final NotificationRepository notifications;
    private final MutableLiveData<Integer> count = new MutableLiveData<>(0);

    private boolean loading;
    private long lastRefreshAt = -MIN_GAP_MILLIS;
    private int lastVersion = -1;

    public NotificationBadgeViewModel() {
        this(new FirebaseNotificationRepository());
    }

    NotificationBadgeViewModel(@NonNull NotificationRepository notifications) {
        this.notifications = notifications;
    }

    /** The number of notifications the person hasn't seen yet. */
    @NonNull
    public LiveData<Integer> getCount() {
        return count;
    }

    /** Recounts when the app changed something or a minute passed since the last count. */
    public void refreshIfNeeded() {
        long now = SystemClock.elapsedRealtime();
        boolean changed = lastVersion != NotificationChange.version();
        if (loading || (!changed && now - lastRefreshAt < MIN_GAP_MILLIS)) {
            return;
        }
        loading = true;
        int version = NotificationChange.version();
        notifications.loadNotifications(new NotificationRepository.LoadCallback() {
            @Override
            public void onLoaded(@NonNull List<AppNotification> result, long lastReadMillis) {
                loading = false;
                lastRefreshAt = SystemClock.elapsedRealtime();
                lastVersion = version;
                int unread = 0;
                for (AppNotification item : result) {
                    if (item.isUnread(lastReadMillis)) {
                        unread++;
                    }
                }
                count.setValue(unread);
            }

            @Override
            public void onFailure(@NonNull NotificationRepository.Failure failure) {
                // Keep the old number; the bell still opens the list, which shows the problem.
                loading = false;
            }
        });
    }

    /** Forget the count (for example after signing out) so the next person doesn't see it. */
    public void reset() {
        count.setValue(0);
        lastVersion = -1;
        lastRefreshAt = -MIN_GAP_MILLIS;
    }
}
