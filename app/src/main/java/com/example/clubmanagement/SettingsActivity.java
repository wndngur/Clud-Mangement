package com.example.clubmanagement;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;

public class SettingsActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "SuperAdminPrefs";
    public static final String KEY_SUPER_ADMIN_MODE = "super_admin_mode_active";

    private MaterialButton btnSuperAdmin;
    private MaterialButton btnLogoutAdmin;
    private TextView tvAdminStatus;
    private ProgressBar progressBar;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

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
        // 현재 최고 관리자 모드 상태 확인
        boolean isSuperAdmin = isSuperAdminMode(this);
        if (isSuperAdmin) {
            tvAdminStatus.setText("현재 상태: 최고 관리자\n\n모든 동아리와 캐러셀을 관리할 수 있습니다.");
            btnLogoutAdmin.setVisibility(View.VISIBLE);
        } else {
            tvAdminStatus.setText("현재 상태: 일반 사용자");
            btnLogoutAdmin.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        // 개발 중: 바로 최고 관리자 모드 활성화 (비밀번호 검증 생략)
        btnSuperAdmin.setOnClickListener(v -> activateSuperAdminMode());
        btnLogoutAdmin.setOnClickListener(v -> logoutAdmin());
    }

    private void activateSuperAdminMode() {
        // 개발 중: 비밀번호 검증 없이 바로 활성화
        setSuperAdminMode(true);
        Toast.makeText(this, "최고 관리자 모드가 활성화되었습니다", Toast.LENGTH_SHORT).show();
        loadAdminStatus();
    }

    private void setSuperAdminMode(boolean active) {
        sharedPreferences.edit().putBoolean(KEY_SUPER_ADMIN_MODE, active).apply();
    }

    public static boolean isSuperAdminMode(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_SUPER_ADMIN_MODE, false);
    }

    private void logoutAdmin() {
        new AlertDialog.Builder(this)
                .setTitle("관리자 로그아웃")
                .setMessage("관리자 권한을 해제하시겠습니까?")
                .setPositiveButton("해제", (dialog, which) -> {
                    // 최고 관리자 모드 비활성화
                    setSuperAdminMode(false);
                    Toast.makeText(SettingsActivity.this, "관리자 권한이 해제되었습니다", Toast.LENGTH_SHORT).show();
                    loadAdminStatus();
                })
                .setNegativeButton("취소", null)
                .show();
    }
}
