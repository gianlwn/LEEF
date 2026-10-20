package com.itismob.s03.group7.leef.ui;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.databinding.DialogImageLinkBinding;

import java.util.Locale;

/**
 * The "paste an image link" box used wherever a person adds a photo (Create Event, Feedback). Photos are
 * web links because uploading files needs Firebase Storage, which is not on the free plan.
 */
public final class ImageLinkDialog {

    /** Called with a link that passed the basic check (https, no spaces). */
    public interface OnLink {
        void onLink(@NonNull String url);
    }

    private ImageLinkDialog() {
    }

    public static void show(@NonNull Activity activity, @Nullable String existing, @NonNull OnLink onLink) {
        DialogImageLinkBinding binding = DialogImageLinkBinding.inflate(activity.getLayoutInflater());
        if (existing != null) {
            binding.linkInput.setText(existing);
        }
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.create_photo_dialog_title)
                .setView(binding.getRoot())
                .setPositiveButton(R.string.create_photo_dialog_use, null)
                .setNegativeButton(R.string.create_photo_dialog_cancel, null)
                .create();
        // The button is wired after show() so a bad link keeps the dialog open.
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String link = binding.linkInput.getText() == null ? "" : binding.linkInput.getText().toString().trim();
            if (!link.toLowerCase(Locale.ROOT).startsWith("https://") || link.length() <= 12 || link.contains(" ")) {
                binding.linkLayout.setError(activity.getString(R.string.create_photo_error_link));
                return;
            }
            onLink.onLink(link);
            dialog.dismiss();
        }));
        dialog.show();
    }
}
