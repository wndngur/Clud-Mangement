package com.example.clubmanagement.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clubmanagement.BaseActivity;
import com.example.clubmanagement.R;
import com.example.clubmanagement.SettingsActivity;
import com.example.clubmanagement.adapters.ChatMessageAdapter;
import com.example.clubmanagement.models.ChatMessage;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChatDetailActivity extends BaseActivity {

    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnBack, btnSend, btnSettings;
    private TextView tvTitle;
    private LinearLayout inputLayout;
    private LinearLayout layoutMutedMessage;

    private FirebaseManager firebaseManager;
    private ChatMessageAdapter adapter;
    private ListenerRegistration messageListener;
    private ListenerRegistration chatRoomListener;

    private String chatRoomId;
    private String partnerUserId;
    private String partnerName;
    private String currentUserId;
    private String currentUserName;
    private boolean isGroupChat;
    private boolean notificationEnabled = true;
    private boolean isClubAdmin = false; // 동아리 관리자 여부
    private boolean partnerIsSuperAdmin = false; // 상대방이 슈퍼관리자인지
    private boolean partnerHasLeft = false; // 상대방이 나갔는지
    private boolean allMembersMuted = false; // 모든 멤버 채팅 정지 상태
    private boolean isMuted = false; // 현재 사용자 채팅 정지 상태

    // 멤버 정보 저장
    private List<Map<String, String>> memberList = new ArrayList<>();
    // 정지된 멤버 ID 목록
    private Set<String> mutedMembers = new HashSet<>();

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
        isGroupChat = getIntent().getBooleanExtra("is_group_chat", false);

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
        loadChatRoomSettings();
        loadChatMembers();

        // 단체 채팅방인 경우 동아리 관리자 여부 확인
        if (isGroupChat) {
            checkClubAdminStatus();
            // 정지 상태 실시간 감지
            listenForMuteStatus();
        } else {
            // 개인 채팅방인 경우 상대방이 슈퍼관리자인지, 나갔는지 확인
            checkPartnerSuperAdminStatus();
        }

        // 현재 열린 채팅방 설정 (알림 방지)
        getChatNotificationManager().setCurrentOpenChatRoom(chatRoomId);

        // 이 채팅방을 읽음으로 표시
        getChatNotificationManager().markChatRoomAsRead(chatRoomId);
    }

    private void initViews() {
        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnBack = findViewById(R.id.btnBack);
        btnSend = findViewById(R.id.btnSend);
        btnSettings = findViewById(R.id.btnSettings);
        tvTitle = findViewById(R.id.tvTitle);
        inputLayout = findViewById(R.id.inputLayout);
        layoutMutedMessage = findViewById(R.id.layoutMutedMessage);

        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendMessage());
        btnSettings.setOnClickListener(v -> showSettingsDialog());
    }

    private void setupRecyclerView() {
        adapter = new ChatMessageAdapter(currentUserId);
        adapter.setOnMessageLongClickListener(this::showMessageOptionsDialog);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);
    }

    private void loadChatRoomSettings() {
        if (chatRoomId == null) return;

        firebaseManager.getDb()
                .collection("chatRooms")
                .document(chatRoomId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Boolean enabled = doc.getBoolean("notificationEnabled");
                        notificationEnabled = enabled == null || enabled;
                    }
                });
    }

    private void loadChatMembers() {
        if (chatRoomId == null) return;

        firebaseManager.getDb()
                .collection("chatRooms")
                .document(chatRoomId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        memberList.clear();

                        if (isGroupChat) {
                            // 단체 채팅방: participants 배열에서 멤버 로드
                            List<String> participants = (List<String>) doc.get("participants");
                            if (participants != null) {
                                for (String participantId : participants) {
                                    loadMemberInfo(participantId);
                                }
                            }
                        } else {
                            // 개인 채팅방: user1, user2에서 멤버 로드
                            Map<String, Object> user1 = (Map<String, Object>) doc.get("user1");
                            Map<String, Object> user2 = (Map<String, Object>) doc.get("user2");

                            if (user1 != null) {
                                Map<String, String> member = new HashMap<>();
                                member.put("userId", (String) user1.get("userId"));
                                member.put("name", (String) user1.get("name"));
                                memberList.add(member);
                            }
                            if (user2 != null) {
                                Map<String, String> member = new HashMap<>();
                                member.put("userId", (String) user2.get("userId"));
                                member.put("name", (String) user2.get("name"));
                                memberList.add(member);
                            }
                        }
                    }
                });
    }

    private void loadMemberInfo(String userId) {
        firebaseManager.getDb()
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Map<String, String> member = new HashMap<>();
                        member.put("userId", userId);
                        String name = doc.getString("name");
                        if (name == null || name.isEmpty()) {
                            name = doc.getString("email");
                        }
                        member.put("name", name);
                        memberList.add(member);
                    }
                });
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_chat_settings, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // 뷰 초기화
        CardView cardMembers = dialogView.findViewById(R.id.cardMembers);
        CardView cardNotification = dialogView.findViewById(R.id.cardNotification);
        CardView cardLeave = dialogView.findViewById(R.id.cardLeave);
        CardView cardMuteAll = dialogView.findViewById(R.id.cardMuteAll);
        TextView tvMemberCount = dialogView.findViewById(R.id.tvMemberCount);
        LinearLayout layoutMemberList = dialogView.findViewById(R.id.layoutMemberList);
        RecyclerView rvMembers = dialogView.findViewById(R.id.rvMembers);
        ImageView ivNotificationIcon = dialogView.findViewById(R.id.ivNotificationIcon);
        TextView tvNotificationStatus = dialogView.findViewById(R.id.tvNotificationStatus);
        SwitchCompat switchNotification = dialogView.findViewById(R.id.switchNotification);
        ImageView ivMuteAllIcon = dialogView.findViewById(R.id.ivMuteAllIcon);
        TextView tvMuteAllStatus = dialogView.findViewById(R.id.tvMuteAllStatus);

        // 멤버 수 표시
        tvMemberCount.setText(memberList.size() + "명");

        // 알림 상태 설정
        switchNotification.setChecked(notificationEnabled);
        updateNotificationUI(ivNotificationIcon, tvNotificationStatus, notificationEnabled);

        // 모든 멤버 채팅 중단 버튼 (단체 채팅방 + 동아리 관리자만)
        if (isGroupChat && isClubAdmin) {
            cardMuteAll.setVisibility(View.VISIBLE);
            updateMuteAllUI(ivMuteAllIcon, tvMuteAllStatus, allMembersMuted);

            cardMuteAll.setOnClickListener(v -> {
                dialog.dismiss();
                showMuteAllConfirmDialog(!allMembersMuted);
            });
        } else {
            cardMuteAll.setVisibility(View.GONE);
        }

        // 나가기 버튼 표시 조건
        // - 단체 채팅방: 동아리 관리자만 가능
        // - 개인 채팅방:
        //   - 슈퍼관리자가 상대인 경우: 슈퍼관리자가 먼저 나가야 내가 나갈 수 있음
        //   - 그 외: 누구나 가능
        boolean canLeave;
        if (isGroupChat) {
            // 단체 채팅방은 동아리 관리자만 나가기 가능
            canLeave = isClubAdmin;
        } else {
            // 개인 채팅방
            boolean iAmSuperAdmin = SettingsActivity.isSuperAdminMode(this);
            if (iAmSuperAdmin) {
                // 내가 슈퍼관리자면 항상 나갈 수 있음
                canLeave = true;
            } else if (partnerIsSuperAdmin) {
                // 상대방이 슈퍼관리자인 경우, 슈퍼관리자가 먼저 나가야 나갈 수 있음
                canLeave = partnerHasLeft;
            } else {
                // 그 외에는 누구나 나갈 수 있음
                canLeave = true;
            }
        }

        if (!canLeave) {
            cardLeave.setVisibility(View.GONE);
        }

        // 멤버 카드 클릭 - 멤버 목록 토글
        cardMembers.setOnClickListener(v -> {
            if (layoutMemberList.getVisibility() == View.GONE) {
                layoutMemberList.setVisibility(View.VISIBLE);
                setupMemberList(rvMembers, dialog);
            } else {
                layoutMemberList.setVisibility(View.GONE);
            }
        });

        // 알림 스위치 변경
        switchNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            notificationEnabled = isChecked;
            updateNotificationUI(ivNotificationIcon, tvNotificationStatus, isChecked);
            saveNotificationSetting(isChecked);
        });

        // 채팅방 나가기
        cardLeave.setOnClickListener(v -> {
            dialog.dismiss();
            showLeaveConfirmDialog();
        });

        dialog.show();
    }

    private void updateNotificationUI(ImageView icon, TextView status, boolean enabled) {
        if (enabled) {
            icon.setImageResource(R.drawable.ic_notifications_on);
            status.setText("알림 켜기");
        } else {
            icon.setImageResource(R.drawable.ic_notifications_off);
            status.setText("알림 끄기");
        }
    }

    private void saveNotificationSetting(boolean enabled) {
        if (chatRoomId == null) return;

        firebaseManager.getDb()
                .collection("chatRooms")
                .document(chatRoomId)
                .update("notificationEnabled", enabled)
                .addOnSuccessListener(aVoid -> {
                    String message = enabled ? "알림이 켜졌습니다" : "알림이 꺼졌습니다";
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                });
    }

    private void setupMemberList(RecyclerView rvMembers, AlertDialog parentDialog) {
        rvMembers.setLayoutManager(new LinearLayoutManager(this));

        // 간단한 어댑터로 멤버 표시
        rvMembers.setAdapter(new RecyclerView.Adapter<MemberViewHolder>() {
            @Override
            public MemberViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_chat_member, parent, false);
                return new MemberViewHolder(view);
            }

            @Override
            public void onBindViewHolder(MemberViewHolder holder, int position) {
                Map<String, String> member = memberList.get(position);
                String rawName = member.get("name");
                String memberId = member.get("userId");

                // 이름 처리
                final String displayName;
                if (rawName != null && rawName.contains("@")) {
                    displayName = rawName.substring(0, rawName.indexOf("@"));
                } else {
                    displayName = rawName != null ? rawName : "알 수 없음";
                }
                holder.tvName.setText(displayName);

                // 본인 표시
                if (memberId != null && memberId.equals(currentUserId)) {
                    holder.tvIsMe.setVisibility(View.VISIBLE);
                } else {
                    holder.tvIsMe.setVisibility(View.GONE);
                }

                // 정지 상태 표시
                if (mutedMembers.contains(memberId)) {
                    holder.tvMuted.setVisibility(View.VISIBLE);
                } else {
                    holder.tvMuted.setVisibility(View.GONE);
                }

                // 관리자가 길게 터치 시 개별 멤버 정지/해제 (본인 제외, 단체 채팅방만)
                if (isGroupChat && isClubAdmin && memberId != null && !memberId.equals(currentUserId)) {
                    final String finalMemberId = memberId;
                    holder.itemView.setOnLongClickListener(v -> {
                        parentDialog.dismiss();
                        showMemberMuteDialog(finalMemberId, displayName, mutedMembers.contains(finalMemberId));
                        return true;
                    });
                } else {
                    holder.itemView.setOnLongClickListener(null);
                }
            }

            @Override
            public int getItemCount() {
                return memberList.size();
            }
        });
    }

    // 멤버 ViewHolder
    private static class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvIsMe, tvMuted;
        ImageView ivProfile;

        MemberViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvMemberName);
            tvIsMe = itemView.findViewById(R.id.tvIsMe);
            tvMuted = itemView.findViewById(R.id.tvMuted);
            ivProfile = itemView.findViewById(R.id.ivMemberProfile);
        }
    }

    private void showLeaveConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("채팅방 나가기")
                .setMessage("정말 이 채팅방을 나가시겠습니까?\n나가면 대화 내용이 모두 삭제됩니다.")
                .setPositiveButton("나가기", (dialog, which) -> leaveChatRoom())
                .setNegativeButton("취소", null)
                .show();
    }

    private void leaveChatRoom() {
        if (chatRoomId == null) return;

        if (isGroupChat) {
            // 단체 채팅방: participants에서 본인 제거
            firebaseManager.getDb()
                    .collection("chatRooms")
                    .document(chatRoomId)
                    .update("participants", FieldValue.arrayRemove(currentUserId))
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "채팅방을 나갔습니다", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "나가기 실패", Toast.LENGTH_SHORT).show());
        } else {
            // 개인 채팅방: leftUserId 설정 + participants에서 본인 제거
            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("leftUserId", currentUserId);
            updates.put("lastMessage", "상대방이 나갔습니다");
            updates.put("lastMessageTime", System.currentTimeMillis());

            firebaseManager.getDb()
                    .collection("chatRooms")
                    .document(chatRoomId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        // participants에서도 제거
                        firebaseManager.getDb()
                                .collection("chatRooms")
                                .document(chatRoomId)
                                .update("participants", FieldValue.arrayRemove(currentUserId))
                                .addOnSuccessListener(aVoid2 -> {
                                    Toast.makeText(this, "채팅방을 나갔습니다", Toast.LENGTH_SHORT).show();
                                    setResult(RESULT_OK);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "채팅방을 나갔습니다", Toast.LENGTH_SHORT).show();
                                    setResult(RESULT_OK);
                                    finish();
                                });
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "나가기 실패", Toast.LENGTH_SHORT).show());
        }
    }

    private void showMessageOptionsDialog(ChatMessage message, boolean isOwnMessage) {
        if (isOwnMessage) {
            // 본인 메시지: 수정/삭제 옵션
            String[] options = {"수정", "삭제"};
            new AlertDialog.Builder(this)
                    .setTitle("메시지 옵션")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            showEditMessageDialog(message);
                        } else {
                            showDeleteConfirmDialog(message);
                        }
                    })
                    .show();
        } else {
            // 상대방 메시지: 복사 옵션만
            String[] options = {"복사"};
            new AlertDialog.Builder(this)
                    .setTitle("메시지 옵션")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            copyMessageToClipboard(message.getMessage());
                        }
                    })
                    .show();
        }
    }

    private void showEditMessageDialog(ChatMessage message) {
        EditText editText = new EditText(this);
        editText.setText(message.getMessage());
        editText.setSelection(editText.getText().length());

        new AlertDialog.Builder(this)
                .setTitle("메시지 수정")
                .setView(editText)
                .setPositiveButton("수정", (dialog, which) -> {
                    String newMessage = editText.getText().toString().trim();
                    if (!TextUtils.isEmpty(newMessage)) {
                        updateMessage(message.getMessageId(), newMessage);
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void showDeleteConfirmDialog(ChatMessage message) {
        new AlertDialog.Builder(this)
                .setTitle("메시지 삭제")
                .setMessage("이 메시지를 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> deleteMessage(message.getMessageId()))
                .setNegativeButton("취소", null)
                .show();
    }

    private void updateMessage(String messageId, String newMessage) {
        firebaseManager.getDb()
                .collection("chatRooms")
                .document(chatRoomId)
                .collection("messages")
                .document(messageId)
                .update("message", newMessage, "edited", true)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "메시지가 수정되었습니다", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "수정 실패", Toast.LENGTH_SHORT).show());
    }

    private void deleteMessage(String messageId) {
        firebaseManager.getDb()
                .collection("chatRooms")
                .document(chatRoomId)
                .collection("messages")
                .document(messageId)
                .delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "메시지가 삭제되었습니다", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "삭제 실패", Toast.LENGTH_SHORT).show());
    }

    private void copyMessageToClipboard(String text) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("message", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "메시지가 복사되었습니다", Toast.LENGTH_SHORT).show();
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

        // orderBy를 제거하고 클라이언트에서 정렬 (기존 메시지 중 timestamp가 없는 경우 대비)
        messageListener = firebaseManager.getDb()
                .collection("chatRooms")
                .document(chatRoomId)
                .collection("messages")
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null || queryDocumentSnapshots == null) return;

                    List<ChatMessage> messages = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        // 수동으로 ChatMessage 객체 생성 (timestamp 타입 문제 방지)
                        ChatMessage message = new ChatMessage();
                        message.setMessageId(doc.getId());
                        message.setSenderId(doc.getString("senderId"));
                        message.setSenderName(doc.getString("senderName"));
                        message.setMessage(doc.getString("message"));

                        // timestamp 처리 (Long 또는 Timestamp 타입 모두 처리)
                        Object timestampObj = doc.get("timestamp");
                        long timestamp = 0;
                        if (timestampObj instanceof Long) {
                            timestamp = (Long) timestampObj;
                        } else if (timestampObj instanceof Number) {
                            timestamp = ((Number) timestampObj).longValue();
                        } else if (timestampObj instanceof com.google.firebase.Timestamp) {
                            timestamp = ((com.google.firebase.Timestamp) timestampObj).toDate().getTime();
                        }

                        // timestamp가 0이면 createdAt 또는 time 필드 확인
                        if (timestamp == 0) {
                            Object createdAtObj = doc.get("createdAt");
                            if (createdAtObj instanceof Long) {
                                timestamp = (Long) createdAtObj;
                            } else if (createdAtObj instanceof com.google.firebase.Timestamp) {
                                timestamp = ((com.google.firebase.Timestamp) createdAtObj).toDate().getTime();
                            }
                        }
                        if (timestamp == 0) {
                            Object timeObj = doc.get("time");
                            if (timeObj instanceof Long) {
                                timestamp = (Long) timeObj;
                            } else if (timeObj instanceof com.google.firebase.Timestamp) {
                                timestamp = ((com.google.firebase.Timestamp) timeObj).toDate().getTime();
                            }
                        }

                        message.setTimestamp(timestamp);

                        Boolean isRead = doc.getBoolean("isRead");
                        message.setRead(isRead != null && isRead);

                        messages.add(message);
                    }

                    // 클라이언트 측에서 timestamp 기준으로 정렬
                    Collections.sort(messages, (m1, m2) -> Long.compare(m1.getTimestamp(), m2.getTimestamp()));

                    // 새 리스트로 복사해서 전달 (참조 문제 방지)
                    List<ChatMessage> sortedMessages = new ArrayList<>(messages);
                    adapter.setMessages(sortedMessages);
                    if (!messages.isEmpty()) {
                        // 레이아웃 완료 후 맨 아래로 스크롤
                        rvMessages.post(() -> {
                            rvMessages.scrollToPosition(messages.size() - 1);
                        });
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
        messageData.put("timestamp", FieldValue.serverTimestamp());
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

                    // 메시지 전송 후 맨 아래로 스크롤
                    rvMessages.postDelayed(() -> {
                        int itemCount = adapter.getItemCount();
                        if (itemCount > 0) {
                            rvMessages.smoothScrollToPosition(itemCount - 1);
                        }
                    }, 100);
                });
    }

    private void updateChatRoomLastMessage(String lastMessage) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", lastMessage);
        updates.put("lastMessageTime", FieldValue.serverTimestamp());

        firebaseManager.getDb()
                .collection("chatRooms")
                .document(chatRoomId)
                .update(updates);
    }

    /**
     * 단체 채팅방인 경우 현재 사용자가 해당 동아리의 관리자인지 확인
     */
    private void checkClubAdminStatus() {
        if (chatRoomId == null || !chatRoomId.startsWith("group_")) {
            return;
        }

        // chatRoomId에서 clubId 추출 (group_clubId 형식)
        String clubId = chatRoomId.substring("group_".length());

        // 해당 동아리의 멤버 목록에서 현재 사용자의 역할 확인
        firebaseManager.getClubMembers(clubId, new FirebaseManager.MembersCallback() {
            @Override
            public void onSuccess(List<com.example.clubmanagement.models.Member> members) {
                for (com.example.clubmanagement.models.Member member : members) {
                    if (currentUserId != null && currentUserId.equals(member.getUserId())) {
                        // 관리자 역할인지 확인
                        String role = member.getRole();
                        boolean isAdmin = member.isAdmin();
                        if (isAdmin || isAdminRole(role)) {
                            isClubAdmin = true;
                        }
                        break;
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                // 실패 시 기본값 유지 (isClubAdmin = false)
            }
        });
    }

    /**
     * 관리자 역할인지 확인
     */
    private boolean isAdminRole(String role) {
        if (role == null) return false;
        return role.equals("회장") || role.equals("admin") ||
               role.equals("부회장") || role.equals("총무") || role.equals("회계");
    }

    /**
     * 개인 채팅방에서 상대방이 슈퍼관리자인지, 나갔는지 확인
     */
    private void checkPartnerSuperAdminStatus() {
        if (chatRoomId == null || partnerUserId == null) return;

        // 1. 채팅방 정보에서 상대방이 나갔는지 확인 (leftUserId)
        firebaseManager.getDb()
                .collection("chatRooms")
                .document(chatRoomId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String leftUserId = documentSnapshot.getString("leftUserId");
                        // 상대방이 나갔는지 확인
                        if (leftUserId != null && leftUserId.equals(partnerUserId)) {
                            partnerHasLeft = true;
                        }
                    }
                });

        // 2. 상대방이 슈퍼관리자인지 확인
        firebaseManager.getDb()
                .collection("users")
                .document(partnerUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean isSuperAdmin = documentSnapshot.getBoolean("isSuperAdmin");
                        if (isSuperAdmin != null && isSuperAdmin) {
                            partnerIsSuperAdmin = true;
                        }
                    }
                });
    }

    /**
     * 모든 멤버 채팅 중단 UI 업데이트
     */
    private void updateMuteAllUI(ImageView icon, TextView status, boolean muted) {
        if (muted) {
            icon.setImageResource(R.drawable.ic_notifications_on);
            status.setText("모든 멤버 채팅 재개");
        } else {
            icon.setImageResource(R.drawable.ic_notifications_off);
            status.setText("모든 멤버 채팅 중단");
        }
    }

    /**
     * 모든 멤버 채팅 중단/해제 확인 다이얼로그
     */
    private void showMuteAllConfirmDialog(boolean mute) {
        String title = mute ? "모든 멤버 채팅 중단" : "모든 멤버 채팅 재개";
        String message = mute
                ? "관리자를 제외한 모든 멤버의 채팅을 중단하시겠습니까?\n멤버들은 메시지를 보낼 수 없게 됩니다."
                : "모든 멤버의 채팅을 다시 허용하시겠습니까?";
        String positiveButton = mute ? "중단" : "재개";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButton, (dialog, which) -> {
                    setAllMembersMuted(mute);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    /**
     * 모든 멤버 채팅 정지/해제 Firebase 저장
     */
    private void setAllMembersMuted(boolean mute) {
        if (chatRoomId == null) return;

        firebaseManager.getDb()
                .collection("chatRooms")
                .document(chatRoomId)
                .update("allMembersMuted", mute)
                .addOnSuccessListener(aVoid -> {
                    allMembersMuted = mute;
                    String message = mute ? "모든 멤버의 채팅이 중단되었습니다" : "모든 멤버의 채팅이 재개되었습니다";
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "설정 변경 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * 개별 멤버 채팅 정지/해제 다이얼로그
     */
    private void showMemberMuteDialog(String memberId, String memberName, boolean currentlyMuted) {
        String title = currentlyMuted ? "채팅 정지 해제" : "채팅 정지";
        String message = currentlyMuted
                ? memberName + "님의 채팅 정지를 해제하시겠습니까?"
                : memberName + "님의 채팅을 정지하시겠습니까?\n해당 멤버는 메시지를 보낼 수 없게 됩니다.";
        String positiveButton = currentlyMuted ? "해제" : "정지";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButton, (dialog, which) -> {
                    setMemberMuted(memberId, !currentlyMuted);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    /**
     * 개별 멤버 채팅 정지/해제 Firebase 저장
     */
    private void setMemberMuted(String memberId, boolean mute) {
        if (chatRoomId == null || memberId == null) return;

        if (mute) {
            // 정지 목록에 추가
            firebaseManager.getDb()
                    .collection("chatRooms")
                    .document(chatRoomId)
                    .update("mutedMembers", FieldValue.arrayUnion(memberId))
                    .addOnSuccessListener(aVoid -> {
                        mutedMembers.add(memberId);
                        Toast.makeText(this, "해당 멤버의 채팅이 정지되었습니다", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "설정 변경 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // 정지 목록에서 제거
            firebaseManager.getDb()
                    .collection("chatRooms")
                    .document(chatRoomId)
                    .update("mutedMembers", FieldValue.arrayRemove(memberId))
                    .addOnSuccessListener(aVoid -> {
                        mutedMembers.remove(memberId);
                        Toast.makeText(this, "해당 멤버의 채팅 정지가 해제되었습니다", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "설정 변경 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    /**
     * 정지 상태 실시간 감지 리스너
     */
    private void listenForMuteStatus() {
        if (chatRoomId == null) return;

        chatRoomListener = firebaseManager.getDb()
                .collection("chatRooms")
                .document(chatRoomId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null || documentSnapshot == null || !documentSnapshot.exists()) return;

                    // 모든 멤버 정지 상태
                    Boolean allMuted = documentSnapshot.getBoolean("allMembersMuted");
                    allMembersMuted = allMuted != null && allMuted;

                    // 개별 정지 멤버 목록
                    List<String> mutedList = (List<String>) documentSnapshot.get("mutedMembers");
                    mutedMembers.clear();
                    if (mutedList != null) {
                        mutedMembers.addAll(mutedList);
                    }

                    // 현재 사용자가 정지 상태인지 확인 (관리자는 제외)
                    boolean amIMuted = !isClubAdmin && (allMembersMuted || mutedMembers.contains(currentUserId));

                    // UI 업데이트
                    updateMuteUI(amIMuted);
                });
    }

    /**
     * 채팅 입력 UI 업데이트 (정지 상태에 따라)
     */
    private void updateMuteUI(boolean muted) {
        isMuted = muted;
        if (muted) {
            // 입력창 숨기고 정지 메시지 표시
            inputLayout.setVisibility(View.GONE);
            layoutMutedMessage.setVisibility(View.VISIBLE);
        } else {
            // 입력창 표시하고 정지 메시지 숨김
            inputLayout.setVisibility(View.VISIBLE);
            layoutMutedMessage.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 채팅방을 나갈 때 읽음 처리 갱신
        if (chatRoomId != null) {
            getChatNotificationManager().markChatRoomAsRead(chatRoomId);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) {
            messageListener.remove();
        }
        if (chatRoomListener != null) {
            chatRoomListener.remove();
        }
        // 현재 열린 채팅방 해제
        getChatNotificationManager().clearCurrentOpenChatRoom();
    }
}
