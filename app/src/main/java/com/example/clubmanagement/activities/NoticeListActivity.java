package com.example.clubmanagement.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clubmanagement.BaseActivity;
import com.example.clubmanagement.R;
import com.example.clubmanagement.SettingsActivity;
import com.example.clubmanagement.adapters.ClubNoticeAdapter;
import com.example.clubmanagement.models.ClubNotice;
import com.example.clubmanagement.utils.FirebaseManager;

import java.util.List;

public class NoticeListActivity extends BaseActivity implements ClubNoticeAdapter.OnNoticeClickListener {

    private ImageView ivBack;
    private TextView tvTitle;
    private FrameLayout flNotification;
    private TextView tvNotificationBadge;
    private ImageView ivWrite;
    private RecyclerView rvNotices;
    private LinearLayout layoutEmpty;
    private ProgressBar progressBar;

    private String clubId;
    private String clubName;
    private boolean isAdmin = false;

    private FirebaseManager firebaseManager;
    private ClubNoticeAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_notice_list);

        clubId = getIntent().getStringExtra("club_id");
        clubName = getIntent().getStringExtra("club_name");

        if (clubId == null || clubId.isEmpty()) {
            Toast.makeText(this, "동아리 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        firebaseManager = FirebaseManager.getInstance();

        initViews();
        setupListeners();
        checkAdminPermission();
        loadNotices();
        loadUnreadNotificationCount();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotices();
        loadUnreadNotificationCount();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        tvTitle = findViewById(R.id.tvTitle);
        flNotification = findViewById(R.id.flNotification);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        ivWrite = findViewById(R.id.ivWrite);
        rvNotices = findViewById(R.id.rvNotices);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        progressBar = findViewById(R.id.progressBar);

        tvTitle.setText(clubName + " 공지사항");

        adapter = new ClubNoticeAdapter(this);
        rvNotices.setLayoutManager(new LinearLayoutManager(this));
        rvNotices.setAdapter(adapter);
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        flNotification.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationListActivity.class);
            startActivity(intent);
        });

        ivWrite.setOnClickListener(v -> {
            Intent intent = new Intent(this, NoticeWriteActivity.class);
            intent.putExtra("club_id", clubId);
            intent.putExtra("club_name", clubName);
            startActivity(intent);
        });
    }

    private void checkAdminPermission() {
        // 최고 관리자이거나 동아리 관리자 모드인 경우
        if (SettingsActivity.isSuperAdminMode(this) || ClubSettingsActivity.isClubAdminMode(this)) {
            isAdmin = true;
            ivWrite.setVisibility(View.VISIBLE);
        } else {
            // Firebase에서 관리자 권한 확인
            String userId = firebaseManager.getCurrentUserId();
            if (userId != null) {
                firebaseManager.checkMemberAdminPermission(clubId, userId, new FirebaseManager.AdminCheckCallback() {
                    @Override
                    public void onResult(boolean isAdminResult) {
                        isAdmin = isAdminResult;
                        if (isAdmin) {
                            ivWrite.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        // 실패해도 무시
                    }
                });
            }
        }
    }

    private void loadNotices() {
        progressBar.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);

        firebaseManager.getClubNotices(clubId, new FirebaseManager.ClubNoticeListCallback() {
            @Override
            public void onSuccess(List<ClubNotice> notices) {
                progressBar.setVisibility(View.GONE);

                if (notices.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    rvNotices.setVisibility(View.GONE);
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                    rvNotices.setVisibility(View.VISIBLE);
                    adapter.setNotices(notices);
                }
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                layoutEmpty.setVisibility(View.VISIBLE);
                Toast.makeText(NoticeListActivity.this, "공지 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUnreadNotificationCount() {
        String userId = firebaseManager.getCurrentUserId();
        if (userId == null) return;

        firebaseManager.getUnreadNotificationCount(userId, new FirebaseManager.CountCallback() {
            @Override
            public void onSuccess(int count) {
                if (count > 0) {
                    tvNotificationBadge.setVisibility(View.VISIBLE);
                    tvNotificationBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                } else {
                    tvNotificationBadge.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Exception e) {
                tvNotificationBadge.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onNoticeClick(ClubNotice notice) {
        Intent intent = new Intent(this, NoticeDetailActivity.class);
        intent.putExtra("club_id", clubId);
        intent.putExtra("club_name", clubName);
        intent.putExtra("notice_id", notice.getId());
        intent.putExtra("is_admin", isAdmin);
        startActivity(intent);
    }
}
