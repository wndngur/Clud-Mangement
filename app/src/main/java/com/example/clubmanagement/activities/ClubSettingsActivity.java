package com.example.clubmanagement.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.clubmanagement.R;
import com.example.clubmanagement.models.UserData;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.android.material.button.MaterialButton;

public class ClubSettingsActivity extends AppCompatActivity {

    private FirebaseManager firebaseManager;
    private MaterialButton btnClubAdmin;
    private MaterialButton btnLogoutAdmin;
    private MaterialButton btnMyClubs;
    private MaterialButton btnGeneralClubs;
    private TextView tvAdminStatus;
    private TextView tvClubName;
    private ProgressBar progressBar;
    private UserData currentUserData;
    private String clubName;
    private String clubId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_club_settings);

        firebaseManager = FirebaseManager.getInstance();

        // Get club name and ID from intent
        clubName = getIntent().getStringExtra("club_name");
        clubId = getIntent().getStringExtra("club_id");

        if (clubName == null) {
            clubName = "동아리";
        }
        if (clubId == null) {
            clubId = clubName.replaceAll("\\s+", "_").toLowerCase();
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(clubName + " 설정");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());

        initViews();
        loadAdminStatus();
        setupListeners();
    }

    private void initViews() {
        btnClubAdmin = findViewById(R.id.btnClubAdmin);
        btnLogoutAdmin = findViewById(R.id.btnLogoutAdmin);
        btnMyClubs = findViewById(R.id.btnMyClubs);
        btnGeneralClubs = findViewById(R.id.btnGeneralClubs);
        tvAdminStatus = findViewById(R.id.tvAdminStatus);
        tvClubName = findViewById(R.id.tvClubName);
        progressBar = findViewById(R.id.progressBarSettings);

        tvClubName.setText(clubName);
    }

    private void loadAdminStatus() {
        progressBar.setVisibility(ProgressBar.VISIBLE);

        firebaseManager.getUserData(firebaseManager.getCurrentUserId(), new FirebaseManager.UserDataCallback() {
            @Override
            public void onSuccess(UserData userData) {
                progressBar.setVisibility(ProgressBar.GONE);
                currentUserData = userData;
                updateAdminStatusDisplay();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(ProgressBar.GONE);
                Toast.makeText(ClubSettingsActivity.this, "상태 확인 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        btnClubAdmin.setOnClickListener(v -> showPasswordDialog());
        btnLogoutAdmin.setOnClickListener(v -> logoutAdmin());
        btnMyClubs.setOnClickListener(v -> openMyClubs());
        btnGeneralClubs.setOnClickListener(v -> openGeneralClubsList());
    }

    private void openMyClubs() {
        Intent intent = new Intent(ClubSettingsActivity.this, MyClubsActivity.class);
        startActivity(intent);
    }

    private void openGeneralClubsList() {
        Intent intent = new Intent(ClubSettingsActivity.this, ClubListActivity.class);
        intent.putExtra("from_club_settings", true); // 중앙동아리 설정에서 왔다는 플래그
        startActivity(intent);
    }

    private void showPasswordDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_admin_password, null);
        EditText etPassword = dialogView.findViewById(R.id.etAdminPassword);

        new AlertDialog.Builder(this)
                .setTitle("동아리 관리자 인증")
                .setMessage("동아리 관리자 비밀번호를 입력하세요\n(기본값: clubadmin123)")
                .setView(dialogView)
                .setPositiveButton("인증", (dialog, which) -> {
                    String password = etPassword.getText().toString().trim();
                    if (password.isEmpty()) {
                        Toast.makeText(this, "비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show();
                    } else {
                        verifyAndSetAdmin(password);
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void verifyAndSetAdmin(String password) {
        progressBar.setVisibility(ProgressBar.VISIBLE);

        firebaseManager.verifyAdminPassword("CLUB_ADMIN", clubId, password, new FirebaseManager.PasswordVerifyCallback() {
            @Override
            public void onSuccess(boolean isValid, String level, String verifiedClubId) {
                if (isValid) {
                    setUserAdminLevel(level, verifiedClubId);
                } else {
                    progressBar.setVisibility(ProgressBar.GONE);
                    Toast.makeText(ClubSettingsActivity.this, "비밀번호가 일치하지 않습니다", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(ProgressBar.GONE);
                Toast.makeText(ClubSettingsActivity.this, "인증 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setUserAdminLevel(String adminLevel, String verifiedClubId) {
        String userId = firebaseManager.getCurrentUserId();

        firebaseManager.setUserAdminLevel(userId, adminLevel, verifiedClubId, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(ProgressBar.GONE);
                Toast.makeText(ClubSettingsActivity.this, "동아리 관리자 권한이 활성화되었습니다", Toast.LENGTH_SHORT).show();
                loadAdminStatus();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(ProgressBar.GONE);
                Toast.makeText(ClubSettingsActivity.this, "권한 설정 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void logoutAdmin() {
        new AlertDialog.Builder(this)
                .setTitle("관리자 로그아웃")
                .setMessage("관리자 권한을 해제하시겠습니까?")
                .setPositiveButton("해제", (dialog, which) -> {
                    String userId = firebaseManager.getCurrentUserId();
                    firebaseManager.setUserAdminLevel(userId, "NONE", null, new FirebaseManager.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(ClubSettingsActivity.this, "관리자 권한이 해제되었습니다", Toast.LENGTH_SHORT).show();
                            loadAdminStatus();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(ClubSettingsActivity.this, "권한 해제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void updateAdminStatusDisplay() {
        if (currentUserData == null || currentUserData.getAdminLevel() == null || currentUserData.getAdminLevel().equals("NONE")) {
            tvAdminStatus.setText("현재 상태: 일반 사용자");
            btnLogoutAdmin.setVisibility(View.GONE);
        } else if (currentUserData.isSuperAdmin()) {
            tvAdminStatus.setText("현재 상태: 최고 관리자\n\n모든 동아리를 관리할 수 있습니다.");
            btnLogoutAdmin.setVisibility(View.VISIBLE);
        } else if (currentUserData.isClubAdmin()) {
            String managingClubId = currentUserData.getClubId() != null ? currentUserData.getClubId() : "미지정";
            boolean isThisClub = managingClubId.equals(clubId);

            if (isThisClub) {
                tvAdminStatus.setText("현재 상태: 동아리 관리자\n\n이 동아리를 관리할 수 있습니다.");
            } else {
                tvAdminStatus.setText("현재 상태: 동아리 관리자\n관리 동아리: " + managingClubId + "\n\n다른 동아리의 관리자입니다.");
            }
            btnLogoutAdmin.setVisibility(View.VISIBLE);
        }
    }
}
