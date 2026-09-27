package com.itismob.s03.group7.leef.onboarding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.databinding.ItemOnboardingFeatureBinding;
import com.itismob.s03.group7.leef.databinding.ItemOnboardingHeroBinding;
import com.itismob.s03.group7.leef.ui.ArtAssets;

import java.util.List;

/** Binds {@link OnboardingPage}s into the onboarding ViewPager2. */
final class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.PageHolder> {

    private static final Object PAYLOAD_TOP_INSET = new Object();

    private final List<OnboardingPage> pages;
    private int topInset;

    OnboardingAdapter(@NonNull List<OnboardingPage> pages) {
        this.pages = pages;
    }

    /** Status bar height, so page content starts below it while the art bleeds behind it. */
    void setTopInset(int topInset) {
        if (this.topInset == topInset) {
            return;
        }
        this.topInset = topInset;
        notifyItemRangeChanged(0, getItemCount(), PAYLOAD_TOP_INSET);
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    @Override
    public int getItemViewType(int position) {
        return pages.get(position).getType();
    }

    @NonNull
    @Override
    public PageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == OnboardingPage.TYPE_HERO) {
            return new HeroHolder(ItemOnboardingHeroBinding.inflate(inflater, parent, false));
        }
        return new FeatureHolder(ItemOnboardingFeatureBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull PageHolder holder, int position) {
        holder.bind(pages.get(position));
        holder.applyTopInset(topInset);
    }

    @Override
    public void onBindViewHolder(@NonNull PageHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty() && allTopInset(payloads)) {
            holder.applyTopInset(topInset);
        } else {
            onBindViewHolder(holder, position);
        }
    }

    private static boolean allTopInset(List<Object> payloads) {
        for (Object payload : payloads) {
            if (payload != PAYLOAD_TOP_INSET) {
                return false;
            }
        }
        return true;
    }

    abstract static class PageHolder extends RecyclerView.ViewHolder {

        private final View content;
        private final int baseTopPadding;

        PageHolder(@NonNull View itemView, @NonNull View content) {
            super(itemView);
            this.content = content;
            this.baseTopPadding = content.getPaddingTop();
        }

        abstract void bind(@NonNull OnboardingPage page);

        final void applyTopInset(int inset) {
            content.setPadding(
                    content.getPaddingLeft(),
                    baseTopPadding + inset,
                    content.getPaddingRight(),
                    content.getPaddingBottom());
        }
    }

    static final class HeroHolder extends PageHolder {

        private final ItemOnboardingHeroBinding binding;

        HeroHolder(@NonNull ItemOnboardingHeroBinding binding) {
            super(binding.getRoot(), binding.pageContent);
            this.binding = binding;
        }

        @Override
        void bind(@NonNull OnboardingPage page) {
            int artRes = ArtAssets.find(itemView.getContext(), page.getArtName());
            boolean hasArt = artRes != 0;
            // Real artwork runs full-bleed behind the lockup with the cream wave over its base;
            // until it exists, the sky backdrop and a labeled placeholder frame stand in.
            binding.heroArt.setImageResource(hasArt ? artRes : R.drawable.bg_onboarding_panel);
            binding.heroWave.setVisibility(hasArt ? View.VISIBLE : View.GONE);
            binding.heroPlaceholder.setVisibility(hasArt ? View.GONE : View.VISIBLE);
        }
    }

    static final class FeatureHolder extends PageHolder {

        private final ItemOnboardingFeatureBinding binding;

        FeatureHolder(@NonNull ItemOnboardingFeatureBinding binding) {
            super(binding.getRoot(), binding.pageContent);
            this.binding = binding;
            ViewCompat.setAccessibilityHeading(binding.title, true);
        }

        @Override
        void bind(@NonNull OnboardingPage page) {
            binding.title.setText(page.getTitleRes());
            binding.body.setText(page.getBodyRes());
            binding.illustration.setArt(page.getArtName(), page.getArtTitleRes());
        }
    }
}
