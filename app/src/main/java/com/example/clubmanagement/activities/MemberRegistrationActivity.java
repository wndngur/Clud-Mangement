package com.example.clubmanagement.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.util.ArrayList;
import java.util.List;

import com.example.clubmanagement.R;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class MemberRegistrationActivity extends AppCompatActivity {

    private ImageView ivBack;
    private TextView tvTitle;
    private TextInputEditText etName;
    private TextInputEditText etDepartment;
    private TextInputEditText etStudentId;
    private TextInputEditText etPhone;
    private TextInputEditText etEmail;
    private Spinner spinnerBirthMonth;
    private Spinner spinnerBirthDay;
    private CheckBox cbPrivacyAgreement;
    private MaterialButton btnFetchInfo;
    private MaterialButton btnRegister;
    private View overlayBackground;
    private CardView cardApprovalStatus;
    private TextView tvApprovalMessage;
    private MaterialButton btnConfirmApproval;
    private ProgressBar progressBar;

    private String clubName;
    private String centralClubId;
    private boolean isCentralClub = false;
    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_registration);

        firebaseManager = FirebaseManager.getInstance();

        // Intent에서 동아리 정보 받기
        clubName = getIntent().getStringExtra("club_name");
        isCentralClub = getIntent().getBooleanExtra("is_central_club", false);
        centralClubId = getIntent().getStringExtra("central_club_id");

        android.util.Log.d("MemberRegistration", "onCreate - clubName: " + clubName + ", centralClubId: " + centralClubId);

        if (clubName == null || clubName.isEmpty()) {
            clubName = "동아리"; // 기본값
        }
        if (centralClubId == null || centralClubId.isEmpty()) {
            centralClubId = clubName.replaceAll("\\s+", "_").toLowerCase();
            android.util.Log.d("MemberRegistration", "Generated centralClubId: " + centralClubId);
        }

        initViews();
        setupTitle();
        setupBirthdaySpinners();
        setupListeners();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        tvTitle = findViewById(R.id.tvTitle);
        etName = findViewById(R.id.etName);
        etDepartment = findViewById(R.id.etDepartment);
        etStudentId = findViewById(R.id.etStudentId);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        spinnerBirthMonth = findViewById(R.id.spinnerBirthMonth);
        spinnerBirthDay = findViewById(R.id.spinnerBirthDay);
        cbPrivacyAgreement = findViewById(R.id.cbPrivacyAgreement);
        btnFetchInfo = findViewById(R.id.btnFetchInfo);
        btnRegister = findViewById(R.id.btnRegister);
        overlayBackground = findViewById(R.id.overlayBackground);
        cardApprovalStatus = findViewById(R.id.cardApprovalStatus);
        tvApprovalMessage = findViewById(R.id.tvApprovalMessage);
        btnConfirmApproval = findViewById(R.id.btnConfirmApproval);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupBirthdaySpinners() {
        // 월 스피너 설정
        List<String> months = new ArrayList<>();
        months.add("선택"); // 0번 인덱스
        for (int i = 1; i <= 12; i++) {
            months.add(String.valueOf(i));
        }
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, months);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBirthMonth.setAdapter(monthAdapter);

        // 일 스피너 설정
        List<String> days = new ArrayList<>();
        days.add("선택"); // 0번 인덱스
        for (int i = 1; i <= 31; i++) {
            days.add(String.valueOf(i));
        }
        ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, days);
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBirthDay.setAdapter(dayAdapter);
    }

    private int getSelectedBirthMonth() {
        int position = spinnerBirthMonth.getSelectedItemPosition();
        return position > 0 ? position : 0; // "선택"이면 0 반환
    }

    private int getSelectedBirthDay() {
        int position = spinnerBirthDay.getSelectedItemPosition();
        return position > 0 ? position : 0; // "선택"이면 0 반환
    }

    private void setupTitle() {
        // TODO: 나중에 관리자가 수정 가능하도록 변경 필요
        tvTitle.setText(clubName + " 동아리 가입");
    }

    private void setupListeners() {
        // 뒤로가기 버튼
        ivBack.setOnClickListener(v -> finish());

        // 정보 가져오기 버튼
        btnFetchInfo.setOnClickListener(v -> {
            // Firebase에서 사용자 정보 가져오기
            fetchUserInfo();
        });

        // 가입신청 하기 버튼
        btnRegister.setOnClickListener(v -> {
            if (validateInputs()) {
                registerMember();
            }
        });

        // 오버레이 배경 클릭 - 아무 동작 없음 (카드 바깥 클릭 방지)
        overlayBackground.setOnClickListener(v -> {
            // 아무 동작 없음
        });

        // 확인 버튼 클릭 시 액티비티 종료
        btnConfirmApproval.setOnClickListener(v -> {
            finish();
        });
    }

    private void fetchUserInfo() {
        // Firebase에서 현재 로그인한 사용자 정보 가져오기
        String userId = firebaseManager.getCurrentUserId();

        if (userId == null) {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        firebaseManager.getDb().collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }

                    if (documentSnapshot.exists()) {
                        // 사용자 정보 가져와서 필드에 채우기
                        String name = documentSnapshot.getString("name");
                        String department = documentSnapshot.getString("department");
                        String studentId = documentSnapshot.getString("studentId");
                        String phone = documentSnapshot.getString("phone");
                        String email = documentSnapshot.getString("email");

                        // 이메일이 없으면 Firebase Auth에서 가져오기
                        if (email == null || email.isEmpty()) {
                            if (firebaseManager.getCurrentUser() != null) {
                                email = firebaseManager.getCurrentUser().getEmail();
                            }
                        }

                        // 필드에 값 설정 (null 체크)
                        if (name != null && !name.isEmpty()) {
                            etName.setText(name);
                        }
                        if (department != null && !department.isEmpty()) {
                            etDepartment.setText(department);
                        }
                        if (studentId != null && !studentId.isEmpty()) {
                            etStudentId.setText(studentId);
                        }
                        if (phone != null && !phone.isEmpty()) {
                            etPhone.setText(phone);
                        }
                        if (email != null && !email.isEmpty()) {
                            etEmail.setText(email);
                        }

                        Toast.makeText(this, "내 정보를 가져왔습니다", Toast.LENGTH_SHORT).show();
                    } else {
                        // 사용자 문서가 없으면 이메일만 채우기
                        if (firebaseManager.getCurrentUser() != null) {
                            String email = firebaseManager.getCurrentUser().getEmail();
                            if (email != null) {
                                etEmail.setText(email);
                            }
                        }
                        Toast.makeText(this, "저장된 정보가 없습니다. 직접 입력해주세요.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    Toast.makeText(this, "정보를 가져오는데 실패했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private boolean validateInputs() {
        // 이름 확인
        if (TextUtils.isEmpty(etName.getText())) {
            Toast.makeText(this, "이름을 입력해주세요", Toast.LENGTH_SHORT).show();
            etName.requestFocus();
            return false;
        }

        // 학과 확인
        if (TextUtils.isEmpty(etDepartment.getText())) {
            Toast.makeText(this, "학과를 입력해주세요", Toast.LENGTH_SHORT).show();
            etDepartment.requestFocus();
            return false;
        }

        // 학번 확인
        if (TextUtils.isEmpty(etStudentId.getText())) {
            Toast.makeText(this, "학번을 입력해주세요", Toast.LENGTH_SHORT).show();
            etStudentId.requestFocus();
            return false;
        }

        // 전화번호 확인
        if (TextUtils.isEmpty(etPhone.getText())) {
            Toast.makeText(this, "전화번호를 입력해주세요", Toast.LENGTH_SHORT).show();
            etPhone.requestFocus();
            return false;
        }

        // 이메일 확인
        if (TextUtils.isEmpty(etEmail.getText())) {
            Toast.makeText(this, "이메일을 입력해주세요", Toast.LENGTH_SHORT).show();
            etEmail.requestFocus();
            return false;
        }

        // 이메일 형식 확인
        String email = etEmail.getText().toString();
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "올바른 이메일 형식을 입력해주세요", Toast.LENGTH_SHORT).show();
            etEmail.requestFocus();
            return false;
        }

        // 개인정보 동의 확인
        if (!cbPrivacyAgreement.isChecked()) {
            Toast.makeText(this, "개인정보 수집 및 이용에 동의해주세요", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void registerMember() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        int birthMonth = getSelectedBirthMonth();
        int birthDay = getSelectedBirthDay();

        // 입력 정보 가져오기
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String department = etDepartment.getText() != null ? etDepartment.getText().toString().trim() : "";
        String studentId = etStudentId.getText() != null ? etStudentId.getText().toString().trim() : "";
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        // 가입 신청 (승인 대기 상태로 생성)
        firebaseManager.createMembershipApplication(
                centralClubId,
                clubName,
                name,
                department,
                studentId,
                phone,
                email,
                birthMonth,
                birthDay,
                isCentralClub,
                new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }

                // 승인 대기 메시지 표시
                String message = clubName + " 가입 신청이 완료 되었습니다\n승인중이니 잠시만 기다리세요";

                // Toast 메시지 표시
                Toast.makeText(MemberRegistrationActivity.this, message, Toast.LENGTH_LONG).show();

                // 승인 대기 카드 UI 표시
                tvApprovalMessage.setText(clubName + " 가입 신청이 완료 되었습니다");
                showApprovalStatus();
            }

            @Override
            public void onFailure(Exception e) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                Toast.makeText(MemberRegistrationActivity.this,
                    "가입 신청 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showApprovalStatus() {
        overlayBackground.setVisibility(View.VISIBLE);
        cardApprovalStatus.setVisibility(View.VISIBLE);
    }

    private void hideApprovalStatus() {
        overlayBackground.setVisibility(View.GONE);
        cardApprovalStatus.setVisibility(View.GONE);
    }
}
