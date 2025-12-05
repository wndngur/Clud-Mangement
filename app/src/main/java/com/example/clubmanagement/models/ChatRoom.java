package com.example.clubmanagement.models;

public class ChatRoom {
    private String chatRoomId;
    private String partnerUserId;
    private String partnerName;
    private String partnerRole;  // 상대방 직급
    private String clubId;
    private String clubName;
    private String lastMessage;
    private long lastMessageTime;
    private int unreadCount;
    private boolean notificationEnabled;  // 알림 활성화 여부
    private String leftUserId;  // 나간 사용자 ID (null이면 아무도 안 나감)
    private boolean isGroupChat;  // 단체 채팅방 여부
    private int memberCount;  // 단체 채팅방 멤버 수

    public ChatRoom() {
        this.notificationEnabled = true;
        this.isGroupChat = false;
    }

    public ChatRoom(String chatRoomId, String partnerUserId, String partnerName, String partnerRole, String clubId, String clubName) {
        this.chatRoomId = chatRoomId;
        this.partnerUserId = partnerUserId;
        this.partnerName = partnerName;
        this.partnerRole = partnerRole;
        this.clubId = clubId;
        this.clubName = clubName;
        this.lastMessage = "";
        this.lastMessageTime = System.currentTimeMillis();
        this.unreadCount = 0;
        this.notificationEnabled = true;
        this.leftUserId = null;
        this.isGroupChat = false;
    }

    // 단체 채팅방 생성자
    public ChatRoom(String chatRoomId, String clubId, String clubName, int memberCount) {
        this.chatRoomId = chatRoomId;
        this.clubId = clubId;
        this.clubName = clubName;
        this.lastMessage = "";
        this.lastMessageTime = System.currentTimeMillis();
        this.unreadCount = 0;
        this.notificationEnabled = true;
        this.isGroupChat = true;
        this.memberCount = memberCount;
    }

    public String getChatRoomId() {
        return chatRoomId;
    }

    public void setChatRoomId(String chatRoomId) {
        this.chatRoomId = chatRoomId;
    }

    public String getPartnerUserId() {
        return partnerUserId;
    }

    public void setPartnerUserId(String partnerUserId) {
        this.partnerUserId = partnerUserId;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public void setPartnerName(String partnerName) {
        this.partnerName = partnerName;
    }

    public String getClubId() {
        return clubId;
    }

    public void setClubId(String clubId) {
        this.clubId = clubId;
    }

    public String getClubName() {
        return clubName;
    }

    public void setClubName(String clubName) {
        this.clubName = clubName;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(long lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public String getPartnerRole() {
        return partnerRole;
    }

    public void setPartnerRole(String partnerRole) {
        this.partnerRole = partnerRole;
    }

    public boolean isNotificationEnabled() {
        return notificationEnabled;
    }

    public void setNotificationEnabled(boolean notificationEnabled) {
        this.notificationEnabled = notificationEnabled;
    }

    public String getLeftUserId() {
        return leftUserId;
    }

    public void setLeftUserId(String leftUserId) {
        this.leftUserId = leftUserId;
    }

    public boolean isGroupChat() {
        return isGroupChat;
    }

    public void setGroupChat(boolean groupChat) {
        isGroupChat = groupChat;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    // 표시용 이름 (이메일이면 @ 앞부분만)
    public String getDisplayName() {
        if (partnerName == null || partnerName.isEmpty()) {
            return "이름 없음";
        }
        if (partnerName.contains("@")) {
            return partnerName.substring(0, partnerName.indexOf("@"));
        }
        return partnerName;
    }

    // 채팅방 제목 표시 (동아리명 직급 이름님과의 채팅 또는 단체 채팅방)
    public String getChatRoomTitle() {
        // 단체 채팅방인 경우
        if (isGroupChat) {
            if (clubName != null && !clubName.isEmpty()) {
                return clubName + " 단체 채팅방";
            }
            return "단체 채팅방";
        }

        // 개인 채팅방인 경우
        StringBuilder title = new StringBuilder();

        // 동아리명
        if (clubName != null && !clubName.isEmpty()) {
            title.append(clubName).append(" ");
        }

        // 직급
        if (partnerRole != null && !partnerRole.isEmpty() && !"회원".equals(partnerRole)) {
            title.append(partnerRole).append(" ");
        }

        // 이름
        title.append(getDisplayName()).append("님과의 채팅");

        return title.toString();
    }
}
