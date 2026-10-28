package com.itismob.s03.group7.leef.organizer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.databinding.ActivityAttendeesBinding;
import com.itismob.s03.group7.leef.ui.LeefEdgeToEdge;

/**
 * The organizer's list of who registered for an event: name, ticket, code, and whether they have been
 * checked in, with a button to check someone in by hand. Only the event's organizer can read it; the
 * database rules enforce that.
 */
public class AttendeesActivity extends AppCompatActivity {

    private static final String EXTRA_EVENT_ID = "event_id";

    private ActivityAttendeesBinding binding;
    private AttendeesViewModel viewModel;
    private AttendeeAdapter adapter;

    @NonNull
    public static Intent createIntent(@NonNull Context context, @NonNull String eventId) {
        return new Intent(context, AttendeesActivity.class).putExtra(EXTRA_EVENT_ID, eventId);
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
        binding = ActivityAttendeesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets();

        viewModel = new ViewModelProvider(this, new AttendeesViewModel.Factory(eventId)).get(AttendeesViewModel.class);
        adapter = new AttendeeAdapter(viewModel::checkIn);
        binding.backButton.setOnClickListener(v -> finish());
        binding.retryButton.setOnClickListener(v -> viewModel.retry());
        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.list.setAdapter(adapter);
        binding.list.setItemAnimator(null);

        viewModel.getState().observe(this, this::render);
        viewModel.getMessage().observe(this, messageRes -> {
            if (messageRes != null) {
                Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show();
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

    private void render(@NonNull AttendeesState state) {
        AttendeesState.Status status = state.getStatus();
        boolean content = status == AttendeesState.Status.CONTENT;
        binding.loadingSpinner.setVisibility(status == AttendeesState.Status.LOADING ? View.VISIBLE : View.GONE);
        binding.list.setVisibility(content ? View.VISIBLE : View.GONE);
        binding.summary.setVisibility(content ? View.VISIBLE : View.GONE);
        boolean showState = status == AttendeesState.Status.EMPTY || status == AttendeesState.Status.ERROR;
        binding.stateGroup.setVisibility(showState ? View.VISIBLE : View.GONE);
        if (content) {
            binding.summary.setText(getString(R.string.attendees_summary, state.getPeople(), state.getCheckedInPeople()));
            adapter.submit(state.getAttendees(), state.getBusyIds());
        } else if (status == AttendeesState.Status.EMPTY) {
            binding.stateIcon.setImageResource(R.drawable.ic_people);
            binding.stateTitle.setText(R.string.attendees_empty_title);
            binding.stateBody.setText(R.string.attendees_empty_body);
            binding.retryButton.setVisibility(View.GONE);
        } else if (status == AttendeesState.Status.ERROR) {
            binding.stateIcon.setImageResource(R.drawable.ic_state_cloud_off);
            binding.stateTitle.setText(R.string.attendees_title);
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
