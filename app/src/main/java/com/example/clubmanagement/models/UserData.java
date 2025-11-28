package com.example.clubmanagement.models;

import com.google.firebase.Timestamp;

public class UserData {
    private String userId;
    private String adminLevel;      // "NONE", "CLUB_ADMIN", "SUPER_ADMIN"
    private String clubId;          // For CLUB_ADMIN: which club they manage
    private Timestamp lastUpdated;

    // Firebase requires no-argument constructor
    public UserData() {
    }

    public UserData(String userId, String adminLevel, String clubId) {
        this.userId = userId;
        this.adminLevel = adminLevel != null ? adminLevel : "NONE";
        this.clubId = clubId;
        this.lastUpdated = Timestamp.now();
    }

    // Getters
    public String getUserId() {
        return userId;
    }

    public String getAdminLevel() {
        return adminLevel;
    }

    public String getClubId() {
        return clubId;
    }

    public Timestamp getLastUpdated() {
        return lastUpdated;
    }

    // Setters
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setAdminLevel(String adminLevel) {
        this.adminLevel = adminLevel;
    }

    public void setClubId(String clubId) {
        this.clubId = clubId;
    }

    public void setLastUpdated(Timestamp lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    // Helper methods
    public AdminLevel getAdminLevelEnum() {
        if (adminLevel == null) return AdminLevel.NONE;
        try {
            return AdminLevel.valueOf(adminLevel);
        } catch (IllegalArgumentException e) {
            return AdminLevel.NONE;
        }
    }

    public boolean isSuperAdmin() {
        return AdminLevel.SUPER_ADMIN.name().equals(adminLevel);
    }

    public boolean isClubAdmin() {
        return AdminLevel.CLUB_ADMIN.name().equals(adminLevel);
    }

    public boolean isAnyAdmin() {
        return isSuperAdmin() || isClubAdmin();
    }
}
