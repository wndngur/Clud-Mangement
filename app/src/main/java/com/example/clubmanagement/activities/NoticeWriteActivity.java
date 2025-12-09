package com.example.clubmanagement.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;

import com.example.clubmanagement.BaseActivity;
import com.example.clubmanagement.R;
import com.example.clubmanagement.models.ClubNotice;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class NoticeWriteActivity extends BaseActivity {

    private ImageView ivBack;
    private TextView tvTitle;
    private MaterialButton btnSubmit;
    private TextInputEditText etTitle;
    private TextInputEditText etContent;
    private SwitchCompat switchPinned;
    private ProgressBar progressBar;

    private String clubId;
    private String clubName;
    private String noticeId;
    private boolean isEditMode = false;

    private FirebaseManager firebaseManager;
    private ClubNotice currentNotice;
    private String currentUserId;
    private String currentUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_notice_write);

        clubId = getIntent().getStringExtra("club_id");
        clubName = getIntent().getStringExtra("club_name");
        noticeId = getIntent().getStringExtra("notice_id");
        isEditMode = getIntent().getBooleanExtra("edit_mode", false);

        if (clubId == null) {
            Toast.makeText(this, "동아리 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        firebaseManager = FirebaseManager.getInstance();
        currentUserId = firebaseManager.getCurrentUserId();

        initViews();
        setupListeners();
        loadCurrentUserName();

        if (isEditMode && noticeId != null) {
            loadNotice();
        }
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        tvTitle = findViewById(R.id.tvTitle);
        btnSubmit = findViewById(R.id.btnSubmit);
        etTitle = findViewById(R.id.etTitle);
        etContent = findViewById(R.id.etContent);
        switchPinned = findViewById(R.id.switchPinned);
        progressBar = findViewById(R.id.progressBar);

        if (isEditMode) {
            tvTitle.setText("공지 수정");
            btnSubmit.setText("수정");
        } else {
            tvTitle.setText("공지 작성");
            btnSubmit.setText("등록");
        }
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        btnSubmit.setOnClickListener(v -> {
            if (isEditMode) {
                updateNotice();
            } else {
                createNotice();
            }
        });
    }

    private void loadCurrentUserName() {
        if (currentUserId == null) return;

        firebaseManager.getDb().collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentUserName = doc.getString("name");
                        if (currentUserName == null || currentUserName.isEmpty()) {
                            currentUserName = doc.getString("email");
                        }
                    }
                });
    }

    private void loadNotice() {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.getClubNotice(clubId, noticeId, new FirebaseManager.ClubNoticeCallback() {
            @Override
            public void onSuccess(ClubNotice notice) {
                progressBar.setVisibility(View.GONE);
                currentNotice = notice;

                etTitle.setText(notice.getTitle());
                etContent.setText(notice.getContent());
                switchPinned.setChecked(notice.isPinned());
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(NoticeWriteActivity.this, "공지 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void createNotice() {
        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String content = etContent.getText() != null ? etContent.getText().toString().trim() : "";
        boolean isPinned = switchPinned.isChecked();

        if (TextUtils.isEmpty(title)) {
            etTitle.setError("제목을 입력해주세요");
            etTitle.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(content)) {
            etContent.setError("내용을 입력해주세요");
            etContent.requestFocus();
            return;
        }

        if (currentUserId == null) {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        String authorName = currentUserName != null ? currentUserName : "관리자";

        ClubNotice notice = new ClubNotice(clubId, title, content, currentUserId, authorName);
        notice.setPinned(isPinned);

        progressBar.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        firebaseManager.createClubNotice(notice, clubName, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(NoticeWriteActivity.this, "공지가 등록되었습니다", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                btnSubmit.setEnabled(true);
                Toast.makeText(NoticeWriteActivity.this, "등록 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateNotice() {
        if (currentNotice == null) {
            Toast.makeText(this, "공지 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String content = etContent.getText() != null ? etContent.getText().toString().trim() : "";
        boolean isPinned = switchPinned.isChecked();

        if (TextUtils.isEmpty(title)) {
            etTitle.setError("제목을 입력해주세요");
            etTitle.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(content)) {
            etContent.setError("내용을 입력해주세요");
            etContent.requestFocus();
            return;
        }

        currentNotice.setTitle(title);
        currentNotice.setContent(content);
        currentNotice.setPinned(isPinned);

        progressBar.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        firebaseManager.updateClubNotice(currentNotice, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(NoticeWriteActivity.this, "공지가 수정되었습니다", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                btnSubmit.setEnabled(true);
                Toast.makeText(NoticeWriteActivity.this, "수정 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
