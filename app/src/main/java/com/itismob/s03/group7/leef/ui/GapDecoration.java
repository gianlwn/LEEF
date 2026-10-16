package com.itismob.s03.group7.leef.ui;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/** Even space between the cards of a list, without extra space before the first or after the last. */
public final class GapDecoration extends RecyclerView.ItemDecoration {

    private final int gapPx;
    private final int orientation;

    /** @param orientation {@link RecyclerView#HORIZONTAL} or {@link RecyclerView#VERTICAL} */
    public GapDecoration(int gapPx, int orientation) {
        this.gapPx = gapPx;
        this.orientation = orientation;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {
        if (parent.getChildAdapterPosition(view) == 0) {
            return;
        }
        if (orientation == RecyclerView.HORIZONTAL) {
            outRect.left = gapPx;
        } else {
            outRect.top = gapPx;
        }
    }
}
