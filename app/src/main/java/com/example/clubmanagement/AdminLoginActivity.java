package com.example.clubmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.clubmanagement.BaseActivity;
import com.example.clubmanagement.utils.ThemeHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class AdminLoginActivity extends BaseActivity {

    private ImageButton btnBack;
    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;
    private TextView tvAdminSignUp, tvTestLogin;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        firebaseAuth = FirebaseAuth.getInstance();

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
        tvAdminSignUp = findViewById(R.id.tvAdminSignUp);
        tvTestLogin = findViewById(R.id.tvTestLogin);
    }

    private void setupListeners() {
        // 뒤로가기 버튼
        btnBack.setOnClickListener(v -> finish());

        // 관리자 로그인 버튼
        btnLogin.setOnClickListener(v -> attemptAdminLogin());

        // 관리자 회원가입 링크
        tvAdminSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminSignUpActivity.class);
            startActivity(intent);
        });

        // 테스트 접속 버튼
        tvTestLogin.setOnClickListener(v -> {
            // 최고 관리자 모드 활성화
            SettingsActivity.setSuperAdminModeStatic(this, true);

            Toast.makeText(this, "관리자 테스트 모드로 접속합니다", Toast.LENGTH_SHORT).show();

            // 관리자 메인 화면으로 이동
            Intent intent = new Intent(this, AdminMainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void attemptAdminLogin() {
        // 에러 초기화
        tilEmail.setError(null);
        tilPassword.setError(null);

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // 유효성 검사
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("이메일을 입력해주세요");
            etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("올바른 이메일 형식이 아닙니다");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("비밀번호를 입력해주세요");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            tilPassword.setError("비밀번호는 6자 이상이어야 합니다");
            etPassword.requestFocus();
            return;
        }

        // 관리자 로그인 시도
        showLoading(true);

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    showLoading(false);

                    if (task.isSuccessful()) {
                        // 최고 관리자 모드 활성화
                        SettingsActivity.setSuperAdminModeStatic(this, true);

                        Toast.makeText(this, "관리자 로그인 성공", Toast.LENGTH_SHORT).show();
                        // Firebase에서 테마 동기화 후 관리자 메인 화면으로 이동
                        ThemeHelper.syncThemeFromFirebase(this, () -> {
                            Intent intent = new Intent(this, AdminMainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        });
                    } else {
                        String errorMessage = "로그인에 실패했습니다";
                        if (task.getException() != null) {
                            String exceptionMessage = task.getException().getMessage();
                            if (exceptionMessage != null) {
                                if (exceptionMessage.contains("no user record") ||
                                    exceptionMessage.contains("INVALID_LOGIN_CREDENTIALS")) {
                                    errorMessage = "등록되지 않은 관리자 이메일이거나 비밀번호가 틀렸습니다";
                                } else if (exceptionMessage.contains("password is invalid")) {
                                    errorMessage = "비밀번호가 틀렸습니다";
                                } else if (exceptionMessage.contains("network")) {
                                    errorMessage = "네트워크 연결을 확인해주세요";
                                }
                            }
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
        etEmail.setEnabled(!show);
        etPassword.setEnabled(!show);
    }
}
