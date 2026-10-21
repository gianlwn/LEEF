package com.itismob.s03.group7.leef.boost;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.data.BoostRepository;
import com.itismob.s03.group7.leef.data.EventRepository;
import com.itismob.s03.group7.leef.data.FirebaseBoostRepository;
import com.itismob.s03.group7.leef.data.FirebaseEventRepository;
import com.itismob.s03.group7.leef.model.BoostPlan;
import com.itismob.s03.group7.leef.model.BoostReceipt;
import com.itismob.s03.group7.leef.model.Event;
import com.itismob.s03.group7.leef.model.PaymentMethod;
import com.itismob.s03.group7.leef.payment.ConfirmationCode;
import com.itismob.s03.group7.leef.payment.PaymentGateway;
import com.itismob.s03.group7.leef.payment.PaymentInput;
import com.itismob.s03.group7.leef.payment.PaymentResult;
import com.itismob.s03.group7.leef.payment.SimulatedPaymentGateway;

/**
 * Promote Event. Loads the event, keeps the chosen plan and payment method, runs the simulated payment
 * and then saves the boost. If the payment goes through but saving fails (for example offline), the
 * approved code is kept so trying again doesn't charge a second time.
 */
public class BoostViewModel extends ViewModel {

    private final EventRepository events;
    private final BoostRepository boosts;
    private final PaymentGateway gateway;
    private final String eventId;
    @Nullable
    private final String userId;

    private final MutableLiveData<BoostState> state = new MutableLiveData<>();
    private final MutableLiveData<Integer> paymentError = new MutableLiveData<>();

    @Nullable
    private Event event;
    @Nullable
    private EventRepository.Failure failure;
    private boolean loading;
    private BoostPlan plan = BoostPlan.THREE_DAYS;
    private PaymentMethod method = PaymentMethod.GCASH;
    private boolean paying;
    @Nullable
    private BoostReceipt receipt;
    /** Set once a payment was approved but saving the boost failed, so a retry doesn't charge again. */
    @Nullable
    private String approvedCode;

    BoostViewModel(@NonNull EventRepository events, @NonNull BoostRepository boosts,
                   @NonNull PaymentGateway gateway, @NonNull String eventId, @Nullable String userId) {
        this.events = events;
        this.boosts = boosts;
        this.gateway = gateway;
        this.eventId = eventId;
        this.userId = userId;
        load();
    }

    @NonNull
    public LiveData<BoostState> getState() {
        return state;
    }

    /** Why the last payment attempt didn't finish (shown in the payment sheet). */
    @NonNull
    public LiveData<Integer> getPaymentError() {
        return paymentError;
    }

    public void paymentErrorHandled() {
        paymentError.setValue(null);
    }

    public void retry() {
        load();
    }

    public void selectPlan(@NonNull BoostPlan chosen) {
        BoostState current = state.getValue();
        if (!canEdit() || current == null || !current.isAllowed(chosen)) {
            return;
        }
        plan = chosen;
        publish();
    }

    public void selectMethod(@NonNull PaymentMethod chosen) {
        if (!canEdit()) {
            return;
        }
        method = chosen;
        publish();
    }

    /** Pays through the simulated gateway, then saves the boost. */
    public void pay(@NonNull PaymentInput input) {
        if (event == null || paying) {
            return;
        }
        paying = true;
        publish();
        if (approvedCode != null) {
            // Already paid; only the saving failed last time.
            save(approvedCode);
            return;
        }
        gateway.pay(input, plan.getPrice(), new PaymentGateway.Callback() {
            @Override
            public void onResult(@NonNull PaymentResult result) {
                if (!result.isApproved()) {
                    paying = false;
                    publish();
                    paymentError.setValue(result.getDeclineMessageRes());
                    return;
                }
                approvedCode = ConfirmationCode.generate();
                save(approvedCode);
            }
        });
    }

    private boolean canEdit() {
        return event != null && state.getValue() != null
                && state.getValue().getStatus() == BoostState.Status.FORM
                && !paying && approvedCode == null;
    }

    private void save(@NonNull String code) {
        boosts.saveBoost(eventId, plan, method, code, new BoostRepository.SaveCallback() {
            @Override
            public void onSaved(@NonNull BoostReceipt saved) {
                receipt = saved;
                paying = false;
                approvedCode = null;
                publish();
            }

            @Override
            public void onFailure(@NonNull BoostRepository.Failure reason) {
                paying = false;
                if (isRefusal(reason)) {
                    // Nothing was boosted, so the payment is dropped rather than kept for a retry.
                    approvedCode = null;
                }
                publish();
                paymentError.setValue(failureText(reason));
            }
        });
    }

    private static boolean isRefusal(@NonNull BoostRepository.Failure reason) {
        switch (reason) {
            case NOT_FOUND:
            case NOT_ORGANIZER:
            case STARTED:
            case TOO_LONG:
                return true;
            default:
                return false;
        }
    }

    private static int failureText(@NonNull BoostRepository.Failure reason) {
        switch (reason) {
            case NOT_FOUND:
                return R.string.boost_error_not_found;
            case NOT_ORGANIZER:
                return R.string.boost_error_not_organizer;
            case STARTED:
                return R.string.boost_error_started;
            case TOO_LONG:
                return R.string.boost_error_too_long;
            case PERMISSION:
                return R.string.boost_error_permission;
            case NETWORK:
                return R.string.boost_error_network_paid;
            default:
                return R.string.boost_error_generic_paid;
        }
    }

    private void load() {
        if (loading) {
            return;
        }
        loading = true;
        failure = null;
        publish();
        events.loadEvent(eventId, new EventRepository.EventCallback() {
            @Override
            public void onLoaded(@NonNull Event result) {
                loading = false;
                event = result;
                failure = null;
                // Start on the longest plan that still fits, when the default no longer does.
                long now = System.currentTimeMillis();
                long active = result.isBoosted(now) ? result.getBoostedUntilMillis() : 0;
                if (!BoostState.allowed(plan, active, now)) {
                    for (BoostPlan candidate : BoostPlan.values()) {
                        if (BoostState.allowed(candidate, active, now)) {
                            plan = candidate;
                            break;
                        }
                    }
                }
                publish();
            }

            @Override
            public void onFailure(@NonNull EventRepository.Failure reason) {
                loading = false;
                failure = reason;
                publish();
            }
        });
    }

    private void publish() {
        long now = System.currentTimeMillis();
        BoostState.Status status;
        if (receipt != null) {
            status = BoostState.Status.DONE;
        } else if (event != null) {
            if (userId == null || !userId.equals(event.getOrganizerId())) {
                status = BoostState.Status.NOT_ORGANIZER;
            } else if (event.hasStarted(now)) {
                status = BoostState.Status.STARTED;
            } else {
                status = BoostState.Status.FORM;
            }
        } else if (loading || failure == null) {
            status = BoostState.Status.LOADING;
        } else if (failure == EventRepository.Failure.NOT_FOUND) {
            status = BoostState.Status.NOT_FOUND;
        } else {
            status = BoostState.Status.ERROR;
        }
        state.setValue(new BoostState(status, event, failure, plan, method, paying, receipt, now));
    }

    /** Creates the view model for one event id. */
    static final class Factory implements ViewModelProvider.Factory {
        private final String eventId;

        Factory(@NonNull String eventId) {
            this.eventId = eventId;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            return (T) new BoostViewModel(new FirebaseEventRepository(), new FirebaseBoostRepository(),
                    new SimulatedPaymentGateway(), eventId, user == null ? null : user.getUid());
        }
    }
}
