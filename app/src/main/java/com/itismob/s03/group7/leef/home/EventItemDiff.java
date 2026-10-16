package com.itismob.s03.group7.leef.home;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

/** Lets the event lists animate and redraw only the cards that actually changed. */
final class EventItemDiff extends DiffUtil.ItemCallback<EventItem> {

    @Override
    public boolean areItemsTheSame(@NonNull EventItem oldItem, @NonNull EventItem newItem) {
        return oldItem.getEvent().getId().equals(newItem.getEvent().getId());
    }

    @Override
    public boolean areContentsTheSame(@NonNull EventItem oldItem, @NonNull EventItem newItem) {
        return oldItem.equals(newItem);
    }
}
