package com.itismob.s03.group7.leef.home;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.chip.ChipGroup;
import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.model.Event;

import java.util.List;

/** Fills the pieces that Home's featured card and compact row have in common. */
public final class EventCardBinder {

    /** The most tags shown under an event. */
    private static final int MAX_TAGS = 3;

    private EventCardBinder() {
    }

    /**
     * Shows the event's photo over the default cover art. With no photo (or if it fails to load) the
     * view is hidden and the default art shows through.
     */
    public static void bindCover(@NonNull ImageView photo, @Nullable String coverUrl) {
        if (coverUrl == null) {
            Glide.with(photo).clear(photo);
            photo.setImageDrawable(null);
            photo.setVisibility(View.GONE);
            return;
        }
        photo.setVisibility(View.VISIBLE);
        Glide.with(photo)
                .load(coverUrl)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade(150))
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, @Nullable Object model,
                                                @NonNull Target<Drawable> target, boolean isFirstResource) {
                        photo.setVisibility(View.GONE);
                        return true;
                    }

                    @Override
                    public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model,
                                                   Target<Drawable> target, @NonNull DataSource dataSource,
                                                   boolean isFirstResource) {
                        return false;
                    }
                })
                .into(photo);
    }

    /** The coloured category label ("SEMINAR"). */
    public static void bindBadge(@NonNull TextView badge, @NonNull Event event) {
        Context context = badge.getContext();
        badge.setText(event.getCategory().getBadgeRes());
        badge.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(context, event.getCategory().getBadgeColorRes())));
    }

    /**
     * The price pill ("Free", "₱500") laid over the photo. While the early-bird offer lasts it shows the
     * early-bird price under a small "Early bird" label, so nobody mistakes it for the regular rate.
     */
    static void bindPrice(@NonNull TextView pill, @NonNull Event event) {
        Context context = pill.getContext();
        long now = System.currentTimeMillis();
        String price = EventFormat.priceText(context, event);
        if (event.isEarlyBirdActive(now)) {
            SpannableStringBuilder text = new SpannableStringBuilder(context.getString(R.string.event_price_early_label));
            text.setSpan(new RelativeSizeSpan(0.78f), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.append('\n').append(price);
            pill.setText(text);
            pill.setGravity(Gravity.CENTER_HORIZONTAL);
        } else {
            pill.setText(price);
            pill.setGravity(Gravity.START);
        }
        boolean free = event.currentPrice(now) <= 0;
        pill.setBackgroundResource(free ? R.drawable.bg_price_pill_free : R.drawable.bg_price_pill_paid);
        pill.setTextColor(ContextCompat.getColor(context, free ? R.color.leef_green_900 : R.color.white));
    }

    /** Title, date line and venue line. */
    static void bindDetails(@NonNull TextView title, @NonNull TextView date, @NonNull TextView venue,
                            @NonNull Event event, boolean dateWithWeekday) {
        title.setText(event.getTitle());
        date.setText(EventFormat.dateText(event, dateWithWeekday));
        boolean hasVenue = !event.getVenue().isEmpty();
        venue.setText(event.getVenue());
        // The venue line (with its pin icon) disappears together when there's no venue.
        ((View) venue.getParent()).setVisibility(hasVenue ? View.VISIBLE : View.GONE);
    }

    /** Up to three small tag pills under the event details. */
    static void bindTags(@NonNull ChipGroup group, @NonNull List<String> tags) {
        group.removeAllViews();
        if (tags.isEmpty()) {
            group.setVisibility(View.GONE);
            return;
        }
        group.setVisibility(View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(group.getContext());
        for (int i = 0; i < Math.min(tags.size(), MAX_TAGS); i++) {
            TextView tag = (TextView) inflater.inflate(R.layout.item_event_tag, group, false);
            tag.setText(tags.get(i));
            group.addView(tag);
        }
    }

    /** The yellow "Boosted" tag on the photo: shown only while the event's boost is still running. */
    public static void bindBoostedTag(@NonNull TextView view, @NonNull Event event) {
        view.setVisibility(event.isBoosted(System.currentTimeMillis()) ? View.VISIBLE : View.GONE);
    }

    /** "4.6 (12)" with a star in front, or hidden when nobody has rated the event yet. */
    public static void bindRating(@NonNull TextView view, @NonNull Event event) {
        if (!event.hasRating()) {
            view.setVisibility(View.GONE);
            return;
        }
        Context context = view.getContext();
        String average = EventFormat.averageText(event);
        view.setVisibility(View.VISIBLE);
        view.setText(context.getString(R.string.event_rating_short, average, event.getRatingCount()));
        view.setContentDescription(context.getResources().getQuantityString(R.plurals.event_rating_a11y,
                event.getRatingCount(), average, event.getRatingCount()));
    }

    /** The heart: red and filled when saved. */
    public static void bindHeart(@NonNull ImageButton heart, @NonNull EventItem item) {
        Context context = heart.getContext();
        heart.setSelected(item.isSaved());
        heart.setContentDescription(context.getString(
                item.isSaved() ? R.string.event_unsave_a11y : R.string.event_save_a11y,
                item.getEvent().getTitle()));
    }
}
