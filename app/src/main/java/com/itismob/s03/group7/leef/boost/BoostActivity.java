package com.itismob.s03.group7.leef.boost;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.databinding.ActivityBoostBinding;
import com.itismob.s03.group7.leef.databinding.ItemChoiceRowBinding;
import com.itismob.s03.group7.leef.home.EventFormat;
import com.itismob.s03.group7.leef.home.HomeViewModel;
import com.itismob.s03.group7.leef.model.BoostPlan;
import com.itismob.s03.group7.leef.model.BoostReceipt;
import com.itismob.s03.group7.leef.model.Event;
import com.itismob.s03.group7.leef.model.PaymentMethod;
import com.itismob.s03.group7.leef.payment.PaymentInput;
import com.itismob.s03.group7.leef.payment.PaymentSheetHost;
import com.itismob.s03.group7.leef.payment.PaymentSheetState;
import com.itismob.s03.group7.leef.registration.PaymentSheetFragment;
import com.itismob.s03.group7.leef.ui.ButtonSpinner;
import com.itismob.s03.group7.leef.ui.LeefEdgeToEdge;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Promote Event (the organizer's "Boost"): pick how long to feature the event (1 day, 3 days or
 * 1 week), pick a way to pay, then pay and promote. The payment is simulated; no money moves. Returns
 * {@link #RESULT_OK} once the event is boosted.
 */
public class BoostActivity extends AppCompatActivity implements PaymentSheetHost {

    private static final String EXTRA_EVENT_ID = "event_id";

    private ActivityBoostBinding binding;
    private BoostViewModel viewModel;
    private boolean spinnerShown;
    /** What the checkout sheet shows, kept in step with the boost state. */
    private final MediatorLiveData<PaymentSheetState> sheetState = new MediatorLiveData<>();

    @NonNull
    public static Intent createIntent(@NonNull Context context, @NonNull String eventId) {
        return new Intent(context, BoostActivity.class).putExtra(EXTRA_EVENT_ID, eventId);
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

        binding = ActivityBoostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets();

        viewModel = new ViewModelProvider(this, new BoostViewModel.Factory(eventId)).get(BoostViewModel.class);

        binding.backButton.setOnClickListener(v -> finish());
        binding.retryButton.setOnClickListener(v -> viewModel.retry());
        binding.unavailableBackButton.setOnClickListener(v -> finish());

        sheetState.addSource(viewModel.getState(), state ->
                sheetState.setValue(new PaymentSheetState(state.getMethod(), state.getTotal(), state.isPaying())));
        viewModel.getState().observe(this, this::render);
    }

    private void render(@NonNull BoostState state) {
        BoostState.Status status = state.getStatus();
        boolean form = status == BoostState.Status.FORM && state.getEvent() != null;
        boolean done = status == BoostState.Status.DONE && state.getReceipt() != null;
        boolean unavailable = status == BoostState.Status.NOT_FOUND
                || status == BoostState.Status.NOT_ORGANIZER
                || status == BoostState.Status.STARTED;

        binding.loading.setVisibility(status == BoostState.Status.LOADING ? View.VISIBLE : View.GONE);
        binding.errorGroup.setVisibility(status == BoostState.Status.ERROR ? View.VISIBLE : View.GONE);
        binding.unavailableGroup.setVisibility(unavailable ? View.VISIBLE : View.GONE);
        binding.formScroll.setVisibility(form ? View.VISIBLE : View.GONE);
        binding.doneScroll.setVisibility(done ? View.VISIBLE : View.GONE);
        binding.bottomBar.setVisibility(form || done ? View.VISIBLE : View.GONE);

        if (status == BoostState.Status.ERROR) {
            binding.errorText.setText(HomeViewModel.errorMessageFor(state.getFailure()));
        }
        if (unavailable) {
            bindUnavailable(status);
        }
        if (form) {
            bindForm(state, state.getEvent());
        }
        if (done) {
            setResult(RESULT_OK);
            dismissPaymentSheet();
            bindDone(state.getReceipt());
        }
    }

    private void bindUnavailable(@NonNull BoostState.Status status) {
        int title;
        int body;
        switch (status) {
            case NOT_ORGANIZER:
                title = R.string.boost_not_organizer_title;
                body = R.string.boost_not_organizer_body;
                break;
            case STARTED:
                title = R.string.boost_started_title;
                body = R.string.boost_started_body;
                break;
            default:
                title = R.string.registration_unavailable_title;
                body = R.string.registration_unavailable_body;
                break;
        }
        binding.unavailableTitle.setText(title);
        binding.unavailableBody.setText(body);
    }

    private void bindForm(@NonNull BoostState state, @NonNull Event event) {
        binding.eventTitle.setText(event.getTitle());
        String time = EventFormat.timeText(this, event);
        binding.eventWhen.setText(EventFormat.detailDateText(event) + (time == null ? "" : " · " + time));

        long activeUntil = state.getActiveUntilMillis();
        binding.boostStatus.setText(activeUntil > 0
                ? getString(R.string.boost_status_active, untilText(activeUntil))
                : getString(R.string.boost_status_none));

        // How long
        binding.planList.removeAllViews();
        boolean anyAllowed = false;
        for (BoostPlan plan : BoostPlan.values()) {
            boolean allowed = state.isAllowed(plan);
            anyAllowed |= allowed;
            ItemChoiceRowBinding row = inflateRow(binding.planList);
            row.title.setText(plan.getLabelRes());
            row.subtitle.setText(allowed ? getString(plan.getHintRes())
                    : getString(R.string.boost_plan_too_long, BoostPlan.MAX_DAYS_AHEAD));
            row.subtitle.setVisibility(View.VISIBLE);
            row.trailing.setText(EventFormat.priceText(this, plan.getPrice()));
            row.trailing.setVisibility(View.VISIBLE);
            row.getRoot().setSelected(allowed && plan == state.getPlan());
            row.getRoot().setAlpha(allowed ? 1f : 0.45f);
            row.getRoot().setOnClickListener(v -> viewModel.selectPlan(plan));
            binding.planList.addView(row.getRoot());
        }
        binding.maxedNote.setVisibility(anyAllowed ? View.GONE : View.VISIBLE);

        // Payment method
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
        binding.totalLabel.setText(R.string.boost_total);
        binding.totalAmount.setText(EventFormat.priceText(this, state.getTotal()));
        setButtonSpinner(false);
        binding.primaryButton.setEnabled(anyAllowed && !state.isPaying());
        binding.primaryButton.setText(R.string.boost_pay);
        binding.primaryButton.setOnClickListener(v -> showPaymentSheet());
    }

    private void bindDone(@NonNull BoostReceipt receipt) {
        binding.doneEvent.setText(receipt.getEventTitle());
        binding.doneUntil.setText(untilText(receipt.getBoostedUntilMillis()));
        binding.donePlan.setText(receipt.getPlan().getLabelRes());
        binding.donePaid.setText(getString(R.string.registration_paid_via,
                EventFormat.priceText(this, receipt.getAmount()),
                getString(receipt.getPaymentMethod().getLabelRes())));
        binding.doneCode.setText(receipt.getConfirmationCode());

        binding.totalRow.setVisibility(View.GONE);
        setButtonSpinner(false);
        binding.primaryButton.setEnabled(true);
        binding.primaryButton.setText(R.string.registration_done_button);
        binding.primaryButton.setOnClickListener(v -> finish());
    }

    /** "Oct 24, 3:00 PM". */
    @NonNull
    private static String untilText(long millis) {
        return new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(new Date(millis));
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

    // ---- the checkout sheet ----

    @NonNull
    @Override
    public LiveData<PaymentSheetState> getPaymentSheetState() {
        return sheetState;
    }

    @NonNull
    @Override
    public LiveData<Integer> getPaymentError() {
        return viewModel.getPaymentError();
    }

    @Override
    public void paymentErrorHandled() {
        viewModel.paymentErrorHandled();
    }

    @Override
    public void pay(@NonNull PaymentInput input) {
        viewModel.pay(input);
    }

    private void showPaymentSheet() {
        if (getSupportFragmentManager().findFragmentByTag(PaymentSheetFragment.TAG) == null) {
            new PaymentSheetFragment().show(getSupportFragmentManager(), PaymentSheetFragment.TAG);
        }
    }

    private void dismissPaymentSheet() {
        Fragment sheet = getSupportFragmentManager().findFragmentByTag(PaymentSheetFragment.TAG);
        if (sheet instanceof DialogFragment) {
            ((DialogFragment) sheet).dismissAllowingStateLoss();
        }
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.boostRoot, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
