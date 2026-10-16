package com.itismob.s03.group7.leef.home;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.databinding.ItemCategoryChipBinding;
import com.itismob.s03.group7.leef.model.EventCategory;

/**
 * The round category chips under the search bar: "All" first, then each MCO1 category. The selected
 * chip turns solid green with a white icon.
 */
final class CategoryChipAdapter extends RecyclerView.Adapter<CategoryChipAdapter.Holder> {

    interface Listener {
        /** @param category the tapped category, or null for "All" */
        void onCategorySelected(@Nullable EventCategory category);
    }

    private final EventCategory[] categories = EventCategory.values();
    private final Listener listener;
    @Nullable
    private EventCategory selected;

    CategoryChipAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    void setSelected(@Nullable EventCategory category) {
        if (selected != category) {
            selected = category;
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemCount() {
        return categories.length + 1;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemCategoryChipBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(position == 0 ? null : categories[position - 1]);
    }

    final class Holder extends RecyclerView.ViewHolder {
        private final ItemCategoryChipBinding binding;

        Holder(@NonNull ItemCategoryChipBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@Nullable EventCategory category) {
            boolean isSelected = category == selected;
            int selectedColor = ContextCompat.getColor(itemView.getContext(), R.color.leef_green_700);
            int white = ContextCompat.getColor(itemView.getContext(), R.color.white);

            int iconRes = category == null ? R.drawable.ic_cat_all : category.getIconRes();
            int labelRes = category == null ? R.string.category_all : category.getLabelRes();
            int tint = category == null
                    ? R.color.leef_green_700 : category.getChipIconColorRes();
            int fill = category == null
                    ? R.color.leef_green_100 : category.getChipColorRes();

            binding.icon.setImageResource(iconRes);
            binding.circle.setCardBackgroundColor(isSelected
                    ? selectedColor : ContextCompat.getColor(itemView.getContext(), fill));
            binding.icon.setImageTintList(ColorStateList.valueOf(isSelected
                    ? white : ContextCompat.getColor(itemView.getContext(), tint)));
            binding.label.setText(labelRes);
            binding.label.setSelected(isSelected);
            itemView.setSelected(isSelected);
            itemView.setContentDescription(itemView.getContext().getString(labelRes));
            itemView.setOnClickListener(v -> listener.onCategorySelected(category));
        }
    }
}
