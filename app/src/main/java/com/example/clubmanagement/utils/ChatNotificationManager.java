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
    private int unreadCount = 0;
    private OnUnreadCountChangeListener unreadCountListener;
    private String currentOpenChatRoomId = null; // 현재 열려있는 채팅방

    public interface OnUnreadCountChangeListener {
        void onUnreadCountChanged(int count);
    }

    private ChatNotificationManager(Context context) {
        this.context = context.getApplicationContext();
        this.firebaseManager = FirebaseManager.getInstance();
        loadUnreadCount();
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

        android.util.Log.d(TAG, "Starting chat notification listener for user: " + currentUserId);

        // 내가 참여한 모든 채팅방 감시
        firebaseManager.getDb()
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
                });
    }

    /**
     * 특정 채팅방의 새 메시지 감시
     */
    private void listenToChatRoom(String chatRoomId, String currentUserId) {
        ListenerRegistration listener = firebaseManager.getDb()
                .collection("chatRooms")
                .document(chatRoomId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null || snapshots.isEmpty()) return;

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            String senderId = dc.getDocument().getString("senderId");
                            String senderName = dc.getDocument().getString("senderName");
                            String message = dc.getDocument().getString("message");

                            // timestamp 처리 (Long 또는 Timestamp 타입 모두 처리)
                            Object timestampObj = dc.getDocument().get("timestamp");
                            Long timestamp = 0L;
                            if (timestampObj instanceof Long) {
                                timestamp = (Long) timestampObj;
                            } else if (timestampObj instanceof Number) {
                                timestamp = ((Number) timestampObj).longValue();
                            } else if (timestampObj instanceof com.google.firebase.Timestamp) {
                                timestamp = ((com.google.firebase.Timestamp) timestampObj).toDate().getTime();
                            }

                            // 내가 보낸 메시지가 아니고, 현재 열려있는 채팅방이 아닌 경우에만 알림
                            if (senderId != null && !senderId.equals(currentUserId)) {
                                if (!chatRoomId.equals(currentOpenChatRoomId)) {
                                    // 새 메시지 알림
                                    showNotification(senderName, message, chatRoomId);
                                    incrementUnreadCount();
                                }
                            }
                        }
                    }
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
     * 로컬 알림 표시
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
     * 모든 리스너 해제 (로그아웃 시)
     */
    public void stopListening() {
        for (ListenerRegistration listener : chatListeners.values()) {
            listener.remove();
        }
        chatListeners.clear();
    }
}
