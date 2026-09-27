package com.itismob.s03.group7.leef.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.ui.ArtAssets;

/**
 * A spot for a piece of team-supplied artwork.
 *
 * <p>Give it the artwork's file name ({@code app:leefArtName="art_leef_logo"}). If that drawable
 * exists it is shown; otherwise a dashed placeholder names what belongs there and the exact file
 * name to create, so the art can be dropped in later without touching code.
 */
public class ArtSlotView extends FrameLayout {

    private static final int SCALE_FIT_CENTER = 0;
    private static final int SCALE_CENTER_CROP = 1;

    private final ImageView artView;
    private final LinearLayout placeholder;
    private final TextView titleView;
    private final TextView fileView;

    @Nullable
    private String artName;
    @Nullable
    private CharSequence artTitle;

    public ArtSlotView(@NonNull Context context) {
        this(context, null);
    }

    public ArtSlotView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ArtSlotView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.view_art_placeholder, this, true);
        artView = findViewById(R.id.art_image);
        placeholder = findViewById(R.id.art_placeholder);
        titleView = findViewById(R.id.art_placeholder_title);
        fileView = findViewById(R.id.art_placeholder_file);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ArtSlotView, defStyleAttr, 0);
        boolean compact;
        int scale;
        try {
            artName = a.getString(R.styleable.ArtSlotView_leefArtName);
            artTitle = a.getText(R.styleable.ArtSlotView_leefArtTitle);
            compact = a.getBoolean(R.styleable.ArtSlotView_leefArtCompact, false);
            scale = a.getInt(R.styleable.ArtSlotView_leefArtScale, SCALE_FIT_CENTER);
        } finally {
            a.recycle();
        }

        artView.setScaleType(scale == SCALE_CENTER_CROP
                ? ImageView.ScaleType.CENTER_CROP
                : ImageView.ScaleType.FIT_CENTER);
        if (compact) {
            useCompactPlaceholder();
        }
        // Artwork here is decorative; the surrounding screen describes it for screen readers.
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        refresh();
    }

    /** Points this slot at a different artwork, e.g. when a pager page is rebound. */
    public void setArt(@Nullable String name, @StringRes int titleRes) {
        artName = name;
        artTitle = titleRes == 0 ? null : getContext().getText(titleRes);
        refresh();
    }

    /** True once the named artwork has been added to the project. */
    public boolean hasArt() {
        return ArtAssets.find(getContext(), artName) != 0;
    }

    private void refresh() {
        int artRes = ArtAssets.find(getContext(), artName);
        if (artRes != 0) {
            artView.setImageResource(artRes);
            artView.setVisibility(View.VISIBLE);
            placeholder.setVisibility(View.GONE);
        } else {
            artView.setImageDrawable(null);
            artView.setVisibility(View.GONE);
            placeholder.setVisibility(View.VISIBLE);
            titleView.setText(artTitle != null ? artTitle : getContext().getText(R.string.placeholder_image));
            fileView.setText(artName);
            fileView.setVisibility(artName == null ? View.GONE : View.VISIBLE);
        }
    }

    /** Side-by-side icon and text for short slots such as the logo. */
    private void useCompactPlaceholder() {
        placeholder.setOrientation(LinearLayout.HORIZONTAL);
        int padding = dp(8);
        placeholder.setPadding(padding, padding, padding, padding);

        View icon = findViewById(R.id.art_placeholder_icon);
        LinearLayout.LayoutParams iconParams = (LinearLayout.LayoutParams) icon.getLayoutParams();
        iconParams.width = dp(24);
        iconParams.height = dp(24);
        icon.setLayoutParams(iconParams);

        View text = findViewById(R.id.art_placeholder_text);
        LinearLayout.LayoutParams textParams = (LinearLayout.LayoutParams) text.getLayoutParams();
        textParams.topMargin = 0;
        textParams.setMarginStart(dp(8));
        text.setLayoutParams(textParams);
        ((LinearLayout) text).setGravity(Gravity.START);
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()));
    }
}
