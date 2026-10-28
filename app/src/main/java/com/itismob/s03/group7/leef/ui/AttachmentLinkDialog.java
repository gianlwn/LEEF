package com.itismob.s03.group7.leef.ui;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.databinding.DialogAttachmentLinkBinding;
import com.itismob.s03.group7.leef.model.EventAttachment;

/**
 * The "add a file link" box on Create Event. Files are web links (a Google Drive PDF, a flyer image)
 * because uploading files needs Firebase Storage, which is not on the free plan.
 */
public final class AttachmentLinkDialog {

    /** Called with a name and a link that passed the basic checks (a name, and an https link with no spaces). */
    public interface OnFile {
        void onFile(@NonNull String name, @NonNull String url);
    }

    private AttachmentLinkDialog() {
    }

    public static void show(@NonNull Activity activity, @NonNull OnFile onFile) {
        DialogAttachmentLinkBinding binding = DialogAttachmentLinkBinding.inflate(activity.getLayoutInflater());
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.create_files_dialog_title)
                .setView(binding.getRoot())
                .setPositiveButton(R.string.create_files_dialog_add, null)
                .setNegativeButton(R.string.create_photo_dialog_cancel, null)
                .create();
        // The button is wired after show() so a bad entry keeps the dialog open.
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = binding.nameInput.getText() == null ? "" : binding.nameInput.getText().toString().trim();
            String link = binding.linkInput.getText() == null ? "" : binding.linkInput.getText().toString().trim();
            boolean ok = true;
            binding.nameLayout.setError(null);
            binding.linkLayout.setError(null);
            if (name.isEmpty()) {
                binding.nameLayout.setError(activity.getString(R.string.create_files_error_name));
                ok = false;
            }
            if (!EventAttachment.isValidUrl(link)) {
                binding.linkLayout.setError(activity.getString(R.string.create_photo_error_link));
                ok = false;
            }
            if (!ok) {
                return;
            }
            onFile.onFile(name, link);
            dialog.dismiss();
        }));
        dialog.show();
    }
}
