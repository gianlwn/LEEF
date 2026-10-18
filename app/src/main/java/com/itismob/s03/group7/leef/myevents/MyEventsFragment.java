package com.itismob.s03.group7.leef.myevents;

import android.os.Bundle;
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

import com.itismob.s03.group7.leef.MainViewModel;
import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.databinding.FragmentMyEventsBinding;
import com.itismob.s03.group7.leef.details.EventDetailsActivity;
import com.itismob.s03.group7.leef.home.HomeViewModel;
import com.itismob.s03.group7.leef.model.UserRole;
import com.itismob.s03.group7.leef.postevent.CreateEventActivity;
import com.itismob.s03.group7.leef.search.SearchActivity;

/**
 * Screen 8 - My Events Dashboard: the events the person registered for or saved, on an Upcoming and a
 * Past tab, each marked Registered, Group registered or Saved.
 */
public class MyEventsFragment extends Fragment {

    private FragmentMyEventsBinding binding;
    private MyEventsViewModel viewModel;
    private MyEventAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMyEventsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MyEventsViewModel.class);
        ViewCompat.setAccessibilityHeading(binding.title, true);

        adapter = new MyEventAdapter(item ->
                startActivity(EventDetailsActivity.createIntent(requireContext(), item.getEventId())));
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.setAdapter(adapter);
        binding.list.setItemAnimator(null);

        binding.tabUpcoming.setOnClickListener(v -> viewModel.selectTab(false));
        binding.tabPast.setOnClickListener(v -> viewModel.selectTab(true));
        binding.swipeRefresh.setColorSchemeResources(R.color.leef_green_700);
        binding.swipeRefresh.setOnRefreshListener(viewModel::refresh);
        binding.retryButton.setOnClickListener(v -> viewModel.retry());
        binding.postEventButton.setOnClickListener(v ->
                startActivity(CreateEventActivity.createIntent(requireContext())));
        // Organizer accounts get the Post event button (Screen 11).
        MainViewModel mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        mainViewModel.getProfile().observe(getViewLifecycleOwner(), profile ->
                binding.postEventButton.setVisibility(
                        profile != null && profile.getRole() == UserRole.ORGANIZER ? View.VISIBLE : View.GONE));
        binding.exploreButton.setOnClickListener(v ->
                startActivity(SearchActivity.createIntent(requireContext(), false)));

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

    private void render(@NonNull MyEventsState state) {
        boolean past = state.isShowPast();
        binding.tabUpcoming.setSelected(!past);
        binding.tabPast.setSelected(past);

        MyEventsState.Status status = state.getStatus();
        boolean content = status == MyEventsState.Status.CONTENT;
        boolean empty = content && state.getVisible().isEmpty();

        binding.loading.setVisibility(status == MyEventsState.Status.LOADING ? View.VISIBLE : View.GONE);
        binding.errorGroup.setVisibility(status == MyEventsState.Status.ERROR ? View.VISIBLE : View.GONE);
        binding.emptyGroup.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.swipeRefresh.setVisibility(content && !empty ? View.VISIBLE : View.GONE);
        binding.swipeRefresh.setRefreshing(state.isRefreshing());

        if (status == MyEventsState.Status.ERROR) {
            binding.errorText.setText(HomeViewModel.errorMessageFor(state.getFailure()));
        }
        if (empty) {
            binding.emptyTitle.setText(past ? R.string.my_events_empty_past_title : R.string.my_events_empty_upcoming_title);
            binding.emptyBody.setText(past ? R.string.my_events_empty_past_body : R.string.my_events_empty_upcoming_body);
            binding.exploreButton.setVisibility(past ? View.GONE : View.VISIBLE);
        }
        adapter.submitList(state.getVisible());
    }
}
