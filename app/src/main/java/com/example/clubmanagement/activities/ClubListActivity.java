package com.example.clubmanagement.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.clubmanagement.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class ClubListActivity extends AppCompatActivity {

    private LinearLayout llClubListContainer;
    private ProgressBar progressBar;
    private BottomNavigationView bottomNavigation;
    private ImageView ivBack;
    private boolean fromClubSettings = false; // 중앙동아리 설정에서 왔는지 여부
    private com.example.clubmanagement.models.User currentUser;

    // Sample club data
    private static class ClubItem {
        String id;
        String name;
        String description;
        int memberCount; // 현재 인원 수

        ClubItem(String id, String name, String description, int memberCount) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.memberCount = memberCount;
        }
    }

    // 중앙동아리 최소 인원
    private static final int CENTRAL_CLUB_MIN_MEMBERS = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_club_list);

        // Check if came from ClubSettingsActivity
        fromClubSettings = getIntent().getBooleanExtra("from_club_settings", false);

        initViews();
        setupBottomNavigation();
        setupBackButton();
        loadCurrentUser();
    }

    private void loadCurrentUser() {
        com.example.clubmanagement.utils.FirebaseManager firebaseManager =
            com.example.clubmanagement.utils.FirebaseManager.getInstance();

        // 로그인되지 않은 경우 바로 동아리 목록 표시
        if (firebaseManager.getCurrentUserId() == null) {
            currentUser = null;
            loadClubList();
            return;
        }

        firebaseManager.getCurrentUser(new com.example.clubmanagement.utils.FirebaseManager.UserCallback() {
            @Override
            public void onSuccess(com.example.clubmanagement.models.User user) {
                currentUser = user;
                loadClubList();
            }

            @Override
            public void onFailure(Exception e) {
                currentUser = null;
                loadClubList();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 네비게이션 바가 보이는 경우에만 선택 상태 설정
        if (!fromClubSettings && bottomNavigation.getVisibility() == View.VISIBLE) {
            bottomNavigation.setSelectedItemId(R.id.nav_clubs);
        }
    }

    private void initViews() {
        llClubListContainer = findViewById(R.id.llClubListContainer);
        progressBar = findViewById(R.id.progressBar);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        ivBack = findViewById(R.id.ivBack);
    }

    private void setupBackButton() {
        if (fromClubSettings) {
            // 중앙동아리 설정에서 온 경우
            // 1. 네비게이션 바 숨기기
            bottomNavigation.setVisibility(View.GONE);

            // 2. 뒤로가기 버튼 표시
            ivBack.setVisibility(View.VISIBLE);
            ivBack.setOnClickListener(v -> finish());
        } else {
            // 하단 네비게이션에서 온 경우
            // 네비게이션 바 표시, 뒤로가기 버튼 숨김
            bottomNavigation.setVisibility(View.VISIBLE);
            ivBack.setVisibility(View.GONE);
        }
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                // 홈으로 이동 - 중앙동아리 가입 여부에 따라 다른 화면으로 이동
                navigateToHome();
                return true;
            } else if (itemId == R.id.nav_clubs) {
                // 현재 페이지 - 아무 동작 안함
                return true;
            } else if (itemId == R.id.nav_recommend) {
                // 동아리 추천 페이지로 이동
                Intent intent = new Intent(ClubListActivity.this, ClubRecommendActivity.class);
                startActivity(intent);
                finish(); // 현재 페이지 종료
                return true;
            } else if (itemId == R.id.nav_myinfo) {
                Toast.makeText(this, "내정보", Toast.LENGTH_SHORT).show();
                return true;
            }

            return false;
        });
    }

    private void navigateToHome() {
        com.example.clubmanagement.utils.FirebaseManager firebaseManager =
            com.example.clubmanagement.utils.FirebaseManager.getInstance();

        // 로그인되지 않은 경우 바로 MainActivityNew로 이동
        if (firebaseManager.getCurrentUserId() == null) {
            Intent intent = new Intent(ClubListActivity.this,
                com.example.clubmanagement.MainActivityNew.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
            return;
        }

        firebaseManager.getCurrentUser(new com.example.clubmanagement.utils.FirebaseManager.UserCallback() {
            @Override
            public void onSuccess(com.example.clubmanagement.models.User user) {
                Intent intent;
                if (user != null && user.hasJoinedCentralClub()) {
                    // 중앙동아리에 가입된 경우 - 동아리 메인으로 이동
                    intent = new Intent(ClubListActivity.this,
                        com.example.clubmanagement.activities.ClubMainActivity.class);
                    intent.putExtra("club_name", user.getCentralClubName());
                    intent.putExtra("club_id", user.getCentralClubId());
                } else {
                    // 미가입 - MainActivityNew로 이동
                    intent = new Intent(ClubListActivity.this,
                        com.example.clubmanagement.MainActivityNew.class);
                }
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                // 오류 발생 시 MainActivityNew로 이동
                Intent intent = new Intent(ClubListActivity.this,
                    com.example.clubmanagement.MainActivityNew.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });
    }

    private void loadClubList() {
        // Sample club data with member counts
        List<ClubItem> clubs = new ArrayList<>();
        clubs.add(new ClubItem(
            "computer_science_club",
            "컴퓨터공학과 학술동아리",
            "컴퓨터공학 전공 학생들이 함께 프로그래밍 공부와 프로젝트를 진행하는 동아리입니다. 매주 스터디와 세미나를 진행하며, 학기당 1회 이상 팀 프로젝트를 완성합니다.",
            18 // 18명 - 2명 더 필요
        ));
        clubs.add(new ClubItem(
            "english_conversation_club",
            "영어회화 동아리",
            "영어 회화 실력 향상을 목표로 하는 동아리입니다. 원어민 선생님과 함께하는 weekly conversation class와 영어 토론 활동을 진행합니다.",
            22 // 22명 - 중앙동아리 등록 가능
        ));
        clubs.add(new ClubItem(
            "photography_club",
            "사진 동아리",
            "사진 촬영 기술과 편집을 배우고 함께 출사를 다니는 동아리입니다. 매달 주제를 정해 작품 활동을 하며, 연말에는 사진전을 개최합니다.",
            12 // 12명 - 8명 더 필요
        ));
        clubs.add(new ClubItem(
            "volunteer_club",
            "봉사 동아리",
            "지역사회 봉사활동을 중심으로 활동하는 동아리입니다. 매주 노인복지관, 아동센터 등에서 봉사활동을 진행하며, 연 2회 해외 봉사활동도 참여합니다.",
            15 // 15명 - 5명 더 필요
        ));
        clubs.add(new ClubItem(
            "band_club",
            "밴드 동아리",
            "음악을 사랑하는 학생들이 모여 밴드를 구성하고 공연하는 동아리입니다. 학기당 2회 정기공연을 개최하며, 교내외 행사에도 참여합니다.",
            8 // 8명 - 12명 더 필요
        ));
        clubs.add(new ClubItem(
            "startup_club",
            "창업 동아리",
            "아이디어를 사업화하고 창업을 준비하는 동아리입니다. 창업 교육, 멘토링, 팀 프로젝트를 진행하며 실제 창업 경진대회에도 참여합니다.",
            25 // 25명 - 중앙동아리 등록 가능
        ));

        // Add accordion items to container, filtering out joined clubs
        for (ClubItem club : clubs) {
            // 사용자가 이미 가입한 동아리는 숨기기
            if (currentUser != null && currentUser.hasJoinedGeneralClub(club.id)) {
                continue; // Skip this club
            }
            addAccordionItem(club);
        }
    }

    private void addAccordionItem(ClubItem club) {
        View accordionView = LayoutInflater.from(this)
            .inflate(R.layout.item_club_accordion, llClubListContainer, false);

        // Find views
        LinearLayout accordionHeader = accordionView.findViewById(R.id.accordionHeader);
        LinearLayout accordionContent = accordionView.findViewById(R.id.accordionContent);
        View divider = accordionView.findViewById(R.id.divider);
        TextView tvClubName = accordionView.findViewById(R.id.tvClubName);
        TextView tvClubDescription = accordionView.findViewById(R.id.tvClubDescription);
        ImageView ivExpandIcon = accordionView.findViewById(R.id.ivExpandIcon);
        MaterialButton btnJoinClub = accordionView.findViewById(R.id.btnJoinClub);

        // Member progress views
        TextView tvMemberCountText = accordionView.findViewById(R.id.tvMemberCountText);
        View viewMemberProgressBar = accordionView.findViewById(R.id.viewMemberProgressBar);
        TextView tvMemberProgressPercent = accordionView.findViewById(R.id.tvMemberProgressPercent);
        TextView tvMemberStatusMessage = accordionView.findViewById(R.id.tvMemberStatusMessage);

        // Set data
        tvClubName.setText(club.name);
        tvClubDescription.setText(club.description);

        // Set member count progress
        updateMemberProgressUI(club.memberCount, tvMemberCountText, viewMemberProgressBar,
            tvMemberProgressPercent, tvMemberStatusMessage);

        // Set click listener for accordion expansion
        accordionHeader.setOnClickListener(v -> {
            boolean isExpanded = accordionContent.getVisibility() == View.VISIBLE;

            if (isExpanded) {
                // Collapse
                accordionContent.setVisibility(View.GONE);
                divider.setVisibility(View.GONE);
                ivExpandIcon.setRotation(0);
            } else {
                // Expand
                accordionContent.setVisibility(View.VISIBLE);
                divider.setVisibility(View.VISIBLE);
                ivExpandIcon.setRotation(180);
            }
        });

        // Set click listener for join button
        btnJoinClub.setOnClickListener(v -> {
            Intent intent = new Intent(ClubListActivity.this, DetailActivity.class);
            intent.putExtra("club_id", club.id);
            intent.putExtra("club_name", club.name);
            intent.putExtra("from_club_list", true);
            startActivity(intent);
        });

        // Add to container
        llClubListContainer.addView(accordionView);
    }

    private void updateMemberProgressUI(int memberCount, TextView tvMemberCountText,
            View viewMemberProgressBar, TextView tvMemberProgressPercent,
            TextView tvMemberStatusMessage) {

        // 인원 텍스트 설정
        tvMemberCountText.setText(memberCount + "/" + CENTRAL_CLUB_MIN_MEMBERS + "명");

        // 퍼센트 계산 (최대 100%)
        int percent = memberCount >= CENTRAL_CLUB_MIN_MEMBERS ? 100 :
            (memberCount * 100 / CENTRAL_CLUB_MIN_MEMBERS);
        tvMemberProgressPercent.setText(percent + "%");

        // 프로그레스바 너비 설정
        viewMemberProgressBar.post(() -> {
            int parentWidth = ((View) viewMemberProgressBar.getParent()).getWidth();
            int progressWidth = (int) (parentWidth * percent / 100.0f);
            android.view.ViewGroup.LayoutParams params = viewMemberProgressBar.getLayoutParams();
            params.width = progressWidth;
            viewMemberProgressBar.setLayoutParams(params);

            // 색상 설정
            if (percent >= 100) {
                viewMemberProgressBar.setBackgroundResource(R.drawable.member_progress_fill);
            } else if (percent >= 75) {
                viewMemberProgressBar.setBackgroundResource(R.drawable.member_progress_fill_warning);
            } else {
                viewMemberProgressBar.setBackgroundResource(R.drawable.member_progress_fill_danger);
            }
        });

        // 상태 메시지 설정
        if (memberCount >= CENTRAL_CLUB_MIN_MEMBERS) {
            tvMemberStatusMessage.setText("중앙동아리 등록 가능!");
            tvMemberStatusMessage.setTextColor(
                androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_green_dark));
        } else {
            int needed = CENTRAL_CLUB_MIN_MEMBERS - memberCount;
            tvMemberStatusMessage.setText(needed + "명 더 모집 시 중앙동아리 등록 가능");
            tvMemberStatusMessage.setTextColor(
                androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_orange_dark));
        }
    }
}
