package com.itismob.s03.group7.leef.model;

import androidx.annotation.NonNull;

/** The record of one paid boost: what was bought, how it was paid, and until when the event is featured. */
public final class BoostReceipt {

    public static final String COLLECTION = "boosts";
    public static final String FIELD_USER_ID = "userId";
    public static final String FIELD_EVENT_ID = "eventId";
    public static final String FIELD_EVENT_TITLE = "eventTitle";
    public static final String FIELD_PLAN = "plan";
    public static final String FIELD_DAYS = "days";
    public static final String FIELD_AMOUNT = "amount";
    public static final String FIELD_PAYMENT_METHOD = "paymentMethod";
    public static final String FIELD_CONFIRMATION_CODE = "confirmationCode";
    public static final String FIELD_BOOSTED_UNTIL = "boostedUntil";
    public static final String FIELD_CREATED_AT = "createdAt";

    private final String eventId;
    private final String eventTitle;
    private final BoostPlan plan;
    private final double amount;
    private final PaymentMethod paymentMethod;
    private final String confirmationCode;
    private final long boostedUntilMillis;
    /** When it was paid for; 0 for a receipt just made on this phone (not read back from the database). */
    private final long createdAtMillis;

    public BoostReceipt(@NonNull String eventId, @NonNull String eventTitle, @NonNull BoostPlan plan,
                        double amount, @NonNull PaymentMethod paymentMethod, @NonNull String confirmationCode,
                        long boostedUntilMillis) {
        this(eventId, eventTitle, plan, amount, paymentMethod, confirmationCode, boostedUntilMillis, 0);
    }

    public BoostReceipt(@NonNull String eventId, @NonNull String eventTitle, @NonNull BoostPlan plan,
                        double amount, @NonNull PaymentMethod paymentMethod, @NonNull String confirmationCode,
                        long boostedUntilMillis, long createdAtMillis) {
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.plan = plan;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.confirmationCode = confirmationCode;
        this.boostedUntilMillis = boostedUntilMillis;
        this.createdAtMillis = createdAtMillis;
    }

    @NonNull
    public String getEventId() {
        return eventId;
    }

    @NonNull
    public String getEventTitle() {
        return eventTitle;
    }

    @NonNull
    public BoostPlan getPlan() {
        return plan;
    }

    public double getAmount() {
        return amount;
    }

    @NonNull
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    @NonNull
    public String getConfirmationCode() {
        return confirmationCode;
    }

    /** When the event stops being featured. */
    public long getBoostedUntilMillis() {
        return boostedUntilMillis;
    }

    /** When the boost was paid for; 0 when unknown. */
    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    /** True while the event is still featured because of this boost (or a later one that extends it). */
    public boolean isRunning(long nowMillis) {
        return boostedUntilMillis > nowMillis;
    }
}
