package com.itismob.s03.group7.leef.payment;

import androidx.annotation.NonNull;

import com.itismob.s03.group7.leef.model.PaymentMethod;

/** What the checkout sheet shows: how the person pays, how much, and whether a payment is running. */
public final class PaymentSheetState {

    private final PaymentMethod method;
    private final double total;
    private final boolean paying;

    public PaymentSheetState(@NonNull PaymentMethod method, double total, boolean paying) {
        this.method = method;
        this.total = total;
        this.paying = paying;
    }

    @NonNull
    public PaymentMethod getMethod() {
        return method;
    }

    public double getTotal() {
        return total;
    }

    public boolean isPaying() {
        return paying;
    }
}
