package com.example.clubmanagement.models;

import com.google.firebase.Timestamp;

/**
 * 동아리 탈퇴 신청 모델
 */
public class WithdrawalRequest {
    private String id;
    private String clubId;
    private String clubName;
    private String userId;
    private String userEmail;
    private String userName;
    private String reason;          // 탈퇴 사유
    private String status;          // pending, approved, rejected
    private Timestamp createdAt;

    // 기본 생성자 (Firebase용)
    public WithdrawalRequest() {
        this.status = "pending";
    }

    public WithdrawalRequest(String clubId, String clubName, String userId, String userEmail, String userName, String reason) {
        this.clubId = clubId;
        this.clubName = clubName;
        this.userId = userId;
        this.userEmail = userEmail;
        this.userName = userName;
        this.reason = reason;
        this.status = "pending";
        this.createdAt = Timestamp.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClubId() {
        return clubId;
    }

    public void setClubId(String clubId) {
        this.clubId = clubId;
    }

    public String getClubName() {
        return clubName;
    }

    public void setClubName(String clubName) {
        this.clubName = clubName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getFormattedDate() {
        if (createdAt != null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.KOREA);
            return sdf.format(createdAt.toDate());
        }
        return "";
    }
}
