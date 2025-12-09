package com.example.clubmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.clubmanagement.BaseActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class AdminSignUpActivity extends BaseActivity {

    // 관리자 인증 코드 (숫자 비밀번호)
    private static final String ADMIN_SECRET_CODE = "123456";

    private ImageButton btnBack;
    private TextInputLayout tilAdminCode, tilEmail, tilPassword, tilPasswordConfirm;
    private TextInputEditText etAdminCode, etEmail, etPassword, etPasswordConfirm;
    private MaterialButton btnSignUp;
    private ProgressBar progressBar;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_sign_up);

        firebaseAuth = FirebaseAuth.getInstance();

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tilAdminCode = findViewById(R.id.tilAdminCode);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilPasswordConfirm = findViewById(R.id.tilPasswordConfirm);
        etAdminCode = findViewById(R.id.etAdminCode);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etPasswordConfirm = findViewById(R.id.etPasswordConfirm);
        btnSignUp = findViewById(R.id.btnSignUp);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        // 뒤로가기 버튼
        btnBack.setOnClickListener(v -> finish());

        // 회원가입 버튼
        btnSignUp.setOnClickListener(v -> attemptSignUp());
    }

    private void attemptSignUp() {
        // 에러 초기화
        tilAdminCode.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilPasswordConfirm.setError(null);

        String adminCode = etAdminCode.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String passwordConfirm = etPasswordConfirm.getText().toString().trim();

        // 관리자 인증 코드 검사
        if (TextUtils.isEmpty(adminCode)) {
            tilAdminCode.setError("관리자 인증 코드를 입력해주세요");
            etAdminCode.requestFocus();
            return;
        }

        if (!adminCode.equals(ADMIN_SECRET_CODE)) {
            tilAdminCode.setError("관리자 인증 코드가 올바르지 않습니다");
            etAdminCode.requestFocus();
            return;
        }

        // 이메일 검사
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

        // 비밀번호 검사
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

        // 비밀번호 확인 검사
        if (TextUtils.isEmpty(passwordConfirm)) {
            tilPasswordConfirm.setError("비밀번호 확인을 입력해주세요");
            etPasswordConfirm.requestFocus();
            return;
        }

        if (!password.equals(passwordConfirm)) {
            tilPasswordConfirm.setError("비밀번호가 일치하지 않습니다");
            etPasswordConfirm.requestFocus();
            return;
        }

        // 회원가입 시도
        showLoading(true);

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    showLoading(false);

                    if (task.isSuccessful()) {
                        Toast.makeText(this, "관리자 회원가입 성공! 로그인해주세요.", Toast.LENGTH_LONG).show();
                        // 관리자 로그인 화면으로 돌아가기
                        finish();
                    } else {
                        String errorMessage = "회원가입에 실패했습니다";
                        if (task.getException() != null) {
                            String exceptionMessage = task.getException().getMessage();
                            if (exceptionMessage != null) {
                                if (exceptionMessage.contains("email address is already in use")) {
                                    errorMessage = "이미 사용 중인 이메일입니다";
                                } else if (exceptionMessage.contains("badly formatted")) {
                                    errorMessage = "이메일 형식이 올바르지 않습니다";
                                } else if (exceptionMessage.contains("weak password")) {
                                    errorMessage = "비밀번호가 너무 약합니다";
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
        btnSignUp.setEnabled(!show);
        etAdminCode.setEnabled(!show);
        etEmail.setEnabled(!show);
        etPassword.setEnabled(!show);
        etPasswordConfirm.setEnabled(!show);
    }
}
