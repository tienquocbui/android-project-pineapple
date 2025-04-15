package com.pineapple.capture.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.pineapple.capture.R;
import com.pineapple.capture.activities.LoginActivity;
import com.pineapple.capture.profile.ProfileViewModel;
import com.pineapple.capture.models.User;

public class ProfileFragment extends Fragment {

    private ProfileViewModel viewModel;
    private TextView usernameText;
    private TextView emailText;
    private Button editEmailButton;
    private Button changePasswordButton;
    private Button logoutButton;
    private Button deleteAccountButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        // Initialize views
        usernameText = view.findViewById(R.id.username);
        emailText = view.findViewById(R.id.email);
        editEmailButton = view.findViewById(R.id.edit_email_button);
        changePasswordButton = view.findViewById(R.id.change_password_button);
        logoutButton = view.findViewById(R.id.logout_button);
        deleteAccountButton = view.findViewById(R.id.delete_account_button);

        // Set up click listeners
        editEmailButton.setOnClickListener(v -> showEditEmailDialog());
        changePasswordButton.setOnClickListener(v -> showChangePasswordDialog());
        logoutButton.setOnClickListener(v -> logout());
        deleteAccountButton.setOnClickListener(v -> showDeleteAccountDialog());

        // Observe user data
        viewModel.getUserData().observe(getViewLifecycleOwner(), this::updateUI);
        
        // Observe error messages
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void updateUI(User user) {
        if (user != null) {
            usernameText.setText(user.getUsername());
            emailText.setText(user.getEmail());
        }
    }

    private void showEditEmailDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_email, null);
        EditText newEmailInput = dialogView.findViewById(R.id.new_email_input);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Change Email")
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String newEmail = newEmailInput.getText().toString().trim();
                    if (!newEmail.isEmpty()) {
                        viewModel.updateEmail(newEmail);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showChangePasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        EditText currentPasswordInput = dialogView.findViewById(R.id.current_password_input);
        EditText newPasswordInput = dialogView.findViewById(R.id.new_password_input);
        EditText confirmPasswordInput = dialogView.findViewById(R.id.confirm_password_input);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Change Password")
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String currentPassword = currentPasswordInput.getText().toString();
                    String newPassword = newPasswordInput.getText().toString();
                    String confirmPassword = confirmPasswordInput.getText().toString();

                    if (newPassword.equals(confirmPassword)) {
                        viewModel.updatePassword(currentPassword, newPassword);
                    } else {
                        Toast.makeText(requireContext(), 
                            "New passwords do not match", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteAccountDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_delete_account, null);
        EditText passwordInput = dialogView.findViewById(R.id.password_input);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setView(dialogView)
                .setPositiveButton("Delete", (dialog, which) -> {
                    String password = passwordInput.getText().toString();
                    viewModel.deleteAccount(password);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
