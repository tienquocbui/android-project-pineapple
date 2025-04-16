package com.pineapple.capture.feed;

import com.google.firebase.Timestamp;

public class FeedItem {
    private String id;
    private String userId;
    private String content;
    private String imageUrl;
    private Timestamp timestamp;
    private int likes;
    private String profilePictureUrl;
    private String username;

    // Required empty constructor for Firestore
    public FeedItem() {}

    public FeedItem(String userId, String content, String imageUrl, String profilePictureUrl, String username) {
        this.userId = userId;
        this.content = content;
        this.imageUrl = imageUrl;
        this.timestamp = Timestamp.now();
        this.likes = 0;
        this.profilePictureUrl = profilePictureUrl;
        this.username = username;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    
    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }
    
    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
}