package com.example.clubmanagement.models;

import com.google.firebase.Timestamp;

public class SignatureData {
    private String signaturePadUrl;      // 화면 서명 저장 URL
    private String signatureImageUrl;    // 사진 업로드 후 정제된 서명 이미지 URL
    private Timestamp lastUpdated;
    private String origin;               // "pad" | "image"
    private String userId;

    public SignatureData() {
        // Required empty constructor for Firestore
    }

    public SignatureData(String userId, String signaturePadUrl, String signatureImageUrl,
                        Timestamp lastUpdated, String origin) {
        this.userId = userId;
        this.signaturePadUrl = signaturePadUrl;
        this.signatureImageUrl = signatureImageUrl;
        this.lastUpdated = lastUpdated;
        this.origin = origin;
    }

    // Getters
    public String getSignaturePadUrl() {
        return signaturePadUrl;
    }

    public String getSignatureImageUrl() {
        return signatureImageUrl;
    }

    public Timestamp getLastUpdated() {
        return lastUpdated;
    }

    public String getOrigin() {
        return origin;
    }

    public String getUserId() {
        return userId;
    }

    // Setters
    public void setSignaturePadUrl(String signaturePadUrl) {
        this.signaturePadUrl = signaturePadUrl;
    }

    public void setSignatureImageUrl(String signatureImageUrl) {
        this.signatureImageUrl = signatureImageUrl;
    }

    public void setLastUpdated(Timestamp lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getActiveSignatureUrl() {
        if ("pad".equals(origin) && signaturePadUrl != null) {
            return signaturePadUrl;
        } else if ("image".equals(origin) && signatureImageUrl != null) {
            return signatureImageUrl;
        }
        return signaturePadUrl != null ? signaturePadUrl : signatureImageUrl;
    }
}
