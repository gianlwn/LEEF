package com.itismob.s03.group7.leef.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import com.google.android.material.progressindicator.CircularProgressIndicatorSpec;
import com.google.android.material.progressindicator.IndeterminateDrawable;
import com.itismob.s03.group7.leef.R;

/** A small white spinner to show inside a filled button (via {@code MaterialButton.setIcon}) while it works. */
public final class ButtonSpinner {

    private ButtonSpinner() {
    }

    @NonNull
    public static Drawable create(@NonNull Context context) {
        CircularProgressIndicatorSpec spec = new CircularProgressIndicatorSpec(
                context, null, 0, R.style.Widget_LEEF_CircularProgressIndicator_OnButton);
        return IndeterminateDrawable.createCircularDrawable(context, spec);
    }
}
