package com.itismob.s03.group7.leef.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.itismob.s03.group7.leef.data.AccountRepository;
import com.itismob.s03.group7.leef.data.FirebaseAccountRepository;

/** Keeps a "Continue with Google" attempt alive across screen rotation (Log In and Create Account). */
public class GoogleSignInViewModel extends ViewModel {

    /** What went wrong, in terms a screen can explain. */
    enum Problem {
        CANCELLED,
        NO_ACCOUNT,
        NOT_CONFIGURED,
        ACCOUNT_EXISTS,
        ACCOUNT_DISABLED,
        NETWORK,
        UNKNOWN
    }

    /** One-time result for the screen to react to. */
    static final class Result {
        enum Kind {
            /** Signed in; the person already has a LEEF profile. */
            SIGNED_IN,
            /** Signed in; first time here, so the profile still has to be filled in. */
            PROFILE_NEEDED,
            FAILED
        }

        final Kind kind;
        @Nullable
        final String uid;
        @Nullable
        final Problem problem;

        private Result(@NonNull Kind kind, @Nullable String uid, @Nullable Problem problem) {
            this.kind = kind;
            this.uid = uid;
            this.problem = problem;
        }
    }

    private final AccountRepository repository;
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Result> result = new MutableLiveData<>();

    public GoogleSignInViewModel() {
        this(new FirebaseAccountRepository());
    }

    GoogleSignInViewModel(@NonNull AccountRepository repository) {
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

    /** Marks an attempt as started. Returns false if one is already running. */
    boolean begin() {
        if (Boolean.TRUE.equals(loading.getValue())) {
            return false;
        }
        loading.setValue(true);
        return true;
    }

    /** Google's sheet returned a token: sign in to Firebase with it. */
    void onToken(@NonNull String idToken) {
        repository.signInWithGoogle(idToken, new AccountRepository.GoogleSignInCallback() {
            @Override
            public void onSignedIn() {
                finish(new Result(Result.Kind.SIGNED_IN, null, null));
            }

            @Override
            public void onProfileNeeded(@NonNull String uid, @NonNull String suggestedName,
                                        @NonNull String email) {
                finish(new Result(Result.Kind.PROFILE_NEEDED, uid, null));
            }

            @Override
            public void onFailure(@NonNull AccountRepository.GoogleSignInFailure failure) {
                finish(new Result(Result.Kind.FAILED, null, toProblem(failure)));
            }
        });
    }

    void onTokenFailure(@NonNull GoogleSignInHelper.Failure failure) {
        Problem problem;
        switch (failure) {
            case CANCELLED:
                problem = Problem.CANCELLED;
                break;
            case NO_ACCOUNT:
                problem = Problem.NO_ACCOUNT;
                break;
            case NOT_CONFIGURED:
                problem = Problem.NOT_CONFIGURED;
                break;
            case UNKNOWN:
            default:
                problem = Problem.UNKNOWN;
                break;
        }
        finish(new Result(Result.Kind.FAILED, null, problem));
    }

    private void finish(@NonNull Result outcome) {
        loading.setValue(false);
        result.setValue(outcome);
    }

    @NonNull
    private static Problem toProblem(@NonNull AccountRepository.GoogleSignInFailure failure) {
        switch (failure) {
            case ACCOUNT_EXISTS_WITH_OTHER_METHOD:
                return Problem.ACCOUNT_EXISTS;
            case ACCOUNT_DISABLED:
                return Problem.ACCOUNT_DISABLED;
            case NETWORK:
                return Problem.NETWORK;
            case UNKNOWN:
            default:
                return Problem.UNKNOWN;
        }
    }
}
