package com.example.clubmanagement;

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

import com.example.clubmanagement.models.AdminLevel;
import com.example.clubmanagement.models.UserData;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.android.material.button.MaterialButton;

public class SettingsActivity extends AppCompatActivity {

    private FirebaseManager firebaseManager;
    private MaterialButton btnSuperAdmin;
    private MaterialButton btnLogoutAdmin;
    private TextView tvAdminStatus;
    private ProgressBar progressBar;
    private UserData currentUserData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        firebaseManager = FirebaseManager.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 뒤로가기 버튼 활성화
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());

        initViews();
        loadAdminStatus();
        setupListeners();
    }

    private void initViews() {
        btnSuperAdmin = findViewById(R.id.btnSuperAdmin);
        btnLogoutAdmin = findViewById(R.id.btnLogoutAdmin);
        tvAdminStatus = findViewById(R.id.tvAdminStatus);
        progressBar = findViewById(R.id.progressBarSettings);
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
                Toast.makeText(SettingsActivity.this, "상태 확인 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        btnSuperAdmin.setOnClickListener(v -> showPasswordDialog("SUPER_ADMIN", null));
        btnLogoutAdmin.setOnClickListener(v -> logoutAdmin());
    }

    private void showPasswordDialog(String adminLevel, String clubId) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_admin_password, null);
        EditText etPassword = dialogView.findViewById(R.id.etAdminPassword);

        String title = adminLevel.equals("SUPER_ADMIN") ? "최고 관리자 인증" : "동아리 관리자 인증";
        String message = adminLevel.equals("SUPER_ADMIN")
                ? "최고 관리자 비밀번호를 입력하세요\n(기본값: superadmin123)"
                : "동아리 관리자 비밀번호를 입력하세요\n(기본값: clubadmin123)";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setView(dialogView)
                .setPositiveButton("인증", (dialog, which) -> {
                    String password = etPassword.getText().toString().trim();
                    if (password.isEmpty()) {
                        Toast.makeText(this, "비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show();
                    } else {
                        verifyAndSetAdmin(adminLevel, clubId, password);
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void verifyAndSetAdmin(String adminLevel, String clubId, String password) {
        progressBar.setVisibility(ProgressBar.VISIBLE);

        firebaseManager.verifyAdminPassword(adminLevel, clubId, password, new FirebaseManager.PasswordVerifyCallback() {
            @Override
            public void onSuccess(boolean isValid, String level, String verifiedClubId) {
                if (isValid) {
                    setUserAdminLevel(level, verifiedClubId);
                } else {
                    progressBar.setVisibility(ProgressBar.GONE);
                    Toast.makeText(SettingsActivity.this, "비밀번호가 일치하지 않습니다", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(ProgressBar.GONE);
                Toast.makeText(SettingsActivity.this, "인증 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setUserAdminLevel(String adminLevel, String clubId) {
        String userId = firebaseManager.getCurrentUserId();

        firebaseManager.setUserAdminLevel(userId, adminLevel, clubId, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(ProgressBar.GONE);

                String message = adminLevel.equals("SUPER_ADMIN")
                        ? "최고 관리자 권한이 활성화되었습니다"
                        : "동아리 관리자 권한이 활성화되었습니다";
                Toast.makeText(SettingsActivity.this, message, Toast.LENGTH_SHORT).show();

                loadAdminStatus();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(ProgressBar.GONE);
                Toast.makeText(SettingsActivity.this, "권한 설정 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(SettingsActivity.this, "관리자 권한이 해제되었습니다", Toast.LENGTH_SHORT).show();
                            loadAdminStatus();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(SettingsActivity.this, "권한 해제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
            tvAdminStatus.setText("현재 상태: 최고 관리자\n\n모든 동아리와 캐러셀을 관리할 수 있습니다.");
            btnLogoutAdmin.setVisibility(View.VISIBLE);
        } else if (currentUserData.isClubAdmin()) {
            String clubId = currentUserData.getClubId() != null ? currentUserData.getClubId() : "미지정";
            tvAdminStatus.setText("현재 상태: 동아리 관리자\n동아리: " + clubId + "\n\n자신의 동아리만 관리할 수 있습니다.");
            btnLogoutAdmin.setVisibility(View.VISIBLE);
        }
    }
}
