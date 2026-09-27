package com.itismob.s03.group7.leef.ui.widget;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * Like {@code centerCrop}, but keeps the bottom edge of the image anchored. Used for artwork whose
 * bottom (the cream wave) must line up with the content below it on every screen size; any excess
 * is trimmed from the top and the sides.
 */
public class BottomCropImageView extends AppCompatImageView {

    public BottomCropImageView(@NonNull Context context) {
        this(context, null);
    }

    public BottomCropImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BottomCropImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        super.setScaleType(ScaleType.MATRIX);
    }

    @Override
    public void setScaleType(ScaleType scaleType) {
        // The crop is always computed by updateCropMatrix().
        super.setScaleType(ScaleType.MATRIX);
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);
        updateCropMatrix();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateCropMatrix();
    }

    private void updateCropMatrix() {
        Drawable drawable = getDrawable();
        int viewWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        int viewHeight = getHeight() - getPaddingTop() - getPaddingBottom();
        if (drawable == null || viewWidth <= 0 || viewHeight <= 0) {
            return;
        }
        int drawableWidth = drawable.getIntrinsicWidth();
        int drawableHeight = drawable.getIntrinsicHeight();
        if (drawableWidth <= 0 || drawableHeight <= 0) {
            return;
        }

        float scale = Math.max(
                (float) viewWidth / drawableWidth,
                (float) viewHeight / drawableHeight);
        float dx = (viewWidth - drawableWidth * scale) * 0.5f;
        float dy = viewHeight - drawableHeight * scale;

        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);
        matrix.postTranslate(Math.round(dx), Math.round(dy));
        setImageMatrix(matrix);
    }
}
