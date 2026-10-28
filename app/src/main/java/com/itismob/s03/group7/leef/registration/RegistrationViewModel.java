package com.itismob.s03.group7.leef.registration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.data.EventRepository;
import com.itismob.s03.group7.leef.data.FirebaseEventRepository;
import com.itismob.s03.group7.leef.data.FirebaseRegistrationRepository;
import com.itismob.s03.group7.leef.data.RegistrationRepository;
import com.itismob.s03.group7.leef.model.Event;
import com.itismob.s03.group7.leef.model.PaymentMethod;
import com.itismob.s03.group7.leef.model.Registration;
import com.itismob.s03.group7.leef.model.TicketType;
import com.itismob.s03.group7.leef.payment.ConfirmationCode;
import com.itismob.s03.group7.leef.payment.PaymentGateway;
import com.itismob.s03.group7.leef.payment.PaymentInput;
import com.itismob.s03.group7.leef.payment.PaymentResult;
import com.itismob.s03.group7.leef.payment.SimulatedPaymentGateway;

import java.util.Collections;
import java.util.List;

/**
 * Screen 7 - Registration &amp; Payment. Loads the event (and whether the person already registered),
 * keeps the chosen ticket, group size and payment method, runs the simulated payment, and saves the
 * registration.
 */
public class RegistrationViewModel extends ViewModel {

    private final EventRepository events;
    private final RegistrationRepository registrations;
    private final PaymentGateway gateway;
    private final String eventId;

    private final MutableLiveData<RegistrationState> state = new MutableLiveData<>();
    private final MutableLiveData<Integer> message = new MutableLiveData<>();
    private final MutableLiveData<Integer> paymentError = new MutableLiveData<>();

    @Nullable
    private Event event;
    @Nullable
    private EventRepository.Failure failure;
    private boolean loading;
    private List<TicketType> tickets = Collections.emptyList();
    private TicketType ticket = TicketType.REGULAR;
    private int groupSize = TicketType.GROUP_MIN_SIZE;
    private PaymentMethod method = PaymentMethod.GCASH;
    private boolean paying;
    @Nullable
    private Registration registration;
    private boolean alreadyRegistered;
    /** Set once a payment was approved but saving the registration failed, so a retry doesn't charge again. */
    @Nullable
    private String approvedCode;

    RegistrationViewModel(@NonNull EventRepository events, @NonNull RegistrationRepository registrations,
                          @NonNull PaymentGateway gateway, @NonNull String eventId) {
        this.events = events;
        this.registrations = registrations;
        this.gateway = gateway;
        this.eventId = eventId;
        load();
    }

    @NonNull
    public LiveData<RegistrationState> getState() {
        return state;
    }

    /** A toast message (string resource id). Call {@link #messageHandled()} after showing it. */
    @NonNull
    public LiveData<Integer> getMessage() {
        return message;
    }

