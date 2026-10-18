package com.itismob.s03.group7.leef.messages;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.databinding.ItemConversationBinding;
import com.itismob.s03.group7.leef.model.Conversation;

/** The rows of the Messages list. */
final class ConversationAdapter extends ListAdapter<Conversation, ConversationAdapter.Holder> {

    interface Listener {
        void onConversationClick(@NonNull Conversation conversation);
    }

    private final Listener listener;
    @NonNull
    private final String myUid;

    ConversationAdapter(@NonNull String myUid, @NonNull Listener listener) {
        super(new DiffUtil.ItemCallback<Conversation>() {
            @Override
            public boolean areItemsTheSame(@NonNull Conversation oldItem, @NonNull Conversation newItem) {
                return oldItem.getRef().equals(newItem.getRef());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Conversation oldItem, @NonNull Conversation newItem) {
                return oldItem.equals(newItem);
            }
        });
        this.myUid = myUid;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemConversationBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position));
    }

    final class Holder extends RecyclerView.ViewHolder {
        private final ItemConversationBinding binding;

        Holder(@NonNull ItemConversationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull Conversation conversation) {
            Context context = binding.getRoot().getContext();
            binding.getRoot().setOnClickListener(v -> listener.onConversationClick(conversation));

            // Avatar: the event's photo, or the person's initials
            ChatAvatar.bind(binding.avatarArt, binding.avatarPhoto, binding.avatarInitials,
                    conversation.getRef(), conversation.getTitle(), conversation.getCoverUrl());

            binding.title.setText(conversation.getTitle());
            binding.preview.setText(previewText(context, conversation));
            binding.time.setText(conversation.hasMessages()
                    ? ChatFormat.listTime(context, conversation.getLastMessageAtMillis()) : "");

            int unread = conversation.getUnreadCount();
            binding.unreadBadge.setVisibility(unread > 0 ? View.VISIBLE : View.GONE);
            binding.unreadBadge.setText(unread > 9 ? context.getString(R.string.messages_unread_many)
                    : String.valueOf(unread));
            boolean bold = unread > 0;
            binding.title.setTypeface(binding.title.getTypeface(), bold ? Typeface.BOLD
                    : Typeface.NORMAL);
            binding.preview.setTextColor(ContextCompat.getColor(context,
                    bold ? R.color.leef_ink : R.color.leef_gray_700));
        }

        /** "You: Thanks!", "John: The materials are ready", or the message alone in a direct chat. */
        @NonNull
        private String previewText(@NonNull Context context, @NonNull Conversation conversation) {
            if (!conversation.hasMessages()) {
                return context.getString(R.string.messages_no_messages);
            }
            String text = conversation.getLastMessage() == null ? "" : conversation.getLastMessage();
            if (myUid.equals(conversation.getLastSenderId())) {
                return context.getString(R.string.messages_preview_you, text);
            }
            if (conversation.getRef().isEvent() && conversation.getLastSenderName() != null
                    && !conversation.getLastSenderName().isEmpty()) {
                return context.getString(R.string.messages_preview_sender,
                        ChatFormat.firstName(conversation.getLastSenderName()), text);
            }
            return text;
        }
    }
}
