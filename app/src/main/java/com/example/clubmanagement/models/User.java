package com.example.clubmanagement.models;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String uid;
    private String name;
    private String email;
    private String centralClubId;  // 가입한 중앙동아리 ID (null이면 미가입)
    private String centralClubName; // 가입한 중앙동아리 이름
    private String joinDate;        // 가입 날짜
    private List<String> generalClubIds; // 가입한 일반 동아리 ID 목록
    private List<String> generalClubNames; // 가입한 일반 동아리 이름 목록

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
}
