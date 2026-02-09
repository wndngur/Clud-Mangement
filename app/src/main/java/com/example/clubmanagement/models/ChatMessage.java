package com.example.clubmanagement.models;

import java.util.ArrayList;
import java.util.List;

public class ChatMessage {
    private String messageId;
    private String senderId;
    private String senderName;
    private String message;
    private long timestamp;
    private boolean isRead;
    private List<String> readBy; // 읽은 사용자 ID 목록

    public ChatMessage() {
        this.readBy = new ArrayList<>();
    }

    public ChatMessage(String senderId, String senderName, String message) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
        this.isRead = false;
        this.readBy = new ArrayList<>();
        // 보낸 사람은 자동으로 읽음 처리
        this.readBy.add(senderId);
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public List<String> getReadBy() {
        if (readBy == null) {
            readBy = new ArrayList<>();
        }
        return readBy;
    }

    public void setReadBy(List<String> readBy) {
        this.readBy = readBy;
    }

    /**
     * 읽은 사람 수 반환
     */
    public int getReadCount() {
        return readBy != null ? readBy.size() : 0;
    }

    /**
     * 특정 사용자가 읽었는지 확인
     */
    public boolean isReadByUser(String userId) {
        return readBy != null && readBy.contains(userId);
    }

    /**
     * 읽은 사람 추가
     */
    public void addReadBy(String userId) {
        if (readBy == null) {
            readBy = new ArrayList<>();
        }
        if (!readBy.contains(userId)) {
            readBy.add(userId);
        }
    }
}
