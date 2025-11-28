package com.example.clubmanagement.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

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
    private CheckBox cbPrivacyAgreement;
    private MaterialButton btnFetchInfo;
    private MaterialButton btnRegister;
    private View overlayBackground;
    private CardView cardApprovalStatus;
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

        if (clubName == null || clubName.isEmpty()) {
            clubName = "동아리"; // 기본값
        }
        if (centralClubId == null || centralClubId.isEmpty()) {
            centralClubId = clubName.replaceAll("\\s+", "_").toLowerCase();
        }

        initViews();
        setupTitle();
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
        cbPrivacyAgreement = findViewById(R.id.cbPrivacyAgreement);
        btnFetchInfo = findViewById(R.id.btnFetchInfo);
        btnRegister = findViewById(R.id.btnRegister);
        overlayBackground = findViewById(R.id.overlayBackground);
        cardApprovalStatus = findViewById(R.id.cardApprovalStatus);
        progressBar = findViewById(R.id.progressBar);
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
            // TODO: 저장된 정보를 가져오는 기능 구현
            // 예시 데이터로 임시 구현
            fetchSampleData();
        });

        // 가입신청 하기 버튼
        btnRegister.setOnClickListener(v -> {
            if (validateInputs()) {
                registerMember();
            }
        });

        // 오버레이 배경 클릭 시 숨기기
        overlayBackground.setOnClickListener(v -> hideApprovalStatus());
    }

    private void fetchSampleData() {
        // 예시 데이터로 필드 채우기
        etName.setText("홍길동");
        etDepartment.setText("컴퓨터공학과");
        etStudentId.setText("20240001");
        etPhone.setText("010-1234-5678");
        etEmail.setText("hong@example.com");
        Toast.makeText(this, "정보를 가져왔습니다", Toast.LENGTH_SHORT).show();
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
        // 회원 정보 수집
        String name = etName.getText().toString().trim();
        String department = etDepartment.getText().toString().trim();
        String studentId = etStudentId.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();

        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        if (isCentralClub) {
            // 중앙동아리 가입 처리 - Firebase에 저장
            firebaseManager.joinCentralClub(centralClubId, clubName, new FirebaseManager.SimpleCallback() {
                @Override
                public void onSuccess() {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }

                    // 토스트 메시지에 동아리 이름 포함
                    String message = clubName + " 동아리 가입이 완료되었습니다!";
                    Toast.makeText(MemberRegistrationActivity.this, message, Toast.LENGTH_LONG).show();

                    // 가입 완료 후 동아리 메인 페이지로 이동
                    Intent intent = new Intent(MemberRegistrationActivity.this, ClubMainActivity.class);
                    intent.putExtra("club_name", clubName);
                    intent.putExtra("club_id", centralClubId);
                    // 이전 액티비티 스택 모두 제거하고 새로 시작
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onFailure(Exception e) {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    Toast.makeText(MemberRegistrationActivity.this,
                        "가입 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // 일반 동아리 가입 처리
            firebaseManager.joinGeneralClub(centralClubId, clubName, new FirebaseManager.SimpleCallback() {
                @Override
                public void onSuccess() {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }

                    String message = clubName + " 동아리 가입이 완료되었습니다!";
                    Toast.makeText(MemberRegistrationActivity.this, message, Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(MemberRegistrationActivity.this, ClubMainActivity.class);
                    intent.putExtra("club_name", clubName);
                    intent.putExtra("club_id", centralClubId);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onFailure(Exception e) {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    Toast.makeText(MemberRegistrationActivity.this,
                        "가입 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
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
