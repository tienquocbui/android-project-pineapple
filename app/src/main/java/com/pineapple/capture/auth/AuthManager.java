package com.pineapple.capture.auth;

import android.net.Uri;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.pineapple.capture.models.User;
import java.util.Objects;

public class AuthManager {
    private static AuthManager instance;
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final FirebaseStorage storage;
    private static final String USERS_COLLECTION = "users";
    private static final String PROFILE_PICTURES = "profile_pictures";

    private AuthManager() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public static synchronized AuthManager getInstance() {
        if (instance == null) {
            instance = new AuthManager();
        }
        return instance;
    }

    public Task<AuthResult> signup(String displayName, String email, String password) {
        return signUp(email, password, displayName);
    }

    public Task<AuthResult> signUp(String email, String password, String displayName) {
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
                                email,
                                email, // username
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

    public void logout() {
        auth.signOut();
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public Task<Void> updateProfilePicture(Uri imageUri) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            return null;
        }

        StorageReference storageRef = storage.getReference()
                .child(PROFILE_PICTURES)
                .child(user.getUid() + ".jpg");

        return storageRef.putFile(imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw Objects.requireNonNull(task.getException());
                    }
                    return storageRef.getDownloadUrl();
                })
                .continueWithTask(task -> {
                    String downloadUrl = task.getResult().toString();
                    
                    // Update Firebase User profile
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setPhotoUri(Uri.parse(downloadUrl))
                            .build();
                    
                    // Update Firestore document
                    db.collection(USERS_COLLECTION)
                            .document(user.getUid())
                            .update("profilePictureUrl", downloadUrl);
                            
                    return user.updateProfile(profileUpdates);
                });
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

    public Task<Void> sendEmailVerification() {
        FirebaseUser user = getCurrentUser();
        if (user != null) {
            return user.sendEmailVerification();
        }
        return null;
    }

    public boolean isEmailVerified() {
        FirebaseUser user = getCurrentUser();
        return user != null && user.isEmailVerified();
    }
} 