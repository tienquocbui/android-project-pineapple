package com.pineapple.capture.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.pineapple.capture.R;
import com.pineapple.capture.activities.LoginActivity;
import com.pineapple.capture.profile.ProfileViewModel;
import com.pineapple.capture.models.User;

public class ProfileFragment extends Fragment {

    private ProfileViewModel viewModel;
    private ImageView profileImage;
    private TextView displayNameText;
    private TextView usernameText;
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
        profileImage = view.findViewById(R.id.profile_image);
        displayNameText = view.findViewById(R.id.display_name);
        usernameText = view.findViewById(R.id.username);
        editEmailButton = view.findViewById(R.id.edit_email_button);
        changePasswordButton = view.findViewById(R.id.change_password_button);
        logoutButton = view.findViewById(R.id.logout_button);
        deleteAccountButton = view.findViewById(R.id.delete_account_button);

        // Set up click listeners
        editEmailButton.setOnClickListener(v -> showEditDisplayNameDialog());
        editEmailButton.setText("Change Display Name");
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
            displayNameText.setText(user.getDisplayName());
            usernameText.setText(user.getUsername());
            
            // Load profile image using Glide
            if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isEmpty()) {
                Glide.with(this)
                        .load(user.getProfilePictureUrl())
                        .circleCrop()
                        .into(profileImage);
            }
        }
    }

    private void showEditEmailDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_email, null);
        EditText currentPasswordInput = dialogView.findViewById(R.id.current_password_input);
        EditText newEmailInput = dialogView.findViewById(R.id.new_email_input);

        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Change Email")
                .setView(dialogView)
                .setPositiveButton("Update", null) // Set to null to handle click manually
                .setNegativeButton("Cancel", null);

        AlertDialog dialog = dialogBuilder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String currentPassword = currentPasswordInput.getText().toString().trim();
                String newEmail = newEmailInput.getText().toString().trim();
                
                if (currentPassword.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter your current password", 
                        Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (newEmail.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter a new email", 
                        Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                    Toast.makeText(requireContext(), "Please enter a valid email address", 
                        Toast.LENGTH_SHORT).show();
                    return;
                }

                // Show loading state
                positiveButton.setEnabled(false);
                currentPasswordInput.setEnabled(false);
                newEmailInput.setEnabled(false);
                
                viewModel.updateEmail(currentPassword, newEmail);
                
                // Observe the result
                viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
                    if (message != null && !message.isEmpty()) {
                        if (message.contains("successfully")) {
                            dialog.dismiss();
                        }
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        
                        // Re-enable inputs
                        positiveButton.setEnabled(true);
                        currentPasswordInput.setEnabled(true);
                        newEmailInput.setEnabled(true);
                    }
                });
            });
        });

        dialog.show();
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
                    
                    // Observe the deletion result
                    viewModel.getDeletionResult().observe(getViewLifecycleOwner(), result -> {
                        if (result != null) {
                            if (result) {
                                // Navigate to login screen
                                Intent intent = new Intent(requireContext(), LoginActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                requireActivity().finish();
                            } else {
                                Toast.makeText(requireContext(), 
                                    "Failed to delete account", 
                                    Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
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

    private void showEditDisplayNameDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_display_name, null);
        EditText newDisplayNameInput = dialogView.findViewById(R.id.new_display_name_input);

        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Change Display Name")
                .setView(dialogView)
                .setPositiveButton("Update", null)
                .setNegativeButton("Cancel", null);

        AlertDialog dialog = dialogBuilder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String newDisplayName = newDisplayNameInput.getText().toString().trim();
                
                if (newDisplayName.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter a display name", 
                        Toast.LENGTH_SHORT).show();
                    return;
                }

                if (newDisplayName.length() > 30) {
                    Toast.makeText(requireContext(), "Display name cannot be longer than 30 characters", 
                        Toast.LENGTH_SHORT).show();
                    return;
                }

                // Validate display name format
                if (!isValidDisplayName(newDisplayName)) {
                    Toast.makeText(requireContext(), 
                        "Display name can only contain letters, numbers, spaces, and basic punctuation", 
                        Toast.LENGTH_SHORT).show();
                    return;
                }

                // Show loading state
                positiveButton.setEnabled(false);
                newDisplayNameInput.setEnabled(false);
                
                viewModel.updateDisplayName(newDisplayName);
                
                // Observe the result
                viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
                    if (message != null && !message.isEmpty()) {
                        if (message.contains("successfully")) {
                            dialog.dismiss();
                        }
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        
                        // Re-enable inputs
                        positiveButton.setEnabled(true);
                        newDisplayNameInput.setEnabled(true);
                    }
                });
            });
        });

        dialog.show();
    }

    private boolean isValidDisplayName(String displayName) {
        // Only allow letters, numbers, spaces, and basic punctuation
        return displayName.matches("^[a-zA-Z0-9\\s.,!?-]+$");
    }
}
