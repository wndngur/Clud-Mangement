package com.example.clubmanagement.models;

import com.google.firebase.Timestamp;

public class Banner {
    private String id;
    private String imageUrl;
    private String title;
    private String description;
    private String linkUrl;      // Optional external link
    private Timestamp updatedAt;

    // Firebase requires no-argument constructor
    public Banner() {
    }

    public Banner(String id, String imageUrl, String title, String description, String linkUrl) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.title = title;
        this.description = description;
        this.linkUrl = linkUrl;
        this.updatedAt = Timestamp.now();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
