package com.example.clubmanagement.models;

import com.google.firebase.Timestamp;

public class ClubNotice {
    private String id;
    private String clubId;
    private String title;
    private String content;
    private String authorId;
    private String authorName;
    private int commentCount;
    private int viewCount;
    private boolean isPinned; // 상단 고정 여부
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Firebase requires no-argument constructor
    public ClubNotice() {
    }

    public ClubNotice(String clubId, String title, String content, String authorId, String authorName) {
        this.clubId = clubId;
        this.title = title;
        this.content = content;
        this.authorId = authorId;
        this.authorName = authorName;
        this.commentCount = 0;
        this.viewCount = 0;
        this.isPinned = false;
        this.createdAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getClubId() {
        return clubId;
    }

    public String getTitle() {
        return title;
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

    public int getCommentCount() {
        return commentCount;
    }

    public int getViewCount() {
        return viewCount;
    }

    public boolean isPinned() {
        return isPinned;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setClubId(String clubId) {
        this.clubId = clubId;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    public void setPinned(boolean pinned) {
        isPinned = pinned;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Helper methods
    public String getFormattedDate() {
        if (createdAt == null) return "";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy.MM.dd HH:mm", java.util.Locale.KOREA);
        return sdf.format(createdAt.toDate());
    }

    public String getShortDate() {
        if (createdAt == null) return "";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MM.dd", java.util.Locale.KOREA);
        return sdf.format(createdAt.toDate());
    }
}
