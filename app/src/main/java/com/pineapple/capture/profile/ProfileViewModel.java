package com.pineapple.capture.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
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

    public void updateEmail(String currentPassword, String newEmail) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            errorMessage.setValue("No user is currently signed in");
            return;
        }

        // First, re-authenticate the user
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
        user.reauthenticate(credential)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // After successful re-authentication, update the email
                    user.updateEmail(newEmail)
                        .addOnCompleteListener(emailTask -> {
                            if (emailTask.isSuccessful()) {
                                // Update email in Firestore
                                db.collection("users").document(user.getUid())
                                    .update("email", newEmail)
                                    .addOnSuccessListener(aVoid -> {
                                        // Update the local user data
                                        User currentUser = userData.getValue();
                                        if (currentUser != null) {
                                            currentUser.setEmail(newEmail);
                                            userData.setValue(currentUser);
                                        }
                                        errorMessage.setValue("Email updated successfully");
                                    })
                                    .addOnFailureListener(e -> {
                                        // If Firestore update fails, revert the email in Firebase Auth
                                        user.updateEmail(user.getEmail())
                                            .addOnCompleteListener(revertTask -> {
                                                if (!revertTask.isSuccessful()) {
                                                    errorMessage.setValue("Failed to update email and couldn't revert changes. Please contact support.");
                                                } else {
                                                    errorMessage.setValue("Failed to update email in database. Changes reverted.");
                                                }
                                            });
                                    });
                            } else {
                                Exception exception = emailTask.getException();
                                if (exception instanceof FirebaseAuthRecentLoginRequiredException) {
                                    errorMessage.setValue("Please sign in again to change your email");
                                } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                                    errorMessage.setValue("Invalid email format");
                                } else {
                                    errorMessage.setValue("Failed to update email: " + exception.getMessage());
                                }
                            }
                        });
                } else {
                    Exception exception = task.getException();
                    if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                        errorMessage.setValue("Current password is incorrect");
                    } else if (exception instanceof FirebaseAuthRecentLoginRequiredException) {
                        errorMessage.setValue("Please sign in again to change your email");
                    } else {
                        errorMessage.setValue("Authentication failed: " + exception.getMessage());
                    }
                }
            });
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

    public void updateDisplayName(String newDisplayName) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            errorMessage.setValue("No user is currently signed in");
            return;
        }

        // Update display name in Firestore
        db.collection("users").document(user.getUid())
            .update("displayName", newDisplayName)
            .addOnSuccessListener(aVoid -> {
                // Update the local user data
                User currentUser = userData.getValue();
                if (currentUser != null) {
                    currentUser.setDisplayName(newDisplayName);
                    userData.setValue(currentUser);
                }
                errorMessage.setValue("Display name updated successfully");
            })
            .addOnFailureListener(e -> {
                errorMessage.setValue("Failed to update display name: " + e.getMessage());
            });
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