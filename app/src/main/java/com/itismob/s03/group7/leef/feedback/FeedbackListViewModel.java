package com.itismob.s03.group7.leef.feedback;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.data.FeedbackRepository;
import com.itismob.s03.group7.leef.data.FirebaseFeedbackRepository;
import com.itismob.s03.group7.leef.model.Feedback;

import java.util.ArrayList;
import java.util.List;

/** Loads everything an event's attendees said, for the event's organizer. */
public class FeedbackListViewModel extends ViewModel {

    private final String eventId;
    private final FeedbackRepository feedback;
    private final MutableLiveData<FeedbackListState> state = new MutableLiveData<>();

    public FeedbackListViewModel(@NonNull String eventId) {
        this(eventId, new FirebaseFeedbackRepository());
    }

    FeedbackListViewModel(@NonNull String eventId, @NonNull FeedbackRepository feedback) {
        this.eventId = eventId;
        this.feedback = feedback;
        state.setValue(new FeedbackListState(FeedbackListState.Status.LOADING, new ArrayList<>(), 0, 0));
        load();
    }

    @NonNull
    public LiveData<FeedbackListState> getState() {
        return state;
    }

    public void retry() {
        state.setValue(new FeedbackListState(FeedbackListState.Status.LOADING, new ArrayList<>(), 0, 0));
        load();
    }

    private void load() {
        feedback.loadForEvent(eventId, new FeedbackRepository.ListCallback() {
            @Override
            public void onLoaded(@NonNull List<Feedback> loaded) {
                if (loaded.isEmpty()) {
                    state.setValue(new FeedbackListState(FeedbackListState.Status.EMPTY, loaded, 0, 0));
                    return;
                }
                double total = 0;
                for (Feedback item : loaded) {
                    total += item.getRating();
                }
                state.setValue(new FeedbackListState(FeedbackListState.Status.CONTENT, loaded,
                        total / loaded.size(), 0));
            }

            @Override
            public void onFailure(@NonNull FeedbackRepository.Failure failure) {
                int message = failure == FeedbackRepository.Failure.NETWORK ? R.string.home_error_network
                        : failure == FeedbackRepository.Failure.PERMISSION ? R.string.feedback_list_denied
                        : R.string.home_error_generic;
                state.setValue(new FeedbackListState(FeedbackListState.Status.ERROR, new ArrayList<>(), 0, message));
            }
        });
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
            return (T) new FeedbackListViewModel(eventId);
        }
    }
}
