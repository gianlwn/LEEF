package com.itismob.s03.group7.leef.registration;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.databinding.SheetPaymentBinding;
import com.itismob.s03.group7.leef.home.EventFormat;
import com.itismob.s03.group7.leef.model.PaymentMethod;
import com.itismob.s03.group7.leef.payment.PaymentInput;
import com.itismob.s03.group7.leef.payment.PaymentSheetHost;
import com.itismob.s03.group7.leef.payment.PaymentSheetState;
import com.itismob.s03.group7.leef.payment.PaymentValidator;
import com.itismob.s03.group7.leef.ui.ButtonSpinner;

import java.util.Calendar;

/**
 * The simulated checkout. A mobile number for GCash or Maya, card details for a card; "Pay" runs the
 * simulated gateway through the host Activity (registration or boost). Nothing typed here is saved or
 * logged.
 */
public class PaymentSheetFragment extends BottomSheetDialogFragment {

    public static final String TAG = "payment";

    private SheetPaymentBinding binding;
    private PaymentSheetHost host;
    @Nullable
    private PaymentMethod shownMethod;
    private boolean spinnerShown;

    @Override
    public int getTheme() {
        return R.style.ThemeOverlay_LEEF_BottomSheetDialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = SheetPaymentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!(requireActivity() instanceof PaymentSheetHost)) {
            dismissAllowingStateLoss();
            return;
        }
        host = (PaymentSheetHost) requireActivity();

        ViewCompat.setOnApplyWindowInsetsListener(binding.sheetRoot, (root, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            root.setPadding(0, 0, 0, Math.max(bars.bottom, ime.bottom));
            return windowInsets;
        });

        binding.cardNumberInput.addTextChangedListener(new GroupingWatcher(binding.cardNumberInput, 19, 4, ' ', false));
        binding.expiryInput.addTextChangedListener(new GroupingWatcher(binding.expiryInput, 4, 2, '/', true));
        clearErrorOnEdit(binding.mobileInput, binding.mobileLayout);
        clearErrorOnEdit(binding.cvcInput, binding.cvcLayout);
        binding.sheetPayButton.setOnClickListener(v -> submit());
        binding.mobileInput.setOnEditorActionListener((v, actionId, event) -> {
            submit();
            return true;
        });
        binding.cvcInput.setOnEditorActionListener((v, actionId, event) -> {
            submit();
            return true;
        });

