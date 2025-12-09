package com.example.clubmanagement;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.clubmanagement.BaseActivity;
import com.example.clubmanagement.models.User;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class SignUpActivity extends BaseActivity {

    private ImageButton btnBack;
    private TextInputLayout tilEmail, tilPassword, tilPasswordConfirm;
    private TextInputLayout tilStudentId, tilDepartment, tilName, tilPhone;
    private TextInputEditText etEmail, etPassword, etPasswordConfirm;
    private TextInputEditText etStudentId, etDepartment, etName, etPhone;
    private MaterialCheckBox cbPrivacyAgree;
    private MaterialButton btnSignUp;
    private ProgressBar progressBar;

    private FirebaseAuth firebaseAuth;
    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseManager = FirebaseManager.getInstance();

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);

        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilPasswordConfirm = findViewById(R.id.tilPasswordConfirm);
        tilStudentId = findViewById(R.id.tilStudentId);
        tilDepartment = findViewById(R.id.tilDepartment);
        tilName = findViewById(R.id.tilName);
        tilPhone = findViewById(R.id.tilPhone);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etPasswordConfirm = findViewById(R.id.etPasswordConfirm);
        etStudentId = findViewById(R.id.etStudentId);
        etDepartment = findViewById(R.id.etDepartment);
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);

        cbPrivacyAgree = findViewById(R.id.cbPrivacyAgree);
        btnSignUp = findViewById(R.id.btnSignUp);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSignUp.setOnClickListener(v -> attemptSignUp());
    }

    private void attemptSignUp() {
        // 에러 초기화
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilPasswordConfirm.setError(null);
        tilStudentId.setError(null);
        tilDepartment.setError(null);
        tilName.setError(null);
        tilPhone.setError(null);

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String passwordConfirm = etPasswordConfirm.getText().toString().trim();
        String studentId = etStudentId.getText().toString().trim();
        String department = etDepartment.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

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

        if (TextUtils.isEmpty(studentId)) {
            tilStudentId.setError("학번을 입력해주세요");
            etStudentId.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(department)) {
            tilDepartment.setError("학과를 입력해주세요");
            etDepartment.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(name)) {
            tilName.setError("이름을 입력해주세요");
            etName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            tilPhone.setError("전화번호를 입력해주세요");
            etPhone.requestFocus();
            return;
        }

        if (!cbPrivacyAgree.isChecked()) {
            Toast.makeText(this, "개인정보 수집 및 이용에 동의해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 회원가입 시도
        showLoading(true);

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Firebase Auth 회원가입 성공 -> 사용자 정보 저장
                        String odl = firebaseAuth.getCurrentUser().getUid();
                        saveUserInfo(odl, email, studentId, department, name, phone);
                    } else {
                        showLoading(false);
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

    private void saveUserInfo(String uid, String email, String studentId,
                              String department, String name, String phone) {
        User user = new User();
        user.setUid(uid);
        user.setEmail(email);
        user.setStudentId(studentId);
        user.setDepartment(department);
        user.setName(name);
        user.setPhone(phone);

        firebaseManager.saveUser(user, new FirebaseManager.OperationCallback() {
            @Override
            public void onSuccess() {
                showLoading(false);
                Toast.makeText(SignUpActivity.this, "회원가입이 완료되었습니다!", Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                showLoading(false);
                Toast.makeText(SignUpActivity.this,
                    "사용자 정보 저장에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSignUp.setEnabled(!show);
        etEmail.setEnabled(!show);
        etPassword.setEnabled(!show);
        etPasswordConfirm.setEnabled(!show);
        etStudentId.setEnabled(!show);
        etDepartment.setEnabled(!show);
        etName.setEnabled(!show);
        etPhone.setEnabled(!show);
        cbPrivacyAgree.setEnabled(!show);
    }
}
