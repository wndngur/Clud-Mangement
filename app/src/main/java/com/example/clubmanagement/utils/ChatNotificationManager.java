package com.example.clubmanagement.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.example.clubmanagement.R;
import com.example.clubmanagement.activities.ChatDetailActivity;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

/**
 * 채팅 알림 관리자
 * - 새 메시지 실시간 감지
 * - 로컬 푸시 알림 발송
 * - 읽지 않은 메시지 카운트 관리
 */
public class ChatNotificationManager {

    private static final String TAG = "ChatNotificationManager";
    private static final String CHANNEL_ID = "chat_messages";
    private static final String CHANNEL_NAME = "채팅 메시지";
    private static final String PREFS_NAME = "chat_notification_prefs";
    private static final String KEY_UNREAD_COUNT = "unread_count";

    private static ChatNotificationManager instance;
    private Context context;
    private FirebaseManager firebaseManager;
    private Map<String, ListenerRegistration> chatListeners = new HashMap<>();
    private ListenerRegistration chatRoomListListener = null;
    private int unreadCount = 0;
    private OnUnreadCountChangeListener unreadCountListener;
    private String currentOpenChatRoomId = null; // 현재 열려있는 채팅방
    private Map<String, Long> lastReadTimestamps = new HashMap<>(); // 각 채팅방의 마지막 읽은 시간
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isListening = false; // 리스너가 이미 시작되었는지 여부
    private Map<String, Boolean> initialLoadComplete = new HashMap<>(); // 초기 로드 완료 여부
    private long listenerStartTime = 0; // 리스너 시작 시간

    public interface OnUnreadCountChangeListener {
        void onUnreadCountChanged(int count);
    }

    private ChatNotificationManager(Context context) {
        this.context = context.getApplicationContext();
        this.firebaseManager = FirebaseManager.getInstance();
        loadUnreadCount();
        loadLastReadTimestamps();
        createNotificationChannel();
    }

    public static synchronized ChatNotificationManager getInstance(Context context) {
        if (instance == null) {
            instance = new ChatNotificationManager(context);
        }
        return instance;
    }

    /**
     * 채팅 알림 리스너 시작 (로그인 시 호출)
     */
    public void startListening() {
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) return;

        // 이미 리스닝 중이면 중복 시작 방지
        if (isListening) {
            android.util.Log.d(TAG, "Already listening, skipping duplicate start");
            return;
        }

        android.util.Log.d(TAG, "Starting chat notification listener for user: " + currentUserId);
        isListening = true;
        listenerStartTime = System.currentTimeMillis();
        initialLoadComplete.clear();

        // 기존 리스너 정리
        if (chatRoomListListener != null) {
            chatRoomListListener.remove();
        }
        chatListeners.clear();

        // 로그인 시 읽지 않은 메시지 알림 표시
        checkUnreadMessagesOnLogin(currentUserId);

        // 내가 참여한 모든 채팅방 감시
        chatRoomListListener = firebaseManager.getDb()
                .collection("chatRooms")
                .whereArrayContains("participants", currentUserId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        String chatRoomId = dc.getDocument().getId();

                        if (dc.getType() == DocumentChange.Type.ADDED ||
                            dc.getType() == DocumentChange.Type.MODIFIED) {
                            // 각 채팅방의 메시지 리스너 등록
                            if (!chatListeners.containsKey(chatRoomId)) {
                                listenToChatRoom(chatRoomId, currentUserId);
                            }
                        }
                    }

