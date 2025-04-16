package com.pineapple.capture.models;

import com.google.firebase.firestore.Exclude;

import java.util.List;

public class User {
    private String id;
    private String username;
    private String email;
    private List<String> profilePictureUrl;
    private String displayName;

    public User() {
        // Required empty constructor for Firestore
    }

    public User(String id, String username, String email, List<String> profilePictureUrl, String displayName) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.profilePictureUrl = profilePictureUrl;
        this.displayName = displayName;
    }

    @Exclude
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<String> getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(List<String> profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
