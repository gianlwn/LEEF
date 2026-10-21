package com.itismob.s03.group7.leef.home;

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
import androidx.recyclerview.widget.RecyclerView;

import com.itismob.s03.group7.leef.MainActivity;
import com.itismob.s03.group7.leef.MainViewModel;
import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.databinding.FragmentHomeBinding;
import com.itismob.s03.group7.leef.details.EventDetailsActivity;
import com.itismob.s03.group7.leef.model.Event;
import com.itismob.s03.group7.leef.model.EventCategory;
import com.itismob.s03.group7.leef.model.UserProfile;
import com.itismob.s03.group7.leef.search.SearchActivity;
import com.itismob.s03.group7.leef.ui.ComingSoon;
import com.itismob.s03.group7.leef.ui.GapDecoration;

import java.util.Calendar;
import java.util.Locale;

/**
 * Screen 4 - Home / Event Feed: greeting, search entry, category chips, Featured Events carousel
 * and the Upcoming Events list. Search, event details, notifications and the other tabs arrive with
 * their own screens, so those taps show a short "coming soon" note for now.
 */
public class HomeFragment extends Fragment {

    private static final int SIDE_PADDING_DP = 20;
    /** How much of the next featured card peeks in at the right edge. */
    private static final int PEEK_DP = 36;
    private static final int CARD_GAP_DP = 12;

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private MainViewModel mainViewModel;
    private CategoryChipAdapter chipAdapter;
    private FeaturedEventAdapter featuredAdapter;
    private EventRowAdapter rowAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        binding.greetingLine.setText(greetingRes());
        ViewCompat.setAccessibilityHeading(binding.featuredTitle, true);
        ViewCompat.setAccessibilityHeading(binding.upcomingTitle, true);

        EventActions actions = new EventActions() {
            @Override
            public void onEventClick(@NonNull Event event) {
                startActivity(EventDetailsActivity.createIntent(requireContext(), event.getId()));
            }

            @Override
            public void onSaveClick(@NonNull Event event) {
                viewModel.toggleSaved(event);
            }
        };

        chipAdapter = new CategoryChipAdapter(category -> viewModel.selectCategory(category));
        binding.categoryList.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        binding.categoryList.setAdapter(chipAdapter);

        featuredAdapter = new FeaturedEventAdapter(actions);
        featuredAdapter.setCardWidthPx(featuredCardWidthPx());
        binding.featuredList.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        binding.featuredList.addItemDecoration(new GapDecoration(dp(CARD_GAP_DP), RecyclerView.HORIZONTAL));
        binding.featuredList.setAdapter(featuredAdapter);

        rowAdapter = new EventRowAdapter(actions);
        binding.upcomingList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.upcomingList.addItemDecoration(new GapDecoration(dp(12), RecyclerView.VERTICAL));
        binding.upcomingList.setNestedScrollingEnabled(false);
        binding.upcomingList.setAdapter(rowAdapter);

