package com.itismob.s03.group7.leef.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.itismob.s03.group7.leef.R;

/**
 * The ways to pay that LEEF simulates. They are the ones students in the Philippines actually use,
 * but no real money moves: every one goes through the simulated gateway in the {@code payment} package.
 * To drop a method, delete its line here.
 */
public enum PaymentMethod {
    GCASH("gcash", R.string.pay_gcash, R.string.pay_gcash_hint, "art_pay_gcash", R.string.art_title_pay_gcash, true),
    MAYA("maya", R.string.pay_maya, R.string.pay_maya_hint, "art_pay_maya", R.string.art_title_pay_maya, true),
    CARD("card", R.string.pay_card, R.string.pay_card_hint, "art_pay_card", R.string.art_title_pay_card, false);

    private final String value;
    private final int labelRes;
    private final int hintRes;
    private final String artName;
    private final int artTitleRes;
    private final boolean wallet;

    PaymentMethod(@NonNull String value, @StringRes int labelRes, @StringRes int hintRes,
                  @NonNull String artName, @StringRes int artTitleRes, boolean wallet) {
        this.value = value;
        this.labelRes = labelRes;
        this.hintRes = hintRes;
        this.artName = artName;
        this.artTitleRes = artTitleRes;
        this.wallet = wallet;
    }

    /** How the method is stored in Firestore. */
    @NonNull
    public String getValue() {
        return value;
    }

    @StringRes
    public int getLabelRes() {
        return labelRes;
    }

    /** The one-line description under the name ("Pay with your GCash wallet"). */
    @StringRes
    public int getHintRes() {
        return hintRes;
    }

    /** File name of the method's logo, e.g. {@code art_pay_gcash}. */
    @NonNull
    public String getArtName() {
        return artName;
    }

    @StringRes
    public int getArtTitleRes() {
        return artTitleRes;
    }

    /** True for GCash and Maya, which ask for a mobile number instead of card details. */
    public boolean isWallet() {
        return wallet;
    }

    @Nullable
    public static PaymentMethod fromValue(@Nullable String value) {
        for (PaymentMethod method : values()) {
            if (method.value.equals(value)) {
                return method;
            }
        }
        return null;
    }
}
