package com.itismob.s03.group7.leef.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.itismob.s03.group7.leef.R;

/**
 * Five tappable stars. {@link #setRating(int)} fills that many (0 = none); tapping a star reports its
 * number to the listener. Set {@link #setEditable(boolean)} to false to use it as a read-only display.
 */
public class StarRatingBar extends LinearLayout {

    /** Told when the person taps a star. */
    public interface OnRatingChangeListener {
        void onRatingChanged(int rating);
    }

    private static final int STARS = 5;

    private final ImageView[] stars = new ImageView[STARS];
    private int rating;
    private boolean editable = true;
    @Nullable
    private OnRatingChangeListener listener;

    public StarRatingBar(@NonNull Context context) {
        this(context, null);
    }

    public StarRatingBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);
        int size = dp(context, 44);
        for (int i = 0; i < STARS; i++) {
            ImageView star = new ImageView(context);
            LayoutParams params = new LayoutParams(size, size);
            star.setLayoutParams(params);
            star.setPadding(dp(context, 4), dp(context, 4), dp(context, 4), dp(context, 4));
            star.setScaleType(ImageView.ScaleType.FIT_CENTER);
            final int value = i + 1;
            star.setOnClickListener(v -> {
                if (editable) {
                    setRating(value);
                    if (listener != null) {
                        listener.onRatingChanged(value);
                    }
                }
            });
            stars[i] = star;
            addView(star);
        }
        refresh();
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int value) {
        rating = Math.max(0, Math.min(STARS, value));
        refresh();
    }

    public void setEditable(boolean value) {
        editable = value;
        refresh();
    }

    /** Smaller stars for read-only use in lists. */
    public void setStarSizeDp(int dp) {
        int size = dp(getContext(), dp);
        int padding = Math.max(1, size / 11);
        for (ImageView star : stars) {
            LayoutParams params = (LayoutParams) star.getLayoutParams();
            params.width = size;
            params.height = size;
            star.setLayoutParams(params);
            star.setPadding(padding, padding, padding, padding);
        }
    }

    public void setOnRatingChangeListener(@Nullable OnRatingChangeListener value) {
        listener = value;
    }

    private void refresh() {
        for (int i = 0; i < STARS; i++) {
            ImageView star = stars[i];
            star.setImageResource(i < rating ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
            star.setClickable(editable);
            star.setFocusable(editable);
            star.setImportantForAccessibility(editable ? View.IMPORTANT_FOR_ACCESSIBILITY_YES
                    : View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            star.setContentDescription(getContext().getString(R.string.feedback_star_a11y, i + 1));
            star.setSelected(i < rating);
        }
        if (!editable) {
            setContentDescription(getContext().getString(R.string.feedback_rating_a11y, rating));
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        }
    }

    private static int dp(@NonNull Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
