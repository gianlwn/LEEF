package com.itismob.s03.group7.leef.ui;

import android.app.Activity;
import android.app.Application;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;

import com.itismob.s03.group7.leef.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Shows a slim "You're offline" bar across the top of every screen while the phone has no internet.
 * The screen underneath moves down by the height of the bar so nothing (like a back button) is covered.
 * Saved events can still be browsed; anything that needs the server tells the person itself.
 */
public final class OfflineBanner implements Application.ActivityLifecycleCallbacks {

    private final Map<Activity, Attached> attached = new HashMap<>();

    /** Registers the bar for every screen of the app. */
    public static void install(@NonNull Application application) {
        application.registerActivityLifecycleCallbacks(new OfflineBanner());
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null || attached.containsKey(activity)) {
            return;
        }
        Attached banner = new Attached(activity, content);
        attached.put(activity, banner);
        banner.start();
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        Attached banner = attached.remove(activity);
        if (banner != null) {
            banner.stop();
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

    /** The bar on one screen. */
    private static final class Attached implements Observer<Boolean> {
        private final ViewGroup content;
        private final TextView bar;
        private final int extraHeight;
        private final int barHeight;

        Attached(@NonNull Activity activity, @NonNull ViewGroup content) {
            this.content = content;
            float density = activity.getResources().getDisplayMetrics().density;
            extraHeight = Math.round(32 * density);
            barHeight = statusBarHeight(activity.getResources()) + extraHeight;

            bar = new TextView(activity);
            bar.setText(R.string.offline_banner);
            bar.setTextColor(Color.WHITE);
            bar.setTextSize(13f);
            bar.setGravity(Gravity.CENTER);
            bar.setBackgroundColor(ContextCompat.getColor(activity, R.color.leef_ink));
            bar.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_state_cloud_off, 0, 0, 0);
            bar.setCompoundDrawableTintList(ColorStateList.valueOf(Color.WHITE));
            bar.setCompoundDrawablePadding(Math.round(8 * density));
            bar.setPadding(Math.round(16 * density), statusBarHeight(activity.getResources()),
                    Math.round(16 * density), 0);
            bar.setElevation(24 * density);
            bar.setVisibility(View.GONE);
            bar.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);

            // The screen is pushed down by extraHeight (below), so the bar starts above that padding.
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, barHeight, Gravity.TOP);
            params.topMargin = -extraHeight;
            content.setClipToPadding(false);
            content.setClipChildren(false);
            content.addView(bar, params);
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
            bar.setVisibility(offline ? View.VISIBLE : View.GONE);
            content.setPadding(0, offline ? extraHeight : 0, 0, 0);
        }

        private static int statusBarHeight(@NonNull Resources resources) {
            int id = resources.getIdentifier("status_bar_height", "dimen", "android");
            return id > 0 ? resources.getDimensionPixelSize(id) : 0;
        }
    }
}
