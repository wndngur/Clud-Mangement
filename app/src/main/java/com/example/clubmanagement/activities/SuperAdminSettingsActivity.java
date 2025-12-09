package com.example.clubmanagement.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.example.clubmanagement.BaseActivity;
import com.example.clubmanagement.R;
import com.example.clubmanagement.models.Club;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.android.material.button.MaterialButton;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

public class SuperAdminSettingsActivity extends BaseActivity {

    private ImageView ivBack;
    private ProgressBar progressBar;

    // Club Info Views
    private TextView tvClubName;
    private TextView tvClubType;
    private TextView tvCurrentBudget;
    private TextView tvTotalBudget;
    private TextView tvMemberCount;
    private TextView tvMemberStatus;
    private View viewBudgetProgress;
    private View viewMemberProgress;
    private TextView tvBudgetPercent;
    private TextView tvMemberPercent;

    // Carousel Image Views
    private ImageView ivCarouselPreview;
    private TextView tvCarouselTitle;

    // Buttons
    private MaterialButton btnEditCarouselImage;
    private MaterialButton btnDeleteCarouselImage;
    private MaterialButton btnCancelCentralClub;
    private MaterialButton btnDemoteCentralClubs;

    private FirebaseManager firebaseManager;
    private int pageIndex;
    private String clubName;
    private String clubId;
    private String carouselImageUrl;
    private Club currentClub;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_super_admin_settings);

        // Hide ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        firebaseManager = FirebaseManager.getInstance();

        // Get data from intent
        pageIndex = getIntent().getIntExtra("page_index", 0);
        clubName = getIntent().getStringExtra("club_name");
        clubId = getIntent().getStringExtra("club_id");
        carouselImageUrl = getIntent().getStringExtra("carousel_image_url");

        initViews();
        setupImagePickerLauncher();
        setupListeners();
        loadClubData();
        loadCarouselPreview();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        progressBar = findViewById(R.id.progressBar);

        // Club Info
        tvClubName = findViewById(R.id.tvClubName);
        tvClubType = findViewById(R.id.tvClubType);
        tvCurrentBudget = findViewById(R.id.tvCurrentBudget);
        tvTotalBudget = findViewById(R.id.tvTotalBudget);
        tvMemberCount = findViewById(R.id.tvMemberCount);
        tvMemberStatus = findViewById(R.id.tvMemberStatus);
        viewBudgetProgress = findViewById(R.id.viewBudgetProgress);
        viewMemberProgress = findViewById(R.id.viewMemberProgress);
        tvBudgetPercent = findViewById(R.id.tvBudgetPercent);
        tvMemberPercent = findViewById(R.id.tvMemberPercent);

        // Carousel
        ivCarouselPreview = findViewById(R.id.ivCarouselPreview);
        tvCarouselTitle = findViewById(R.id.tvCarouselTitle);

        // Buttons
        btnEditCarouselImage = findViewById(R.id.btnEditCarouselImage);
        btnDeleteCarouselImage = findViewById(R.id.btnDeleteCarouselImage);
        btnCancelCentralClub = findViewById(R.id.btnCancelCentralClub);
        btnDemoteCentralClubs = findViewById(R.id.btnDemoteCentralClubs);

        // Set club name
        tvClubName.setText(clubName != null ? clubName : "동아리");
    }

    private void setupImagePickerLauncher() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            uploadCarouselImage(imageUri);
                        }
                    }
                }
        );
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        btnEditCarouselImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        btnDeleteCarouselImage.setOnClickListener(v -> {
            showDeleteCarouselImageDialog();
        });

        btnCancelCentralClub.setOnClickListener(v -> {
            showCancelCentralClubDialog();
        });

        btnDemoteCentralClubs.setOnClickListener(v -> {
            Intent intent = new Intent(SuperAdminSettingsActivity.this, DemoteCentralClubActivity.class);
            startActivity(intent);
        });
    }

    private void loadClubData() {
        if (clubId == null) {
            displayDefaultClubInfo();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.getClub(clubId, new FirebaseManager.ClubCallback() {
            @Override
            public void onSuccess(Club club) {
                progressBar.setVisibility(View.GONE);
                if (club != null) {
                    currentClub = club;
                    displayClubInfo(club);
                } else {
                    displayDefaultClubInfo();
                }
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(SuperAdminSettingsActivity.this,
                        "동아리 정보 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                displayDefaultClubInfo();
            }
        });
    }

    private void displayClubInfo(Club club) {
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.KOREA);

        // Club Type
        tvClubType.setText(club.isCentralClub() ? "중앙동아리" : "일반동아리");
        tvClubType.setBackgroundResource(club.isCentralClub() ?
                R.drawable.badge_central_club : R.drawable.badge_general_club);

        // Budget Info
        tvCurrentBudget.setText(formatter.format(club.getCurrentBudget()) + "원");
        tvTotalBudget.setText("/ " + formatter.format(club.getTotalBudget()) + "원");

        int budgetPercent = club.getBudgetUsagePercent();
        tvBudgetPercent.setText(budgetPercent + "%");

        viewBudgetProgress.post(() -> {
            int parentWidth = ((View) viewBudgetProgress.getParent()).getWidth();
            int progressWidth = (int) (parentWidth * budgetPercent / 100.0f);
            viewBudgetProgress.getLayoutParams().width = progressWidth;
            viewBudgetProgress.requestLayout();
        });

        // Member Info
        int memberCount = club.getMemberCount();
        int targetMembers = club.isCentralClub() ?
                Club.CENTRAL_CLUB_MAINTAIN_MIN_MEMBERS : Club.CENTRAL_CLUB_REGISTER_MIN_MEMBERS;

        tvMemberCount.setText(memberCount + "명 / " + targetMembers + "명");

        int memberPercent = memberCount >= targetMembers ? 100 : (memberCount * 100 / targetMembers);
        tvMemberPercent.setText(memberPercent + "%");

        viewMemberProgress.post(() -> {
            int parentWidth = ((View) viewMemberProgress.getParent()).getWidth();
            int progressWidth = (int) (parentWidth * memberPercent / 100.0f);
            viewMemberProgress.getLayoutParams().width = progressWidth;

            // Color based on status
            if (memberPercent >= 100) {
                viewMemberProgress.setBackgroundResource(R.drawable.member_progress_fill);
            } else if (memberPercent >= 75) {
                viewMemberProgress.setBackgroundResource(R.drawable.member_progress_fill_warning);
            } else {
                viewMemberProgress.setBackgroundResource(R.drawable.member_progress_fill_danger);
            }
            viewMemberProgress.requestLayout();
        });

        // Member Status Message
        if (club.isCentralClub()) {
            if (memberCount >= Club.CENTRAL_CLUB_MAINTAIN_MIN_MEMBERS) {
                tvMemberStatus.setText("중앙동아리 유지 가능");
                tvMemberStatus.setTextColor(getColor(android.R.color.holo_green_dark));
            } else {
                int needed = Club.CENTRAL_CLUB_MAINTAIN_MIN_MEMBERS - memberCount;
                tvMemberStatus.setText(needed + "명 부족 (유지 불가 위험)");
                tvMemberStatus.setTextColor(getColor(android.R.color.holo_red_dark));
            }
        } else {
            if (memberCount >= Club.CENTRAL_CLUB_REGISTER_MIN_MEMBERS) {
                tvMemberStatus.setText("중앙동아리 등록 가능");
                tvMemberStatus.setTextColor(getColor(android.R.color.holo_green_dark));
            } else {
                int needed = Club.CENTRAL_CLUB_REGISTER_MIN_MEMBERS - memberCount;
                tvMemberStatus.setText(needed + "명 더 필요");
                tvMemberStatus.setTextColor(getColor(android.R.color.holo_orange_dark));
            }
        }
    }

    private void displayDefaultClubInfo() {
        tvClubType.setText("중앙동아리");
        tvClubType.setBackgroundResource(R.drawable.badge_central_club);
        tvCurrentBudget.setText("150,000원");
        tvTotalBudget.setText("/ 500,000원");
        tvBudgetPercent.setText("30%");
        tvMemberCount.setText("15명 / 17명");
        tvMemberPercent.setText("88%");
        tvMemberStatus.setText("2명 부족 (유지 불가 위험)");
        tvMemberStatus.setTextColor(getColor(android.R.color.holo_red_dark));
    }

    private void loadCarouselPreview() {
        String title = getIntent().getStringExtra("carousel_title");
        tvCarouselTitle.setText(title != null ? title : clubName);

        if (carouselImageUrl != null && !carouselImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(carouselImageUrl)
                    .centerCrop()
                    .into(ivCarouselPreview);
        } else {
            // Default image based on page index
            int defaultImage;
            switch (pageIndex) {
                case 0:
                    defaultImage = R.drawable.carousel_image_1;
                    break;
                case 1:
                    defaultImage = R.drawable.carousel_image_2;
                    break;
                case 2:
                    defaultImage = R.drawable.carousel_image_3;
                    break;
                default:
                    defaultImage = R.drawable.carousel_image_1;
            }
            ivCarouselPreview.setImageResource(defaultImage);
        }
    }

    private void uploadCarouselImage(Uri imageUri) {
        progressBar.setVisibility(View.VISIBLE);

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] imageData = baos.toByteArray();

            firebaseManager.uploadCarouselImage(imageData, pageIndex, new FirebaseManager.SignatureCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    progressBar.setVisibility(View.GONE);
                    carouselImageUrl = downloadUrl;

                    Glide.with(SuperAdminSettingsActivity.this)
                            .load(downloadUrl)
                            .centerCrop()
                            .into(ivCarouselPreview);

                    Toast.makeText(SuperAdminSettingsActivity.this,
                            "캐러셀 이미지가 업데이트되었습니다", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(Exception e) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(SuperAdminSettingsActivity.this,
                            "이미지 업로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (IOException e) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "이미지 처리 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteCarouselImageDialog() {
        new AlertDialog.Builder(this)
                .setTitle("캐러셀 이미지 삭제")
                .setMessage("캐러셀 이미지를 삭제하시겠습니까?\n기본 이미지로 대체됩니다.")
                .setPositiveButton("삭제", (dialog, which) -> {
                    deleteCarouselImage();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void deleteCarouselImage() {
        progressBar.setVisibility(View.VISIBLE);

        // Delete image from Firebase Storage and update Firestore
        firebaseManager.deleteCarouselImage(pageIndex, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                carouselImageUrl = null;

                // Show default image
                int defaultImage;
                switch (pageIndex) {
                    case 0:
                        defaultImage = R.drawable.carousel_image_1;
                        break;
                    case 1:
                        defaultImage = R.drawable.carousel_image_2;
                        break;
                    case 2:
                        defaultImage = R.drawable.carousel_image_3;
                        break;
                    default:
                        defaultImage = R.drawable.carousel_image_1;
                }
                ivCarouselPreview.setImageResource(defaultImage);

                Toast.makeText(SuperAdminSettingsActivity.this,
                        "캐러셀 이미지가 삭제되었습니다", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(SuperAdminSettingsActivity.this,
                        "이미지 삭제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCancelCentralClubDialog() {
        new AlertDialog.Builder(this)
                .setTitle("중앙동아리 취소")
                .setMessage("이 동아리의 중앙동아리 자격을 취소하시겠습니까?\n\n" +
                        "• 동아리가 일반동아리로 변경됩니다\n" +
                        "• 모든 회원에게 알림이 발송됩니다\n" +
                        "• 이 작업은 되돌릴 수 없습니다")
                .setPositiveButton("취소하기", (dialog, which) -> {
                    cancelCentralClubStatus();
                })
                .setNegativeButton("닫기", null)
                .show();
    }

    private void cancelCentralClubStatus() {
        if (clubId == null) {
            Toast.makeText(this, "동아리 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.cancelCentralClubStatus(clubId, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(SuperAdminSettingsActivity.this,
                        "중앙동아리 자격이 취소되었습니다", Toast.LENGTH_SHORT).show();

                // Reload club data
                loadClubData();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(SuperAdminSettingsActivity.this,
                        "취소 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
