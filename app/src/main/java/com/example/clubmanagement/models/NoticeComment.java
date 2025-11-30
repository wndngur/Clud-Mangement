package com.example.clubmanagement.models;

import com.google.firebase.Timestamp;

public class NoticeComment {
    private String id;
    private String noticeId;
    private String clubId;
    private String content;
    private String authorId;
    private String authorName;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private boolean edited;

    // Firebase requires no-argument constructor
    public NoticeComment() {
    }

    public NoticeComment(String noticeId, String clubId, String content, String authorId, String authorName) {
        this.noticeId = noticeId;
        this.clubId = clubId;
        this.content = content;
        this.authorId = authorId;
        this.authorName = authorName;
        this.createdAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
        this.edited = false;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getNoticeId() {
        return noticeId;
    }

    public String getClubId() {
        return clubId;
    }

    public String getContent() {
        return content;
    }

    public String getAuthorId() {
        return authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public boolean isEdited() {
        return edited;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setNoticeId(String noticeId) {
        this.noticeId = noticeId;
    }

    public void setClubId(String clubId) {
        this.clubId = clubId;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setEdited(boolean edited) {
        this.edited = edited;
    }

    // Helper methods
    public String getFormattedDate() {
        if (createdAt == null) return "";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MM.dd HH:mm", java.util.Locale.KOREA);
        return sdf.format(createdAt.toDate());
    }

    public boolean isAuthor(String userId) {
        return authorId != null && authorId.equals(userId);
    }
}
