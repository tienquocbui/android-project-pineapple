package com.pineapple.capture.models;

import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String id;
    private String username;
    private String email;
    private List<String> profilePictureUrl;
    private String displayName;
    private String bio;
    private String location;
    private List<String> followers = new ArrayList<>();
    private List<String> following = new ArrayList<>();

    public User() {
        // Required empty constructor for Firestore
    }

    public User(String id, String username, String email, List<String> profilePictureUrl, 
                String displayName, String bio, String location) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.profilePictureUrl = profilePictureUrl;
        this.displayName = displayName;
        this.bio = bio;
        this.location = location;
        this.followers = new ArrayList<>();
        this.following = new ArrayList<>();
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
    
    public String getBio() {
        return bio;
    }
    
    public void setBio(String bio) {
        this.bio = bio;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public List<String> getFollowers() {
        if (followers == null) {
            followers = new ArrayList<>();
        }
        return followers;
    }
    
    public void setFollowers(List<String> followers) {
        this.followers = followers;
    }
    
    public List<String> getFollowing() {
        if (following == null) {
            following = new ArrayList<>();
        }
        return following;
    }
    
    public void setFollowing(List<String> following) {
        this.following = following;
    }
    
    @Exclude
    public int getFollowersCount() {
        return followers != null ? followers.size() : 0;
    }
    
    @Exclude
    public int getFollowingCount() {
        return following != null ? following.size() : 0;
    }
    
    @Exclude
    public int getFriendsCount() {
        // Friends are mutual followers (people who follow you and you follow them)
        if (followers == null || following == null) {
            return 0;
        }
        
        int count = 0;
        for (String followerId : followers) {
            if (following.contains(followerId)) {
                count++;
            }
        }
        return count;
    }
}
