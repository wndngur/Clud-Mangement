package com.example.clubmanagement.models;

import com.google.firebase.firestore.Exclude;

public class Member {
    private String userId;
    private String name;
    private String email;
    private String studentId;
    private String department;
    private String phone;
    private String role; // "회장", "부회장", "총무", "회계", "회원"
    private boolean isAdmin;
    private Object joinedAt; // Firebase Timestamp 또는 Long
    private String joinDate; // "yyyy.MM.dd" 형식 가입일
    private String requestStatus; // "pending", "approved", "rejected"
    private String signatureUrl; // 서명 이미지 URL
    private int birthMonth; // 생일 월 (1-12, 0이면 미입력)
    private int birthDay;   // 생일 일 (1-31, 0이면 미입력)
    private String applicationId; // membershipApplications 문서 ID (가입 신청에서 온 경우)

    // Empty constructor for Firebase
    public Member() {
    }

    public Member(String userId, String name, String studentId, String department) {
        this.userId = userId;
        this.name = name;
        this.studentId = studentId;
        this.department = department;
        this.isAdmin = false;
        this.joinedAt = Long.valueOf(System.currentTimeMillis());
    }

    public Member(String userId, String name, String studentId, String department, boolean isAdmin) {
        this.userId = userId;
        this.name = name;
        this.studentId = studentId;
        this.department = department;
        this.isAdmin = isAdmin;
        this.joinedAt = Long.valueOf(System.currentTimeMillis());
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    // Firebase Firestore가 isAdmin 필드를 역직렬화할 때 사용
    public void setIsAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    // Firebase Firestore 직렬화용 getter
    public boolean getIsAdmin() {
        return isAdmin;
    }

    @Exclude
    public long getJoinedAtLong() {
        if (joinedAt == null) {
            return 0;
        }
        if (joinedAt instanceof Long) {
            return (Long) joinedAt;
        }
        if (joinedAt instanceof com.google.firebase.Timestamp) {
            return ((com.google.firebase.Timestamp) joinedAt).toDate().getTime();
        }
        if (joinedAt instanceof Number) {
            return ((Number) joinedAt).longValue();
        }
        return 0;
    }

    public Object getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Object joinedAt) {
        this.joinedAt = joinedAt;
    }

    public void setJoinedAtLong(long joinedAt) {
        this.joinedAt = joinedAt;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(String joinDate) {
        this.joinDate = joinDate;
    }

    public String getSignatureUrl() {
        return signatureUrl;
    }

    public void setSignatureUrl(String signatureUrl) {
        this.signatureUrl = signatureUrl;
    }

    public int getBirthMonth() {
        return birthMonth;
    }

    public void setBirthMonth(int birthMonth) {
        this.birthMonth = birthMonth;
    }

    public int getBirthDay() {
        return birthDay;
    }

    public void setBirthDay(int birthDay) {
        this.birthDay = birthDay;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    // 생일이 설정되어 있는지 확인
    public boolean hasBirthday() {
        return birthMonth > 0 && birthDay > 0;
    }

    // 생일 문자열 반환 (MM월 DD일 형식)
    public String getBirthdayString() {
        if (!hasBirthday()) {
            return "";
        }
        return birthMonth + "월 " + birthDay + "일";
    }

    // 오늘이 생일인지 확인
    public boolean isBirthdayToday() {
        if (!hasBirthday()) {
            return false;
        }
        java.util.Calendar today = java.util.Calendar.getInstance();
        int todayMonth = today.get(java.util.Calendar.MONTH) + 1; // 0-indexed
        int todayDay = today.get(java.util.Calendar.DAY_OF_MONTH);
        return birthMonth == todayMonth && birthDay == todayDay;
    }
}
