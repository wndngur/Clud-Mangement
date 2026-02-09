package com.example.clubmanagement.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.clubmanagement.BaseActivity;
import com.example.clubmanagement.R;
import com.example.clubmanagement.models.CarouselItem;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class CarouselEditActivity extends BaseActivity {

    private FirebaseManager firebaseManager;
    private String clubId;
    private String clubName;

    private TextView tvClubName;
    private ImageView ivCurrentPoster;
    private ImageView ivNewPoster;
    private MaterialCardView cardNewImage;
    private MaterialButton btnSelectImage;
    private MaterialButton btnAdjustSize;
    private MaterialButton btnSave;
    private ProgressBar progressBar;

    private Uri selectedImageUri;
    private String croppedImagePath;  // 크롭된 이미지 경로
    private CarouselItem currentCarouselItem;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> imageCropLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carousel_edit);

        firebaseManager = FirebaseManager.getInstance();

        clubId = getIntent().getStringExtra("club_id");
        clubName = getIntent().getStringExtra("club_name");

        if (clubName == null) {
            clubName = "동아리";
        }

        initViews();
        setupToolbar();
        setupImagePicker();
        setupListeners();
        loadCurrentPoster();
    }

    private void initViews() {
        tvClubName = findViewById(R.id.tvClubName);
        ivCurrentPoster = findViewById(R.id.ivCurrentPoster);
        ivNewPoster = findViewById(R.id.ivNewPoster);
        cardNewImage = findViewById(R.id.cardNewImage);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnAdjustSize = findViewById(R.id.btnAdjustSize);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);

        tvClubName.setText(clubName + " 포스터");

        // 초기에는 사이즈 조절 버튼 비활성화 (저장된 이미지가 없으면)
        btnAdjustSize.setEnabled(false);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("포스터 사진변경");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupImagePicker() {
        // 이미지 선택 후 크롭 화면으로 이동
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            // 크롭 화면으로 이동
                            openCropActivity(selectedImageUri);
                        }
                    }
                }
        );

        // 크롭 결과 처리
        imageCropLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        croppedImagePath = result.getData().getStringExtra(ImageCropActivity.EXTRA_CROPPED_IMAGE_PATH);
                        if (croppedImagePath != null) {
                            showCroppedImagePreview();
                        }
                    }
                }
        );
    }

    private void openCropActivity(Uri imageUri) {
        Intent intent = new Intent(this, ImageCropActivity.class);
        intent.putExtra(ImageCropActivity.EXTRA_IMAGE_URI, imageUri);
        imageCropLauncher.launch(intent);
    }

    private void setupListeners() {
        btnSelectImage.setOnClickListener(v -> openGallery());
        btnAdjustSize.setOnClickListener(v -> adjustCurrentPosterSize());
        btnSave.setOnClickListener(v -> saveNewPoster());
    }

    private void adjustCurrentPosterSize() {
        // 현재 저장된 포스터 이미지가 있으면 크롭 화면 열기
        if (currentCarouselItem != null && currentCarouselItem.hasFirebaseImage()) {
            // Firebase 이미지 URL을 Uri로 변환하여 크롭 화면 열기
            Uri imageUri = Uri.parse(currentCarouselItem.getImageUrl());
            openCropActivity(imageUri);
        } else {
            Toast.makeText(this, "먼저 포스터 이미지를 저장해주세요", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void showNewImagePreview() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        cardNewImage.setVisibility(View.VISIBLE);
        Glide.with(CarouselEditActivity.this)
                .load(selectedImageUri)
                .centerCrop()
                .into(ivNewPoster);
        btnSave.setEnabled(true);
    }

    private void showCroppedImagePreview() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        cardNewImage.setVisibility(View.VISIBLE);
        Glide.with(CarouselEditActivity.this)
                .load(new File(croppedImagePath))
                .centerCrop()
                .into(ivNewPoster);
        btnSave.setEnabled(true);
    }

    private void loadCurrentPoster() {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.getCarouselItems(new FirebaseManager.CarouselListCallback() {
            @Override
            public void onSuccess(java.util.List<CarouselItem> items) {
                progressBar.setVisibility(View.GONE);

                if (items != null) {
                    for (CarouselItem item : items) {
                        if (clubId != null && clubId.equals(item.getClubId())) {
                            currentCarouselItem = item;
                            displayCurrentPoster(item);
                            return;
                        }
                    }
                }

                // 현재 동아리에 해당하는 캐러셀 아이템이 없으면 기본 이미지 표시
                ivCurrentPoster.setImageResource(R.drawable.carousel_image_1);
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(CarouselEditActivity.this, "이미지 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                ivCurrentPoster.setImageResource(R.drawable.carousel_image_1);
            }
        });
    }

    private void displayCurrentPoster(CarouselItem item) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (item.hasFirebaseImage()) {
            Glide.with(CarouselEditActivity.this)
                    .load(item.getImageUrl())
                    .centerCrop()
                    .placeholder(R.drawable.carousel_image_1)
                    .into(ivCurrentPoster);
            // Firebase 이미지가 있으면 사이즈 조절 버튼 활성화
            btnAdjustSize.setEnabled(true);
        } else if (item.getImageRes() != 0) {
            ivCurrentPoster.setImageResource(item.getImageRes());
            btnAdjustSize.setEnabled(false);
        } else {
            ivCurrentPoster.setImageResource(R.drawable.carousel_image_1);
            btnAdjustSize.setEnabled(false);
        }
    }

    private void saveNewPoster() {
        // 크롭된 이미지가 있으면 우선 사용, 없으면 원본 이미지 사용
        if (croppedImagePath == null && selectedImageUri == null) {
            Toast.makeText(this, "이미지를 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);
        btnSelectImage.setEnabled(false);

        try {
            Bitmap bitmap;
            if (croppedImagePath != null) {
                // 크롭된 이미지 사용
                bitmap = BitmapFactory.decodeFile(croppedImagePath);
            } else {
                // 원본 이미지 사용 (크롭 안 한 경우)
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
            }

            if (bitmap == null) {
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                btnSelectImage.setEnabled(true);
                Toast.makeText(this, "이미지를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
                return;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
            byte[] imageData = baos.toByteArray();

            // 캐러셀 이미지 업로드
            firebaseManager.uploadCarouselImage(clubId, imageData, new FirebaseManager.SignatureCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    // 캐러셀 아이템 업데이트
                    updateCarouselItem(downloadUrl);

                    // 임시 파일 삭제
                    if (croppedImagePath != null) {
                        new File(croppedImagePath).delete();
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    btnSelectImage.setEnabled(true);
                    Toast.makeText(CarouselEditActivity.this, "이미지 업로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (IOException e) {
            progressBar.setVisibility(View.GONE);
            btnSave.setEnabled(true);
            btnSelectImage.setEnabled(true);
            Toast.makeText(this, "이미지 처리 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateCarouselItem(String imageUrl) {
        if (currentCarouselItem == null) {
            // 새 캐러셀 아이템 생성
            currentCarouselItem = new CarouselItem();
            currentCarouselItem.setClubId(clubId);
            currentCarouselItem.setClubName(clubName);
            currentCarouselItem.setTitle(clubName);
            currentCarouselItem.setDescription(clubName + " 동아리입니다.");
            currentCarouselItem.setPosition(0);
        }

        currentCarouselItem.setImageUrl(imageUrl);

        firebaseManager.saveCarouselItem(currentCarouselItem, new FirebaseManager.CarouselCallback() {
            @Override
            public void onSuccess(CarouselItem item) {
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                btnSelectImage.setEnabled(true);

                Toast.makeText(CarouselEditActivity.this, "포스터가 변경되었습니다", Toast.LENGTH_SHORT).show();

                // 현재 이미지 업데이트
                currentCarouselItem = item;
                displayCurrentPoster(item);

                // 새 이미지 미리보기 숨김
                cardNewImage.setVisibility(View.GONE);
                selectedImageUri = null;
                croppedImagePath = null;
                btnSave.setEnabled(false);
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                btnSelectImage.setEnabled(true);
                Toast.makeText(CarouselEditActivity.this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
