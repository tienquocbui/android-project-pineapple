package com.pineapple.capture.feed;

import com.google.firebase.Timestamp;

public class FeedItem {
    private String id;
    private String userId;
    private String content;
    private String imageUrl;
    private Timestamp timestamp;
    private int likes;

    // Required empty constructor for Firestore
    public FeedItem() {}

    public FeedItem(String userId,  String content, String imageUrl) {
        this.userId = userId;
        this.content = content;
        this.imageUrl = imageUrl;
        this.timestamp = Timestamp.now();
        this.likes = 0;
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

    public byte[] getProfilePictureUrl() {
        return new byte[0];
    }

}