package com.itismob.s03.group7.leef.boost;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.itismob.s03.group7.leef.model.BoostReceipt;

import java.util.Collections;
import java.util.List;

/** Everything the Boost history screen draws at one moment. */
public final class BoostHistoryState {

    public enum Status {
        LOADING,
        ERROR,
        EMPTY,
        CONTENT
    }

    private final Status status;
    private final List<BoostReceipt> receipts;
    private final double totalSpent;
    @StringRes
    private final int errorRes;

    BoostHistoryState(@NonNull Status status, @NonNull List<BoostReceipt> receipts, double totalSpent,
                      @StringRes int errorRes) {
        this.status = status;
        this.receipts = Collections.unmodifiableList(receipts);
        this.totalSpent = totalSpent;
        this.errorRes = errorRes;
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    /** Newest first. */
    @NonNull
    public List<BoostReceipt> getReceipts() {
        return receipts;
    }

    /** The total of every boost's price, in pesos. */
    public double getTotalSpent() {
        return totalSpent;
    }

    @StringRes
    public int getErrorRes() {
        return errorRes;
    }
}
