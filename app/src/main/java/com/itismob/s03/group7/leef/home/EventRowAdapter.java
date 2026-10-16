package com.itismob.s03.group7.leef.home;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.itismob.s03.group7.leef.databinding.ItemEventRowBinding;
import com.itismob.s03.group7.leef.model.Event;

/** The compact event cards in the Upcoming Events list. */
public final class EventRowAdapter extends ListAdapter<EventItem, EventRowAdapter.Holder> {

    private final EventActions actions;

    public EventRowAdapter(@NonNull EventActions actions) {
        super(new EventItemDiff());
        this.actions = actions;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemEventRowBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position));
    }

    final class Holder extends RecyclerView.ViewHolder {
        private final ItemEventRowBinding binding;

        Holder(@NonNull ItemEventRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull EventItem item) {
            Event event = item.getEvent();
            EventCardBinder.bindCover(binding.coverPhoto, event.getCoverUrl());
            EventCardBinder.bindPrice(binding.price, event);
            EventCardBinder.bindDetails(binding.title, binding.dateText, binding.venueText, event, false);
            EventCardBinder.bindTags(binding.tags, event.getTags());
            EventCardBinder.bindHeart(binding.heart, item);
            binding.getRoot().setOnClickListener(v -> actions.onEventClick(event));
            binding.heart.setOnClickListener(v -> actions.onSaveClick(event));
        }
    }
}
