package com.itismob.s03.group7.leef.messages;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.data.FirebaseMessageRepository;
import com.itismob.s03.group7.leef.databinding.FragmentMessagesBinding;
import com.itismob.s03.group7.leef.data.MessageRepository;
import com.itismob.s03.group7.leef.search.SearchActivity;

/**
 * Screen 10 - Messaging: the person's conversations (the chat of each event they registered for, and
 * their direct chats), newest first, with a search box and unread badges. Tapping one opens the chat.
 */
public class MessagesFragment extends Fragment {

    private FragmentMessagesBinding binding;
    private MessagesViewModel viewModel;
    private ConversationAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMessagesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MessagesViewModel.class);
        ViewCompat.setAccessibilityHeading(binding.title, true);

        String uid = new FirebaseMessageRepository().getSignedInUid();
        adapter = new ConversationAdapter(uid == null ? "" : uid, conversation ->
                startActivity(ChatActivity.createIntent(requireContext(), conversation.getRef(),
                        conversation.getTitle(), conversation.getCoverUrl())));
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.setAdapter(adapter);
        binding.list.setItemAnimator(null);

        binding.swipeRefresh.setColorSchemeResources(R.color.leef_green_700);
        binding.swipeRefresh.setOnRefreshListener(viewModel::refresh);
        binding.retryButton.setOnClickListener(v -> viewModel.retry());
        binding.exploreButton.setOnClickListener(v ->
                startActivity(SearchActivity.createIntent(requireContext(), false)));

        binding.searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                binding.searchClear.setVisibility(s.length() == 0 ? View.GONE : View.VISIBLE);
                viewModel.setQuery(s.toString());
            }
        });
        binding.searchClear.setOnClickListener(v -> binding.searchField.setText(""));

        viewModel.getState().observe(getViewLifecycleOwner(), this::render);
        viewModel.getMessage().observe(getViewLifecycleOwner(), messageRes -> {
            if (messageRes != null) {
                Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show();
                viewModel.messageHandled();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isHidden()) {
            viewModel.reload();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && viewModel != null) {
            viewModel.reload();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void render(@NonNull MessagesState state) {
        MessagesState.Status status = state.getStatus();
        boolean content = status == MessagesState.Status.CONTENT;
        boolean empty = content && state.getVisible().isEmpty();

        binding.loading.setVisibility(status == MessagesState.Status.LOADING ? View.VISIBLE : View.GONE);
        binding.errorGroup.setVisibility(status == MessagesState.Status.ERROR ? View.VISIBLE : View.GONE);
        binding.emptyGroup.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.swipeRefresh.setVisibility(content && !empty ? View.VISIBLE : View.GONE);
        binding.swipeRefresh.setRefreshing(state.isRefreshing());

        if (status == MessagesState.Status.ERROR) {
            binding.errorText.setText(errorMessageFor(state.getFailure()));
        }
        if (empty) {
            boolean searching = state.hasAny();
            binding.emptyTitle.setText(searching ? R.string.messages_no_matches_title : R.string.messages_empty_title);
            binding.emptyBody.setText(searching ? R.string.messages_no_matches_body : R.string.messages_empty_body);
            binding.exploreButton.setVisibility(searching ? View.GONE : View.VISIBLE);
        }
        adapter.submitList(state.getVisible());
    }

    /** The text for a failed load or send. */
    static int errorMessageFor(@Nullable MessageRepository.Failure failure) {
        if (failure == MessageRepository.Failure.NETWORK) {
            return R.string.messages_error_network;
        }
        if (failure == MessageRepository.Failure.PERMISSION) {
            return R.string.messages_error_permission;
        }
        return R.string.messages_error_generic;
    }
}
