package com.itismob.s03.group7.leef.legal;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.itismob.s03.group7.leef.R;

/** The two documents a person agrees to when they sign up. */
public enum LegalDocument {
    TERMS(R.string.auth_terms_of_service, R.array.terms_headings, R.array.terms_bodies),
    PRIVACY(R.string.auth_privacy_policy, R.array.privacy_headings, R.array.privacy_bodies);

    @StringRes
    private final int titleRes;
    @ArrayRes
    private final int headingsRes;
    @ArrayRes
    private final int bodiesRes;

    LegalDocument(@StringRes int titleRes, @ArrayRes int headingsRes, @ArrayRes int bodiesRes) {
        this.titleRes = titleRes;
        this.headingsRes = headingsRes;
        this.bodiesRes = bodiesRes;
    }

    @StringRes
    public int getTitleRes() {
        return titleRes;
    }

    @ArrayRes
    public int getHeadingsRes() {
        return headingsRes;
    }

    @ArrayRes
    public int getBodiesRes() {
        return bodiesRes;
    }

    @NonNull
    public static LegalDocument fromName(String name) {
        for (LegalDocument document : values()) {
            if (document.name().equals(name)) {
                return document;
            }
        }
        return TERMS;
    }
}
