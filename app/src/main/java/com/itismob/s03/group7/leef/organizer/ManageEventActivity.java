package com.itismob.s03.group7.leef.organizer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.boost.BoostActivity;
import com.itismob.s03.group7.leef.databinding.ActivityManageEventBinding;
import com.itismob.s03.group7.leef.databinding.ViewProfileRowBinding;
import com.itismob.s03.group7.leef.details.EventDetailsActivity;
import com.itismob.s03.group7.leef.feedback.FeedbackListActivity;
import com.itismob.s03.group7.leef.home.EventFormat;
import com.itismob.s03.group7.leef.home.HomeViewModel;
import com.itismob.s03.group7.leef.model.Event;
import com.itismob.s03.group7.leef.postevent.CreateEventActivity;
import com.itismob.s03.group7.leef.ui.LeefEdgeToEdge;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * An organizer's control panel for one event they posted: how many people registered and checked in,
 * the attendee list, the check-in scanner, feedback, promoting the event, and the public page.
 */
public class ManageEventActivity extends AppCompatActivity {

    private static final String EXTRA_EVENT_ID = "event_id";

    private ActivityManageEventBinding binding;
    private ManageEventViewModel viewModel;
    private String eventId;

    @NonNull
    public static Intent createIntent(@NonNull Context context, @NonNull String eventId) {
        return new Intent(context, ManageEventActivity.class).putExtra(EXTRA_EVENT_ID, eventId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeefEdgeToEdge.enable(this);
        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null || eventId.isEmpty()) {
            finish();
            return;
        }
        binding = ActivityManageEventBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets();

        viewModel = new ViewModelProvider(this, new ManageEventViewModel.Factory(eventId))
                .get(ManageEventViewModel.class);
        binding.backButton.setOnClickListener(v -> finish());
        binding.retryButton.setOnClickListener(v -> viewModel.retry());
        binding.unavailableBackButton.setOnClickListener(v -> finish());
        viewModel.getState().observe(this, this::render);
        viewModel.getMessage().observe(this, messageRes -> {
            if (messageRes != null) {
                Toast.makeText(this, messageRes, Toast.LENGTH_LONG).show();
                viewModel.messageHandled();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.reload();
        }
    }

    private void render(@NonNull ManageEventState state) {
        ManageEventState.Status status = state.getStatus();
        boolean content = status == ManageEventState.Status.CONTENT && state.getEvent() != null;
        boolean unavailable = status == ManageEventState.Status.NOT_FOUND
                || status == ManageEventState.Status.NOT_ORGANIZER;

        binding.loading.setVisibility(status == ManageEventState.Status.LOADING ? View.VISIBLE : View.GONE);
        binding.errorGroup.setVisibility(status == ManageEventState.Status.ERROR ? View.VISIBLE : View.GONE);
        binding.unavailableGroup.setVisibility(unavailable ? View.VISIBLE : View.GONE);
        binding.scroll.setVisibility(content ? View.VISIBLE : View.GONE);

        if (status == ManageEventState.Status.ERROR) {
            binding.errorText.setText(HomeViewModel.errorMessageFor(state.getFailure()));
        }
        if (unavailable) {
            boolean notOrganizer = status == ManageEventState.Status.NOT_ORGANIZER;
            binding.unavailableTitle.setText(notOrganizer ? R.string.boost_not_organizer_title
                    : R.string.registration_unavailable_title);
            binding.unavailableBody.setText(notOrganizer ? R.string.manage_not_organizer_body
                    : R.string.registration_unavailable_body);
        }
        if (content) {
            bind(state, state.getEvent());
        }
    }

    private void bind(@NonNull ManageEventState state, @NonNull Event event) {
        long now = System.currentTimeMillis();
        binding.eventTitle.setText(event.getTitle());
        String time = EventFormat.timeText(this, event);
        binding.eventWhen.setText(EventFormat.detailDateText(event) + (time == null ? "" : " · " + time));
        String place = !event.getVenue().isEmpty() ? event.getVenue()
                : !event.getAddress().isEmpty() ? event.getAddress() : event.getCity();
        binding.eventWhere.setText(place);
        binding.eventWhere.setVisibility(place.isEmpty() ? View.GONE : View.VISIBLE);

        binding.registeredCount.setText(state.isCountsKnown() ? String.valueOf(state.getPeople()) : "–");
        binding.checkedInCount.setText(state.isCountsKnown() ? String.valueOf(state.getCheckedInPeople()) : "–");

        boolean ended = event.hasEnded(now);
        boolean cancelled = state.isCancelled();
        if (cancelled) {
            binding.statusLine.setText(R.string.manage_status_cancelled);
        } else if (event.isBoosted(now) && event.getBoostedUntilMillis() > 0) {
            binding.statusLine.setText(getString(R.string.manage_status_boosted, new SimpleDateFormat(
                    "MMM d, h:mm a", Locale.getDefault()).format(new Date(event.getBoostedUntilMillis()))));
        } else {
            binding.statusLine.setText(ended ? R.string.manage_status_ended
                    : event.hasStarted(now) ? R.string.manage_status_live : R.string.manage_status_upcoming);
        }

        binding.actions.removeAllViews();
        binding.actions.setAlpha(state.isBusy() ? 0.5f : 1f);
        addRow(R.drawable.ic_people, R.string.manage_attendees, v ->
                startActivity(AttendeesActivity.createIntent(this, eventId)));
        if (!cancelled) {
            addRow(R.drawable.ic_scan, R.string.manage_check_in, v ->
                    startActivity(ScanCheckInActivity.createIntent(this, eventId)));
            addRow(R.drawable.ic_star_outline, R.string.manage_feedback, v ->
                    startActivity(FeedbackListActivity.createIntent(this, eventId)));
        }
        if (!cancelled && !event.hasStarted(now)) {
            addRow(R.drawable.ic_bolt, event.isBoosted(now) ? R.string.manage_promote_more : R.string.manage_promote,
                    v -> startActivity(BoostActivity.createIntent(this, eventId)));
            addRow(R.drawable.ic_edit, R.string.manage_edit, v ->
                    startActivity(CreateEventActivity.editIntent(this, eventId)));
            addRow(R.drawable.ic_close, R.string.manage_cancel_event, v -> confirmCancel(event, state));
        }
        addRow(R.drawable.ic_doc, R.string.manage_view_page, v ->
                startActivity(EventDetailsActivity.createIntent(this, eventId)));
    }

    /** Asks before cancelling, because it can't be undone. */
    private void confirmCancel(@NonNull Event event, @NonNull ManageEventState state) {
        if (state.isBusy()) {
            return;
        }
        String body = state.isCountsKnown() && state.getPeople() > 0
                ? getResources().getQuantityString(R.plurals.manage_cancel_body_people, state.getPeople(),
                state.getPeople())
                : getString(R.string.manage_cancel_body);
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.manage_cancel_title, event.getTitle()))
                .setMessage(body)
                .setPositiveButton(R.string.manage_cancel_confirm, (dialog, which) -> viewModel.cancelEvent())
                .setNegativeButton(R.string.manage_cancel_keep, null)
                .show();
    }

    private void addRow(@DrawableRes int icon, @StringRes int title, @NonNull View.OnClickListener onClick) {
        ViewProfileRowBinding row = ViewProfileRowBinding.inflate(LayoutInflater.from(this), binding.actions, false);
        row.rowIcon.setImageResource(icon);
        row.rowTitle.setText(title);
        row.getRoot().setOnClickListener(onClick);
        binding.actions.addView(row.getRoot());
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.manageRoot, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
