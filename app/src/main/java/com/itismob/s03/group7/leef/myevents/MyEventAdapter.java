package com.itismob.s03.group7.leef.myevents;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
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

/** The rows of the Upcoming and Past lists. */
final class MyEventAdapter extends ListAdapter<MyEventItem, MyEventAdapter.Holder> {

    interface Listener {
        void onEventClick(@NonNull MyEventItem item);
    }

    private final Listener listener;

    MyEventAdapter(@NonNull Listener listener) {
        super(new DiffUtil.ItemCallback<MyEventItem>() {
            @Override
            public boolean areItemsTheSame(@NonNull MyEventItem oldItem, @NonNull MyEventItem newItem) {
                return oldItem.getEventId().equals(newItem.getEventId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull MyEventItem oldItem, @NonNull MyEventItem newItem) {
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

        void bind(@NonNull MyEventItem item) {
            Context context = binding.getRoot().getContext();
            Event event = item.getEvent();

            EventCardBinder.bindCover(binding.coverPhoto, event == null ? null : event.getCoverUrl());
            binding.title.setText(item.getTitle());
            binding.dateText.setText(EventFormat.dateTimeLine(event, item.getStartsAtMillis()));

            String place = event == null ? "" : event.getVenue();
            if (place.isEmpty() && event != null) {
                place = !event.getAddress().isEmpty() ? event.getAddress() : event.getCity();
            }
            binding.venueText.setText(place);
            binding.venueRow.setVisibility(place.isEmpty() ? android.view.View.GONE : android.view.View.VISIBLE);

            bindStatus(context, item.getKind());
            binding.getRoot().setOnClickListener(v -> listener.onEventClick(item));
        }

        private void bindStatus(@NonNull Context context, @NonNull MyEventItem.Kind kind) {
            int label;
            @DrawableRes int icon;
            int background;
            int textColor;
            switch (kind) {
                case GROUP_REGISTERED:
                    label = R.string.my_events_status_group;
                    icon = R.drawable.ic_status_group;
                    background = R.drawable.bg_status_group;
                    textColor = R.color.leef_cat_competition_badge;
                    break;
                case CANCELLED:
                    label = R.string.my_events_status_cancelled;
                    icon = R.drawable.ic_close;
                    background = R.drawable.bg_status_saved;
                    textColor = R.color.leef_error;
                    break;
                case SAVED:
                    label = R.string.my_events_status_saved;
                    icon = R.drawable.ic_status_saved;
                    background = R.drawable.bg_status_saved;
                    textColor = R.color.leef_gray_700;
                    break;
                case REGISTERED:
                default:
                    label = R.string.my_events_status_registered;
                    icon = R.drawable.ic_status_registered;
                    background = R.drawable.bg_status_registered;
                    textColor = R.color.leef_green_900;
                    break;
            }
            int color = ContextCompat.getColor(context, textColor);
            binding.status.setText(label);
            binding.status.setBackgroundResource(background);
            binding.status.setTextColor(color);
            binding.status.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0);
            TextViewCompat.setCompoundDrawableTintList(binding.status, android.content.res.ColorStateList.valueOf(color));
        }
    }
}
