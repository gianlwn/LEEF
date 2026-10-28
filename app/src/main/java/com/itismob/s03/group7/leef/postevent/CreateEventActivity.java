package com.itismob.s03.group7.leef.postevent;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.chip.Chip;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.databinding.ActivityCreateEventBinding;
import com.itismob.s03.group7.leef.databinding.ItemFileEditBinding;
import com.itismob.s03.group7.leef.details.EventDetailsActivity;
import com.itismob.s03.group7.leef.model.Event;
import com.itismob.s03.group7.leef.model.EventAttachment;
import com.itismob.s03.group7.leef.model.EventAudience;
import com.itismob.s03.group7.leef.model.EventCategory;
import com.itismob.s03.group7.leef.search.SearchFilters;
import com.itismob.s03.group7.leef.ui.ButtonSpinner;
import com.itismob.s03.group7.leef.ui.AttachmentLinkDialog;
import com.itismob.s03.group7.leef.ui.ImageLinkDialog;
import com.itismob.s03.group7.leef.ui.LeefEdgeToEdge;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Screen 11 - Create / Post Event. Organizers fill in the event (title, description, category, date and
 * time, location, audience, tags, pricing and a photo link) and publish it; it then shows on Home and
 * Search for everyone. Only accounts with the Organizer role get the form.
 */
public class CreateEventActivity extends AppCompatActivity {

    private static final String EXTRA_EDIT_EVENT_ID = "edit_event_id";

    private ActivityCreateEventBinding binding;
    private CreateEventViewModel viewModel;
    private final EventCategory[] categories = EventCategory.values();
    private boolean restoring;
    private boolean lastPosting;
    private boolean lastEditing;
    /** Last state shown, so a re-render (rotation, error text) doesn't reset the scroll or the fields. */
    @Nullable
    private CreateEventState.Status shownStatus;

    @NonNull
    public static Intent createIntent(@NonNull Context context) {
        return new Intent(context, CreateEventActivity.class);
    }

