package com.pineapple.capture.feed;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class FeedItem {
    private String id;
    private String userId;
    private String content;
    private String imageUrl;
    
    @ServerTimestamp
    private Timestamp timestamp;
    
    private int likes;
    private String profilePictureUrl;
    private String username;

    // Required empty constructor for Firestore
    public FeedItem() {
        // Default likes to 0
        this.likes = 0;
    }

    public FeedItem(String userId, String content, String imageUrl, String profilePictureUrl, String username) {
        this.userId = userId;
        this.content = content;
        this.imageUrl = imageUrl;
        this.timestamp = Timestamp.now(); // Will be overridden by server timestamp
        this.likes = 0;
        this.profilePictureUrl = profilePictureUrl;
        this.username = username;
    }

    // Convert to Firestore Map for easier saving
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("content", content);
        map.put("imageUrl", imageUrl);
        map.put("timestamp", Timestamp.now()); // Use current timestamp as server timestamp may not be applied yet
        map.put("likes", likes);
        map.put("profilePictureUrl", profilePictureUrl);
        map.put("username", username);
        return map;
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
    
    public Timestamp getTimestamp() { 
        // Ensure timestamp is never null
        if (timestamp == null) {
            timestamp = Timestamp.now();
        }
        return timestamp; 
    }
    
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    
    // Convenience method to get Date from Timestamp
    public Date getTimestampAsDate() {
        if (timestamp == null) {
            return new Date();
        }
        return timestamp.toDate();
    }
    
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
    
    @Override
    public String toString() {
        return "FeedItem{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", content='" + content + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", timestamp=" + timestamp +
                ", likes=" + likes +
                ", username='" + username + '\'' +
                '}';
    }
}