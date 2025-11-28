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
        tvAdminStatus = findViewById(R.id.tvAdminStatus);
        progressBar = findViewById(R.id.progressBarSettings);
    }

    private void loadAdminStatus() {
        // 현재 최고 관리자 모드 상태 확인
        boolean isSuperAdmin = isSuperAdminMode(this);
        if (isSuperAdmin) {
            tvAdminStatus.setText("현재 상태: 최고 관리자\n\n모든 동아리와 캐러셀을 관리할 수 있습니다.");
            btnSuperAdmin.setText("최고 관리자 모드 종료");
            btnSuperAdmin.setIconResource(android.R.drawable.ic_menu_close_clear_cancel);
        } else {
            tvAdminStatus.setText("현재 상태: 일반 사용자");
            btnSuperAdmin.setText("최고 관리자 모드");
            btnSuperAdmin.setIconResource(android.R.drawable.star_big_on);
        }
    }

    private void setupListeners() {
        // 토글 방식: 버튼을 누르면 관리자 모드 켜기/끄기
        btnSuperAdmin.setOnClickListener(v -> toggleSuperAdminMode());
    }

    private void toggleSuperAdminMode() {
        boolean currentMode = isSuperAdminMode(this);

        if (currentMode) {
            // 현재 관리자 모드면 -> 종료
            new AlertDialog.Builder(this)
                    .setTitle("관리자 모드 종료")
                    .setMessage("최고 관리자 모드를 종료하시겠습니까?")
                    .setPositiveButton("종료", (dialog, which) -> {
                        setSuperAdminMode(false);
                        Toast.makeText(this, "최고 관리자 모드가 종료되었습니다", Toast.LENGTH_SHORT).show();
                        loadAdminStatus();
                    })
                    .setNegativeButton("취소", null)
                    .show();
        } else {
            // 관리자 모드가 아니면 -> 활성화
            setSuperAdminMode(true);
            Toast.makeText(this, "최고 관리자 모드가 활성화되었습니다", Toast.LENGTH_SHORT).show();
            loadAdminStatus();
        }
    }

    private void setSuperAdminMode(boolean active) {
        sharedPreferences.edit().putBoolean(KEY_SUPER_ADMIN_MODE, active).apply();
    }

    public static boolean isSuperAdminMode(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_SUPER_ADMIN_MODE, false);
    }
}
