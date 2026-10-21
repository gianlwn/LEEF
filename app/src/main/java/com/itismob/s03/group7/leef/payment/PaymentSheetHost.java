package com.itismob.s03.group7.leef.payment;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

/**
 * Implemented by an Activity that opens the checkout sheet (registering for an event, boosting one).
 * The sheet only collects the payment details; the host decides what is bought and what happens after.
 */
public interface PaymentSheetHost {

    @NonNull
    LiveData<PaymentSheetState> getPaymentSheetState();

    /** Why the last payment attempt didn't finish (a string resource id), shown in the sheet. */
    @NonNull
    LiveData<Integer> getPaymentError();

    void paymentErrorHandled();

    void pay(@NonNull PaymentInput input);
}
