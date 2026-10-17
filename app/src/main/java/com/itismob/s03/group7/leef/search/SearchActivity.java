package com.itismob.s03.group7.leef.search;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.databinding.ActivitySearchBinding;
import com.itismob.s03.group7.leef.details.EventDetailsActivity;
import com.itismob.s03.group7.leef.home.EventActions;
import com.itismob.s03.group7.leef.home.EventRowAdapter;
import com.itismob.s03.group7.leef.home.HomeViewModel;
import com.itismob.s03.group7.leef.model.Event;
import com.itismob.s03.group7.leef.ui.GapDecoration;
import com.itismob.s03.group7.leef.ui.LeefEdgeToEdge;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Screen 5 - Search & Filter Events: a keyword box, quick Date / Location / Category chips and a
 * Filters sheet. Results are the upcoming events that match, updated as the person types.
 */
public class SearchActivity extends AppCompatActivity {

    private static final String EXTRA_FOCUS_SEARCH = "focus_search";

    private ActivitySearchBinding binding;
    private SearchViewModel viewModel;
    private EventRowAdapter adapter;

    /**
     * @param focusSearch true to put the cursor in the search box and open the keyboard (used when
     *                    the person tapped Home's search bar)
     */
    @NonNull
    public static Intent createIntent(@NonNull Context context, boolean focusSearch) {
        return new Intent(context, SearchActivity.class).putExtra(EXTRA_FOCUS_SEARCH, focusSearch);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeefEdgeToEdge.enable(this);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets();

        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);

        EventActions actions = new EventActions() {
            @Override
            public void onEventClick(@NonNull Event event) {
                startActivity(EventDetailsActivity.createIntent(SearchActivity.this, event.getId()));
            }

            @Override
            public void onSaveClick(@NonNull Event event) {
                viewModel.toggleSaved(event);
            }
        };
        adapter = new EventRowAdapter(actions);
        binding.resultList.setLayoutManager(new LinearLayoutManager(this));
        binding.resultList.addItemDecoration(new GapDecoration(dp(12), RecyclerView.VERTICAL));
        binding.resultList.setAdapter(adapter);

