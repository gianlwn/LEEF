package com.itismob.s03.group7.leef.feedback;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.chip.Chip;
import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.databinding.ActivityFeedbackBinding;
import com.itismob.s03.group7.leef.home.EventCardBinder;
import com.itismob.s03.group7.leef.home.EventFormat;
import com.itismob.s03.group7.leef.model.Event;
import com.itismob.s03.group7.leef.model.FeedbackTag;
import com.itismob.s03.group7.leef.reminder.ReminderSync;
import com.itismob.s03.group7.leef.ui.ButtonSpinner;
import com.itismob.s03.group7.leef.ui.ImageLinkDialog;
import com.itismob.s03.group7.leef.ui.LeefEdgeToEdge;

/**
 * Screen 12 - Event Feedback: after an event the person registered for is over, they give it 1 to 5
 * stars, pick what stood out, and can add a comment and a photo link. Sent feedback can be changed by
 * opening the screen again.
 */
public class FeedbackActivity extends AppCompatActivity {

    private static final String EXTRA_EVENT_ID = "event_id";

    private ActivityFeedbackBinding binding;
    private FeedbackViewModel viewModel;
    private boolean lastPosting;
    @Nullable
    private FeedbackState.Status shownStatus;

    @NonNull
    public static Intent createIntent(@NonNull Context context, @NonNull String eventId) {
        return new Intent(context, FeedbackActivity.class).putExtra(EXTRA_EVENT_ID, eventId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeefEdgeToEdge.enable(this);

        String eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null || eventId.isEmpty()) {
            finish();
            return;
        }
        binding = ActivityFeedbackBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets();

        viewModel = new ViewModelProvider(this, new FeedbackViewModel.Factory(eventId))
                .get(FeedbackViewModel.class);

        ViewCompat.setAccessibilityHeading(binding.questionTitle, true);
        ViewCompat.setAccessibilityHeading(binding.commentTitle, true);
        ViewCompat.setAccessibilityHeading(binding.photoTitle, true);

        binding.backButton.setOnClickListener(v -> finish());
        binding.doneButton.setOnClickListener(v -> finish());
        binding.stateButton.setOnClickListener(v -> {
            FeedbackState current = viewModel.getState().getValue();
            if (current != null && current.getStatus() == FeedbackState.Status.ERROR) {
                viewModel.retry();
            } else {
                finish();
            }
        });

        binding.starBar.setOnRatingChangeListener(rating -> viewModel.setRating(rating));
        binding.photoCard.setOnClickListener(v -> askForPhotoLink());
        binding.changePhotoButton.setOnClickListener(v -> askForPhotoLink());
        binding.removePhotoButton.setOnClickListener(v -> {
            viewModel.setPhotoUrl(null);
            showPhoto();
        });
        binding.submitButton.setOnClickListener(v -> {
            hideKeyboard();
            String comment = binding.commentInput.getText() == null ? ""
                    : binding.commentInput.getText().toString();
            viewModel.submit(comment);
        });

        setUpTags();
        binding.starBar.setRating(viewModel.getRating());
        showPhoto();

        viewModel.getState().observe(this, this::render);
        viewModel.getRatingMissing().observe(this, missing ->
                binding.ratingError.setVisibility(Boolean.TRUE.equals(missing) ? View.VISIBLE : View.GONE));
        viewModel.getExistingComment().observe(this, comment -> {
            if (comment != null) {
                binding.commentInput.setText(comment);
                viewModel.existingCommentHandled();
            }
        });
        viewModel.getMessage().observe(this, messageRes -> {
            if (messageRes != null) {
                Toast.makeText(this, messageRes, Toast.LENGTH_LONG).show();
                viewModel.messageHandled();
            }
        });
    }

