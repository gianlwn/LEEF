package com.itismob.s03.group7.leef.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

import com.itismob.s03.group7.leef.R;

/**
 * Page indicator for a pager. The active page is drawn as a stretched pill, and the pill slides
 * and morphs between dots as the pager scrolls ({@link #setPosition(int, float)}).
 */
public class PagerDotsIndicator extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF dotBounds = new RectF();

    private final int dotColor;
    private final int activeDotColor;
    private final float dotSize;
    private final float activeDotWidth;
    private final float dotSpacing;

    private int dotCount;
    private float position;

    public PagerDotsIndicator(@NonNull Context context) {
        this(context, null);
    }

    public PagerDotsIndicator(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PagerDotsIndicator(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        float density = getResources().getDisplayMetrics().density;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PagerDotsIndicator, defStyleAttr, 0);
        try {
            dotColor = a.getColor(R.styleable.PagerDotsIndicator_leefDotColor, 0xFFD9D5CC);
            activeDotColor = a.getColor(R.styleable.PagerDotsIndicator_leefDotActiveColor, 0xFF1B5840);
            dotSize = a.getDimension(R.styleable.PagerDotsIndicator_leefDotSize, 8 * density);
            activeDotWidth = a.getDimension(R.styleable.PagerDotsIndicator_leefDotActiveWidth, 24 * density);
            dotSpacing = a.getDimension(R.styleable.PagerDotsIndicator_leefDotSpacing, 8 * density);
            dotCount = Math.max(0, a.getInt(R.styleable.PagerDotsIndicator_leefDotCount, 0));
        } finally {
            a.recycle();
        }
        // Progress is announced through the content description set by the host screen.
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    public void setDotCount(int count) {
        int newCount = Math.max(0, count);
        if (newCount == dotCount) {
            return;
        }
        dotCount = newCount;
        position = Math.min(position, Math.max(0, dotCount - 1));
        requestLayout();
        invalidate();
    }

    /** Mirrors {@code ViewPager2.OnPageChangeCallback#onPageScrolled}. */
    public void setPosition(int page, float pageOffset) {
        float newPosition = page + pageOffset;
        if (newPosition != position) {
            position = newPosition;
            invalidate();
        }
    }

    private float contentWidth() {
        if (dotCount == 0) {
            return 0f;
        }
        return (dotCount - 1) * (dotSize + dotSpacing) + activeDotWidth;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = Math.round(contentWidth()) + getPaddingLeft() + getPaddingRight();
        int desiredHeight = Math.round(dotSize) + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(
                resolveSize(desiredWidth, widthMeasureSpec),
                resolveSize(desiredHeight, heightMeasureSpec));
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (dotCount == 0) {
            return;
        }
        float availableWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        float availableHeight = getHeight() - getPaddingTop() - getPaddingBottom();
        float x = getPaddingLeft() + (availableWidth - contentWidth()) / 2f;
        float top = getPaddingTop() + (availableHeight - dotSize) / 2f;
        float radius = dotSize / 2f;
        boolean rtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;

        for (int slot = 0; slot < dotCount; slot++) {
            int index = rtl ? dotCount - 1 - slot : slot;
            // 1 when this dot is the current page, falling to 0 one page away.
            float activeness = Math.max(0f, 1f - Math.abs(position - index));
            float width = dotSize + (activeDotWidth - dotSize) * activeness;
            paint.setColor(ColorUtils.blendARGB(dotColor, activeDotColor, activeness));
            dotBounds.set(x, top, x + width, top + dotSize);
            canvas.drawRoundRect(dotBounds, radius, radius, paint);
            x += width + dotSpacing;
        }
    }
}
