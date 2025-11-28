package com.example.clubmanagement.models;

import com.google.firebase.Timestamp;

public class Notice {
    private String id;
    private String title;
    private String content;
    private int position;       // Order in list (0, 1, 2)
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private String creatorId;

    // Firebase requires no-argument constructor
    public Notice() {
    }

    public Notice(String id, String title, String content, int position) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.position = position;
        this.createdAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public int getPosition() {
        return position;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public String getCreatorId() {
        return creatorId;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }
}