        host.getPaymentSheetState().observe(getViewLifecycleOwner(), this::render);
        host.getPaymentError().observe(getViewLifecycleOwner(), errorRes -> {
            if (errorRes != null) {
                binding.sheetError.setText(errorRes);
                binding.sheetError.setVisibility(View.VISIBLE);
                host.paymentErrorHandled();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Open fully so the fields and the Pay button are in view.
        if (getDialog() instanceof BottomSheetDialog) {
            BottomSheetBehavior<FrameLayout> behavior = ((BottomSheetDialog) getDialog()).getBehavior();
            behavior.setSkipCollapsed(true);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void render(@NonNull PaymentSheetState state) {
        PaymentMethod method = state.getMethod();
        if (method != shownMethod) {
            shownMethod = method;
            binding.sheetTitle.setText(getString(R.string.pay_sheet_title, getString(method.getLabelRes())));
            binding.walletGroup.setVisibility(method.isWallet() ? View.VISIBLE : View.GONE);
            binding.cardGroup.setVisibility(method.isWallet() ? View.GONE : View.VISIBLE);
            binding.sheetTestHint.setText(method.isWallet() ? R.string.pay_test_hint_wallet : R.string.pay_test_hint_card);
        }
        String amount = EventFormat.priceText(requireContext(), state.getTotal());
        binding.sheetAmount.setText(amount);

        boolean paying = state.isPaying();
        binding.sheetPayButton.setEnabled(!paying);
        binding.sheetPayButton.setText(paying
                ? getString(R.string.pay_sheet_processing) : getString(R.string.pay_sheet_button, amount));
        if (paying != spinnerShown) {
            binding.sheetPayButton.setIcon(paying ? ButtonSpinner.create(requireContext()) : null);
            spinnerShown = paying;
        }
        for (EditText field : new EditText[]{binding.mobileInput, binding.cardNumberInput,
                binding.expiryInput, binding.cvcInput}) {
            field.setEnabled(!paying);
        }
        if (paying) {
            binding.sheetError.setVisibility(View.GONE);
        }
    }

    /** A field's error goes away as soon as the person starts fixing it. */
    private static void clearErrorOnEdit(@NonNull EditText field,
                                         @NonNull com.google.android.material.textfield.TextInputLayout layout) {
        field.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                layout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void submit() {
        if (shownMethod == null || !binding.sheetPayButton.isEnabled()) {
            return;
        }
        PaymentInput input = shownMethod.isWallet() ? walletInput() : cardInput();
        if (input != null) {
            binding.sheetError.setVisibility(View.GONE);
            host.pay(input);
        }
    }

    @Nullable
    private PaymentInput walletInput() {
        String mobile = text(binding.mobileInput);
        int error = PaymentValidator.mobileError(mobile);
        binding.mobileLayout.setError(error == 0 ? null : getString(error));
        return error == 0 ? PaymentInput.wallet(shownMethod, mobile) : null;
    }

    @Nullable
    private PaymentInput cardInput() {
        String number = text(binding.cardNumberInput);
        String expiry = text(binding.expiryInput);
        String code = text(binding.cvcInput);
        int numberError = PaymentValidator.cardNumberError(number);
        int expiryError = PaymentValidator.expiryError(expiry, Calendar.getInstance());
        int codeError = PaymentValidator.securityCodeError(code);
        binding.cardNumberLayout.setError(numberError == 0 ? null : getString(numberError));
        binding.expiryLayout.setError(expiryError == 0 ? null : getString(expiryError));
        binding.cvcLayout.setError(codeError == 0 ? null : getString(codeError));
        if (numberError != 0 || expiryError != 0 || codeError != 0) {
            return null;
        }
        return PaymentInput.card(PaymentValidator.digitsOnly(number), expiry, code);
    }

    @NonNull
    private static String text(@NonNull EditText field) {
        Editable text = field.getText();
        return text == null ? "" : text.toString().trim();
    }

    /**
     * Re-formats what's typed into digit groups: "4242 4242 4242 4242" or "12/28". With
     * {@code separatorAfterGroup} the separator appears as soon as a group is complete ("12" becomes
     * "12/"), but not while the person is deleting, so backspace still works.
     */
    private static final class GroupingWatcher implements TextWatcher {
        private final EditText field;
        private final int maxDigits;
        private final int groupSize;
        private final char separator;
        private final boolean separatorAfterGroup;
        private boolean editing;
        private boolean deleting;

        GroupingWatcher(@NonNull EditText field, int maxDigits, int groupSize, char separator,
                        boolean separatorAfterGroup) {
            this.field = field;
            this.maxDigits = maxDigits;
            this.groupSize = groupSize;
            this.separator = separator;
            this.separatorAfterGroup = separatorAfterGroup;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            deleting = before > count;
        }

        @Override
        public void afterTextChanged(Editable editable) {
            if (editing) {
                return;
            }
            String digits = PaymentValidator.digitsOnly(editable.toString());
            if (digits.length() > maxDigits) {
                digits = digits.substring(0, maxDigits);
            }
            StringBuilder formatted = new StringBuilder();
            for (int i = 0; i < digits.length(); i++) {
                if (i > 0 && i % groupSize == 0) {
                    formatted.append(separator);
                }
                formatted.append(digits.charAt(i));
            }
            boolean groupComplete = digits.length() > 0 && digits.length() % groupSize == 0
                    && digits.length() < maxDigits;
            if (separatorAfterGroup && groupComplete && !deleting) {
                formatted.append(separator);
            }
            if (!formatted.toString().equals(editable.toString())) {
                editing = true;
                editable.replace(0, editable.length(), formatted);
                editing = false;
            }
            // Clear a shown error as soon as the person edits the field.
            com.google.android.material.textfield.TextInputLayout layout = layoutOf(field);
            if (layout != null) {
                layout.setError(null);
            }
        }

        @Nullable
        private static com.google.android.material.textfield.TextInputLayout layoutOf(@NonNull View view) {
            android.view.ViewParent parent = view.getParent();
            while (parent != null) {
                if (parent instanceof com.google.android.material.textfield.TextInputLayout) {
                    return (com.google.android.material.textfield.TextInputLayout) parent;
                }
                parent = parent.getParent();
            }
            return null;
        }
    }
}
