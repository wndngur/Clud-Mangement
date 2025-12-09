package com.example.clubmanagement;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.clubmanagement.BaseActivity;
import com.example.clubmanagement.activities.ClubMainActivity;
import com.example.clubmanagement.utils.FirebaseManager;
import com.example.clubmanagement.utils.ThemeHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class LoginActivity extends BaseActivity {

    private static final String PREFS_NAME = "LoginPrefs";
    private static final String KEY_AUTO_LOGIN = "auto_login";
    private static final String KEY_EMAIL = "saved_email";
    private static final String KEY_PASSWORD = "saved_password";

    private ImageButton btnSettings;
    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialCheckBox cbAutoLogin;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;
    private TextView tvSignUp, tvTestLogin;

    private FirebaseAuth firebaseAuth;
    private SharedPreferences sharedPreferences;
    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseManager = FirebaseManager.getInstance();
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        initViews();
        setupListeners();
        checkAutoLogin();
    }

    private void initViews() {
        btnSettings = findViewById(R.id.btnSettings);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        cbAutoLogin = findViewById(R.id.cbAutoLogin);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
        tvSignUp = findViewById(R.id.tvSignUp);
        tvTestLogin = findViewById(R.id.tvTestLogin);
    }

    private void setupListeners() {
        // 설정 버튼 (로그인 설정 화면으로 이동)
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginSettingsActivity.class);
            startActivity(intent);
        });

        // 로그인 버튼
        btnLogin.setOnClickListener(v -> attemptLogin());

        // 회원가입 링크
        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(this, SignUpActivity.class);
            startActivity(intent);
        });

        // 테스트 접속 버튼
        tvTestLogin.setOnClickListener(v -> {
            // 일반 사용자 테스트이므로 관리자 모드 해제
            SettingsActivity.setSuperAdminModeStatic(this, false);
            Toast.makeText(this, "테스트 모드로 접속합니다", Toast.LENGTH_SHORT).show();
            goToMainActivity();
        });
    }

    private void checkAutoLogin() {
        boolean autoLogin = sharedPreferences.getBoolean(KEY_AUTO_LOGIN, false);
        if (autoLogin) {
            String savedEmail = sharedPreferences.getString(KEY_EMAIL, "");
            String savedPassword = sharedPreferences.getString(KEY_PASSWORD, "");

            if (!savedEmail.isEmpty() && !savedPassword.isEmpty()) {
                etEmail.setText(savedEmail);
                etPassword.setText(savedPassword);
                cbAutoLogin.setChecked(true);
                // 자동 로그인 시도
                attemptLogin();
            }
        }
    }

    private void saveLoginInfo(String email, String password) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_AUTO_LOGIN, cbAutoLogin.isChecked());
        if (cbAutoLogin.isChecked()) {
            editor.putString(KEY_EMAIL, email);
            editor.putString(KEY_PASSWORD, password);
        } else {
            editor.remove(KEY_EMAIL);
            editor.remove(KEY_PASSWORD);
        }
        editor.apply();
    }

    private void goToMainActivity() {
        // Firebase에서 테마 설정 동기화 후 화면 이동
        ThemeHelper.syncThemeFromFirebase(this, () -> {
            // 가입한 동아리가 있는지 확인 후 해당 화면으로 이동
            checkAndNavigateToJoinedClub();
        });
    }

    private void checkAndNavigateToJoinedClub() {
        String userId = firebaseAuth.getCurrentUser() != null ? firebaseAuth.getCurrentUser().getUid() : null;

        if (userId == null) {
            // 로그인 안된 상태면 메인으로 이동
            navigateToMain();
            return;
        }

        firebaseManager.getUserJoinedClubs(userId, new FirebaseManager.UserClubsCallback() {
            @Override
            public void onSuccess(String centralClubId, List<String> generalClubIds) {
                if (centralClubId != null && !centralClubId.isEmpty()) {
                    // 중앙동아리 가입된 경우 해당 동아리로 이동
                    navigateToClub(centralClubId, true);
                } else if (generalClubIds != null && !generalClubIds.isEmpty()) {
                    // 일반동아리만 가입된 경우 첫번째 동아리로 이동
                    navigateToClub(generalClubIds.get(0), false);
                } else {
                    // 가입한 동아리가 없으면 메인으로 이동
                    navigateToMain();
                }
            }

            @Override
            public void onFailure(Exception e) {
                // 실패시 메인으로 이동
                navigateToMain();
            }
        });
    }

    private void navigateToClub(String clubId, boolean isCentralClub) {
        Intent intent = new Intent(this, ClubMainActivity.class);
        intent.putExtra("clubId", clubId);
        intent.putExtra("isCentralClub", isCentralClub);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivityNew.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void attemptLogin() {
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

        // 로그인 시도
        showLoading(true);

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    showLoading(false);

                    if (task.isSuccessful()) {
                        // 일반 사용자 로그인이므로 관리자 모드 해제
                        SettingsActivity.setSuperAdminModeStatic(this, false);
                        // 자동 로그인 정보 저장
                        saveLoginInfo(email, password);
                        Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show();
                        goToMainActivity();
                    } else {
                        String errorMessage = "로그인에 실패했습니다";
                        if (task.getException() != null) {
                            String exceptionMessage = task.getException().getMessage();
                            if (exceptionMessage != null) {
                                if (exceptionMessage.contains("no user record") ||
                                    exceptionMessage.contains("INVALID_LOGIN_CREDENTIALS")) {
                                    errorMessage = "등록되지 않은 이메일이거나 비밀번호가 틀렸습니다";
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
