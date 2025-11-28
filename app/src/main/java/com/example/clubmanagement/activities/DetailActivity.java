package com.example.clubmanagement.activities;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import com.example.clubmanagement.adapters.DetailImageAdapter;
import com.example.clubmanagement.models.CarouselItem;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.viewpager2.widget.ViewPager2;

public class DetailActivity extends AppCompatActivity {

    private ImageView ivBack;
    private ImageView ivSuperAdminSettings;
    private TextView tvDetailTitle;
    private TextView tvDetailDescription;
    private LinearLayout llFeatureList;
    private MaterialButton btnAction;
    private FloatingActionButton fabEdit;
    private ProgressBar progressBar;
    private boolean isSuperAdminMode = false;

    // ViewPager2 for image carousel
    private ViewPager2 vpDetailImages;
    private DetailImageAdapter detailImageAdapter;
    private LinearLayout llIndicator;

    private int pageIndex;
    private boolean isAdmin = false;
    private FirebaseManager firebaseManager;
    private CarouselItem currentItem;
    private String clubName;
    private boolean fromClubList = false;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // Intent에서 데이터 받기
        pageIndex = getIntent().getIntExtra("page_index", 0);
        clubName = getIntent().getStringExtra("club_name");
        fromClubList = getIntent().getBooleanExtra("from_club_list", false);

        firebaseManager = FirebaseManager.getInstance();

        initViews();
        setupImagePickerLauncher();
        checkAdminStatus();
        checkSuperAdminMode();

        if (fromClubList && clubName != null) {
            // ClubListActivity에서 온 경우
            setupClubListContent();
        } else {
            // 메인 화면 캐러셀에서 온 경우
            loadCarouselData();
        }

        setupListeners();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        ivSuperAdminSettings = findViewById(R.id.ivSuperAdminSettings);
        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        llFeatureList = findViewById(R.id.llFeatureList);
        btnAction = findViewById(R.id.btnAction);
        fabEdit = findViewById(R.id.fabEdit);
        progressBar = findViewById(R.id.progressBar);

        // ViewPager2 for image carousel
        vpDetailImages = findViewById(R.id.vpDetailImages);
        llIndicator = findViewById(R.id.llIndicator);

