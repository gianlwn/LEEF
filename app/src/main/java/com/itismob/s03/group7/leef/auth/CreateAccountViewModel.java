package com.itismob.s03.group7.leef.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.itismob.s03.group7.leef.data.AccountRepository;
import com.itismob.s03.group7.leef.data.FirebaseAccountRepository;
import com.itismob.s03.group7.leef.model.UserProfile;
import com.itismob.s03.group7.leef.model.UserRole;

/**
 * Keeps a registration alive across screen rotation: the request keeps running and its result is
 * delivered to whichever instance of the screen is showing when it finishes.
 */
public class CreateAccountViewModel extends ViewModel {

    /** Outcome of one registration attempt: exactly one of profile / failure is set. */
    static final class Result {
        @Nullable
        final UserProfile profile;
        /** Only meaningful on success: false if the verification email still has to be resent. */
        final boolean verificationEmailSent;
        @Nullable
        final AccountRepository.Failure failure;

        private Result(@Nullable UserProfile profile, boolean verificationEmailSent,
                       @Nullable AccountRepository.Failure failure) {
            this.profile = profile;
            this.verificationEmailSent = verificationEmailSent;
            this.failure = failure;
        }
    }

    private final AccountRepository repository;
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Result> result = new MutableLiveData<>();

    public CreateAccountViewModel() {
        this(new FirebaseAccountRepository());
    }

    CreateAccountViewModel(@NonNull AccountRepository repository) {
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

    void register(@NonNull String fullName, @NonNull String schoolOrganization, @NonNull String email,
                  @NonNull UserRole role, @NonNull String password) {
        if (Boolean.TRUE.equals(loading.getValue())) {
            return;
        }
        loading.setValue(true);
        repository.register(fullName, schoolOrganization, email, role, password,
                new AccountRepository.RegisterCallback() {
                    @Override
                    public void onSuccess(@NonNull UserProfile profile, boolean verificationEmailSent) {
                        loading.setValue(false);
                        result.setValue(new Result(profile, verificationEmailSent, null));
                    }

                    @Override
                    public void onFailure(@NonNull AccountRepository.Failure failure) {
                        loading.setValue(false);
                        result.setValue(new Result(null, false, failure));
                    }
                });
    }
}
