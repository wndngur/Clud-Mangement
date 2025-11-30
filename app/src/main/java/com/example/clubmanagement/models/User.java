package com.example.clubmanagement.models;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String uid;
    private String name;
    private String email;
    private String studentId;       // 학번
    private String department;      // 학과
    private String phone;           // 전화번호
    private String centralClubId;  // 가입한 중앙동아리 ID (null이면 미가입)
    private String centralClubName; // 가입한 중앙동아리 이름
    private String joinDate;        // 가입 날짜
    private List<String> generalClubIds; // 가입한 일반 동아리 ID 목록
    private List<String> generalClubNames; // 가입한 일반 동아리 이름 목록
    private List<ExpulsionRecord> expulsionHistory; // 퇴출 이력

    /**
     * 퇴출 이력 기록 클래스
     */
    public static class ExpulsionRecord {
        private String clubId;
        private String clubName;
        private String reason;
        private long expelledAt;

        public ExpulsionRecord() {
        }

        public ExpulsionRecord(String clubId, String clubName, String reason) {
            this.clubId = clubId;
            this.clubName = clubName;
            this.reason = reason;
            this.expelledAt = System.currentTimeMillis();
        }

        public String getClubId() { return clubId; }
        public void setClubId(String clubId) { this.clubId = clubId; }
        public String getClubName() { return clubName; }
        public void setClubName(String clubName) { this.clubName = clubName; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public long getExpelledAt() { return expelledAt; }
        public void setExpelledAt(long expelledAt) { this.expelledAt = expelledAt; }

        public String getFormattedDate() {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.KOREA);
            return sdf.format(new java.util.Date(expelledAt));
        }
    }

    public User() {
        // Default constructor required for Firebase
        this.generalClubIds = new ArrayList<>();
        this.generalClubNames = new ArrayList<>();
    }

    public User(String uid, String email) {
        this.uid = uid;
        this.email = email;
        this.generalClubIds = new ArrayList<>();
        this.generalClubNames = new ArrayList<>();
    }

    // Getters and Setters
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCentralClubId() {
        return centralClubId;
    }

    public void setCentralClubId(String centralClubId) {
        this.centralClubId = centralClubId;
    }

    public String getCentralClubName() {
        return centralClubName;
    }

    public void setCentralClubName(String centralClubName) {
        this.centralClubName = centralClubName;
    }

    public String getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(String joinDate) {
        this.joinDate = joinDate;
    }

    public List<String> getGeneralClubIds() {
        if (generalClubIds == null) {
            generalClubIds = new ArrayList<>();
        }
        return generalClubIds;
    }

    public void setGeneralClubIds(List<String> generalClubIds) {
        this.generalClubIds = generalClubIds != null ? generalClubIds : new ArrayList<>();
    }

    public List<String> getGeneralClubNames() {
        if (generalClubNames == null) {
            generalClubNames = new ArrayList<>();
        }
        return generalClubNames;
    }

    public void setGeneralClubNames(List<String> generalClubNames) {
        this.generalClubNames = generalClubNames != null ? generalClubNames : new ArrayList<>();
    }

    public boolean hasJoinedCentralClub() {
        return centralClubId != null && !centralClubId.isEmpty();
    }

    public boolean hasJoinedGeneralClub(String clubId) {
        return generalClubIds != null && generalClubIds.contains(clubId);
    }

    public int getGeneralClubCount() {
        return generalClubIds != null ? generalClubIds.size() : 0;
    }

    // Firebase Firestore가 generalClubCount 필드를 역직렬화할 때 사용
    public void setGeneralClubCount(int count) {
        // 읽기 전용 - generalClubIds 기반으로 계산됨
    }

    public List<ExpulsionRecord> getExpulsionHistory() {
        if (expulsionHistory == null) {
            expulsionHistory = new ArrayList<>();
        }
        return expulsionHistory;
    }

    public void setExpulsionHistory(List<ExpulsionRecord> expulsionHistory) {
        this.expulsionHistory = expulsionHistory != null ? expulsionHistory : new ArrayList<>();
    }

    public void addExpulsionRecord(String clubId, String clubName, String reason) {
        if (expulsionHistory == null) {
            expulsionHistory = new ArrayList<>();
        }
        expulsionHistory.add(new ExpulsionRecord(clubId, clubName, reason));
    }

    public boolean hasExpulsionHistory() {
        return expulsionHistory != null && !expulsionHistory.isEmpty();
    }
}
