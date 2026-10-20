package com.itismob.s03.group7.leef.feedback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.data.EventRepository;
import com.itismob.s03.group7.leef.data.FeedbackRepository;
import com.itismob.s03.group7.leef.data.FirebaseEventRepository;
import com.itismob.s03.group7.leef.data.FirebaseFeedbackRepository;
import com.itismob.s03.group7.leef.data.FirebaseRegistrationRepository;
import com.itismob.s03.group7.leef.data.RegistrationRepository;
import com.itismob.s03.group7.leef.model.Event;
import com.itismob.s03.group7.leef.model.Feedback;
import com.itismob.s03.group7.leef.model.FeedbackTag;
import com.itismob.s03.group7.leef.model.Registration;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Screen 12 - Event Feedback. Works out whether the person can rate the event (registered, and it is
 * over), loads any feedback they already gave, keeps the stars, quick tags and photo link they pick, and
 * sends it. The typed comment is handed over when they tap Submit.
 */
public class FeedbackViewModel extends ViewModel {

    private final String eventId;
    private final EventRepository events;
    private final RegistrationRepository registrations;
    private final FeedbackRepository feedback;

    private final MutableLiveData<FeedbackState> state = new MutableLiveData<>();
    private final MutableLiveData<Integer> message = new MutableLiveData<>();
    /** Set once when an existing comment should be put into the text box. */
    private final MutableLiveData<String> existingComment = new MutableLiveData<>();
    private final MutableLiveData<Boolean> ratingMissing = new MutableLiveData<>(false);

    private FeedbackState.Status status = FeedbackState.Status.LOADING;
    @Nullable
    private Event event;
    private boolean posting;
    private boolean editing;
    private int titleRes;
    private int bodyRes;

    private int rating;
    private final Set<FeedbackTag> tags = new LinkedHashSet<>();
    @Nullable
    private String photoUrl;

    public FeedbackViewModel(@NonNull String eventId) {
        this(eventId, new FirebaseEventRepository(), new FirebaseRegistrationRepository(),
                new FirebaseFeedbackRepository());
    }

    FeedbackViewModel(@NonNull String eventId, @NonNull EventRepository events,
                      @NonNull RegistrationRepository registrations, @NonNull FeedbackRepository feedback) {
        this.eventId = eventId;
        this.events = events;
        this.registrations = registrations;
        this.feedback = feedback;
        publish();
        load();
    }

    @NonNull
    public LiveData<FeedbackState> getState() {
        return state;
    }

    /** A toast message (string resource id). Call {@link #messageHandled()} after showing it. */
    @NonNull
    public LiveData<Integer> getMessage() {
        return message;
    }

    public void messageHandled() {
        message.setValue(null);
    }

    /** The comment the person wrote earlier, to show in the text box once; call {@link #existingCommentHandled()}. */
    @NonNull
    public LiveData<String> getExistingComment() {
        return existingComment;
    }

    public void existingCommentHandled() {
        existingComment.setValue(null);
    }

    /** True after Submit was tapped with no stars chosen. */
    @NonNull
    public LiveData<Boolean> getRatingMissing() {
        return ratingMissing;
    }

    public void retry() {
        if (status == FeedbackState.Status.ERROR) {
            status = FeedbackState.Status.LOADING;
            publish();
            load();
        }
    }

    // ---- what the person picked -------------------------------------------------------------

    public int getRating() {
        return rating;
    }

    public void setRating(int value) {
        rating = value;
        ratingMissing.setValue(false);
    }

    @NonNull
    public Set<FeedbackTag> getTags() {
        return tags;
    }

    public void setTag(@NonNull FeedbackTag tag, boolean selected) {
        if (selected) {
            tags.add(tag);
        } else {
            tags.remove(tag);
        }
    }

    @Nullable
    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(@Nullable String url) {
        photoUrl = url;
    }

    // ---- sending ----------------------------------------------------------------------------

