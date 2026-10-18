package com.itismob.s03.group7.leef.messages;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.data.FirebaseMessageRepository;
import com.itismob.s03.group7.leef.data.MessageRepository;
import com.itismob.s03.group7.leef.databinding.ActivityChatBinding;
import com.itismob.s03.group7.leef.model.ChatMessage;
import com.itismob.s03.group7.leef.model.ChatRef;
import com.itismob.s03.group7.leef.ui.LeefEdgeToEdge;

import java.util.List;

/**
 * Screen 10 - one chat: an event's group chat (everyone registered for it) or a direct chat between
 * two people. Messages arrive live. Tapping the name above someone's message in an event chat offers to
 * message them privately.
 */
public class ChatActivity extends AppCompatActivity {

    private static final String EXTRA_KIND = "kind";
    private static final String EXTRA_TARGET_ID = "target_id";
    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_COVER_URL = "cover_url";

    private ActivityChatBinding binding;
    private ChatViewModel viewModel;
    private ChatAdapter adapter;
    private ChatRef ref;
    private String title;
    private String myUid = "";
    private boolean started;
    private int shownCount;
    @Nullable
    private String lastShownId;

    /**
     * @param title    the event's title (event chat) or the other person's name (direct chat)
     * @param coverUrl the event's photo for an event chat, if known
     */
    @NonNull
    public static Intent createIntent(@NonNull Context context, @NonNull ChatRef ref, @NonNull String title,
                                      @Nullable String coverUrl) {
        return new Intent(context, ChatActivity.class)
                .putExtra(EXTRA_KIND, ref.getKind().name())
                .putExtra(EXTRA_TARGET_ID, ref.getTargetId())
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_COVER_URL, coverUrl);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeefEdgeToEdge.enable(this);

        String kind = getIntent().getStringExtra(EXTRA_KIND);
        String targetId = getIntent().getStringExtra(EXTRA_TARGET_ID);
        title = getIntent().getStringExtra(EXTRA_TITLE);
        if (kind == null || targetId == null || title == null) {
            finish();
            return;
        }
        ref = ChatRef.Kind.EVENT.name().equals(kind) ? ChatRef.event(targetId) : ChatRef.direct(targetId);
        String uid = new FirebaseMessageRepository().getSignedInUid();
        if (uid == null) {
            finish();
            return;
        }
        myUid = uid;

        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets();

        viewModel = new ViewModelProvider(this, new ChatViewModel.Factory(ref, title)).get(ChatViewModel.class);

        // Header
        binding.chatTitle.setText(title);
        binding.chatSubtitle.setText(ref.isEvent() ? R.string.chat_subtitle_event : R.string.chat_subtitle_direct);
        ChatAvatar.bind(binding.avatarArt, binding.avatarPhoto, binding.avatarInitials, ref, title,
                getIntent().getStringExtra(EXTRA_COVER_URL));
        binding.emptyBody.setText(ref.isEvent() ? R.string.chat_empty_body_event : R.string.chat_empty_body_direct);

        // Messages
        adapter = new ChatAdapter(this::offerDirectMessage);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.messageList.setLayoutManager(layoutManager);
        binding.messageList.setAdapter(adapter);
        binding.messageList.setItemAnimator(null);

        // Writing
        binding.messageField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                binding.sendButton.setEnabled(s.toString().trim().length() > 0);
            }
        });
        binding.sendButton.setEnabled(binding.messageField.getText().toString().trim().length() > 0);
        binding.sendButton.setOnClickListener(v -> send());
        binding.backButton.setOnClickListener(v -> finish());
        binding.retryButton.setOnClickListener(v -> viewModel.retry());

        viewModel.getState().observe(this, this::render);
        viewModel.getMessage().observe(this, messageRes -> {
            if (messageRes != null) {
                Toast.makeText(this, messageRes, Toast.LENGTH_LONG).show();
                viewModel.messageHandled();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        started = true;
        if (viewModel != null) {
            viewModel.markRead();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        started = false;
    }

    private void send() {
        String text = binding.messageField.getText().toString();
        if (text.trim().isEmpty()) {
            return;
        }
        binding.messageField.setText("");
        viewModel.send(text);
    }

    private void render(@NonNull ChatState state) {
        ChatState.Status status = state.getStatus();
        boolean content = status == ChatState.Status.CONTENT;
        boolean empty = content && state.getMessages().isEmpty();

        binding.loading.setVisibility(status == ChatState.Status.LOADING ? View.VISIBLE : View.GONE);
        binding.errorGroup.setVisibility(status == ChatState.Status.ERROR ? View.VISIBLE : View.GONE);
        binding.emptyGroup.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.messageList.setVisibility(content && !empty ? View.VISIBLE : View.GONE);
        binding.inputBar.setVisibility(status == ChatState.Status.ERROR ? View.GONE : View.VISIBLE);

        if (status == ChatState.Status.ERROR) {
            binding.errorText.setText(errorText(state.getFailure()));
        }
        if (content) {
            showMessages(state.getMessages());
        }
    }

    private void showMessages(@NonNull List<ChatMessage> messages) {
        List<ChatRow> rows = ChatRow.build(this, messages, myUid, ref.isEvent());
        ChatMessage newest = messages.isEmpty() ? null : messages.get(messages.size() - 1);
        String newestId = newest == null ? null : newest.getId();
        boolean grew = messages.size() != shownCount || (newestId != null && !newestId.equals(lastShownId));
        LinearLayoutManager layoutManager = (LinearLayoutManager) binding.messageList.getLayoutManager();
        boolean atBottom = layoutManager == null || adapter.getItemCount() == 0
                || layoutManager.findLastVisibleItemPosition() >= adapter.getItemCount() - 2;
        boolean mine = newest != null && newest.getSenderId().equals(myUid);

        shownCount = messages.size();
        lastShownId = newestId;
        adapter.submitList(rows, () -> {
            // Follow new messages when already at the end (or when the person just sent one).
            if (grew && !rows.isEmpty() && (atBottom || mine)) {
                binding.messageList.scrollToPosition(rows.size() - 1);
            }
        });
        if (grew && started) {
            viewModel.markRead();
        }
    }

    /** In an event chat, tapping someone's name offers a private chat with them. */
    private void offerDirectMessage(@NonNull ChatMessage message) {
        if (message.getSenderId().equals(myUid)) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.chat_dm_title, message.getSenderName()))
                .setMessage(R.string.chat_dm_body)
                .setPositiveButton(R.string.chat_dm_confirm, (dialog, which) ->
                        startActivity(createIntent(this, ChatRef.direct(message.getSenderId()),
                                message.getSenderName(), null)))
                .setNegativeButton(R.string.chat_dm_cancel, null)
                .show();
    }

    private int errorText(@Nullable MessageRepository.Failure failure) {
        if (failure == MessageRepository.Failure.PERMISSION) {
            return ref.isEvent() ? R.string.chat_error_not_member : R.string.messages_error_permission;
        }
        return MessagesFragment.errorMessageFor(failure);
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.chatRoot, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
