package com.itismob.s03.group7.leef.feedback;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.databinding.ActivityFeedbackListBinding;
import com.itismob.s03.group7.leef.ui.LeefEdgeToEdge;

/**
 * The organizer's view of the feedback an event received (MCO1: feedback "helps organizers improve
 * future events"): the average rating and every piece of feedback, newest first. Only the organizer of
 * the event can read it; the database rules enforce that.
 */
public class FeedbackListActivity extends AppCompatActivity {

    private static final String EXTRA_EVENT_ID = "event_id";

    private ActivityFeedbackListBinding binding;
    private final FeedbackListAdapter adapter = new FeedbackListAdapter();

    @NonNull
    public static Intent createIntent(@NonNull Context context, @NonNull String eventId) {
        return new Intent(context, FeedbackListActivity.class).putExtra(EXTRA_EVENT_ID, eventId);
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
        binding = ActivityFeedbackListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets();

        FeedbackListViewModel viewModel = new ViewModelProvider(this, new FeedbackListViewModel.Factory(eventId))
                .get(FeedbackListViewModel.class);
        binding.backButton.setOnClickListener(v -> finish());
        binding.retryButton.setOnClickListener(v -> viewModel.retry());
        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.list.setAdapter(adapter);
        viewModel.getState().observe(this, this::render);
    }

    private void render(@NonNull FeedbackListState state) {
        FeedbackListState.Status status = state.getStatus();
        binding.loadingSpinner.setVisibility(status == FeedbackListState.Status.LOADING ? View.VISIBLE : View.GONE);
        binding.list.setVisibility(status == FeedbackListState.Status.CONTENT ? View.VISIBLE : View.GONE);
        boolean showState = status == FeedbackListState.Status.EMPTY || status == FeedbackListState.Status.ERROR;
        binding.stateGroup.setVisibility(showState ? View.VISIBLE : View.GONE);
        if (status == FeedbackListState.Status.CONTENT) {
            adapter.submit(state.getFeedback(), state.getAverage());
        } else if (status == FeedbackListState.Status.EMPTY) {
            binding.stateIcon.setImageResource(R.drawable.ic_star_outline);
            binding.stateTitle.setText(R.string.feedback_list_empty_title);
            binding.stateBody.setText(R.string.feedback_list_empty_body);
            binding.retryButton.setVisibility(View.GONE);
        } else if (status == FeedbackListState.Status.ERROR) {
            binding.stateIcon.setImageResource(R.drawable.ic_state_cloud_off);
            binding.stateTitle.setText(R.string.feedback_list_title);
            binding.stateBody.setText(state.getErrorRes());
            binding.retryButton.setVisibility(View.VISIBLE);
        }
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.listRoot, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
