package com.itismob.s03.group7.leef.auth;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.itismob.s03.group7.leef.R;

/**
 * The "Continue with Google" behaviour shared by Log In and Create Account: starts Google's sheet,
 * then either opens the app, opens the profile step for first-time Google users, or reports a problem
 * back to the screen.
 */
final class GoogleSignInController {

    /** What the hosting screen shows. */
    interface Host {
        /** A Google attempt started or finished; lock or unlock the screen. */
        void onGoogleBusyChanged(boolean busy);

        void onGoogleError(@StringRes int messageRes);
    }

    private final AppCompatActivity activity;
    private final Host host;
    private final GoogleSignInViewModel viewModel;

    GoogleSignInController(@NonNull AppCompatActivity activity, @NonNull Host host) {
        this.activity = activity;
        this.host = host;
        this.viewModel = new ViewModelProvider(activity).get(GoogleSignInViewModel.class);
        viewModel.getLoading().observe(activity, host::onGoogleBusyChanged);
        viewModel.getResult().observe(activity, this::handleResult);
    }

    /** Called when the Google button is tapped. */
    void start() {
        if (viewModel.begin()) {
            GoogleSignInHelper.request(activity, viewModel);
        }
    }

    private void handleResult(@Nullable GoogleSignInViewModel.Result result) {
        if (result == null) {
            return;
        }
        viewModel.resultHandled();
        switch (result.kind) {
            case SIGNED_IN:
                Toast.makeText(activity, R.string.login_welcome_toast, Toast.LENGTH_SHORT).show();
                activity.startActivity(SessionRouter.homeIntent(activity));
                activity.finish();
                break;
            case PROFILE_NEEDED:
                if (result.uid != null) {
                    activity.startActivity(CompleteProfileActivity.createIntentForNewUser(activity, result.uid));
                    activity.finish();
                }
                break;
            case FAILED:
                if (result.problem != null && result.problem != GoogleSignInViewModel.Problem.CANCELLED) {
                    host.onGoogleError(messageFor(result.problem));
                }
                break;
            default:
                break;
        }
    }

    @StringRes
    private static int messageFor(@NonNull GoogleSignInViewModel.Problem problem) {
        switch (problem) {
            case NO_ACCOUNT:
                return R.string.auth_google_error_no_account;
            case NOT_CONFIGURED:
                return R.string.auth_google_error_not_configured;
            case ACCOUNT_EXISTS:
                return R.string.auth_google_error_exists;
            case ACCOUNT_DISABLED:
                return R.string.login_error_disabled;
            case NETWORK:
                return R.string.auth_error_network;
            case CANCELLED:
            case UNKNOWN:
            default:
                return R.string.auth_google_error_failed;
        }
    }
}