    public void submit(@NonNull String comment) {
        if (posting || status != FeedbackState.Status.FORM) {
            return;
        }
        if (rating < Feedback.MIN_RATING) {
            ratingMissing.setValue(true);
            return;
        }
        posting = true;
        publish();
        feedback.submit(eventId, rating, new ArrayList<>(tags), comment.trim(), photoUrl,
                new FeedbackRepository.DoneCallback() {
                    @Override
                    public void onDone() {
                        posting = false;
                        status = FeedbackState.Status.DONE;
                        publish();
                    }

                    @Override
                    public void onFailure(@NonNull FeedbackRepository.Failure failure) {
                        posting = false;
                        publish();
                        message.setValue(failure == FeedbackRepository.Failure.NETWORK
                                ? R.string.feedback_post_offline
                                : failure == FeedbackRepository.Failure.PERMISSION
                                ? R.string.feedback_post_denied : R.string.feedback_post_failed);
                    }
                });
    }

    // ---- loading ----------------------------------------------------------------------------

    private void load() {
        events.loadEvent(eventId, new EventRepository.EventCallback() {
            @Override
            public void onLoaded(@NonNull Event loaded) {
                event = loaded;
                if (!loaded.hasEnded(System.currentTimeMillis())) {
                    unavailable(R.string.feedback_unavailable_early_title, R.string.feedback_unavailable_early_body);
                    return;
                }
                loadRegistration();
            }

            @Override
            public void onFailure(@NonNull EventRepository.Failure failure) {
                if (failure == EventRepository.Failure.NOT_FOUND) {
                    unavailable(R.string.feedback_unavailable_gone_title, R.string.feedback_unavailable_gone_body);
                } else {
                    error(failure == EventRepository.Failure.NETWORK
                            ? R.string.home_error_network : R.string.home_error_generic);
                }
            }
        });
    }

    private void loadRegistration() {
        registrations.loadRegistration(eventId, new RegistrationRepository.LoadCallback() {
            @Override
            public void onLoaded(@Nullable Registration registration) {
                if (registration == null) {
                    unavailable(R.string.feedback_unavailable_not_registered_title,
                            R.string.feedback_unavailable_not_registered_body);
                } else {
                    loadMine();
                }
            }

            @Override
            public void onFailure(@NonNull RegistrationRepository.Failure failure) {
                error(failure == RegistrationRepository.Failure.NETWORK
                        ? R.string.home_error_network : R.string.home_error_generic);
            }
        });
    }

    private void loadMine() {
        feedback.loadMine(eventId, new FeedbackRepository.MineCallback() {
            @Override
            public void onLoaded(@Nullable Feedback mine) {
                if (mine != null) {
                    editing = true;
                    rating = mine.getRating();
                    tags.clear();
                    tags.addAll(mine.getTags());
                    photoUrl = mine.getPhotoUrl();
                    if (!mine.getComment().isEmpty()) {
                        existingComment.setValue(mine.getComment());
                    }
                }
                status = FeedbackState.Status.FORM;
                publish();
            }

            @Override
            public void onFailure(@NonNull FeedbackRepository.Failure failure) {
                error(failure == FeedbackRepository.Failure.NETWORK
                        ? R.string.home_error_network : R.string.home_error_generic);
            }
        });
    }

    private void unavailable(int title, int body) {
        status = FeedbackState.Status.UNAVAILABLE;
        titleRes = title;
        bodyRes = body;
        publish();
    }

    private void error(int body) {
        status = FeedbackState.Status.ERROR;
        titleRes = 0;
        bodyRes = body;
        publish();
    }

    private void publish() {
        state.setValue(new FeedbackState(status, event, posting, editing, titleRes, bodyRes));
    }

    /** Creates the view model for one event. */
    public static final class Factory implements ViewModelProvider.Factory {
        private final String eventId;

        public Factory(@NonNull String eventId) {
            this.eventId = eventId;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new FeedbackViewModel(eventId);
        }
    }
}
