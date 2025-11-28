package com.example.clubmanagement.models;

import com.google.firebase.Timestamp;

public class AdminPassword {
    private String id;
    private String level;           // "SUPER_ADMIN" or "CLUB_ADMIN"
    private String password;        // Hashed password
    private String clubId;          // For CLUB_ADMIN only
    private Timestamp updatedAt;

    // Firebase requires no-argument constructor
    public AdminPassword() {
    }

    public AdminPassword(String id, String level, String password, String clubId) {
        this.id = id;
        this.level = level;
        this.password = password;
        this.clubId = clubId;
        this.updatedAt = Timestamp.now();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getLevel() {
        return level;
    }

    public String getPassword() {
        return password;
    }

    public String getClubId() {
        return clubId;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setClubId(String clubId) {
        this.clubId = clubId;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
