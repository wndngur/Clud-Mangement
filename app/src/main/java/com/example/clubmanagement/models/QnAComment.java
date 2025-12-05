package com.example.clubmanagement.models;

import com.google.firebase.Timestamp;

/**
 * Q&A 댓글(답변) 모델
 */
public class QnAComment {
    private String id;
    private String qnaId;           // Q&A 항목 ID
    private String clubId;          // 동아리 ID
    private String content;         // 댓글 내용
    private String authorId;        // 작성자 ID
    private String authorName;      // 작성자 이름
    private boolean isAdmin;        // 관리자 여부
    private Timestamp createdAt;    // 작성 시간

    // Firebase requires no-argument constructor
    public QnAComment() {
    }

    public QnAComment(String id, String qnaId, String clubId, String content,
                      String authorId, String authorName, boolean isAdmin) {
        this.id = id;
        this.qnaId = qnaId;
        this.clubId = clubId;
        this.content = content;
        this.authorId = authorId;
        this.authorName = authorName;
        this.isAdmin = isAdmin;
        this.createdAt = Timestamp.now();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getQnaId() {
        return qnaId;
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

    public boolean isAdmin() {
        return isAdmin;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setQnaId(String qnaId) {
        this.qnaId = qnaId;
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

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
