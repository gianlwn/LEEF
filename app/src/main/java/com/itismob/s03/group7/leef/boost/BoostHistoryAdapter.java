package com.itismob.s03.group7.leef.boost;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.databinding.ItemBoostReceiptBinding;
import com.itismob.s03.group7.leef.home.EventFormat;
import com.itismob.s03.group7.leef.model.BoostReceipt;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** The rows of the Boost history list: one paid boost each, with the receipt details. */
final class BoostHistoryAdapter extends ListAdapter<BoostReceipt, BoostHistoryAdapter.Holder> {

    interface Listener {
        void onReceiptClick(@NonNull BoostReceipt receipt);
    }

    private final Listener listener;

    BoostHistoryAdapter(@NonNull Listener listener) {
        super(new DiffUtil.ItemCallback<BoostReceipt>() {
            @Override
            public boolean areItemsTheSame(@NonNull BoostReceipt oldItem, @NonNull BoostReceipt newItem) {
                return oldItem.getConfirmationCode().equals(newItem.getConfirmationCode())
                        && oldItem.getEventId().equals(newItem.getEventId())
                        && oldItem.getCreatedAtMillis() == newItem.getCreatedAtMillis();
            }

            @Override
            public boolean areContentsTheSame(@NonNull BoostReceipt oldItem, @NonNull BoostReceipt newItem) {
                return oldItem.getBoostedUntilMillis() == newItem.getBoostedUntilMillis()
                        && oldItem.getAmount() == newItem.getAmount()
                        && oldItem.getEventTitle().equals(newItem.getEventTitle());
            }
        });
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemBoostReceiptBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position));
    }

    final class Holder extends RecyclerView.ViewHolder {
        private final ItemBoostReceiptBinding binding;

        Holder(@NonNull ItemBoostReceiptBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull BoostReceipt receipt) {
            Context context = binding.getRoot().getContext();
            long now = System.currentTimeMillis();
            boolean running = receipt.isRunning(now);
            binding.title.setText(receipt.getEventTitle().isEmpty()
                    ? context.getString(R.string.boost_history_untitled) : receipt.getEventTitle());
            binding.status.setText(running ? R.string.boost_history_active : R.string.boost_history_ended);
            binding.status.setTextColor(ContextCompat.getColor(context,
                    running ? R.color.leef_green_700 : R.color.leef_gray_700));
            binding.plan.setText(context.getString(R.string.boost_history_plan_line,
                    context.getString(receipt.getPlan().getLabelRes()),
                    EventFormat.priceText(context, receipt.getAmount()),
                    context.getString(receipt.getPaymentMethod().getLabelRes())));
            binding.dates.setText(receipt.getCreatedAtMillis() > 0
                    ? context.getString(R.string.boost_history_dates_line, dateTime(receipt.getCreatedAtMillis()),
                            dateTime(receipt.getBoostedUntilMillis()))
                    : context.getString(R.string.boost_history_until_line, dateTime(receipt.getBoostedUntilMillis())));
            binding.code.setText(context.getString(R.string.boost_history_code_line, receipt.getConfirmationCode()));
            binding.getRoot().setOnClickListener(v -> listener.onReceiptClick(receipt));
        }
    }

    @NonNull
    private static String dateTime(long millis) {
        return new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(new Date(millis));
    }
}
