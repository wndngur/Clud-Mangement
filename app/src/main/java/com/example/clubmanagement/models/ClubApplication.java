package com.example.clubmanagement.models;

import com.google.firebase.Timestamp;

/**
 * 일반 동아리 설립 신청 모델
 */
public class ClubApplication {
    private String id;
    private String clubName;           // 동아리명
    private String description;        // 동아리 설명
    private String purpose;            // 설립 목적
    private String activityPlan;       // 활동 계획
    private String applicantId;        // 신청자 ID
    private String applicantEmail;     // 신청자 이메일
    private String applicantName;      // 신청자 이름
    private Timestamp createdAt;       // 신청 일시
    private String status;             // pending, approved, rejected

    // 기본 생성자 (Firebase용)
    public ClubApplication() {
        this.status = "pending";
    }

    public ClubApplication(String clubName, String description, String purpose, String activityPlan,
                           String applicantId, String applicantEmail, String applicantName) {
        this.clubName = clubName;
        this.description = description;
        this.purpose = purpose;
        this.activityPlan = activityPlan;
        this.applicantId = applicantId;
        this.applicantEmail = applicantEmail;
        this.applicantName = applicantName;
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

    public String getClubName() {
        return clubName;
    }

    public void setClubName(String clubName) {
        this.clubName = clubName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getActivityPlan() {
        return activityPlan;
    }

    public void setActivityPlan(String activityPlan) {
        this.activityPlan = activityPlan;
    }

    public String getApplicantId() {
        return applicantId;
    }

    public void setApplicantId(String applicantId) {
        this.applicantId = applicantId;
    }

    public String getApplicantEmail() {
        return applicantEmail;
    }

    public void setApplicantEmail(String applicantEmail) {
        this.applicantEmail = applicantEmail;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFormattedDate() {
        if (createdAt != null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.KOREA);
            return sdf.format(createdAt.toDate());
        }
        return "";
    }
}
