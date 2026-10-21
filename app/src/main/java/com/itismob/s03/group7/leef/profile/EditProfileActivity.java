package com.itismob.s03.group7.leef.profile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.auth.RegistrationValidator;
import com.itismob.s03.group7.leef.data.ProfileRepository;
import com.itismob.s03.group7.leef.databinding.ActivityEditProfileBinding;
import com.itismob.s03.group7.leef.model.UserProfile;
import com.itismob.s03.group7.leef.model.UserRole;
import com.itismob.s03.group7.leef.ui.ButtonSpinner;
import com.itismob.s03.group7.leef.ui.LeefEdgeToEdge;

/**
 * Edit profile (opened from the Profile tab): change the name, school or organization and role. The
 * email is shown but can't be changed. Finishes with {@code RESULT_OK} once the changes are saved.
 */
public class EditProfileActivity extends AppCompatActivity {

    private static final String EXTRA_NAME = "name";
    private static final String EXTRA_SCHOOL = "school";
    private static final String EXTRA_ROLE = "role";
    private static final String EXTRA_EMAIL = "email";
    private static final String STATE_ROLE = "state_role";
    private static final int SCROLL_MARGIN_DP = 24;

    private ActivityEditProfileBinding binding;
    private EditProfileViewModel viewModel;
    private final UserRole[] roles = UserRole.values();
    @Nullable
    private UserRole selectedRole;
    private String originalName = "";
    private String originalSchool = "";
    @Nullable
    private UserRole originalRole;
    private boolean spinnerShown;

