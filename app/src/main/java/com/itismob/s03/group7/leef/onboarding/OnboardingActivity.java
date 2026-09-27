package com.itismob.s03.group7.leef.onboarding;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.databinding.ActivityOnboardingBinding;
import com.itismob.s03.group7.leef.ui.LeefEdgeToEdge;

import java.util.List;

/**
 * Screen 1 - Welcome / Onboarding. A warm introduction to LEEF followed by three pages on what the
 * app does, ending in Create Account. Returning users can jump straight to Log In from any page.
 */
public class OnboardingActivity extends AppCompatActivity {

    private static final long SKIP_FADE_MS = 180L;

    private ActivityOnboardingBinding binding;
    private List<OnboardingPage> pages;
    private OnboardingAdapter adapter;
    /** The layout starts with the arrow icon on the primary button. */
    private boolean arrowShown = true;

    /** System back steps to the previous page before leaving the screen. */
    private final OnBackPressedCallback previousPageCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            binding.pager.setCurrentItem(binding.pager.getCurrentItem() - 1);
        }
    };

    private final ViewPager2.OnPageChangeCallback pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            binding.dots.setPosition(position, positionOffset);
        }

        @Override
        public void onPageSelected(int position) {
            renderControls(position, true);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeefEdgeToEdge.enable(this);
        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        pages = OnboardingPage.defaultPages();
        adapter = new OnboardingAdapter(pages);
        binding.pager.setAdapter(adapter);
        binding.pager.setOffscreenPageLimit(1);
        binding.pager.setPageTransformer(new OnboardingPageTransformer());
        binding.pager.registerOnPageChangeCallback(pageChangeCallback);
        binding.dots.setDotCount(pages.size());

        binding.primaryButton.setOnClickListener(v -> onPrimaryAction());
        binding.skipButton.setOnClickListener(v -> binding.pager.setCurrentItem(pages.size() - 1));
        binding.loginRow.setOnClickListener(v -> openLogIn());
        getOnBackPressedDispatcher().addCallback(this, previousPageCallback);

        applyWindowInsets();
        renderControls(binding.pager.getCurrentItem(), false);
        // MaterialButton places a textEnd icon from its measured size, which it doesn't have on the
        // very first pass. Re-applying the label once laid out puts the arrow beside the text.
        binding.primaryButton.post(() -> binding.primaryButton.setText(binding.primaryButton.getText()));
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // ViewPager2 restores its page after onCreate; sync the controls once it has.
        binding.pager.post(() -> renderControls(binding.pager.getCurrentItem(), false));
    }

    @Override
    protected void onDestroy() {
        binding.pager.unregisterOnPageChangeCallback(pageChangeCallback);
        super.onDestroy();
    }

    private void onPrimaryAction() {
        int current = binding.pager.getCurrentItem();
        if (current < pages.size() - 1) {
            binding.pager.setCurrentItem(current + 1);
        } else {
            openCreateAccount();
        }
    }

    /** Updates the button label, Skip, back handling and the indicator for the given page. */
    private void renderControls(int position, boolean animate) {
        boolean isFirst = position == 0;
        boolean isLast = position == pages.size() - 1;

        previousPageCallback.setEnabled(!isFirst);
        setSkipVisible(!isFirst && !isLast, animate);

        binding.primaryButton.setText(isLast ? R.string.onboarding_create_account
                : isFirst ? R.string.onboarding_get_started : R.string.onboarding_next);
        // Only touch the icon when it changes: re-setting it before the button is measured
        // leaves the arrow at the far edge instead of beside the label.
        boolean showArrow = !isLast;
        if (showArrow != arrowShown) {
            if (showArrow) {
                binding.primaryButton.setIconResource(R.drawable.ic_arrow_forward);
            } else {
                binding.primaryButton.setIcon(null);
            }
            arrowShown = showArrow;
        }

        if (!animate) {
            binding.dots.setPosition(position, 0f);
        }
        binding.dots.setContentDescription(
                getString(R.string.onboarding_page_indicator, position + 1, pages.size()));
    }

    private void setSkipVisible(boolean visible, boolean animate) {
        View skip = binding.skipButton;
        skip.animate().cancel();
        if (!animate) {
            skip.setAlpha(visible ? 1f : 0f);
            skip.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
            return;
        }
        if (visible) {
            skip.setVisibility(View.VISIBLE);
            skip.animate().alpha(1f).setDuration(SKIP_FADE_MS).start();
        } else {
            skip.animate()
                    .alpha(0f)
                    .setDuration(SKIP_FADE_MS)
                    .withEndAction(() -> skip.setVisibility(View.INVISIBLE))
                    .start();
        }
    }

    /**
     * Artwork runs edge to edge behind the status bar; interactive controls stay clear of the
     * status bar, navigation bar and display cutouts.
     */
    private void applyWindowInsets() {
        View bottomBar = binding.bottomBar;
        int bottomBarBasePadding = bottomBar.getPaddingBottom();
        ViewGroup.MarginLayoutParams skipParams =
                (ViewGroup.MarginLayoutParams) binding.skipButton.getLayoutParams();
        int skipBaseTop = skipParams.topMargin;
        int skipBaseEnd = skipParams.getMarginEnd();

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (root, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

            adapter.setTopInset(bars.top);

            bottomBar.setPadding(
                    bottomBar.getPaddingLeft(),
                    bottomBar.getPaddingTop(),
                    bottomBar.getPaddingRight(),
                    bottomBarBasePadding + bars.bottom);

            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) binding.skipButton.getLayoutParams();
            params.topMargin = skipBaseTop + bars.top;
            params.setMarginEnd(skipBaseEnd + Math.max(bars.left, bars.right));
            binding.skipButton.setLayoutParams(params);

            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void openCreateAccount() {
        // TODO(Screen 2): startActivity(new Intent(this, CreateAccountActivity.class));
        Toast.makeText(this, R.string.todo_create_account, Toast.LENGTH_SHORT).show();
    }

    private void openLogIn() {
        // TODO(Screen 3): startActivity(new Intent(this, LogInActivity.class));
        Toast.makeText(this, R.string.todo_log_in, Toast.LENGTH_SHORT).show();
    }
}
