package com.itismob.s03.group7.leef.registration;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.databinding.ActivityRegistrationBinding;
import com.itismob.s03.group7.leef.databinding.ItemChoiceRowBinding;
import com.itismob.s03.group7.leef.home.EventFormat;
import com.itismob.s03.group7.leef.home.HomeViewModel;
import com.itismob.s03.group7.leef.model.Event;
import com.itismob.s03.group7.leef.model.PaymentMethod;
import com.itismob.s03.group7.leef.model.Registration;
import com.itismob.s03.group7.leef.model.TicketType;
import com.itismob.s03.group7.leef.ui.ButtonSpinner;
import com.itismob.s03.group7.leef.ui.LeefEdgeToEdge;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen 7 - Registration &amp; Payment: pick a ticket type (Early Bird, Regular, Group 5+), pick a way
 * to pay (GCash, Maya or card), then pay and register. The payment is simulated; no money moves.
 * Returns {@link #RESULT_OK} once the person is registered.
 */
public class RegistrationActivity extends AppCompatActivity {

    private static final String EXTRA_EVENT_ID = "event_id";

    private ActivityRegistrationBinding binding;
    private RegistrationViewModel viewModel;
    private boolean spinnerShown;

    @NonNull
    public static Intent createIntent(@NonNull Context context, @NonNull String eventId) {
        return new Intent(context, RegistrationActivity.class).putExtra(EXTRA_EVENT_ID, eventId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeefEdgeToEdge.enable(this);

        String eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null || eventId.isEmpty()) {
            finish();
            return;
        }

        binding = ActivityRegistrationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets();

        viewModel = new ViewModelProvider(this, new RegistrationViewModel.Factory(eventId))
                .get(RegistrationViewModel.class);

        binding.backButton.setOnClickListener(v -> finish());
        binding.retryButton.setOnClickListener(v -> viewModel.retry());
        binding.unavailableBackButton.setOnClickListener(v -> finish());
        binding.groupMinus.setOnClickListener(v -> viewModel.changeGroupSize(-1));
        binding.groupPlus.setOnClickListener(v -> viewModel.changeGroupSize(1));

        viewModel.getState().observe(this, this::render);
        viewModel.getMessage().observe(this, messageRes -> {
            if (messageRes != null) {
                Toast.makeText(this, messageRes, Toast.LENGTH_LONG).show();
                viewModel.messageHandled();
            }
        });
    }

    private void render(@NonNull RegistrationState state) {
        RegistrationState.Status status = state.getStatus();
        boolean form = status == RegistrationState.Status.FORM && state.getEvent() != null;
        boolean done = status == RegistrationState.Status.DONE && state.getRegistration() != null;
        boolean unavailable = status == RegistrationState.Status.NOT_FOUND
                || status == RegistrationState.Status.CLOSED;

        binding.loading.setVisibility(status == RegistrationState.Status.LOADING ? View.VISIBLE : View.GONE);
        binding.errorGroup.setVisibility(status == RegistrationState.Status.ERROR ? View.VISIBLE : View.GONE);
        binding.unavailableGroup.setVisibility(unavailable ? View.VISIBLE : View.GONE);
        binding.formScroll.setVisibility(form ? View.VISIBLE : View.GONE);
        binding.doneScroll.setVisibility(done ? View.VISIBLE : View.GONE);
        binding.bottomBar.setVisibility(form || done ? View.VISIBLE : View.GONE);

        if (status == RegistrationState.Status.ERROR) {
            binding.errorText.setText(HomeViewModel.errorMessageFor(state.getFailure()));
        }
        if (unavailable) {
            boolean closed = status == RegistrationState.Status.CLOSED;
            binding.unavailableTitle.setText(closed ? R.string.registration_closed_title : R.string.registration_unavailable_title);
            binding.unavailableBody.setText(closed ? R.string.registration_closed_body : R.string.registration_unavailable_body);
        }
        if (form) {
            bindForm(state, state.getEvent());
        }
        if (done) {
            setResult(RESULT_OK);
            dismissPaymentSheet();
            bindDone(state, state.getRegistration());
        }
    }

    private void bindForm(@NonNull RegistrationState state, @NonNull Event event) {
        // The event being registered for
        binding.eventTitle.setText(event.getTitle());
        String time = EventFormat.timeText(this, event);
        binding.eventWhen.setText(EventFormat.detailDateText(event) + (time == null ? "" : " · " + time));
        String place = !event.getVenue().isEmpty() ? event.getVenue()
                : !event.getAddress().isEmpty() ? event.getAddress() : event.getCity();
        binding.eventWhere.setText(place);
        binding.eventWhere.setVisibility(place.isEmpty() ? View.GONE : View.VISIBLE);

        // Ticket type
        binding.ticketList.removeAllViews();
        for (TicketType type : state.getTickets()) {
            ItemChoiceRowBinding row = inflateRow(binding.ticketList);
            row.title.setText(type.getLabelRes());
            row.subtitle.setText(ticketHint(type, event));
            row.subtitle.setVisibility(View.VISIBLE);
            row.trailing.setText(EventFormat.priceText(this, state.priceOf(type)));
            row.trailing.setVisibility(View.VISIBLE);
            row.getRoot().setSelected(type == state.getTicket());
            row.getRoot().setOnClickListener(v -> viewModel.selectTicket(type));
            binding.ticketList.addView(row.getRoot());
        }

        // Group size
        boolean group = state.getTicket() == TicketType.GROUP;
        binding.groupRow.setVisibility(group ? View.VISIBLE : View.GONE);
        binding.groupCount.setText(String.valueOf(state.getGroupSize()));
        binding.groupHint.setText(getString(R.string.registration_group_hint,
                TicketType.GROUP_MIN_SIZE, TicketType.GROUP_MAX_SIZE));
        binding.groupMinus.setEnabled(state.getGroupSize() > TicketType.GROUP_MIN_SIZE && !state.isPaying());
        binding.groupPlus.setEnabled(state.getGroupSize() < TicketType.GROUP_MAX_SIZE && !state.isPaying());

        // Payment method (nothing to choose when it's free)
        boolean free = state.isFree();
        binding.paymentSection.setVisibility(free ? View.GONE : View.VISIBLE);
        binding.freeNote.setVisibility(free ? View.VISIBLE : View.GONE);
        binding.methodList.removeAllViews();
        for (PaymentMethod method : PaymentMethod.values()) {
            ItemChoiceRowBinding row = inflateRow(binding.methodList);
            row.logo.setArt(method.getArtName(), method.getArtTitleRes());
            row.logo.setVisibility(View.VISIBLE);
            row.title.setText(method.getLabelRes());
            row.subtitle.setText(method.getHintRes());
            row.subtitle.setVisibility(View.VISIBLE);
            row.getRoot().setSelected(method == state.getMethod());
            row.getRoot().setOnClickListener(v -> viewModel.selectMethod(method));
            binding.methodList.addView(row.getRoot());
        }

        // Total and the main button
        binding.totalRow.setVisibility(View.VISIBLE);
        binding.totalLabel.setText(group
                ? getString(R.string.registration_total_people, state.getQuantity())
                : getString(R.string.registration_total));
        binding.totalAmount.setText(EventFormat.priceText(this, state.getTotal()));
        boolean busy = state.isPaying() && free;
        binding.primaryButton.setEnabled(!state.isPaying());
        binding.primaryButton.setText(busy ? R.string.registration_registering
                : free ? R.string.registration_register_free : R.string.registration_pay_and_register);
        setButtonSpinner(busy);
        binding.primaryButton.setOnClickListener(v -> {
            if (free) {
                viewModel.registerFree();
            } else {
                showPaymentSheet();
            }
        });
    }

    private void bindDone(@NonNull RegistrationState state, @NonNull Registration registration) {
        binding.doneTitle.setText(state.isAlreadyRegistered()
                ? R.string.registration_done_already : R.string.registration_done_title);
        binding.doneEvent.setText(registration.getEventTitle());
        binding.doneCode.setText(registration.getConfirmationCode());

        String ticket = getString(registration.getTicketType().getLabelRes());
        binding.doneTicket.setText(registration.getQuantity() > 1
                ? getString(R.string.registration_ticket_line, ticket, registration.getQuantity()) : ticket);

        PaymentMethod method = registration.getPaymentMethod();
        if (registration.getAmount() <= 0) {
            binding.donePaid.setText(R.string.registration_done_free);
        } else {
            String amount = EventFormat.priceText(this, registration.getAmount());
            binding.donePaid.setText(method == null ? amount
                    : getString(R.string.registration_paid_via, amount, getString(method.getLabelRes())));
        }

        binding.totalRow.setVisibility(View.GONE);
        setButtonSpinner(false);
        binding.primaryButton.setEnabled(true);
        binding.primaryButton.setText(R.string.registration_done_button);
        binding.primaryButton.setOnClickListener(v -> finish());
    }

    private String ticketHint(@NonNull TicketType type, @NonNull Event event) {
        switch (type) {
            case EARLY_BIRD:
                return getString(R.string.ticket_early_bird_until,
                        EventFormat.shortDateText(event.getEarlyBirdUntilMillis()));
            case GROUP:
                return getString(R.string.ticket_group_hint);
            case REGULAR:
            default:
                return getString(R.string.ticket_regular_hint);
        }
    }

    @NonNull
    private ItemChoiceRowBinding inflateRow(@NonNull ViewGroup parent) {
        return ItemChoiceRowBinding.inflate(LayoutInflater.from(this), parent, false);
    }

    private void setButtonSpinner(boolean show) {
        if (show != spinnerShown) {
            binding.primaryButton.setIcon(show ? ButtonSpinner.create(this) : null);
            spinnerShown = show;
        }
    }

    private void showPaymentSheet() {
        if (getSupportFragmentManager().findFragmentByTag(PaymentSheetFragment.TAG) == null) {
            new PaymentSheetFragment().show(getSupportFragmentManager(), PaymentSheetFragment.TAG);
        }
    }

    private void dismissPaymentSheet() {
        androidx.fragment.app.Fragment sheet = getSupportFragmentManager().findFragmentByTag(PaymentSheetFragment.TAG);
        if (sheet instanceof DialogFragment) {
            ((DialogFragment) sheet).dismissAllowingStateLoss();
        }
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.registrationRoot, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
