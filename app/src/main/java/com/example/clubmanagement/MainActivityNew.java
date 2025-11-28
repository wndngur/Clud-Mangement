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

        initViews();
        loadCarouselData();
        setupListeners();
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
        items.add(new CarouselItem(
                android.R.drawable.ic_menu_camera,
                "서명 시스템",
                "간편하게 디지털 서명을 생성하고\n문서에 자동으로 삽입하세요"
        ));
        items.add(new CarouselItem(
                android.R.drawable.ic_menu_gallery,
                "문서 관리",
                "클럽 활동 보고서부터 회의록까지\n한 곳에서 관리하세요"
        ));
        items.add(new CarouselItem(
                android.R.drawable.ic_menu_edit,
                "부원 관리",
                "부원 정보와 서명 현황을\n실시간으로 확인하세요"
        ));
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
                Toast.makeText(this, "학과 동아리 보기", Toast.LENGTH_SHORT).show();
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
