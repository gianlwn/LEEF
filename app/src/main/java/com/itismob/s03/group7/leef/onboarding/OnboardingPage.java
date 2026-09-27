package com.itismob.s03.group7.leef.onboarding;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.itismob.s03.group7.leef.R;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** One page of the Welcome / Onboarding pager. */
final class OnboardingPage {

    static final int TYPE_HERO = 0;
    static final int TYPE_FEATURE = 1;

    /*
     * Artwork file names. Add a PNG/WebP with exactly this name to res/drawable-nodpi/ and the page
     * shows it automatically; until then it shows a labeled placeholder.
     */
    static final String ART_WELCOME = "art_onboarding_welcome";
    static final String ART_DISCOVER = "art_onboarding_discover";
    static final String ART_REGISTER = "art_onboarding_register";
    static final String ART_CONNECT = "art_onboarding_connect";

    private final int type;
    @StringRes
    private final int titleRes;
    @StringRes
    private final int bodyRes;
    @NonNull
    private final String artName;
    @StringRes
    private final int artTitleRes;

    private OnboardingPage(int type, @StringRes int titleRes, @StringRes int bodyRes,
                           @NonNull String artName, @StringRes int artTitleRes) {
        this.type = type;
        this.titleRes = titleRes;
        this.bodyRes = bodyRes;
        this.artName = artName;
        this.artTitleRes = artTitleRes;
    }

    private static OnboardingPage hero() {
        return new OnboardingPage(TYPE_HERO, 0, 0, ART_WELCOME, R.string.art_title_onboarding_welcome);
    }

    private static OnboardingPage feature(@StringRes int titleRes, @StringRes int bodyRes,
                                          @NonNull String artName, @StringRes int artTitleRes) {
        return new OnboardingPage(TYPE_FEATURE, titleRes, bodyRes, artName, artTitleRes);
    }

    /** Welcome, then what LEEF helps you do: discover, register, connect. */
    static List<OnboardingPage> defaultPages() {
        return Collections.unmodifiableList(Arrays.asList(
                hero(),
                feature(R.string.onboarding_discover_title,
                        R.string.onboarding_discover_body,
                        ART_DISCOVER, R.string.art_title_onboarding_discover),
                feature(R.string.onboarding_join_title,
                        R.string.onboarding_join_body,
                        ART_REGISTER, R.string.art_title_onboarding_register),
                feature(R.string.onboarding_connect_title,
                        R.string.onboarding_connect_body,
                        ART_CONNECT, R.string.art_title_onboarding_connect)));
    }

    int getType() {
        return type;
    }

    @StringRes
    int getTitleRes() {
        return titleRes;
    }

    @StringRes
    int getBodyRes() {
        return bodyRes;
    }

    @NonNull
    String getArtName() {
        return artName;
    }

    @StringRes
    int getArtTitleRes() {
        return artTitleRes;
    }
}
