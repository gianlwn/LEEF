package com.itismob.s03.group7.leef.auth;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.data.AccountRepository;
import com.itismob.s03.group7.leef.databinding.ActivityCreateAccountBinding;
import com.itismob.s03.group7.leef.model.UserProfile;
import com.itismob.s03.group7.leef.model.UserRole;
import com.itismob.s03.group7.leef.ui.ButtonSpinner;
import com.itismob.s03.group7.leef.ui.LeefEdgeToEdge;

/**
 * Screen 2 - Create Account. Collects the MCO1 registration details (full name, school or
 * organization, email, role, password), creates the Firebase account plus the user's profile, and
 * continues into the app.
 */
public class CreateAccountActivity extends AppCompatActivity {

    private static final String STATE_ROLE = "state_role";
    private static final int SCROLL_MARGIN_DP = 24;

    private ActivityCreateAccountBinding binding;
    private CreateAccountViewModel viewModel;
    private final UserRole[] roles = UserRole.values();
    @Nullable
    private UserRole selectedRole;
    /** The button starts without a spinner; only touch its icon when loading actually changes. */
    private boolean spinnerShown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeefEdgeToEdge.enable(this);
        binding = ActivityCreateAccountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(CreateAccountViewModel.class);

        ViewCompat.setAccessibilityHeading(binding.title, true);
        setUpRoleDropdown(savedInstanceState);
        setUpTermsText();
        setUpFieldErrorClearing();

