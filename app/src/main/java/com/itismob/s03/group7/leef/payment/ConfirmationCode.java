package com.itismob.s03.group7.leef.payment;

import androidx.annotation.NonNull;

import java.security.SecureRandom;

/** Makes the short code on a registration ("LEEF-7KQ2MD"), without look-alike characters such as 0/O or 1/I. */
public final class ConfirmationCode {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private ConfirmationCode() {
    }

    @NonNull
    public static String generate() {
        StringBuilder code = new StringBuilder("LEEF-");
        for (int i = 0; i < LENGTH; i++) {
            code.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
