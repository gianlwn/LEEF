package com.itismob.s03.group7.leef.boost;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.data.BoostRepository;
import com.itismob.s03.group7.leef.data.FirebaseBoostRepository;
import com.itismob.s03.group7.leef.model.BoostReceipt;

import java.util.ArrayList;
import java.util.List;

/** Loads the signed-in organizer's paid boosts. */
public class BoostHistoryViewModel extends ViewModel {

    private final BoostRepository boosts;
    private final MutableLiveData<BoostHistoryState> state = new MutableLiveData<>();

    private List<BoostReceipt> loaded = new ArrayList<>();
    private boolean loadedOnce;
    private boolean loading;
    @StringRes
    private int errorRes;

    public BoostHistoryViewModel() {
        this(new FirebaseBoostRepository());
    }

    BoostHistoryViewModel(@NonNull BoostRepository boosts) {
        this.boosts = boosts;
        publish();
        load();
    }

    @NonNull
    public LiveData<BoostHistoryState> getState() {
        return state;
    }

    public void retry() {
        load();
    }

    /** Picks up a boost bought since the screen was last shown. */
    public void reload() {
        if (loadedOnce) {
            load();
        }
    }

    @StringRes
    static int errorMessageFor(@NonNull BoostRepository.Failure failure) {
        switch (failure) {
            case NETWORK:
                return R.string.boost_history_error_network;
            case PERMISSION:
                return R.string.boost_history_error_permission;
            default:
                return R.string.boost_history_error_generic;
        }
    }

    private void load() {
        if (loading) {
            return;
        }
        loading = true;
        errorRes = 0;
        publish();
        boosts.loadMyBoosts(new BoostRepository.HistoryCallback() {
            @Override
            public void onLoaded(@NonNull List<BoostReceipt> receipts) {
                loading = false;
                loadedOnce = true;
                loaded = receipts;
                publish();
            }

            @Override
            public void onFailure(@NonNull BoostRepository.Failure failure) {
                loading = false;
                if (!loadedOnce) {
                    errorRes = errorMessageFor(failure);
                }
                publish();
            }
        });
    }

    private void publish() {
        BoostHistoryState.Status status;
        if (loadedOnce) {
            status = loaded.isEmpty() ? BoostHistoryState.Status.EMPTY : BoostHistoryState.Status.CONTENT;
        } else if (!loading && errorRes != 0) {
            status = BoostHistoryState.Status.ERROR;
        } else {
            status = BoostHistoryState.Status.LOADING;
        }
        double total = 0;
        for (BoostReceipt receipt : loaded) {
            total += receipt.getAmount();
        }
        state.setValue(new BoostHistoryState(status, loaded, total, errorRes));
    }
}
