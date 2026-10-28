package com.itismob.s03.group7.leef.organizer;

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
import com.itismob.s03.group7.leef.databinding.ActivityPostedEventsBinding;
import com.itismob.s03.group7.leef.ui.LeefEdgeToEdge;

/** The events the signed-in organizer posted; tapping one opens its Manage screen. */
public class PostedEventsActivity extends AppCompatActivity {

    private ActivityPostedEventsBinding binding;
    private PostedEventsViewModel viewModel;
    private PostedEventsAdapter adapter;

    @NonNull
    public static Intent createIntent(@NonNull Context context) {
        return new Intent(context, PostedEventsActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeefEdgeToEdge.enable(this);
        binding = ActivityPostedEventsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets();

        viewModel = new ViewModelProvider(this).get(PostedEventsViewModel.class);
        adapter = new PostedEventsAdapter(event ->
                startActivity(ManageEventActivity.createIntent(this, event.getId())));
        binding.backButton.setOnClickListener(v -> finish());
        binding.retryButton.setOnClickListener(v -> viewModel.retry());
        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.list.setAdapter(adapter);
        binding.list.setItemAnimator(null);
        viewModel.getState().observe(this, this::render);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.reload();
        }
    }

    private void render(@NonNull PostedEventsState state) {
        PostedEventsState.Status status = state.getStatus();
        binding.loadingSpinner.setVisibility(status == PostedEventsState.Status.LOADING ? View.VISIBLE : View.GONE);
        binding.list.setVisibility(status == PostedEventsState.Status.CONTENT ? View.VISIBLE : View.GONE);
        boolean showState = status == PostedEventsState.Status.EMPTY || status == PostedEventsState.Status.ERROR;
        binding.stateGroup.setVisibility(showState ? View.VISIBLE : View.GONE);
        if (status == PostedEventsState.Status.CONTENT) {
            adapter.submitList(state.getEvents());
        } else if (status == PostedEventsState.Status.EMPTY) {
            binding.stateIcon.setImageResource(R.drawable.ic_nav_my_events);
            binding.stateTitle.setText(R.string.posted_empty_title);
            binding.stateBody.setText(R.string.posted_empty_body);
            binding.retryButton.setVisibility(View.GONE);
        } else if (status == PostedEventsState.Status.ERROR) {
            binding.stateIcon.setImageResource(R.drawable.ic_state_cloud_off);
            binding.stateTitle.setText(R.string.posted_title);
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