    public void messageHandled() {
        message.setValue(null);
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

    public void selectTicket(@NonNull TicketType type) {
        if (!canEdit() || !tickets.contains(type)) {
            return;
        }
        ticket = type;
        publish();
    }

    /** Group size steps by one, between 5 and 50 people. */
    public void changeGroupSize(int delta) {
        if (!canEdit()) {
            return;
        }
        groupSize = Math.max(TicketType.GROUP_MIN_SIZE, Math.min(TicketType.GROUP_MAX_SIZE, groupSize + delta));
        publish();
    }

    public void selectMethod(@NonNull PaymentMethod chosen) {
        if (!canEdit()) {
            return;
        }
        method = chosen;
        publish();
    }

    /** Registers for an event that costs nothing (no payment). */
    public void registerFree() {
        if (event == null || paying || approvedCode != null || currentTotal() > 0) {
            return;
        }
        paying = true;
        publish();
        save(null, ConfirmationCode.generate(), false);
    }

    /** Pays through the simulated gateway, then saves the registration. */
    public void pay(@NonNull PaymentInput input) {
        if (event == null || paying) {
            return;
        }
        paying = true;
        publish();
        if (approvedCode != null) {
            // Already paid; only the saving failed last time.
            save(method, approvedCode, true);
            return;
        }
        gateway.pay(input, currentTotal(), new PaymentGateway.Callback() {
            @Override
            public void onResult(@NonNull PaymentResult result) {
                if (!result.isApproved()) {
                    paying = false;
                    publish();
                    paymentError.setValue(result.getDeclineMessageRes());
                    return;
                }
                approvedCode = ConfirmationCode.generate();
                save(input.getMethod(), approvedCode, true);
            }
        });
    }

    private boolean canEdit() {
        return event != null && state.getValue() != null
                && state.getValue().getStatus() == RegistrationState.Status.FORM
                && !paying && approvedCode == null;
    }

    private double currentTotal() {
        return event == null ? 0 : TicketPricing.total(event, ticket, groupSize);
    }

    private void save(@Nullable PaymentMethod paidWith, @NonNull String code, boolean afterPayment) {
        Event current = event;
        if (current == null) {
            return;
        }
        Registration toSave = new Registration(current.getId(), current.getTitle(), current.getStartsAtMillis(),
                ticket, TicketPricing.quantity(ticket, groupSize), currentTotal(), paidWith, code);
        registrations.saveRegistration(toSave, new RegistrationRepository.SaveCallback() {
            @Override
            public void onSaved() {
                registration = toSave;
                paying = false;
                approvedCode = null;
                publish();
            }

            @Override
            public void onFailure(@NonNull RegistrationRepository.Failure reason) {
                paying = false;
                publish();
                int text = failureText(reason, afterPayment);
                if (afterPayment) {
                    paymentError.setValue(text);
                } else {
                    message.setValue(text);
                }
            }
        });
    }

    private static int failureText(@NonNull RegistrationRepository.Failure reason, boolean afterPayment) {
        if (reason == RegistrationRepository.Failure.PERMISSION) {
            return R.string.registration_error_permission;
        }
        if (reason == RegistrationRepository.Failure.NETWORK) {
            return afterPayment ? R.string.registration_error_network_paid : R.string.registration_error_network;
        }
        return afterPayment ? R.string.registration_error_generic_paid : R.string.registration_error_generic;
    }

    private void load() {
        if (loading) {
            return;
        }
        loading = true;
        failure = null;
        publish();

        final int[] pending = {2};
        final Event[] loadedEvent = {null};
        final Registration[] existing = {null};
        final EventRepository.Failure[] eventFailure = {null};

        Runnable finish = () -> {
            if (--pending[0] > 0) {
                return;
            }
            loading = false;
            if (loadedEvent[0] != null) {
                event = loadedEvent[0];
                failure = null;
                long now = System.currentTimeMillis();
                tickets = TicketPricing.available(event, now);
                ticket = TicketPricing.defaultTicket(event, now);
                if (existing[0] != null) {
                    registration = existing[0];
                    alreadyRegistered = true;
                }
            } else {
                failure = eventFailure[0] == null ? EventRepository.Failure.UNKNOWN : eventFailure[0];
            }
            publish();
        };

        events.loadEvent(eventId, new EventRepository.EventCallback() {
            @Override
            public void onLoaded(@NonNull Event result) {
                loadedEvent[0] = result;
                finish.run();
            }

            @Override
            public void onFailure(@NonNull EventRepository.Failure reason) {
                eventFailure[0] = reason;
                finish.run();
            }
        });
        registrations.loadRegistration(eventId, new RegistrationRepository.LoadCallback() {
            @Override
            public void onLoaded(@Nullable Registration result) {
                existing[0] = result;
                finish.run();
            }

            @Override
            public void onFailure(@NonNull RegistrationRepository.Failure reason) {
                // Not knowing is fine: saving later is refused if they already registered.
                finish.run();
            }
        });
    }

    private void publish() {
        RegistrationState.Status status;
        if (registration != null) {
            status = RegistrationState.Status.DONE;
        } else if (event != null) {
            status = event.isCancelled() || event.hasEnded(System.currentTimeMillis())
                    ? RegistrationState.Status.CLOSED : RegistrationState.Status.FORM;
        } else if (loading || failure == null) {
            status = RegistrationState.Status.LOADING;
        } else if (failure == EventRepository.Failure.NOT_FOUND) {
            status = RegistrationState.Status.NOT_FOUND;
        } else {
            status = RegistrationState.Status.ERROR;
        }
        state.setValue(new RegistrationState(status, event, failure, tickets, ticket, groupSize, method,
                paying, registration, alreadyRegistered));
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
            return (T) new RegistrationViewModel(new FirebaseEventRepository(),
                    new FirebaseRegistrationRepository(), new SimulatedPaymentGateway(), eventId);
        }
    }
}
