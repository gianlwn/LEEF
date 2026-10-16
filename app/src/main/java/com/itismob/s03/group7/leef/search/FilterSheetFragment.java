package com.itismob.s03.group7.leef.search;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.databinding.SheetFiltersBinding;
import com.itismob.s03.group7.leef.model.EventAudience;
import com.itismob.s03.group7.leef.model.EventCategory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * The Filters sheet (Screen 5): category, date range, location, audience / age group and price. It
 * edits the view model's draft; "Show N Events" applies it, and the number on the button is how many
 * events would match right now.
 */
public class FilterSheetFragment extends BottomSheetDialogFragment {

    public static final String TAG = "filters";

    /** Which part of the sheet to bring into view when it opens. */
    public static final int SECTION_TOP = 0;
    public static final int SECTION_CATEGORY = 1;
    public static final int SECTION_DATE = 2;
    public static final int SECTION_LOCATION = 3;

    private static final String ARG_SECTION = "section";
    private static final String DATE_PICKER_TAG = "filters_date";

    private SheetFiltersBinding binding;
    private SearchViewModel viewModel;
    /** True while the sheet is drawing the draft into its views, so those changes aren't read back as taps. */
    private boolean rendering;

    private final List<Chip> categoryChips = new ArrayList<>();
    private final List<Chip> audienceChips = new ArrayList<>();
    private final List<Chip> priceChips = new ArrayList<>();

