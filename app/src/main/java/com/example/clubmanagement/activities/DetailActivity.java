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
import com.example.clubmanagement.models.CarouselItem;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class DetailActivity extends AppCompatActivity {

    private ImageView ivDetailImage;
    private ImageView ivBack;
    private TextView tvDetailTitle;
    private TextView tvDetailDescription;
    private LinearLayout llFeatureList;
    private MaterialButton btnAction;
    private FloatingActionButton fabEdit;
    private ProgressBar progressBar;

    private int pageIndex;
    private boolean isAdmin = false;
    private FirebaseManager firebaseManager;
    private CarouselItem currentItem;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // Intent에서 페이지 인덱스 받기
        pageIndex = getIntent().getIntExtra("page_index", 0);

        firebaseManager = FirebaseManager.getInstance();

        initViews();
        setupImagePickerLauncher();
        checkAdminStatus();
        loadCarouselData();
        setupListeners();
    }

    private void initViews() {
        ivDetailImage = findViewById(R.id.ivDetailImage);
        ivBack = findViewById(R.id.ivBack);
        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        llFeatureList = findViewById(R.id.llFeatureList);
        btnAction = findViewById(R.id.btnAction);
        fabEdit = findViewById(R.id.fabEdit);
        progressBar = findViewById(R.id.progressBar);

        // Initially hide edit button
        fabEdit.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
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

        // Load image
        if (item.hasFirebaseImage()) {
            Glide.with(this)
                    .load(item.getImageUrl())
                    .centerCrop()
                    .into(ivDetailImage);
        } else if (item.getImageRes() != 0) {
            ivDetailImage.setImageResource(item.getImageRes());
        } else {
            // Set background color if available
            if (item.getBackgroundColor() != null && !item.getBackgroundColor().isEmpty()) {
                try {
                    ivDetailImage.setBackgroundColor(Color.parseColor(item.getBackgroundColor()));
                } catch (Exception e) {
                    ivDetailImage.setBackgroundColor(getDefaultColor(pageIndex));
                }
            } else {
                ivDetailImage.setBackgroundColor(getDefaultColor(pageIndex));
            }
        }

        // Clear and set features (keeping default for now)
        llFeatureList.removeAllViews();
        setupDefaultFeatures();

        btnAction.setText("가입하기");
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
        // 배경색 설정
        ivDetailImage.setBackgroundColor(0xFF6200EA); // 보라색

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
        btnAction.setText("가입하기");
    }

    private void setupDocumentManagementContent() {
        // 배경색 설정
        ivDetailImage.setBackgroundColor(0xFF00C853); // 초록색

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
        btnAction.setText("가입하기");
    }

    private void setupMemberManagementContent() {
        // 배경색 설정
        ivDetailImage.setBackgroundColor(0xFFFF6D00); // 주황색

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
        btnAction.setText("가입하기");
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

        // 액션 버튼 - 회원가입 화면으로 이동
        btnAction.setOnClickListener(v -> {
            Intent intent = new Intent(DetailActivity.this, MemberRegistrationActivity.class);
            String clubName = getClubName(pageIndex);
            intent.putExtra("club_name", clubName);
            startActivity(intent);
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

                    // Display updated image
                    Glide.with(DetailActivity.this)
                            .load(downloadUrl)
                            .centerCrop()
                            .into(ivDetailImage);
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

    private String getClubName(int index) {
        // TODO: 나중에 관리자가 수정 가능하도록 변경 필요
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
}
