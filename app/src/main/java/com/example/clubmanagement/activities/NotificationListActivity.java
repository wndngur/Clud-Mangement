package com.example.clubmanagement.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clubmanagement.BaseActivity;
import com.example.clubmanagement.R;
import com.example.clubmanagement.adapters.NotificationAdapter;
import com.example.clubmanagement.models.ClubNotification;
import com.example.clubmanagement.utils.FirebaseManager;

import java.util.List;

public class NotificationListActivity extends BaseActivity implements NotificationAdapter.OnNotificationClickListener {

    private ImageView ivBack;
    private TextView tvMarkAllRead;
    private RecyclerView rvNotifications;
    private LinearLayout layoutEmpty;
    private ProgressBar progressBar;

    private FirebaseManager firebaseManager;
    private NotificationAdapter adapter;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_notification_list);

        firebaseManager = FirebaseManager.getInstance();
        currentUserId = firebaseManager.getCurrentUserId();

        if (currentUserId == null) {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupListeners();
        loadNotifications();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        tvMarkAllRead = findViewById(R.id.tvMarkAllRead);
        rvNotifications = findViewById(R.id.rvNotifications);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        progressBar = findViewById(R.id.progressBar);

        adapter = new NotificationAdapter(this);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        tvMarkAllRead.setOnClickListener(v -> markAllAsRead());
    }

    private void loadNotifications() {
        progressBar.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);

        firebaseManager.getUserNotifications(currentUserId, new FirebaseManager.ClubNotificationListCallback() {
            @Override
            public void onSuccess(List<ClubNotification> notifications) {
                progressBar.setVisibility(View.GONE);

                if (notifications.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    rvNotifications.setVisibility(View.GONE);
                    tvMarkAllRead.setVisibility(View.GONE);
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                    rvNotifications.setVisibility(View.VISIBLE);
                    tvMarkAllRead.setVisibility(View.VISIBLE);
                    adapter.setNotifications(notifications);
                }
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                layoutEmpty.setVisibility(View.VISIBLE);
                Toast.makeText(NotificationListActivity.this, "알림 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void markAllAsRead() {
        firebaseManager.markAllNotificationsAsRead(currentUserId, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(NotificationListActivity.this, "모든 알림을 읽음으로 표시했습니다", Toast.LENGTH_SHORT).show();
                loadNotifications();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(NotificationListActivity.this, "처리 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onNotificationClick(ClubNotification notification) {
        // 알림 읽음 처리
        if (!notification.isRead()) {
            firebaseManager.markNotificationAsRead(notification.getId(), new FirebaseManager.SimpleCallback() {
                @Override
                public void onSuccess() {
                    notification.setRead(true);
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onFailure(Exception e) {
                    // 실패해도 무시
                }
            });
        }

        // 알림 타입에 따라 해당 화면으로 이동
        if (ClubNotification.TYPE_NOTICE.equals(notification.getType())) {
            Intent intent = new Intent(this, NoticeDetailActivity.class);
            intent.putExtra("club_id", notification.getClubId());
            intent.putExtra("club_name", notification.getClubName());
            intent.putExtra("notice_id", notification.getTargetId());
            startActivity(intent);
        } else if (ClubNotification.TYPE_COMMENT.equals(notification.getType())) {
            Intent intent = new Intent(this, NoticeDetailActivity.class);
            intent.putExtra("club_id", notification.getClubId());
            intent.putExtra("club_name", notification.getClubName());
            intent.putExtra("notice_id", notification.getTargetId());
            startActivity(intent);
        }
        // 다른 타입의 알림 처리 추가 가능
    }
}