    @NonNull
    public static FilterSheetFragment newInstance(int section) {
        FilterSheetFragment fragment = new FilterSheetFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION, section);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getTheme() {
        return R.style.ThemeOverlay_LEEF_BottomSheetDialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = SheetFiltersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(SearchViewModel.class);

        ViewCompat.setOnApplyWindowInsetsListener(binding.sheetRoot, (root, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            root.setPadding(0, 0, 0, Math.max(bars.bottom, ime.bottom));
            return windowInsets;
        });

        buildCategoryChips();
        buildAudienceChips();
        buildPriceChips();

        binding.categoryGroup.setOnCheckedStateChangeListener((group, ids) -> {
            if (!rendering && !ids.isEmpty()) {
                viewModel.updateDraft(draft().withCategory((EventCategory) tagOf(group, ids.get(0))));
            }
        });
        binding.audienceGroup.setOnCheckedStateChangeListener((group, ids) -> {
            if (!rendering && !ids.isEmpty()) {
                viewModel.updateDraft(draft().withAudience((EventAudience) tagOf(group, ids.get(0))));
            }
        });
        binding.priceGroup.setOnCheckedStateChangeListener((group, ids) -> {
            if (!rendering && !ids.isEmpty()) {
                viewModel.updateDraft(draft().withPrice((SearchFilters.Price) tagOf(group, ids.get(0))));
            }
        });

        binding.fromField.setOnClickListener(v -> pickDate(true));
        binding.toField.setOnClickListener(v -> pickDate(false));
        binding.fromIcon.setOnClickListener(v -> {
            if (draft().getFromDayMillis() > 0) {
                viewModel.updateDraft(draft().withFromDay(0));
            } else {
                pickDate(true);
            }
        });
        binding.toIcon.setOnClickListener(v -> {
            if (draft().getToDayMillis() > 0) {
                viewModel.updateDraft(draft().withToDay(0));
            } else {
                pickDate(false);
            }
        });

        binding.locationField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!rendering) {
                    viewModel.updateDraft(draft().withCity(s.toString()));
                }
            }
        });

        binding.clearAll.setOnClickListener(v -> viewModel.updateDraft(draft().withoutSheetFilters()));
        binding.showButton.setOnClickListener(v -> {
            viewModel.applyDraft();
            dismiss();
        });

        viewModel.getDraft().observe(getViewLifecycleOwner(), this::render);

        int section = requireArguments().getInt(ARG_SECTION, SECTION_TOP);
        if (section != SECTION_TOP && savedInstanceState == null) {
            View target = section == SECTION_CATEGORY ? binding.categoryTitle
                    : section == SECTION_DATE ? binding.dateTitle : binding.locationTitle;
            binding.scroll.post(() -> {
                if (binding != null) {
                    binding.scroll.smoothScrollTo(0, Math.max(0, target.getTop() - dp(8)));
                }
            });
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Open tall and fully expanded, like the wireframe, instead of peeking.
        Dialog dialog = getDialog();
        if (dialog instanceof BottomSheetDialog) {
            BottomSheetDialog sheetDialog = (BottomSheetDialog) dialog;
            View sheet = sheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                int screenHeight = getResources().getDisplayMetrics().heightPixels;
                sheet.getLayoutParams().height = (int) (screenHeight * 0.9f);
                sheet.requestLayout();
            }
            BottomSheetBehavior<FrameLayout> behavior = sheetDialog.getBehavior();
            behavior.setSkipCollapsed(true);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        categoryChips.clear();
        audienceChips.clear();
        priceChips.clear();
        binding = null;
    }

    // ---------------------------------------------------------------------------------------
    // Building the chips (these are fixed MCO1 option lists, not database records)
    // ---------------------------------------------------------------------------------------

    private void buildCategoryChips() {
        addChip(binding.categoryGroup, categoryChips, R.string.filters_all, null);
        for (EventCategory category : EventCategory.values()) {
            addChip(binding.categoryGroup, categoryChips, category.getBadgeRes(), category);
        }
    }

    private void buildAudienceChips() {
        addChip(binding.audienceGroup, audienceChips, R.string.filters_all, null);
        for (EventAudience audience : EventAudience.values()) {
            addChip(binding.audienceGroup, audienceChips, audience.getLabelRes(), audience);
        }
    }

    private void buildPriceChips() {
        addChip(binding.priceGroup, priceChips, R.string.filters_all, SearchFilters.Price.ALL);
        addChip(binding.priceGroup, priceChips, R.string.filters_free, SearchFilters.Price.FREE);
        addChip(binding.priceGroup, priceChips, R.string.filters_paid, SearchFilters.Price.PAID);
    }

    private void addChip(@NonNull ChipGroup group, @NonNull List<Chip> list, @StringRes int label,
                         @Nullable Object value) {
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_filter_chip, group, false);
        chip.setId(View.generateViewId());
        chip.setText(label);
        chip.setTag(value);
        group.addView(chip);
        list.add(chip);
    }

    /** The value (category, audience or price) behind the chip with this id; null is the "All" chip. */
    @Nullable
    private static Object tagOf(@NonNull ChipGroup group, int chipId) {
        View chip = group.findViewById(chipId);
        return chip == null ? null : chip.getTag();
    }

    // ---------------------------------------------------------------------------------------
    // Drawing the draft
    // ---------------------------------------------------------------------------------------

    @NonNull
    private SearchFilters draft() {
        SearchFilters current = viewModel.getDraft().getValue();
        return current == null ? SearchFilters.NONE : current;
    }

    private void render(@NonNull SearchFilters filters) {
        rendering = true;
        checkChip(categoryChips, filters.getCategory());
        checkChip(audienceChips, filters.getAudience());
        checkChip(priceChips, filters.getPrice());
        renderDate(binding.fromText, binding.fromIcon, filters.getFromDayMillis(), R.string.filters_from,
                R.string.filters_clear_date_a11y);
        renderDate(binding.toText, binding.toIcon, filters.getToDayMillis(), R.string.filters_to,
                R.string.filters_clear_date_a11y);
        if (!binding.locationField.getText().toString().equals(filters.getCity())) {
            binding.locationField.setText(filters.getCity());
            binding.locationField.setSelection(binding.locationField.length());
        }
        rendering = false;

        binding.clearAll.setVisibility(filters.hasSheetFilters() ? View.VISIBLE : View.INVISIBLE);
        renderShowButton(filters);
    }

    private static void checkChip(@NonNull List<Chip> chips, @Nullable Object value) {
        for (Chip chip : chips) {
            Object tag = chip.getTag();
            chip.setChecked(tag == null ? value == null : tag.equals(value));
        }
    }

    private void renderDate(@NonNull android.widget.TextView text, @NonNull android.widget.ImageView icon,
                            long dayMillis, @StringRes int hint, @StringRes int clearDescription) {
        if (dayMillis > 0) {
            text.setText(new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(new Date(dayMillis)));
            text.setTextColor(ContextCompat.getColor(requireContext(), R.color.leef_ink));
            icon.setImageResource(R.drawable.ic_close);
            icon.setContentDescription(getString(clearDescription));
        } else {
            text.setText(hint);
            text.setTextColor(ContextCompat.getColor(requireContext(), R.color.leef_gray_600));
            icon.setImageResource(R.drawable.ic_event_date);
            icon.setContentDescription(null);
        }
    }

    private void renderShowButton(@NonNull SearchFilters filters) {
        if (!viewModel.isLoaded()) {
            binding.showButton.setText(R.string.filters_show_generic);
            return;
        }
        int count = viewModel.countMatching(filters);
        if (count == 0) {
            binding.showButton.setText(R.string.filters_show_none);
        } else {
            binding.showButton.setText(getResources().getQuantityString(R.plurals.filters_show_events, count, count));
        }
    }

    // ---------------------------------------------------------------------------------------
    // Date picking
    // ---------------------------------------------------------------------------------------

    private void pickDate(boolean isFrom) {
        if (getParentFragmentManager().findFragmentByTag(DATE_PICKER_TAG) != null) {
            return;
        }
        SearchFilters current = draft();
        long chosen = isFrom ? current.getFromDayMillis() : current.getToDayMillis();
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(isFrom ? R.string.filters_pick_from : R.string.filters_pick_to)
                .setSelection(chosen > 0
                        ? SearchFilters.localDayToUtcPick(chosen)
                        : MaterialDatePicker.todayInUtcMilliseconds())
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            long day = SearchFilters.utcPickToLocalDay(selection);
            SearchFilters now = draft();
            SearchFilters next;
            if (isFrom) {
                next = now.withFromDay(day);
                if (now.getToDayMillis() > 0 && day > now.getToDayMillis()) {
                    next = next.withToDay(day);
                }
            } else {
                next = now.withToDay(day);
                if (now.getFromDayMillis() > 0 && day < now.getFromDayMillis()) {
                    next = next.withFromDay(day);
                }
            }
            viewModel.updateDraft(next);
        });
        picker.show(getParentFragmentManager(), DATE_PICKER_TAG);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
