package com.itismob.s03.group7.leef.feedback;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.itismob.s03.group7.leef.databinding.ItemFeedbackBinding;
import com.itismob.s03.group7.leef.databinding.ItemFeedbackSummaryBinding;
import com.itismob.s03.group7.leef.home.EventFormat;
import com.itismob.s03.group7.leef.model.Feedback;
import com.itismob.s03.group7.leef.model.FeedbackTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The organizer's feedback list: a summary card first, then one card per piece of feedback. */
final class FeedbackListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SUMMARY = 0;
    private static final int TYPE_FEEDBACK = 1;

    private final List<Feedback> items = new ArrayList<>();
    private double average;

    void submit(@NonNull List<Feedback> feedback, double averageRating) {
        items.clear();
        items.addAll(feedback);
        average = averageRating;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_SUMMARY : TYPE_FEEDBACK;
    }

    @Override
    public int getItemCount() {
        return items.isEmpty() ? 0 : items.size() + 1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_SUMMARY) {
            ItemFeedbackSummaryBinding binding = ItemFeedbackSummaryBinding.inflate(inflater, parent, false);
            binding.averageStars.setEditable(false);
            binding.averageStars.setStarSizeDp(26);
            return new SummaryHolder(binding);
        }
        ItemFeedbackBinding binding = ItemFeedbackBinding.inflate(inflater, parent, false);
        binding.stars.setEditable(false);
        binding.stars.setStarSizeDp(26);
        return new FeedbackHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof SummaryHolder) {
            ((SummaryHolder) holder).bind(average, items.size());
        } else {
            ((FeedbackHolder) holder).bind(items.get(position - 1));
        }
    }

    private static final class SummaryHolder extends RecyclerView.ViewHolder {
        private final ItemFeedbackSummaryBinding binding;

        SummaryHolder(@NonNull ItemFeedbackSummaryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(double average, int count) {
            Context context = itemView.getContext();
            binding.average.setText(String.format(Locale.getDefault(), "%.1f", average));
            binding.averageStars.setRating((int) Math.round(average));
            binding.count.setText(context.getResources().getQuantityString(
                    com.itismob.s03.group7.leef.R.plurals.feedback_list_count, count, count));
        }
    }

    private static final class FeedbackHolder extends RecyclerView.ViewHolder {
        private final ItemFeedbackBinding binding;

        FeedbackHolder(@NonNull ItemFeedbackBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull Feedback feedback) {
            Context context = itemView.getContext();
            binding.stars.setRating(feedback.getRating());
            binding.date.setText(feedback.getSubmittedAtMillis() > 0
                    ? EventFormat.shortDateText(feedback.getSubmittedAtMillis()) : "");

            List<String> labels = new ArrayList<>();
            for (FeedbackTag tag : feedback.getTags()) {
                labels.add(context.getString(tag.getLabelRes()));
            }
            binding.tags.setVisibility(labels.isEmpty() ? View.GONE : View.VISIBLE);
            binding.tags.setText(TextUtils.join(" · ", labels));

            binding.comment.setVisibility(feedback.getComment().isEmpty() ? View.GONE : View.VISIBLE);
            binding.comment.setText(feedback.getComment());

            if (feedback.getPhotoUrl() == null) {
                Glide.with(binding.photo).clear(binding.photo);
                binding.photo.setVisibility(View.GONE);
            } else {
                binding.photo.setVisibility(View.VISIBLE);
                Glide.with(binding.photo).load(feedback.getPhotoUrl()).centerCrop().into(binding.photo);
            }
        }
    }
}
