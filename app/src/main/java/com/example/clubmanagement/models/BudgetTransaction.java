package com.example.clubmanagement.models;

import com.google.firebase.Timestamp;

/**
 * 공금 거래 내역 모델
 */
public class BudgetTransaction {
    private String id;
    private String clubId;
    private String type;           // "EXPENSE" (지출) or "INCOME" (수입) or "ADJUSTMENT" (조정)
    private long amount;           // 금액
    private String description;    // 설명 (예: "회식비", "장비 구입")
    private String receiptImageUrl; // 영수증 이미지 URL (optional)
    private String createdBy;      // 작성자 ID
    private String createdByName;  // 작성자 이름
    private Timestamp createdAt;   // 생성 시간
    private long balanceAfter;     // 거래 후 잔액

    // Transaction types
    public static final String TYPE_EXPENSE = "EXPENSE";
    public static final String TYPE_INCOME = "INCOME";
    public static final String TYPE_ADJUSTMENT = "ADJUSTMENT";

    // Firebase requires no-argument constructor
    public BudgetTransaction() {
    }

    public BudgetTransaction(String clubId, String type, long amount, String description) {
        this.clubId = clubId;
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.createdAt = Timestamp.now();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getClubId() {
        return clubId;
    }

    public String getType() {
        return type;
    }

    public long getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public String getReceiptImageUrl() {
        return receiptImageUrl;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public long getBalanceAfter() {
        return balanceAfter;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setClubId(String clubId) {
        this.clubId = clubId;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setReceiptImageUrl(String receiptImageUrl) {
        this.receiptImageUrl = receiptImageUrl;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public void setBalanceAfter(long balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    // Helper methods
    public boolean isExpense() {
        return TYPE_EXPENSE.equals(type);
    }

    public boolean isIncome() {
        return TYPE_INCOME.equals(type);
    }

    public String getFormattedAmount() {
        java.text.NumberFormat numberFormat = java.text.NumberFormat.getNumberInstance(java.util.Locale.KOREA);
        String prefix = isExpense() ? "-" : "+";
        return prefix + numberFormat.format(amount) + "원";
    }

    public String getFormattedDate() {
        if (createdAt == null) return "";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy.MM.dd HH:mm", java.util.Locale.KOREA);
        return sdf.format(createdAt.toDate());
    }

    public String getTypeDisplayName() {
        switch (type) {
            case TYPE_EXPENSE:
                return "지출";
            case TYPE_INCOME:
                return "수입";
            case TYPE_ADJUSTMENT:
                return "조정";
            default:
                return type;
        }
    }
}
