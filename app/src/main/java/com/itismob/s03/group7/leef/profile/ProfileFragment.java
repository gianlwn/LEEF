package com.itismob.s03.group7.leef.profile;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.itismob.s03.group7.leef.MainViewModel;
import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.auth.GoogleSignInHelper;
import com.itismob.s03.group7.leef.auth.LogInActivity;
import com.itismob.s03.group7.leef.databinding.FragmentProfileBinding;
import com.itismob.s03.group7.leef.databinding.ViewProfileRowBinding;
import com.itismob.s03.group7.leef.home.HomeFragment;
import com.itismob.s03.group7.leef.legal.LegalActivity;
import com.itismob.s03.group7.leef.legal.LegalDocument;
import com.itismob.s03.group7.leef.model.UserProfile;

/**
 * The Profile tab: the person's name, role, school and email, then Edit profile, Change password
 * (a free reset email), the Terms and Privacy pages and Sign out.
 */
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private MainViewModel mainViewModel;
    private ProfileViewModel viewModel;
    private ActivityResultLauncher<Intent> editLauncher;
    @Nullable
    private UserProfile current;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        editLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                // Edit profile saved: reload, so this tab and Home's greeting show the new details.
                mainViewModel.refreshProfile();
                Toast.makeText(requireContext(), R.string.profile_updated, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        ViewCompat.setAccessibilityHeading(binding.title, true);

        setUpRow(binding.rowEdit, R.drawable.ic_edit, R.string.profile_row_edit);
        setUpRow(binding.rowPassword, R.drawable.ic_field_lock, R.string.profile_row_password);
        setUpRow(binding.rowTerms, R.drawable.ic_doc, R.string.auth_terms_of_service);
        setUpRow(binding.rowPrivacy, R.drawable.ic_doc, R.string.auth_privacy_policy);

        binding.rowEdit.getRoot().setOnClickListener(v -> openEditProfile());
        binding.rowPassword.getRoot().setOnClickListener(v -> confirmPasswordReset());
        binding.rowTerms.getRoot().setOnClickListener(v ->
                startActivity(LegalActivity.createIntent(requireContext(), LegalDocument.TERMS)));
        binding.rowPrivacy.getRoot().setOnClickListener(v ->
                startActivity(LegalActivity.createIntent(requireContext(), LegalDocument.PRIVACY)));
        binding.signOutButton.setOnClickListener(v -> confirmSignOut());

        binding.footer.setText(footerText());

        mainViewModel.getProfile().observe(getViewLifecycleOwner(), this::render);
        viewModel.getSendingReset().observe(getViewLifecycleOwner(), sending ->
                binding.rowPassword.getRoot().setEnabled(!Boolean.TRUE.equals(sending)));
        viewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            if (message == null) {
                return;
            }
            String text = message.argument == null
                    ? getString(message.messageRes)
                    : getString(message.messageRes, message.argument);
            Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show();
            viewModel.messageHandled();
        });
        viewModel.getSignedOut().observe(getViewLifecycleOwner(), signedOut -> {
            if (Boolean.TRUE.equals(signedOut)) {
                leaveAfterSignOut();
            }
        });
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        // The first load may have failed (no internet): try again when the tab is opened.
        if (!hidden && mainViewModel.getProfile().getValue() == null) {
            mainViewModel.refreshProfile();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static void setUpRow(@NonNull ViewProfileRowBinding row, int iconRes, int titleRes) {
        row.rowIcon.setImageResource(iconRes);
        row.rowTitle.setText(titleRes);
    }

    /** Shows the person's details, or just their email while the profile hasn't loaded. */
    private void render(@Nullable UserProfile profile) {
        current = profile;
        String email = profile != null ? profile.getEmail() : viewModel.getEmail();

        if (profile != null) {
            binding.name.setText(profile.getFullName());
            binding.avatarInitials.setText(HomeFragment.initialsOf(profile.getFullName()));
            binding.avatarInitials.setVisibility(View.VISIBLE);
            binding.avatarIcon.setVisibility(View.GONE);
            binding.rolePill.setText(profile.getRole().getLabelRes());
            binding.rolePill.setVisibility(View.VISIBLE);
            binding.school.setText(profile.getSchoolOrganization());
            binding.schoolLine.setVisibility(View.VISIBLE);
        } else {
            binding.name.setText(R.string.profile_fallback_name);
            binding.avatarInitials.setVisibility(View.GONE);
            binding.avatarIcon.setVisibility(View.VISIBLE);
            binding.rolePill.setVisibility(View.GONE);
            binding.schoolLine.setVisibility(View.GONE);
        }
        if (email != null && !email.isEmpty()) {
            binding.email.setText(email);
            binding.emailLine.setVisibility(View.VISIBLE);
        } else {
            binding.emailLine.setVisibility(View.GONE);
        }

        boolean canEdit = profile != null;
        boolean canChangePassword = viewModel.hasPassword();
        binding.rowEdit.getRoot().setVisibility(canEdit ? View.VISIBLE : View.GONE);
        binding.rowPassword.getRoot().setVisibility(canChangePassword ? View.VISIBLE : View.GONE);
        binding.accountDivider.setVisibility(canEdit && canChangePassword ? View.VISIBLE : View.GONE);
        boolean anyAccountRow = canEdit || canChangePassword;
        binding.accountHeading.setVisibility(anyAccountRow ? View.VISIBLE : View.GONE);
        binding.accountCard.setVisibility(anyAccountRow ? View.VISIBLE : View.GONE);
    }

    private void openEditProfile() {
        if (current == null) {
            return;
        }
        editLauncher.launch(EditProfileActivity.createIntent(requireContext(), current));
    }

    private void confirmPasswordReset() {
        String email = viewModel.getEmail();
        if (email == null || email.isEmpty()) {
            Toast.makeText(requireContext(), R.string.profile_error_signed_out, Toast.LENGTH_SHORT).show();
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.profile_password_title)
                .setMessage(getString(R.string.profile_password_message, email))
                .setPositiveButton(R.string.login_reset_send, (dialog, which) -> viewModel.sendPasswordReset())
                .setNegativeButton(R.string.profile_cancel, null)
                .show();
    }

    private void confirmSignOut() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.profile_sign_out_title)
                .setMessage(R.string.profile_sign_out_message)
                .setPositiveButton(R.string.profile_sign_out, (dialog, which) -> viewModel.signOut())
                .setNegativeButton(R.string.profile_cancel, null)
                .show();
    }

    /** After signing out, Google forgets which account was picked and the person lands on Log In. */
    private void leaveAfterSignOut() {
        GoogleSignInHelper.forgetChosenAccount(requireContext());
        Intent intent = new Intent(requireContext(), LogInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    @NonNull
    private String footerText() {
        String version = null;
        try {
            version = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
            // The footer just leaves the version out.
        }
        return version == null || version.isEmpty()
                ? getString(R.string.profile_footer_no_version)
                : getString(R.string.profile_footer, version);
    }
}
