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
import androidx.appcompat.app.AppCompatActivity;

import com.example.clubmanagement.R;
import com.example.clubmanagement.models.CarouselItem;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CentralClubApplicationsActivity extends AppCompatActivity {

    private LinearLayout llApplicationsContainer;
    private LinearLayout layoutEmpty;
    private ProgressBar progressBar;
    private ImageView ivBack;
    private FirebaseManager firebaseManager;

    // 중앙동아리 등록 최소 인원
    private static final int CENTRAL_CLUB_MIN_MEMBERS = 20;
    // 중앙동아리 신청 가능 최소 일수 (6개월 = 180일)
    private static final int CENTRAL_CLUB_MIN_DAYS = 180;

    // 신청 데이터 클래스
    private static class ApplicationItem {
        String clubId;
        String clubName;
        String description;
        int memberCount;
        Date foundedAt;
        Date applicationDate;
        String status; // "pending", "approved", "rejected"

        ApplicationItem(String clubId, String clubName, String description,
                       int memberCount, Date foundedAt, Date applicationDate, String status) {
            this.clubId = clubId;
            this.clubName = clubName;
            this.description = description;
            this.memberCount = memberCount;
            this.foundedAt = foundedAt;
            this.applicationDate = applicationDate;
            this.status = status;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_central_club_applications);

        firebaseManager = FirebaseManager.getInstance();

        initViews();
        setupListeners();
        loadApplications();
    }

    private void initViews() {
        llApplicationsContainer = findViewById(R.id.llApplicationsContainer);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        progressBar = findViewById(R.id.progressBar);
        ivBack = findViewById(R.id.ivBack);
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());
    }

    private void loadApplications() {
        // 샘플 신청 데이터 (실제로는 Firebase에서 가져옴)
        List<ApplicationItem> applications = new ArrayList<>();

        // 대기중인 신청 추가
        applications.add(new ApplicationItem(
            "english_conversation_club",
            "영어회화 동아리",
            "영어 회화 실력 향상을 목표로 하는 동아리입니다. 원어민 선생님과 함께하는 weekly conversation class와 영어 토론 활동을 진행합니다.",
            22,
            getDateDaysAgo(400),
            getDateDaysAgo(5),
            "pending"
        ));

        applications.add(new ApplicationItem(
            "startup_club",
            "창업 동아리",
            "아이디어를 사업화하고 창업을 준비하는 동아리입니다. 창업 교육, 멘토링, 팀 프로젝트를 진행하며 실제 창업 경진대회에도 참여합니다.",
            25,
            getDateDaysAgo(365),
            getDateDaysAgo(3),
            "pending"
        ));

        applications.add(new ApplicationItem(
            "computer_science_club",
            "컴퓨터공학과 학술동아리",
            "컴퓨터공학 전공 학생들이 함께 프로그래밍 공부와 프로젝트를 진행하는 동아리입니다. 매주 스터디와 세미나를 진행하며, 학기당 1회 이상 팀 프로젝트를 완성합니다.",
            20,
            getDateDaysAgo(250),
            getDateDaysAgo(1),
            "pending"
        ));

        // 빈 목록 처리
        if (applications.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            llApplicationsContainer.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            llApplicationsContainer.setVisibility(View.VISIBLE);

            for (ApplicationItem application : applications) {
                addApplicationItem(application);
            }
        }
    }

    private Date getDateDaysAgo(int daysAgo) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo);
        return cal.getTime();
    }

    private void addApplicationItem(ApplicationItem application) {
        View itemView = LayoutInflater.from(this)
            .inflate(R.layout.item_central_club_application, llApplicationsContainer, false);

        // Find views
        LinearLayout accordionHeader = itemView.findViewById(R.id.accordionHeader);
        LinearLayout accordionContent = itemView.findViewById(R.id.accordionContent);
        View divider = itemView.findViewById(R.id.divider);
        TextView tvClubName = itemView.findViewById(R.id.tvClubName);
        TextView tvApplicationDate = itemView.findViewById(R.id.tvApplicationDate);
        TextView tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
        TextView tvClubDescription = itemView.findViewById(R.id.tvClubDescription);
        ImageView ivExpandIcon = itemView.findViewById(R.id.ivExpandIcon);

        // Progress views
        TextView tvFoundingDateText = itemView.findViewById(R.id.tvFoundingDateText);
        View viewFoundingProgressBar = itemView.findViewById(R.id.viewFoundingProgressBar);
        TextView tvFoundingProgressPercent = itemView.findViewById(R.id.tvFoundingProgressPercent);
        TextView tvFoundingStatusMessage = itemView.findViewById(R.id.tvFoundingStatusMessage);

        TextView tvMemberCountText = itemView.findViewById(R.id.tvMemberCountText);
        View viewMemberProgressBar = itemView.findViewById(R.id.viewMemberProgressBar);
        TextView tvMemberProgressPercent = itemView.findViewById(R.id.tvMemberProgressPercent);
        TextView tvMemberStatusMessage = itemView.findViewById(R.id.tvMemberStatusMessage);

        // Buttons
        MaterialButton btnApprove = itemView.findViewById(R.id.btnApprove);
        MaterialButton btnReject = itemView.findViewById(R.id.btnReject);

        // Set data
        tvClubName.setText(application.clubName);
        tvClubDescription.setText(application.description);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd", Locale.KOREA);
        tvApplicationDate.setText("신청일: " + sdf.format(application.applicationDate));

        // Status badge
        if ("pending".equals(application.status)) {
            tvStatusBadge.setText("대기중");
            tvStatusBadge.setBackgroundResource(R.drawable.badge_pending);
        }

        // Set founding date progress
        updateFoundingProgressUI(application.foundedAt, tvFoundingDateText, viewFoundingProgressBar,
            tvFoundingProgressPercent, tvFoundingStatusMessage);

        // Set member count progress
        updateMemberProgressUI(application.memberCount, tvMemberCountText, viewMemberProgressBar,
            tvMemberProgressPercent, tvMemberStatusMessage);

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

        // Approve button click listener
        btnApprove.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("중앙동아리 승인")
                .setMessage(application.clubName + "을(를) 중앙동아리로 승인하시겠습니까?\n\n승인 후 메인 캐러셀에 추가됩니다.")
                .setPositiveButton("승인", (dialog, which) -> {
                    approveApplication(application, itemView);
                })
                .setNegativeButton("취소", null)
                .show();
        });

        // Reject button click listener
        btnReject.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("신청 거절")
                .setMessage(application.clubName + "의 중앙동아리 신청을 거절하시겠습니까?")
                .setPositiveButton("거절", (dialog, which) -> {
                    rejectApplication(application, itemView);
                })
                .setNegativeButton("취소", null)
                .show();
        });

        llApplicationsContainer.addView(itemView);
    }

    private void approveApplication(ApplicationItem application, View itemView) {
        progressBar.setVisibility(View.VISIBLE);

        // 캐러셀에 추가할 아이템 생성
        CarouselItem carouselItem = new CarouselItem();
        carouselItem.setClubId(application.clubId);
        carouselItem.setTitle(application.clubName);
        carouselItem.setDescription(application.description);
        // 캐러셀 position은 Firebase에서 다음 빈 슬롯 찾아서 할당

        // Firebase에 캐러셀 아이템 추가
        firebaseManager.addCarouselItem(carouselItem, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(CentralClubApplicationsActivity.this,
                    application.clubName + "이(가) 중앙동아리로 승인되었습니다!", Toast.LENGTH_LONG).show();

                // UI에서 해당 아이템 제거
                llApplicationsContainer.removeView(itemView);

                // 빈 목록 체크
                if (llApplicationsContainer.getChildCount() == 0) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(CentralClubApplicationsActivity.this,
                    "승인 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void rejectApplication(ApplicationItem application, View itemView) {
        progressBar.setVisibility(View.VISIBLE);

        // 동아리의 배너(캐러셀)를 초기화
        firebaseManager.clearAllBanners(new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(CentralClubApplicationsActivity.this,
                    application.clubName + " 신청이 거절되었습니다.\n동아리 배너가 초기화되었습니다.",
                    Toast.LENGTH_LONG).show();

                llApplicationsContainer.removeView(itemView);

                if (llApplicationsContainer.getChildCount() == 0) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(CentralClubApplicationsActivity.this,
                    application.clubName + " 신청이 거절되었습니다.",
                    Toast.LENGTH_SHORT).show();

                llApplicationsContainer.removeView(itemView);

                if (llApplicationsContainer.getChildCount() == 0) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void updateFoundingProgressUI(Date foundedAt, TextView tvFoundingDateText,
            View viewFoundingProgressBar, TextView tvFoundingProgressPercent,
            TextView tvFoundingStatusMessage) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd", Locale.KOREA);

        if (foundedAt == null) {
            tvFoundingDateText.setText("미설정");
            tvFoundingProgressPercent.setText("0%");
            tvFoundingStatusMessage.setText("설립일 정보 없음");
            return;
        }

        long diffMillis = System.currentTimeMillis() - foundedAt.getTime();
        long daysSinceFounding = diffMillis / (1000 * 60 * 60 * 24);

        tvFoundingDateText.setText(sdf.format(foundedAt) + " (" + daysSinceFounding + "일 경과)");

        int percent = daysSinceFounding >= CENTRAL_CLUB_MIN_DAYS ? 100 :
            (int) ((daysSinceFounding * 100) / CENTRAL_CLUB_MIN_DAYS);
        tvFoundingProgressPercent.setText(percent + "%");

        viewFoundingProgressBar.post(() -> {
            int parentWidth = ((View) viewFoundingProgressBar.getParent()).getWidth();
            int progressWidth = (int) (parentWidth * percent / 100.0f);
            android.view.ViewGroup.LayoutParams params = viewFoundingProgressBar.getLayoutParams();
            params.width = progressWidth;
            viewFoundingProgressBar.setLayoutParams(params);
        });

        if (daysSinceFounding >= CENTRAL_CLUB_MIN_DAYS) {
            tvFoundingStatusMessage.setText("신청 조건 충족 (6개월 이상)");
            tvFoundingStatusMessage.setTextColor(
                androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_green_dark));
            tvFoundingProgressPercent.setTextColor(
                androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_green_dark));
        } else {
            long daysNeeded = CENTRAL_CLUB_MIN_DAYS - daysSinceFounding;
            tvFoundingStatusMessage.setText(daysNeeded + "일 부족");
            tvFoundingStatusMessage.setTextColor(
                androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_orange_dark));
            tvFoundingProgressPercent.setTextColor(
                androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_orange_dark));
        }
    }

    private void updateMemberProgressUI(int memberCount, TextView tvMemberCountText,
            View viewMemberProgressBar, TextView tvMemberProgressPercent,
            TextView tvMemberStatusMessage) {

        tvMemberCountText.setText(memberCount + "/" + CENTRAL_CLUB_MIN_MEMBERS + "명");

        int percent = memberCount >= CENTRAL_CLUB_MIN_MEMBERS ? 100 :
            (memberCount * 100 / CENTRAL_CLUB_MIN_MEMBERS);
        tvMemberProgressPercent.setText(percent + "%");

        viewMemberProgressBar.post(() -> {
            int parentWidth = ((View) viewMemberProgressBar.getParent()).getWidth();
            int progressWidth = (int) (parentWidth * percent / 100.0f);
            android.view.ViewGroup.LayoutParams params = viewMemberProgressBar.getLayoutParams();
            params.width = progressWidth;
            viewMemberProgressBar.setLayoutParams(params);
        });

        if (memberCount >= CENTRAL_CLUB_MIN_MEMBERS) {
            tvMemberStatusMessage.setText("인원 조건 충족!");
            tvMemberStatusMessage.setTextColor(
                androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_green_dark));
            tvMemberProgressPercent.setTextColor(
                androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_green_dark));
        } else {
            int needed = CENTRAL_CLUB_MIN_MEMBERS - memberCount;
            tvMemberStatusMessage.setText(needed + "명 부족");
            tvMemberStatusMessage.setTextColor(
                androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_orange_dark));
            tvMemberProgressPercent.setTextColor(
                androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_orange_dark));
        }
    }
}