        binding.passwordInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard();
                return true;
            }
            return false;
        });
        binding.createButton.setOnClickListener(v -> submit());
        binding.loginRow.setOnClickListener(v -> openLogIn());

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
        if (savedInstanceState != null) {
            UserRole restored = UserRole.fromValue(savedInstanceState.getString(STATE_ROLE));
            if (restored != null) {
                selectedRole = restored;
                // filter=false keeps the full list available when the dropdown is opened again.
                binding.roleInput.setText(getString(restored.getLabelRes()), false);
            }
        }
    }

    /** "I agree to the Terms of Service and Privacy Policy." with both names tappable. */
    private void setUpTermsText() {
        String terms = getString(R.string.auth_terms_of_service);
        String privacy = getString(R.string.auth_privacy_policy);
        String full = getString(R.string.auth_terms_agree, terms, privacy);
        SpannableString text = new SpannableString(full);
        addLink(text, full, terms);
        addLink(text, full, privacy);
        binding.termsCheck.setText(text);
        binding.termsCheck.setMovementMethod(LinkMovementMethod.getInstance());
        binding.termsCheck.setHighlightColor(Color.TRANSPARENT);
        binding.termsCheck.setOnCheckedChangeListener((button, checked) -> {
            if (checked) {
                binding.termsError.setVisibility(View.GONE);
            }
        });
    }

    private void addLink(@NonNull SpannableString text, @NonNull String full, @NonNull String label) {
        int start = full.indexOf(label);
        if (start < 0) {
            return;
        }
        int end = start + label.length();
        text.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setSpan(new LegalLinkSpan(label), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void setUpFieldErrorClearing() {
        binding.nameInput.addTextChangedListener(new ErrorClearingWatcher(binding.nameLayout));
        binding.schoolInput.addTextChangedListener(new ErrorClearingWatcher(binding.schoolLayout));
        binding.emailInput.addTextChangedListener(new ErrorClearingWatcher(binding.emailLayout));
        binding.passwordInput.addTextChangedListener(new ErrorClearingWatcher(binding.passwordLayout));
    }

    /**
     * The leaves bleed behind the status bar; the form stays clear of the system bars, display
     * cutouts and the on-screen keyboard.
     */
    private void applyWindowInsets() {
        View content = binding.scrollContent;
        int baseLeft = content.getPaddingLeft();
        int baseTop = content.getPaddingTop();
        int baseRight = content.getPaddingRight();
        int baseBottom = content.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (root, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            content.setPadding(
                    baseLeft + bars.left,
                    baseTop + bars.top,
                    baseRight + bars.right,
                    baseBottom + Math.max(bars.bottom, ime.bottom));
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
        String email = textOf(binding.emailInput).trim();
        String password = textOf(binding.passwordInput);

        View firstInvalid = null;
        firstInvalid = check(binding.nameLayout, RegistrationValidator.validateFullName(fullName), firstInvalid);
        firstInvalid = check(binding.schoolLayout, RegistrationValidator.validateSchoolOrganization(school), firstInvalid);
        firstInvalid = check(binding.emailLayout, RegistrationValidator.validateEmail(email), firstInvalid);
        firstInvalid = check(binding.passwordLayout, RegistrationValidator.validatePassword(password), firstInvalid);
        firstInvalid = check(binding.roleLayout, selectedRole == null ? R.string.auth_error_role_required : 0, firstInvalid);
        if (!binding.termsCheck.isChecked()) {
            binding.termsError.setVisibility(View.VISIBLE);
            if (firstInvalid == null) {
                firstInvalid = binding.termsCheck;
            }
        }

        if (firstInvalid != null || selectedRole == null) {
            if (firstInvalid != null) {
                scrollTo(firstInvalid);
            }
            return;
        }
        viewModel.register(fullName, school, email.toLowerCase(java.util.Locale.ROOT), selectedRole, password);
    }

    /** Shows (or clears) the field's error; returns the first invalid field seen so far. */
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
        // The form sits at the top of the scroll content (below the status bar padding).
        int y = Math.max(0, binding.content.getTop() + target.getTop() - Math.round(SCROLL_MARGIN_DP * density));
        binding.scroll.post(() -> binding.scroll.smoothScrollTo(0, y));
    }

    // ---------------------------------------------------------------------------------------
    // State from the ViewModel
    // ---------------------------------------------------------------------------------------

    private void renderLoading(boolean loading) {
        binding.nameLayout.setEnabled(!loading);
        binding.schoolLayout.setEnabled(!loading);
        binding.emailLayout.setEnabled(!loading);
        binding.passwordLayout.setEnabled(!loading);
        binding.roleLayout.setEnabled(!loading);
        binding.termsCheck.setEnabled(!loading);
        binding.loginRow.setEnabled(!loading);
        binding.createButton.setEnabled(!loading);
        binding.createButton.setText(loading ? R.string.auth_creating_account : R.string.auth_create_account);

        if (loading != spinnerShown) {
            binding.createButton.setIcon(loading ? ButtonSpinner.create(this) : null);
            spinnerShown = loading;
        }
    }

    private void handleResult(@Nullable CreateAccountViewModel.Result result) {
        if (result == null) {
            return;
        }
        viewModel.resultHandled();
        if (result.profile != null) {
            onAccountCreated(result.profile, result.verificationEmailSent);
        } else if (result.failure != null) {
            showFailure(result.failure);
        }
    }

    private void onAccountCreated(@NonNull UserProfile profile, boolean verificationEmailSent) {
        // Next the person confirms their email with the link we just sent.
        startActivity(VerifyEmailActivity.createIntent(this, verificationEmailSent));
        finish();
    }

    private void showFailure(@NonNull AccountRepository.Failure failure) {
        switch (failure) {
            case EMAIL_IN_USE:
                binding.emailLayout.setError(getString(R.string.auth_error_email_in_use));
                scrollTo(binding.emailLayout);
                break;
            case INVALID_EMAIL:
                binding.emailLayout.setError(getString(R.string.auth_error_email_invalid));
                scrollTo(binding.emailLayout);
                break;
            case WEAK_PASSWORD:
                binding.passwordLayout.setError(getString(R.string.auth_error_password_weak));
                scrollTo(binding.passwordLayout);
                break;
            case NETWORK:
                showFormError(R.string.auth_error_network);
                break;
            case PROFILE_NOT_SAVED:
                showFormError(R.string.auth_error_profile_not_saved);
                break;
            case UNKNOWN:
            default:
                showFormError(R.string.auth_error_unknown);
                break;
        }
    }

    private void showFormError(@StringRes int messageRes) {
        binding.formError.setText(messageRes);
        binding.formError.setVisibility(View.VISIBLE);
        binding.scroll.post(() -> binding.scroll.fullScroll(View.FOCUS_DOWN));
    }

    // ---------------------------------------------------------------------------------------
    // Navigation and helpers
    // ---------------------------------------------------------------------------------------

    private void openLogIn() {
        // TODO(Screen 3): startActivity(new Intent(this, LogInActivity.class)); finish();
        Toast.makeText(this, R.string.todo_log_in, Toast.LENGTH_SHORT).show();
    }

    private void showLegalDialog(@NonNull String title) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(R.string.auth_legal_placeholder)
                .setPositiveButton(R.string.auth_legal_ok, null)
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
    private static String textOf(@NonNull com.google.android.material.textfield.TextInputEditText input) {
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

    /** A bold, underlined name inside the terms sentence that opens its document. */
    private final class LegalLinkSpan extends ClickableSpan {
        private final String title;

        LegalLinkSpan(@NonNull String title) {
            this.title = title;
        }

        @Override
        public void onClick(@NonNull View widget) {
            showLegalDialog(title);
        }

        @Override
        public void updateDrawState(@NonNull TextPaint paint) {
            paint.setColor(ContextCompat.getColor(CreateAccountActivity.this, R.color.leef_ink));
            paint.setUnderlineText(true);
        }
    }
}
