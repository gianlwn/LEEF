package com.itismob.s03.group7.leef.messages;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.model.ChatMessage;

/** The lines of one chat: day headings and message bubbles. */
final class ChatAdapter extends ListAdapter<ChatRow, RecyclerView.ViewHolder> {

    interface Listener {
        /** The person tapped the name above someone else's message. */
        void onSenderClick(@NonNull ChatMessage message);
    }

    private final Listener listener;

    ChatAdapter(@NonNull Listener listener) {
        super(new DiffUtil.ItemCallback<ChatRow>() {
            @Override
            public boolean areItemsTheSame(@NonNull ChatRow oldItem, @NonNull ChatRow newItem) {
                return oldItem.id.equals(newItem.id);
            }

            @Override
            public boolean areContentsTheSame(@NonNull ChatRow oldItem, @NonNull ChatRow newItem) {
                return oldItem.equals(newItem);
            }
        });
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).type.ordinal();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ChatRow.Type type = ChatRow.Type.values()[viewType];
        switch (type) {
            case DAY:
                return new DayHolder(inflater.inflate(R.layout.item_chat_day, parent, false));
            case MINE:
                return new MessageHolder(inflater.inflate(R.layout.item_chat_message_mine, parent, false));
            case OTHER:
            default:
                return new MessageHolder(inflater.inflate(R.layout.item_chat_message_other, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatRow row = getItem(position);
        if (holder instanceof DayHolder) {
            ((DayHolder) holder).text.setText(row.dayText);
        } else if (holder instanceof MessageHolder && row.message != null) {
            ((MessageHolder) holder).bind(row);
        }
    }

    private static final class DayHolder extends RecyclerView.ViewHolder {
        final TextView text;

        DayHolder(@NonNull View view) {
            super(view);
            text = view.findViewById(R.id.dayText);
        }
    }

    private final class MessageHolder extends RecyclerView.ViewHolder {
        private final TextView text;
        private final TextView time;
        private final TextView sender;

        MessageHolder(@NonNull View view) {
            super(view);
            text = view.findViewById(R.id.messageText);
            time = view.findViewById(R.id.messageTime);
            sender = view.findViewById(R.id.senderName); // only in the "other" layout
        }

        void bind(@NonNull ChatRow row) {
            ChatMessage message = row.message;
            text.setText(message.getText());
            time.setText(ChatFormat.clockTime(message.getSentAtMillis()));
            if (sender != null) {
                boolean show = row.showSender && !message.getSenderName().isEmpty();
                sender.setVisibility(show ? View.VISIBLE : View.GONE);
                sender.setText(message.getSenderName());
                sender.setOnClickListener(show ? v -> listener.onSenderClick(message) : null);
                sender.setClickable(show);
            }
        }
    }
}
