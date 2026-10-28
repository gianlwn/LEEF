package com.itismob.s03.group7.leef.boost;

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
import com.itismob.s03.group7.leef.databinding.ActivityBoostHistoryBinding;
import com.itismob.s03.group7.leef.details.EventDetailsActivity;
import com.itismob.s03.group7.leef.home.EventFormat;
import com.itismob.s03.group7.leef.ui.LeefEdgeToEdge;

/** The organizer's past boosts (receipts), opened from My Events. Tapping one opens the event. */
public class BoostHistoryActivity extends AppCompatActivity {

    private ActivityBoostHistoryBinding binding;
    private BoostHistoryViewModel viewModel;
    private BoostHistoryAdapter adapter;

    @NonNull
    public static Intent createIntent(@NonNull Context context) {
        return new Intent(context, BoostHistoryActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeefEdgeToEdge.enable(this);
        binding = ActivityBoostHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets();

        viewModel = new ViewModelProvider(this).get(BoostHistoryViewModel.class);
        adapter = new BoostHistoryAdapter(receipt ->
                startActivity(EventDetailsActivity.createIntent(this, receipt.getEventId())));
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

    private void render(@NonNull BoostHistoryState state) {
        BoostHistoryState.Status status = state.getStatus();
        binding.loadingSpinner.setVisibility(status == BoostHistoryState.Status.LOADING ? View.VISIBLE : View.GONE);
        binding.list.setVisibility(status == BoostHistoryState.Status.CONTENT ? View.VISIBLE : View.GONE);
        binding.summary.setVisibility(status == BoostHistoryState.Status.CONTENT ? View.VISIBLE : View.GONE);
        boolean showState = status == BoostHistoryState.Status.EMPTY || status == BoostHistoryState.Status.ERROR;
        binding.stateGroup.setVisibility(showState ? View.VISIBLE : View.GONE);
        if (status == BoostHistoryState.Status.CONTENT) {
            int count = state.getReceipts().size();
            binding.summary.setText(getResources().getQuantityString(R.plurals.boost_history_summary, count, count,
                    EventFormat.priceText(this, state.getTotalSpent())));
            adapter.submitList(state.getReceipts());
        } else if (status == BoostHistoryState.Status.EMPTY) {
            binding.stateIcon.setImageResource(R.drawable.ic_bolt);
            binding.stateTitle.setText(R.string.boost_history_empty_title);
            binding.stateBody.setText(R.string.boost_history_empty_body);
            binding.retryButton.setVisibility(View.GONE);
        } else if (status == BoostHistoryState.Status.ERROR) {
            binding.stateIcon.setImageResource(R.drawable.ic_state_cloud_off);
            binding.stateTitle.setText(R.string.boost_history_error_title);
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
