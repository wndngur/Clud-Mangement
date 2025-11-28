package com.example.clubmanagement.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.clubmanagement.MainActivityNew;
import com.example.clubmanagement.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

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

    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_club_recommend);

        initViews();
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

        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                // 홈으로 이동 - 중앙동아리 가입 여부에 따라 다른 화면으로 이동
                navigateToHome();
                return true;
            } else if (itemId == R.id.nav_clubs) {
                // 일반 동아리 페이지로 이동
                Intent intent = new Intent(ClubRecommendActivity.this, ClubListActivity.class);
                startActivity(intent);
                finish(); // 현재 페이지 종료
                return true;
            } else if (itemId == R.id.nav_recommend) {
                // 현재 페이지 - 아무 동작 안함
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
