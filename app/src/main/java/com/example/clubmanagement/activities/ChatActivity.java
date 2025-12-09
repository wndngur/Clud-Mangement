package com.example.clubmanagement.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clubmanagement.AdminMainActivity;
import com.example.clubmanagement.BaseActivity;
import com.example.clubmanagement.MainActivityNew;
import com.example.clubmanagement.R;
import com.example.clubmanagement.SettingsActivity;
import com.example.clubmanagement.adapters.ChatRoomAdapter;
import com.example.clubmanagement.models.ChatRoom;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;

public class ChatActivity extends BaseActivity {

    private RecyclerView rvChatRooms;
    private LinearLayout llEmptyState;
    private ProgressBar progressBar;
    private BottomNavigationView bottomNavigation;
    private LinearLayout tabFriends, tabChat;
    private ImageView ivTabFriends, ivTabChat;
    private TextView tvTabFriends, tvTabChat, tvEmptyTitle, tvEmptySubtitle;
    private EditText etSearch;
    private ImageButton btnClearSearch;

    private FirebaseManager firebaseManager;
    private ChatRoomAdapter chatRoomAdapter;
    private ActivityResultLauncher<Intent> chatDetailLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_chat);

        firebaseManager = FirebaseManager.getInstance();

        // ChatDetailActivity에서 돌아올 때 결과 처리
        chatDetailLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // 채팅방에서 나가기 등의 변경이 있었을 때 목록 새로고침
                        loadChatRooms();
                    }
                }
        );

        initViews();
        setupSearch();
        setupTabNavigation();
        setupBottomNavigation();
        loadChatRooms();
    }

    private void initViews() {
        rvChatRooms = findViewById(R.id.rvChatRooms);
        llEmptyState = findViewById(R.id.llEmptyState);
        progressBar = findViewById(R.id.progressBar);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        tabFriends = findViewById(R.id.tabFriends);
        tabChat = findViewById(R.id.tabChat);
        ivTabFriends = findViewById(R.id.ivTabFriends);
        ivTabChat = findViewById(R.id.ivTabChat);
        tvTabFriends = findViewById(R.id.tvTabFriends);
        tvTabChat = findViewById(R.id.tvTabChat);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle);
        etSearch = findViewById(R.id.etSearch);
        btnClearSearch = findViewById(R.id.btnClearSearch);

        // ChatRoomAdapter 설정
        chatRoomAdapter = new ChatRoomAdapter();
        chatRoomAdapter.setOnChatRoomClickListener(this::onChatRoomClick);
        chatRoomAdapter.setOnChatRoomLongClickListener(this::showChatRoomOptionsDialog);
        rvChatRooms.setLayoutManager(new LinearLayoutManager(this));
        rvChatRooms.setAdapter(chatRoomAdapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString();
                chatRoomAdapter.filter(query);

                // X 버튼 표시/숨김
                btnClearSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);

                // 검색 결과에 따라 빈 상태 업데이트
                updateEmptyState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            etSearch.clearFocus();
        });
    }

    private void updateEmptyState() {
        if (chatRoomAdapter.getItemCount() == 0) {
            llEmptyState.setVisibility(View.VISIBLE);
            rvChatRooms.setVisibility(View.GONE);

            if (chatRoomAdapter.hasNoResults()) {
                tvEmptyTitle.setText("검색 결과가 없습니다");
                tvEmptySubtitle.setText("다른 검색어로 시도해보세요");
            } else {
                tvEmptyTitle.setText("참여 중인 채팅방이 없습니다");
                tvEmptySubtitle.setText("동아리에 가입하면 채팅방이 생성됩니다");
            }
        } else {
            llEmptyState.setVisibility(View.GONE);
            rvChatRooms.setVisibility(View.VISIBLE);
        }
    }

    private void onChatRoomClick(ChatRoom chatRoom) {
        Intent intent = new Intent(this, ChatDetailActivity.class);
        intent.putExtra("chat_room_id", chatRoom.getChatRoomId());
        intent.putExtra("partner_user_id", chatRoom.getPartnerUserId());
        intent.putExtra("partner_name", chatRoom.getPartnerName());
        intent.putExtra("partner_role", chatRoom.getPartnerRole());
        intent.putExtra("club_name", chatRoom.getClubName());
        intent.putExtra("is_group_chat", chatRoom.isGroupChat());
        chatDetailLauncher.launch(intent);
    }

    // 길게 터치 시 설정 다이얼로그 표시
    private void showChatRoomOptionsDialog(ChatRoom chatRoom, int position) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_chat_room_options, null);
        dialog.setContentView(view);

        TextView tvChatRoomName = view.findViewById(R.id.tvChatRoomName);
        LinearLayout layoutNotification = view.findViewById(R.id.layoutNotification);
        LinearLayout layoutLeave = view.findViewById(R.id.layoutLeave);
        TextView tvNotificationText = view.findViewById(R.id.tvNotificationText);

        tvChatRoomName.setText(chatRoom.getChatRoomTitle());

        // 알림 상태에 따라 텍스트 변경
        tvNotificationText.setText(chatRoom.isNotificationEnabled() ? "알림 끄기" : "알림 켜기");

        // 알림 설정
        layoutNotification.setOnClickListener(v -> {
            dialog.dismiss();
            toggleNotification(chatRoom, position);
        });

        // 채팅방 나가기 (단체 채팅방이 아닌 경우에만)
        if (chatRoom.isGroupChat()) {
            layoutLeave.setVisibility(View.GONE);
        } else {
            layoutLeave.setOnClickListener(v -> {
                dialog.dismiss();
                showLeaveConfirmDialog(chatRoom, position);
            });
        }

        dialog.show();
    }

    private void toggleNotification(ChatRoom chatRoom, int position) {
        boolean newState = !chatRoom.isNotificationEnabled();
        firebaseManager.toggleChatRoomNotification(chatRoom.getChatRoomId(), newState, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                chatRoom.setNotificationEnabled(newState);
                String message = newState ? "알림이 켜졌습니다" : "알림이 꺼졌습니다";
                Toast.makeText(ChatActivity.this, message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ChatActivity.this, "알림 설정 변경 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLeaveConfirmDialog(ChatRoom chatRoom, int position) {
        new AlertDialog.Builder(this)
                .setTitle("채팅방 나가기")
                .setMessage("채팅방을 나가시겠습니까?\n대화 내용이 삭제되며 상대방에게 나갔음이 표시됩니다.")
                .setPositiveButton("나가기", (dialog, which) -> {
                    leaveChatRoom(chatRoom, position);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void leaveChatRoom(ChatRoom chatRoom, int position) {
        String chatRoomId = chatRoom.getChatRoomId();
        firebaseManager.leaveChatRoom(chatRoomId, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                // ID로 채팅방 제거 (더 안정적)
                chatRoomAdapter.removeChatRoomById(chatRoomId);
                Toast.makeText(ChatActivity.this, "채팅방을 나갔습니다", Toast.LENGTH_SHORT).show();
                updateEmptyState();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ChatActivity.this, "채팅방 나가기 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupTabNavigation() {
        // 친구 탭 - 친구 화면으로 이동
        tabFriends.setOnClickListener(v -> {
            Intent intent = new Intent(ChatActivity.this, ChatListActivity.class);
            startActivity(intent);
            finish();
            overridePendingTransition(0, 0);
        });

        // 채팅 탭 - 현재 화면
        tabChat.setOnClickListener(v -> {
            // 이미 채팅 화면
        });
    }

    private void updateNavigationForSuperAdmin() {
        if (bottomNavigation != null) {
            boolean isSuperAdmin = SettingsActivity.isSuperAdminMode(this);
            bottomNavigation.getMenu().findItem(R.id.nav_recommend).setVisible(!isSuperAdmin);
        }
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                navigateToHome();
                return true;
            } else if (itemId == R.id.nav_chat) {
                // 현재 페이지
                return true;
            } else if (itemId == R.id.nav_clubs) {
                Intent intent = new Intent(ChatActivity.this, ClubListActivity.class);
                intent.putExtra("from_club_settings", false);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_recommend) {
                Intent intent = new Intent(ChatActivity.this, ClubRecommendActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_myinfo) {
                Intent intent = new Intent(ChatActivity.this, SettingsActivity.class);
                startActivity(intent);
                finish();
                return true;
            }

            return false;
        });
    }

    private void navigateToHome() {
        // 관리자 모드인지 확인
        if (SettingsActivity.isSuperAdminMode(this)) {
            Intent intent = new Intent(ChatActivity.this, AdminMainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
            return;
        }

        // 로그인되지 않은 경우
        if (firebaseManager.getCurrentUserId() == null) {
            Intent intent = new Intent(ChatActivity.this, MainActivityNew.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
            return;
        }

        firebaseManager.getCurrentUser(new FirebaseManager.UserCallback() {
            @Override
            public void onSuccess(com.example.clubmanagement.models.User user) {
                Intent intent;
                if (user != null && user.hasJoinedCentralClub()) {
                    intent = new Intent(ChatActivity.this, ClubMainActivity.class);
                    intent.putExtra("club_name", user.getCentralClubName());
                    intent.putExtra("club_id", user.getCentralClubId());
                } else {
                    intent = new Intent(ChatActivity.this, MainActivityNew.class);
                }
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                Intent intent = new Intent(ChatActivity.this, MainActivityNew.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });
    }

    private void loadChatRooms() {
        progressBar.setVisibility(View.VISIBLE);
        llEmptyState.setVisibility(View.GONE);
        rvChatRooms.setVisibility(View.GONE);

        // 로그인되지 않은 경우 빈 상태 표시
        if (firebaseManager.getCurrentUserId() == null) {
            progressBar.setVisibility(View.GONE);
            llEmptyState.setVisibility(View.VISIBLE);
            return;
        }

        // 먼저 사용자가 가입한 동아리의 단체 채팅방에 참여 확인
        firebaseManager.ensureGroupChatMembership(new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                // 채팅방 목록 로드
                loadChatRoomsData();
            }

            @Override
            public void onFailure(Exception e) {
                // 실패해도 채팅방 목록은 로드
                loadChatRoomsData();
            }
        });
    }

    private void loadChatRoomsData() {
        firebaseManager.getChatRooms(new FirebaseManager.ChatRoomsCallback() {
            @Override
            public void onSuccess(List<ChatRoom> chatRooms) {
                if (chatRooms == null || chatRooms.isEmpty()) {
                    progressBar.setVisibility(View.GONE);
                    llEmptyState.setVisibility(View.VISIBLE);
                    rvChatRooms.setVisibility(View.GONE);
                } else {
                    // 각 채팅방의 읽지 않은 메시지 수 계산
                    calculateUnreadCountsForRooms(chatRooms);
                }
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                llEmptyState.setVisibility(View.VISIBLE);
                rvChatRooms.setVisibility(View.GONE);
                Toast.makeText(ChatActivity.this, "채팅방 목록 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void calculateUnreadCountsForRooms(List<ChatRoom> chatRooms) {
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) {
            progressBar.setVisibility(View.GONE);
            llEmptyState.setVisibility(View.GONE);
            rvChatRooms.setVisibility(View.VISIBLE);
            chatRoomAdapter.setChatRooms(chatRooms);
            return;
        }

        final int[] processedCount = {0};
        int totalRooms = chatRooms.size();

        for (ChatRoom chatRoom : chatRooms) {
            String chatRoomId = chatRoom.getChatRoomId();
            long lastReadTime = getChatNotificationManager().getLastReadTimestamp(chatRoomId);

            // 마지막 읽은 시간 이후의 메시지 중 내가 보내지 않은 메시지 수 계산
            firebaseManager.getDb()
                    .collection("chatRooms")
                    .document(chatRoomId)
                    .collection("messages")
                    .whereGreaterThan("timestamp", lastReadTime)
                    .get()
                    .addOnSuccessListener(messagesSnapshot -> {
                        int unreadCount = 0;
                        for (com.google.firebase.firestore.DocumentSnapshot msgDoc : messagesSnapshot.getDocuments()) {
                            String senderId = msgDoc.getString("senderId");
                            // 내가 보낸 메시지가 아닌 경우만 카운트
                            if (senderId != null && !senderId.equals(currentUserId)) {
                                unreadCount++;
                            }
                        }
                        chatRoom.setUnreadCount(unreadCount);

                        processedCount[0]++;
                        if (processedCount[0] >= totalRooms) {
                            // 모든 채팅방 처리 완료
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                llEmptyState.setVisibility(View.GONE);
                                rvChatRooms.setVisibility(View.VISIBLE);
                                chatRoomAdapter.setChatRooms(chatRooms);
                            });
                        }
                    })
                    .addOnFailureListener(e -> {
                        processedCount[0]++;
                        if (processedCount[0] >= totalRooms) {
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                llEmptyState.setVisibility(View.GONE);
                                rvChatRooms.setVisibility(View.VISIBLE);
                                chatRoomAdapter.setChatRooms(chatRooms);
                            });
                        }
                    });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigation.setSelectedItemId(R.id.nav_chat);
        updateNavigationForSuperAdmin();
        // 화면 복귀 시 채팅방 목록 새로고침
        loadChatRooms();
    }
}
