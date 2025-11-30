package com.example.clubmanagement.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.viewpager2.widget.ViewPager2;

import com.example.clubmanagement.BaseActivity;

import com.example.clubmanagement.adapters.BannerAdapter;
import com.google.firebase.Timestamp;

import java.util.Calendar;
import java.util.Date;

import com.bumptech.glide.Glide;
import com.example.clubmanagement.MainActivityNew;
import com.example.clubmanagement.R;
import com.example.clubmanagement.SettingsActivity;
import com.example.clubmanagement.adapters.CalendarAdapter;
import com.example.clubmanagement.models.Banner;
import com.example.clubmanagement.models.EditRequest;
import com.example.clubmanagement.models.LinkButton;
import com.example.clubmanagement.models.Notice;
import com.example.clubmanagement.models.Schedule;
import com.example.clubmanagement.models.UserData;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class ClubMainActivity extends BaseActivity {

    private FirebaseManager firebaseManager;
    private boolean isAdmin = false;
    private UserData currentUserData;

    // UI Components
    private TextView tvClubName;
    private ImageView ivEditClubName;
    private ImageView ivSettings;
    private ImageView ivHome;
    private ImageView ivBack;  // 호환성을 위해 유지
    private TextView tvAdminModeIndicator;
    private LinearLayout llNoticesContainer;
    private MaterialButton btnAddNotice;
    private MaterialButton btnClubInfo;
    private ImageView ivEditBanner;
    private ImageView ivAddBanner;
    private ImageView ivBannerSpeed;
    private ViewPager2 vpBanners;
    private LinearLayout layoutIndicator;
    private androidx.cardview.widget.CardView cardBanner;
    private ProgressBar progressBar;

    // Banner auto-slide
    private BannerAdapter bannerAdapter;
    private Handler autoSlideHandler;
    private Runnable autoSlideRunnable;
    private long bannerSlideInterval = 3000L; // Default 3 seconds
    private boolean isAutoSliding = true;

    // Budget UI Components
    private TextView tvCurrentBudget;
    private TextView tvTotalBudget;
    private View viewBudgetProgress;
    private TextView tvBudgetPercent;
    private TextView tvBudgetStatus;
    private ImageView ivEditBudget;
    private androidx.cardview.widget.CardView cardBudget;

    // Member Count UI Components
    private TextView tvCurrentMemberCount;
    private TextView tvRequiredMemberCount;
    private View viewMemberProgress;
    private TextView tvMemberPercent;
    private TextView tvMemberStatus;
    private TextView tvClubTypeBadge;
    private TextView tvMemberManage;
    private MaterialButton btnMemberList;
    private MaterialButton btnClubNotice;
    private androidx.cardview.widget.CardView cardMemberCount;

    // Founding Date UI Components
    private TextView tvFoundingDate;
    private TextView tvDaysSinceFounding;
    private View viewFoundingProgress;
    private TextView tvFoundingPercent;
    private TextView tvFoundingStatus;
    private ImageView ivEditFoundingDate;
    private androidx.cardview.widget.CardView cardFoundingDate;

    // Calendar UI Components
    private androidx.cardview.widget.CardView cardCalendar;
    private GridView gridCalendar;
    private TextView tvCurrentMonth;
    private ImageView ivPrevMonth;
    private ImageView ivNextMonth;
    private ImageView ivAddSchedule;
    private LinearLayout llDdayContainer;
    private TextView tvNoSchedule;
    private CalendarAdapter calendarAdapter;
    private Calendar displayedCalendar;
    private List<Schedule> schedules;

    // Data
    private List<Notice> notices;
    private List<Banner> banners;
    private String clubName;
    private String clubId;
    private boolean isCentralClub = false;
    private com.example.clubmanagement.models.Club currentClub;

    // Firebase에서 가져온 인원 제한 값 (기본값 설정)
    private int registerLimit = 20;
    private int maintainLimit = 15;

    private ActivityResultLauncher<Intent> bannerImagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_club_main);

        firebaseManager = FirebaseManager.getInstance();

        // Get club info from intent (두 가지 키 모두 확인)
        clubName = getIntent().getStringExtra("club_name");
        if (clubName == null) {
            clubName = "동아리";
        }
        // clubId 또는 club_id 키로 전달된 값 확인
        clubId = getIntent().getStringExtra("clubId");
        if (clubId == null || clubId.isEmpty()) {
            clubId = getIntent().getStringExtra("club_id");
        }
        android.util.Log.d("ClubMainActivity", "onCreate - received clubId: " + clubId + ", clubName: " + clubName);
        // isCentralClub 또는 is_central_club 키로 전달된 값 확인
        isCentralClub = getIntent().getBooleanExtra("isCentralClub", false);
        if (!isCentralClub) {
            isCentralClub = getIntent().getBooleanExtra("is_central_club", false);
        }

        initViews();
        setupImagePickerLauncher();
        setupBackPressedCallback();
        checkAdminStatus();
        loadAllData();
        setupListeners();
    }

    private void setupBackPressedCallback() {
        // 최고 관리자 또는 일반동아리 회원은 뒤로가기로 캐러셀 화면으로 돌아갈 수 있음
        // 중앙동아리 회원은 뒤로가기 방지
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (SettingsActivity.isSuperAdminMode(ClubMainActivity.this) || !isCentralClub) {
                    navigateToMainCarousel();
                } else {
                    moveTaskToBack(true);
                }
            }
        });
    }

    private void navigateToMainCarousel() {
        Intent intent = new Intent(ClubMainActivity.this, MainActivityNew.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void initViews() {
        TextView tvJoinDate = findViewById(R.id.tvJoinDate);
        tvClubName = findViewById(R.id.tvClubName);
        ivEditClubName = findViewById(R.id.ivEditClubName);
        ivSettings = findViewById(R.id.ivSettings);
        ivHome = findViewById(R.id.ivHome);
        ivBack = findViewById(R.id.ivBack);  // 호환성을 위해 유지
        tvAdminModeIndicator = findViewById(R.id.tvAdminModeIndicator);

        // 홈 버튼 클릭 시 캐러셀 화면으로 이동 (항상 표시)
        if (ivHome != null) {
            ivHome.setOnClickListener(v -> navigateToMainCarousel());
        }
        llNoticesContainer = findViewById(R.id.llNoticesContainer);
        btnAddNotice = findViewById(R.id.btnAddNotice);
        btnClubInfo = findViewById(R.id.btnClubInfo);
        ivEditBanner = findViewById(R.id.ivEditBanner);
        ivAddBanner = findViewById(R.id.ivAddBanner);
        ivBannerSpeed = findViewById(R.id.ivBannerSpeed);
        vpBanners = findViewById(R.id.vpBanners);
        layoutIndicator = findViewById(R.id.layoutIndicator);
        cardBanner = findViewById(R.id.cardBanner);
        progressBar = findViewById(R.id.progressBar);

        // Setup banner adapter and ViewPager2
        setupBannerViewPager();

        // Budget views
        tvCurrentBudget = findViewById(R.id.tvCurrentBudget);
        tvTotalBudget = findViewById(R.id.tvTotalBudget);
        viewBudgetProgress = findViewById(R.id.viewBudgetProgress);
        tvBudgetPercent = findViewById(R.id.tvBudgetPercent);
        tvBudgetStatus = findViewById(R.id.tvBudgetStatus);
        ivEditBudget = findViewById(R.id.ivEditBudget);
        cardBudget = findViewById(R.id.cardBudget);

        // Member count views
        tvCurrentMemberCount = findViewById(R.id.tvCurrentMemberCount);
        tvRequiredMemberCount = findViewById(R.id.tvRequiredMemberCount);
        viewMemberProgress = findViewById(R.id.viewMemberProgress);
        tvMemberPercent = findViewById(R.id.tvMemberPercent);
        tvMemberStatus = findViewById(R.id.tvMemberStatus);
        tvClubTypeBadge = findViewById(R.id.tvClubTypeBadge);
        tvMemberManage = findViewById(R.id.tvMemberManage);
        btnMemberList = findViewById(R.id.btnMemberList);
        btnClubNotice = findViewById(R.id.btnClubNotice);
        cardMemberCount = findViewById(R.id.cardMemberCount);

        // Founding date views
        tvFoundingDate = findViewById(R.id.tvFoundingDate);
        tvDaysSinceFounding = findViewById(R.id.tvDaysSinceFounding);
        viewFoundingProgress = findViewById(R.id.viewFoundingProgress);
        tvFoundingPercent = findViewById(R.id.tvFoundingPercent);
        tvFoundingStatus = findViewById(R.id.tvFoundingStatus);
        ivEditFoundingDate = findViewById(R.id.ivEditFoundingDate);
        cardFoundingDate = findViewById(R.id.cardFoundingDate);

        // Calendar views
        cardCalendar = findViewById(R.id.cardCalendar);
        gridCalendar = findViewById(R.id.gridCalendar);
        tvCurrentMonth = findViewById(R.id.tvCurrentMonth);
        ivPrevMonth = findViewById(R.id.ivPrevMonth);
        ivNextMonth = findViewById(R.id.ivNextMonth);
        ivAddSchedule = findViewById(R.id.ivAddSchedule);
        llDdayContainer = findViewById(R.id.llDdayContainer);
        tvNoSchedule = findViewById(R.id.tvNoSchedule);

        // Initialize calendar
        displayedCalendar = Calendar.getInstance();
        schedules = new java.util.ArrayList<>();
        setupCalendar();

        tvClubName.setText(clubName);
    }

    private void setupImagePickerLauncher() {
        bannerImagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            uploadBannerImage(imageUri);
                        }
                    }
                }
        );
    }

    private void checkAdminStatus() {
        // 동아리 관리자 모드 확인 (SharedPreferences 기반)
        boolean isClubAdminMode = ClubSettingsActivity.isClubAdminMode(this);

        if (isClubAdminMode) {
            isAdmin = true;
            showAdminUI();
            return;
        }

        // Firebase 기반 관리자 확인 (기존 로직)
        firebaseManager.getUserData(firebaseManager.getCurrentUserId(), new FirebaseManager.UserDataCallback() {
            @Override
            public void onSuccess(UserData userData) {
                currentUserData = userData;

                if (userData != null) {
                    // Check if user is club admin for THIS club or super admin
                    boolean isSuperAdmin = userData.isSuperAdmin();
                    boolean isThisClubAdmin = userData.isClubAdmin() &&
                                              userData.getClubId() != null &&
                                              userData.getClubId().equals(getClubId());

                    isAdmin = isSuperAdmin || isThisClubAdmin;

                    if (isAdmin) {
                        showAdminUI();
                    } else {
                        hideAdminUI();
                    }
                } else {
                    isAdmin = false;
                    hideAdminUI();
                }
            }

            @Override
            public void onFailure(Exception e) {
                isAdmin = false;
                hideAdminUI();
            }
        });
    }

    private void showAdminUI() {
        ivEditClubName.setVisibility(View.VISIBLE);
        btnAddNotice.setVisibility(View.VISIBLE);
        ivEditBanner.setVisibility(View.VISIBLE);
        ivAddBanner.setVisibility(View.VISIBLE);
        ivBannerSpeed.setVisibility(View.VISIBLE);
        ivEditBudget.setVisibility(View.VISIBLE);
        ivEditFoundingDate.setVisibility(View.VISIBLE);
        ivAddSchedule.setVisibility(View.VISIBLE);

        // 관리자용 부원 관리 표시, 일반 사용자용 부원 명단 버튼 숨김
        tvMemberManage.setVisibility(View.VISIBLE);
        btnMemberList.setVisibility(View.GONE);

        // 동아리 관리자 모드 표시
        if (ClubSettingsActivity.isClubAdminMode(this)) {
            tvAdminModeIndicator.setVisibility(View.VISIBLE);
        }
    }

    private void hideAdminUI() {
        ivEditClubName.setVisibility(View.GONE);
        btnAddNotice.setVisibility(View.GONE);
        ivEditBanner.setVisibility(View.GONE);
        ivAddBanner.setVisibility(View.GONE);
        ivBannerSpeed.setVisibility(View.GONE);
        ivEditBudget.setVisibility(View.GONE);
        ivEditFoundingDate.setVisibility(View.GONE);
        ivAddSchedule.setVisibility(View.GONE);
        tvAdminModeIndicator.setVisibility(View.GONE);

        // 일반 사용자용 부원 명단 버튼 표시, 관리자용 부원 관리 숨김
        tvMemberManage.setVisibility(View.GONE);
        btnMemberList.setVisibility(View.VISIBLE);
    }

    private String getClubId() {
        // Use club ID from intent if available, otherwise generate from club name
        if (clubId != null && !clubId.isEmpty()) {
            return clubId;
        }
        return clubName.replaceAll("\\s+", "_").toLowerCase();
    }

    private void loadAllData() {
        loadMemberLimits();
        loadClubInfo();
        loadNotices();
        loadBanner();
        loadSchedules();
        checkBirthdayMembers();
    }

    private void loadMemberLimits() {
        firebaseManager.getMemberLimits(new FirebaseManager.MemberLimitsCallback() {
            @Override
            public void onSuccess(int register, int maintain) {
                registerLimit = register;
                maintainLimit = maintain;
                // 인원 제한 값이 로드되면 UI 갱신
                displayMemberCount();
            }

            @Override
            public void onFailure(Exception e) {
                // 실패 시 기본값 사용 (이미 설정됨)
            }
        });
    }

    private void setupListeners() {
        ivSettings.setOnClickListener(v -> openClubSettings());

        // Edit club name on click (admin only)
        tvClubName.setOnClickListener(v -> {
            if (isAdmin) {
                showEditClubNameDialog();
            }
        });
        ivEditClubName.setOnClickListener(v -> showEditClubNameDialog());

        btnAddNotice.setOnClickListener(v -> showAddNoticeDialog());
        ivEditBanner.setOnClickListener(v -> showEditCurrentBannerDialog());
        ivAddBanner.setOnClickListener(v -> showAddBannerDialog());
        ivBannerSpeed.setOnClickListener(v -> showBannerSpeedDialog());
        btnClubInfo.setOnClickListener(v -> openClubInfo());
        btnClubNotice.setOnClickListener(v -> openClubNoticeList());

        ivEditBudget.setOnClickListener(v -> showEditBudgetDialog());
        ivEditFoundingDate.setOnClickListener(v -> showEditFoundingDateDialog());

        // Budget card click - open budget history
        cardBudget.setOnClickListener(v -> openBudgetHistory());

        // Member count card click - 관리자는 부원 관리로 이동
        cardMemberCount.setOnClickListener(v -> {
            if (isAdmin) {
                openMemberManagement();
            }
        });

        // 일반 사용자용 부원 명단 보기 버튼 클릭
        btnMemberList.setOnClickListener(v -> openMemberList());

        // Founding date card click - show info
        cardFoundingDate.setOnClickListener(v -> {
            if (currentClub != null && currentClub.getFoundedAt() != null) {
                long days = currentClub.getDaysSinceFounding();
                boolean canApply = currentClub.canApplyForCentralByDate();
                String message = canApply ?
                        "설립 후 " + days + "일이 경과하여 중앙동아리 신청이 가능합니다." :
                        "중앙동아리 신청까지 " + currentClub.getDaysUntilCentralEligible() + "일 남았습니다.";
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "설립일이 설정되지 않았습니다", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openBudgetHistory() {
        Intent intent = new Intent(ClubMainActivity.this, BudgetHistoryActivity.class);
        intent.putExtra("club_id", getClubId());
        intent.putExtra("club_name", clubName);
        startActivity(intent);
    }

    private void openClubSettings() {
        String clubIdToPass = getClubId();
        android.util.Log.d("ClubMainActivity", "openClubSettings - passing clubId: " + clubIdToPass + ", clubName: " + clubName);
        Intent intent = new Intent(ClubMainActivity.this, ClubSettingsActivity.class);
        intent.putExtra("club_name", clubName);
        intent.putExtra("club_id", clubIdToPass);
        startActivity(intent);
    }

    private void openClubInfo() {
        Intent intent = new Intent(ClubMainActivity.this, ClubInfoActivity.class);
        intent.putExtra("club_id", getClubId());
        intent.putExtra("club_name", clubName);
        startActivity(intent);
    }

    private void openMemberManagement() {
        Intent intent = new Intent(ClubMainActivity.this, MemberManagementActivity.class);
        intent.putExtra("club_id", getClubId());
        intent.putExtra("club_name", clubName);
        startActivity(intent);
    }

    private void openMemberList() {
        Intent intent = new Intent(ClubMainActivity.this, MemberListActivity.class);
        intent.putExtra("club_id", getClubId());
        intent.putExtra("club_name", clubName);
        startActivity(intent);
    }

    private void openClubNoticeList() {
        Intent intent = new Intent(ClubMainActivity.this, NoticeListActivity.class);
        intent.putExtra("club_id", getClubId());
        intent.putExtra("club_name", clubName);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 관리자 모드 상태에 따라 UI 업데이트
        updateAdminModeUI();
        // Reload admin status when returning from settings
        checkAdminStatus();
        // Resume auto-slide
        isAutoSliding = true;
        startAutoSlide();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pause auto-slide
        isAutoSliding = false;
        stopAutoSlide();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up handler
        if (autoSlideHandler != null) {
            autoSlideHandler.removeCallbacks(autoSlideRunnable);
        }
    }

    private void updateAdminModeUI() {
        boolean isClubAdminMode = ClubSettingsActivity.isClubAdminMode(this);

        if (isClubAdminMode) {
            tvAdminModeIndicator.setVisibility(View.VISIBLE);
        } else {
            tvAdminModeIndicator.setVisibility(View.GONE);
            // 관리자 모드가 꺼지면 관리 UI도 숨김 (Firebase 관리자가 아닌 경우)
            if (!isAdmin) {
                hideAdminUI();
            }
        }
    }

    // ========================================
    // Notice Methods
    // ========================================

    private void loadNotices() {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.getNotices(new FirebaseManager.NoticeListCallback() {
            @Override
            public void onSuccess(List<Notice> noticeList) {
                progressBar.setVisibility(View.GONE);
                notices = noticeList;
                displayNotices();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ClubMainActivity.this, "공지사항 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayNotices() {
        llNoticesContainer.removeAllViews();

        if (notices == null || notices.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("등록된 공지사항이 없습니다.");
            emptyView.setTextSize(14);
            emptyView.setPadding(16, 16, 16, 16);
            llNoticesContainer.addView(emptyView);
            return;
        }

        for (Notice notice : notices) {
            addNoticeAccordion(notice);
        }
    }

    private void addNoticeAccordion(Notice notice) {
        View accordionView = LayoutInflater.from(this).inflate(R.layout.item_notice_accordion, llNoticesContainer, false);

        LinearLayout llHeader = accordionView.findViewById(R.id.llAccordionHeader);
        LinearLayout llContent = accordionView.findViewById(R.id.llAccordionContent);
        View divider = accordionView.findViewById(R.id.divider);
        TextView tvTitle = accordionView.findViewById(R.id.tvNoticeTitle);
        TextView tvContent = accordionView.findViewById(R.id.tvNoticeContent);
        ImageView ivExpand = accordionView.findViewById(R.id.ivExpandCollapse);
        ImageView ivEdit = accordionView.findViewById(R.id.ivEditNotice);
        ImageView ivDelete = accordionView.findViewById(R.id.ivDeleteNotice);
        LinearLayout llLinkSection = accordionView.findViewById(R.id.llLinkSection);
        TextView tvLink = accordionView.findViewById(R.id.tvNoticeLink);

        tvTitle.setText(notice.getTitle());

        // Enable hyperlinks in content
        tvContent.setText(notice.getContent());
        tvContent.setAutoLinkMask(android.text.util.Linkify.WEB_URLS | android.text.util.Linkify.EMAIL_ADDRESSES);
        tvContent.setLinksClickable(true);

        // Show link section if link exists
        if (notice.hasLink()) {
            llLinkSection.setVisibility(View.VISIBLE);
            tvLink.setText(notice.getLinkUrl());

            // Link click handler - open in browser
            llLinkSection.setOnClickListener(v -> {
                String url = notice.getLinkUrl();
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }
                try {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(browserIntent);
                } catch (Exception e) {
                    Toast.makeText(this, "링크를 열 수 없습니다", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            llLinkSection.setVisibility(View.GONE);
        }

        // Show admin buttons if admin
        if (isAdmin) {
            ivEdit.setVisibility(View.VISIBLE);
            ivDelete.setVisibility(View.VISIBLE);
        }

        // Accordion expand/collapse
        llHeader.setOnClickListener(v -> {
            if (llContent.getVisibility() == View.GONE) {
                llContent.setVisibility(View.VISIBLE);
                divider.setVisibility(View.VISIBLE);
                ivExpand.setRotation(180);
            } else {
                llContent.setVisibility(View.GONE);
                divider.setVisibility(View.GONE);
                ivExpand.setRotation(0);
            }
        });

        // Edit button
        ivEdit.setOnClickListener(v -> showEditNoticeDialog(notice));

        // Delete button
        ivDelete.setOnClickListener(v -> showDeleteNoticeDialog(notice));

        llNoticesContainer.addView(accordionView);
    }

    private void showAddNoticeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_notice, null);

        EditText etTitle = dialogView.findViewById(R.id.etNoticeTitle);
        EditText etContent = dialogView.findViewById(R.id.etNoticeContent);
        EditText etLink = dialogView.findViewById(R.id.etNoticeLink);

        builder.setView(dialogView)
                .setTitle("공지사항 추가")
                .setPositiveButton("추가", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String content = etContent.getText().toString().trim();
                    String link = etLink.getText().toString().trim();

                    if (title.isEmpty() || content.isEmpty()) {
                        Toast.makeText(this, "제목과 내용을 입력해주세요", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int position = notices != null ? notices.size() : 0;
                    Notice newNotice = new Notice(null, title, content, link, position);
                    saveNotice(newNotice);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void showEditNoticeDialog(Notice notice) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_notice, null);

        EditText etTitle = dialogView.findViewById(R.id.etNoticeTitle);
        EditText etContent = dialogView.findViewById(R.id.etNoticeContent);
        EditText etLink = dialogView.findViewById(R.id.etNoticeLink);

        etTitle.setText(notice.getTitle());
        etContent.setText(notice.getContent());
        if (notice.getLinkUrl() != null) {
            etLink.setText(notice.getLinkUrl());
        }

        builder.setView(dialogView)
                .setTitle("공지사항 수정")
                .setPositiveButton("저장", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String content = etContent.getText().toString().trim();
                    String link = etLink.getText().toString().trim();

                    if (title.isEmpty() || content.isEmpty()) {
                        Toast.makeText(this, "제목과 내용을 입력해주세요", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    notice.setTitle(title);
                    notice.setContent(content);
                    notice.setLinkUrl(link);
                    saveNotice(notice);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void showDeleteNoticeDialog(Notice notice) {
        new AlertDialog.Builder(this)
                .setTitle("공지사항 삭제")
                .setMessage("이 공지사항을 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> deleteNotice(notice))
                .setNegativeButton("취소", null)
                .show();
    }

    private void saveNotice(Notice notice) {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.saveNotice(notice, new FirebaseManager.NoticeCallback() {
            @Override
            public void onSuccess(Notice savedNotice) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ClubMainActivity.this, "저장 완료", Toast.LENGTH_SHORT).show();
                loadNotices();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ClubMainActivity.this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteNotice(Notice notice) {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.deleteNotice(notice.getId(), new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ClubMainActivity.this, "삭제 완료", Toast.LENGTH_SHORT).show();
                loadNotices();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ClubMainActivity.this, "삭제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ========================================
    // Banner Methods
    // ========================================

    private void setupBannerViewPager() {
        bannerAdapter = new BannerAdapter(this);
        vpBanners.setAdapter(bannerAdapter);

        // Set banner click listener
        bannerAdapter.setOnBannerClickListener(new BannerAdapter.OnBannerClickListener() {
            @Override
            public void onBannerClick(Banner banner) {
                openBannerLink(banner);
            }

            @Override
            public void onBannerLongClick(Banner banner) {
                if (isAdmin) {
                    showEditBannerDialog(banner);
                }
            }
        });

        // Page change callback for indicator
        vpBanners.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateIndicator(position);
                // Reset auto-slide timer when user swipes
                resetAutoSlide();
            }
        });

        // Initialize auto-slide handler
        autoSlideHandler = new Handler(Looper.getMainLooper());
        autoSlideRunnable = () -> {
            if (bannerAdapter.getItemCount() > 1 && isAutoSliding) {
                int nextPosition = (vpBanners.getCurrentItem() + 1) % bannerAdapter.getItemCount();
                vpBanners.setCurrentItem(nextPosition, true);
            }
        };
    }

    private void loadBanner() {
        // First load slide interval setting
        firebaseManager.getBannerSlideInterval(new FirebaseManager.BannerSettingsCallback() {
            @Override
            public void onSuccess(long interval) {
                bannerSlideInterval = interval;
                loadBanners();
            }

            @Override
            public void onFailure(Exception e) {
                bannerSlideInterval = 3000L;
                loadBanners();
            }
        });
    }

    private void loadBanners() {
        String currentClubId = getClubId();

        // 동아리별 배너 (clubs/{clubId}/banners) 먼저 로드
        firebaseManager.getClubBanners(currentClubId, new FirebaseManager.BannerListCallback() {
            @Override
            public void onSuccess(List<Banner> clubBannerList) {
                // 동아리 배너가 있으면 사용
                if (clubBannerList != null && !clubBannerList.isEmpty()) {
                    banners = clubBannerList;
                    bannerAdapter.setBanners(banners);
                    setupIndicator();
                    startAutoSlide();
                } else {
                    // 동아리 배너가 없으면 전역 배너 로드
                    loadGlobalBanners();
                }
            }

            @Override
            public void onFailure(Exception e) {
                // 실패 시 전역 배너 로드 시도
                loadGlobalBanners();
            }
        });
    }

    private void loadGlobalBanners() {
        firebaseManager.getBanners(new FirebaseManager.BannerListCallback() {
            @Override
            public void onSuccess(List<Banner> bannerList) {
                banners = bannerList;
                if (banners == null || banners.isEmpty()) {
                    // Add default empty banner
                    Banner defaultBanner = new Banner();
                    defaultBanner.setTitle("배너 제목");
                    defaultBanner.setDescription("관리자가 배너를 등록하면 여기에 표시됩니다.");
                    banners = new java.util.ArrayList<>();
                    banners.add(defaultBanner);
                }
                bannerAdapter.setBanners(banners);
                setupIndicator();
                startAutoSlide();
            }

            @Override
            public void onFailure(Exception e) {
                // 기본 배너 표시
                Banner defaultBanner = new Banner();
                defaultBanner.setTitle("배너 제목");
                defaultBanner.setDescription("배너를 불러올 수 없습니다.");
                banners = new java.util.ArrayList<>();
                banners.add(defaultBanner);
                bannerAdapter.setBanners(banners);
                setupIndicator();
            }
        });
    }

    private void setupIndicator() {
        layoutIndicator.removeAllViews();
        int count = bannerAdapter.getItemCount();

        if (count <= 1) {
            layoutIndicator.setVisibility(View.GONE);
            return;
        }

        layoutIndicator.setVisibility(View.VISIBLE);

        for (int i = 0; i < count; i++) {
            View dot = new View(this);
            int size = (int) (8 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(4, 0, 4, 0);
            dot.setLayoutParams(params);

            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(i == 0 ? 0xFF6200EE : 0xFFBDBDBD);
            dot.setBackground(drawable);

            layoutIndicator.addView(dot);
        }
    }

    private void updateIndicator(int position) {
        int count = layoutIndicator.getChildCount();
        for (int i = 0; i < count; i++) {
            View dot = layoutIndicator.getChildAt(i);
            GradientDrawable drawable = (GradientDrawable) dot.getBackground();
            drawable.setColor(i == position ? 0xFF6200EE : 0xFFBDBDBD);
        }
    }

    private void startAutoSlide() {
        if (bannerAdapter.getItemCount() > 1) {
            autoSlideHandler.removeCallbacks(autoSlideRunnable);
            autoSlideHandler.postDelayed(autoSlideRunnable, bannerSlideInterval);
        }
    }

    private void stopAutoSlide() {
        autoSlideHandler.removeCallbacks(autoSlideRunnable);
    }

    private void resetAutoSlide() {
        stopAutoSlide();
        startAutoSlide();
    }

    private void showAddBannerDialog() {
        showEditBannerDialog(null);
    }

    private void showEditCurrentBannerDialog() {
        int currentPosition = vpBanners.getCurrentItem();
        Banner currentBanner = bannerAdapter.getBannerAt(currentPosition);
        if (currentBanner != null && currentBanner.getId() != null) {
            showEditBannerDialog(currentBanner);
        } else {
            showAddBannerDialog();
        }
    }

    private Banner editingBanner = null;

    private void showEditBannerDialog(Banner banner) {
        editingBanner = banner;
        boolean isNew = (banner == null || banner.getId() == null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_banner, null);

        EditText etTitle = dialogView.findViewById(R.id.etBannerTitle);
        EditText etDescription = dialogView.findViewById(R.id.etBannerDescription);
        EditText etLink = dialogView.findViewById(R.id.etBannerLink);
        MaterialButton btnChangeImage = dialogView.findViewById(R.id.btnChangeBannerImage);
        ImageView ivPreview = dialogView.findViewById(R.id.ivBannerPreview);

        if (banner != null) {
            etTitle.setText(banner.getTitle());
            etDescription.setText(banner.getDescription());
            etLink.setText(banner.getLinkUrl());

            if (banner.getImageUrl() != null && !banner.getImageUrl().isEmpty()) {
                Glide.with(this)
                        .load(banner.getImageUrl())
                        .centerCrop()
                        .into(ivPreview);
            }
        }

        btnChangeImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            bannerImagePickerLauncher.launch(intent);
        });

        AlertDialog dialog = builder.setView(dialogView)
                .setTitle(isNew ? "배너 추가" : "배너 수정")
                .setPositiveButton("저장", null)
                .setNegativeButton("취소", null)
                .setNeutralButton(isNew ? null : "삭제", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String title = etTitle.getText().toString().trim();
                String description = etDescription.getText().toString().trim();
                String link = etLink.getText().toString().trim();

                if (title.isEmpty()) {
                    Toast.makeText(this, "제목을 입력해주세요", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (editingBanner == null) {
                    editingBanner = new Banner();
                    editingBanner.setPosition(banners != null ? banners.size() : 0);
                }

                editingBanner.setTitle(title);
                editingBanner.setDescription(description);
                editingBanner.setLinkUrl(link);

                saveBanner(editingBanner, isNew);
                dialog.dismiss();
            });

            if (!isNew && dialog.getButton(AlertDialog.BUTTON_NEUTRAL) != null) {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                    new AlertDialog.Builder(this)
                            .setTitle("배너 삭제")
                            .setMessage("이 배너를 삭제하시겠습니까?")
                            .setPositiveButton("삭제", (d, w) -> {
                                deleteBanner(banner);
                                dialog.dismiss();
                            })
                            .setNegativeButton("취소", null)
                            .show();
                });
            }
        });

        dialog.show();
    }

    private void uploadBannerImage(Uri imageUri) {
        progressBar.setVisibility(View.VISIBLE);

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] imageData = baos.toByteArray();

            firebaseManager.uploadBannerImage(imageData, new FirebaseManager.SignatureCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ClubMainActivity.this, "이미지 업로드 성공", Toast.LENGTH_SHORT).show();

                    if (editingBanner == null) {
                        editingBanner = new Banner();
                    }
                    editingBanner.setImageUrl(downloadUrl);
                }

                @Override
                public void onFailure(Exception e) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ClubMainActivity.this, "이미지 업로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (IOException e) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "이미지 처리 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveBanner(Banner banner, boolean isNew) {
        progressBar.setVisibility(View.VISIBLE);

        FirebaseManager.BannerCallback callback = new FirebaseManager.BannerCallback() {
            @Override
            public void onSuccess(Banner savedBanner) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ClubMainActivity.this, "저장 완료", Toast.LENGTH_SHORT).show();
                loadBanners();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ClubMainActivity.this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        if (isNew) {
            firebaseManager.addBanner(banner, callback);
        } else {
            firebaseManager.updateBanner(banner, callback);
        }
    }

    private void deleteBanner(Banner banner) {
        if (banner == null || banner.getId() == null) return;

        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.deleteBanner(banner.getId(), new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ClubMainActivity.this, "삭제 완료", Toast.LENGTH_SHORT).show();
                loadBanners();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ClubMainActivity.this, "삭제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openBannerLink(Banner banner) {
        if (banner == null || banner.getLinkUrl() == null || banner.getLinkUrl().isEmpty()) {
            Toast.makeText(this, "연결된 링크가 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = banner.getLinkUrl();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        } catch (Exception e) {
            Toast.makeText(this, "링크를 열 수 없습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showBannerSpeedDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_banner_speed, null);
        SeekBar seekBar = dialogView.findViewById(R.id.seekBarSpeed);
        TextView tvSpeedValue = dialogView.findViewById(R.id.tvSpeedValue);

        // Convert interval to slider value (1-10 seconds)
        int currentSeconds = (int) (bannerSlideInterval / 1000);
        if (currentSeconds < 1) currentSeconds = 1;
        if (currentSeconds > 10) currentSeconds = 10;
        seekBar.setProgress(currentSeconds - 1);
        tvSpeedValue.setText(currentSeconds + "초");

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvSpeedValue.setText((progress + 1) + "초");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        new AlertDialog.Builder(this)
                .setTitle("배너 슬라이드 속도")
                .setView(dialogView)
                .setPositiveButton("저장", (dialog, which) -> {
                    int seconds = seekBar.getProgress() + 1;
                    long newInterval = seconds * 1000L;
                    saveBannerSlideInterval(newInterval);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void saveBannerSlideInterval(long intervalMs) {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.saveBannerSlideInterval(intervalMs, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                bannerSlideInterval = intervalMs;
                resetAutoSlide();
                Toast.makeText(ClubMainActivity.this,
                        "슬라이드 속도가 " + (intervalMs / 1000) + "초로 설정되었습니다", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ClubMainActivity.this, "설정 저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ========================================
    // Budget Methods
    // ========================================

    private void displayBudget() {
        if (currentClub == null) {
            // 기본값 표시
            updateBudgetUI(0, 0);
            return;
        }

        long currentBudget = currentClub.getCurrentBudget();
        long totalBudget = currentClub.getTotalBudget();
        updateBudgetUI(currentBudget, totalBudget);
    }

    private void updateBudgetUI(long currentBudget, long totalBudget) {
        // 금액 포맷팅
        java.text.NumberFormat numberFormat = java.text.NumberFormat.getNumberInstance(java.util.Locale.KOREA);
        tvCurrentBudget.setText(numberFormat.format(currentBudget));
        tvTotalBudget.setText(numberFormat.format(totalBudget) + "원");

        // 퍼센트 계산
        final int percent;
        if (totalBudget > 0) {
            percent = (int) ((currentBudget * 100) / totalBudget);
        } else {
            percent = 0;
        }
        tvBudgetPercent.setText(percent + "%");

        // 프로그래스바 너비 조절
        viewBudgetProgress.post(() -> {
            int parentWidth = ((View) viewBudgetProgress.getParent()).getWidth();
            int progressWidth = (int) (parentWidth * percent / 100.0f);
            android.view.ViewGroup.LayoutParams params = viewBudgetProgress.getLayoutParams();
            params.width = progressWidth;
            viewBudgetProgress.setLayoutParams(params);
        });

        // 상태 텍스트 설정
        if (percent >= 50) {
            tvBudgetStatus.setText("잔액이 충분합니다");
            tvBudgetStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_green_dark));
        } else if (percent >= 20) {
            tvBudgetStatus.setText("잔액이 부족해지고 있습니다");
            tvBudgetStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_orange_dark));
        } else {
            tvBudgetStatus.setText("잔액이 부족합니다");
            tvBudgetStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_red_dark));
        }
    }

    private void showEditBudgetDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_budget, null);
        EditText etCurrentBudget = dialogView.findViewById(R.id.etCurrentBudget);
        EditText etTotalBudget = dialogView.findViewById(R.id.etTotalBudget);

        // 현재 값 설정
        long oldCurrentBudget = 0;
        long oldTotalBudget = 0;
        if (currentClub != null) {
            oldCurrentBudget = currentClub.getCurrentBudget();
            oldTotalBudget = currentClub.getTotalBudget();
            etCurrentBudget.setText(String.valueOf(oldCurrentBudget));
            etTotalBudget.setText(String.valueOf(oldTotalBudget));
        } else {
            etCurrentBudget.setText("0");
            etTotalBudget.setText("0");
        }

        final long finalOldCurrentBudget = oldCurrentBudget;
        final long finalOldTotalBudget = oldTotalBudget;

        new AlertDialog.Builder(this)
                .setTitle("공금 설정")
                .setView(dialogView)
                .setPositiveButton("저장", (dialog, which) -> {
                    String currentBudgetStr = etCurrentBudget.getText().toString().trim();
                    String totalBudgetStr = etTotalBudget.getText().toString().trim();

                    if (currentBudgetStr.isEmpty() || totalBudgetStr.isEmpty()) {
                        Toast.makeText(this, "금액을 입력해주세요", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        long currentBudget = Long.parseLong(currentBudgetStr);
                        long totalBudget = Long.parseLong(totalBudgetStr);

                        if (totalBudget < currentBudget) {
                            Toast.makeText(this, "총 예산은 현재 잔액보다 커야 합니다", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // 최고 관리자인 경우 수정 사유 입력 후 수정
                        if (SettingsActivity.isSuperAdminMode(this)) {
                            String oldValue = "현재: " + formatCurrency(finalOldCurrentBudget) + " / 총: " + formatCurrency(finalOldTotalBudget);
                            String newValue = "현재: " + formatCurrency(currentBudget) + " / 총: " + formatCurrency(totalBudget);
                            showEditReasonAndSaveDialog("budget", "공금", oldValue, newValue,
                                    () -> saveBudget(currentBudget, totalBudget));
                        } else {
                            saveBudget(currentBudget, totalBudget);
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "올바른 금액을 입력해주세요", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private String formatCurrency(long amount) {
        java.text.NumberFormat formatter = java.text.NumberFormat.getInstance(java.util.Locale.KOREA);
        return formatter.format(amount) + "원";
    }

    private void saveBudget(long currentBudget, long totalBudget) {
        progressBar.setVisibility(View.VISIBLE);

        String clubId = getClubId();
        if (currentClub == null) {
            currentClub = new com.example.clubmanagement.models.Club(clubId, clubName);
        }

        currentClub.setCurrentBudget(currentBudget);
        currentClub.setTotalBudget(totalBudget);

        firebaseManager.saveClub(currentClub, new FirebaseManager.ClubCallback() {
            @Override
            public void onSuccess(com.example.clubmanagement.models.Club club) {
                progressBar.setVisibility(View.GONE);
                currentClub = club;
                displayBudget();
                Toast.makeText(ClubMainActivity.this, "공금이 저장되었습니다", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ClubMainActivity.this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ========================================
    // Member Count Methods
    // ========================================

    private void displayMemberCount() {
        if (currentClub == null) {
            // intent에서 받은 isCentralClub 값 사용
            updateMemberCountUI(0, isCentralClub);
            return;
        }

        int memberCount = currentClub.getMemberCount();
        // currentClub의 isCentralClub 또는 intent에서 받은 값 사용
        boolean isCentral = currentClub.isCentralClub() || isCentralClub;
        updateMemberCountUI(memberCount, isCentral);
    }

    private void updateMemberCountUI(int memberCount, boolean isCentralClub) {
        // Firebase에서 가져온 인원 제한 값 사용
        // 중앙동아리: 유지 인원, 일반동아리: 등록 인원
        int targetMembers = isCentralClub ? maintainLimit : registerLimit;

        // 현재 인원 수 표시
        tvCurrentMemberCount.setText(String.valueOf(memberCount));

        // 동아리 유형 배지 설정
        if (isCentralClub) {
            tvClubTypeBadge.setText("중앙동아리");
            tvClubTypeBadge.setBackgroundResource(R.drawable.badge_central_club);
            tvRequiredMemberCount.setText(maintainLimit + "명 유지 필요");
        } else {
            tvClubTypeBadge.setText("일반동아리");
            tvClubTypeBadge.setBackgroundResource(R.drawable.badge_general_club);
            tvRequiredMemberCount.setText(registerLimit + "명 필요");
        }

        // 퍼센트 계산 (최대 100%)
        int percent = memberCount >= targetMembers ? 100 : (memberCount * 100 / targetMembers);
        tvMemberPercent.setText(percent + "%");

        // 프로그레스바 너비 및 색상 설정
        viewMemberProgress.post(() -> {
            int parentWidth = ((View) viewMemberProgress.getParent()).getWidth();
            int progressWidth = (int) (parentWidth * percent / 100.0f);
            android.view.ViewGroup.LayoutParams params = viewMemberProgress.getLayoutParams();
            params.width = progressWidth;
            viewMemberProgress.setLayoutParams(params);

            // 색상 설정 (인원에 따라 다르게)
            if (percent >= 100) {
                viewMemberProgress.setBackgroundResource(R.drawable.member_progress_fill);
            } else if (percent >= 75) {
                viewMemberProgress.setBackgroundResource(R.drawable.member_progress_fill_warning);
            } else {
                viewMemberProgress.setBackgroundResource(R.drawable.member_progress_fill_danger);
            }
        });

        // 상태 메시지 설정
        if (isCentralClub) {
            // 중앙동아리인 경우 (유지 인원 이상 필요)
            if (memberCount >= maintainLimit) {
                tvMemberStatus.setText("중앙동아리 유지 가능");
                tvMemberStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_green_dark));
            } else {
                int needed = maintainLimit - memberCount;
                tvMemberStatus.setText(needed + "명 더 필요합니다 (유지 불가 위험)");
                tvMemberStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_red_dark));
            }
        } else {
            // 일반동아리인 경우 (등록 인원 이상 필요)
            if (memberCount >= registerLimit) {
                tvMemberStatus.setText("중앙동아리 등록 가능!");
                tvMemberStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_green_dark));
            } else {
                int needed = registerLimit - memberCount;
                tvMemberStatus.setText(needed + "명 더 모집 시 중앙동아리 등록 가능");
                tvMemberStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_orange_dark));
            }
        }
    }

    // ========================================
    // Club Name Methods
    // ========================================

    private void loadClubInfo() {
        String clubId = getClubId();

        firebaseManager.getClub(clubId, new FirebaseManager.ClubCallback() {
            @Override
            public void onSuccess(com.example.clubmanagement.models.Club club) {
                if (club != null) {
                    currentClub = club;
                    clubName = club.getName();
                    tvClubName.setText(clubName);
                } else {
                    // Club doesn't exist yet, create it with current name
                    currentClub = new com.example.clubmanagement.models.Club(getClubId(), clubName);
                    // 기본 공금 설정 (0원으로 초기화)
                    currentClub.setTotalBudget(0);
                    currentClub.setCurrentBudget(0);
                    // intent에서 받은 isCentralClub 값 사용, 멤버 수 0
                    currentClub.setCentralClub(isCentralClub);
                    currentClub.setMemberCount(0);
                }
                // 공금 표시 업데이트
                displayBudget();
                // 설립일 표시 업데이트
                displayFoundingDate();
                // 실제 멤버 수를 Firebase에서 가져와서 표시
                loadActualMemberCount();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ClubMainActivity.this, "동아리 정보 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                // 오류 시에도 기본값으로 표시
                displayBudget();
                displayMemberCount();
                displayFoundingDate();
            }
        });
    }

    private void loadActualMemberCount() {
        String clubId = getClubId();

        firebaseManager.getClubMembers(clubId, new FirebaseManager.MembersCallback() {
            @Override
            public void onSuccess(java.util.List<com.example.clubmanagement.models.Member> members) {
                int actualMemberCount = members != null ? members.size() : 0;

                // 현재 클럽의 멤버 수 업데이트
                if (currentClub != null) {
                    currentClub.setMemberCount(actualMemberCount);
                }

                // UI 업데이트
                displayMemberCount();
            }

            @Override
            public void onFailure(Exception e) {
                // 실패 시 기존 값으로 표시
                displayMemberCount();
            }
        });
    }

    private void showEditClubNameDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_club_name, null);
        EditText etClubName = dialogView.findViewById(R.id.etClubName);

        etClubName.setText(clubName);
        etClubName.setSelection(clubName.length());

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("저장", (dialog, which) -> {
                    String newClubName = etClubName.getText().toString().trim();
                    if (!newClubName.isEmpty()) {
                        // 최고 관리자인 경우 수정 사유 입력 후 수정
                        if (SettingsActivity.isSuperAdminMode(this)) {
                            showEditReasonAndSaveDialog("name", "동아리명", clubName, newClubName,
                                    () -> saveClubName(newClubName));
                        } else {
                            saveClubName(newClubName);
                        }
                    } else {
                        Toast.makeText(this, "동아리명을 입력해주세요", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void saveClubName(String newClubName) {
        progressBar.setVisibility(View.VISIBLE);

        String clubId = getClubId();
        if (currentClub == null) {
            currentClub = new com.example.clubmanagement.models.Club(clubId, newClubName);
        } else {
            currentClub.setName(newClubName);
        }

        firebaseManager.saveClub(currentClub, new FirebaseManager.ClubCallback() {
            @Override
            public void onSuccess(com.example.clubmanagement.models.Club club) {
                progressBar.setVisibility(View.GONE);
                clubName = newClubName;
                currentClub = club;
                tvClubName.setText(clubName);
                Toast.makeText(ClubMainActivity.this, "동아리명이 변경되었습니다", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ClubMainActivity.this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ========================================
    // Edit Log Methods (최고 관리자 수정 기록)
    // ========================================

    private interface SaveAction {
        void execute();
    }

    private void showEditReasonAndSaveDialog(String fieldName, String fieldDisplayName,
                                              String oldValue, String newValue, SaveAction saveAction) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_request_reason, null);
        TextView tvFieldName = dialogView.findViewById(R.id.tvFieldName);
        TextView tvOldValue = dialogView.findViewById(R.id.tvOldValue);
        TextView tvNewValue = dialogView.findViewById(R.id.tvNewValue);
        TextInputEditText etReason = dialogView.findViewById(R.id.etReason);

        tvFieldName.setText(fieldDisplayName);
        tvOldValue.setText(oldValue != null ? oldValue : "(없음)");
        tvNewValue.setText(newValue != null ? newValue : "(없음)");

        new AlertDialog.Builder(this)
                .setTitle("수정 사유 입력")
                .setView(dialogView)
                .setPositiveButton("수정 완료", (dialog, which) -> {
                    String reason = etReason.getText() != null ? etReason.getText().toString().trim() : "";
                    if (reason.isEmpty()) {
                        Toast.makeText(this, "수정 사유를 입력해주세요", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // 먼저 수정 기록을 저장하고, 그 다음 실제 수정 수행
                    saveEditLog(fieldName, fieldDisplayName, oldValue, newValue, reason, saveAction);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void saveEditLog(String fieldName, String fieldDisplayName,
                              String oldValue, String newValue, String reason, SaveAction saveAction) {
        String requesterId = firebaseManager.getCurrentUserId();
        String requesterEmail = firebaseManager.getCurrentUser() != null ?
                firebaseManager.getCurrentUser().getEmail() : "알 수 없음";

        EditRequest editLog = new EditRequest(
                getClubId(),
                clubName,
                fieldName,
                fieldDisplayName,
                oldValue,
                newValue,
                reason,
                requesterId,
                requesterEmail
        );

        // 수정 기록 저장
        firebaseManager.createEditRequest(editLog, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                // 수정 기록 저장 성공 후 실제 수정 수행
                saveAction.execute();
            }

            @Override
            public void onFailure(Exception e) {
                // 수정 기록 저장 실패해도 수정은 수행
                Toast.makeText(ClubMainActivity.this,
                        "수정 기록 저장 실패 (수정은 진행됩니다)", Toast.LENGTH_SHORT).show();
                saveAction.execute();
            }
        });
    }

    // ========================================
    // Founding Date Methods
    // ========================================

    private void displayFoundingDate() {
        if (currentClub == null || currentClub.getFoundedAt() == null) {
            updateFoundingDateUI(null);
            return;
        }

        updateFoundingDateUI(currentClub.getFoundedAt());
    }

    private void updateFoundingDateUI(Timestamp foundedAt) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy.MM.dd", java.util.Locale.KOREA);

        if (foundedAt == null) {
            tvFoundingDate.setText("미설정");
            tvDaysSinceFounding.setText("0");
            tvFoundingPercent.setText("0%");
            tvFoundingStatus.setText("설립일을 설정해주세요");
            tvFoundingStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.darker_gray));

            // Progress bar를 0으로 설정
            viewFoundingProgress.post(() -> {
                android.view.ViewGroup.LayoutParams params = viewFoundingProgress.getLayoutParams();
                params.width = 0;
                viewFoundingProgress.setLayoutParams(params);
            });
            return;
        }

        // 설립일 표시
        tvFoundingDate.setText(sdf.format(foundedAt.toDate()));

        // 경과 일수 계산
        long daysSinceFounding = currentClub.getDaysSinceFounding();
        tvDaysSinceFounding.setText(String.valueOf(daysSinceFounding));

        // 퍼센트 계산 (180일 기준, 최대 100%)
        int minDays = com.example.clubmanagement.models.Club.CENTRAL_CLUB_MIN_DAYS;
        int percent = daysSinceFounding >= minDays ? 100 : (int) ((daysSinceFounding * 100) / minDays);
        tvFoundingPercent.setText(percent + "%");

        // 프로그레스바 너비 설정
        viewFoundingProgress.post(() -> {
            int parentWidth = ((View) viewFoundingProgress.getParent()).getWidth();
            int progressWidth = (int) (parentWidth * percent / 100.0f);
            android.view.ViewGroup.LayoutParams params = viewFoundingProgress.getLayoutParams();
            params.width = progressWidth;
            viewFoundingProgress.setLayoutParams(params);
        });

        // 상태 메시지 설정
        if (currentClub.canApplyForCentralByDate()) {
            tvFoundingStatus.setText("중앙동아리 신청 가능 (6개월 이상 경과)");
            tvFoundingStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_green_dark));
        } else {
            long daysRemaining = currentClub.getDaysUntilCentralEligible();
            tvFoundingStatus.setText(daysRemaining + "일 후 중앙동아리 신청 가능");
            tvFoundingStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_orange_dark));
        }
    }

    private void showEditFoundingDateDialog() {
        Calendar calendar = Calendar.getInstance();

        // 현재 설립일이 있으면 그 날짜로 초기화
        String oldDateStr = "미설정";
        if (currentClub != null && currentClub.getFoundedAt() != null) {
            calendar.setTime(currentClub.getFoundedAt().toDate());
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy.MM.dd", java.util.Locale.KOREA);
            oldDateStr = sdf.format(currentClub.getFoundedAt().toDate());
        }

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        final String finalOldDateStr = oldDateStr;

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(selectedYear, selectedMonth, selectedDay);

                    // 미래 날짜 검증
                    if (selectedDate.getTime().after(new Date())) {
                        Toast.makeText(this, "설립일은 오늘 이전이어야 합니다", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy.MM.dd", java.util.Locale.KOREA);
                    String newDateStr = sdf.format(selectedDate.getTime());

                    // 최고 관리자인 경우 수정 사유 입력 후 수정
                    if (SettingsActivity.isSuperAdminMode(this)) {
                        showEditReasonAndSaveDialog("foundedAt", "설립일", finalOldDateStr, newDateStr,
                                () -> saveFoundingDate(new Timestamp(selectedDate.getTime())));
                    } else {
                        saveFoundingDate(new Timestamp(selectedDate.getTime()));
                    }
                },
                year, month, day
        );

        // 미래 날짜 선택 불가
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.setTitle("동아리 설립일 선택");
        datePickerDialog.show();
    }

    private void saveFoundingDate(Timestamp foundedAt) {
        progressBar.setVisibility(View.VISIBLE);

        String clubId = getClubId();
        if (currentClub == null) {
            currentClub = new com.example.clubmanagement.models.Club(clubId, clubName);
        }

        currentClub.setFoundedAt(foundedAt);

        firebaseManager.saveClub(currentClub, new FirebaseManager.ClubCallback() {
            @Override
            public void onSuccess(com.example.clubmanagement.models.Club club) {
                progressBar.setVisibility(View.GONE);
                currentClub = club;
                displayFoundingDate();
                Toast.makeText(ClubMainActivity.this, "설립일이 저장되었습니다", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ClubMainActivity.this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ========================================
    // Calendar / Schedule Methods
    // ========================================

    private void setupCalendar() {
        // 현재 월 표시
        updateMonthDisplay();

        // 캘린더 어댑터 설정
        calendarAdapter = new CalendarAdapter(this,
                displayedCalendar.get(Calendar.YEAR),
                displayedCalendar.get(Calendar.MONTH));
        gridCalendar.setAdapter(calendarAdapter);

        // 날짜 클릭 리스너
        calendarAdapter.setOnDateClickListener(new CalendarAdapter.OnDateClickListener() {
            @Override
            public void onDateClick(int year, int month, int day) {
                // 선택된 날짜의 일정 보여주기
                showSchedulesForDate(year, month, day);
            }

            @Override
            public void onDateLongClick(int year, int month, int day) {
                // 관리자인 경우 일정 추가 다이얼로그 표시
                if (isAdmin) {
                    showAddScheduleDialog(year, month, day);
                }
            }
        });

        // 이전 달 버튼
        ivPrevMonth.setOnClickListener(v -> {
            displayedCalendar.add(Calendar.MONTH, -1);
            updateMonthDisplay();
            calendarAdapter.setMonth(
                    displayedCalendar.get(Calendar.YEAR),
                    displayedCalendar.get(Calendar.MONTH));
            calendarAdapter.setScheduledDays(schedules);
        });

        // 다음 달 버튼
        ivNextMonth.setOnClickListener(v -> {
            displayedCalendar.add(Calendar.MONTH, 1);
            updateMonthDisplay();
            calendarAdapter.setMonth(
                    displayedCalendar.get(Calendar.YEAR),
                    displayedCalendar.get(Calendar.MONTH));
            calendarAdapter.setScheduledDays(schedules);
        });

        // 일정 추가 버튼 (관리자용)
        ivAddSchedule.setOnClickListener(v -> {
            Calendar today = Calendar.getInstance();
            showAddScheduleDialog(
                    today.get(Calendar.YEAR),
                    today.get(Calendar.MONTH),
                    today.get(Calendar.DAY_OF_MONTH));
        });
    }

    private void updateMonthDisplay() {
        int year = displayedCalendar.get(Calendar.YEAR);
        int month = displayedCalendar.get(Calendar.MONTH) + 1; // 0-indexed
        tvCurrentMonth.setText(year + "년 " + month + "월");
    }

    private void loadSchedules() {
        String currentClubId = getClubId();

        firebaseManager.getSchedules(currentClubId, new FirebaseManager.ScheduleListCallback() {
            @Override
            public void onSuccess(List<Schedule> loadedSchedules) {
                schedules = loadedSchedules;

                // 캘린더에 일정 표시
                if (calendarAdapter != null) {
                    calendarAdapter.setScheduledDays(schedules);
                }

                // D-day 목록 업데이트
                updateDdayDisplay();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ClubMainActivity.this, "일정 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateDdayDisplay() {
        // D-day 컨테이너 초기화 (tvNoSchedule 제외)
        for (int i = llDdayContainer.getChildCount() - 1; i >= 0; i--) {
            View child = llDdayContainer.getChildAt(i);
            if (child.getId() != R.id.tvNoSchedule) {
                llDdayContainer.removeViewAt(i);
            }
        }

        // 다가오는 일정 필터링 (오늘 이후만, 최대 5개)
        List<Schedule> upcomingSchedules = new java.util.ArrayList<>();
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        for (Schedule schedule : schedules) {
            if (schedule.getDday() >= 0) { // 오늘 이후 또는 오늘
                upcomingSchedules.add(schedule);
            }
        }

        // D-day 기준 정렬 (가까운 순)
        upcomingSchedules.sort((s1, s2) -> Integer.compare(s1.getDday(), s2.getDday()));

        // 최대 5개만 표시
        int count = Math.min(upcomingSchedules.size(), 5);

        if (count == 0) {
            tvNoSchedule.setVisibility(View.VISIBLE);
        } else {
            tvNoSchedule.setVisibility(View.GONE);

            for (int i = 0; i < count; i++) {
                Schedule schedule = upcomingSchedules.get(i);
                View ddayItem = LayoutInflater.from(this).inflate(R.layout.item_dday, llDdayContainer, false);

                TextView tvDday = ddayItem.findViewById(R.id.tvDday);
                TextView tvScheduleTitle = ddayItem.findViewById(R.id.tvScheduleTitle);
                TextView tvScheduleDate = ddayItem.findViewById(R.id.tvScheduleDate);
                ImageView ivDeleteSchedule = ddayItem.findViewById(R.id.ivDeleteSchedule);

                // D-day 표시
                String ddayText = schedule.getDdayString();
                tvDday.setText(ddayText);

                // D-day 색상 설정
                int ddayValue = schedule.getDday();
                if (ddayValue == 0) {
                    tvDday.setBackgroundResource(R.drawable.dday_badge_today);
                } else if (ddayValue <= 3) {
                    tvDday.setBackgroundResource(R.drawable.dday_badge_urgent);
                } else {
                    tvDday.setBackgroundResource(R.drawable.dday_badge_background);
                }

                tvScheduleTitle.setText(schedule.getTitle());
                tvScheduleDate.setText(schedule.getDateString());

                // 관리자용 삭제 버튼
                if (isAdmin) {
                    ivDeleteSchedule.setVisibility(View.VISIBLE);
                    ivDeleteSchedule.setOnClickListener(v -> {
                        showDeleteScheduleDialog(schedule);
                    });
                }

                // D-day 아이템 클릭시 상세 정보 표시
                ddayItem.setOnClickListener(v -> {
                    showScheduleDetailDialog(schedule);
                });

                // tvNoSchedule 앞에 추가
                int insertIndex = llDdayContainer.indexOfChild(tvNoSchedule);
                llDdayContainer.addView(ddayItem, insertIndex);
            }
        }
    }

    private void showSchedulesForDate(int year, int month, int day) {
        // 해당 날짜의 일정 찾기
        List<Schedule> dateSchedules = new java.util.ArrayList<>();
        for (Schedule schedule : schedules) {
            if (schedule.getYear() == year &&
                    schedule.getMonth() == month &&
                    schedule.getDay() == day) {
                dateSchedules.add(schedule);
            }
        }

        if (dateSchedules.isEmpty()) {
            Toast.makeText(this, (month + 1) + "월 " + day + "일에 등록된 일정이 없습니다", Toast.LENGTH_SHORT).show();
        } else {
            // 일정이 있으면 목록 다이얼로그 표시
            StringBuilder message = new StringBuilder();
            for (Schedule schedule : dateSchedules) {
                message.append("• ").append(schedule.getTitle());
                if (schedule.getDescription() != null && !schedule.getDescription().isEmpty()) {
                    message.append("\n  ").append(schedule.getDescription());
                }
                message.append("\n\n");
            }

            new AlertDialog.Builder(this)
                    .setTitle((month + 1) + "월 " + day + "일 일정")
                    .setMessage(message.toString().trim())
                    .setPositiveButton("확인", null)
                    .show();
        }
    }

    private void showAddScheduleDialog(int year, int month, int day) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_schedule, null);

        com.google.android.material.textfield.TextInputEditText etTitle =
                dialogView.findViewById(R.id.etScheduleTitle);
        com.google.android.material.textfield.TextInputEditText etDescription =
                dialogView.findViewById(R.id.etScheduleDescription);
        TextView tvSelectedDate = dialogView.findViewById(R.id.tvSelectedDate);
        ImageView ivSelectDate = dialogView.findViewById(R.id.ivSelectDate);

        // 선택된 날짜 저장용
        final int[] selectedDate = {year, month, day};
        tvSelectedDate.setText(year + "." + (month + 1) + "." + day);

        // 날짜 선택 버튼
        View.OnClickListener dateSelectListener = v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    (view, y, m, d) -> {
                        selectedDate[0] = y;
                        selectedDate[1] = m;
                        selectedDate[2] = d;
                        tvSelectedDate.setText(y + "." + (m + 1) + "." + d);
                    },
                    selectedDate[0], selectedDate[1], selectedDate[2]);
            datePickerDialog.show();
        };

        tvSelectedDate.setOnClickListener(dateSelectListener);
        ivSelectDate.setOnClickListener(dateSelectListener);

        new AlertDialog.Builder(this)
                .setTitle("일정 추가")
                .setView(dialogView)
                .setPositiveButton("추가", (dialog, which) -> {
                    String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
                    String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";

                    if (title.isEmpty()) {
                        Toast.makeText(this, "일정 제목을 입력하세요", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    addSchedule(title, description, selectedDate[0], selectedDate[1], selectedDate[2]);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void addSchedule(String title, String description, int year, int month, int day) {
        progressBar.setVisibility(View.VISIBLE);

        // 날짜 생성
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        com.google.firebase.Timestamp eventDate = new com.google.firebase.Timestamp(cal.getTime());

        Schedule schedule = new Schedule(getClubId(), title, eventDate);
        schedule.setDescription(description);
        schedule.setCreatedBy(firebaseManager.getCurrentUserId());

        firebaseManager.addSchedule(schedule, new FirebaseManager.ScheduleCallback() {
            @Override
            public void onSuccess(Schedule addedSchedule) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ClubMainActivity.this, "일정이 추가되었습니다", Toast.LENGTH_SHORT).show();
                loadSchedules(); // 다시 로드
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ClubMainActivity.this, "일정 추가 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteScheduleDialog(Schedule schedule) {
        new AlertDialog.Builder(this)
                .setTitle("일정 삭제")
                .setMessage("'" + schedule.getTitle() + "' 일정을 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> {
                    deleteSchedule(schedule);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void deleteSchedule(Schedule schedule) {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.deleteSchedule(schedule.getId(), new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ClubMainActivity.this, "일정이 삭제되었습니다", Toast.LENGTH_SHORT).show();
                loadSchedules(); // 다시 로드
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ClubMainActivity.this, "일정 삭제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showScheduleDetailDialog(Schedule schedule) {
        StringBuilder message = new StringBuilder();
        message.append("날짜: ").append(schedule.getDateString()).append("\n");
        message.append("D-day: ").append(schedule.getDdayString()).append("\n");

        if (schedule.getDescription() != null && !schedule.getDescription().isEmpty()) {
            message.append("\n").append(schedule.getDescription());
        }

        new AlertDialog.Builder(this)
                .setTitle(schedule.getTitle())
                .setMessage(message.toString())
                .setPositiveButton("확인", null)
                .show();
    }

    // ========================================
    // Birthday Methods
    // ========================================

    private void checkBirthdayMembers() {
        String currentClubId = getClubId();

        firebaseManager.getClubMembers(currentClubId, new FirebaseManager.MembersCallback() {
            @Override
            public void onSuccess(List<com.example.clubmanagement.models.Member> members) {
                String currentUserId = firebaseManager.getCurrentUserId();
                Calendar today = Calendar.getInstance();
                int todayMonth = today.get(Calendar.MONTH) + 1;
                int todayDay = today.get(Calendar.DAY_OF_MONTH);

                List<com.example.clubmanagement.models.Member> birthdayMembers = new java.util.ArrayList<>();

                // 오늘 생일인 멤버 찾기
                for (com.example.clubmanagement.models.Member member : members) {
                    if (member.getBirthMonth() == todayMonth && member.getBirthDay() == todayDay) {
                        birthdayMembers.add(member);
                    }
                }

                if (!birthdayMembers.isEmpty()) {
                    boolean isMyBirthday = false;
                    StringBuilder otherBirthdayNames = new StringBuilder();

                    for (com.example.clubmanagement.models.Member birthdayMember : birthdayMembers) {
                        if (birthdayMember.getUserId() != null && birthdayMember.getUserId().equals(currentUserId)) {
                            isMyBirthday = true;
                        } else {
                            if (otherBirthdayNames.length() > 0) {
                                otherBirthdayNames.append(", ");
                            }
                            otherBirthdayNames.append(birthdayMember.getName() != null ? birthdayMember.getName() : "부원");
                        }
                    }

                    // 내 생일이면 축하 메시지
                    if (isMyBirthday) {
                        showBirthdayToast("생일 축하합니다! 오늘 하루도 행복하세요!");
                    }

                    // 다른 멤버의 생일이 있으면 알림
                    if (otherBirthdayNames.length() > 0) {
                        new android.os.Handler().postDelayed(() -> {
                            showBirthdayToast("오늘은 " + otherBirthdayNames + "님의 생일입니다! 축하해주세요!");
                        }, isMyBirthday ? 3000 : 0); // 내 생일 토스트 후 3초 딜레이
                    }

                    // 생일 공지사항 자동 생성 확인 및 생성
                    createBirthdayNoticeIfNeeded(birthdayMembers);
                }
            }

            @Override
            public void onFailure(Exception e) {
                // 조용히 실패 처리
            }
        });
    }

    private void showBirthdayToast(String message) {
        LayoutInflater inflater = getLayoutInflater();
        View toastView = inflater.inflate(R.layout.toast_birthday, null);

        TextView tvMessage = toastView.findViewById(R.id.tvBirthdayMessage);
        tvMessage.setText(message);

        Toast toast = new Toast(this);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(toastView);
        toast.setGravity(android.view.Gravity.CENTER, 0, 0);
        toast.show();
    }

    private void createBirthdayNoticeIfNeeded(List<com.example.clubmanagement.models.Member> birthdayMembers) {
        // 이미 오늘 생일 공지가 있는지 확인
        String todayDate = new java.text.SimpleDateFormat("yyyy.MM.dd", java.util.Locale.KOREA).format(new java.util.Date());
        String birthdayNoticePrefix = "[생일] ";

        boolean birthdayNoticeExists = false;
        if (notices != null) {
            for (Notice notice : notices) {
                if (notice.getTitle() != null && notice.getTitle().startsWith(birthdayNoticePrefix) &&
                        notice.getCreatedAt() != null) {
                    String noticeDate = new java.text.SimpleDateFormat("yyyy.MM.dd", java.util.Locale.KOREA)
                            .format(notice.getCreatedAt().toDate());
                    if (noticeDate.equals(todayDate)) {
                        birthdayNoticeExists = true;
                        break;
                    }
                }
            }
        }

        // 생일 공지가 없으면 생성 (관리자 모드일 때만 자동 생성)
        if (!birthdayNoticeExists && isAdmin) {
            StringBuilder names = new StringBuilder();
            for (com.example.clubmanagement.models.Member member : birthdayMembers) {
                if (names.length() > 0) {
                    names.append(", ");
                }
                names.append(member.getName() != null ? member.getName() : "부원");
            }

            String title = birthdayNoticePrefix + names + "님의 생일입니다!";
            String content = "오늘은 " + names + "님의 생일입니다. 따뜻한 축하 메시지를 보내주세요!";

            createBirthdayNotice(title, content);
        }
    }

    private void createBirthdayNotice(String title, String content) {
        Notice birthdayNotice = new Notice();
        birthdayNotice.setTitle(title);
        birthdayNotice.setContent(content);
        birthdayNotice.setCreatedAt(com.google.firebase.Timestamp.now());

        String currentClubId = getClubId();

        firebaseManager.getDb().collection("clubs").document(currentClubId)
                .collection("notices")
                .add(birthdayNotice)
                .addOnSuccessListener(documentReference -> {
                    // 공지 추가 성공 - 목록 새로고침
                    loadNotices();
                })
                .addOnFailureListener(e -> {
                    // 실패시 조용히 처리
                });
    }
}
