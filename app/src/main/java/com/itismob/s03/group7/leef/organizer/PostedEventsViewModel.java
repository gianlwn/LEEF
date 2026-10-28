package com.itismob.s03.group7.leef.organizer;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.itismob.s03.group7.leef.data.EventRepository;
import com.itismob.s03.group7.leef.data.FirebaseEventRepository;
import com.itismob.s03.group7.leef.home.HomeViewModel;
import com.itismob.s03.group7.leef.model.Event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Loads the events the signed-in organizer posted. */
public class PostedEventsViewModel extends ViewModel {

    private final EventRepository events;
    private final MutableLiveData<PostedEventsState> state = new MutableLiveData<>();

    private List<Event> loaded = new ArrayList<>();
    private boolean loadedOnce;
    private boolean loading;
    private int errorRes;

    public PostedEventsViewModel() {
        this(new FirebaseEventRepository());
    }

    PostedEventsViewModel(@NonNull EventRepository events) {
        this.events = events;
        publish();
        load();
    }

    @NonNull
    public LiveData<PostedEventsState> getState() {
        return state;
    }

    public void retry() {
        load();
    }

    /** Picks up a boost or a new event when the screen comes back. */
    public void reload() {
        if (loadedOnce) {
            load();
        }
    }

    private void load() {
        if (loading) {
            return;
        }
        loading = true;
        errorRes = 0;
        publish();
        events.loadPostedEvents(new EventRepository.EventsCallback() {
            @Override
            public void onLoaded(@NonNull List<Event> result) {
                loading = false;
                loadedOnce = true;
                long now = System.currentTimeMillis();
                List<Event> upcoming = new ArrayList<>();
                List<Event> past = new ArrayList<>();
                for (Event event : result) {
                    (event.hasEnded(now) ? past : upcoming).add(event);
                }
                Collections.sort(upcoming, (a, b) -> Long.compare(a.getStartsAtMillis(), b.getStartsAtMillis()));
                Collections.sort(past, (a, b) -> Long.compare(b.getStartsAtMillis(), a.getStartsAtMillis()));
                upcoming.addAll(past);
                loaded = upcoming;
                publish();
            }

            @Override
            public void onFailure(@NonNull EventRepository.Failure failure) {
                loading = false;
                if (!loadedOnce) {
                    errorRes = HomeViewModel.errorMessageFor(failure);
                }
                publish();
            }
        });
    }

    private void publish() {
        PostedEventsState.Status status;
        if (loadedOnce) {
            status = loaded.isEmpty() ? PostedEventsState.Status.EMPTY : PostedEventsState.Status.CONTENT;
        } else if (!loading && errorRes != 0) {
            status = PostedEventsState.Status.ERROR;
        } else {
            status = PostedEventsState.Status.LOADING;
        }
        state.setValue(new PostedEventsState(status, loaded, errorRes));
    }
}
