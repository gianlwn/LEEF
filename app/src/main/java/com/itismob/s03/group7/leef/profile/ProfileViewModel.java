package com.itismob.s03.group7.leef.profile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.data.AccountRepository;
import com.itismob.s03.group7.leef.data.FirebaseAccountRepository;

/**
 * The Profile tab's actions that need Firebase: emailing a password-reset link and signing out. The
 * profile itself comes from the shared {@code MainViewModel}.
 */
public class ProfileViewModel extends ViewModel {

    /** A one-time message for the person: a string resource and, optionally, one value to fill into it. */
    static final class Message {
        @StringRes
        final int messageRes;
        @Nullable
        final String argument;

        Message(@StringRes int messageRes, @Nullable String argument) {
            this.messageRes = messageRes;
            this.argument = argument;
        }
    }

    private final AccountRepository accounts;
    private final MutableLiveData<Boolean> sendingReset = new MutableLiveData<>(false);
    private final MutableLiveData<Message> message = new MutableLiveData<>();
    private final MutableLiveData<Boolean> signedOut = new MutableLiveData<>(false);

    public ProfileViewModel() {
        this(new FirebaseAccountRepository());
    }

    ProfileViewModel(@NonNull AccountRepository accounts) {
        this.accounts = accounts;
    }

    LiveData<Boolean> getSendingReset() {
        return sendingReset;
    }

    /** Emits once per message; call {@link #messageHandled()} after showing it. */
    LiveData<Message> getMessage() {
        return message;
    }

    void messageHandled() {
        message.setValue(null);
    }

    /** True once the person has been signed out and the screen should leave. */
    LiveData<Boolean> getSignedOut() {
        return signedOut;
    }

    /** Google-only accounts have no password to change. */
    boolean hasPassword() {
        return accounts.hasPasswordSignIn();
    }

    @Nullable
    String getEmail() {
        return accounts.getSignedInEmail();
    }

    /** Emails the signed-in account a link to choose a new password (Firebase's free reset email). */
    void sendPasswordReset() {
        if (Boolean.TRUE.equals(sendingReset.getValue())) {
            return;
        }
        String email = accounts.getSignedInEmail();
        if (email == null || email.isEmpty()) {
            message.setValue(new Message(R.string.profile_error_signed_out, null));
            return;
        }
        sendingReset.setValue(true);
        accounts.sendPasswordReset(email, new AccountRepository.PasswordResetCallback() {
            @Override
            public void onSent() {
                sendingReset.setValue(false);
                message.setValue(new Message(R.string.login_reset_sent, email));
            }

            @Override
            public void onFailure(@NonNull AccountRepository.PasswordResetFailure failure) {
                sendingReset.setValue(false);
                message.setValue(new Message(errorFor(failure), null));
            }
        });
    }

    void signOut() {
        accounts.signOut();
        signedOut.setValue(true);
    }

    @StringRes
    private static int errorFor(@NonNull AccountRepository.PasswordResetFailure failure) {
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
}
