package com.itismob.s03.group7.leef.ui;

import android.graphics.Color;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.NonNull;

/**
 * Draws every LEEF screen behind the system bars with dark bar icons, since all screens use
 * light backgrounds. Call before {@code setContentView}.
 */
public final class LeefEdgeToEdge {

    /** Used only on API 24-25, where light navigation bar icons are not supported. */
    private static final int LEGACY_NAV_SCRIM = Color.argb(0x80, 0x1B, 0x1B, 0x1B);

    private LeefEdgeToEdge() {
    }

    public static void enable(@NonNull ComponentActivity activity) {
        EdgeToEdge.enable(
                activity,
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                SystemBarStyle.light(Color.TRANSPARENT, LEGACY_NAV_SCRIM));
    }
}
