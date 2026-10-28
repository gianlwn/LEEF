package com.itismob.s03.group7.leef.organizer;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.databinding.ItemMyEventBinding;
import com.itismob.s03.group7.leef.home.EventCardBinder;
import com.itismob.s03.group7.leef.home.EventFormat;
import com.itismob.s03.group7.leef.model.Event;

/** The rows of the Posted Events list (same card as My Events, with Boosted / Upcoming / Ended). */
final class PostedEventsAdapter extends ListAdapter<Event, PostedEventsAdapter.Holder> {

    interface Listener {
        void onEventClick(@NonNull Event event);
    }

    private final Listener listener;

    PostedEventsAdapter(@NonNull Listener listener) {
        super(new DiffUtil.ItemCallback<Event>() {
            @Override
            public boolean areItemsTheSame(@NonNull Event oldItem, @NonNull Event newItem) {
                return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Event oldItem, @NonNull Event newItem) {
                return oldItem.equals(newItem);
            }
        });
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemMyEventBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position));
    }

    final class Holder extends RecyclerView.ViewHolder {
        private final ItemMyEventBinding binding;

        Holder(@NonNull ItemMyEventBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull Event event) {
            Context context = binding.getRoot().getContext();
            EventCardBinder.bindCover(binding.coverPhoto, event.getCoverUrl());
            binding.title.setText(event.getTitle());
            binding.dateText.setText(EventFormat.dateTimeLine(event, event.getStartsAtMillis()));

            String place = event.getVenue();
            if (place.isEmpty()) {
                place = !event.getAddress().isEmpty() ? event.getAddress() : event.getCity();
            }
            binding.venueText.setText(place);
            binding.venueRow.setVisibility(place.isEmpty() ? View.GONE : View.VISIBLE);

            long now = System.currentTimeMillis();
            boolean ended = event.hasEnded(now);
            int label;
            int icon;
            int background;
            int textColor;
            if (event.isCancelled()) {
                label = R.string.posted_status_cancelled;
                icon = R.drawable.ic_close;
                background = R.drawable.bg_status_saved;
                textColor = R.color.leef_error;
            } else if (ended) {
                label = R.string.posted_status_ended;
                icon = R.drawable.ic_status_saved;
                background = R.drawable.bg_status_saved;
                textColor = R.color.leef_gray_700;
            } else if (event.isBoosted(now)) {
                label = R.string.posted_status_boosted;
                icon = R.drawable.ic_bolt;
                background = R.drawable.bg_status_group;
                textColor = R.color.leef_cat_competition_badge;
            } else {
                label = R.string.posted_status_upcoming;
                icon = R.drawable.ic_status_registered;
                background = R.drawable.bg_status_registered;
                textColor = R.color.leef_green_900;
            }
            int color = ContextCompat.getColor(context, textColor);
            binding.status.setText(label);
            binding.status.setBackgroundResource(background);
            binding.status.setTextColor(color);
            binding.status.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0);
            TextViewCompat.setCompoundDrawableTintList(binding.status, ColorStateList.valueOf(color));

            binding.getRoot().setOnClickListener(v -> listener.onEventClick(event));
        }
    }
}
