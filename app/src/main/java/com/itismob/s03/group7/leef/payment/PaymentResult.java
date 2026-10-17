package com.itismob.s03.group7.leef.payment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

/** The outcome of a payment: approved, or declined with a reason the person can read. */
public final class PaymentResult {

    private final boolean approved;
    @StringRes
    private final int declineMessageRes;

    private PaymentResult(boolean approved, @StringRes int declineMessageRes) {
        this.approved = approved;
        this.declineMessageRes = declineMessageRes;
    }

    @NonNull
    public static PaymentResult approved() {
        return new PaymentResult(true, 0);
    }

    @NonNull
    public static PaymentResult declined(@StringRes int messageRes) {
        return new PaymentResult(false, messageRes);
    }

    public boolean isApproved() {
        return approved;
    }

    /** Why it was declined (a string resource); 0 when approved. */
    @StringRes
    public int getDeclineMessageRes() {
        return declineMessageRes;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof PaymentResult
                && ((PaymentResult) other).approved == approved
                && ((PaymentResult) other).declineMessageRes == declineMessageRes;
    }

    @Override
    public int hashCode() {
        return 31 * (approved ? 1 : 0) + declineMessageRes;
    }
}
