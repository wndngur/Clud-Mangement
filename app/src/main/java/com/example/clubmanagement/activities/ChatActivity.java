package com.example.clubmanagement.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

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

import java.util.List;

public class ChatActivity extends BaseActivity {

    private RecyclerView rvChatRooms;
    private LinearLayout llEmptyState;
    private ProgressBar progressBar;
    private BottomNavigationView bottomNavigation;
    private Button btnTabList, btnTabChat;

    private FirebaseManager firebaseManager;
    private ChatRoomAdapter chatRoomAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_chat);

        firebaseManager = FirebaseManager.getInstance();

        initViews();
        setupTabNavigation();
        setupBottomNavigation();
        loadChatRooms();
    }

    private void initViews() {
        rvChatRooms = findViewById(R.id.rvChatRooms);
        llEmptyState = findViewById(R.id.llEmptyState);
        progressBar = findViewById(R.id.progressBar);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        btnTabList = findViewById(R.id.btnTabList);
        btnTabChat = findViewById(R.id.btnTabChat);

        // ChatRoomAdapter 설정
        chatRoomAdapter = new ChatRoomAdapter();
        chatRoomAdapter.setOnChatRoomClickListener(this::onChatRoomClick);
        chatRoomAdapter.setOnChatRoomSettingsListener(new ChatRoomAdapter.OnChatRoomSettingsListener() {
            @Override
            public void onToggleNotification(ChatRoom chatRoom, int position) {
                toggleNotification(chatRoom, position);
            }

            @Override
            public void onDeleteChatRoom(ChatRoom chatRoom, int position) {
                showDeleteConfirmDialog(chatRoom, position);
            }
        });
        rvChatRooms.setLayoutManager(new LinearLayoutManager(this));
        rvChatRooms.setAdapter(chatRoomAdapter);
    }

    private void onChatRoomClick(ChatRoom chatRoom) {
        Intent intent = new Intent(this, ChatDetailActivity.class);
        intent.putExtra("chat_room_id", chatRoom.getChatRoomId());
        intent.putExtra("partner_user_id", chatRoom.getPartnerUserId());
        intent.putExtra("partner_name", chatRoom.getPartnerName());
        intent.putExtra("partner_role", chatRoom.getPartnerRole());
        intent.putExtra("club_name", chatRoom.getClubName());
        intent.putExtra("is_group_chat", chatRoom.isGroupChat());
        startActivity(intent);
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

    private void showDeleteConfirmDialog(ChatRoom chatRoom, int position) {
        new AlertDialog.Builder(this)
                .setTitle("채팅방 나가기")
                .setMessage("채팅방을 나가시겠습니까?\n상대방에게 나갔음이 표시됩니다.")
                .setPositiveButton("나가기", (dialog, which) -> {
                    leaveChatRoom(chatRoom, position);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void leaveChatRoom(ChatRoom chatRoom, int position) {
        firebaseManager.leaveChatRoom(chatRoom.getChatRoomId(), new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                chatRoomAdapter.removeChatRoom(position);
                Toast.makeText(ChatActivity.this, "채팅방을 나갔습니다", Toast.LENGTH_SHORT).show();

                // 채팅방이 없으면 빈 상태 표시
                if (chatRoomAdapter.getItemCount() == 0) {
                    llEmptyState.setVisibility(View.VISIBLE);
                    rvChatRooms.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ChatActivity.this, "채팅방 나가기 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupTabNavigation() {
        // 목록 버튼 - 목록 화면으로 이동
        btnTabList.setOnClickListener(v -> {
            Intent intent = new Intent(ChatActivity.this, ChatListActivity.class);
            startActivity(intent);
            finish();
            overridePendingTransition(0, 0);
        });

        // 채팅 버튼 - 현재 화면
        btnTabChat.setOnClickListener(v -> {
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
                progressBar.setVisibility(View.GONE);

                if (chatRooms == null || chatRooms.isEmpty()) {
                    llEmptyState.setVisibility(View.VISIBLE);
                    rvChatRooms.setVisibility(View.GONE);
                } else {
                    llEmptyState.setVisibility(View.GONE);
                    rvChatRooms.setVisibility(View.VISIBLE);
                    chatRoomAdapter.setChatRooms(chatRooms);
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

    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigation.setSelectedItemId(R.id.nav_chat);
        updateNavigationForSuperAdmin();
        // 화면 복귀 시 채팅방 목록 새로고침
        loadChatRooms();
    }
}
