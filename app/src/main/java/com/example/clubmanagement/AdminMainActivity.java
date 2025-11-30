package com.example.clubmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.clubmanagement.activities.ClubListActivity;
import com.example.clubmanagement.activities.ClubMainActivity;
import com.example.clubmanagement.activities.ClubRecommendActivity;
import com.example.clubmanagement.activities.DetailActivity;
import com.example.clubmanagement.adapters.CarouselAdapter;
import com.example.clubmanagement.models.CarouselItem;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class AdminMainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private CarouselAdapter carouselAdapter;
    private BottomNavigationView bottomNavigation;
    private MaterialButton btnGoToClub;
    private TextView tvPageCounter;
    private ProgressBar progressBar;

    private FirebaseManager firebaseManager;
    private int totalPages = 3;

    // 현재 선택된 동아리 정보
    private List<CarouselItem> carouselItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ActionBar 숨기기
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_admin_main);

        firebaseManager = FirebaseManager.getInstance();

        initViews();
        loadCarouselData();
        setupListeners();
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        btnGoToClub = findViewById(R.id.btnGoToClub);
        tvPageCounter = findViewById(R.id.tvPageCounter);
        progressBar = findViewById(R.id.progressBar);

        // 초기 페이지 카운터 설정
        updatePageCounter(0);

        // 최고 관리자 모드에서 추천 메뉴 숨기기
        updateNavigationForSuperAdmin();
    }

    private void updateNavigationForSuperAdmin() {
        if (bottomNavigation != null) {
            boolean isSuperAdmin = SettingsActivity.isSuperAdminMode(this);
            bottomNavigation.getMenu().findItem(R.id.nav_recommend).setVisible(!isSuperAdmin);
        }
    }

    private void loadCarouselData() {
        progressBar.setVisibility(ProgressBar.VISIBLE);

        firebaseManager.getCarouselItems(new FirebaseManager.CarouselListCallback() {
            @Override
            public void onSuccess(List<CarouselItem> items) {
                progressBar.setVisibility(ProgressBar.GONE);

                if (items != null && !items.isEmpty()) {
                    carouselItems = items;
                    setupCarousel(items);
                } else {
                    carouselItems = getDefaultCarouselItems();
                    setupCarousel(carouselItems);
                }
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(ProgressBar.GONE);
                Toast.makeText(AdminMainActivity.this, "데이터 로드 실패, 기본 데이터 사용", Toast.LENGTH_SHORT).show();
                carouselItems = getDefaultCarouselItems();
                setupCarousel(carouselItems);
            }
        });
    }

    private List<CarouselItem> getDefaultCarouselItems() {
        List<CarouselItem> items = new ArrayList<>();

        CarouselItem item1 = new CarouselItem(
                R.drawable.carousel_image_1,
                "서명 시스템",
                "간편하게 디지털 서명을 생성하고\n문서에 자동으로 삽입하세요"
        );
        item1.setClubId("club_signature");
        item1.setClubName("서명 시스템");
        items.add(item1);

        CarouselItem item2 = new CarouselItem(
                R.drawable.carousel_image_2,
                "문서 관리",
                "클럽 활동 보고서부터 회의록까지\n한 곳에서 관리하세요"
        );
        item2.setClubId("club_document");
        item2.setClubName("문서 관리");
        items.add(item2);

        CarouselItem item3 = new CarouselItem(
                R.drawable.carousel_image_3,
                "부원 관리",
                "부원 정보와 서명 현황을\n실시간으로 확인하세요"
        );
        item3.setClubId("club_member");
        item3.setClubName("부원 관리");
        items.add(item3);

        return items;
    }

    private void setupCarousel(List<CarouselItem> items) {
        totalPages = items.size();

        carouselAdapter = new CarouselAdapter(items);
        viewPager.setAdapter(carouselAdapter);
        viewPager.setOffscreenPageLimit(1);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updatePageCounter(position);
            }
        });
    }

    private void setupListeners() {
        // 동아리 관리하기 버튼 클릭 -> 동아리 상세 정보 페이지로 이동
        btnGoToClub.setOnClickListener(v -> {
            int currentPage = viewPager.getCurrentItem();

            if (carouselItems != null && currentPage < carouselItems.size()) {
                CarouselItem currentItem = carouselItems.get(currentPage);

                String clubId = currentItem.getClubId();
                String clubName = currentItem.getClubName();

                // clubId가 없으면 title을 사용
                if (clubId == null || clubId.isEmpty()) {
                    clubId = "club_" + currentPage;
                }
                if (clubName == null || clubName.isEmpty()) {
                    clubName = currentItem.getTitle();
                }

                // 동아리 상세 정보 페이지로 이동 (관리자 모드)
                Intent intent = new Intent(AdminMainActivity.this, DetailActivity.class);
                intent.putExtra("page_index", currentPage);
                intent.putExtra("club_id", clubId);
                intent.putExtra("club_name", clubName);
                intent.putExtra("is_admin", true);
                startActivity(intent);
            } else {
                Toast.makeText(this, "동아리 정보를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
            }
        });

        // 하단 내비게이션 클릭
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                // 현재 화면이므로 아무것도 하지 않음
                return true;
            } else if (itemId == R.id.nav_clubs) {
                Intent intent = new Intent(AdminMainActivity.this, ClubListActivity.class);
                intent.putExtra("from_club_settings", false);  // 네비게이션 바 표시
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_recommend) {
                Intent intent = new Intent(AdminMainActivity.this, ClubRecommendActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_myinfo) {
                Intent intent = new Intent(AdminMainActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;
            }

            return false;
        });
    }

    private void updatePageCounter(int position) {
        String pageText = (position + 1) + "/" + totalPages;
        tvPageCounter.setText(pageText);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 홈 탭 선택 상태로 설정
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_home);
        }
        // 최고 관리자 모드에서 추천 메뉴 숨기기 업데이트
        updateNavigationForSuperAdmin();
    }
}
