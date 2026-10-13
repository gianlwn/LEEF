package com.itismob.s03.group7.leef.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.itismob.s03.group7.leef.data.AccountRepository;
import com.itismob.s03.group7.leef.data.FirebaseAccountRepository;

/** Keeps a sign-in (or password-reset request) alive across screen rotation. */
public class LogInViewModel extends ViewModel {

    /** One-time result for the screen to react to. */
    static final class Result {
        enum Kind {
            SIGNED_IN_VERIFIED,
            SIGNED_IN_UNVERIFIED,
            SIGN_IN_FAILED,
            RESET_SENT,
            RESET_FAILED
        }

        final Kind kind;
        @Nullable
        final AccountRepository.SignInFailure signInFailure;
        @Nullable
        final AccountRepository.PasswordResetFailure resetFailure;
        /** The address a reset link was requested for (shown back to the person). */
        @Nullable
        final String email;

        private Result(@NonNull Kind kind, @Nullable AccountRepository.SignInFailure signInFailure,
                       @Nullable AccountRepository.PasswordResetFailure resetFailure, @Nullable String email) {
            this.kind = kind;
            this.signInFailure = signInFailure;
            this.resetFailure = resetFailure;
            this.email = email;
        }
    }

    private final AccountRepository repository;
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Result> result = new MutableLiveData<>();

    public LogInViewModel() {
        this(new FirebaseAccountRepository());
    }

    LogInViewModel(@NonNull AccountRepository repository) {
        this.repository = repository;
    }

    LiveData<Boolean> getLoading() {
        return loading;
    }

    /** Emits once per attempt; call {@link #resultHandled()} after reacting to it. */
    LiveData<Result> getResult() {
        return result;
    }

    void resultHandled() {
        result.setValue(null);
    }

    void signIn(@NonNull String email, @NonNull String password) {
        if (Boolean.TRUE.equals(loading.getValue())) {
            return;
        }
        loading.setValue(true);
        repository.signIn(email, password, new AccountRepository.SignInCallback() {
            @Override
            public void onSignedIn(boolean emailVerified) {
                loading.setValue(false);
                result.setValue(new Result(emailVerified
                        ? Result.Kind.SIGNED_IN_VERIFIED : Result.Kind.SIGNED_IN_UNVERIFIED, null, null, null));
            }

            @Override
            public void onFailure(@NonNull AccountRepository.SignInFailure failure) {
                loading.setValue(false);
                result.setValue(new Result(Result.Kind.SIGN_IN_FAILED, failure, null, null));
            }
        });
    }

    void sendPasswordReset(@NonNull String email) {
        if (Boolean.TRUE.equals(loading.getValue())) {
            return;
        }
        loading.setValue(true);
        repository.sendPasswordReset(email, new AccountRepository.PasswordResetCallback() {
            @Override
            public void onSent() {
                loading.setValue(false);
                result.setValue(new Result(Result.Kind.RESET_SENT, null, null, email));
            }

            @Override
            public void onFailure(@NonNull AccountRepository.PasswordResetFailure failure) {
                loading.setValue(false);
                result.setValue(new Result(Result.Kind.RESET_FAILED, null, failure, email));
            }
        });
    }
}
