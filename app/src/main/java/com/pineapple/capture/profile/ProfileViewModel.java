package com.pineapple.capture.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileViewModel extends ViewModel {
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private MutableLiveData<UserProfile> userProfile;

    public ProfileViewModel() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        userProfile = new MutableLiveData<>();
        loadUserProfile();
    }

    private void loadUserProfile() {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId != null) {
            db.collection("users").document(userId)
                .addSnapshotListener((document, error) -> {
                    if (error != null) {
                        return;
                    }
                    if (document != null && document.exists()) {
                        UserProfile profile = document.toObject(UserProfile.class);
                        userProfile.setValue(profile);
                    }
                });
        }
    }

    public void updateProfile(String name, String bio) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId != null) {
            UserProfile profile = new UserProfile(name, bio);
            db.collection("users").document(userId)
                .set(profile);
        }
    }

    public LiveData<UserProfile> getUserProfile() {
        return userProfile;
    }
} 