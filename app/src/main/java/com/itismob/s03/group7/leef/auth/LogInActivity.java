package com.itismob.s03.group7.leef.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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
import com.itismob.s03.group7.leef.databinding.ActivityLogInBinding;
import com.itismob.s03.group7.leef.ui.ButtonSpinner;
import com.itismob.s03.group7.leef.ui.LeefEdgeToEdge;

import java.util.Locale;

/**
 * Screen 3 - Log In. Email and password sign-in through Firebase Authentication, a free
 * "Forgot password?" email, and a hand-off to the app (or to Verify Email for unverified accounts).
 */
public class LogInActivity extends AppCompatActivity {

    private static final int SCROLL_MARGIN_DP = 24;

    private ActivityLogInBinding binding;
    private LogInViewModel viewModel;
    /** The button starts without a spinner; only touch its icon when loading actually changes. */
    private boolean spinnerShown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeefEdgeToEdge.enable(this);
        binding = ActivityLogInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(LogInViewModel.class);

        ViewCompat.setAccessibilityHeading(binding.title, true);
        binding.emailInput.addTextChangedListener(new ErrorClearingWatcher(binding.emailLayout));
        binding.passwordInput.addTextChangedListener(new ErrorClearingWatcher(binding.passwordLayout));
        binding.passwordInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit();
                return true;
            }
            return false;
        });

        binding.logInButton.setOnClickListener(v -> submit());
        binding.forgotButton.setOnClickListener(v -> startPasswordReset());
        binding.signUpRow.setOnClickListener(v -> openCreateAccount());

        applyWindowInsets();

        viewModel.getLoading().observe(this, this::renderLoading);
        viewModel.getResult().observe(this, this::handleResult);
    }

    // ---------------------------------------------------------------------------------------
    // Setup
    // ---------------------------------------------------------------------------------------

    /**
     * The corner art bleeds behind the status bar and the scene sits at the bottom; the form stays
     * clear of the system bars, display cutouts and the on-screen keyboard.
     */
    private void applyWindowInsets() {
        View wrapper = binding.scrollContent;
        View form = binding.content;
        int baseLeft = wrapper.getPaddingLeft();
        int baseRight = wrapper.getPaddingRight();
        int formBaseTop = form.getPaddingTop();
        int formBaseBottom = form.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (root, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            // The art itself runs to the screen edges; only the form is kept clear of the bars.
            wrapper.setPadding(baseLeft + bars.left, 0, baseRight + bars.right, ime.bottom);
            form.setPadding(form.getPaddingLeft(), formBaseTop + bars.top,
                    form.getPaddingRight(), formBaseBottom + bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    // ---------------------------------------------------------------------------------------
    // Actions
    // ---------------------------------------------------------------------------------------

    private void submit() {
        hideKeyboard();
        binding.statusText.setVisibility(View.GONE);

        String email = textOf(binding.emailInput).trim();
        String password = textOf(binding.passwordInput);

        View firstInvalid = null;
        firstInvalid = check(binding.emailLayout, RegistrationValidator.validateEmail(email), firstInvalid);
        firstInvalid = check(binding.passwordLayout, RegistrationValidator.validateLoginPassword(password), firstInvalid);
        if (firstInvalid != null) {
            scrollTo(firstInvalid);
            return;
        }
        viewModel.signIn(email.toLowerCase(Locale.ROOT), password);
    }

    /** Asks before emailing a reset link, using the address typed above. */
    private void startPasswordReset() {
        hideKeyboard();
        binding.statusText.setVisibility(View.GONE);

        String email = textOf(binding.emailInput).trim();
        int error = RegistrationValidator.validateEmail(email);
        if (error != 0) {
            binding.emailLayout.setError(getString(
                    email.isEmpty() ? R.string.login_error_reset_needs_email : error));
            scrollTo(binding.emailLayout);
            return;
        }
        String normalized = email.toLowerCase(Locale.ROOT);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.login_reset_title)
                .setMessage(getString(R.string.login_reset_message, normalized))
                .setPositiveButton(R.string.login_reset_send, (dialog, which) -> viewModel.sendPasswordReset(normalized))
                .setNegativeButton(R.string.login_reset_cancel, null)
                .show();
    }

    private void openCreateAccount() {
        startActivity(new Intent(this, CreateAccountActivity.class));
        finish();
    }

    // ---------------------------------------------------------------------------------------
    // State from the ViewModel
    // ---------------------------------------------------------------------------------------

    private void renderLoading(boolean loading) {
        binding.emailLayout.setEnabled(!loading);
        binding.passwordLayout.setEnabled(!loading);
        binding.forgotButton.setEnabled(!loading);
        binding.signUpRow.setEnabled(!loading);
        binding.logInButton.setEnabled(!loading);
        binding.logInButton.setText(loading ? R.string.login_logging_in : R.string.auth_log_in);

        if (loading != spinnerShown) {
            binding.logInButton.setIcon(loading ? ButtonSpinner.create(this) : null);
            spinnerShown = loading;
        }
    }

    private void handleResult(@Nullable LogInViewModel.Result result) {
        if (result == null) {
            return;
        }
        viewModel.resultHandled();
        switch (result.kind) {
            case SIGNED_IN_VERIFIED:
                Toast.makeText(this, R.string.login_welcome_toast, Toast.LENGTH_SHORT).show();
                startActivity(SessionRouter.homeIntent(this));
                finish();
                break;
            case SIGNED_IN_UNVERIFIED:
                // Signed in, but the email still has to be confirmed first.
                startActivity(VerifyEmailActivity.createResumeIntent(this));
                finish();
                break;
            case SIGN_IN_FAILED:
                if (result.signInFailure != null) {
                    showStatus(signInErrorMessage(result.signInFailure), true, null);
                }
                break;
            case RESET_SENT:
                showStatus(R.string.login_reset_sent, false, result.email);
                break;
            case RESET_FAILED:
                if (result.resetFailure != null) {
                    showStatus(resetErrorMessage(result.resetFailure), true, null);
                }
                break;
            default:
                break;
        }
    }

    @StringRes
    private static int signInErrorMessage(@NonNull AccountRepository.SignInFailure failure) {
        switch (failure) {
            case INVALID_CREDENTIALS:
                return R.string.login_error_invalid;
            case ACCOUNT_DISABLED:
                return R.string.login_error_disabled;
            case NETWORK:
                return R.string.auth_error_network;
            case TOO_MANY_REQUESTS:
                return R.string.login_error_too_many;
            case UNKNOWN:
            default:
                return R.string.auth_error_unknown;
        }
    }

    @StringRes
    private static int resetErrorMessage(@NonNull AccountRepository.PasswordResetFailure failure) {
        switch (failure) {
            case INVALID_EMAIL:
                return R.string.auth_error_email_invalid;
            case NETWORK:
                return R.string.auth_error_network;
            case TOO_MANY_REQUESTS:
                return R.string.login_error_too_many;
            case UNKNOWN:
            default:
                return R.string.auth_error_unknown;
        }
    }

    /** Shows a message above the Log In button: red for errors, green for good news. */
    private void showStatus(@StringRes int messageRes, boolean isError, @Nullable String formatArg) {
        binding.statusText.setText(formatArg == null
                ? getString(messageRes) : getString(messageRes, formatArg));
        binding.statusText.setTextColor(ContextCompat.getColor(this,
                isError ? R.color.leef_error : R.color.leef_green_700));
        binding.statusText.setVisibility(View.VISIBLE);
    }

    // ---------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------

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
        int y = Math.max(0, binding.content.getTop() + target.getTop() - Math.round(SCROLL_MARGIN_DP * density));
        binding.scroll.post(() -> binding.scroll.smoothScrollTo(0, y));
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
}
