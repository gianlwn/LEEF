package com.itismob.s03.group7.leef.ui;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.itismob.s03.group7.leef.R;

/**
 * Tells the person that a button leads to a screen that hasn't been built yet. Used while the app is
 * built one screen at a time; each call is removed when its screen arrives.
 */
public final class ComingSoon {

    private ComingSoon() {
    }

    public static void show(@NonNull Context context, @StringRes int featureName) {
        Toast.makeText(context,
                context.getString(R.string.coming_soon_format, context.getString(featureName)),
                Toast.LENGTH_SHORT).show();
    }
}
