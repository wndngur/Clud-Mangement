package com.example.clubmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.clubmanagement.adapters.CarouselAdapter;
import com.example.clubmanagement.models.CarouselItem;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivityNew extends AppCompatActivity {

    private ViewPager2 viewPager;
    private CarouselAdapter carouselAdapter;
    private ImageView ivSettings;
    private BottomNavigationView bottomNavigation;
    private MaterialButton btnDetailView;
    private TextView tvPageCounter;
    private ProgressBar progressBar;

    private FirebaseManager firebaseManager;
    private int totalPages = 3;

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

        firebaseManager.getCurrentUser(new FirebaseManager.UserCallback() {
            @Override
            public void onSuccess(com.example.clubmanagement.models.User user) {
                progressBar.setVisibility(ProgressBar.GONE);

                if (user != null && user.hasJoinedCentralClub()) {
                    // 중앙동아리에 가입된 경우 - 동아리 페이지로 바로 이동
                    Intent intent = new Intent(MainActivityNew.this,
                            com.example.clubmanagement.activities.ClubMainActivity.class);
                    intent.putExtra("club_name", user.getCentralClubName());
                    intent.putExtra("club_id", user.getCentralClubId());
                    startActivity(intent);
                    finish();
                } else {
                    // 중앙동아리 미가입 - 메인 화면 표시
                    initViews();
                    loadCarouselData();
                    setupListeners();
                }
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(ProgressBar.GONE);
                // 오류 발생 시에도 메인 화면 표시
                initViews();
                loadCarouselData();
                setupListeners();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 중앙동아리 가입 여부를 다시 확인하여 가입된 경우 동아리 페이지로 리다이렉트
        checkCentralClubMembershipAndRedirect();
    }

    private void checkCentralClubMembershipAndRedirect() {
        // 로그인되지 않은 경우 스킵
        if (firebaseManager.getCurrentUserId() == null) {
            if (bottomNavigation != null) {
                bottomNavigation.setSelectedItemId(R.id.nav_home);
            }
            return;
        }

        firebaseManager.getCurrentUser(new FirebaseManager.UserCallback() {
            @Override
            public void onSuccess(com.example.clubmanagement.models.User user) {
                if (user != null && user.hasJoinedCentralClub()) {
                    // 중앙동아리에 가입된 경우 - 동아리 페이지로 바로 이동
                    Intent intent = new Intent(MainActivityNew.this,
                            com.example.clubmanagement.activities.ClubMainActivity.class);
                    intent.putExtra("club_name", user.getCentralClubName());
                    intent.putExtra("club_id", user.getCentralClubId());
                    // 뒤로가기 방지를 위해 태스크 스택 클리어
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    // 미가입 상태 - 홈 버튼 선택 상태로 설정
                    if (bottomNavigation != null) {
                        bottomNavigation.setSelectedItemId(R.id.nav_home);
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                // 오류 발생 시 홈 버튼 선택 상태로 설정
                if (bottomNavigation != null) {
                    bottomNavigation.setSelectedItemId(R.id.nav_home);
                }
            }
        });
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        ivSettings = findViewById(R.id.ivSettings);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        btnDetailView = findViewById(R.id.btnDetailView);
        tvPageCounter = findViewById(R.id.tvPageCounter);
        progressBar = findViewById(R.id.progressBar);

        // 초기 페이지 카운터 설정
        updatePageCounter(0);
    }

    private void loadCarouselData() {
        progressBar.setVisibility(ProgressBar.VISIBLE);

        firebaseManager.getCarouselItems(new FirebaseManager.CarouselListCallback() {
            @Override
            public void onSuccess(List<CarouselItem> items) {
                progressBar.setVisibility(ProgressBar.GONE);

                if (items != null && !items.isEmpty()) {
                    // Use Firebase data
                    setupCarousel(items);
                } else {
                    // Use default data
                    setupCarousel(getDefaultCarouselItems());
                }
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(ProgressBar.GONE);
                Toast.makeText(MainActivityNew.this, "데이터 로드 실패, 기본 데이터 사용", Toast.LENGTH_SHORT).show();
                // Use default data on error
                setupCarousel(getDefaultCarouselItems());
            }
        });
    }

    private List<CarouselItem> getDefaultCarouselItems() {
        List<CarouselItem> items = new ArrayList<>();

        // 첫 번째 캐러셀 (서명 시스템)
        CarouselItem item1 = new CarouselItem(
                R.drawable.carousel_image_1,  // drawable 폴더에 이미지 넣기
                "서명 시스템",
                "간편하게 디지털 서명을 생성하고\n문서에 자동으로 삽입하세요"
        );
        items.add(item1);

        // 두 번째 캐러셀 (문서 관리)
        CarouselItem item2 = new CarouselItem(
                R.drawable.carousel_image_2,  // drawable 폴더에 이미지 넣기
                "문서 관리",
                "클럽 활동 보고서부터 회의록까지\n한 곳에서 관리하세요"
        );
        items.add(item2);

        // 세 번째 캐러셀 (부원 관리)
        CarouselItem item3 = new CarouselItem(
                R.drawable.carousel_image_3,  // drawable 폴더에 이미지 넣기
                "부원 관리",
                "부원 정보와 서명 현황을\n실시간으로 확인하세요"
        );
        items.add(item3);

        return items;
    }

    private void setupCarousel(List<CarouselItem> items) {
        // Update total pages
        totalPages = items.size();

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
        // 톱니바퀴 아이콘 클릭 -> 설정 화면
        ivSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivityNew.this, SettingsActivity.class);
            startActivity(intent);
        });

        // 상세 보기 버튼 클릭
        btnDetailView.setOnClickListener(v -> {
            int currentPage = viewPager.getCurrentItem();
            Intent intent = new Intent(MainActivityNew.this,
                com.example.clubmanagement.activities.DetailActivity.class);
            intent.putExtra("page_index", currentPage);
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
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_recommend) {
                Intent intent = new Intent(MainActivityNew.this,
                        com.example.clubmanagement.activities.ClubRecommendActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_myinfo) {
                Toast.makeText(this, "내정보", Toast.LENGTH_SHORT).show();
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
