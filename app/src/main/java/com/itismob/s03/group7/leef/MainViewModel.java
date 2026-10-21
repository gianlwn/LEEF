package com.itismob.s03.group7.leef;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.itismob.s03.group7.leef.data.FirebaseProfileRepository;
import com.itismob.s03.group7.leef.data.ProfileRepository;
import com.itismob.s03.group7.leef.model.UserProfile;

/**
 * Loads the signed-in person's profile once for the whole app shell. Home reads the name from it for
 * the greeting; {@link MainActivity} uses it to catch an account that has no profile yet.
 */
public class MainViewModel extends ViewModel {

    private final ProfileRepository profiles;

    private final MutableLiveData<UserProfile> profile = new MutableLiveData<>();
    private final MutableLiveData<Boolean> profileMissing = new MutableLiveData<>(false);
    /** True once the profile load has finished one way or the other (loaded or failed). */
    private final MutableLiveData<Boolean> profileFinished = new MutableLiveData<>(false);
    private boolean loading;

    public MainViewModel() {
        this(new FirebaseProfileRepository());
    }

    MainViewModel(@NonNull ProfileRepository profiles) {
        this.profiles = profiles;
        loadProfile();
    }

    /** The loaded profile, or null until it arrives (or if it could not be loaded). */
    @NonNull
    public LiveData<UserProfile> getProfile() {
        return profile;
    }

    /** Becomes true when the profile has loaded or failed to; until then the greeting shouldn't guess a name. */
    @NonNull
    public LiveData<Boolean> getProfileFinished() {
        return profileFinished;
    }

    /** True once we know this account has no LEEF profile. Handle it once, then call {@link #profileMissingHandled()}. */
    @NonNull
    public LiveData<Boolean> getProfileMissing() {
        return profileMissing;
    }

    public void profileMissingHandled() {
        profileMissing.setValue(false);
    }

    /** Loads the profile again (after it was edited, or when the first load failed). */
    public void refreshProfile() {
        loadProfile();
    }

    /** First name for the greeting, or null while unknown. */
    @Nullable
    public String getFirstName() {
        UserProfile current = profile.getValue();
        return current == null ? null : current.getFirstName();
    }

    private void loadProfile() {
        if (loading) {
            return;
        }
        loading = true;
        profiles.loadProfile(new ProfileRepository.LoadCallback() {
            @Override
            public void onLoaded(@NonNull UserProfile loaded) {
                loading = false;
                profile.setValue(loaded);
                profileFinished.setValue(true);
            }

            @Override
            public void onMissing() {
                loading = false;
                profileMissing.setValue(true);
            }

            @Override
            public void onFailure(@NonNull ProfileRepository.Failure failure) {
                // The greeting falls back to a plain welcome; Home still works.
                loading = false;
                profileFinished.setValue(true);
            }
        });
    }
}
