package com.pineapple.capture.auth;

import androidx.annotation.NonNull;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pineapple.capture.models.User;

public class AuthManager {
    private static AuthManager instance;
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private static final String USERS_COLLECTION = "users";
    private static final String PROFILE_PICTURES = "profile_pictures";

    private AuthManager() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized AuthManager getInstance() {
        if (instance == null) {
            instance = new AuthManager();
        }
        return instance;
    }

    // Callback interface for username uniqueness
    public interface UsernameUniqueCallback {
        void onResult(boolean isUnique);
    }

    // Check if username is unique in Firestore
    public void isUsernameUnique(String username, UsernameUniqueCallback callback) {
        db.collection(USERS_COLLECTION)
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                callback.onResult(queryDocumentSnapshots.isEmpty());
            })
            .addOnFailureListener(e -> {
                // On failure, treat as not unique to be safe
                callback.onResult(false);
            });
    }

    // Updated signup to accept username
    public Task<AuthResult> signup(String displayName, String username, String email, String password) {
        return auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        // Update display name in Firebase Auth
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(displayName)
                                .build();
                        firebaseUser.updateProfile(profileUpdates);

                        // Create user document in Firestore
                        User user = new User(
                                firebaseUser.getUid(),
                                username, // username
                                email,
                                null, // No profile picture initially
                                displayName
                        );
                        db.collection(USERS_COLLECTION)
                                .document(firebaseUser.getUid())
                                .set(user);
                    }
                });
    }

    public Task<AuthResult> login(String email, String password) {
        return auth.signInWithEmailAndPassword(email, password);
    }


    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }


    public Task<User> getUserData(@NonNull String uid) {
        return db.collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        return task.getResult().toObject(User.class);
                    }
                    return null;
                });
    }

    public Task<Void> sendPasswordResetEmail(String email) {
        return auth.sendPasswordResetEmail(email);
    }


} 