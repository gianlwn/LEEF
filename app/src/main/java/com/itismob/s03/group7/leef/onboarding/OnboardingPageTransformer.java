package com.itismob.s03.group7.leef.onboarding;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

import com.itismob.s03.group7.leef.R;

/**
 * Gentle parallax: each page's background slides at full speed while its text and illustration
 * trail behind and fade, so swiping feels layered rather than flat.
 */
final class OnboardingPageTransformer implements ViewPager2.PageTransformer {

    private static final float CONTENT_PARALLAX = 0.35f;
    private static final float CONTENT_FADE = 0.8f;

    @Override
    public void transformPage(@NonNull View page, float position) {
        View content = page.findViewById(R.id.page_content);
        if (content == null) {
            return;
        }
        if (position <= -1f || position >= 1f) {
            content.setTranslationX(0f);
            content.setAlpha(1f);
            return;
        }
        content.setTranslationX(-position * page.getWidth() * CONTENT_PARALLAX);
        content.setAlpha(1f - Math.abs(position) * CONTENT_FADE);
    }
}
