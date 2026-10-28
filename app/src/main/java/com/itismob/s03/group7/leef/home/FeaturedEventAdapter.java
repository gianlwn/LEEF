package com.itismob.s03.group7.leef.home;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.itismob.s03.group7.leef.databinding.ItemEventFeaturedBinding;
import com.itismob.s03.group7.leef.model.Event;

/** The big cards in the Featured Events carousel. */
final class FeaturedEventAdapter extends ListAdapter<EventItem, FeaturedEventAdapter.Holder> {

    private final EventActions actions;
    /** Width of each card when there is more than one, so the next card peeks in. */
    private int cardWidthPx = ViewGroup.LayoutParams.MATCH_PARENT;

    FeaturedEventAdapter(@NonNull EventActions actions) {
        super(new EventItemDiff());
        this.actions = actions;
    }

    void setCardWidthPx(int widthPx) {
        if (cardWidthPx != widthPx) {
            cardWidthPx = widthPx;
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemEventFeaturedBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position), getItemCount() == 1 ? ViewGroup.LayoutParams.MATCH_PARENT : cardWidthPx);
    }

    final class Holder extends RecyclerView.ViewHolder {
        private final ItemEventFeaturedBinding binding;

        Holder(@NonNull ItemEventFeaturedBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull EventItem item, int widthPx) {
            Event event = item.getEvent();
            ViewGroup.LayoutParams params = binding.getRoot().getLayoutParams();
            if (params.width != widthPx) {
                params.width = widthPx;
                binding.getRoot().setLayoutParams(params);
            }
            EventCardBinder.bindCover(binding.coverPhoto, event.getCoverUrl());
            EventCardBinder.bindBadge(binding.badge, event);
            EventCardBinder.bindPrice(binding.price, event);
            EventCardBinder.bindBoostedTag(binding.boostedTag, event);
            EventCardBinder.bindDetails(binding.title, binding.dateText, binding.venueText, event, true);
            EventCardBinder.bindRating(binding.ratingText, event);
            EventCardBinder.bindTags(binding.tags, event.getTags());
            EventCardBinder.bindHeart(binding.heart, item);
            binding.getRoot().setOnClickListener(v -> actions.onEventClick(event));
            binding.heart.setOnClickListener(v -> actions.onSaveClick(event));
        }
    }
}
