package com.example.clubmanagement.models;

import com.google.firebase.Timestamp;

public class DocumentData {
    private String docId;
    private String title;
    private String type;                 // "activity_report", "meeting_minutes", etc.
    private String content;
    private boolean requiresSignature;
    private boolean signaturePlaced;
    private SignaturePosition signaturePosition;
    private String creatorId;
    private Timestamp createdAt;
    private String pdfUrl;

    public static class SignaturePosition {
        private float x;
        private float y;
        private float width;
        private float height;

        public SignaturePosition() {}

        public SignaturePosition(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public float getX() { return x; }
        public float getY() { return y; }
        public float getWidth() { return width; }
        public float getHeight() { return height; }

        public void setX(float x) { this.x = x; }
        public void setY(float y) { this.y = y; }
        public void setWidth(float width) { this.width = width; }
        public void setHeight(float height) { this.height = height; }
    }

    public DocumentData() {}

    public DocumentData(String docId, String title, String type, String content,
                       boolean requiresSignature, String creatorId) {
        this.docId = docId;
        this.title = title;
        this.type = type;
        this.content = content;
        this.requiresSignature = requiresSignature;
        this.signaturePlaced = false;
        this.creatorId = creatorId;
        this.createdAt = Timestamp.now();

        // 기본 서명 위치 설정
        this.signaturePosition = new SignaturePosition(400, 650, 150, 50);
    }

    // Getters
    public String getDocId() { return docId; }
    public String getTitle() { return title; }
    public String getType() { return type; }
    public String getContent() { return content; }
    public boolean isRequiresSignature() { return requiresSignature; }
    public boolean isSignaturePlaced() { return signaturePlaced; }
    public SignaturePosition getSignaturePosition() { return signaturePosition; }
    public String getCreatorId() { return creatorId; }
    public Timestamp getCreatedAt() { return createdAt; }
    public String getPdfUrl() { return pdfUrl; }

    // Setters
    public void setDocId(String docId) { this.docId = docId; }
    public void setTitle(String title) { this.title = title; }
    public void setType(String type) { this.type = type; }
    public void setContent(String content) { this.content = content; }
    public void setRequiresSignature(boolean requiresSignature) { this.requiresSignature = requiresSignature; }
    public void setSignaturePlaced(boolean signaturePlaced) { this.signaturePlaced = signaturePlaced; }
    public void setSignaturePosition(SignaturePosition signaturePosition) { this.signaturePosition = signaturePosition; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }
}
