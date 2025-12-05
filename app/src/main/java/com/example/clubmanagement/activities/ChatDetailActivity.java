package com.example.clubmanagement.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clubmanagement.BaseActivity;
import com.example.clubmanagement.R;
import com.example.clubmanagement.adapters.ChatMessageAdapter;
import com.example.clubmanagement.models.ChatMessage;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatDetailActivity extends BaseActivity {

    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnBack, btnSend;
    private TextView tvTitle;

    private FirebaseManager firebaseManager;
    private ChatMessageAdapter adapter;
    private ListenerRegistration messageListener;

    private String chatRoomId;
    private String partnerUserId;
    private String partnerName;
    private String currentUserId;
    private String currentUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_chat_detail);

        firebaseManager = FirebaseManager.getInstance();
        currentUserId = firebaseManager.getCurrentUserId();

        // Intent에서 데이터 가져오기
        chatRoomId = getIntent().getStringExtra("chat_room_id");
        partnerUserId = getIntent().getStringExtra("partner_user_id");
        partnerName = getIntent().getStringExtra("partner_name");
        String partnerRole = getIntent().getStringExtra("partner_role");
        String clubName = getIntent().getStringExtra("club_name");
        boolean isGroupChat = getIntent().getBooleanExtra("is_group_chat", false);

        initViews();

        // 제목 설정
        if (isGroupChat) {
            tvTitle.setText(clubName != null ? clubName : "단체 채팅");
        } else {
            // 표시용 이름 처리
            String displayName = partnerName;
            if (displayName == null || displayName.isEmpty()) {
                displayName = "이름 없음";
            } else if (displayName.contains("@")) {
                displayName = displayName.substring(0, displayName.indexOf("@"));
            }
            tvTitle.setText(displayName);
        }

        setupRecyclerView();
        loadCurrentUserName();
        listenForMessages();
    }

    private void initViews() {
        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnBack = findViewById(R.id.btnBack);
        btnSend = findViewById(R.id.btnSend);
        tvTitle = findViewById(R.id.tvTitle);

        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void setupRecyclerView() {
        adapter = new ChatMessageAdapter(currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);
    }

    private void loadCurrentUserName() {
        firebaseManager.getCurrentUser(new FirebaseManager.UserCallback() {
            @Override
            public void onSuccess(com.example.clubmanagement.models.User user) {
                if (user != null) {
                    currentUserName = user.getName();
                    if (currentUserName == null || currentUserName.isEmpty()) {
                        currentUserName = user.getEmail();
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                currentUserName = "사용자";
            }
        });
    }

    private void listenForMessages() {
        if (chatRoomId == null) return;

        messageListener = firebaseManager.getDb()
                .collection("chatRooms")
                .document(chatRoomId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null || queryDocumentSnapshots == null) return;

                    List<ChatMessage> messages = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        ChatMessage message = doc.toObject(ChatMessage.class);
                        if (message != null) {
                            message.setMessageId(doc.getId());
                            messages.add(message);
                        }
                    }

                    adapter.setMessages(messages);
                    if (!messages.isEmpty()) {
                        rvMessages.scrollToPosition(messages.size() - 1);
                    }
                });
    }

    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(messageText)) return;

        etMessage.setText("");

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("senderId", currentUserId);
        messageData.put("senderName", currentUserName != null ? currentUserName : "사용자");
        messageData.put("message", messageText);
        messageData.put("timestamp", System.currentTimeMillis());
        messageData.put("isRead", false);

        // 메시지 저장
        firebaseManager.getDb()
                .collection("chatRooms")
                .document(chatRoomId)
                .collection("messages")
                .add(messageData)
                .addOnSuccessListener(documentReference -> {
                    // 채팅방 정보 업데이트 (마지막 메시지, 시간)
                    updateChatRoomLastMessage(messageText);
                });
    }

    private void updateChatRoomLastMessage(String lastMessage) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", lastMessage);
        updates.put("lastMessageTime", System.currentTimeMillis());

        firebaseManager.getDb()
                .collection("chatRooms")
                .document(chatRoomId)
                .update(updates);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) {
            messageListener.remove();
        }
    }
}
