package com.itismob.s03.group7.leef.boost;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.itismob.s03.group7.leef.data.EventRepository;
import com.itismob.s03.group7.leef.model.BoostPlan;
import com.itismob.s03.group7.leef.model.BoostReceipt;
import com.itismob.s03.group7.leef.model.Event;
import com.itismob.s03.group7.leef.model.PaymentMethod;

/** Everything the Promote Event screen draws at one moment. */
public final class BoostState {

    public enum Status {
        LOADING,
        /** Couldn't load the event; see {@link #getFailure()}. */
        ERROR,
        /** The event doesn't exist (any more). */
        NOT_FOUND,
        /** Somebody other than the organizer opened the screen. */
        NOT_ORGANIZER,
        /** The event has begun, so a boost no longer helps. */
        STARTED,
        /** The organizer cancelled the event, so it can't be promoted. */
        CANCELLED,
        /** Choosing how long to boost and how to pay. */
        FORM,
        /** Boosted: the confirmation is showing. */
        DONE
    }

    private final Status status;
    @Nullable
    private final Event event;
    @Nullable
    private final EventRepository.Failure failure;
    private final BoostPlan plan;
    private final PaymentMethod method;
    private final boolean paying;
    @Nullable
    private final BoostReceipt receipt;
    private final long now;

    BoostState(@NonNull Status status, @Nullable Event event, @Nullable EventRepository.Failure failure,
               @NonNull BoostPlan plan, @NonNull PaymentMethod method, boolean paying,
               @Nullable BoostReceipt receipt, long now) {
        this.status = status;
        this.event = event;
        this.failure = failure;
        this.plan = plan;
        this.method = method;
        this.paying = paying;
        this.receipt = receipt;
        this.now = now;
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    @Nullable
    public Event getEvent() {
        return event;
    }

    @Nullable
    public EventRepository.Failure getFailure() {
        return failure;
    }

    @NonNull
    public BoostPlan getPlan() {
        return plan;
    }

    @NonNull
    public PaymentMethod getMethod() {
        return method;
    }

    public boolean isPaying() {
        return paying;
    }

    /** The finished boost once {@link Status#DONE}. */
    @Nullable
    public BoostReceipt getReceipt() {
        return receipt;
    }

    /** What the chosen plan costs, in pesos. */
    public double getTotal() {
        return plan.getPrice();
    }

    /** When the event is already boosted until, or 0 when it isn't boosted right now. */
    public long getActiveUntilMillis() {
        return event != null && event.isBoosted(now) ? event.getBoostedUntilMillis() : 0;
    }

    /** True when this plan would stay within {@link BoostPlan#MAX_DAYS_AHEAD} days from now. */
    public boolean isAllowed(@NonNull BoostPlan candidate) {
        return allowed(candidate, getActiveUntilMillis(), now);
    }

    static boolean allowed(@NonNull BoostPlan candidate, long activeUntilMillis, long now) {
        long base = Math.max(now, activeUntilMillis);
        return base + candidate.getDurationMillis() - now <= BoostPlan.MAX_DAYS_AHEAD * BoostPlan.DAY_MILLIS;
    }
}