                    // 초기 로드 시 읽지 않은 메시지 수 계산
                    calculateTotalUnreadCount(currentUserId);
                });
    }

    /**
     * 로그인 시 읽지 않은 메시지 확인 및 알림 표시
     */
    private void checkUnreadMessagesOnLogin(String currentUserId) {
        firebaseManager.getDb()
                .collection("chatRooms")
                .whereArrayContains("participants", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int totalRooms = querySnapshot.size();
                    if (totalRooms == 0) return;

                    final int[] processedRooms = {0};
                    final int[] totalUnreadCount = {0};
                    final String[] lastSenderName = {null};
                    final String[] lastChatRoomId = {null};

                    for (com.google.firebase.firestore.DocumentSnapshot roomDoc : querySnapshot.getDocuments()) {
                        String chatRoomId = roomDoc.getId();
                        Long lastReadTime = lastReadTimestamps.get(chatRoomId);
                        if (lastReadTime == null) {
                            lastReadTime = 0L;
                        }

                        final long finalLastReadTime = lastReadTime;
                        android.util.Log.d(TAG, "Checking room: " + chatRoomId + ", lastReadTime: " + finalLastReadTime);

                        // 각 채팅방의 모든 메시지를 가져와서 직접 비교 (timestamp 타입 문제 해결)
                        firebaseManager.getDb()
                                .collection("chatRooms")
                                .document(chatRoomId)
                                .collection("messages")
                                .get()
                                .addOnSuccessListener(messagesSnapshot -> {
                                    int unreadInRoom = 0;
                                    String senderName = null;

                                    for (com.google.firebase.firestore.DocumentSnapshot msgDoc : messagesSnapshot.getDocuments()) {
                                        String senderId = msgDoc.getString("senderId");

                                        // 내가 보낸 메시지가 아닌 경우만 확인
                                        if (senderId != null && !senderId.equals(currentUserId)) {
                                            // timestamp 추출
                                            long msgTimestamp = getTimestampFromDocument(msgDoc);

                                            // 마지막으로 읽은 시간 이후의 메시지만 카운트
                                            if (msgTimestamp > finalLastReadTime) {
                                                unreadInRoom++;
                                                if (senderName == null) {
                                                    senderName = msgDoc.getString("senderName");
                                                }
                                                android.util.Log.d(TAG, "Unread message: " + msgDoc.getString("message") +
                                                    ", timestamp: " + msgTimestamp + ", lastRead: " + finalLastReadTime);
                                            }
                                        }
                                    }

                                    android.util.Log.d(TAG, "Room " + chatRoomId + " has " + unreadInRoom + " unread messages");

                                    if (unreadInRoom > 0) {
                                        totalUnreadCount[0] += unreadInRoom;
                                        if (lastSenderName[0] == null) {
                                            lastSenderName[0] = senderName;
                                            lastChatRoomId[0] = chatRoomId;
                                        }
                                    }

                                    processedRooms[0]++;

                                    // 모든 채팅방 처리 완료 후 알림 표시
                                    if (processedRooms[0] >= totalRooms && totalUnreadCount[0] > 0) {
                                        android.util.Log.d(TAG, "Total unread on login: " + totalUnreadCount[0]);
                                        showUnreadSummaryNotification(lastSenderName[0], totalUnreadCount[0], lastChatRoomId[0]);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    processedRooms[0]++;
                                    if (processedRooms[0] >= totalRooms && totalUnreadCount[0] > 0) {
                                        showUnreadSummaryNotification(lastSenderName[0], totalUnreadCount[0], lastChatRoomId[0]);
                                    }
                                });
                    }
                });
    }

    /**
     * Document에서 timestamp 추출 (다양한 타입 처리)
     */
    private long getTimestampFromDocument(com.google.firebase.firestore.DocumentSnapshot doc) {
        Object timestampObj = doc.get("timestamp");
        if (timestampObj instanceof Long) {
            return (Long) timestampObj;
        } else if (timestampObj instanceof Number) {
            return ((Number) timestampObj).longValue();
        } else if (timestampObj instanceof com.google.firebase.Timestamp) {
            return ((com.google.firebase.Timestamp) timestampObj).toDate().getTime();
        }
        return 0L;
    }

    /**
     * 읽지 않은 메시지 요약 알림 표시
     */
    private void showUnreadSummaryNotification(String senderName, int totalCount, String chatRoomId) {
        String displayName = senderName;
        if (displayName != null && displayName.contains("@")) {
            displayName = displayName.substring(0, displayName.indexOf("@"));
        }
        if (displayName == null || displayName.isEmpty()) {
            displayName = "알 수 없음";
        }

        String title = "새 채팅 메시지";
        String message;
        if (totalCount == 1) {
            message = displayName + "님이 메시지를 보냈습니다";
        } else {
            message = displayName + "님 외 " + (totalCount - 1) + "개의 채팅이 왔습니다";
        }

        // 알림 표시
        Intent intent = new Intent(context, ChatDetailActivity.class);
        intent.putExtra("chat_room_id", chatRoomId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, "unread_summary".hashCode(), intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_chat)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify("unread_summary".hashCode(), notificationBuilder.build());

        // 토스트 메시지 표시
        final String toastMessage = message;
        mainHandler.post(() -> {
            Toast.makeText(context, "💬 " + toastMessage, Toast.LENGTH_LONG).show();
        });
    }

    /**
     * 특정 채팅방의 새 메시지 감시
     */
    private void listenToChatRoom(String chatRoomId, String currentUserId) {
        // 초기 로드 완료 플래그 설정
        initialLoadComplete.put(chatRoomId, false);

        ListenerRegistration listener = firebaseManager.getDb()
                .collection("chatRooms")
                .document(chatRoomId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        android.util.Log.e(TAG, "Error listening to chat room " + chatRoomId, error);
                        return;
                    }
                    if (snapshots == null || snapshots.isEmpty()) {
                        // 초기 로드 완료 표시 (메시지가 없어도)
                        initialLoadComplete.put(chatRoomId, true);
                        return;
                    }

                    // 초기 로드인지 확인
                    boolean isInitialLoad = !Boolean.TRUE.equals(initialLoadComplete.get(chatRoomId));

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            String senderId = dc.getDocument().getString("senderId");
                            String senderName = dc.getDocument().getString("senderName");
                            String message = dc.getDocument().getString("message");

                            // timestamp 처리 (Long 또는 Timestamp 타입 모두 처리)
                            // serverTimestamp()는 처음에 null로 올 수 있으므로 현재 시간 사용
                            Object timestampObj = dc.getDocument().get("timestamp");
                            Long timestamp = 0L;
                            if (timestampObj instanceof Long) {
                                timestamp = (Long) timestampObj;
                            } else if (timestampObj instanceof Number) {
                                timestamp = ((Number) timestampObj).longValue();
                            } else if (timestampObj instanceof com.google.firebase.Timestamp) {
                                timestamp = ((com.google.firebase.Timestamp) timestampObj).toDate().getTime();
                            } else if (timestampObj == null) {
                                // serverTimestamp가 아직 설정되지 않은 경우 - 방금 전송된 메시지
                                timestamp = System.currentTimeMillis();
                            }

                            android.util.Log.d(TAG, "Message detected - chatRoom: " + chatRoomId +
                                    ", sender: " + senderId + ", currentUser: " + currentUserId +
                                    ", isInitialLoad: " + isInitialLoad +
                                    ", currentOpenRoom: " + currentOpenChatRoomId +
                                    ", message: " + message);

                            // 초기 로드가 아닌 경우에만 알림 (새 메시지)
                            // 내가 보낸 메시지가 아니고, 현재 열려있는 채팅방이 아닌 경우
                            if (!isInitialLoad) {
                                android.util.Log.d(TAG, "Not initial load, checking conditions...");
                                boolean isMyMessage = senderId != null && senderId.equals(currentUserId);
                                boolean isChatRoomOpen = chatRoomId.equals(currentOpenChatRoomId);
                                android.util.Log.d(TAG, "isMyMessage: " + isMyMessage + ", isChatRoomOpen: " + isChatRoomOpen);

                                if (!isMyMessage) {
                                    if (!isChatRoomOpen) {
                                        android.util.Log.d(TAG, "Showing notification for new message from: " + senderName);
                                        // 새 메시지 알림
                                        showNotification(senderName, message, chatRoomId);
                                        incrementUnreadCount();
                                    } else {
                                        android.util.Log.d(TAG, "Skipping notification - chat room is open");
                                    }
                                } else {
                                    android.util.Log.d(TAG, "Skipping notification - my own message");
                                }
                            } else {
                                android.util.Log.d(TAG, "Skipping - initial load");
                            }
                        }
                    }

                    // 초기 로드 완료 표시
                    initialLoadComplete.put(chatRoomId, true);
                });

        chatListeners.put(chatRoomId, listener);
    }

    /**
     * 현재 열려있는 채팅방 설정 (ChatDetailActivity 진입 시)
     */
    public void setCurrentOpenChatRoom(String chatRoomId) {
        this.currentOpenChatRoomId = chatRoomId;
    }

    /**
     * 현재 열려있는 채팅방 해제 (ChatDetailActivity 종료 시)
     */
    public void clearCurrentOpenChatRoom() {
        this.currentOpenChatRoomId = null;
    }

    /**
     * 특정 채팅방을 읽음으로 표시 (해당 채팅방의 마지막 읽은 시간 업데이트)
     */
    public void markChatRoomAsRead(String chatRoomId) {
        if (chatRoomId == null) return;

        long currentTime = System.currentTimeMillis();
        lastReadTimestamps.put(chatRoomId, currentTime);
        saveLastReadTimestamps();

        // 전체 읽지 않은 메시지 수 재계산
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId != null) {
            calculateTotalUnreadCount(currentUserId);
        }
    }

    /**
     * 전체 읽지 않은 메시지 수 계산
     */
    private void calculateTotalUnreadCount(String currentUserId) {
        firebaseManager.getDb()
                .collection("chatRooms")
                .whereArrayContains("participants", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    final int[] totalUnread = {0};
                    final int[] processedRooms = {0};
                    int totalRooms = querySnapshot.size();

                    if (totalRooms == 0) {
                        unreadCount = 0;
                        saveUnreadCount();
                        notifyUnreadCountChanged();
                        return;
                    }

                    for (com.google.firebase.firestore.DocumentSnapshot roomDoc : querySnapshot.getDocuments()) {
                        String chatRoomId = roomDoc.getId();
                        Long lastReadTime = lastReadTimestamps.get(chatRoomId);
                        if (lastReadTime == null) {
                            lastReadTime = 0L;
                        }

                        final long finalLastReadTime = lastReadTime;

                        // 각 채팅방의 모든 메시지를 가져와서 직접 비교 (timestamp 타입 문제 해결)
                        firebaseManager.getDb()
                                .collection("chatRooms")
                                .document(chatRoomId)
                                .collection("messages")
                                .get()
                                .addOnSuccessListener(messagesSnapshot -> {
                                    for (com.google.firebase.firestore.DocumentSnapshot msgDoc : messagesSnapshot.getDocuments()) {
                                        String senderId = msgDoc.getString("senderId");
                                        // 내가 보낸 메시지가 아닌 경우만 확인
                                        if (senderId != null && !senderId.equals(currentUserId)) {
                                            long msgTimestamp = getTimestampFromDocument(msgDoc);
                                            // 마지막으로 읽은 시간 이후의 메시지만 카운트
                                            if (msgTimestamp > finalLastReadTime) {
                                                totalUnread[0]++;
                                            }
                                        }
                                    }

                                    processedRooms[0]++;
                                    if (processedRooms[0] >= totalRooms) {
                                        // 모든 채팅방 처리 완료
                                        unreadCount = totalUnread[0];
                                        saveUnreadCount();
                                        notifyUnreadCountChanged();
                                        android.util.Log.d(TAG, "Total unread count calculated: " + unreadCount);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    processedRooms[0]++;
                                    if (processedRooms[0] >= totalRooms) {
                                        unreadCount = totalUnread[0];
                                        saveUnreadCount();
                                        notifyUnreadCountChanged();
                                    }
                                });
                    }
                });
    }

    /**
     * 로컬 알림 표시 및 토스트 메시지
     */
    private void showNotification(String senderName, String message, String chatRoomId) {
        Intent intent = new Intent(context, ChatDetailActivity.class);
        intent.putExtra("chat_room_id", chatRoomId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, chatRoomId.hashCode(), intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        String displayName = senderName;
        if (displayName != null && displayName.contains("@")) {
            displayName = displayName.substring(0, displayName.indexOf("@"));
        }
        if (displayName == null || displayName.isEmpty()) {
            displayName = "알 수 없음";
        }

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_chat)
                        .setContentTitle(displayName)
                        .setContentText(message)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(chatRoomId.hashCode(), notificationBuilder.build());

        // 토스트 메시지 표시 (메인 스레드에서 실행)
        final String finalDisplayName = displayName;
        mainHandler.post(() -> {
            String toastMessage = finalDisplayName + ": " + message;
            // 메시지가 너무 길면 자르기
            if (toastMessage.length() > 50) {
                toastMessage = toastMessage.substring(0, 47) + "...";
            }
            Toast.makeText(context, "💬 " + toastMessage, Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * 알림 채널 생성 (Android O 이상)
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("새 채팅 메시지 알림");

            NotificationManager notificationManager =
                    context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * 읽지 않은 메시지 카운트 증가
     */
    private void incrementUnreadCount() {
        unreadCount++;
        saveUnreadCount();
        notifyUnreadCountChanged();
    }

    /**
     * 읽지 않은 메시지 카운트 초기화
     */
    public void resetUnreadCount() {
        unreadCount = 0;
        saveUnreadCount();
        notifyUnreadCountChanged();
    }

    /**
     * 읽지 않은 메시지 카운트 감소
     */
    public void decrementUnreadCount(int count) {
        unreadCount = Math.max(0, unreadCount - count);
        saveUnreadCount();
        notifyUnreadCountChanged();
    }

    /**
     * 현재 읽지 않은 메시지 수 반환
     */
    public int getUnreadCount() {
        return unreadCount;
    }

    /**
     * 읽지 않은 메시지 변경 리스너 설정
     */
    public void setOnUnreadCountChangeListener(OnUnreadCountChangeListener listener) {
        this.unreadCountListener = listener;
        // 현재 값 즉시 전달
        if (listener != null) {
            listener.onUnreadCountChanged(unreadCount);
        }
    }

    private void notifyUnreadCountChanged() {
        if (unreadCountListener != null) {
            unreadCountListener.onUnreadCountChanged(unreadCount);
        }
    }

    /**
     * SharedPreferences에 저장
     */
    private void saveUnreadCount() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_UNREAD_COUNT, unreadCount).apply();
    }

    /**
     * SharedPreferences에서 로드
     */
    private void loadUnreadCount() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        unreadCount = prefs.getInt(KEY_UNREAD_COUNT, 0);
    }

    /**
     * 마지막 읽은 시간 저장
     */
    private void saveLastReadTimestamps() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Map을 JSON 문자열로 변환하여 저장
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Long> entry : lastReadTimestamps.entrySet()) {
            if (sb.length() > 0) sb.append(";");
            sb.append(entry.getKey()).append(":").append(entry.getValue());
        }
        editor.putString("last_read_timestamps", sb.toString());
        editor.apply();
    }

    /**
     * 마지막 읽은 시간 로드
     */
    private void loadLastReadTimestamps() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String data = prefs.getString("last_read_timestamps", "");

        lastReadTimestamps.clear();
        if (!data.isEmpty()) {
            String[] entries = data.split(";");
            for (String entry : entries) {
                String[] parts = entry.split(":");
                if (parts.length == 2) {
                    try {
                        lastReadTimestamps.put(parts[0], Long.parseLong(parts[1]));
                    } catch (NumberFormatException e) {
                        // 무시
                    }
                }
            }
        }
    }

    /**
     * 읽지 않은 메시지 수 새로고침 (외부에서 호출 가능)
     */
    public void refreshUnreadCount() {
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId != null) {
            calculateTotalUnreadCount(currentUserId);
        }
    }

    /**
     * 특정 채팅방의 마지막 읽은 시간 반환
     */
    public long getLastReadTimestamp(String chatRoomId) {
        Long timestamp = lastReadTimestamps.get(chatRoomId);
        return timestamp != null ? timestamp : 0L;
    }

    /**
     * 모든 리스너 해제 (로그아웃 시)
     */
    public void stopListening() {
        android.util.Log.d(TAG, "Stopping chat notification listener");

        if (chatRoomListListener != null) {
            chatRoomListListener.remove();
            chatRoomListListener = null;
        }
        for (ListenerRegistration listener : chatListeners.values()) {
            listener.remove();
        }
        chatListeners.clear();
        initialLoadComplete.clear();
        isListening = false;

        // 모든 알림 취소
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

        android.util.Log.d(TAG, "Chat notification listener stopped");
    }

    /**
     * 리스너가 활성화되어 있는지 확인
     */
    public boolean isListening() {
        return isListening;
    }
}
