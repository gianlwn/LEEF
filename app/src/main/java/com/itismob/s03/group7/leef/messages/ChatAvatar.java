package com.itismob.s03.group7.leef.messages;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.itismob.s03.group7.leef.home.EventCardBinder;
import com.itismob.s03.group7.leef.model.ChatRef;
import com.itismob.s03.group7.leef.ui.widget.ArtSlotView;

/** The round picture of a chat: the event's photo for an event chat, the person's initials otherwise. */
final class ChatAvatar {

    private ChatAvatar() {
    }

    static void bind(@NonNull ArtSlotView art, @NonNull ImageView photo, @NonNull TextView initials,
                     @NonNull ChatRef ref, @NonNull String title, @Nullable String coverUrl) {
        if (ref.isEvent()) {
            initials.setVisibility(View.GONE);
            art.setVisibility(View.VISIBLE);
            EventCardBinder.bindCover(photo, coverUrl);
        } else {
            EventCardBinder.bindCover(photo, null);
            art.setVisibility(View.GONE);
            initials.setText(ChatFormat.initialsOf(title));
            initials.setVisibility(View.VISIBLE);
        }
    }
}