    /** Opens the form filled in with an event the organizer already posted, to change it. */
    @NonNull
    public static Intent editIntent(@NonNull Context context, @NonNull String eventId) {
        return new Intent(context, CreateEventActivity.class).putExtra(EXTRA_EDIT_EVENT_ID, eventId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeefEdgeToEdge.enable(this);
        binding = ActivityCreateEventBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets();

        String editEventId = getIntent().getStringExtra(EXTRA_EDIT_EVENT_ID);
        if (editEventId != null && editEventId.isEmpty()) {
            editEventId = null;
        }
        viewModel = new ViewModelProvider(this, new CreateEventViewModel.Factory(editEventId))
                .get(CreateEventViewModel.class);
        if (viewModel.isEditing()) {
            binding.screenTitle.setText(R.string.edit_title);
        }

        binding.backButton.setOnClickListener(v -> finish());
        binding.stateButton.setOnClickListener(v -> {
            CreateEventState current = viewModel.getState().getValue();
            if (current != null && current.getStatus() == CreateEventState.Status.ERROR) {
                viewModel.retry();
            } else {
                finish();
            }
        });
        ViewCompat.setAccessibilityHeading(binding.sectionInfo, true);
        ViewCompat.setAccessibilityHeading(binding.sectionAudience, true);
        ViewCompat.setAccessibilityHeading(binding.sectionTags, true);
        ViewCompat.setAccessibilityHeading(binding.sectionPricing, true);
        ViewCompat.setAccessibilityHeading(binding.sectionPhoto, true);

        setUpCategory();
        setUpDates();
        setUpAudience();
        setUpTags();
        setUpPricing();
        setUpCover();
        setUpFiles();
        clearErrorsWhileTyping();
        binding.postButton.setOnClickListener(v -> post());

        restoreFromViewModel();

        viewModel.getState().observe(this, this::render);
        viewModel.getErrors().observe(this, this::showErrors);
        viewModel.getMessage().observe(this, messageRes -> {
            if (messageRes != null) {
                Toast.makeText(this, messageRes, Toast.LENGTH_LONG).show();
                viewModel.messageHandled();
            }
        });
        viewModel.getPosted().observe(this, eventId -> {
            if (eventId != null) {
                viewModel.postedHandled();
                if (viewModel.isEditing()) {
                    // Back to the Manage screen, which reloads and shows the new details.
                    Toast.makeText(this, R.string.edit_saved, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, R.string.create_posted, Toast.LENGTH_LONG).show();
                    startActivity(EventDetailsActivity.createIntent(this, eventId));
                }
                finish();
            }
        });
    }

    // ---- set up -----------------------------------------------------------------------------

    private void setUpCategory() {
        String[] labels = new String[categories.length];
        for (int i = 0; i < categories.length; i++) {
            labels[i] = getString(categories[i].getBadgeRes());
        }
        binding.categoryInput.setAdapter(new ArrayAdapter<>(this, R.layout.item_dropdown_option, labels));
        binding.categoryInput.setOnItemClickListener((parent, view, position, id) ->
                viewModel.setCategory(categories[position]));
    }

    private void setUpDates() {
        binding.startInput.setOnClickListener(v -> pickDateTime(false));
        binding.startLayout.setEndIconOnClickListener(v -> pickDateTime(false));
        binding.endInput.setOnClickListener(v -> pickDateTime(true));
        binding.endLayout.setEndIconOnClickListener(v -> {
            if (viewModel.getEndsAt() > 0) {
                viewModel.setEndsAt(0);
                showEnd();
            } else {
                pickDateTime(true);
            }
        });
        binding.earlyUntilInput.setOnClickListener(v -> pickEarlyBirdDay());
        binding.earlyUntilLayout.setEndIconOnClickListener(v -> pickEarlyBirdDay());
    }

    private void setUpAudience() {
        binding.audienceGroup.removeAllViews();
        for (EventAudience audience : EventAudience.values()) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_filter_chip, binding.audienceGroup, false);
            chip.setId(View.generateViewId());
            chip.setText(audience.getLabelRes());
            chip.setCheckable(true);
            chip.setChecked(viewModel.getAudiences().contains(audience));
            chip.setOnCheckedChangeListener((button, checked) -> viewModel.setAudience(audience, checked));
            binding.audienceGroup.addView(chip);
        }
    }

    private void setUpTags() {
        binding.tagLayout.setEndIconOnClickListener(v -> addTagFromField());
        binding.tagInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addTagFromField();
                return true;
            }
            return false;
        });
        // A comma ends the tag, like pressing the plus button.
        binding.tagInput.addTextChangedListener(new SimpleWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().contains(",")) {
                    addTagFromField();
                }
            }
        });
    }

    private void setUpPricing() {
        binding.pricingGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (restoring) {
                return;
            }
            viewModel.setPaid(checkedIds.contains(R.id.chipPaid));
            showPricingBlocks();
        });
        binding.earlySwitch.setOnCheckedChangeListener((button, checked) -> {
            if (!restoring) {
                viewModel.setEarlyBirdOn(checked);
                showPricingBlocks();
            }
        });
        binding.groupSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (!restoring) {
                viewModel.setGroupOn(checked);
                showPricingBlocks();
            }
        });
    }

    private void setUpCover() {
        binding.coverCard.setOnClickListener(v -> askForImageLink());
        binding.changeCoverButton.setOnClickListener(v -> askForImageLink());
        binding.removeCoverButton.setOnClickListener(v -> {
            viewModel.setCoverUrl(null);
            showCover();
        });
    }

    private void setUpFiles() {
        binding.addFileButton.setOnClickListener(v -> AttachmentLinkDialog.show(this, (name, url) -> {
            switch (viewModel.addAttachment(name, url)) {
                case DUPLICATE:
                    Toast.makeText(this, R.string.create_files_duplicate, Toast.LENGTH_SHORT).show();
                    break;
                case FULL:
                    Toast.makeText(this, R.string.create_files_full, Toast.LENGTH_SHORT).show();
                    break;
                default:
                    showFiles();
                    break;
            }
        }));
    }

    /** The linked files under "Flyers and files", each with a remove button; the add button hides at the limit. */
    private void showFiles() {
        binding.fileList.removeAllViews();
        for (EventAttachment attachment : viewModel.getAttachments()) {
            ItemFileEditBinding row = ItemFileEditBinding.inflate(getLayoutInflater(), binding.fileList, false);
            row.fileName.setText(attachment.getName());
            row.fileUrl.setText(attachment.getUrl());
            row.removeFile.setContentDescription(getString(R.string.create_files_remove_a11y, attachment.getName()));
            row.removeFile.setOnClickListener(v -> {
                viewModel.removeAttachment(attachment);
                showFiles();
            });
            binding.fileList.addView(row.getRoot());
        }
        binding.addFileButton.setVisibility(
                viewModel.getAttachments().size() >= EventAttachment.MAX_COUNT ? View.GONE : View.VISIBLE);
    }

    /** Typing in a field takes its error away. */
    private void clearErrorsWhileTyping() {
        clearOnType(binding.titleInput, binding.titleLayout);
        clearOnType(binding.venueInput, binding.venueLayout);
        clearOnType(binding.priceInput, binding.priceLayout);
        clearOnType(binding.earlyPriceInput, binding.earlyPriceLayout);
        clearOnType(binding.groupPriceInput, binding.groupPriceLayout);
    }

    private static void clearOnType(@NonNull android.widget.EditText input, @NonNull TextInputLayout layout) {
        input.addTextChangedListener(new SimpleWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (layout.getError() != null) {
                    layout.setError(null);
                }
            }
        });
    }

    /** Puts back what the view model remembers (after a rotation); the text fields restore themselves. */
    private void restoreFromViewModel() {
        restoring = true;
        EventCategory category = viewModel.getCategory();
        if (category != null) {
            // filter=false keeps the whole list available when the dropdown is opened again.
            binding.categoryInput.setText(getString(category.getBadgeRes()), false);
        }
        showStart();
        showEnd();
        showEarlyUntil();
        binding.pricingGroup.check(viewModel.isPaid() ? R.id.chipPaid : R.id.chipFree);
        binding.earlySwitch.setChecked(viewModel.isEarlyBirdOn());
        binding.groupSwitch.setChecked(viewModel.isGroupOn());
        showPricingBlocks();
        showTags();
        showCover();
        showFiles();
        restoring = false;
    }

    // ---- showing state ----------------------------------------------------------------------

    private void render(@NonNull CreateEventState state) {
        CreateEventState.Status status = state.getStatus();
        boolean form = status == CreateEventState.Status.FORM;
        binding.formScroll.setVisibility(form ? View.VISIBLE : View.GONE);
        binding.bottomBar.setVisibility(form ? View.VISIBLE : View.GONE);
        binding.statePanel.setVisibility(form ? View.GONE : View.VISIBLE);
        binding.loadingSpinner.setVisibility(status == CreateEventState.Status.LOADING ? View.VISIBLE : View.GONE);
        boolean problem = status == CreateEventState.Status.ERROR || status == CreateEventState.Status.FORBIDDEN
                || status == CreateEventState.Status.LOCKED;
        binding.stateIcon.setVisibility(problem ? View.VISIBLE : View.GONE);
        binding.stateBody.setVisibility(problem ? View.VISIBLE : View.GONE);
        binding.stateButton.setVisibility(problem ? View.VISIBLE : View.GONE);
        binding.stateTitle.setVisibility(status == CreateEventState.Status.FORBIDDEN
                || status == CreateEventState.Status.LOCKED ? View.VISIBLE : View.GONE);
        if (status == CreateEventState.Status.LOCKED) {
            binding.stateIcon.setImageResource(R.drawable.ic_field_lock);
            binding.stateTitle.setText(R.string.edit_locked_title);
            binding.stateBody.setText(state.getErrorRes());
            binding.stateButton.setText(R.string.create_forbidden_back);
        } else if (status == CreateEventState.Status.FORBIDDEN) {
            binding.stateTitle.setText(R.string.create_forbidden_title);
            binding.stateIcon.setImageResource(R.drawable.ic_field_role);
            binding.stateBody.setText(R.string.create_forbidden_body);
            binding.stateButton.setText(R.string.create_forbidden_back);
        } else if (status == CreateEventState.Status.ERROR) {
            binding.stateIcon.setImageResource(R.drawable.ic_state_cloud_off);
            binding.stateBody.setText(state.getErrorRes());
            binding.stateButton.setText(R.string.home_retry);
        }

        if (form) {
            if (state.isEditing()) {
                Event prefill = viewModel.consumePrefill();
                if (prefill != null) {
                    applyPrefill(prefill);
                }
            }
            binding.postingAs.setText(getString(R.string.create_posting_as, state.getOrganizerName()));
            binding.postingAs.setVisibility(state.getOrganizerName().isEmpty() ? View.GONE : View.VISIBLE);
            lastEditing = state.isEditing();
            showPosting(state.isPosting());
            applyPriceLock(state);
        }
        shownStatus = status;
    }

    /** Edit mode: puts the event's text into the fields and shows the picked values. */
    private void applyPrefill(@NonNull Event event) {
        binding.titleInput.setText(event.getTitle());
        binding.descriptionInput.setText(event.getDescription());
        binding.venueInput.setText(event.getVenue());
        binding.cityInput.setText(event.getCity());
        if (!event.isFree()) {
            binding.priceInput.setText(plainPrice(event.getPrice()));
        }
        if (event.hasEarlyBird()) {
            binding.earlyPriceInput.setText(plainPrice(event.getEarlyBirdPrice()));
        }
        if (event.hasGroupPrice()) {
            binding.groupPriceInput.setText(plainPrice(event.getGroupPrice()));
        }
        setUpAudience();
        restoreFromViewModel();
    }

    /** 350.0 -> "350", 350.5 -> "350.50". */
    @NonNull
    private static String plainPrice(double price) {
        return price == Math.rint(price) ? String.valueOf((long) price) : String.format(Locale.US, "%.2f", price);
    }

    /** Edit mode with people registered: the prices can be read but not changed. */
    private void applyPriceLock(@NonNull CreateEventState state) {
        boolean locked = state.isPricesLocked();
        binding.priceLockedNote.setVisibility(locked ? View.VISIBLE : View.GONE);
        if (!locked) {
            return;
        }
        binding.priceLockedNote.setText(state.getRegisteredPeople() < 0
                ? getString(R.string.edit_prices_locked_unknown)
                : getResources().getQuantityString(R.plurals.edit_prices_locked,
                state.getRegisteredPeople(), state.getRegisteredPeople()));
        setFormEnabled(binding.pricingGroup, false);
        setFormEnabled(binding.priceLayout, false);
        binding.earlySwitch.setEnabled(false);
        setFormEnabled(binding.earlyPriceLayout, false);
        setFormEnabled(binding.earlyUntilLayout, false);
        binding.groupSwitch.setEnabled(false);
        setFormEnabled(binding.groupPriceLayout, false);
    }

    private void showPosting(boolean posting) {
        if (posting == lastPosting && shownStatus == CreateEventState.Status.FORM) {
            return;
        }
        lastPosting = posting;
        binding.postButton.setEnabled(!posting);
        binding.postButton.setText(posting ? "" : getString(lastEditing
                ? R.string.edit_save_button : R.string.create_post_button));
        binding.postButton.setIcon(posting ? ButtonSpinner.create(this) : null);
        binding.formScroll.setAlpha(posting ? 0.6f : 1f);
        setFormEnabled(binding.formContent, !posting);
    }

    private static void setFormEnabled(@NonNull View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                setFormEnabled(group.getChildAt(i), enabled);
            }
        }
    }

    private void showErrors(@Nullable EventFormErrors errors) {
        if (errors == null) {
            return;
        }
        setError(binding.titleLayout, errors.title);
        setError(binding.categoryLayout, errors.category);
        setError(binding.startLayout, errors.start);
        setError(binding.endLayout, errors.end);
        setError(binding.venueLayout, errors.venue);
        setError(binding.priceLayout, errors.price);
        setError(binding.earlyPriceLayout, errors.earlyPrice);
        setError(binding.earlyUntilLayout, errors.earlyUntil);
        setError(binding.groupPriceLayout, errors.groupPrice);
        if (errors.hasAny()) {
            scrollToFirstError();
        }
    }

    private void setError(@NonNull TextInputLayout layout, @StringRes int messageRes) {
        layout.setError(messageRes == 0 ? null : getString(messageRes));
    }

    /** Brings the topmost field with a problem into view. */
    private void scrollToFirstError() {
        TextInputLayout[] order = {binding.titleLayout, binding.categoryLayout, binding.startLayout,
                binding.endLayout, binding.venueLayout, binding.priceLayout, binding.earlyPriceLayout,
                binding.earlyUntilLayout, binding.groupPriceLayout};
        for (TextInputLayout layout : order) {
            if (layout.getError() != null && layout.isShown()) {
                int top = layout.getTop();
                View parent = (View) layout.getParent();
                while (parent != null && parent != binding.formContent) {
                    top += parent.getTop();
                    parent = (View) parent.getParent();
                }
                binding.formScroll.smoothScrollTo(0, Math.max(0, top - dp(72)));
                return;
            }
        }
    }

    private void showStart() {
        long start = viewModel.getStartsAt();
        binding.startInput.setText(start > 0 ? formatDateTime(start) : "");
    }

    private void showEnd() {
        long end = viewModel.getEndsAt();
        binding.endInput.setText(end > 0 ? formatDateTime(end) : "");
        binding.endLayout.setEndIconDrawable(end > 0 ? R.drawable.ic_close : R.drawable.ic_event_date);
        binding.endLayout.setEndIconContentDescription(end > 0 ? getString(R.string.create_clear_end_a11y) : null);
    }

    private void showEarlyUntil() {
        long until = viewModel.getEarlyBirdUntil();
        binding.earlyUntilInput.setText(until > 0 ? formatDate(until) : "");
    }

    private void showPricingBlocks() {
        binding.paidBlock.setVisibility(viewModel.isPaid() ? View.VISIBLE : View.GONE);
        binding.earlyBlock.setVisibility(viewModel.isEarlyBirdOn() ? View.VISIBLE : View.GONE);
        binding.groupBlock.setVisibility(viewModel.isGroupOn() ? View.VISIBLE : View.GONE);
    }

    private void showTags() {
        binding.tagGroup.removeAllViews();
        for (String tag : viewModel.getTags()) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_tag_chip, binding.tagGroup, false);
            chip.setText(tag);
            chip.setCloseIconVisible(true);
            chip.setCloseIconContentDescription(getString(R.string.create_remove_tag_a11y, tag));
            chip.setOnCloseIconClickListener(v -> {
                viewModel.removeTag(tag);
                showTags();
            });
            binding.tagGroup.addView(chip);
        }
        binding.tagGroup.setVisibility(viewModel.getTags().isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void showCover() {
        String url = viewModel.getCoverUrl();
        boolean has = url != null && !url.isEmpty();
        binding.coverEmpty.setVisibility(has ? View.GONE : View.VISIBLE);
        binding.coverPreview.setVisibility(has ? View.VISIBLE : View.GONE);
        binding.coverActions.setVisibility(has ? View.VISIBLE : View.GONE);
        binding.coverError.setVisibility(View.GONE);
        if (!has) {
            Glide.with(binding.coverPreview).clear(binding.coverPreview);
            return;
        }
        Glide.with(binding.coverPreview)
                .load(url)
                .centerCrop()
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, @Nullable Object model,
                                                @NonNull Target<Drawable> target, boolean isFirstResource) {
                        // The link isn't a picture (or can't be reached): drop it and say so.
                        if (url.equals(viewModel.getCoverUrl())) {
                            viewModel.setCoverUrl(null);
                            showCover();
                            binding.coverError.setText(R.string.create_photo_failed);
                            binding.coverError.setVisibility(View.VISIBLE);
                        }
                        return true;
                    }

                    @Override
                    public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model,
                                                   Target<Drawable> target, @NonNull DataSource dataSource,
                                                   boolean isFirstResource) {
                        return false;
                    }
                })
                .into(binding.coverPreview);
    }

    // ---- actions ----------------------------------------------------------------------------

    private void post() {
        hideKeyboard();
        viewModel.post(new CreateEventViewModel.FormText(
                text(binding.titleInput), text(binding.descriptionInput), text(binding.venueInput),
                text(binding.cityInput), text(binding.priceInput), text(binding.earlyPriceInput),
                text(binding.groupPriceInput)));
    }

    private void addTagFromField() {
        String typed = text(binding.tagInput).replace(",", " ");
        CreateEventViewModel.TagResult result = viewModel.addTag(typed);
        switch (result) {
            case ADDED:
                binding.tagInput.setText("");
                binding.tagLayout.setError(null);
                showTags();
                break;
            case DUPLICATE:
                binding.tagLayout.setError(getString(R.string.create_tag_duplicate));
                break;
            case FULL:
                binding.tagLayout.setError(getString(R.string.create_tag_full));
                break;
            default:
                binding.tagInput.setText("");
                break;
        }
    }

    /** Date, then time; {@code forEnd} picks the end instead of the start. */
    private void pickDateTime(boolean forEnd) {
        long current = forEnd ? viewModel.getEndsAt() : viewModel.getStartsAt();
        long start = viewModel.getStartsAt();
        long earliestDay = forEnd && start > 0 ? startOfDay(start) : startOfDay(System.currentTimeMillis());
        long initial = current > 0 ? current : (forEnd && start > 0 ? start : earliestDay);

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(forEnd ? R.string.create_date_end : R.string.create_date_start)
                .setSelection(SearchFilters.localDayToUtcPick(initial))
                .setCalendarConstraints(new CalendarConstraints.Builder()
                        .setStart(SearchFilters.localDayToUtcPick(earliestDay))
                        .setValidator(DateValidatorPointForward.from(SearchFilters.localDayToUtcPick(earliestDay)))
                        .build())
                .build();
        datePicker.addOnPositiveButtonClickListener(selection -> {
            long day = SearchFilters.utcPickToLocalDay(selection);
            pickTime(day, forEnd, current);
        });
        datePicker.show(getSupportFragmentManager(), forEnd ? "end_date" : "start_date");
    }

    private void pickTime(long dayMillis, boolean forEnd, long current) {
        Calendar defaultTime = Calendar.getInstance();
        if (current > 0) {
            defaultTime.setTimeInMillis(current);
        } else if (forEnd && viewModel.getStartsAt() > 0) {
            defaultTime.setTimeInMillis(viewModel.getStartsAt() + 2 * 60 * 60 * 1000L);
        } else {
            defaultTime.set(Calendar.HOUR_OF_DAY, 9);
            defaultTime.set(Calendar.MINUTE, 0);
        }
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(defaultTime.get(Calendar.HOUR_OF_DAY))
                .setMinute(defaultTime.get(Calendar.MINUTE))
                .setTitleText(forEnd ? R.string.create_time_end : R.string.create_time_start)
                .build();
        timePicker.addOnPositiveButtonClickListener(v -> {
            Calendar chosen = Calendar.getInstance();
            chosen.setTimeInMillis(dayMillis);
            chosen.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
            chosen.set(Calendar.MINUTE, timePicker.getMinute());
            chosen.set(Calendar.SECOND, 0);
            chosen.set(Calendar.MILLISECOND, 0);
            if (forEnd) {
                viewModel.setEndsAt(chosen.getTimeInMillis());
                showEnd();
            } else {
                viewModel.setStartsAt(chosen.getTimeInMillis());
                showStart();
            }
        });
        timePicker.show(getSupportFragmentManager(), forEnd ? "end_time" : "start_time");
    }

    private void pickEarlyBirdDay() {
        long current = viewModel.getEarlyBirdUntil() > 0 ? viewModel.getEarlyBirdUntil() : System.currentTimeMillis();
        long today = startOfDay(System.currentTimeMillis());
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.create_date_early)
                .setSelection(SearchFilters.localDayToUtcPick(Math.max(current, today)))
                .setCalendarConstraints(new CalendarConstraints.Builder()
                        .setStart(SearchFilters.localDayToUtcPick(today))
                        .setValidator(DateValidatorPointForward.from(SearchFilters.localDayToUtcPick(today)))
                        .build())
                .build();
        datePicker.addOnPositiveButtonClickListener(selection -> {
            viewModel.setEarlyBirdUntilDay(SearchFilters.utcPickToLocalDay(selection));
            showEarlyUntil();
        });
        datePicker.show(getSupportFragmentManager(), "early_date");
    }

    private void askForImageLink() {
        ImageLinkDialog.show(this, viewModel.getCoverUrl(), link -> {
            viewModel.setCoverUrl(link);
            showCover();
        });
    }

    // ---- helpers ----------------------------------------------------------------------------

    @NonNull
    private static String text(@NonNull android.widget.EditText input) {
        Editable value = input.getText();
        return value == null ? "" : value.toString().trim();
    }

    @NonNull
    private static String formatDateTime(long millis) {
        return new SimpleDateFormat("EEE, MMM d, yyyy · h:mm a", Locale.getDefault()).format(new Date(millis));
    }

    @NonNull
    private static String formatDate(long millis) {
        return new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(new Date(millis));
    }

    private static long startOfDay(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void hideKeyboard() {
        View focus = getCurrentFocus();
        if (focus != null) {
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
            }
            focus.clearFocus();
        }
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.createRoot, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });
    }

    /** A TextWatcher where only the after-change hook is interesting. */
    private abstract static class SimpleWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }
}
