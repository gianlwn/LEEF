package com.itismob.s03.group7.leef.notifications;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.itismob.s03.group7.leef.databinding.ItemNotificationBinding;
import com.itismob.s03.group7.leef.model.AppNotification;

import java.util.List;

/** The rows of the notifications list. */
final class NotificationAdapter extends ListAdapter<AppNotification, NotificationAdapter.Holder> {

    interface Listener {
        void onNotificationClick(@NonNull AppNotification item);
    }

    private final Listener listener;
    private long lastReadMillis;

    NotificationAdapter(@NonNull Listener listener) {
        super(new DiffUtil.ItemCallback<AppNotification>() {
            @Override
            public boolean areItemsTheSame(@NonNull AppNotification oldItem, @NonNull AppNotification newItem) {
                return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull AppNotification oldItem, @NonNull AppNotification newItem) {
                return oldItem.equals(newItem);
            }
        });
        this.listener = listener;
    }

    /** Shows the list, with the "new" dot on everything newer than {@code lastReadMillis}. */
    void submit(@NonNull List<AppNotification> items, long lastReadMillis) {
        boolean dotsChanged = this.lastReadMillis != lastReadMillis;
        this.lastReadMillis = lastReadMillis;
        submitList(items);
        if (dotsChanged) {
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemNotificationBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position));
    }

    final class Holder extends RecyclerView.ViewHolder {
        private final ItemNotificationBinding binding;

        Holder(@NonNull ItemNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull AppNotification item) {
            Context context = binding.getRoot().getContext();
            binding.kindIcon.setImageResource(NotificationText.iconFor(item.getKind()));
            binding.headline.setText(NotificationText.headline(context, item));
            binding.body.setText(NotificationText.body(context, item));
            binding.time.setText(NotificationText.timeLabel(context, item.getTimeMillis()));
            boolean unread = item.isUnread(lastReadMillis);
            binding.unreadDot.setVisibility(unread ? View.VISIBLE : View.GONE);
            binding.unreadDot.setContentDescription(unread ? context.getString(
                    com.itismob.s03.group7.leef.R.string.notif_new_a11y) : null);
            binding.getRoot().setOnClickListener(v -> listener.onNotificationClick(item));
        }
    }
}
