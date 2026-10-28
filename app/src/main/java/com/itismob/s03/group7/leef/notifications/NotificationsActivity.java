package com.itismob.s03.group7.leef.notifications;

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
import com.itismob.s03.group7.leef.databinding.ActivityNotificationsBinding;
import com.itismob.s03.group7.leef.details.EventDetailsActivity;
import com.itismob.s03.group7.leef.feedback.FeedbackActivity;
import com.itismob.s03.group7.leef.messages.ChatActivity;
import com.itismob.s03.group7.leef.model.AppNotification;
import com.itismob.s03.group7.leef.ui.LeefEdgeToEdge;

/** The list behind the bell on Home: reminders, registrations, event changes and new messages. */
public class NotificationsActivity extends AppCompatActivity {

    private ActivityNotificationsBinding binding;
    private NotificationsViewModel viewModel;
    private NotificationAdapter adapter;

    @NonNull
    public static Intent createIntent(@NonNull Context context) {
        return new Intent(context, NotificationsActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeefEdgeToEdge.enable(this);
        binding = ActivityNotificationsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets();

        viewModel = new ViewModelProvider(this).get(NotificationsViewModel.class);
        adapter = new NotificationAdapter(this::open);
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

    /** Messages open their chat, "rate" opens the rating screen, everything else opens the event. */
    private void open(@NonNull AppNotification item) {
        switch (item.getKind()) {
            case NEW_MESSAGES:
                if (item.getChatRef() != null) {
                    startActivity(ChatActivity.createIntent(this, item.getChatRef(), item.getTitle(),
                            item.getCoverUrl()));
                }
                break;
            case RATE_EVENT:
                if (item.getEventId() != null) {
                    startActivity(FeedbackActivity.createIntent(this, item.getEventId()));
                }
                break;
            default:
                if (item.getEventId() != null) {
                    startActivity(EventDetailsActivity.createIntent(this, item.getEventId()));
                }
                break;
        }
    }

    private void render(@NonNull NotificationsState state) {
        NotificationsState.Status status = state.getStatus();
        binding.loadingSpinner.setVisibility(status == NotificationsState.Status.LOADING ? View.VISIBLE : View.GONE);
        binding.list.setVisibility(status == NotificationsState.Status.CONTENT ? View.VISIBLE : View.GONE);
        boolean showState = status == NotificationsState.Status.EMPTY || status == NotificationsState.Status.ERROR;
        binding.stateGroup.setVisibility(showState ? View.VISIBLE : View.GONE);
        if (status == NotificationsState.Status.CONTENT) {
            adapter.submit(state.getNotifications(), state.getLastReadMillis());
        } else if (status == NotificationsState.Status.EMPTY) {
            binding.stateIcon.setImageResource(R.drawable.ic_bell);
            binding.stateTitle.setText(R.string.notif_empty_title);
            binding.stateBody.setText(R.string.notif_empty_body);
            binding.retryButton.setVisibility(View.GONE);
        } else if (status == NotificationsState.Status.ERROR) {
            binding.stateIcon.setImageResource(R.drawable.ic_state_cloud_off);
            binding.stateTitle.setText(R.string.notif_error_title);
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
