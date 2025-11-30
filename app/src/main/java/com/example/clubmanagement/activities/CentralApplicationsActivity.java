package com.example.clubmanagement.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.clubmanagement.R;
import com.example.clubmanagement.models.CentralClubApplication;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class CentralApplicationsActivity extends AppCompatActivity {

    private LinearLayout llApplicationsContainer;
    private LinearLayout layoutEmpty;
    private ProgressBar progressBar;
    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_central_applications);

        firebaseManager = FirebaseManager.getInstance();

        setupToolbar();
        initViews();
        loadApplications();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("중앙동아리 신청 목록");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        llApplicationsContainer = findViewById(R.id.llApplicationsContainer);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        progressBar = findViewById(R.id.progressBar);
    }

    private void loadApplications() {
        progressBar.setVisibility(View.VISIBLE);
        llApplicationsContainer.removeAllViews();

        firebaseManager.getPendingCentralApplications(new FirebaseManager.CentralApplicationListCallback() {
            @Override
            public void onSuccess(List<CentralClubApplication> applications) {
                progressBar.setVisibility(View.GONE);

                if (applications == null || applications.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    llApplicationsContainer.setVisibility(View.GONE);
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                    llApplicationsContainer.setVisibility(View.VISIBLE);

                    for (CentralClubApplication application : applications) {
                        addApplicationItem(application);
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(CentralApplicationsActivity.this,
                        "신청 목록 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                layoutEmpty.setVisibility(View.VISIBLE);
                llApplicationsContainer.setVisibility(View.GONE);
            }
        });
    }

    private void addApplicationItem(CentralClubApplication application) {
        View itemView = LayoutInflater.from(this)
                .inflate(R.layout.item_central_application, llApplicationsContainer, false);

        // Find views
        LinearLayout accordionHeader = itemView.findViewById(R.id.accordionHeader);
        LinearLayout accordionContent = itemView.findViewById(R.id.accordionContent);
        View divider = itemView.findViewById(R.id.divider);
        TextView tvClubName = itemView.findViewById(R.id.tvClubName);
        TextView tvApplicationDate = itemView.findViewById(R.id.tvApplicationDate);
        ImageView ivExpandIcon = itemView.findViewById(R.id.ivExpandIcon);

        TextView tvApplicantEmail = itemView.findViewById(R.id.tvApplicantEmail);
        TextView tvMemberCount = itemView.findViewById(R.id.tvMemberCount);
        TextView tvDaysSinceFounding = itemView.findViewById(R.id.tvDaysSinceFounding);
        TextView tvReason = itemView.findViewById(R.id.tvReason);

        MaterialButton btnApprove = itemView.findViewById(R.id.btnApprove);
        MaterialButton btnReject = itemView.findViewById(R.id.btnReject);

        // Set data
        tvClubName.setText(application.getClubName());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA);
        if (application.getCreatedAt() != null) {
            tvApplicationDate.setText(sdf.format(application.getCreatedAt().toDate()));
        }

        tvApplicantEmail.setText(application.getApplicantEmail() != null ?
                application.getApplicantEmail() : "알 수 없음");

        // Member count status
        int memberCount = application.getMemberCount();
        boolean memberOk = memberCount >= 20;
        tvMemberCount.setText(memberCount + "명 " + (memberOk ? "✓" : "✗"));
        tvMemberCount.setTextColor(getResources().getColor(
                memberOk ? android.R.color.holo_green_dark : android.R.color.holo_red_dark, null));

        // Days since founding status
        long days = application.getDaysSinceFounding();
        boolean daysOk = days >= 180;
        tvDaysSinceFounding.setText(days + "일 " + (daysOk ? "✓" : "✗"));
        tvDaysSinceFounding.setTextColor(getResources().getColor(
                daysOk ? android.R.color.holo_green_dark : android.R.color.holo_red_dark, null));

        // Reason
        tvReason.setText(application.getReason() != null && !application.getReason().isEmpty() ?
                application.getReason() : "(사유 없음)");

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
            }
        });

        // Approve button
        btnApprove.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("신청 승인")
                    .setMessage(application.getClubName() + "을(를) 중앙동아리로 승인하시겠습니까?")
                    .setPositiveButton("승인", (dialog, which) -> approveApplication(application, itemView))
                    .setNegativeButton("취소", null)
                    .show();
        });

        // Reject button
        btnReject.setOnClickListener(v -> showRejectDialog(application, itemView));

        llApplicationsContainer.addView(itemView);
    }

    private void approveApplication(CentralClubApplication application, View itemView) {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.approveCentralApplication(application.getId(), application.getClubId(),
                new FirebaseManager.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        progressBar.setVisibility(View.GONE);
                        llApplicationsContainer.removeView(itemView);
                        Toast.makeText(CentralApplicationsActivity.this,
                                application.getClubName() + "이(가) 중앙동아리로 승인되었습니다", Toast.LENGTH_LONG).show();

                        if (llApplicationsContainer.getChildCount() == 0) {
                            layoutEmpty.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(CentralApplicationsActivity.this,
                                "승인 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showRejectDialog(CentralClubApplication application, View itemView) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_reject_reason, null);
        EditText etReason = dialogView.findViewById(R.id.etRejectReason);

        new AlertDialog.Builder(this)
                .setTitle("신청 거절")
                .setMessage(application.getClubName() + "의 신청을 거절합니다.\n거절 사유를 입력해주세요.")
                .setView(dialogView)
                .setPositiveButton("거절", (dialog, which) -> {
                    String reason = etReason.getText().toString().trim();
                    if (reason.isEmpty()) {
                        reason = "사유 없음";
                    }
                    rejectApplication(application, reason, itemView);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void rejectApplication(CentralClubApplication application, String reason, View itemView) {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.rejectCentralApplication(application.getId(), reason,
                new FirebaseManager.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        progressBar.setVisibility(View.GONE);
                        llApplicationsContainer.removeView(itemView);
                        Toast.makeText(CentralApplicationsActivity.this,
                                "신청이 거절되었습니다", Toast.LENGTH_SHORT).show();

                        if (llApplicationsContainer.getChildCount() == 0) {
                            layoutEmpty.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(CentralApplicationsActivity.this,
                                "거절 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
