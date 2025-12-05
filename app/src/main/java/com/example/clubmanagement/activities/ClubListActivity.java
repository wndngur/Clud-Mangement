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

import com.example.clubmanagement.AdminMainActivity;
import com.example.clubmanagement.BaseActivity;
import com.example.clubmanagement.R;
import com.example.clubmanagement.SettingsActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ClubListActivity extends BaseActivity {

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
        Date foundedAt;  // 설립일
        boolean isCentralClub; // 중앙동아리 여부

        ClubItem(String id, String name, String description, int memberCount, Date foundedAt, boolean isCentralClub) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.memberCount = memberCount;
            this.foundedAt = foundedAt;
            this.isCentralClub = isCentralClub;
        }
    }

    // 중앙동아리 신청 가능 최소 일수 (6개월 = 180일)
    private static final int CENTRAL_CLUB_MIN_DAYS = 180;

    // Firebase에서 가져온 인원 제한 값 (기본값 설정)
    private int registerLimit = 20;
    private int maintainLimit = 15;

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
        loadMemberLimits();
    }

    private void loadMemberLimits() {
        com.example.clubmanagement.utils.FirebaseManager.getInstance()
            .getMemberLimits(new com.example.clubmanagement.utils.FirebaseManager.MemberLimitsCallback() {
                @Override
                public void onSuccess(int register, int maintain) {
                    registerLimit = register;
                    maintainLimit = maintain;
                    loadCurrentUser();
                }

                @Override
                public void onFailure(Exception e) {
                    // 실패 시 기본값 사용
                    loadCurrentUser();
                }
            });
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
        // 최고 관리자 모드일 때 추천 메뉴 숨기기
        updateNavigationForSuperAdmin();
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

            // 최고 관리자 모드일 때 추천 메뉴 숨기기
            updateNavigationForSuperAdmin();
        }
    }

    private void updateNavigationForSuperAdmin() {
        if (bottomNavigation != null && bottomNavigation.getVisibility() == View.VISIBLE) {
            boolean isSuperAdmin = SettingsActivity.isSuperAdminMode(this);
            bottomNavigation.getMenu().findItem(R.id.nav_recommend).setVisible(!isSuperAdmin);
        }
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                // 홈으로 이동 - 중앙동아리 가입 여부에 따라 다른 화면으로 이동
                navigateToHome();
                return true;
            } else if (itemId == R.id.nav_chat) {
                Intent intent = new Intent(ClubListActivity.this, ChatActivity.class);
                startActivity(intent);
                finish();
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
                // 내정보(설정) 화면으로 이동
                Intent intent = new Intent(ClubListActivity.this, SettingsActivity.class);
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
            Intent intent = new Intent(ClubListActivity.this, AdminMainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
            return;
        }

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

    private Date getDateDaysAgo(int daysAgo) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo);
        return cal.getTime();
    }

    private void loadClubList() {
        progressBar.setVisibility(View.VISIBLE);
        llClubListContainer.removeAllViews();

        // Firebase에서 일반동아리 목록 가져오기 (isCentralClub = false)
        com.example.clubmanagement.utils.FirebaseManager.getInstance()
            .getAllClubs(new com.example.clubmanagement.utils.FirebaseManager.ClubListCallback() {
                @Override
                public void onSuccess(java.util.List<com.example.clubmanagement.models.Club> clubs) {
                    progressBar.setVisibility(View.GONE);

                    if (clubs.isEmpty()) {
                        // 동아리가 없을 때 안내 메시지 표시
                        TextView tvEmpty = new TextView(ClubListActivity.this);
                        tvEmpty.setText("등록된 일반동아리가 없습니다.");
                        tvEmpty.setTextSize(16);
                        tvEmpty.setTextColor(getResources().getColor(android.R.color.darker_gray));
                        tvEmpty.setPadding(32, 64, 32, 64);
                        tvEmpty.setGravity(android.view.Gravity.CENTER);
                        llClubListContainer.addView(tvEmpty);
                        return;
                    }

                    // 일반동아리만 필터링 (중앙동아리 제외)
                    int clubCount = 0;
                    for (com.example.clubmanagement.models.Club club : clubs) {
                        // 중앙동아리는 제외
                        if (club.isCentralClub()) {
                            continue;
                        }

                        // 사용자가 이미 가입한 동아리는 숨기기
                        if (currentUser != null && currentUser.hasJoinedGeneralClub(club.getId())) {
                            continue;
                        }

                        clubCount++;

                        // ClubItem으로 변환
                        Date foundedAt = club.getFoundedAt() != null ?
                            club.getFoundedAt().toDate() : null;

                        ClubItem clubItem = new ClubItem(
                            club.getId(),
                            club.getName(),
                            club.getDescription() != null ? club.getDescription() : "",
                            club.getMemberCount(),
                            foundedAt,
                            club.isCentralClub()
                        );

                        // 실제 멤버 수를 Firebase에서 로드하여 아코디언 추가
                        loadMemberCountAndAddItem(clubItem);
                    }

                    // 표시된 동아리가 없는 경우
                    if (clubCount == 0) {
                        TextView tvEmpty = new TextView(ClubListActivity.this);
                        tvEmpty.setText("가입 가능한 일반동아리가 없습니다.");
                        tvEmpty.setTextSize(16);
                        tvEmpty.setTextColor(getResources().getColor(android.R.color.darker_gray));
                        tvEmpty.setPadding(32, 64, 32, 64);
                        tvEmpty.setGravity(android.view.Gravity.CENTER);
                        llClubListContainer.addView(tvEmpty);
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ClubListActivity.this,
                        "동아리 목록을 불러오는데 실패했습니다: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void loadMemberCountAndAddItem(ClubItem clubItem) {
        // members 서브컬렉션에서 실제 멤버 수 카운트
        com.example.clubmanagement.utils.FirebaseManager.getInstance()
            .getDb().collection("clubs")
            .document(clubItem.id)
            .collection("members")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                // 실제 멤버 수로 업데이트
                clubItem.memberCount = querySnapshot.size();
                addAccordionItem(clubItem);
            })
            .addOnFailureListener(e -> {
                // 실패해도 기존 값으로 표시
                addAccordionItem(clubItem);
            });
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
        TextView tvMemberSectionTitle = accordionView.findViewById(R.id.tvMemberSectionTitle);
        TextView tvMemberCountText = accordionView.findViewById(R.id.tvMemberCountText);
        View viewMemberProgressBar = accordionView.findViewById(R.id.viewMemberProgressBar);
        TextView tvMemberProgressPercent = accordionView.findViewById(R.id.tvMemberProgressPercent);
        TextView tvMemberStatusMessage = accordionView.findViewById(R.id.tvMemberStatusMessage);

        // 중앙동아리/일반동아리 타이틀 설정
        if (club.isCentralClub) {
            tvMemberSectionTitle.setText("👥 중앙동아리 인원 현황");
        } else {
            tvMemberSectionTitle.setText("👥 일반동아리 인원 현황");
        }

        // Founding date views
        TextView tvFoundingDateText = accordionView.findViewById(R.id.tvFoundingDateText);
        View viewFoundingProgressBar = accordionView.findViewById(R.id.viewFoundingProgressBar);
        TextView tvFoundingProgressPercent = accordionView.findViewById(R.id.tvFoundingProgressPercent);
        TextView tvFoundingStatusMessage = accordionView.findViewById(R.id.tvFoundingStatusMessage);

        // Set data
        tvClubName.setText(club.name);
        tvClubDescription.setText(club.description);

        // Set founding date progress
        updateFoundingProgressUI(club.foundedAt, tvFoundingDateText, viewFoundingProgressBar,
            tvFoundingProgressPercent, tvFoundingStatusMessage);

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

        // Set click listener for detail view button
        btnJoinClub.setOnClickListener(v -> {
            Intent intent = new Intent(ClubListActivity.this, DetailActivity.class);
            intent.putExtra("club_id", club.id);
            intent.putExtra("club_name", club.name);
            intent.putExtra("from_club_list", true);  // 일반동아리 목록에서 왔음
            startActivity(intent);
        });

        // Add to container
        llClubListContainer.addView(accordionView);
    }

    private void updateFoundingProgressUI(Date foundedAt, TextView tvFoundingDateText,
            View viewFoundingProgressBar, TextView tvFoundingProgressPercent,
            TextView tvFoundingStatusMessage) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd", Locale.KOREA);

        if (foundedAt == null) {
            tvFoundingDateText.setText("미설정");
            tvFoundingProgressPercent.setText("0%");
            tvFoundingStatusMessage.setText("설립일 정보 없음");
            tvFoundingStatusMessage.setTextColor(
                androidx.core.content.ContextCompat.getColor(this, android.R.color.darker_gray));
            return;
        }

        // 경과 일수 계산
        long diffMillis = System.currentTimeMillis() - foundedAt.getTime();
        long daysSinceFounding = diffMillis / (1000 * 60 * 60 * 24);

        // 설립일 텍스트 설정
        tvFoundingDateText.setText(sdf.format(foundedAt) + " (" + daysSinceFounding + "일)");

        // 퍼센트 계산 (180일 기준, 최대 100%)
        int percent = daysSinceFounding >= CENTRAL_CLUB_MIN_DAYS ? 100 :
            (int) ((daysSinceFounding * 100) / CENTRAL_CLUB_MIN_DAYS);
        tvFoundingProgressPercent.setText(percent + "%");

        // 프로그레스바 너비 설정
        viewFoundingProgressBar.post(() -> {
            int parentWidth = ((View) viewFoundingProgressBar.getParent()).getWidth();
            int progressWidth = (int) (parentWidth * percent / 100.0f);
            android.view.ViewGroup.LayoutParams params = viewFoundingProgressBar.getLayoutParams();
            params.width = progressWidth;
            viewFoundingProgressBar.setLayoutParams(params);
        });

        // 상태 메시지 설정 (180일 이상이면 중앙동아리 신청 가능)
        if (daysSinceFounding >= CENTRAL_CLUB_MIN_DAYS) {
            tvFoundingStatusMessage.setText("중앙동아리 신청 가능 (6개월 이상 경과)");
            tvFoundingStatusMessage.setTextColor(
                androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_green_dark));
        } else {
            long daysNeeded = CENTRAL_CLUB_MIN_DAYS - daysSinceFounding;
            tvFoundingStatusMessage.setText(daysNeeded + "일 후 중앙동아리 신청 가능");
            tvFoundingStatusMessage.setTextColor(
                androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_orange_dark));
        }
    }

    private void updateMemberProgressUI(int memberCount, TextView tvMemberCountText,
            View viewMemberProgressBar, TextView tvMemberProgressPercent,
            TextView tvMemberStatusMessage) {

        // 인원 텍스트 설정 (일반동아리는 20명 기준)
        tvMemberCountText.setText(memberCount + "/" + registerLimit + "명");

        // 퍼센트 계산 (최대 100%)
        int percent = memberCount >= registerLimit ? 100 :
            (memberCount * 100 / registerLimit);
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

        // 상태 메시지 설정 (20명 이상이면 중앙동아리 등록 가능)
        if (memberCount >= registerLimit) {
            tvMemberStatusMessage.setText("중앙동아리 등록 가능!");
            tvMemberStatusMessage.setTextColor(
                androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_green_dark));
        } else {
            int needed = registerLimit - memberCount;
            tvMemberStatusMessage.setText(needed + "명 더 모집 시 중앙동아리 등록 가능");
            tvMemberStatusMessage.setTextColor(
                androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_orange_dark));
        }
    }
}
