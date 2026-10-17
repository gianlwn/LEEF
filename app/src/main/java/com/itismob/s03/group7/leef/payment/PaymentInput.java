package com.itismob.s03.group7.leef.payment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.itismob.s03.group7.leef.model.PaymentMethod;

/**
 * What the person typed into the payment sheet. It exists only while the payment runs: nothing here is
 * saved to Firestore, written to a log, or kept after the payment finishes.
 */
public final class PaymentInput {

    private final PaymentMethod method;
    @Nullable
    private final String mobileNumber;
    @Nullable
    private final String cardNumber;
    @Nullable
    private final String expiry;
    @Nullable
    private final String securityCode;

    private PaymentInput(@NonNull PaymentMethod method, @Nullable String mobileNumber, @Nullable String cardNumber,
                         @Nullable String expiry, @Nullable String securityCode) {
        this.method = method;
        this.mobileNumber = mobileNumber;
        this.cardNumber = cardNumber;
        this.expiry = expiry;
        this.securityCode = securityCode;
    }

    @NonNull
    public static PaymentInput wallet(@NonNull PaymentMethod method, @NonNull String mobileNumber) {
        return new PaymentInput(method, mobileNumber, null, null, null);
    }

    @NonNull
    public static PaymentInput card(@NonNull String cardNumber, @NonNull String expiry, @NonNull String securityCode) {
        return new PaymentInput(PaymentMethod.CARD, null, cardNumber, expiry, securityCode);
    }

    @NonNull
    public PaymentMethod getMethod() {
        return method;
    }

    @Nullable
    public String getMobileNumber() {
        return mobileNumber;
    }

    @Nullable
    public String getCardNumber() {
        return cardNumber;
    }

    @Nullable
    public String getExpiry() {
        return expiry;
    }

    @Nullable
    public String getSecurityCode() {
        return securityCode;
    }
}
