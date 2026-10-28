package com.itismob.s03.group7.leef.feedback;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.model.Event;
import com.itismob.s03.group7.leef.model.FeedbackTag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Shares the person's experience of an event through the phone's share sheet (Facebook, Instagram, X,
 * chats, email). Only the stars and the quick highlights go into the text, never the written comment.
 */
final class FeedbackShare {

    private FeedbackShare() {
    }

    /** "★★★★☆" for 4 out of 5. */
    @NonNull
    static String starsText(int rating) {
        StringBuilder stars = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            stars.append(i <= rating ? '\u2605' : '\u2606');
        }
        return stars.toString();
    }

    @NonNull
    static String messageText(@NonNull Context context, @NonNull Event event, int rating,
                              @NonNull Collection<FeedbackTag> tags) {
        List<String> lines = new ArrayList<>();
        lines.add(context.getString(R.string.feedback_share_intro, event.getTitle(), starsText(rating)));
        if (!tags.isEmpty()) {
            List<String> labels = new ArrayList<>();
            for (FeedbackTag tag : tags) {
                labels.add(context.getString(tag.getLabelRes()));
            }
            lines.add(context.getString(R.string.feedback_share_highlights, TextUtils.join(", ", labels)));
        }
        lines.add(context.getString(R.string.details_share_outro));
        return TextUtils.join("\n", lines);
    }

    static void share(@NonNull Context context, @NonNull Event event, int rating,
                      @NonNull Collection<FeedbackTag> tags) {
        Intent send = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, event.getTitle())
                .putExtra(Intent.EXTRA_TEXT, messageText(context, event, rating, tags));
        try {
            context.startActivity(Intent.createChooser(send, context.getString(R.string.feedback_share_chooser)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.details_share_failed, Toast.LENGTH_SHORT).show();
        }
    }
}
