package com.example.clubmanagement.models;

import com.google.firebase.Timestamp;

public class Club {
    private String id;              // Club ID (unique identifier)
    private String name;            // Club name (동아리명)
    private String description;     // Club description
    private String purpose;         // 설립 목적
    private String schedule;        // 행사 일정
    private String members;         // 부원 명단
    private String location;        // 동아리방 위치
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp foundedAt;    // 동아리 설립일

    // 공금 관련 필드
    private long totalBudget;       // 총 예산 (원)
    private long currentBudget;     // 현재 잔액 (원)

    // 인원 관련 필드
    private int memberCount;        // 현재 인원 수
    private boolean isCentralClub;  // 중앙동아리 여부

    // 중앙동아리 유지 최소 인원 (15명 이상)
    public static final int CENTRAL_CLUB_MAINTAIN_MIN_MEMBERS = 15;
    // 중앙동아리 신청 가능 최소 일수 (약 6개월 = 180일)
    public static final int CENTRAL_CLUB_MIN_DAYS = 180;
    // 중앙동아리 신규 등록 최소 인원 (20명 이상)
    public static final int CENTRAL_CLUB_REGISTER_MIN_MEMBERS = 20;
    // 중앙동아리 신청 가능 최소 개월 수 (6개월)
    public static final int CENTRAL_CLUB_MIN_MONTHS = 6;

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

    public String getPurpose() {
        return purpose;
    }

    public String getSchedule() {
        return schedule;
    }

    public String getMembers() {
        return members;
    }

    public String getLocation() {
        return location;
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

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public void setMembers(String members) {
        this.members = members;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Timestamp getFoundedAt() {
        return foundedAt;
    }

    public void setFoundedAt(Timestamp foundedAt) {
        this.foundedAt = foundedAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    // 공금 관련 Getters & Setters
    public long getTotalBudget() {
        return totalBudget;
    }

    public void setTotalBudget(long totalBudget) {
        this.totalBudget = totalBudget;
    }

    public long getCurrentBudget() {
        return currentBudget;
    }

    public void setCurrentBudget(long currentBudget) {
        this.currentBudget = currentBudget;
    }

    // 공금 사용 비율 계산 (0.0 ~ 1.0)
    public float getBudgetUsageRatio() {
        if (totalBudget <= 0) return 0f;
        return (float) currentBudget / totalBudget;
    }

    // 공금 사용 퍼센트 계산 (0 ~ 100)
    public int getBudgetUsagePercent() {
        return (int) (getBudgetUsageRatio() * 100);
    }

    // 인원 관련 Getters & Setters
    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public boolean isCentralClub() {
        return isCentralClub;
    }

    public void setCentralClub(boolean centralClub) {
        isCentralClub = centralClub;
    }

    // 중앙동아리 유지 가능 여부 (17명 이상)
    public boolean canMaintainCentralStatus() {
        return memberCount >= CENTRAL_CLUB_MAINTAIN_MIN_MEMBERS;
    }

    // 중앙동아리 신규 등록 가능 여부 (20명 이상)
    public boolean canRegisterAsCentral() {
        return memberCount >= CENTRAL_CLUB_REGISTER_MIN_MEMBERS;
    }

    // 중앙동아리 유지까지 필요한 인원 수
    public int getMembersNeededToMaintain() {
        if (memberCount >= CENTRAL_CLUB_MAINTAIN_MIN_MEMBERS) {
            return 0;
        }
        return CENTRAL_CLUB_MAINTAIN_MIN_MEMBERS - memberCount;
    }

    // 중앙동아리 등록까지 필요한 인원 수
    public int getMembersNeededForCentral() {
        if (memberCount >= CENTRAL_CLUB_REGISTER_MIN_MEMBERS) {
            return 0;
        }
        return CENTRAL_CLUB_REGISTER_MIN_MEMBERS - memberCount;
    }

    // 인원 현황 비율 계산 (중앙동아리: 17명 기준, 일반동아리: 20명 기준)
    public float getMemberRatio(boolean isCentral) {
        int targetMembers = isCentral ? CENTRAL_CLUB_MAINTAIN_MIN_MEMBERS : CENTRAL_CLUB_REGISTER_MIN_MEMBERS;
        if (memberCount >= targetMembers) {
            return 1.0f;
        }
        return (float) memberCount / targetMembers;
    }

    // 인원 현황 퍼센트 계산 (0 ~ 100)
    public int getMemberPercent(boolean isCentral) {
        return (int) (getMemberRatio(isCentral) * 100);
    }

    // 설립일로부터 경과한 일수 계산
    public long getDaysSinceFounding() {
        if (foundedAt == null) return 0;
        long foundedMillis = foundedAt.toDate().getTime();
        long currentMillis = System.currentTimeMillis();
        long diffMillis = currentMillis - foundedMillis;
        return diffMillis / (1000 * 60 * 60 * 24); // 밀리초를 일수로 변환
    }

    // 중앙동아리 신청 가능 여부 (설립 후 6개월 = 180일 이상)
    public boolean canApplyForCentralByDate() {
        return getDaysSinceFounding() >= CENTRAL_CLUB_MIN_DAYS;
    }

    // 중앙동아리 신청까지 남은 일수
    public long getDaysUntilCentralEligible() {
        long daysSinceFounding = getDaysSinceFounding();
        if (daysSinceFounding >= CENTRAL_CLUB_MIN_DAYS) {
            return 0;
        }
        return CENTRAL_CLUB_MIN_DAYS - daysSinceFounding;
    }

    // 설립일 진행률 계산 (0.0 ~ 1.0)
    public float getFoundingProgressRatio() {
        long daysSinceFounding = getDaysSinceFounding();
        if (daysSinceFounding >= CENTRAL_CLUB_MIN_DAYS) {
            return 1.0f;
        }
        return (float) daysSinceFounding / CENTRAL_CLUB_MIN_DAYS;
    }

    // 설립일 진행률 퍼센트 (0 ~ 100)
    public int getFoundingProgressPercent() {
        return (int) (getFoundingProgressRatio() * 100);
    }
}
