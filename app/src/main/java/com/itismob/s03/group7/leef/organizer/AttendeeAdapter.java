package com.itismob.s03.group7.leef.organizer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.databinding.ItemAttendeeBinding;
import com.itismob.s03.group7.leef.home.HomeFragment;
import com.itismob.s03.group7.leef.model.Attendee;

import java.text.DateFormat;
import java.util.Date;
import java.util.Set;

/** The rows of the Attendees list. */
final class AttendeeAdapter extends ListAdapter<Attendee, AttendeeAdapter.Holder> {

    interface Listener {
        void onCheckIn(@NonNull Attendee attendee);
    }

    private final Listener listener;
    private Set<String> busyIds = java.util.Collections.emptySet();

    AttendeeAdapter(@NonNull Listener listener) {
        super(new DiffUtil.ItemCallback<Attendee>() {
            @Override
            public boolean areItemsTheSame(@NonNull Attendee oldItem, @NonNull Attendee newItem) {
                return oldItem.getDocumentId().equals(newItem.getDocumentId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Attendee oldItem, @NonNull Attendee newItem) {
                return oldItem.equals(newItem);
            }
        });
        this.listener = listener;
    }

    /** Shows the people, and greys the button of the ones being checked in. */
    void submit(@NonNull java.util.List<Attendee> attendees, @NonNull Set<String> busy) {
        boolean busyChanged = !busy.equals(busyIds);
        busyIds = busy;
        submitList(attendees);
        if (busyChanged) {
            notifyItemRangeChanged(0, getItemCount());
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemAttendeeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position));
    }

    final class Holder extends RecyclerView.ViewHolder {
        private final ItemAttendeeBinding binding;

        Holder(@NonNull ItemAttendeeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull Attendee attendee) {
            android.content.Context context = binding.getRoot().getContext();
            String name = attendee.getName().isEmpty() ? context.getString(R.string.attendees_unnamed) : attendee.getName();
            binding.initials.setText(attendee.getName().isEmpty() ? "?" : HomeFragment.initialsOf(attendee.getName()));
            binding.name.setText(name);

            String ticket = context.getString(attendee.getTicketType().getLabelRes());
            String detail = attendee.getQuantity() > 1
                    ? context.getString(R.string.attendees_ticket_people, ticket, attendee.getQuantity())
                    : ticket;
            binding.detail.setText(detail);
            binding.code.setText(attendee.getConfirmationCode());

            boolean checkedIn = attendee.isCheckedIn();
            binding.checkedInPill.setVisibility(checkedIn ? View.VISIBLE : View.GONE);
            binding.checkInButton.setVisibility(checkedIn ? View.GONE : View.VISIBLE);
            if (checkedIn) {
                binding.checkedInPill.setText(context.getString(R.string.attendees_checked_in_at,
                        DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(attendee.getCheckedInAtMillis()))));
            }
            binding.checkInButton.setEnabled(!busyIds.contains(attendee.getDocumentId()));
            binding.checkInButton.setOnClickListener(v -> listener.onCheckIn(attendee));
        }
    }
}
