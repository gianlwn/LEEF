package com.itismob.s03.group7.leef.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.itismob.s03.group7.leef.R;

/**
 * Looks up team-supplied artwork by file name (for example {@code "art_leef_logo"}).
 *
 * <p>Artwork is being produced separately from the code. Screens ask for their images by name, so a
 * new PNG dropped into {@code res/drawable-nodpi/} with the expected name appears automatically,
 * and a placeholder is shown until then. res/raw/keep.xml keeps every {@code art_*} drawable in
 * release builds.
 */
public final class ArtAssets {

    private ArtAssets() {
    }

    /** Drawable id for the named artwork, or 0 if that file hasn't been added to the project yet. */
    @SuppressLint("DiscouragedApi") // Name-based lookup is the point: art arrives without code changes.
    @DrawableRes
    public static int find(@NonNull Context context, @Nullable String name) {
        if (TextUtils.isEmpty(name)) {
            return 0;
        }
        String resourcePackage = context.getResources().getResourcePackageName(R.drawable.bg_art_placeholder);
        return context.getResources().getIdentifier(name, "drawable", resourcePackage);
    }
}
