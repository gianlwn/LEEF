package com.itismob.s03.group7.leef.organizer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.databinding.ActivityScanCheckInBinding;
import com.itismob.s03.group7.leef.model.Attendee;
import com.itismob.s03.group7.leef.ui.ButtonSpinner;
import com.itismob.s03.group7.leef.ui.LeefEdgeToEdge;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.journeyapps.barcodescanner.ScanOptions;

import java.text.DateFormat;
import java.util.Date;

/**
 * Check-in at the door (organizer): scan an attendee's ticket QR with the camera, or type the
 * confirmation code from their phone. Shows who it was and whether they were just checked in or had
 * already been.
 */
public class ScanCheckInActivity extends AppCompatActivity {

    private static final String EXTRA_EVENT_ID = "event_id";

    private ActivityScanCheckInBinding binding;
    private ScanCheckInViewModel viewModel;
    private ActivityResultLauncher<ScanOptions> scanner;
    private boolean spinnerShown;

    @NonNull
    public static Intent createIntent(@NonNull Context context, @NonNull String eventId) {
        return new Intent(context, ScanCheckInActivity.class).putExtra(EXTRA_EVENT_ID, eventId);
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
        binding = ActivityScanCheckInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets();

        viewModel = new ViewModelProvider(this, new ScanCheckInViewModel.Factory(eventId))
                .get(ScanCheckInViewModel.class);

        // The library asks for camera permission itself the first time the scanner opens.
        scanner = registerForActivityResult(new ScanContract(), (ScanIntentResult result) -> {
            if (result.getContents() != null) {
                viewModel.submit(result.getContents());
            }
        });

        binding.backButton.setOnClickListener(v -> finish());
        binding.scanButton.setOnClickListener(v -> openScanner());
        binding.codeSubmitButton.setOnClickListener(v -> submitTypedCode());
        binding.codeInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitTypedCode();
                return true;
            }
            return false;
        });

        viewModel.getState().observe(this, this::render);
    }

    private void openScanner() {
        ScanOptions options = new ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt(getString(R.string.scan_prompt))
                .setBeepEnabled(false)
                .setOrientationLocked(true)
                .setCaptureActivity(ScannerCaptureActivity.class);
        scanner.launch(options);
    }

    private void submitTypedCode() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(binding.codeInput.getWindowToken(), 0);
        }
        viewModel.submit(binding.codeInput.getText() == null ? "" : binding.codeInput.getText().toString());
    }

    private void render(@NonNull ScanCheckInState state) {
        boolean busy = state.isBusy();
        binding.scanButton.setEnabled(!busy);
        binding.codeSubmitButton.setEnabled(!busy);
        binding.codeInput.setEnabled(!busy);
        if (busy != spinnerShown) {
            binding.codeSubmitButton.setIcon(busy ? ButtonSpinner.create(this) : null);
            spinnerShown = busy;
        }
        binding.codeSubmitButton.setText(busy ? R.string.scan_looking_up : R.string.scan_submit);

        ScanCheckInState.Result result = state.getResult();
        boolean show = result != ScanCheckInState.Result.NONE && !busy;
        binding.resultCard.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) {
            return;
        }

        Attendee attendee = state.getAttendee();
        boolean good = result == ScanCheckInState.Result.CHECKED_IN;
        boolean already = result == ScanCheckInState.Result.ALREADY_CHECKED_IN;
        int title;
        int body;
        switch (result) {
            case CHECKED_IN:
                title = R.string.scan_result_checked_in;
                body = 0;
                break;
            case ALREADY_CHECKED_IN:
                title = R.string.scan_result_already;
                body = 0;
                break;
            case NOT_FOUND:
                title = R.string.scan_result_not_found;
                body = R.string.scan_result_not_found_body;
                break;
            case WRONG_EVENT:
                title = R.string.scan_result_wrong_event;
                body = R.string.scan_result_wrong_event_body;
                break;
            case UNREADABLE:
                title = R.string.scan_result_unreadable;
                body = R.string.scan_result_unreadable_body;
                break;
            case ERROR:
            default:
                title = R.string.scan_result_error;
                body = state.getErrorRes();
                break;
        }
        binding.resultIcon.setImageResource(good ? R.drawable.ic_check_circle
                : already ? R.drawable.ic_status_saved : R.drawable.ic_state_cloud_off);
        binding.resultTitle.setText(title);
        binding.resultTitle.setTextColor(androidx.core.content.ContextCompat.getColor(this,
                good ? R.color.leef_green_700 : already ? R.color.leef_gray_700 : R.color.leef_error));

        if (attendee != null) {
            String name = attendee.getName().isEmpty() ? getString(R.string.attendees_unnamed) : attendee.getName();
            String ticket = getString(attendee.getTicketType().getLabelRes());
            String detail = attendee.getQuantity() > 1
                    ? getString(R.string.attendees_ticket_people, ticket, attendee.getQuantity()) : ticket;
            binding.resultBody.setText(already
                    ? getString(R.string.scan_result_already_body, name, detail,
                    DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(attendee.getCheckedInAtMillis())))
                    : getString(R.string.scan_result_person, name, detail, attendee.getConfirmationCode()));
        } else {
            binding.resultBody.setText(body);
        }
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.scanRoot, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
