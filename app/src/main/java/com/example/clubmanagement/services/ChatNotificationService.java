package com.example.clubmanagement.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.clubmanagement.R;
import com.example.clubmanagement.activities.ChatActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Firebase Cloud Messaging 서비스
 * 채팅 메시지 알림을 처리합니다.
 */
public class ChatNotificationService extends FirebaseMessagingService {

    private static final String TAG = "ChatNotificationService";
    private static final String CHANNEL_ID = "chat_notifications";
    private static final String CHANNEL_NAME = "채팅 알림";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // 데이터 메시지 처리
        if (remoteMessage.getData().size() > 0) {
            String title = remoteMessage.getData().get("title");
            String body = remoteMessage.getData().get("body");
            String chatRoomId = remoteMessage.getData().get("chatRoomId");

            if (title != null && body != null) {
                sendNotification(title, body, chatRoomId);
            }
        }

        // 알림 메시지 처리
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            sendNotification(title, body, null);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // 토큰을 서버에 저장 (필요 시)
        android.util.Log.d(TAG, "FCM Token: " + token);
        // TODO: FirebaseManager를 통해 사용자 문서에 토큰 저장
    }

    private void sendNotification(String title, String body, String chatRoomId) {
        Intent intent;
        if (chatRoomId != null) {
            intent = new Intent(this, ChatActivity.class);
            intent.putExtra("chat_room_id", chatRoomId);
        } else {
            intent = new Intent(this, ChatActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_chat)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Android O 이상에서는 채널 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("채팅 메시지 알림");
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0, notificationBuilder.build());
    }
}
