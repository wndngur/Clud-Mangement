package com.example.clubmanagement.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class ClubDescriptionEditActivity extends BaseActivity {

    private FirebaseManager firebaseManager;
    private String clubId;
    private String clubName;

    private ImageView ivClubImage;
    private LinearLayout llImageOverlay;
    private MaterialButton btnSelectImage;
    private TextInputEditText etClubName;
    private TextInputEditText etDescription;
    private TextInputEditText etActivities;
    private MaterialButton btnSave;
    private ProgressBar progressBar;

    private CarouselItem currentCarouselItem;
    private Uri selectedImageUri;
    private boolean imageChanged = false;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_club_description_edit);

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
        loadCurrentData();
    }

    private void initViews() {
        ivClubImage = findViewById(R.id.ivClubImage);
        llImageOverlay = findViewById(R.id.llImageOverlay);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        etClubName = findViewById(R.id.etClubName);
        etDescription = findViewById(R.id.etDescription);
        etActivities = findViewById(R.id.etActivities);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            imageChanged = true;
                            displaySelectedImage();
                        }
                    }
                }
        );
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void displaySelectedImage() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        Glide.with(ClubDescriptionEditActivity.this)
                .load(selectedImageUri)
                .centerCrop()
                .into(ivClubImage);
        llImageOverlay.setVisibility(View.GONE);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("동아리 설명 수정");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> saveDescription());
        btnSelectImage.setOnClickListener(v -> openGallery());
        ivClubImage.setOnClickListener(v -> openGallery());
        llImageOverlay.setOnClickListener(v -> openGallery());
    }

    private void loadCurrentData() {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.getCarouselItems(new FirebaseManager.CarouselListCallback() {
            @Override
            public void onSuccess(List<CarouselItem> items) {
                progressBar.setVisibility(View.GONE);

                if (items != null) {
                    for (CarouselItem item : items) {
                        if (clubId != null && clubId.equals(item.getClubId())) {
                            currentCarouselItem = item;
                            displayCurrentData(item);
                            return;
                        }
                    }
                }

                // 현재 동아리에 해당하는 캐러셀 아이템이 없으면 기본값 설정
                etClubName.setText(clubName);
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ClubDescriptionEditActivity.this, "데이터 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                etClubName.setText(clubName);
            }
        });
    }

    private void displayCurrentData(CarouselItem item) {
        // Activity가 종료되었는지 확인
        if (isFinishing() || isDestroyed()) {
            return;
        }

        // 동아리 이미지
        if (item.hasFirebaseImage()) {
            Glide.with(ClubDescriptionEditActivity.this)
                    .load(item.getImageUrl())
                    .centerCrop()
                    .placeholder(R.drawable.carousel_image_1)
                    .into(ivClubImage);
            llImageOverlay.setVisibility(View.GONE);
        } else if (item.getImageRes() != 0) {
            ivClubImage.setImageResource(item.getImageRes());
            llImageOverlay.setVisibility(View.GONE);
        }

        // 동아리 이름
        if (item.getClubName() != null && !item.getClubName().isEmpty()) {
            etClubName.setText(item.getClubName());
        } else if (item.getTitle() != null) {
            etClubName.setText(item.getTitle());
        }

        // 동아리 설명
        if (item.getDescription() != null) {
            String description = item.getDescription();
            // 주요 활동이 포함되어 있으면 분리
            if (description.contains("[주요 활동]")) {
                String[] parts = description.split("\\[주요 활동\\]");
                etDescription.setText(parts[0].trim());
                if (parts.length > 1) {
                    etActivities.setText(parts[1].trim());
                }
            } else {
                etDescription.setText(description);
            }
        }
    }

    private void saveDescription() {
        String newClubName = etClubName.getText().toString().trim();
        String newDescription = etDescription.getText().toString().trim();
        String newActivities = etActivities.getText().toString().trim();

        if (newClubName.isEmpty()) {
            Toast.makeText(this, "동아리 이름을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        if (currentCarouselItem == null) {
            // 새 캐러셀 아이템 생성
            currentCarouselItem = new CarouselItem();
            currentCarouselItem.setClubId(clubId);
            currentCarouselItem.setPosition(0);
        }

        currentCarouselItem.setClubName(newClubName);
        currentCarouselItem.setTitle(newClubName);

        // 설명과 주요 활동을 합쳐서 description에 저장
        StringBuilder fullDescription = new StringBuilder();
        if (!newDescription.isEmpty()) {
            fullDescription.append(newDescription);
        }
        if (!newActivities.isEmpty()) {
            if (fullDescription.length() > 0) {
                fullDescription.append("\n\n[주요 활동]\n");
            }
            fullDescription.append(newActivities);
        }
        currentCarouselItem.setDescription(fullDescription.toString());

        // 이미지가 변경되었으면 먼저 이미지 업로드
        if (imageChanged && selectedImageUri != null) {
            uploadImageAndSave();
        } else {
            saveCarouselItem();
        }
    }

    private void uploadImageAndSave() {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] imageData = baos.toByteArray();

            firebaseManager.uploadCarouselImage(clubId, imageData, new FirebaseManager.SignatureCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    currentCarouselItem.setImageUrl(downloadUrl);
                    saveCarouselItem();
                }

                @Override
                public void onFailure(Exception e) {
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    Toast.makeText(ClubDescriptionEditActivity.this, "이미지 업로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (IOException e) {
            progressBar.setVisibility(View.GONE);
            btnSave.setEnabled(true);
            Toast.makeText(this, "이미지 처리 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveCarouselItem() {
        firebaseManager.saveCarouselItem(currentCarouselItem, new FirebaseManager.CarouselCallback() {
            @Override
            public void onSuccess(CarouselItem item) {
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                currentCarouselItem = item;

                Toast.makeText(ClubDescriptionEditActivity.this, "저장되었습니다", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                Toast.makeText(ClubDescriptionEditActivity.this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