        binding.searchBar.setOnClickListener(v -> startActivity(SearchActivity.createIntent(requireContext(), true)));
        binding.notificationsButton.setOnClickListener(v -> ComingSoon.show(requireContext(), R.string.feature_notifications));
        binding.avatar.setOnClickListener(v -> {
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).openProfileTab();
            }
        });
        binding.featuredSeeAll.setOnClickListener(v -> startActivity(SearchActivity.createIntent(requireContext(), false)));
        binding.upcomingSeeAll.setOnClickListener(v -> startActivity(SearchActivity.createIntent(requireContext(), false)));
        binding.retryButton.setOnClickListener(v -> viewModel.retry());
        binding.swipeRefresh.setColorSchemeResources(R.color.leef_green_700);
        binding.swipeRefresh.setOnRefreshListener(viewModel::refresh);

        viewModel.getState().observe(getViewLifecycleOwner(), this::render);
        viewModel.getMessage().observe(getViewLifecycleOwner(), messageRes -> {
            if (messageRes != null) {
                Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show();
                viewModel.messageHandled();
            }
        });
        mainViewModel.getProfile().observe(getViewLifecycleOwner(), this::showProfile);
        mainViewModel.getProfileFinished().observe(getViewLifecycleOwner(), finished -> showProfile(mainViewModel.getProfile().getValue()));
    }

    @Override
    public void onResume() {
        super.onResume();
        // Back from Event Details the heart may have changed.
        if (viewModel != null) {
            viewModel.reloadIfFeedChanged();
            viewModel.reloadSaved();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        // Coming back from the My Events tab the hearts may have changed.
        if (!hidden && viewModel != null) {
            viewModel.reloadIfFeedChanged();
            viewModel.reloadSaved();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void render(@NonNull HomeState state) {
        chipAdapter.setSelected(state.getSelectedCategory());
        binding.swipeRefresh.setRefreshing(state.isRefreshing());
        featuredAdapter.submitList(state.getFeatured());
        rowAdapter.submitList(state.getUpcoming());

        HomeState.Status status = state.getStatus();
        boolean content = status == HomeState.Status.CONTENT;
        binding.loading.setVisibility(status == HomeState.Status.LOADING ? View.VISIBLE : View.GONE);
        binding.errorGroup.setVisibility(status == HomeState.Status.ERROR ? View.VISIBLE : View.GONE);
        boolean empty = status == HomeState.Status.EMPTY || status == HomeState.Status.EMPTY_FILTER;
        binding.emptyGroup.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.featuredSection.setVisibility(content && !state.getFeatured().isEmpty() ? View.VISIBLE : View.GONE);
        binding.upcomingSection.setVisibility(content && !state.getUpcoming().isEmpty() ? View.VISIBLE : View.GONE);

        if (status == HomeState.Status.ERROR) {
            binding.errorText.setText(HomeViewModel.errorMessageFor(state.getFailure()));
        }
        if (status == HomeState.Status.EMPTY_FILTER && state.getSelectedCategory() != null) {
            String label = getString(state.getSelectedCategory().getLabelRes()).toLowerCase(Locale.getDefault());
            binding.emptyTitle.setText(getString(R.string.home_empty_filter_title, label));
            binding.emptyBody.setText(R.string.home_empty_filter_body);
        } else {
            binding.emptyTitle.setText(R.string.home_empty_title);
            binding.emptyBody.setText(R.string.home_empty_body);
        }
    }

    /** Shows the person's name and initials once the profile is known (or a plain welcome if it can't be loaded). */
    private void showProfile(@Nullable UserProfile profile) {
        if (binding == null) {
            return;
        }
        if (profile != null) {
            binding.nameLine.setText(getString(R.string.home_name_format, profile.getFirstName()));
            binding.nameLine.setVisibility(View.VISIBLE);
            binding.avatarInitials.setText(initialsOf(profile.getFullName()));
            binding.avatarInitials.setVisibility(View.VISIBLE);
            binding.avatarIcon.setVisibility(View.GONE);
        } else if (Boolean.TRUE.equals(mainViewModel.getProfileFinished().getValue())) {
            binding.nameLine.setText(R.string.home_name_unknown);
            binding.nameLine.setVisibility(View.VISIBLE);
        }
    }

    private static int greetingRes() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) {
            return R.string.home_greeting_morning;
        }
        return hour < 18 ? R.string.home_greeting_afternoon : R.string.home_greeting_evening;
    }

    /** "Alex Santos" becomes "AS"; a single name gives one letter. */
    @NonNull
    public static String initialsOf(@NonNull String fullName) {
        String[] words = fullName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        if (words.length > 0 && !words[0].isEmpty()) {
            initials.appendCodePoint(words[0].codePointAt(0));
        }
        if (words.length > 1 && !words[words.length - 1].isEmpty()) {
            initials.appendCodePoint(words[words.length - 1].codePointAt(0));
        }
        return initials.toString().toUpperCase(Locale.getDefault());
    }

    private int featuredCardWidthPx() {
        return getResources().getDisplayMetrics().widthPixels - dp(SIDE_PADDING_DP * 2 + PEEK_DP);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