    private void setUpTags() {
        for (FeedbackTag tag : FeedbackTag.values()) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_filter_chip, binding.tagGroup, false);
            chip.setId(View.generateViewId());
            chip.setText(tag.getLabelRes());
            chip.setCheckable(true);
            chip.setChecked(viewModel.getTags().contains(tag));
            chip.setOnCheckedChangeListener((button, checked) -> viewModel.setTag(tag, checked));
            binding.tagGroup.addView(chip);
        }
    }

    private void render(@NonNull FeedbackState state) {
        FeedbackState.Status status = state.getStatus();
        boolean form = status == FeedbackState.Status.FORM;
        boolean done = status == FeedbackState.Status.DONE;
        binding.formScroll.setVisibility(form ? View.VISIBLE : View.GONE);
        binding.bottomBar.setVisibility(form ? View.VISIBLE : View.GONE);
        binding.donePanel.setVisibility(done ? View.VISIBLE : View.GONE);
        boolean showState = !form && !done;
        binding.statePanel.setVisibility(showState ? View.VISIBLE : View.GONE);
        binding.loadingSpinner.setVisibility(status == FeedbackState.Status.LOADING ? View.VISIBLE : View.GONE);
        boolean error = status == FeedbackState.Status.ERROR;
        boolean unavailable = status == FeedbackState.Status.UNAVAILABLE;
        binding.stateIcon.setVisibility(error || unavailable ? View.VISIBLE : View.GONE);
        binding.stateIcon.setImageResource(error ? R.drawable.ic_state_cloud_off : R.drawable.ic_star_outline);
        binding.stateTitle.setVisibility(unavailable ? View.VISIBLE : View.GONE);
        binding.stateBody.setVisibility(error || unavailable ? View.VISIBLE : View.GONE);
        binding.stateButton.setVisibility(error || unavailable ? View.VISIBLE : View.GONE);
        if (error || unavailable) {
            if (unavailable) {
                binding.stateTitle.setText(state.getTitleRes());
            }
            binding.stateBody.setText(state.getBodyRes());
            binding.stateButton.setText(error ? R.string.home_retry : R.string.feedback_unavailable_back);
        }

        Event event = state.getEvent();
        if (form && event != null) {
            if (shownStatus != FeedbackState.Status.FORM) {
                // First time the form shows: fill in feedback the person gave earlier, if any.
                binding.starBar.setRating(viewModel.getRating());
                for (int i = 0; i < binding.tagGroup.getChildCount(); i++) {
                    ((Chip) binding.tagGroup.getChildAt(i)).setChecked(
                            viewModel.getTags().contains(FeedbackTag.values()[i]));
                }
                showPhoto();
            }
            bindEvent(event);
            showPosting(state);
        }
        shownStatus = status;
    }

    private void bindEvent(@NonNull Event event) {
        EventCardBinder.bindCover(binding.coverPhoto, event.getCoverUrl());
        binding.eventTitle.setText(event.getTitle());
        binding.eventDate.setText(EventFormat.detailDateText(event));
        String place = ReminderSync.placeOf(event);
        binding.eventPlaceRow.setVisibility(place.isEmpty() ? View.GONE : View.VISIBLE);
        binding.eventPlace.setText(place);
    }

    private void showPosting(@NonNull FeedbackState state) {
        boolean posting = state.isPosting();
        if (posting == lastPosting && shownStatus == FeedbackState.Status.FORM) {
            return;
        }
        lastPosting = posting;
        binding.submitButton.setEnabled(!posting);
        binding.submitButton.setText(posting ? "" : getString(
                state.isEditing() ? R.string.feedback_update : R.string.feedback_submit));
        binding.submitButton.setIcon(posting ? ButtonSpinner.create(this) : null);
        binding.formScroll.setAlpha(posting ? 0.6f : 1f);
        setEnabledDeep(binding.formScroll, !posting);
    }

    private static void setEnabledDeep(@NonNull View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                setEnabledDeep(group.getChildAt(i), enabled);
            }
        }
    }

    private void askForPhotoLink() {
        ImageLinkDialog.show(this, viewModel.getPhotoUrl(), link -> {
            viewModel.setPhotoUrl(link);
            showPhoto();
        });
    }

    private void showPhoto() {
        String url = viewModel.getPhotoUrl();
        boolean has = url != null && !url.isEmpty();
        binding.photoEmpty.setVisibility(has ? View.GONE : View.VISIBLE);
        binding.photoPreview.setVisibility(has ? View.VISIBLE : View.GONE);
        binding.photoActions.setVisibility(has ? View.VISIBLE : View.GONE);
        binding.photoError.setVisibility(View.GONE);
        if (!has) {
            Glide.with(binding.photoPreview).clear(binding.photoPreview);
            return;
        }
        Glide.with(binding.photoPreview)
                .load(url)
                .centerCrop()
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, @Nullable Object model,
                                                @NonNull Target<Drawable> target, boolean isFirstResource) {
                        // Not a picture (or not reachable): drop the link and say so.
                        if (url.equals(viewModel.getPhotoUrl())) {
                            viewModel.setPhotoUrl(null);
                            showPhoto();
                            binding.photoError.setVisibility(View.VISIBLE);
                        }
                        return true;
                    }

                    @Override
                    public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model,
                                                   Target<Drawable> target, @NonNull DataSource dataSource,
                                                   boolean isFirstResource) {
                        return false;
                    }
                })
                .into(binding.photoPreview);
    }

    private void hideKeyboard() {
        View focus = getCurrentFocus();
        if (focus != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
            }
            focus.clearFocus();
        }
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.feedbackRoot, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
