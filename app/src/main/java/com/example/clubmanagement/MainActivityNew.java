package com.example.clubmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.viewpager2.widget.ViewPager2;

import com.example.clubmanagement.adapters.CarouselAdapter;
import com.example.clubmanagement.models.CarouselItem;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivityNew extends BaseActivity {

    private ViewPager2 viewPager;
    private CarouselAdapter carouselAdapter;
    private BottomNavigationView bottomNavigation;
    private MaterialButton btnDetailView;
    private TextView tvPageCounter;
    private TextView tvAdminModeIndicator;
    private ProgressBar progressBar;

    private FirebaseManager firebaseManager;
    private int totalPages = 3;
    private List<CarouselItem> currentCarouselItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ActionBar 완전히 숨기기
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_main_new);

        firebaseManager = FirebaseManager.getInstance();

        // 중앙동아리 가입 여부 확인
        checkCentralClubMembership();
    }

    private void checkCentralClubMembership() {
        // ProgressBar 먼저 찾기
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(ProgressBar.VISIBLE);

        // 로그인되지 않은 경우 바로 메인 화면 표시
        if (firebaseManager.getCurrentUserId() == null) {
            progressBar.setVisibility(ProgressBar.GONE);
            initViews();
            loadCarouselData();
            setupListeners();
            return;
        }

        // 먼저 승인된 가입 신청이 있는지 확인
        checkApprovedMembershipAndRedirect();
    }

    private void checkApprovedMembershipAndRedirect() {
        firebaseManager.checkApprovedMembershipApplication(application -> {
            if (application != null) {
                // 승인된 가입 신청이 있음 - 토스트 표시 후 동아리로 이동
                String clubName = (String) application.get("clubName");
                String clubId = (String) application.get("clubId");
                String applicationPath = (String) application.get("applicationDocPath");
                Boolean isCentralClub = (Boolean) application.get("isCentralClub");

                // 알림 확인 완료 표시
                if (applicationPath != null) {
                    firebaseManager.markMembershipApplicationNotified(applicationPath, new FirebaseManager.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            // 성공
                        }

                        @Override
                        public void onFailure(Exception e) {
                            // 실패해도 무시
                        }
                    });
                }

                progressBar.setVisibility(ProgressBar.GONE);

                // 토스트 메시지 표시
                String message = clubName + " 동아리 가입이 완료되었습니다!";
                Toast.makeText(MainActivityNew.this, message, Toast.LENGTH_LONG).show();

                // 동아리 메인 페이지로 이동
                Intent intent = new Intent(MainActivityNew.this,
                        com.example.clubmanagement.activities.ClubMainActivity.class);
                intent.putExtra("club_name", clubName);
                intent.putExtra("club_id", clubId);
                intent.putExtra("isCentralClub", isCentralClub != null ? isCentralClub : false);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                // 승인된 가입 신청이 없음 - 기존 로직 실행
                checkExistingMembership();
            }
        });
    }

    private void checkExistingMembership() {
        // 중앙동아리 가입 여부와 관계없이 캐러셀 화면 표시
        // (다른 중앙동아리 정보 확인 가능)
        progressBar.setVisibility(ProgressBar.GONE);
        initViews();
        loadCarouselData();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 로그인된 경우에만 확인
        if (firebaseManager.getCurrentUserId() != null) {
            // 먼저 승인된 가입 신청이 있는지 확인
            checkApprovedMembershipOnResume();
        } else {
            // 관리자 모드 표시 업데이트 (설정에서 돌아올 때를 위해)
            updateAdminModeIndicator();

            // 최고 관리자에게 동아리 추천 메뉴 숨기기 업데이트
            updateNavigationForSuperAdmin();
        }
    }

    private void checkApprovedMembershipOnResume() {
        firebaseManager.checkApprovedMembershipApplication(application -> {
            if (application != null) {
                // 승인된 가입 신청이 있음 - 토스트 표시 후 동아리로 이동
                String clubName = (String) application.get("clubName");
                String clubId = (String) application.get("clubId");
                String applicationPath = (String) application.get("applicationDocPath");
                Boolean isCentralClub = (Boolean) application.get("isCentralClub");

                // 알림 확인 완료 표시
                if (applicationPath != null) {
                    firebaseManager.markMembershipApplicationNotified(applicationPath, new FirebaseManager.SimpleCallback() {
                        @Override
                        public void onSuccess() {}

                        @Override
                        public void onFailure(Exception e) {}
                    });
                }

                // 토스트 메시지 표시
                String message = clubName + " 동아리 가입이 완료되었습니다!";
                Toast.makeText(MainActivityNew.this, message, Toast.LENGTH_LONG).show();

                // 동아리 메인 페이지로 이동
                Intent intent = new Intent(MainActivityNew.this,
                        com.example.clubmanagement.activities.ClubMainActivity.class);
                intent.putExtra("club_name", clubName);
                intent.putExtra("club_id", clubId);
                intent.putExtra("isCentralClub", isCentralClub != null ? isCentralClub : false);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                // 승인된 가입 신청이 없음 - 기존 로직 실행
                checkCentralClubMembershipAndRedirect();

                // 관리자 모드 표시 업데이트
                updateAdminModeIndicator();

                // 최고 관리자에게 동아리 추천 메뉴 숨기기 업데이트
                updateNavigationForSuperAdmin();
            }
        });
    }

    private void checkCentralClubMembershipAndRedirect() {
        // 중앙동아리 가입 여부와 관계없이 캐러셀 화면 유지
        // (다른 중앙동아리 정보 확인 가능)
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_home);
        }
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        btnDetailView = findViewById(R.id.btnDetailView);
        tvPageCounter = findViewById(R.id.tvPageCounter);
        tvAdminModeIndicator = findViewById(R.id.tvAdminModeIndicator);
        progressBar = findViewById(R.id.progressBar);

        // 초기 페이지 카운터 설정
        updatePageCounter(0);

        // 관리자 모드 표시 업데이트
        updateAdminModeIndicator();

        // 최고 관리자에게 동아리 추천 메뉴 숨기기
        updateNavigationForSuperAdmin();
    }

    private void updateNavigationForSuperAdmin() {
        if (bottomNavigation != null) {
            boolean isSuperAdmin = SettingsActivity.isSuperAdminMode(this);
            bottomNavigation.getMenu().findItem(R.id.nav_recommend).setVisible(!isSuperAdmin);
        }
    }

    private void updateAdminModeIndicator() {
        if (tvAdminModeIndicator != null) {
            boolean isSuperAdmin = SettingsActivity.isSuperAdminMode(this);
            tvAdminModeIndicator.setVisibility(isSuperAdmin ? android.view.View.VISIBLE : android.view.View.GONE);
        }
    }

    private void loadCarouselData() {
        progressBar.setVisibility(ProgressBar.VISIBLE);

        firebaseManager.getCarouselItems(new FirebaseManager.CarouselListCallback() {
            @Override
            public void onSuccess(List<CarouselItem> items) {
                progressBar.setVisibility(ProgressBar.GONE);

                if (items != null && !items.isEmpty()) {
                    // Firebase 데이터에서 position이 0, 1, 2인 중앙 동아리 캐러셀만 필터링
                    List<CarouselItem> centralCarouselItems = new ArrayList<>();
                    for (CarouselItem item : items) {
                        // position이 0, 1, 2이고 유효한 데이터인 경우만 사용
                        if (item.getPosition() >= 0 && item.getPosition() <= 2
                            && item.getTitle() != null && !item.getTitle().isEmpty()) {
                            centralCarouselItems.add(item);
                        }
                    }

                    // 필터링된 결과가 있으면 사용, 없으면 빈 상태 표시
                    if (!centralCarouselItems.isEmpty()) {
                        // position 순서대로 정렬
                        centralCarouselItems.sort((a, b) -> Integer.compare(a.getPosition(), b.getPosition()));
                        setupCarousel(centralCarouselItems);
                        // 상세보기 버튼 표시
                        if (btnDetailView != null) {
                            btnDetailView.setVisibility(android.view.View.VISIBLE);
                        }
                    } else {
                        showEmptyCarouselState();
                    }
                } else {
                    // 데이터 없음 - 빈 상태 표시
                    showEmptyCarouselState();
                }
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(ProgressBar.GONE);
                // 오류 발생 시 빈 상태 표시
                showEmptyCarouselState();
            }
        });
    }

    private void showEmptyCarouselState() {
        // 빈 캐러셀 아이템 생성 (등록된 중앙동아리 없음 안내)
        List<CarouselItem> emptyItems = new ArrayList<>();
        CarouselItem emptyItem = new CarouselItem(
                0,  // 이미지 없음
                "등록된 중앙동아리가 없습니다",
                "중앙동아리가 승인되면\n여기에 표시됩니다"
        );
        emptyItems.add(emptyItem);
        setupCarousel(emptyItems);

        // 상세보기 버튼 숨김
        if (btnDetailView != null) {
            btnDetailView.setVisibility(android.view.View.GONE);
        }
    }

    private void setupCarousel(List<CarouselItem> items) {
        // Update total pages
        totalPages = items.size();

        // 현재 캐러셀 아이템 저장
        currentCarouselItems.clear();
        currentCarouselItems.addAll(items);

        // 어댑터 설정
        carouselAdapter = new CarouselAdapter(items);
        viewPager.setAdapter(carouselAdapter);

        // ViewPager2 오프스크린 페이지 설정 (양옆 카드가 살짝 보이게)
        viewPager.setOffscreenPageLimit(1);

        // 페이지 변경 리스너
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updatePageCounter(position);
            }
        });
    }

    private void setupListeners() {
        // 상세 보기 버튼 클릭
        btnDetailView.setOnClickListener(v -> {
            int currentPage = viewPager.getCurrentItem();
            Intent intent = new Intent(MainActivityNew.this,
                com.example.clubmanagement.activities.DetailActivity.class);
            intent.putExtra("page_index", currentPage);

            // 현재 캐러셀 아이템의 clubId와 clubName 전달
            if (currentPage < currentCarouselItems.size()) {
                CarouselItem currentItem = currentCarouselItems.get(currentPage);
                if (currentItem.getClubId() != null && !currentItem.getClubId().isEmpty()) {
                    intent.putExtra("club_id", currentItem.getClubId());
                }
                if (currentItem.getClubName() != null && !currentItem.getClubName().isEmpty()) {
                    intent.putExtra("club_name", currentItem.getClubName());
                } else if (currentItem.getTitle() != null) {
                    intent.putExtra("club_name", currentItem.getTitle());
                }
            }

            startActivity(intent);
        });

        // 하단 내비게이션 클릭
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                Toast.makeText(this, "홈", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.nav_clubs) {
                Intent intent = new Intent(MainActivityNew.this,
                        com.example.clubmanagement.activities.ClubListActivity.class);
                intent.putExtra("from_club_settings", false);  // 네비게이션 바 표시
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_recommend) {
                Intent intent = new Intent(MainActivityNew.this,
                        com.example.clubmanagement.activities.ClubRecommendActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_myinfo) {
                // 내정보 클릭 시 설정 화면으로 이동
                Intent intent = new Intent(MainActivityNew.this, SettingsActivity.class);
                startActivity(intent);
                return true;
            }

            return false;
        });
    }

    private void updatePageCounter(int position) {
        // N/N 형식으로 페이지 번호 표시
        String pageText = (position + 1) + "/" + totalPages;
        tvPageCounter.setText(pageText);
    }
}
