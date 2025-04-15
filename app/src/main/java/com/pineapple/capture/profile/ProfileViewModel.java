package com.pineapple.capture.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pineapple.capture.models.User;

public class ProfileViewModel extends ViewModel {
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore db;
    private final MutableLiveData<User> userData;
    private final MutableLiveData<String> errorMessage;
    private final MutableLiveData<Boolean> isLoading;
    private final MutableLiveData<Boolean> deletionResult;

    public ProfileViewModel() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userData = new MutableLiveData<>();
        errorMessage = new MutableLiveData<>();
        isLoading = new MutableLiveData<>(false);
        deletionResult = new MutableLiveData<>();
        loadUserData();
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            isLoading.setValue(true);
            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (!documentSnapshot.exists()) {
                            errorMessage.setValue("User document does not exist");
                            isLoading.setValue(false);
                            return;
                        }
                        
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            user.setId(currentUser.getUid());
                            // Set display name from Firebase User if not set in Firestore
                            if (user.getDisplayName() == null || user.getDisplayName().isEmpty()) {
                                user.setDisplayName(currentUser.getDisplayName());
                            }
                            userData.setValue(user);
                        } else {
                            errorMessage.setValue("Failed to convert document to User object");
                        }
                        isLoading.setValue(false);
                    })
                    .addOnFailureListener(e -> {
                        errorMessage.setValue("Failed to load user data: " + e.getMessage());
                        isLoading.setValue(false);
                    });
        } else {
            errorMessage.setValue("No current user found");
        }
    }

    public void updateEmail(String newEmail) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            isLoading.setValue(true);
            user.updateEmail(newEmail)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Update email in Firestore
                            db.collection("users").document(user.getUid())
                                    .update("email", newEmail)
                                    .addOnSuccessListener(aVoid -> {
                                        User updatedUser = userData.getValue();
                                        if (updatedUser != null) {
                                            updatedUser.setEmail(newEmail);
                                            userData.setValue(updatedUser);
                                        }
                                        errorMessage.setValue("Email updated successfully");
                                    })
                                    .addOnFailureListener(e -> 
                                        errorMessage.setValue("Failed to update email in database"));
                        } else {
                            errorMessage.setValue("Failed to update email: " + 
                                task.getException().getMessage());
                        }
                        isLoading.setValue(false);
                    });
        }
    }

    public void updatePassword(String currentPassword, String newPassword) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            isLoading.setValue(true);
            // Re-authenticate user before changing password
            com.google.firebase.auth.AuthCredential credential = 
                com.google.firebase.auth.EmailAuthProvider.getCredential(
                    user.getEmail(), currentPassword);
            
            user.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        user.updatePassword(newPassword)
                            .addOnCompleteListener(passwordTask -> {
                                if (passwordTask.isSuccessful()) {
                                    errorMessage.setValue("Password updated successfully");
                                } else {
                                    errorMessage.setValue("Failed to update password: " + 
                                        passwordTask.getException().getMessage());
                                }
                                isLoading.setValue(false);
                            });
                    } else {
                        errorMessage.setValue("Current password is incorrect");
                        isLoading.setValue(false);
                    }
                });
        }
    }

    public void deleteAccount(String currentPassword) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            isLoading.setValue(true);
            // Re-authenticate user before deleting account
            com.google.firebase.auth.AuthCredential credential = 
                com.google.firebase.auth.EmailAuthProvider.getCredential(
                    user.getEmail(), currentPassword);
            
            user.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Delete user data from Firestore
                        db.collection("users").document(user.getUid())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                // Delete user account
                                user.delete()
                                    .addOnCompleteListener(deleteTask -> {
                                        if (deleteTask.isSuccessful()) {
                                            deletionResult.setValue(true);
                                            errorMessage.setValue("Account deleted successfully");
                                        } else {
                                            deletionResult.setValue(false);
                                            errorMessage.setValue("Failed to delete account: " + 
                                                deleteTask.getException().getMessage());
                                        }
                                        isLoading.setValue(false);
                                    });
                            })
                            .addOnFailureListener(e -> {
                                deletionResult.setValue(false);
                                errorMessage.setValue("Failed to delete user data");
                                isLoading.setValue(false);
                            });
                    } else {
                        deletionResult.setValue(false);
                        errorMessage.setValue("Current password is incorrect");
                        isLoading.setValue(false);
                    }
                });
        }
    }

    public LiveData<User> getUserData() {
        return userData;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<Boolean> getDeletionResult() {
        return deletionResult;
    }
} 