        // Initially hide buttons
        fabEdit.setVisibility(View.GONE);
        ivSuperAdminSettings.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);

        // Setup default ViewPager2 adapter to prevent crash
        setupDefaultImages();
    }

    private void checkSuperAdminMode() {
        // 최고 관리자 모드인지 확인
        isSuperAdminMode = com.example.clubmanagement.SettingsActivity.isSuperAdminMode(this);

        // 최고 관리자 모드이고, 캐러셀에서 온 경우 (일반 동아리 목록에서 온 경우 제외)
        if (isSuperAdminMode && !fromClubList) {
            ivSuperAdminSettings.setVisibility(View.VISIBLE);
        }
    }

    private void setupDefaultImages() {
        List<Object> defaultImages = new ArrayList<>();

        // Add default background colors as placeholder
        // Using drawable resource IDs or color placeholders
        switch (pageIndex) {
            case 0:
                defaultImages.add(R.drawable.carousel_image_1);
                break;
            case 1:
                defaultImages.add(R.drawable.carousel_image_2);
                break;
            case 2:
                defaultImages.add(R.drawable.carousel_image_3);
                break;
            default:
                defaultImages.add(R.drawable.carousel_image_1);
                break;
        }

        detailImageAdapter = new DetailImageAdapter(defaultImages);
        vpDetailImages.setAdapter(detailImageAdapter);
    }

    private void setupImagePickerLauncher() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            uploadImage(imageUri);
                        }
                    }
                }
        );
    }

    private void checkAdminStatus() {
        firebaseManager.isCurrentUserAdmin(new FirebaseManager.AdminCheckCallback() {
            @Override
            public void onResult(boolean admin) {
                isAdmin = admin;
                if (isAdmin) {
                    fabEdit.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Exception e) {
                // Not admin or error
                isAdmin = false;
            }
        });
    }

    private void loadCarouselData() {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.getCarouselItemByPosition(pageIndex, new FirebaseManager.CarouselCallback() {
            @Override
            public void onSuccess(CarouselItem item) {
                progressBar.setVisibility(View.GONE);
                currentItem = item;

                if (item != null) {
                    // Load from Firebase
                    displayCarouselItem(item);
                } else {
                    // Load default content
                    setupDefaultContent();
                }
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(DetailActivity.this, "데이터 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                setupDefaultContent();
            }
        });
    }

    private void displayCarouselItem(CarouselItem item) {
        // Set title
        tvDetailTitle.setText(item.getTitle());

        // Set description
        tvDetailDescription.setText(item.getDescription());

        // TODO: Image loading will be implemented with ViewPager2 carousel
        // Temporarily disabled to prevent crash

        // Clear and set features (keeping default for now)
        llFeatureList.removeAllViews();
        setupDefaultFeatures();

        btnAction.setText("가입 신청하기");
    }

    private void setupDefaultContent() {
        switch (pageIndex) {
            case 0:
                setupSignatureSystemContent();
                break;
            case 1:
                setupDocumentManagementContent();
                break;
            case 2:
                setupMemberManagementContent();
                break;
        }
    }

    private int getDefaultColor(int index) {
        switch (index) {
            case 0:
                return 0xFF6200EA; // Purple
            case 1:
                return 0xFF00C853; // Green
            case 2:
                return 0xFFFF6D00; // Orange
            default:
                return 0xFF6200EA;
        }
    }

    private void setupDefaultFeatures() {
        switch (pageIndex) {
            case 0:
                addFeature("✍️ 화면에 직접 서명 작성");
                addFeature("📷 사진으로 서명 업로드");
                addFeature("🔄 자동 배경 제거 처리");
                addFeature("📄 문서에 자동 삽입");
                addFeature("☁️ 클라우드 저장");
                break;
            case 1:
                addFeature("📝 활동 보고서 작성");
                addFeature("📋 회의록 자동 생성");
                addFeature("📄 PDF 문서 변환");
                addFeature("✍️ 서명 자동 삽입");
                addFeature("📤 문서 공유 및 저장");
                break;
            case 2:
                addFeature("👥 부원 명단 관리");
                addFeature("✅ 서명 등록 현황 확인");
                addFeature("📊 활동 이력 조회");
                addFeature("📩 알림 발송");
                addFeature("📈 통계 및 리포트");
                break;
        }
    }

    private void setupSignatureSystemContent() {
        // TODO: 배경색 설정 - ViewPager2로 변경 예정
        // ivDetailImage.setBackgroundColor(0xFF6200EA); // 보라색

        // 제목
        tvDetailTitle.setText("서명 시스템");

        // 설명
        String description = "디지털 서명을 간편하게 생성하고 관리할 수 있는 시스템입니다. " +
                "스마트폰 화면에 직접 서명하거나 사진으로 업로드하여 자동으로 문서에 삽입할 수 있습니다.";
        tvDetailDescription.setText(description);

        // 기능 목록
        addFeature("✍️ 화면에 직접 서명 작성");
        addFeature("📷 사진으로 서명 업로드");
        addFeature("🔄 자동 배경 제거 처리");
        addFeature("📄 문서에 자동 삽입");
        addFeature("☁️ 클라우드 저장");

        // 버튼
        btnAction.setText("가입 신청하기");
    }

    private void setupDocumentManagementContent() {
        // TODO: 배경색 설정 - ViewPager2로 변경 예정
        // ivDetailImage.setBackgroundColor(0xFF00C853); // 초록색

        // 제목
        tvDetailTitle.setText("문서 관리");

        // 설명
        String description = "클럽 활동에 필요한 모든 문서를 한 곳에서 관리하세요. " +
                "활동 보고서, 회의록, 가입 신청서 등을 템플릿을 통해 쉽게 작성하고 PDF로 생성할 수 있습니다.";
        tvDetailDescription.setText(description);

        // 기능 목록
        addFeature("📝 활동 보고서 작성");
        addFeature("📋 회의록 자동 생성");
        addFeature("📄 PDF 문서 변환");
        addFeature("✍️ 서명 자동 삽입");
        addFeature("📤 문서 공유 및 저장");

        // 버튼
        btnAction.setText("가입 신청하기");
    }

    private void setupMemberManagementContent() {
        // TODO: 배경색 설정 - ViewPager2로 변경 예정
        // ivDetailImage.setBackgroundColor(0xFFFF6D00); // 주황색

        // 제목
        tvDetailTitle.setText("부원 관리");

        // 설명
        String description = "클럽 부원들의 정보와 서명 등록 현황을 실시간으로 확인하고 관리할 수 있습니다. " +
                "부원별 활동 이력과 문서 제출 현황을 한눈에 파악하세요.";
        tvDetailDescription.setText(description);

        // 기능 목록
        addFeature("👥 부원 명단 관리");
        addFeature("✅ 서명 등록 현황 확인");
        addFeature("📊 활동 이력 조회");
        addFeature("📩 알림 발송");
        addFeature("📈 통계 및 리포트");

        // 버튼
        btnAction.setText("가입 신청하기");
    }

    private void addFeature(String featureText) {
        TextView featureItem = new TextView(this);
        featureItem.setText(featureText);
        featureItem.setTextSize(16);
        featureItem.setTextColor(getResources().getColor(android.R.color.darker_gray, null));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 24);
        featureItem.setLayoutParams(params);

        llFeatureList.addView(featureItem);
    }

    private void setupListeners() {
        // 뒤로가기 버튼
        ivBack.setOnClickListener(v -> finish());

        // 최고 관리자 설정 버튼
        ivSuperAdminSettings.setOnClickListener(v -> {
            openSuperAdminSettings();
        });

        // 액션 버튼 - 회원가입 화면으로 이동 또는 일반 동아리 가입
        btnAction.setOnClickListener(v -> {
            if (fromClubList) {
                // 일반 동아리 - 바로 가입 처리
                String clubId = getIntent().getStringExtra("club_id");
                if (clubId == null) {
                    Toast.makeText(this, "동아리 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);
                firebaseManager.joinGeneralClub(clubId, clubName, new FirebaseManager.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(DetailActivity.this, clubName + " 동아리에 가입되었습니다!", Toast.LENGTH_LONG).show();

                        // 동아리 페이지로 이동
                        Intent intent = new Intent(DetailActivity.this, ClubMainActivity.class);
                        intent.putExtra("club_name", clubName);
                        intent.putExtra("club_id", clubId);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(DetailActivity.this, "가입 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                // 중앙 동아리 - 회원가입 화면으로 이동
                Intent intent = new Intent(DetailActivity.this, MemberRegistrationActivity.class);
                String clubName = getClubName(pageIndex);
                intent.putExtra("club_name", clubName);
                intent.putExtra("is_central_club", true);
                intent.putExtra("central_club_id", "central_" + pageIndex);
                startActivity(intent);
            }
        });

        // 편집 버튼 (관리자만)
        fabEdit.setOnClickListener(v -> {
            if (isAdmin) {
                showEditDialog();
            }
        });
    }

    private void showEditDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_carousel, null);

        EditText etTitle = dialogView.findViewById(R.id.etTitle);
        EditText etDescription = dialogView.findViewById(R.id.etDescription);
        MaterialButton btnChangeImage = dialogView.findViewById(R.id.btnChangeImage);
        ImageView ivPreview = dialogView.findViewById(R.id.ivPreview);

        // Set current values
        if (currentItem != null) {
            etTitle.setText(currentItem.getTitle());
            etDescription.setText(currentItem.getDescription());

            if (currentItem.hasFirebaseImage()) {
                Glide.with(this)
                        .load(currentItem.getImageUrl())
                        .centerCrop()
                        .into(ivPreview);
            }
        }

        btnChangeImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        builder.setView(dialogView)
                .setTitle("캐러셀 수정")
                .setPositiveButton("저장", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String description = etDescription.getText().toString().trim();

                    if (title.isEmpty() || description.isEmpty()) {
                        Toast.makeText(this, "제목과 설명을 입력해주세요", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    saveCarouselItem(title, description);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void uploadImage(Uri imageUri) {
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
                    Toast.makeText(DetailActivity.this, "이미지 업로드 성공", Toast.LENGTH_SHORT).show();

                    // Update current item with new image URL
                    if (currentItem == null) {
                        currentItem = new CarouselItem();
                        currentItem.setPosition(pageIndex);
                    }
                    currentItem.setImageUrl(downloadUrl);

                    // TODO: Display updated image in ViewPager2
                    // Glide.with(DetailActivity.this)
                    //         .load(downloadUrl)
                    //         .centerCrop()
                    //         .into(ivDetailImage);
                }

                @Override
                public void onFailure(Exception e) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(DetailActivity.this, "이미지 업로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (IOException e) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "이미지 처리 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveCarouselItem(String title, String description) {
        progressBar.setVisibility(View.VISIBLE);

        if (currentItem == null) {
            currentItem = new CarouselItem();
            currentItem.setPosition(pageIndex);
        }

        currentItem.setTitle(title);
        currentItem.setDescription(description);

        firebaseManager.saveCarouselItem(currentItem, new FirebaseManager.CarouselCallback() {
            @Override
            public void onSuccess(CarouselItem item) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(DetailActivity.this, "저장 완료", Toast.LENGTH_SHORT).show();
                currentItem = item;
                displayCarouselItem(item);
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(DetailActivity.this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupClubListContent() {
        // 동아리 목록에서 온 경우의 콘텐츠 설정
        tvDetailTitle.setText(clubName);

        // TODO: 배경색 설정 - ViewPager2로 변경 예정
        // 동아리 이름에 따라 다른 배경색 설정
        // int colorIndex = Math.abs(clubName.hashCode()) % 3;
        // int backgroundColor = getDefaultColor(colorIndex);
        // ivDetailImage.setBackgroundColor(backgroundColor);

        // 동아리 설명 (기본 템플릿)
        String description = clubName + "에 오신 것을 환영합니다! " +
                "우리 동아리에 가입하시려면 아래 정보를 입력해주세요.";
        tvDetailDescription.setText(description);

        // 기능 목록
        llFeatureList.removeAllViews();
        addFeature("📝 회원 가입 신청");
        addFeature("✅ 관리자 승인 대기");
        addFeature("📧 가입 완료 알림");
        addFeature("👥 동아리 활동 시작");

        // 버튼
        btnAction.setText("가입 신청하기");

        // 편집 버튼 숨김 (동아리 목록에서 온 경우)
        fabEdit.setVisibility(View.GONE);
    }

    private String getClubName(int index) {
        // fromClubList가 true이면 전달받은 clubName 사용
        if (fromClubList && clubName != null && !clubName.isEmpty()) {
            return clubName;
        }

        // 메인 화면 캐러셀에서 온 경우
        switch (index) {
            case 0:
                return "서명 시스템";
            case 1:
                return "문서 관리";
            case 2:
                return "부원 관리";
            default:
                return "동아리";
        }
    }

    private void openSuperAdminSettings() {
        Intent intent = new Intent(this, SuperAdminSettingsActivity.class);
        intent.putExtra("page_index", pageIndex);
        intent.putExtra("club_name", getClubName(pageIndex));
        intent.putExtra("club_id", "central_" + pageIndex);
        if (currentItem != null) {
            intent.putExtra("carousel_title", currentItem.getTitle());
            intent.putExtra("carousel_description", currentItem.getDescription());
            intent.putExtra("carousel_image_url", currentItem.getImageUrl());
        }
        startActivity(intent);
    }
}
