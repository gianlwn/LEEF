package com.itismob.s03.group7.leef.checkin;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.databinding.ActivityCheckInBinding;
import com.itismob.s03.group7.leef.details.EventDetailsActivity;
import com.itismob.s03.group7.leef.home.EventFormat;
import com.itismob.s03.group7.leef.home.HomeViewModel;
import com.itismob.s03.group7.leef.model.Event;
import com.itismob.s03.group7.leef.model.Registration;
import com.itismob.s03.group7.leef.reminder.ReminderSync;
import com.itismob.s03.group7.leef.ui.LeefEdgeToEdge;

/**
 * Screen 9 - Check-in: the person's ticket for one event, with a QR code to show at the venue, their
 * name and their confirmation code. The reminder notification opens this screen. The screen is
 * brightened while the code is showing so it scans easily.
 */
public class CheckInActivity extends AppCompatActivity {

    private static final String EXTRA_EVENT_ID = "event_id";
    private static final int QR_SIZE_DP = 232;

    private ActivityCheckInBinding binding;
    private CheckInViewModel viewModel;
    private String eventId;
    private boolean qrShowing;
    @Nullable
    private String drawnPayload;

    @NonNull
    public static Intent createIntent(@NonNull Context context, @NonNull String eventId) {
        return new Intent(context, CheckInActivity.class).putExtra(EXTRA_EVENT_ID, eventId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeefEdgeToEdge.enable(this);

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null || eventId.isEmpty()) {
            finish();
            return;
        }

        binding = ActivityCheckInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets();

        viewModel = new ViewModelProvider(this, new CheckInViewModel.Factory(eventId)).get(CheckInViewModel.class);

        binding.backButton.setOnClickListener(v -> finish());
        binding.retryButton.setOnClickListener(v -> viewModel.retry());
        binding.endedBackButton.setOnClickListener(v -> finish());
        binding.viewEventButton.setOnClickListener(v -> {
            startActivity(EventDetailsActivity.createIntent(this, eventId));
            finish();
        });

        viewModel.getState().observe(this, this::render);
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyBrightness();
    }

    @Override
    protected void onPause() {
        super.onPause();
        setBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE);
    }

    private void render(@NonNull CheckInState state) {
        CheckInState.Status status = state.getStatus();
        Registration registration = state.getRegistration();
        boolean content = status == CheckInState.Status.CONTENT && registration != null;
        boolean ticket = content && !state.isEnded();

        binding.loading.setVisibility(status == CheckInState.Status.LOADING ? View.VISIBLE : View.GONE);
        binding.errorGroup.setVisibility(status == CheckInState.Status.ERROR ? View.VISIBLE : View.GONE);
        binding.notRegisteredGroup.setVisibility(status == CheckInState.Status.NOT_REGISTERED ? View.VISIBLE : View.GONE);
        binding.endedGroup.setVisibility(content && state.isEnded() ? View.VISIBLE : View.GONE);
        boolean cancelled = content && state.getEvent() != null && state.getEvent().isCancelled();
        binding.endedIcon.setImageResource(cancelled ? R.drawable.ic_close : R.drawable.ic_check_circle);
        binding.endedTitle.setText(cancelled ? R.string.check_in_cancelled_title : R.string.check_in_ended_title);
        binding.endedBody.setText(cancelled ? R.string.check_in_cancelled_body : R.string.check_in_ended_body);
        binding.ticketScroll.setVisibility(ticket ? View.VISIBLE : View.GONE);

        if (status == CheckInState.Status.ERROR) {
            binding.errorText.setText(HomeViewModel.errorMessageFor(state.getFailure()));
        }
        if (ticket) {
            bindTicket(state, registration);
        }
        qrShowing = ticket;
        applyBrightness();
    }

    private void bindTicket(@NonNull CheckInState state, @NonNull Registration registration) {
        Event event = state.getEvent();

        // The event
        // The event's own title and time win: the organizer may have edited them since the person registered.
        binding.eventTitle.setText(event != null ? event.getTitle() : registration.getEventTitle());
        binding.eventWhen.setText(EventFormat.dateTimeLine(event,
                event != null ? event.getStartsAtMillis() : registration.getEventStartsAtMillis()));
        String place = event == null ? "" : ReminderSync.placeOf(event);
        binding.eventWhere.setText(place);
        binding.eventWhere.setVisibility(place.isEmpty() ? View.GONE : View.VISIBLE);

        // The QR code (drawn once per code)
        String payload = QrCodes.checkInPayload(registration.getConfirmationCode(), registration.getEventId());
        if (!payload.equals(drawnPayload)) {
            int sizePx = Math.round(QR_SIZE_DP * getResources().getDisplayMetrics().density);
            Bitmap qr = QrCodes.render(payload, sizePx,
                    ContextCompat.getColor(this, R.color.leef_ink), ContextCompat.getColor(this, R.color.white));
            binding.qrImage.setImageBitmap(qr);
            drawnPayload = qr == null ? null : payload;
        }

        // The person
        String name = state.getName();
        binding.attendeeName.setText(name == null ? "" : name);
        binding.attendeeName.setVisibility(name == null || name.isEmpty() ? View.GONE : View.VISIBLE);
        binding.confirmationCode.setText(getString(R.string.check_in_code, registration.getConfirmationCode()));

        String ticketName = getString(registration.getTicketType().getLabelRes());
        binding.ticketLine.setText(registration.getQuantity() > 1
                ? getString(R.string.registration_ticket_line, ticketName, registration.getQuantity()) : ticketName);
    }

    private void applyBrightness() {
        setBrightness(qrShowing ? WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                : WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE);
    }

    private void setBrightness(float brightness) {
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        if (attributes.screenBrightness != brightness) {
            attributes.screenBrightness = brightness;
            getWindow().setAttributes(attributes);
        }
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.checkInRoot, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
