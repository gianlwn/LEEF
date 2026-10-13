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

/** Keeps the "finish your profile" save alive across screen rotation. */
public class CompleteProfileViewModel extends ViewModel {

    /** Outcome of one save: exactly one of profile / failure is set. */
    static final class Result {
        @Nullable
        final UserProfile profile;
        @Nullable
        final AccountRepository.ProfileFailure failure;

        private Result(@Nullable UserProfile profile, @Nullable AccountRepository.ProfileFailure failure) {
            this.profile = profile;
            this.failure = failure;
        }
    }

    private final AccountRepository repository;
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Result> result = new MutableLiveData<>();

    public CompleteProfileViewModel() {
        this(new FirebaseAccountRepository());
    }

    CompleteProfileViewModel(@NonNull AccountRepository repository) {
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

    void save(@NonNull String fullName, @NonNull String schoolOrganization, @NonNull UserRole role) {
        if (Boolean.TRUE.equals(loading.getValue())) {
            return;
        }
        loading.setValue(true);
        repository.saveGoogleProfile(fullName, schoolOrganization, role, new AccountRepository.SaveProfileCallback() {
            @Override
            public void onSaved(@NonNull UserProfile profile) {
                loading.setValue(false);
                result.setValue(new Result(profile, null));
            }

            @Override
            public void onFailure(@NonNull AccountRepository.ProfileFailure failure) {
                loading.setValue(false);
                result.setValue(new Result(null, failure));
            }
        });
    }
}
