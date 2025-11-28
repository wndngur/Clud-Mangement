package com.example.clubmanagement.models;

import com.google.firebase.Timestamp;

public class Club {
    private String id;              // Club ID (unique identifier)
    private String name;            // Club name (동아리명)
    private String description;     // Club description
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Firebase requires no-argument constructor
    public Club() {
    }

    public Club(String id, String name) {
        this.id = id;
        this.name = name;
        this.createdAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
    }

    public Club(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
