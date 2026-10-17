package com.itismob.s03.group7.leef.payment;

import androidx.annotation.NonNull;

/**
 * Takes a payment. The app only talks to this interface, so the simulated gateway can later be swapped
 * for a real one (for example PayMongo, which needs a server and so a paid Firebase plan) without
 * touching the screens.
 */
public interface PaymentGateway {

    interface Callback {
        /** Always called on the main thread. */
        void onResult(@NonNull PaymentResult result);
    }

    void pay(@NonNull PaymentInput input, double amountPesos, @NonNull Callback callback);
}
