package com.example.clubmanagement.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;

import java.util.Calendar;
import java.util.Date;

import com.bumptech.glide.Glide;
import com.example.clubmanagement.R;
import com.example.clubmanagement.models.Banner;
import com.example.clubmanagement.models.LinkButton;
import com.example.clubmanagement.models.Notice;
import com.example.clubmanagement.models.UserData;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.android.material.button.MaterialButton;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class ClubMainActivity extends AppCompatActivity {

    private FirebaseManager firebaseManager;
    private boolean isAdmin = false;
    private UserData currentUserData;

    // UI Components
    private TextView tvClubName;
    private ImageView ivEditClubName;
    private ImageView ivSettings;
    private LinearLayout llNoticesContainer;
    private LinearLayout llLinkButtonsContainer;
    private MaterialButton btnAddNotice;
    private MaterialButton btnAddLink;
    private MaterialButton btnClubInfo;
    private ImageView ivEditBanner;
    private ImageView ivBannerImage;
    private TextView tvBannerTitle;
    private TextView tvBannerDescription;
    private ProgressBar progressBar;

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
    private androidx.cardview.widget.CardView cardMemberCount;

    // Founding Date UI Components
    private TextView tvFoundingDate;
    private TextView tvDaysSinceFounding;
    private View viewFoundingProgress;
    private TextView tvFoundingPercent;
    private TextView tvFoundingStatus;
    private ImageView ivEditFoundingDate;
    private androidx.cardview.widget.CardView cardFoundingDate;

    // Data
    private List<Notice> notices;
    private List<LinkButton> linkButtons;
    private Banner currentBanner;
    private String clubName;
    private com.example.clubmanagement.models.Club currentClub;

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

        // Get club name from intent
        clubName = getIntent().getStringExtra("club_name");
        if (clubName == null) {
            clubName = "동아리";
        }

        initViews();
        setupImagePickerLauncher();
        setupBackPressedCallback();
        checkAdminStatus();
        loadAllData();
        setupListeners();
    }

    private void setupBackPressedCallback() {
        // 중앙동아리 메인 화면에서 뒤로가기 방지
        // 앱을 종료하거나 아무 동작도 하지 않음
        // 사용자가 원하면 홈 버튼으로 앱을 나갈 수 있음
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                moveTaskToBack(true);
            }
        });
    }

    private void initViews() {
        TextView tvJoinDate = findViewById(R.id.tvJoinDate);
        tvClubName = findViewById(R.id.tvClubName);
        ivEditClubName = findViewById(R.id.ivEditClubName);
        ivSettings = findViewById(R.id.ivSettings);

        // Set join date (임시로 현재 날짜 표시)
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy.MM.dd", java.util.Locale.KOREA);
        String currentDate = sdf.format(new java.util.Date());
        tvJoinDate.setText("가입일: " + currentDate);
        llNoticesContainer = findViewById(R.id.llNoticesContainer);
        llLinkButtonsContainer = findViewById(R.id.llLinkButtonsContainer);
        btnAddNotice = findViewById(R.id.btnAddNotice);
        btnAddLink = findViewById(R.id.btnAddLink);
        btnClubInfo = findViewById(R.id.btnClubInfo);
        ivEditBanner = findViewById(R.id.ivEditBanner);
        ivBannerImage = findViewById(R.id.ivBannerImage);
        tvBannerTitle = findViewById(R.id.tvBannerTitle);
        tvBannerDescription = findViewById(R.id.tvBannerDescription);
        progressBar = findViewById(R.id.progressBar);

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
        cardMemberCount = findViewById(R.id.cardMemberCount);

        // Founding date views
        tvFoundingDate = findViewById(R.id.tvFoundingDate);
        tvDaysSinceFounding = findViewById(R.id.tvDaysSinceFounding);
        viewFoundingProgress = findViewById(R.id.viewFoundingProgress);
        tvFoundingPercent = findViewById(R.id.tvFoundingPercent);
        tvFoundingStatus = findViewById(R.id.tvFoundingStatus);
        ivEditFoundingDate = findViewById(R.id.ivEditFoundingDate);
        cardFoundingDate = findViewById(R.id.cardFoundingDate);

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
                        ivEditClubName.setVisibility(View.VISIBLE);
                        btnAddNotice.setVisibility(View.VISIBLE);
                        btnAddLink.setVisibility(View.VISIBLE);
                        ivEditBanner.setVisibility(View.VISIBLE);
                        ivEditBudget.setVisibility(View.VISIBLE);
                        ivEditFoundingDate.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                isAdmin = false;
            }
        });
    }

    private String getClubId() {
        // Generate club ID from club name for now
        // TODO: Use actual club ID from database
        return clubName.replaceAll("\\s+", "_").toLowerCase();
    }

    private void loadAllData() {
        loadClubInfo();
        loadNotices();
        loadLinkButtons();
        loadBanner();
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
        btnAddLink.setOnClickListener(v -> showAddLinkButtonDialog());
        ivEditBanner.setOnClickListener(v -> showEditBannerDialog());
        btnClubInfo.setOnClickListener(v -> openClubInfo());
        ivEditBudget.setOnClickListener(v -> showEditBudgetDialog());
        ivEditFoundingDate.setOnClickListener(v -> showEditFoundingDateDialog());

        // Budget card click - open budget history
        cardBudget.setOnClickListener(v -> openBudgetHistory());

        // Member count card click - open member management (TODO: implement member management activity)
        cardMemberCount.setOnClickListener(v -> {
            Toast.makeText(this, "부원 관리 기능은 준비 중입니다", Toast.LENGTH_SHORT).show();
        });

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
        Intent intent = new Intent(ClubMainActivity.this, ClubSettingsActivity.class);
        intent.putExtra("club_name", clubName);
        intent.putExtra("club_id", getClubId());
        startActivity(intent);
    }

    private void openClubInfo() {
        Intent intent = new Intent(ClubMainActivity.this, ClubInfoActivity.class);
        intent.putExtra("club_id", getClubId());
        intent.putExtra("club_name", clubName);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload admin status when returning from settings
        checkAdminStatus();
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

        tvTitle.setText(notice.getTitle());

        // Enable hyperlinks in content
        tvContent.setText(notice.getContent());
        tvContent.setAutoLinkMask(android.text.util.Linkify.WEB_URLS | android.text.util.Linkify.EMAIL_ADDRESSES);
        tvContent.setLinksClickable(true);

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

        builder.setView(dialogView)
                .setTitle("공지사항 추가")
                .setPositiveButton("추가", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String content = etContent.getText().toString().trim();

                    if (title.isEmpty() || content.isEmpty()) {
                        Toast.makeText(this, "제목과 내용을 입력해주세요", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int position = notices != null ? notices.size() : 0;
                    Notice newNotice = new Notice(null, title, content, position);
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

        etTitle.setText(notice.getTitle());
        etContent.setText(notice.getContent());

        builder.setView(dialogView)
                .setTitle("공지사항 수정")
                .setPositiveButton("저장", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String content = etContent.getText().toString().trim();

                    if (title.isEmpty() || content.isEmpty()) {
                        Toast.makeText(this, "제목과 내용을 입력해주세요", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    notice.setTitle(title);
                    notice.setContent(content);
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
    // Link Button Methods
    // ========================================

    private void loadLinkButtons() {
        firebaseManager.getLinkButtons(new FirebaseManager.LinkButtonListCallback() {
            @Override
            public void onSuccess(List<LinkButton> linkButtonList) {
                linkButtons = linkButtonList;
                displayLinkButtons();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ClubMainActivity.this, "링크 버튼 로드 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayLinkButtons() {
        llLinkButtonsContainer.removeAllViews();

        if (linkButtons == null || linkButtons.isEmpty()) {
            return;
        }

        for (LinkButton linkButton : linkButtons) {
            addLinkButton(linkButton);
        }
    }

    private void addLinkButton(LinkButton linkButton) {
        MaterialButton button = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        button.setText(linkButton.getLabel());
        button.setTextSize(14);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        params.setMargins(8, 0, 8, 0);
        button.setLayoutParams(params);

        button.setOnClickListener(v -> {
            // Open URL
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(linkButton.getUrl()));
            startActivity(browserIntent);
        });

        button.setOnLongClickListener(v -> {
            if (isAdmin) {
                showEditLinkButtonDialog(linkButton);
                return true;
            }
            return false;
        });

        llLinkButtonsContainer.addView(button);
    }

    private void showAddLinkButtonDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_link_button, null);

        EditText etLabel = dialogView.findViewById(R.id.etLinkLabel);
        EditText etUrl = dialogView.findViewById(R.id.etLinkUrl);

        builder.setView(dialogView)
                .setTitle("링크 버튼 추가")
                .setPositiveButton("추가", (dialog, which) -> {
                    String label = etLabel.getText().toString().trim();
                    String url = etUrl.getText().toString().trim();

                    if (label.isEmpty() || url.isEmpty()) {
                        Toast.makeText(this, "라벨과 URL을 입력해주세요", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int position = linkButtons != null ? linkButtons.size() : 0;
                    LinkButton newButton = new LinkButton(null, label, url, position);
                    saveLinkButton(newButton);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void showEditLinkButtonDialog(LinkButton linkButton) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_link_button, null);

        EditText etLabel = dialogView.findViewById(R.id.etLinkLabel);
        EditText etUrl = dialogView.findViewById(R.id.etLinkUrl);

        etLabel.setText(linkButton.getLabel());
        etUrl.setText(linkButton.getUrl());

        builder.setView(dialogView)
                .setTitle("링크 버튼 수정")
                .setPositiveButton("저장", (dialog, which) -> {
                    String label = etLabel.getText().toString().trim();
                    String url = etUrl.getText().toString().trim();

                    if (label.isEmpty() || url.isEmpty()) {
                        Toast.makeText(this, "라벨과 URL을 입력해주세요", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    linkButton.setLabel(label);
                    linkButton.setUrl(url);
                    saveLinkButton(linkButton);
                })
                .setNeutralButton("삭제", (dialog, which) -> deleteLinkButton(linkButton))
                .setNegativeButton("취소", null)
                .show();
    }

    private void saveLinkButton(LinkButton linkButton) {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.saveLinkButton(linkButton, new FirebaseManager.LinkButtonCallback() {
            @Override
            public void onSuccess(LinkButton savedButton) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ClubMainActivity.this, "저장 완료", Toast.LENGTH_SHORT).show();
                loadLinkButtons();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ClubMainActivity.this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteLinkButton(LinkButton linkButton) {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.deleteLinkButton(linkButton.getId(), new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ClubMainActivity.this, "삭제 완료", Toast.LENGTH_SHORT).show();
                loadLinkButtons();
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

    private void loadBanner() {
        firebaseManager.getBanner(new FirebaseManager.BannerCallback() {
            @Override
            public void onSuccess(Banner banner) {
                currentBanner = banner;
                displayBanner();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ClubMainActivity.this, "배너 로드 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayBanner() {
        if (currentBanner == null) {
            tvBannerTitle.setText("배너 제목");
            tvBannerDescription.setText("관리자가 배너를 등록하면 여기에 표시됩니다.");
            return;
        }

        tvBannerTitle.setText(currentBanner.getTitle());
        tvBannerDescription.setText(currentBanner.getDescription());

        if (currentBanner.getImageUrl() != null && !currentBanner.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(currentBanner.getImageUrl())
                    .centerCrop()
                    .into(ivBannerImage);
        }
    }

    private void showEditBannerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_banner, null);

        EditText etTitle = dialogView.findViewById(R.id.etBannerTitle);
        EditText etDescription = dialogView.findViewById(R.id.etBannerDescription);
        EditText etLink = dialogView.findViewById(R.id.etBannerLink);
        MaterialButton btnChangeImage = dialogView.findViewById(R.id.btnChangeBannerImage);
        ImageView ivPreview = dialogView.findViewById(R.id.ivBannerPreview);

        if (currentBanner != null) {
            etTitle.setText(currentBanner.getTitle());
            etDescription.setText(currentBanner.getDescription());
            etLink.setText(currentBanner.getLinkUrl());

            if (currentBanner.getImageUrl() != null && !currentBanner.getImageUrl().isEmpty()) {
                Glide.with(this)
                        .load(currentBanner.getImageUrl())
                        .centerCrop()
                        .into(ivPreview);
            }
        }

        btnChangeImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            bannerImagePickerLauncher.launch(intent);
        });

        builder.setView(dialogView)
                .setTitle("배너 수정")
                .setPositiveButton("저장", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String description = etDescription.getText().toString().trim();
                    String link = etLink.getText().toString().trim();

                    if (title.isEmpty() || description.isEmpty()) {
                        Toast.makeText(this, "제목과 설명을 입력해주세요", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (currentBanner == null) {
                        currentBanner = new Banner();
                    }

                    currentBanner.setTitle(title);
                    currentBanner.setDescription(description);
                    currentBanner.setLinkUrl(link);

                    saveBanner();
                })
                .setNegativeButton("취소", null)
                .show();
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

                    if (currentBanner == null) {
                        currentBanner = new Banner();
                    }
                    currentBanner.setImageUrl(downloadUrl);
                    displayBanner();
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

    private void saveBanner() {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.saveBanner(currentBanner, new FirebaseManager.BannerCallback() {
            @Override
            public void onSuccess(Banner banner) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ClubMainActivity.this, "저장 완료", Toast.LENGTH_SHORT).show();
                currentBanner = banner;
                displayBanner();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ClubMainActivity.this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
        if (currentClub != null) {
            etCurrentBudget.setText(String.valueOf(currentClub.getCurrentBudget()));
            etTotalBudget.setText(String.valueOf(currentClub.getTotalBudget()));
        } else {
            etCurrentBudget.setText("0");
            etTotalBudget.setText("0");
        }

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

                        saveBudget(currentBudget, totalBudget);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "올바른 금액을 입력해주세요", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("취소", null)
                .show();
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
            updateMemberCountUI(0, true);
            return;
        }

        int memberCount = currentClub.getMemberCount();
        boolean isCentral = currentClub.isCentralClub();
        updateMemberCountUI(memberCount, isCentral);
    }

    private void updateMemberCountUI(int memberCount, boolean isCentralClub) {
        // 중앙동아리: 유지 17명, 일반동아리: 등록 20명
        int maintainMin = com.example.clubmanagement.models.Club.CENTRAL_CLUB_MAINTAIN_MIN_MEMBERS;
        int registerMin = com.example.clubmanagement.models.Club.CENTRAL_CLUB_REGISTER_MIN_MEMBERS;
        int targetMembers = isCentralClub ? maintainMin : registerMin;

        // 현재 인원 수 표시
        tvCurrentMemberCount.setText(String.valueOf(memberCount));

        // 동아리 유형 배지 설정
        if (isCentralClub) {
            tvClubTypeBadge.setText("중앙동아리");
            tvClubTypeBadge.setBackgroundResource(R.drawable.badge_central_club);
            tvRequiredMemberCount.setText(maintainMin + "명 유지 필요");
        } else {
            tvClubTypeBadge.setText("일반동아리");
            tvClubTypeBadge.setBackgroundResource(R.drawable.badge_general_club);
            tvRequiredMemberCount.setText(registerMin + "명 필요");
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
            // 중앙동아리인 경우 (17명 이상 유지 필요)
            if (memberCount >= maintainMin) {
                tvMemberStatus.setText("중앙동아리 유지 가능");
                tvMemberStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_green_dark));
            } else {
                int needed = maintainMin - memberCount;
                tvMemberStatus.setText(needed + "명 더 필요합니다 (유지 불가 위험)");
                tvMemberStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_red_dark));
            }
        } else {
            // 일반동아리인 경우 (20명 이상 등록 가능)
            if (memberCount >= registerMin) {
                tvMemberStatus.setText("중앙동아리 등록 가능!");
                tvMemberStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_green_dark));
            } else {
                int needed = registerMin - memberCount;
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
                    currentClub = new com.example.clubmanagement.models.Club(clubId, clubName);
                    // 기본 공금 설정
                    currentClub.setTotalBudget(500000);
                    currentClub.setCurrentBudget(150000);
                    // 기본 인원 설정 (중앙동아리로 가정)
                    currentClub.setCentralClub(true);
                    currentClub.setMemberCount(15);
                }
                // 공금 표시 업데이트
                displayBudget();
                // 인원 현황 표시 업데이트
                displayMemberCount();
                // 설립일 표시 업데이트
                displayFoundingDate();
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
                        saveClubName(newClubName);
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
        if (currentClub != null && currentClub.getFoundedAt() != null) {
            calendar.setTime(currentClub.getFoundedAt().toDate());
        }

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

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

                    saveFoundingDate(new Timestamp(selectedDate.getTime()));
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
}
