package com.example.clubmanagement.models;

import com.google.firebase.Timestamp;

import java.util.List;
import java.util.Map;

public class CentralClubApplication {
    private String id;
    private String clubId;
    private String clubName;
    private String applicantId;      // 신청자 ID
    private String applicantEmail;   // 신청자 이메일
    private int memberCount;         // 신청 시점의 부원 수
    private long daysSinceFounding;  // 신청 시점의 설립 후 일수
    private String status;           // pending, approved, rejected
    private String reason;           // 신청 사유 (선택)
    private String rejectReason;     // 거절 사유 (거절 시)
    private Timestamp createdAt;
    private Timestamp processedAt;   // 처리 일시
    private long expectedBudget;     // 예상 연간 사용 금액
    private Map<String, String> monthlyPlans; // 월별 활동 계획 (1~12월)
    private List<String> memberSignatures;    // 부원 서명 URL 목록
    private int signatureCount;      // 서명한 부원 수

    // Status constants
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_REJECTED = "rejected";

    public CentralClubApplication() {
    }

    public CentralClubApplication(String clubId, String clubName, String applicantId,
                                   String applicantEmail, int memberCount, long daysSinceFounding) {
        this.clubId = clubId;
        this.clubName = clubName;
        this.applicantId = applicantId;
        this.applicantEmail = applicantEmail;
        this.memberCount = memberCount;
        this.daysSinceFounding = daysSinceFounding;
        this.status = STATUS_PENDING;
        this.createdAt = Timestamp.now();
    }

    // Getters
    public String getId() { return id; }
    public String getClubId() { return clubId; }
    public String getClubName() { return clubName; }
    public String getApplicantId() { return applicantId; }
    public String getApplicantEmail() { return applicantEmail; }
    public int getMemberCount() { return memberCount; }
    public long getDaysSinceFounding() { return daysSinceFounding; }
    public String getStatus() { return status; }
    public String getReason() { return reason; }
    public String getRejectReason() { return rejectReason; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getProcessedAt() { return processedAt; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setClubId(String clubId) { this.clubId = clubId; }
    public void setClubName(String clubName) { this.clubName = clubName; }
    public void setApplicantId(String applicantId) { this.applicantId = applicantId; }
    public void setApplicantEmail(String applicantEmail) { this.applicantEmail = applicantEmail; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }
    public void setDaysSinceFounding(long daysSinceFounding) { this.daysSinceFounding = daysSinceFounding; }
    public void setStatus(String status) { this.status = status; }
    public void setReason(String reason) { this.reason = reason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setProcessedAt(Timestamp processedAt) { this.processedAt = processedAt; }
    public long getExpectedBudget() { return expectedBudget; }
    public void setExpectedBudget(long expectedBudget) { this.expectedBudget = expectedBudget; }
    public Map<String, String> getMonthlyPlans() { return monthlyPlans; }
    public void setMonthlyPlans(Map<String, String> monthlyPlans) { this.monthlyPlans = monthlyPlans; }
    public List<String> getMemberSignatures() { return memberSignatures; }
    public void setMemberSignatures(List<String> memberSignatures) { this.memberSignatures = memberSignatures; }
    public int getSignatureCount() { return signatureCount; }
    public void setSignatureCount(int signatureCount) { this.signatureCount = signatureCount; }

    // Helper methods
    public boolean isPending() {
        return STATUS_PENDING.equals(status);
    }

    public boolean isApproved() {
        return STATUS_APPROVED.equals(status);
    }

    public boolean isRejected() {
        return STATUS_REJECTED.equals(status);
    }

    public String getStatusDisplayName() {
        if (STATUS_PENDING.equals(status)) return "대기중";
        if (STATUS_APPROVED.equals(status)) return "승인됨";
        if (STATUS_REJECTED.equals(status)) return "거절됨";
        return "알 수 없음";
    }
}
