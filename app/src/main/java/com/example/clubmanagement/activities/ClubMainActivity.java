package com.example.clubmanagement.activities;

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
    private ImageView ivEditBanner;
    private ImageView ivBannerImage;
    private TextView tvBannerTitle;
    private TextView tvBannerDescription;
    private ProgressBar progressBar;

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
        checkAdminStatus();
        loadAllData();
        setupListeners();
    }

    private void initViews() {
        tvClubName = findViewById(R.id.tvClubName);
        ivEditClubName = findViewById(R.id.ivEditClubName);
        ivSettings = findViewById(R.id.ivSettings);
        llNoticesContainer = findViewById(R.id.llNoticesContainer);
        llLinkButtonsContainer = findViewById(R.id.llLinkButtonsContainer);
        btnAddNotice = findViewById(R.id.btnAddNotice);
        btnAddLink = findViewById(R.id.btnAddLink);
        ivEditBanner = findViewById(R.id.ivEditBanner);
        ivBannerImage = findViewById(R.id.ivBannerImage);
        tvBannerTitle = findViewById(R.id.tvBannerTitle);
        tvBannerDescription = findViewById(R.id.tvBannerDescription);
        progressBar = findViewById(R.id.progressBar);

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
    }

    private void openClubSettings() {
        Intent intent = new Intent(ClubMainActivity.this, ClubSettingsActivity.class);
        intent.putExtra("club_name", clubName);
        intent.putExtra("club_id", getClubId());
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
        tvContent.setText(notice.getContent());

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
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ClubMainActivity.this, "동아리 정보 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
}
