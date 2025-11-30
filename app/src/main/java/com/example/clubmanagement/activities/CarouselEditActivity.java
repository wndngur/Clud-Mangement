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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.clubmanagement.R;
import com.example.clubmanagement.models.CarouselItem;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CarouselEditActivity extends AppCompatActivity {

    private FirebaseManager firebaseManager;
    private String clubId;
    private String clubName;

    private TextView tvClubName;
    private ImageView ivCurrentPoster;
    private ImageView ivNewPoster;
    private MaterialCardView cardNewImage;
    private MaterialButton btnSelectImage;
    private MaterialButton btnSave;
    private ProgressBar progressBar;

    private Uri selectedImageUri;
    private CarouselItem currentCarouselItem;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

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
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);

        tvClubName.setText(clubName + " 포스터");
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
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            showNewImagePreview();
                        }
                    }
                }
        );
    }

    private void setupListeners() {
        btnSelectImage.setOnClickListener(v -> openGallery());
        btnSave.setOnClickListener(v -> saveNewPoster());
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
        } else if (item.getImageRes() != 0) {
            ivCurrentPoster.setImageResource(item.getImageRes());
        } else {
            ivCurrentPoster.setImageResource(R.drawable.carousel_image_1);
        }
    }

    private void saveNewPoster() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "이미지를 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);
        btnSelectImage.setEnabled(false);

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] imageData = baos.toByteArray();

            // 캐러셀 이미지 업로드
            firebaseManager.uploadCarouselImage(clubId, imageData, new FirebaseManager.SignatureCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    // 캐러셀 아이템 업데이트
                    updateCarouselItem(downloadUrl);
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
