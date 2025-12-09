package com.example.clubmanagement.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.clubmanagement.BaseActivity;
import com.example.clubmanagement.R;
import com.example.clubmanagement.models.ClubApplication;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ClubEstablishActivity extends BaseActivity {

    private ImageView ivBack;
    private TextInputEditText etClubName;
    private TextInputEditText etDescription;
    private TextInputEditText etPurpose;
    private TextInputEditText etActivityPlan;
    private MaterialButton btnSubmit;
    private ProgressBar progressBar;

    private FirebaseManager firebaseManager;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ActionBar 숨기기
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_club_establish);

        firebaseManager = FirebaseManager.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        initViews();
        setupListeners();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        etClubName = findViewById(R.id.etClubName);
        etDescription = findViewById(R.id.etDescription);
        etPurpose = findViewById(R.id.etPurpose);
        etActivityPlan = findViewById(R.id.etActivityPlan);
        btnSubmit = findViewById(R.id.btnSubmit);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        btnSubmit.setOnClickListener(v -> submitApplication());
    }

    private void submitApplication() {
        // 입력값 가져오기
        String clubName = etClubName.getText() != null ? etClubName.getText().toString().trim() : "";
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
        String purpose = etPurpose.getText() != null ? etPurpose.getText().toString().trim() : "";
        String activityPlan = etActivityPlan.getText() != null ? etActivityPlan.getText().toString().trim() : "";

        // 유효성 검사
        if (clubName.isEmpty()) {
            etClubName.setError("동아리명을 입력해주세요");
            etClubName.requestFocus();
            return;
        }

        if (description.isEmpty()) {
            etDescription.setError("동아리 설명을 입력해주세요");
            etDescription.requestFocus();
            return;
        }

        if (purpose.isEmpty()) {
            etPurpose.setError("설립 목적을 입력해주세요");
            etPurpose.requestFocus();
            return;
        }

        if (activityPlan.isEmpty()) {
            etActivityPlan.setError("활동 계획을 입력해주세요");
            etActivityPlan.requestFocus();
            return;
        }

        // 현재 사용자 정보 가져오기
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        String applicantId = currentUser.getUid();
        String applicantEmail = currentUser.getEmail() != null ? currentUser.getEmail() : "";
        String applicantName = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : applicantEmail;

        // ClubApplication 객체 생성
        ClubApplication application = new ClubApplication(
                clubName,
                description,
                purpose,
                activityPlan,
                applicantId,
                applicantEmail,
                applicantName
        );

        // 로딩 표시
        progressBar.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        // Firebase에 저장
        firebaseManager.submitClubApplication(application, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                btnSubmit.setEnabled(true);
                Toast.makeText(ClubEstablishActivity.this,
                        "동아리 설립 신청이 완료되었습니다.\n관리자 승인을 기다려주세요.",
                        Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                btnSubmit.setEnabled(true);
                Toast.makeText(ClubEstablishActivity.this,
                        "신청 실패: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
