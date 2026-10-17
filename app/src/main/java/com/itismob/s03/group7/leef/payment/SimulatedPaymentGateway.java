package com.itismob.s03.group7.leef.payment;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.itismob.s03.group7.leef.R;

/**
 * A pretend payment gateway: it waits a moment like a real one, then approves or declines. No money
 * moves and nothing is sent anywhere.
 *
 * <p>Test values, shown to the person on the payment sheet:
 * <ul>
 *   <li>GCash or Maya: a mobile number ending in 0000 is declined (not enough balance); any other valid number is approved.</li>
 *   <li>Card: 4000 0000 0000 0002 is declined; any other valid card number (such as 4242 4242 4242 4242) is approved.</li>
 * </ul>
 */
public final class SimulatedPaymentGateway implements PaymentGateway {

    private static final long PROCESSING_MILLIS = 1800;
    private static final String DECLINED_CARD = "4000000000000002";
    private static final String DECLINED_MOBILE_ENDING = "0000";

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void pay(@NonNull PaymentInput input, double amountPesos, @NonNull Callback callback) {
        PaymentResult result = decide(input);
        handler.postDelayed(() -> callback.onResult(result), PROCESSING_MILLIS);
    }

    @NonNull
    static PaymentResult decide(@NonNull PaymentInput input) {
        if (input.getMethod().isWallet()) {
            String mobile = PaymentValidator.digitsOnly(input.getMobileNumber());
            if (mobile.endsWith(DECLINED_MOBILE_ENDING)) {
                return PaymentResult.declined(R.string.pay_declined_wallet);
            }
            return PaymentResult.approved();
        }
        if (DECLINED_CARD.equals(PaymentValidator.digitsOnly(input.getCardNumber()))) {
            return PaymentResult.declined(R.string.pay_declined_card);
        }
        return PaymentResult.approved();
    }
}
