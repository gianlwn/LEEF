package com.itismob.s03.group7.leef.auth;

import android.util.Patterns;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.itismob.s03.group7.leef.R;

/** Checks the Create Account fields before anything is sent to Firebase. Each method returns 0 when valid. */
final class RegistrationValidator {

    static final int MIN_PASSWORD_LENGTH = 8;

    private RegistrationValidator() {
    }

    @StringRes
    static int validateFullName(@NonNull String fullName) {
        String value = fullName.trim();
        if (value.isEmpty()) {
            return R.string.auth_error_name_required;
        }
        return value.length() < 2 ? R.string.auth_error_name_short : 0;
    }

    @StringRes
    static int validateSchoolOrganization(@NonNull String schoolOrganization) {
        return schoolOrganization.trim().isEmpty() ? R.string.auth_error_school_required : 0;
    }

    @StringRes
    static int validateEmail(@NonNull String email) {
        String value = email.trim();
        if (value.isEmpty()) {
            return R.string.auth_error_email_required;
        }
        return Patterns.EMAIL_ADDRESS.matcher(value).matches() ? 0 : R.string.auth_error_email_invalid;
    }

    @StringRes
    static int validatePassword(@NonNull String password) {
        if (password.isEmpty()) {
            return R.string.auth_error_password_required;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return R.string.auth_error_password_short;
        }
        return meetsPasswordPolicy(password) ? 0 : R.string.auth_error_password_rules;
    }

    /** Password policy: an uppercase letter, a lowercase letter, a number and a symbol. */
    private static boolean meetsPasswordPolicy(@NonNull String password) {
        boolean upper = false;
        boolean lower = false;
        boolean digit = false;
        boolean symbol = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isUpperCase(c)) {
                upper = true;
            } else if (Character.isLowerCase(c)) {
                lower = true;
            } else if (Character.isDigit(c)) {
                digit = true;
            } else if (!Character.isWhitespace(c) && !Character.isLetter(c)) {
                symbol = true;
            }
        }
        return upper && lower && digit && symbol;
    }
}
