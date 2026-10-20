package com.itismob.s03.group7.leef.ui;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.databinding.ViewNoInternetBinding;

import java.util.HashMap;
import java.util.Map;

/**
 * While the phone has no internet, every screen of the app is covered by a "No internet connection"
 * page with a Try again button. The page goes away by itself when the connection returns. (The class
 * keeps its first name, from when it was only a bar across the top.)
 */
public final class OfflineBanner implements Application.ActivityLifecycleCallbacks {

    private final Map<Activity, Attached> attached = new HashMap<>();

    /** Registers the cover for every screen of the app. */
    public static void install(@NonNull Application application) {
        application.registerActivityLifecycleCallbacks(new OfflineBanner());
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null || attached.containsKey(activity)) {
            return;
        }
        Attached cover = new Attached(activity, content);
        attached.put(activity, cover);
        cover.start();
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        Attached cover = attached.remove(activity);
        if (cover != null) {
            cover.stop();
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    /** The cover on one screen. */
    private static final class Attached implements Observer<Boolean> {
        private final Activity activity;
        private final ViewGroup content;
        private final View cover;

        Attached(@NonNull Activity activity, @NonNull ViewGroup content) {
            this.activity = activity;
            this.content = content;
            ViewNoInternetBinding binding = ViewNoInternetBinding.inflate(LayoutInflater.from(activity), content, false);
            cover = binding.getRoot();
            cover.setVisibility(View.GONE);
            binding.noInternetRetry.setOnClickListener(v -> {
                ConnectionMonitor.refresh();
                if (!Boolean.TRUE.equals(ConnectionMonitor.online().getValue())) {
                    Toast.makeText(activity, R.string.offline_still, Toast.LENGTH_SHORT).show();
                }
            });
            content.addView(cover);
        }

        void start() {
            ConnectionMonitor.online().observeForever(this);
        }

        void stop() {
            ConnectionMonitor.online().removeObserver(this);
        }

        @Override
        public void onChanged(Boolean online) {
            boolean offline = Boolean.FALSE.equals(online);
            cover.setVisibility(offline ? View.VISIBLE : View.GONE);
            // Keep screen readers on the cover, not on the screen hidden behind it.
            for (int i = 0; i < content.getChildCount(); i++) {
                View child = content.getChildAt(i);
                if (child != cover) {
                    child.setImportantForAccessibility(offline
                            ? View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                            : View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
                }
            }
        }
    }
}
