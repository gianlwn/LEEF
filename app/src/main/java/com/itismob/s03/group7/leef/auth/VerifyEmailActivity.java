package com.itismob.s03.group7.leef.auth;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
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

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.databinding.ActivityVerifyEmailBinding;
import com.itismob.s03.group7.leef.onboarding.OnboardingActivity;
import com.itismob.s03.group7.leef.ui.ButtonSpinner;
import com.itismob.s03.group7.leef.ui.LeefEdgeToEdge;

import java.util.Locale;

/**
 * Verify Email. Right after sign-up (and whenever someone signs in with an unverified email) we
 * ask them to tap the link in the free Firebase verification email, then continue into the app.
 */
public class VerifyEmailActivity extends AppCompatActivity {

    private static final String EXTRA_EMAIL_SENT = "extra_email_sent";
    private static final long TICK_MS = 1000L;

    private ActivityVerifyEmailBinding binding;
    private VerifyEmailViewModel viewModel;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean busy;
    private boolean spinnerShown;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            renderResendButton();
            if (viewModel.getCooldownRemainingMs() > 0) {
                handler.postDelayed(this, TICK_MS);
            }
        }
    };

    /** Opens this screen as a fresh task so Back leaves the app instead of returning to sign-up. */
    @NonNull
    public static Intent createIntent(@NonNull Context context, boolean verificationEmailSent) {
        Intent intent = new Intent(context, VerifyEmailActivity.class);
        intent.putExtra(EXTRA_EMAIL_SENT, verificationEmailSent);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeefEdgeToEdge.enable(this);
        viewModel = new ViewModelProvider(this).get(VerifyEmailViewModel.class);

        String email = viewModel.getEmail();
        if (email == null) {
            // Nobody is signed in any more; start over from the welcome screen.
            goToWelcome();
            return;
        }

        binding = ActivityVerifyEmailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setAccessibilityHeading(binding.title, true);
        binding.emailText.setText(email);

        boolean emailSent = getIntent().getBooleanExtra(EXTRA_EMAIL_SENT, false);
        viewModel.start(emailSent);
        if (savedInstanceState == null && !emailSent) {
            showStatus(R.string.verify_status_not_sent, true);
        }

        binding.continueButton.setOnClickListener(v -> viewModel.checkVerified(false));
        binding.resendButton.setOnClickListener(v -> viewModel.resend());
        binding.changeEmailButton.setOnClickListener(v -> {
            viewModel.signOut();
            Intent intent = new Intent(this, CreateAccountActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        applyWindowInsets();
        viewModel.getBusy().observe(this, this::renderBusy);
        viewModel.getOutcome().observe(this, this::handleOutcome);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding == null) {
            return;
        }
        // Coming back from the email app: continue automatically if the link was already tapped.
        viewModel.checkVerified(true);
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(tick);
        super.onPause();
    }

    // ---------------------------------------------------------------------------------------

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

    private void renderBusy(boolean isBusy) {
        busy = isBusy;
        binding.continueButton.setEnabled(!isBusy);
        binding.changeEmailButton.setEnabled(!isBusy);
        binding.continueButton.setText(isBusy ? R.string.verify_checking : R.string.verify_continue);
        if (isBusy != spinnerShown) {
            binding.continueButton.setIcon(isBusy ? ButtonSpinner.create(this) : null);
            spinnerShown = isBusy;
        }
        renderResendButton();
    }

    /** "Resend email", or a countdown while the wait between emails is running. */
    private void renderResendButton() {
        long remainingMs = viewModel.getCooldownRemainingMs();
        if (remainingMs > 0) {
            long seconds = (remainingMs + 999) / 1000;
            String time = String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60);
            binding.resendButton.setText(getString(R.string.verify_resend_wait, time));
            binding.resendButton.setEnabled(false);
        } else {
            binding.resendButton.setText(R.string.verify_resend);
            binding.resendButton.setEnabled(!busy);
        }
    }

    private void handleOutcome(@Nullable VerifyEmailViewModel.Outcome outcome) {
        if (outcome == null) {
            return;
        }
        viewModel.outcomeHandled();
        switch (outcome) {
            case VERIFIED:
                Toast.makeText(this, R.string.verify_done_toast, Toast.LENGTH_LONG).show();
                startActivity(SessionRouter.homeIntent(this));
                finish();
                break;
            case NOT_VERIFIED_YET:
                showStatus(R.string.verify_status_not_yet, true);
                break;
            case EMAIL_SENT:
                showStatus(R.string.verify_status_sent, false);
                handler.removeCallbacks(tick);
                handler.post(tick);
                break;
            case SEND_FAILED:
                showStatus(R.string.verify_status_send_failed, true);
                break;
            case NETWORK:
                showStatus(R.string.auth_error_network, true);
                break;
            case TOO_MANY_REQUESTS:
                showStatus(R.string.verify_status_too_many, true);
                break;
            case SIGNED_OUT:
                goToWelcome();
                break;
            case UNKNOWN:
            default:
                showStatus(R.string.auth_error_unknown, true);
                break;
        }
    }

    private void showStatus(@StringRes int messageRes, boolean isError) {
        binding.statusText.setText(messageRes);
        binding.statusText.setTextColor(ContextCompat.getColor(this,
                isError ? R.color.leef_error : R.color.leef_green_700));
        binding.statusText.setVisibility(View.VISIBLE);
    }

    private void goToWelcome() {
        Intent intent = new Intent(this, OnboardingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
