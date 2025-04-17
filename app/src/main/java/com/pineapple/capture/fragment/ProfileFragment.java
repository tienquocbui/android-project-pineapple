package com.pineapple.capture.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.bottomsheet.BottomSheetDialog;
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
    private ImageButton editDisplayNameButton;
    
    // Bio elements
    private Button addBioButton;
    private LinearLayout bioContainer;
    private TextView bioText;
    private ImageButton editBioButton;
    
    // Location elements
    private Button addLocationButton;
    private LinearLayout locationContainer;
    private TextView locationText;
    private ImageButton editLocationButton;
    
    // Stats elements
    private TextView friendsCountText;
    private TextView followersCountText;
    private TextView followingCountText;
    
    // Share profile button
    private Button shareProfileButton;

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
        editDisplayNameButton = view.findViewById(R.id.edit_display_name_button);
        ImageButton settingsButton = view.findViewById(R.id.settings_button);
        
        // Bio views
        addBioButton = view.findViewById(R.id.add_bio_button);
        bioContainer = view.findViewById(R.id.bio_container);
        bioText = view.findViewById(R.id.bio_text);
        editBioButton = view.findViewById(R.id.edit_bio_button);
        
        // Location views
        addLocationButton = view.findViewById(R.id.add_location_button);
        locationContainer = view.findViewById(R.id.location_container);
        locationText = view.findViewById(R.id.location_text);
        editLocationButton = view.findViewById(R.id.edit_location_button);
        
        // Stats views
        friendsCountText = view.findViewById(R.id.friends_count);
        followersCountText = view.findViewById(R.id.followers_count);
        followingCountText = view.findViewById(R.id.following_count);
        
        // Share profile button
        shareProfileButton = view.findViewById(R.id.share_profile_button);

        // Set up click listeners
        editDisplayNameButton.setOnClickListener(v -> showEditDisplayNameDialog());
        settingsButton.setOnClickListener(v -> showAccountSettingsBottomSheet());
        
        // Bio button listeners
        addBioButton.setOnClickListener(v -> showEditBioDialog(null));
        editBioButton.setOnClickListener(v -> showEditBioDialog(bioText.getText().toString()));
        
        // Location button listeners
        addLocationButton.setOnClickListener(v -> showEditLocationDialog(null));
        editLocationButton.setOnClickListener(v -> showEditLocationDialog(locationText.getText().toString()));
        
        // Share profile button listener
        shareProfileButton.setOnClickListener(v -> shareProfile());

        // Observe user data
        viewModel.getUserData().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                updateUI(user);
            }
        });
        
        // Observe error messages
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }
    
    private void updateUI(User user) {
        // Update basic info
        displayNameText.setText(user.getDisplayName());
        usernameText.setText("@" + user.getUsername());
        
        // Load profile image using Glide
        if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isEmpty()) {
            Glide.with(this)
                    .load(user.getProfilePictureUrl())
                    .circleCrop()
                    .into(profileImage);
        }
        
        // Update bio
        if (user.getBio() != null && !user.getBio().isEmpty()) {
            bioText.setText(user.getBio());
            bioContainer.setVisibility(View.VISIBLE);
            addBioButton.setVisibility(View.GONE);
        } else {
            bioContainer.setVisibility(View.GONE);
            addBioButton.setVisibility(View.VISIBLE);
        }
        
        // Update location
        if (user.getLocation() != null && !user.getLocation().isEmpty()) {
            locationText.setText(user.getLocation());
            locationContainer.setVisibility(View.VISIBLE);
            addLocationButton.setVisibility(View.GONE);
        } else {
            locationContainer.setVisibility(View.GONE);
            addLocationButton.setVisibility(View.VISIBLE);
        }
        
        // Update stats
        friendsCountText.setText(String.valueOf(user.getFriendsCount()));
        followersCountText.setText(String.valueOf(user.getFollowersCount()));
        followingCountText.setText(String.valueOf(user.getFollowingCount()));
    }

    private void showEditDisplayNameDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_display_name, null);
        EditText newDisplayNameInput = dialogView.findViewById(R.id.new_display_name_input);

        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Edit Display Name")
                .setView(dialogView)
                .setPositiveButton("Update", null)
                .setNegativeButton("Cancel", null);

        AlertDialog dialog = dialogBuilder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String newDisplayName = newDisplayNameInput.getText().toString().trim();

                if (newDisplayName.isEmpty()) {
                    Toast.makeText(requireContext(), "Display name cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (newDisplayName.length() > 30) {
                    Toast.makeText(requireContext(), "Display name cannot be longer than 30 characters", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Allow letters, numbers, spaces, and basic punctuation
                if (!newDisplayName.matches("^[a-zA-Z0-9 .,'!?-]+$")) {
                    Toast.makeText(requireContext(), "Display name can only contain letters, numbers, spaces, and basic punctuation", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Show loading state
                positiveButton.setEnabled(false);
                newDisplayNameInput.setEnabled(false);

                viewModel.updateDisplayNameOnly(newDisplayName);

                viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
                    if (message != null && !message.isEmpty()) {
                        if (message.contains("successfully")) {
                            dialog.dismiss();
                        }
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        positiveButton.setEnabled(true);
                        newDisplayNameInput.setEnabled(true);
                    }
                });
            });
        });
        dialog.show();
    }
    
    private void showEditBioDialog(String currentBio) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_bio, null);
        EditText bioInput = dialogView.findViewById(R.id.bio_input);
        TextView charCountText = dialogView.findViewById(R.id.bio_char_count);
        
        if (currentBio != null) {
            bioInput.setText(currentBio);
            charCountText.setText(currentBio.length() + "/150");
        }
        
        bioInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                charCountText.setText(s.length() + "/150");
            }
        });

        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Edit Bio")
                .setView(dialogView)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null);

        AlertDialog dialog = dialogBuilder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String newBio = bioInput.getText().toString().trim();
                
                // Show loading state
                positiveButton.setEnabled(false);
                bioInput.setEnabled(false);

                viewModel.updateBio(newBio);
                
                viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
                    if (message != null && !message.isEmpty()) {
                        if (message.contains("successfully")) {
                            dialog.dismiss();
                        }
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        positiveButton.setEnabled(true);
                        bioInput.setEnabled(true);
                    }
                });
            });
        });
        dialog.show();
    }
    
    private void showEditLocationDialog(String currentLocation) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_location, null);
        EditText locationInput = dialogView.findViewById(R.id.location_input);
        TextView charCountText = dialogView.findViewById(R.id.location_char_count);
        
        if (currentLocation != null) {
            locationInput.setText(currentLocation);
            charCountText.setText(currentLocation.length() + "/50");
        }
        
        locationInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                charCountText.setText(s.length() + "/50");
            }
        });

        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Edit Location")
                .setView(dialogView)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null);

        AlertDialog dialog = dialogBuilder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String newLocation = locationInput.getText().toString().trim();
                
                // Show loading state
                positiveButton.setEnabled(false);
                locationInput.setEnabled(false);

                viewModel.updateLocation(newLocation);
                
                viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
                    if (message != null && !message.isEmpty()) {
                        if (message.contains("successfully")) {
                            dialog.dismiss();
                        }
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        positiveButton.setEnabled(true);
                        locationInput.setEnabled(true);
                    }
                });
            });
        });
        dialog.show();
    }
    
    private void shareProfile() {
        User user = viewModel.getUserData().getValue();
        if (user == null) return;
        
        String shareText = "Check out my profile on Capture App!\n\n" +
                           "Name: " + user.getDisplayName() + "\n" +
                           "Username: @" + user.getUsername();
        
        if (user.getBio() != null && !user.getBio().isEmpty()) {
            shareText += "\n\nBio: " + user.getBio();
        }
        
        if (user.getLocation() != null && !user.getLocation().isEmpty()) {
            shareText += "\n\nLocation: " + user.getLocation();
        }
        
        shareText += "\n\nFollowers: " + user.getFollowersCount() + 
                     " | Following: " + user.getFollowingCount();
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        
        startActivity(Intent.createChooser(shareIntent, "Share via"));
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

    private void showEditUsernameDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_username, null);
        EditText newUsernameInput = dialogView.findViewById(R.id.new_username_input);

        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Change Username")
                .setView(dialogView)
                .setPositiveButton("Update", null)
                .setNegativeButton("Cancel", null);

        AlertDialog dialog = dialogBuilder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String newUsername = newUsernameInput.getText().toString().trim();
                
                if (newUsername.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter a username", 
                        Toast.LENGTH_SHORT).show();
                    return;
                }

                if (newUsername.length() > 30) {
                    Toast.makeText(requireContext(), "Username cannot be longer than 30 characters", 
                        Toast.LENGTH_SHORT).show();
                    return;
                }

                if (newUsername.contains(" ")) {
                    Toast.makeText(requireContext(), "Username cannot contain spaces", 
                        Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!newUsername.matches("^[a-zA-Z0-9._]+$")) {
                    Toast.makeText(requireContext(), 
                        "Username can only contain letters, numbers, periods, and underscores", 
                        Toast.LENGTH_SHORT).show();
                    return;
                }

                if (newUsername.startsWith(".") || newUsername.endsWith(".")) {
                    Toast.makeText(requireContext(), 
                        "Username cannot start or end with a period", 
                        Toast.LENGTH_SHORT).show();
                    return;
                }

                // Show loading state
                positiveButton.setEnabled(false);
                newUsernameInput.setEnabled(false);
                
                viewModel.updateDisplayName(newUsername);
                
                // Observe the result
                viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
                    if (message != null && !message.isEmpty()) {
                        if (message.contains("successfully")) {
                            dialog.dismiss();
                        }
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        
                        // Re-enable inputs
                        positiveButton.setEnabled(true);
                        newUsernameInput.setEnabled(true);
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

    private void showAccountSettingsBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_account_settings, null);
        bottomSheetDialog.setContentView(sheetView);

        sheetView.findViewById(R.id.action_change_username).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            showEditUsernameDialog();
        });
        sheetView.findViewById(R.id.action_change_display_name).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            showEditDisplayNameDialog();
        });
        sheetView.findViewById(R.id.action_change_password).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            showChangePasswordDialog();
        });
        sheetView.findViewById(R.id.action_logout).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            logout();
        });
        sheetView.findViewById(R.id.action_delete_account).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            showDeleteAccountDialog();
        });

        bottomSheetDialog.show();
    }
}