        binding.backButton.setOnClickListener(v -> finish());
        binding.searchClear.setOnClickListener(v -> binding.searchField.setText(""));
        binding.searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                binding.searchClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                viewModel.setKeyword(s.toString());
            }
        });
        binding.searchField.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                return true;
            }
            return false;
        });

        binding.chipDate.setOnClickListener(v -> openFilters(FilterSheetFragment.SECTION_DATE));
        binding.chipLocation.setOnClickListener(v -> openFilters(FilterSheetFragment.SECTION_LOCATION));
        binding.chipCategory.setOnClickListener(v -> openFilters(FilterSheetFragment.SECTION_CATEGORY));
        binding.filterButton.setOnClickListener(v -> openFilters(FilterSheetFragment.SECTION_TOP));
        binding.retryButton.setOnClickListener(v -> viewModel.retry());
        binding.clearFiltersButton.setOnClickListener(v -> viewModel.clearSheetFilters());
        binding.swipeRefresh.setColorSchemeResources(R.color.leef_green_700);
        binding.swipeRefresh.setOnRefreshListener(viewModel::refresh);

        viewModel.getState().observe(this, this::render);
        viewModel.getMessage().observe(this, messageRes -> {
            if (messageRes != null) {
                Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show();
                viewModel.messageHandled();
            }
        });

        if (savedInstanceState == null && getIntent().getBooleanExtra(EXTRA_FOCUS_SEARCH, false)) {
            binding.searchField.requestFocus();
            binding.searchField.post(() -> WindowCompat.getInsetsController(getWindow(), binding.searchField)
                    .show(WindowInsetsCompat.Type.ime()));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Back from Event Details the heart may have changed.
        viewModel.reloadSaved();
    }

    private void openFilters(int section) {
        hideKeyboard();
        if (getSupportFragmentManager().findFragmentByTag(FilterSheetFragment.TAG) != null) {
            return;
        }
        viewModel.beginEditingFilters();
        FilterSheetFragment.newInstance(section).show(getSupportFragmentManager(), FilterSheetFragment.TAG);
    }

    private void render(@NonNull SearchState state) {
        SearchFilters filters = state.getFilters();
        renderQuickChips(filters);

        adapter.submitList(state.getResults());
        binding.swipeRefresh.setRefreshing(state.isRefreshing());

        SearchState.Status status = state.getStatus();
        boolean content = status == SearchState.Status.CONTENT;
        boolean hasResults = content && !state.getResults().isEmpty();
        boolean empty = content && state.getResults().isEmpty();

        binding.loading.setVisibility(status == SearchState.Status.LOADING ? View.VISIBLE : View.GONE);
        binding.errorGroup.setVisibility(status == SearchState.Status.ERROR ? View.VISIBLE : View.GONE);
        binding.emptyGroup.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.swipeRefresh.setVisibility(hasResults ? View.VISIBLE : View.INVISIBLE);

        if (status == SearchState.Status.ERROR) {
            binding.errorText.setText(HomeViewModel.errorMessageFor(state.getFailure()));
        }
        if (hasResults) {
            binding.resultCount.setText(getResources().getQuantityString(
                    R.plurals.search_result_count, state.getResults().size(), state.getResults().size()));
            binding.resultCount.setVisibility(View.VISIBLE);
        } else {
            binding.resultCount.setVisibility(View.INVISIBLE);
        }
        if (empty) {
            if (state.getTotalLoaded() == 0) {
                binding.emptyTitle.setText(R.string.home_empty_title);
                binding.emptyBody.setText(R.string.home_empty_body);
            } else {
                binding.emptyTitle.setText(R.string.search_empty_title);
                binding.emptyBody.setText(R.string.search_empty_body);
            }
            binding.clearFiltersButton.setVisibility(
                    state.getTotalLoaded() > 0 && filters.hasSheetFilters() ? View.VISIBLE : View.GONE);
        }
    }

    /** Date / Location / Category chips show what is picked, and turn mint while a filter is on. */
    private void renderQuickChips(@NonNull SearchFilters filters) {
        String dateText = dateChipText(filters);
        styleChip(binding.chipDate, dateText != null, dateText != null ? dateText : getString(R.string.search_chip_date));
        styleChip(binding.chipLocation, !filters.getCity().isEmpty(),
                filters.getCity().isEmpty() ? getString(R.string.search_chip_location) : filters.getCity());
        styleChip(binding.chipCategory, filters.getCategory() != null,
                filters.getCategory() == null ? getString(R.string.search_chip_category)
                        : getString(filters.getCategory().getBadgeRes()));

        boolean filtersOn = filters.hasSheetFilters();
        binding.filterDot.setVisibility(filtersOn ? View.VISIBLE : View.GONE);
        binding.filterButton.setContentDescription(getString(
                filtersOn ? R.string.search_filters_on_a11y : R.string.search_filters_a11y));
    }

    private void styleChip(@NonNull Chip chip, boolean active, @NonNull String text) {
        chip.setText(text);
        chip.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(this,
                active ? R.color.leef_green_100 : R.color.white)));
        chip.setChipStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this,
                active ? R.color.leef_green_700 : R.color.leef_outline)));
        chip.setTextColor(ContextCompat.getColor(this, active ? R.color.leef_green_900 : R.color.leef_ink));
    }

    /** "Nov 3 – Nov 10", "From Nov 3", "Until Nov 10", or null when no date is picked. */
    private String dateChipText(@NonNull SearchFilters filters) {
        long from = filters.getFromDayMillis();
        long to = filters.getToDayMillis();
        SimpleDateFormat format = new SimpleDateFormat("MMM d", Locale.getDefault());
        if (from > 0 && to > 0) {
            return getString(R.string.search_date_range, format.format(new Date(from)), format.format(new Date(to)));
        }
        if (from > 0) {
            return getString(R.string.search_date_from_only, format.format(new Date(from)));
        }
        if (to > 0) {
            return getString(R.string.search_date_to_only, format.format(new Date(to)));
        }
        return null;
    }

    private void hideKeyboard() {
        WindowCompat.getInsetsController(getWindow(), binding.searchField)
                .hide(WindowInsetsCompat.Type.ime());
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.searchRoot, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
