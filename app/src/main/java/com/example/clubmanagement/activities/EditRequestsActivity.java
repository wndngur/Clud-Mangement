package com.example.clubmanagement.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.example.clubmanagement.BaseActivity;
import com.example.clubmanagement.R;
import com.example.clubmanagement.models.EditRequest;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class EditRequestsActivity extends BaseActivity {

    private LinearLayout llRequestsContainer;
    private LinearLayout layoutEmpty;
    private ProgressBar progressBar;
    private FirebaseManager firebaseManager;
    private String clubId;
    private String clubName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_requests);

        firebaseManager = FirebaseManager.getInstance();

        // Get club info from intent
        clubId = getIntent().getStringExtra("club_id");
        clubName = getIntent().getStringExtra("club_name");

        if (clubId == null) {
            Toast.makeText(this, "동아리 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar();
        initViews();
        loadEditRequests();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("수정 내역");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        llRequestsContainer = findViewById(R.id.llRequestsContainer);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        progressBar = findViewById(R.id.progressBar);
    }

    private void loadEditRequests() {
        progressBar.setVisibility(View.VISIBLE);
        llRequestsContainer.removeAllViews();

        firebaseManager.getEditRequestsForClub(clubId, new FirebaseManager.EditRequestListCallback() {
            @Override
            public void onSuccess(List<EditRequest> requests) {
                progressBar.setVisibility(View.GONE);

                if (requests == null || requests.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    llRequestsContainer.setVisibility(View.GONE);
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                    llRequestsContainer.setVisibility(View.VISIBLE);

                    for (EditRequest request : requests) {
                        addRequestItem(request);
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(EditRequestsActivity.this,
                    "수정 요청 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                layoutEmpty.setVisibility(View.VISIBLE);
                llRequestsContainer.setVisibility(View.GONE);
            }
        });
    }

    private void addRequestItem(EditRequest request) {
        View itemView = LayoutInflater.from(this)
            .inflate(R.layout.item_edit_request, llRequestsContainer, false);

        // Find views
        LinearLayout accordionHeader = itemView.findViewById(R.id.accordionHeader);
        LinearLayout accordionContent = itemView.findViewById(R.id.accordionContent);
        View divider = itemView.findViewById(R.id.divider);
        TextView tvFieldName = itemView.findViewById(R.id.tvFieldName);
        TextView tvRequestDate = itemView.findViewById(R.id.tvRequestDate);
        TextView tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
        ImageView ivExpandIcon = itemView.findViewById(R.id.ivExpandIcon);

        TextView tvOldValue = itemView.findViewById(R.id.tvOldValue);
        TextView tvNewValue = itemView.findViewById(R.id.tvNewValue);
        TextView tvReason = itemView.findViewById(R.id.tvReason);
        TextView tvRequesterEmail = itemView.findViewById(R.id.tvRequesterEmail);

        MaterialButton btnMarkRead = itemView.findViewById(R.id.btnMarkRead);
        MaterialButton btnDelete = itemView.findViewById(R.id.btnDelete);

        // Set data
        tvFieldName.setText(request.getFieldDisplayName() + " 수정 요청");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA);
        if (request.getCreatedAt() != null) {
            tvRequestDate.setText(sdf.format(request.getCreatedAt().toDate()));
        }

        // Status badge
        if (!request.isRead()) {
            tvStatusBadge.setText("새 수정");
            tvStatusBadge.setBackgroundResource(R.drawable.badge_pending);
            tvStatusBadge.setVisibility(View.VISIBLE);
        } else {
            tvStatusBadge.setVisibility(View.GONE);
        }

        // Content details
        tvOldValue.setText(request.getOldValue() != null ? request.getOldValue() : "(없음)");
        tvNewValue.setText(request.getNewValue() != null ? request.getNewValue() : "(없음)");
        tvReason.setText(request.getReason() != null ? request.getReason() : "(사유 없음)");
        tvRequesterEmail.setText(request.getRequesterEmail() != null ? request.getRequesterEmail() : "알 수 없음");

        // Update button text based on read status
        if (request.isRead()) {
            btnMarkRead.setText("확인됨");
            btnMarkRead.setEnabled(false);
        }

        // Accordion click listener
        accordionHeader.setOnClickListener(v -> {
            boolean isExpanded = accordionContent.getVisibility() == View.VISIBLE;

            if (isExpanded) {
                accordionContent.setVisibility(View.GONE);
                divider.setVisibility(View.GONE);
                ivExpandIcon.setRotation(0);
            } else {
                accordionContent.setVisibility(View.VISIBLE);
                divider.setVisibility(View.VISIBLE);
                ivExpandIcon.setRotation(180);

                // 펼칠 때 읽음 처리
                if (!request.isRead()) {
                    markAsRead(request, tvStatusBadge, btnMarkRead);
                }
            }
        });

        // Mark as read button
        btnMarkRead.setOnClickListener(v -> {
            if (!request.isRead()) {
                markAsRead(request, tvStatusBadge, btnMarkRead);
            }
        });

        // Delete button
        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("수정 내역 삭제")
                .setMessage("이 수정 내역을 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> deleteRequest(request, itemView))
                .setNegativeButton("취소", null)
                .show();
        });

        llRequestsContainer.addView(itemView);
    }

    private void markAsRead(EditRequest request, TextView tvStatusBadge, MaterialButton btnMarkRead) {
        firebaseManager.markEditRequestAsRead(request.getId(), new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                request.setRead(true);
                tvStatusBadge.setVisibility(View.GONE);
                btnMarkRead.setText("확인됨");
                btnMarkRead.setEnabled(false);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(EditRequestsActivity.this,
                    "읽음 처리 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteRequest(EditRequest request, View itemView) {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.deleteEditRequest(request.getId(), new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                llRequestsContainer.removeView(itemView);
                Toast.makeText(EditRequestsActivity.this, "삭제되었습니다", Toast.LENGTH_SHORT).show();

                if (llRequestsContainer.getChildCount() == 0) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(EditRequestsActivity.this,
                    "삭제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
