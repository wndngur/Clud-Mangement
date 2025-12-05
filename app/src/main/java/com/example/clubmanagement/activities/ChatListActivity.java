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
import com.example.clubmanagement.adapters.ClubMemberListAdapter;
import com.example.clubmanagement.models.Member;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class ChatListActivity extends BaseActivity {

    private RecyclerView rvMemberList;
    private LinearLayout llEmptyState;
    private ProgressBar progressBar;
    private BottomNavigationView bottomNavigation;
    private Button btnTabList, btnTabChat;

    private FirebaseManager firebaseManager;
    private ClubMemberListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_chat_list);

        firebaseManager = FirebaseManager.getInstance();

        initViews();
        setupTabNavigation();
        setupBottomNavigation();
        loadJoinedClubsWithMembers();
    }

    private void initViews() {
        rvMemberList = findViewById(R.id.rvMemberList);
        llEmptyState = findViewById(R.id.llEmptyState);
        progressBar = findViewById(R.id.progressBar);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        btnTabList = findViewById(R.id.btnTabList);
        btnTabChat = findViewById(R.id.btnTabChat);

        adapter = new ClubMemberListAdapter();
        adapter.setOnMemberClickListener(this::showChatConfirmDialog);
        rvMemberList.setLayoutManager(new LinearLayoutManager(this));
        rvMemberList.setAdapter(adapter);
    }

    private void showChatConfirmDialog(Member member, String clubId, String clubName) {
        // userId 확인
        if (member.getUserId() == null || member.getUserId().isEmpty()) {
            Toast.makeText(this, "상대방 정보를 가져올 수 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        // 이름이 이메일 형식이면 @ 앞부분만 표시
        String memberName = member.getName();
        if (memberName == null || memberName.isEmpty()) {
            memberName = "이름 없음";
        } else if (memberName.contains("@")) {
            memberName = memberName.substring(0, memberName.indexOf("@"));
        }

        final String displayName = memberName;
        final String originalName = member.getName();
        final String partnerRole = member.getRole() != null ? member.getRole() : "회원";

        // 디버그 로그
        android.util.Log.d("ChatListActivity", "Creating chat - partnerId: " + member.getUserId() + ", partnerName: " + originalName + ", clubId: " + clubId);

        new AlertDialog.Builder(this)
                .setTitle("채팅")
                .setMessage(displayName + "님과 채팅을 하시겠습니까?")
                .setPositiveButton("확인", (dialog, which) -> {
                    // 채팅방 생성 및 ChatActivity로 이동
                    progressBar.setVisibility(View.VISIBLE);
                    firebaseManager.createOrGetChatRoom(
                            member.getUserId(),
                            originalName,
                            partnerRole,
                            clubId,
                            clubName,
                            new FirebaseManager.ChatRoomCallback() {
                                @Override
                                public void onSuccess(com.example.clubmanagement.models.ChatRoom chatRoom) {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(ChatListActivity.this, displayName + "님과의 채팅방이 생성되었습니다.", Toast.LENGTH_SHORT).show();

                                    // ChatActivity로 이동
                                    Intent intent = new Intent(ChatListActivity.this, ChatActivity.class);
                                    startActivity(intent);
                                    finish();
                                    overridePendingTransition(0, 0);
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(ChatListActivity.this, "채팅방 생성 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
                })
                .setNegativeButton("취소", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    private void setupTabNavigation() {
        // 목록 버튼 - 현재 화면
        btnTabList.setOnClickListener(v -> {
            // 이미 목록 화면
        });

        // 채팅 버튼 - 채팅 화면으로 이동
        btnTabChat.setOnClickListener(v -> {
            Intent intent = new Intent(ChatListActivity.this, ChatActivity.class);
            startActivity(intent);
            finish();
            overridePendingTransition(0, 0);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigation.setSelectedItemId(R.id.nav_chat);
        updateNavigationForSuperAdmin();
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
                Intent intent = new Intent(ChatListActivity.this, ClubListActivity.class);
                intent.putExtra("from_club_settings", false);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_recommend) {
                Intent intent = new Intent(ChatListActivity.this, ClubRecommendActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_myinfo) {
                Intent intent = new Intent(ChatListActivity.this, SettingsActivity.class);
                startActivity(intent);
                finish();
                return true;
            }

            return false;
        });
    }

    private void navigateToHome() {
        if (SettingsActivity.isSuperAdminMode(this)) {
            Intent intent = new Intent(ChatListActivity.this, AdminMainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
            return;
        }

        if (firebaseManager.getCurrentUserId() == null) {
            Intent intent = new Intent(ChatListActivity.this, MainActivityNew.class);
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
                    intent = new Intent(ChatListActivity.this, ClubMainActivity.class);
                    intent.putExtra("club_name", user.getCentralClubName());
                    intent.putExtra("club_id", user.getCentralClubId());
                } else {
                    intent = new Intent(ChatListActivity.this, MainActivityNew.class);
                }
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                Intent intent = new Intent(ChatListActivity.this, MainActivityNew.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });
    }

    private void loadJoinedClubsWithMembers() {
        progressBar.setVisibility(View.VISIBLE);
        llEmptyState.setVisibility(View.GONE);
        rvMemberList.setVisibility(View.GONE);

        if (firebaseManager.getCurrentUserId() == null) {
            progressBar.setVisibility(View.GONE);
            llEmptyState.setVisibility(View.VISIBLE);
            return;
        }

        // 사용자가 가입한 동아리 목록 가져오기
        firebaseManager.getCurrentUser(new FirebaseManager.UserCallback() {
            @Override
            public void onSuccess(com.example.clubmanagement.models.User user) {
                if (user == null) {
                    progressBar.setVisibility(View.GONE);
                    llEmptyState.setVisibility(View.VISIBLE);
                    return;
                }

                List<String> joinedClubIds = new ArrayList<>();
                List<String> joinedClubNames = new ArrayList<>();

                // 중앙동아리
                if (user.hasJoinedCentralClub()) {
                    joinedClubIds.add(user.getCentralClubId());
                    joinedClubNames.add(user.getCentralClubName());
                }

                // 일반동아리들
                List<String> generalClubIds = user.getGeneralClubIds();
                List<String> generalClubNames = user.getGeneralClubNames();
                if (generalClubIds != null && generalClubNames != null) {
                    for (int i = 0; i < generalClubIds.size(); i++) {
                        String clubId = generalClubIds.get(i);
                        if (!joinedClubIds.contains(clubId)) {
                            joinedClubIds.add(clubId);
                            String clubName = (i < generalClubNames.size()) ? generalClubNames.get(i) : "동아리";
                            joinedClubNames.add(clubName != null ? clubName : "동아리");
                        }
                    }
                }

                if (joinedClubIds.isEmpty()) {
                    progressBar.setVisibility(View.GONE);
                    llEmptyState.setVisibility(View.VISIBLE);
                    return;
                }

                // 각 동아리의 멤버 목록 가져오기
                loadMembersForClubs(joinedClubIds, joinedClubNames);
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                llEmptyState.setVisibility(View.VISIBLE);
            }
        });
    }

    private void loadMembersForClubs(List<String> clubIds, List<String> clubNames) {
        List<ClubMemberListAdapter.ClubWithMembers> clubList = new ArrayList<>();
        final int[] loadedCount = {0};

        for (int i = 0; i < clubIds.size(); i++) {
            String clubId = clubIds.get(i);
            String clubName = clubNames.get(i);
            final int index = i;

            firebaseManager.getClubMembers(clubId, new FirebaseManager.MembersCallback() {
                @Override
                public void onSuccess(List<Member> members) {
                    // 본인 제외한 멤버 목록 생성
                    String currentUserId = firebaseManager.getCurrentUserId();
                    List<Member> filteredMembers = new ArrayList<>();
                    for (Member member : members) {
                        if (!member.getUserId().equals(currentUserId)) {
                            filteredMembers.add(member);
                        }
                    }

                    synchronized (clubList) {
                        clubList.add(new ClubMemberListAdapter.ClubWithMembers(clubId, clubName, filteredMembers));
                        loadedCount[0]++;

                        if (loadedCount[0] == clubIds.size()) {
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                if (clubList.isEmpty()) {
                                    llEmptyState.setVisibility(View.VISIBLE);
                                } else {
                                    rvMemberList.setVisibility(View.VISIBLE);
                                    adapter.setClubList(clubList);
                                }
                            });
                        }
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    synchronized (clubList) {
                        loadedCount[0]++;

                        if (loadedCount[0] == clubIds.size()) {
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                if (clubList.isEmpty()) {
                                    llEmptyState.setVisibility(View.VISIBLE);
                                } else {
                                    rvMemberList.setVisibility(View.VISIBLE);
                                    adapter.setClubList(clubList);
                                }
                            });
                        }
                    }
                }
            });
        }
    }
}
