package com.example.clubmanagement.models;

import com.google.firebase.Timestamp;

/**
 * 최고 관리자가 동아리 정보를 수정했을 때 기록하는 모델
 * 중간 관리자(동아리 관리자)가 수정 내역과 사유를 확인할 수 있음
 */
public class EditRequest {
    private String id;              // Request ID
    private String clubId;          // 대상 동아리 ID
    private String clubName;        // 대상 동아리 이름
    private String fieldName;       // 수정 대상 필드 (name, description, budget 등)
    private String fieldDisplayName; // 필드 표시명 (동아리명, 설명, 공금 등)
    private String oldValue;        // 기존 값
    private String newValue;        // 새로운 값
    private String reason;          // 수정 사유
    private String requesterId;     // 요청자 ID (최고 관리자)
    private String requesterEmail;  // 요청자 이메일
    private String status;          // pending, approved, rejected
    private Timestamp createdAt;    // 요청 생성일
    private Timestamp processedAt;  // 처리일
    private String processedBy;     // 처리자 ID (동아리 관리자)
    private boolean isRead;         // 읽음 여부

    // Firebase requires no-argument constructor
    public EditRequest() {
    }

    public EditRequest(String clubId, String clubName, String fieldName, String fieldDisplayName,
                       String oldValue, String newValue, String reason,
                       String requesterId, String requesterEmail) {
        this.clubId = clubId;
        this.clubName = clubName;
        this.fieldName = fieldName;
        this.fieldDisplayName = fieldDisplayName;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.reason = reason;
        this.requesterId = requesterId;
        this.requesterEmail = requesterEmail;
        this.status = "pending";
        this.createdAt = Timestamp.now();
        this.isRead = false;
    }

    // Getters
    public String getId() { return id; }
    public String getClubId() { return clubId; }
    public String getClubName() { return clubName; }
    public String getFieldName() { return fieldName; }
    public String getFieldDisplayName() { return fieldDisplayName; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
    public String getReason() { return reason; }
    public String getRequesterId() { return requesterId; }
    public String getRequesterEmail() { return requesterEmail; }
    public String getStatus() { return status; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getProcessedAt() { return processedAt; }
    public String getProcessedBy() { return processedBy; }
    public boolean isRead() { return isRead; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setClubId(String clubId) { this.clubId = clubId; }
    public void setClubName(String clubName) { this.clubName = clubName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    public void setFieldDisplayName(String fieldDisplayName) { this.fieldDisplayName = fieldDisplayName; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }
    public void setReason(String reason) { this.reason = reason; }
    public void setRequesterId(String requesterId) { this.requesterId = requesterId; }
    public void setRequesterEmail(String requesterEmail) { this.requesterEmail = requesterEmail; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setProcessedAt(Timestamp processedAt) { this.processedAt = processedAt; }
    public void setProcessedBy(String processedBy) { this.processedBy = processedBy; }
    public void setRead(boolean read) { this.isRead = read; }

    // 상태 확인 헬퍼 메서드
    public boolean isPending() { return "pending".equals(status); }
    public boolean isApproved() { return "approved".equals(status); }
    public boolean isRejected() { return "rejected".equals(status); }
}
