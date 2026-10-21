package com.itismob.s03.group7.leef.profile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.itismob.s03.group7.leef.data.FirebaseProfileRepository;
import com.itismob.s03.group7.leef.data.ProfileRepository;
import com.itismob.s03.group7.leef.model.UserRole;

/** Keeps the Edit profile save alive across screen rotation. */
public class EditProfileViewModel extends ViewModel {

    /** Outcome of one save: no failure means it worked. */
    static final class Result {
        @Nullable
        final ProfileRepository.SaveFailure failure;

        Result(@Nullable ProfileRepository.SaveFailure failure) {
            this.failure = failure;
        }
    }

    private final ProfileRepository profiles;
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Result> result = new MutableLiveData<>();

    public EditProfileViewModel() {
        this(new FirebaseProfileRepository());
    }

    EditProfileViewModel(@NonNull ProfileRepository profiles) {
        this.profiles = profiles;
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
        profiles.updateProfile(fullName, schoolOrganization, role, new ProfileRepository.SaveCallback() {
            @Override
            public void onSaved() {
                loading.setValue(false);
                result.setValue(new Result(null));
            }

            @Override
            public void onFailure(@NonNull ProfileRepository.SaveFailure failure) {
                loading.setValue(false);
                result.setValue(new Result(failure));
            }
        });
    }
}
