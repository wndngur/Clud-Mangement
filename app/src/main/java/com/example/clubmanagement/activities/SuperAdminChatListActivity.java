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

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clubmanagement.AdminMainActivity;
import com.example.clubmanagement.BaseActivity;
import com.example.clubmanagement.R;
import com.example.clubmanagement.SettingsActivity;
import com.example.clubmanagement.adapters.ClubMemberListAdapter;
import com.example.clubmanagement.models.Club;
import com.example.clubmanagement.models.Member;
import com.example.clubmanagement.models.User;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

/**
 * 총관리자용 채팅 목록 Activity
 * - 모든 동아리의 관리자를 동아리별로 그룹화하여 표시
 * - 모든 최고 관리자를 별도 그룹으로 표시
 */
public class SuperAdminChatListActivity extends BaseActivity {

    private RecyclerView rvMemberList;
    private LinearLayout llEmptyState;
    private ProgressBar progressBar;
    private BottomNavigationView bottomNavigation;
    private LinearLayout tabFriends, tabChat;
    private ImageView ivTabFriends, ivTabChat;
    private TextView tvTabFriends, tvTabChat, tvEmptyTitle, tvEmptySubtitle;
    private EditText etSearch;
    private ImageButton btnClearSearch;

    private FirebaseManager firebaseManager;
    private ClubMemberListAdapter adapter;
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_chat_list);

        firebaseManager = FirebaseManager.getInstance();

        initViews();
        setupSearch();
        setupTabNavigation();
        setupBottomNavigation();
        loadAllAdminsAndSuperAdmins();
    }

    private void initViews() {
        rvMemberList = findViewById(R.id.rvMemberList);
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

        adapter = new ClubMemberListAdapter();
        adapter.setOnMemberClickListener(this::showChatConfirmDialog);
        rvMemberList.setLayoutManager(new LinearLayoutManager(this));
        rvMemberList.setAdapter(adapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                adapter.filter(currentSearchQuery);

                // X 버튼 표시/숨김
                btnClearSearch.setVisibility(currentSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);

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
        if (adapter.getItemCount() == 0) {
            llEmptyState.setVisibility(View.VISIBLE);
            rvMemberList.setVisibility(View.GONE);

            if (!currentSearchQuery.isEmpty()) {
                tvEmptyTitle.setText("검색 결과가 없습니다");
                tvEmptySubtitle.setText("다른 이름으로 검색해보세요");
            } else {
                tvEmptyTitle.setText("채팅 가능한 관리자가 없습니다");
                tvEmptySubtitle.setText("동아리 관리자가 등록되면 표시됩니다");
            }
        } else {
            llEmptyState.setVisibility(View.GONE);
            rvMemberList.setVisibility(View.VISIBLE);
        }
    }

    private void showChatConfirmDialog(Member member, String clubId, String clubName) {
        if (member.getUserId() == null || member.getUserId().isEmpty()) {
            Toast.makeText(this, "상대방 정보를 가져올 수 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        String memberName = member.getName();
        if (memberName == null || memberName.isEmpty()) {
            memberName = "이름 없음";
        } else if (memberName.contains("@")) {
            memberName = memberName.substring(0, memberName.indexOf("@"));
        }

        final String displayName = memberName;
        final String originalName = member.getName();
        final String partnerRole = member.getRole() != null ? member.getRole() : "관리자";

        new AlertDialog.Builder(this)
                .setTitle("채팅")
                .setMessage(displayName + "님과 채팅을 하시겠습니까?")
                .setPositiveButton("확인", (dialog, which) -> {
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
                                    Toast.makeText(SuperAdminChatListActivity.this,
                                            displayName + "님과의 채팅방이 생성되었습니다.", Toast.LENGTH_SHORT).show();

                                    Intent intent = new Intent(SuperAdminChatListActivity.this, ChatActivity.class);
                                    startActivity(intent);
                                    finish();
                                    overridePendingTransition(0, 0);
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(SuperAdminChatListActivity.this,
                                            "채팅방 생성 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void setupTabNavigation() {
        // 친구 탭 - 현재 화면
        tabFriends.setOnClickListener(v -> {
            // 이미 목록 화면
        });

        // 채팅 탭 - 채팅 화면으로 이동
        tabChat.setOnClickListener(v -> {
            Intent intent = new Intent(SuperAdminChatListActivity.this, ChatActivity.class);
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
            bottomNavigation.getMenu().findItem(R.id.nav_recommend).setVisible(false);
        }
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                Intent intent = new Intent(SuperAdminChatListActivity.this, AdminMainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_chat) {
                return true;
            } else if (itemId == R.id.nav_clubs) {
                Intent intent = new Intent(SuperAdminChatListActivity.this, ClubListActivity.class);
                intent.putExtra("from_club_settings", false);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_myinfo) {
                Intent intent = new Intent(SuperAdminChatListActivity.this, SettingsActivity.class);
                startActivity(intent);
                finish();
                return true;
            }

            return false;
        });
    }

    private void loadAllAdminsAndSuperAdmins() {
        progressBar.setVisibility(View.VISIBLE);
        llEmptyState.setVisibility(View.GONE);
        rvMemberList.setVisibility(View.GONE);

        List<ClubMemberListAdapter.ClubWithMembers> resultList = new ArrayList<>();
        String currentUserId = firebaseManager.getCurrentUserId();

        // 1. 먼저 모든 최고 관리자 로드
        firebaseManager.getAllSuperAdmins(new FirebaseManager.UsersCallback() {
            @Override
            public void onSuccess(List<User> superAdmins) {
                // 최고 관리자 그룹 생성
                List<Member> superAdminMembers = new ArrayList<>();
                for (User admin : superAdmins) {
                    if (admin.getUserId() != null && !admin.getUserId().equals(currentUserId)) {
                        Member member = new Member();
                        member.setUserId(admin.getUserId());
                        member.setName(admin.getName() != null ? admin.getName() : admin.getEmail());
                        member.setRole("최고 관리자");
                        member.setAdmin(true);
                        superAdminMembers.add(member);
                    }
                }

                if (!superAdminMembers.isEmpty()) {
                    resultList.add(new ClubMemberListAdapter.ClubWithMembers(
                            "super_admin",
                            "최고 관리자",
                            superAdminMembers
                    ));
                }

                // 2. 모든 동아리의 관리자 로드
                loadAllClubAdmins(resultList, currentUserId);
            }

            @Override
            public void onFailure(Exception e) {
                // 최고 관리자 로드 실패해도 동아리 관리자는 로드
                loadAllClubAdmins(resultList, currentUserId);
            }
        });
    }

    private void loadAllClubAdmins(List<ClubMemberListAdapter.ClubWithMembers> resultList, String currentUserId) {
        // 모든 동아리 가져오기
        firebaseManager.getAllClubs(new FirebaseManager.ClubListCallback() {
            @Override
            public void onSuccess(List<Club> clubs) {
                if (clubs.isEmpty()) {
                    showResults(resultList);
                    return;
                }

                final int[] loadedCount = {0};
                final int totalClubs = clubs.size();

                for (Club club : clubs) {
                    String clubId = club.getId();
                    String clubName = club.getName();

                    // 동아리 멤버 중 관리자만 가져오기
                    firebaseManager.getClubMembers(clubId, new FirebaseManager.MembersCallback() {
                        @Override
                        public void onSuccess(List<Member> members) {
                            // 관리자만 필터링 (본인 제외)
                            List<Member> adminMembers = new ArrayList<>();
                            for (Member member : members) {
                                if (member.getUserId() != null &&
                                    !member.getUserId().equals(currentUserId) &&
                                    (member.isAdmin() || isAdminRole(member.getRole()))) {
                                    adminMembers.add(member);
                                }
                            }

                            synchronized (resultList) {
                                if (!adminMembers.isEmpty()) {
                                    String groupName = clubName + " 관리자";
                                    resultList.add(new ClubMemberListAdapter.ClubWithMembers(
                                            clubId,
                                            groupName,
                                            adminMembers
                                    ));
                                }

                                loadedCount[0]++;
                                if (loadedCount[0] == totalClubs) {
                                    runOnUiThread(() -> showResults(resultList));
                                }
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {
                            synchronized (resultList) {
                                loadedCount[0]++;
                                if (loadedCount[0] == totalClubs) {
                                    runOnUiThread(() -> showResults(resultList));
                                }
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailure(Exception e) {
                showResults(resultList);
            }
        });
    }

    private boolean isAdminRole(String role) {
        if (role == null) return false;
        return role.equals("회장") || role.equals("admin") ||
               role.equals("부회장") || role.equals("총무") || role.equals("회계");
    }

    private void showResults(List<ClubMemberListAdapter.ClubWithMembers> resultList) {
        progressBar.setVisibility(View.GONE);

        if (resultList.isEmpty()) {
            llEmptyState.setVisibility(View.VISIBLE);
            rvMemberList.setVisibility(View.GONE);
        } else {
            llEmptyState.setVisibility(View.GONE);
            rvMemberList.setVisibility(View.VISIBLE);
            adapter.setClubList(resultList);
        }
    }
}
