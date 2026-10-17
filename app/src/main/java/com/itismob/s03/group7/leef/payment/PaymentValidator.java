package com.itismob.s03.group7.leef.payment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.itismob.s03.group7.leef.R;

import java.util.Calendar;

/** Checks what was typed into the payment sheet. Each method returns a string resource for the error, or 0 when it's fine. */
public final class PaymentValidator {

    private PaymentValidator() {
    }

    /** Digits only: "0917 123 4567" and "+63 917 123 4567" both become plain digits. */
    @NonNull
    public static String digitsOnly(@Nullable String text) {
        return text == null ? "" : text.replaceAll("\\D", "");
    }

    /** A Philippine mobile number: 09XXXXXXXXX, 639XXXXXXXXX or +639XXXXXXXXX. */
    @StringRes
    public static int mobileError(@Nullable String text) {
        String digits = digitsOnly(text);
        boolean local = digits.length() == 11 && digits.startsWith("09");
        boolean international = digits.length() == 12 && digits.startsWith("639");
        return local || international ? 0 : R.string.pay_error_mobile;
    }

    /** 13 to 19 digits that pass the Luhn check, the way real card numbers do. */
    @StringRes
    public static int cardNumberError(@Nullable String text) {
        String digits = digitsOnly(text);
        if (digits.length() < 13 || digits.length() > 19 || !passesLuhn(digits)) {
            return R.string.pay_error_card_number;
        }
        return 0;
    }

    /** MM/YY (or MMYY), a real month, and not already past. */
    @StringRes
    public static int expiryError(@Nullable String text, @NonNull Calendar now) {
        String digits = digitsOnly(text);
        if (digits.length() != 4) {
            return R.string.pay_error_expiry;
        }
        int month = Integer.parseInt(digits.substring(0, 2));
        int year = 2000 + Integer.parseInt(digits.substring(2));
        if (month < 1 || month > 12) {
            return R.string.pay_error_expiry;
        }
        int nowYear = now.get(Calendar.YEAR);
        int nowMonth = now.get(Calendar.MONTH) + 1;
        if (year < nowYear || (year == nowYear && month < nowMonth)) {
            return R.string.pay_error_expiry_past;
        }
        return 0;
    }

    /** The 3 or 4 digit security code on the back of a card. */
    @StringRes
    public static int securityCodeError(@Nullable String text) {
        int length = digitsOnly(text).length();
        return length == 3 || length == 4 ? 0 : R.string.pay_error_cvc;
    }

    private static boolean passesLuhn(@NonNull String digits) {
        int sum = 0;
        boolean doubleIt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int digit = digits.charAt(i) - '0';
            if (doubleIt) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            doubleIt = !doubleIt;
        }
        return sum % 10 == 0;
    }
}
