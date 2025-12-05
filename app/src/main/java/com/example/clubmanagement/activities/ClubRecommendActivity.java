package com.example.clubmanagement.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clubmanagement.AdminMainActivity;
import com.example.clubmanagement.MainActivityNew;
import com.example.clubmanagement.R;
import com.example.clubmanagement.SettingsActivity;
import com.example.clubmanagement.adapters.RecommendedClubAdapter;
import com.example.clubmanagement.models.Club;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClubRecommendActivity extends AppCompatActivity {

    // Checkboxes
    private CheckBox cbChristian;
    private CheckBox cbLively;
    private CheckBox cbQuiet;
    private CheckBox cbVolunteer;
    private CheckBox cbSports;
    private CheckBox cbOutdoor;
    private CheckBox cbCareer;
    private CheckBox cbAcademic;
    private CheckBox cbArt;

    // New UI components
    private MaterialButton btnRecommend;
    private LinearLayout resultsSection;
    private RecyclerView rvRecommendedClubs;
    private TextView tvNoResults;
    private ProgressBar progressBar;

    private BottomNavigationView bottomNavigation;

    private FirebaseManager firebaseManager;
    private RecommendedClubAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_club_recommend);

        firebaseManager = FirebaseManager.getInstance();

        initViews();
        setupRecyclerView();
        setupListeners();
        setupBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 화면으로 돌아올 때마다 현재 페이지 버튼 선택 상태로 설정
        bottomNavigation.setSelectedItemId(R.id.nav_recommend);
    }

    private void initViews() {
        cbChristian = findViewById(R.id.cbChristian);
        cbLively = findViewById(R.id.cbLively);
        cbQuiet = findViewById(R.id.cbQuiet);
        cbVolunteer = findViewById(R.id.cbVolunteer);
        cbSports = findViewById(R.id.cbSports);
        cbOutdoor = findViewById(R.id.cbOutdoor);
        cbCareer = findViewById(R.id.cbCareer);
        cbAcademic = findViewById(R.id.cbAcademic);
        cbArt = findViewById(R.id.cbArt);

        btnRecommend = findViewById(R.id.btnRecommend);
        resultsSection = findViewById(R.id.resultsSection);
        rvRecommendedClubs = findViewById(R.id.rvRecommendedClubs);
        tvNoResults = findViewById(R.id.tvNoResults);
        progressBar = findViewById(R.id.progressBar);

        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void setupRecyclerView() {
        adapter = new RecommendedClubAdapter(this, club -> {
            // 동아리 상세 페이지로 이동
            Intent intent = new Intent(this, ClubInfoActivity.class);
            intent.putExtra("club_id", club.getId());
            intent.putExtra("club_name", club.getName());
            intent.putExtra("from_club_list", true);  // 가입 신청 버튼 표시를 위해 추가
            startActivity(intent);
        });
        rvRecommendedClubs.setLayoutManager(new LinearLayoutManager(this));
        rvRecommendedClubs.setAdapter(adapter);
    }

    private void setupListeners() {
        btnRecommend.setOnClickListener(v -> performRecommendation());

        // 분위기 체크박스 - 하나만 선택 가능하게
        cbLively.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) cbQuiet.setChecked(false);
        });
        cbQuiet.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) cbLively.setChecked(false);
        });

        // 활동 유형 - 최대 2개 선택 제한
        CheckBox[] activityCheckboxes = {cbVolunteer, cbSports, cbOutdoor};
        for (CheckBox cb : activityCheckboxes) {
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && countChecked(activityCheckboxes) > 2) {
                    buttonView.setChecked(false);
                    Toast.makeText(this, "활동 유형은 최대 2개까지 선택 가능합니다", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // 목적 - 최대 2개 선택 제한
        CheckBox[] purposeCheckboxes = {cbCareer, cbAcademic, cbArt};
        for (CheckBox cb : purposeCheckboxes) {
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && countChecked(purposeCheckboxes) > 2) {
                    buttonView.setChecked(false);
                    Toast.makeText(this, "목적은 최대 2개까지 선택 가능합니다", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private int countChecked(CheckBox[] checkboxes) {
        int count = 0;
        for (CheckBox cb : checkboxes) {
            if (cb.isChecked()) count++;
        }
        return count;
    }

    private void performRecommendation() {
        // 선택 값 수집
        boolean wantChristian = cbChristian.isChecked();

        String wantAtmosphere = null;
        if (cbLively.isChecked()) wantAtmosphere = "lively";
        else if (cbQuiet.isChecked()) wantAtmosphere = "quiet";

        List<String> wantActivityTypes = new ArrayList<>();
        if (cbVolunteer.isChecked()) wantActivityTypes.add("volunteer");
        if (cbSports.isChecked()) wantActivityTypes.add("sports");
        if (cbOutdoor.isChecked()) wantActivityTypes.add("outdoor");

        List<String> wantPurposes = new ArrayList<>();
        if (cbCareer.isChecked()) wantPurposes.add("career");
        if (cbAcademic.isChecked()) wantPurposes.add("academic");
        if (cbArt.isChecked()) wantPurposes.add("art");

        // 아무것도 선택 안했으면 경고
        if (!wantChristian && wantAtmosphere == null &&
            wantActivityTypes.isEmpty() && wantPurposes.isEmpty()) {
            Toast.makeText(this, "최소 하나 이상의 조건을 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 로딩 표시
        progressBar.setVisibility(View.VISIBLE);
        resultsSection.setVisibility(View.GONE);

        Toast.makeText(this, "동아리를 검색 중...", Toast.LENGTH_SHORT).show();

        // Firebase에서 모든 동아리 조회
        final String finalWantAtmosphere = wantAtmosphere;
        final boolean finalWantChristian = wantChristian;
        final List<String> finalWantActivityTypes = wantActivityTypes;
        final List<String> finalWantPurposes = wantPurposes;

        firebaseManager.getAllClubs(new FirebaseManager.ClubListCallback() {
            @Override
            public void onSuccess(List<Club> clubs) {
                progressBar.setVisibility(View.GONE);

                // 점수 계산 및 정렬
                List<Club> matchedClubs = new ArrayList<>();
                List<Integer> matchedScores = new ArrayList<>();

                for (Club club : clubs) {
                    int score = club.calculateRecommendScore(
                            finalWantChristian, finalWantAtmosphere,
                            finalWantActivityTypes, finalWantPurposes);

                    // 점수가 있거나, 키워드가 설정된 동아리만 표시
                    if (score > 0) {
                        matchedClubs.add(club);
                        matchedScores.add(score);
                    } else if (club.hasKeywords()) {
                        // 키워드는 있지만 매칭 안 되는 경우도 낮은 점수로 표시
                        matchedClubs.add(club);
                        matchedScores.add(5); // 기본 점수
                    }
                }

                // 점수 기준 내림차순 정렬
                sortByScore(matchedClubs, matchedScores);

                // 결과 표시
                resultsSection.setVisibility(View.VISIBLE);
                if (matchedClubs.isEmpty()) {
                    tvNoResults.setVisibility(View.VISIBLE);
                    rvRecommendedClubs.setVisibility(View.GONE);
                    Toast.makeText(ClubRecommendActivity.this,
                        "키워드가 설정된 동아리가 없습니다.\n관리자가 동아리 키워드를 설정해야 합니다.",
                        Toast.LENGTH_LONG).show();
                } else {
                    tvNoResults.setVisibility(View.GONE);
                    rvRecommendedClubs.setVisibility(View.VISIBLE);
                    adapter.setClubs(matchedClubs, matchedScores);
                    Toast.makeText(ClubRecommendActivity.this,
                        matchedClubs.size() + "개의 동아리를 찾았습니다",
                        Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ClubRecommendActivity.this,
                    "동아리 목록을 불러오는데 실패했습니다: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            }
        });
    }

    private void sortByScore(List<Club> clubs, List<Integer> scores) {
        // 간단한 버블 정렬로 점수 내림차순 정렬
        for (int i = 0; i < scores.size() - 1; i++) {
            for (int j = 0; j < scores.size() - i - 1; j++) {
                if (scores.get(j) < scores.get(j + 1)) {
                    // 점수 교환
                    int tempScore = scores.get(j);
                    scores.set(j, scores.get(j + 1));
                    scores.set(j + 1, tempScore);

                    // 클럽도 교환
                    Club tempClub = clubs.get(j);
                    clubs.set(j, clubs.get(j + 1));
                    clubs.set(j + 1, tempClub);
                }
            }
        }
    }

    private void setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                // 홈으로 이동 - 중앙동아리 가입 여부에 따라 다른 화면으로 이동
                navigateToHome();
                return true;
            } else if (itemId == R.id.nav_chat) {
                Intent intent = new Intent(ClubRecommendActivity.this, ChatActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_clubs) {
                // 일반 동아리 페이지로 이동
                Intent intent = new Intent(ClubRecommendActivity.this, ClubListActivity.class);
                intent.putExtra("from_club_settings", false);  // 네비게이션 바 표시
                startActivity(intent);
                finish(); // 현재 페이지 종료
                return true;
            } else if (itemId == R.id.nav_recommend) {
                // 현재 페이지 - 아무 동작 안함
                return true;
            } else if (itemId == R.id.nav_myinfo) {
                // 내정보(설정) 화면으로 이동
                Intent intent = new Intent(ClubRecommendActivity.this, SettingsActivity.class);
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
            Intent intent = new Intent(ClubRecommendActivity.this, AdminMainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
            return;
        }

        com.example.clubmanagement.utils.FirebaseManager firebaseManager =
            com.example.clubmanagement.utils.FirebaseManager.getInstance();

        // 로그인되지 않은 경우 바로 MainActivityNew로 이동
        if (firebaseManager.getCurrentUserId() == null) {
            Intent intent = new Intent(ClubRecommendActivity.this,
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
                    intent = new Intent(ClubRecommendActivity.this,
                        com.example.clubmanagement.activities.ClubMainActivity.class);
                    intent.putExtra("club_name", user.getCentralClubName());
                    intent.putExtra("club_id", user.getCentralClubId());
                } else {
                    // 미가입 - MainActivityNew로 이동
                    intent = new Intent(ClubRecommendActivity.this,
                        com.example.clubmanagement.MainActivityNew.class);
                }
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                // 오류 발생 시 MainActivityNew로 이동
                Intent intent = new Intent(ClubRecommendActivity.this,
                    com.example.clubmanagement.MainActivityNew.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });
    }

}
