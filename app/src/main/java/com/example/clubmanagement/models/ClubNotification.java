package com.example.clubmanagement.models;

import com.google.firebase.Timestamp;

public class ClubNotification {
    // 알림 타입 상수
    public static final String TYPE_NOTICE = "notice";           // 새 공지
    public static final String TYPE_COMMENT = "comment";         // 새 댓글
    public static final String TYPE_MEMBER_JOIN = "member_join"; // 새 멤버 가입
    public static final String TYPE_ADMIN_GRANT = "admin_grant"; // 관리자 권한 부여

    private String id;
    private String userId;          // 알림 받을 사용자 ID
    private String clubId;
    private String clubName;
    private String type;            // 알림 타입
    private String title;           // 알림 제목
    private String message;         // 알림 내용
    private String targetId;        // 관련 대상 ID (공지 ID, 댓글 ID 등)
    private boolean isRead;         // 읽음 여부
    private Timestamp createdAt;

    // Firebase requires no-argument constructor
    public ClubNotification() {
    }

    public ClubNotification(String userId, String clubId, String clubName,
                           String type, String title, String message, String targetId) {
        this.userId = userId;
        this.clubId = clubId;
        this.clubName = clubName;
        this.type = type;
        this.title = title;
        this.message = message;
        this.targetId = targetId;
        this.isRead = false;
        this.createdAt = Timestamp.now();
    }

    // Static factory methods
    public static ClubNotification createNoticeNotification(String userId, String clubId,
                                                            String clubName, String noticeId,
                                                            String noticeTitle) {
        return new ClubNotification(
            userId, clubId, clubName,
            TYPE_NOTICE,
            "새 공지사항",
            "[" + clubName + "] " + noticeTitle,
            noticeId
        );
    }

    public static ClubNotification createCommentNotification(String userId, String clubId,
                                                             String clubName, String noticeId,
                                                             String commenterName) {
        return new ClubNotification(
            userId, clubId, clubName,
            TYPE_COMMENT,
            "새 댓글",
            commenterName + "님이 댓글을 남겼습니다.",
            noticeId
        );
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getClubId() {
        return clubId;
    }

    public String getClubName() {
        return clubName;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getTargetId() {
        return targetId;
    }

    public boolean isRead() {
        return isRead;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setClubId(String clubId) {
        this.clubId = clubId;
    }

    public void setClubName(String clubName) {
        this.clubName = clubName;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    // Helper methods
    public String getFormattedDate() {
        if (createdAt == null) return "";

        long diff = System.currentTimeMillis() - createdAt.toDate().getTime();
        long minutes = diff / (60 * 1000);
        long hours = diff / (60 * 60 * 1000);
        long days = diff / (24 * 60 * 60 * 1000);

        if (minutes < 1) {
            return "방금 전";
        } else if (minutes < 60) {
            return minutes + "분 전";
        } else if (hours < 24) {
            return hours + "시간 전";
        } else if (days < 7) {
            return days + "일 전";
        } else {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MM.dd", java.util.Locale.KOREA);
            return sdf.format(createdAt.toDate());
        }
    }
}