    @NonNull
    static Intent createIntent(@NonNull Context context, @NonNull UserProfile profile) {
        return new Intent(context, EditProfileActivity.class)
                .putExtra(EXTRA_NAME, profile.getFullName())
                .putExtra(EXTRA_SCHOOL, profile.getSchoolOrganization())
                .putExtra(EXTRA_ROLE, profile.getRole().getValue())
                .putExtra(EXTRA_EMAIL, profile.getEmail());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeefEdgeToEdge.enable(this);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(EditProfileViewModel.class);

        Intent intent = getIntent();
        originalName = valueOrEmpty(intent.getStringExtra(EXTRA_NAME)).trim();
        originalSchool = valueOrEmpty(intent.getStringExtra(EXTRA_SCHOOL)).trim();
        originalRole = UserRole.fromValue(intent.getStringExtra(EXTRA_ROLE));

        ViewCompat.setAccessibilityHeading(binding.screenTitle, true);
        binding.emailInput.setText(valueOrEmpty(intent.getStringExtra(EXTRA_EMAIL)));
        if (savedInstanceState == null) {
            binding.nameInput.setText(originalName);
            binding.schoolInput.setText(originalSchool);
        }
        setUpRoleDropdown(savedInstanceState);

        binding.nameInput.addTextChangedListener(new ErrorClearingWatcher(binding.nameLayout));
        binding.schoolInput.addTextChangedListener(new ErrorClearingWatcher(binding.schoolLayout));
        binding.schoolInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard();
                return true;
            }
            return false;
        });

        binding.backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.saveButton.setOnClickListener(v -> submit());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                leave();
            }
        });

        applyWindowInsets();

        viewModel.getLoading().observe(this, this::renderLoading);
        viewModel.getResult().observe(this, this::handleResult);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (selectedRole != null) {
            outState.putString(STATE_ROLE, selectedRole.getValue());
        }
    }

    // ---------------------------------------------------------------------------------------
    // Setup
    // ---------------------------------------------------------------------------------------

    private void setUpRoleDropdown(@Nullable Bundle savedInstanceState) {
        String[] labels = new String[roles.length];
        for (int i = 0; i < roles.length; i++) {
            labels[i] = getString(roles[i].getLabelRes());
        }
        binding.roleInput.setAdapter(new ArrayAdapter<>(this, R.layout.item_dropdown_option, labels));
        binding.roleInput.setOnItemClickListener((parent, view, position, id) -> {
            selectedRole = roles[position];
            binding.roleLayout.setError(null);
        });

        UserRole start = originalRole;
        if (savedInstanceState != null) {
            UserRole restored = UserRole.fromValue(savedInstanceState.getString(STATE_ROLE));
            if (restored != null) {
                start = restored;
            }
        }
        if (start != null) {
            selectedRole = start;
            binding.roleInput.setText(getString(start.getLabelRes()), false);
        }
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.editRoot, (root, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            root.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });
    }

    // ---------------------------------------------------------------------------------------
    // Submit
    // ---------------------------------------------------------------------------------------

    private void submit() {
        hideKeyboard();
        binding.formError.setVisibility(View.GONE);

        String fullName = textOf(binding.nameInput).trim();
        String school = textOf(binding.schoolInput).trim();

        View firstInvalid = null;
        firstInvalid = check(binding.nameLayout, RegistrationValidator.validateFullName(fullName), firstInvalid);
        firstInvalid = check(binding.schoolLayout, RegistrationValidator.validateSchoolOrganization(school), firstInvalid);
        firstInvalid = check(binding.roleLayout, selectedRole == null ? R.string.auth_error_role_required : 0, firstInvalid);
        if (firstInvalid != null || selectedRole == null) {
            if (firstInvalid != null) {
                scrollTo(firstInvalid);
            }
            return;
        }
        if (!hasChanges()) {
            finish();
            return;
        }
        viewModel.save(fullName, school, selectedRole);
    }

    @Nullable
    private View check(@NonNull TextInputLayout layout, @StringRes int errorRes, @Nullable View firstInvalid) {
        if (errorRes == 0) {
            layout.setError(null);
            return firstInvalid;
        }
        layout.setError(getString(errorRes));
        return firstInvalid != null ? firstInvalid : layout;
    }

    private void scrollTo(@NonNull View target) {
        if (target instanceof TextInputLayout && ((TextInputLayout) target).getEditText() != null) {
            ((TextInputLayout) target).getEditText().requestFocus();
        }
        float density = getResources().getDisplayMetrics().density;
        int y = Math.max(0, target.getTop() - Math.round(SCROLL_MARGIN_DP * density));
        binding.scroll.post(() -> binding.scroll.smoothScrollTo(0, y));
    }

    // ---------------------------------------------------------------------------------------
    // State from the ViewModel
    // ---------------------------------------------------------------------------------------

    private void renderLoading(boolean loading) {
        binding.nameLayout.setEnabled(!loading);
        binding.schoolLayout.setEnabled(!loading);
        binding.roleLayout.setEnabled(!loading);
        binding.backButton.setEnabled(!loading);
        binding.saveButton.setEnabled(!loading);
        binding.saveButton.setText(loading ? R.string.edit_profile_saving : R.string.edit_profile_save);

        if (loading != spinnerShown) {
            binding.saveButton.setIcon(loading ? ButtonSpinner.create(this) : null);
            spinnerShown = loading;
        }
    }

    private void handleResult(@Nullable EditProfileViewModel.Result result) {
        if (result == null) {
            return;
        }
        viewModel.resultHandled();
        if (result.failure == null) {
            setResult(Activity.RESULT_OK);
            finish();
        } else {
            showFailure(result.failure);
        }
    }

    private void showFailure(@NonNull ProfileRepository.SaveFailure failure) {
        int message;
        switch (failure) {
            case NETWORK:
                message = R.string.auth_error_network;
                break;
            case SIGNED_OUT:
                message = R.string.profile_error_signed_out;
                break;
            case PERMISSION:
                message = R.string.edit_profile_error_permission;
                break;
            case UNKNOWN:
            default:
                message = R.string.edit_profile_error_failed;
                break;
        }
        binding.formError.setText(message);
        binding.formError.setVisibility(View.VISIBLE);
        binding.scroll.post(() -> binding.scroll.fullScroll(View.FOCUS_DOWN));
    }

    // ---------------------------------------------------------------------------------------
    // Navigation and helpers
    // ---------------------------------------------------------------------------------------

    private boolean hasChanges() {
        return !textOf(binding.nameInput).trim().equals(originalName)
                || !textOf(binding.schoolInput).trim().equals(originalSchool)
                || selectedRole != originalRole;
    }

    /** Leaves the screen, asking first when there are edits that would be lost. */
    private void leave() {
        if (Boolean.TRUE.equals(viewModel.getLoading().getValue())) {
            return;
        }
        if (!hasChanges()) {
            finish();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.edit_profile_discard_title)
                .setMessage(R.string.edit_profile_discard_message)
                .setPositiveButton(R.string.edit_profile_discard, (dialog, which) -> finish())
                .setNegativeButton(R.string.edit_profile_keep, null)
                .show();
    }

    private void hideKeyboard() {
        View focused = getCurrentFocus();
        InputMethodManager imm = getSystemService(InputMethodManager.class);
        if (focused != null && imm != null) {
            imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
        }
    }

    @NonNull
    private static String valueOrEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }

    @NonNull
    private static String textOf(@NonNull TextInputEditText input) {
        Editable text = input.getText();
        return text == null ? "" : text.toString();
    }

    /** Clears a field's error as soon as the person edits it. */
    private static final class ErrorClearingWatcher implements TextWatcher {
        private final TextInputLayout layout;

        ErrorClearingWatcher(@NonNull TextInputLayout layout) {
            this.layout = layout;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (layout.getError() != null) {
                layout.setError(null);
            }
        }
    }
}
