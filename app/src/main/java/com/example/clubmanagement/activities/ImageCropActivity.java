package com.example.clubmanagement.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.clubmanagement.BaseActivity;
import com.example.clubmanagement.R;
import com.example.clubmanagement.views.CropImageView;
import com.google.android.material.button.MaterialButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 이미지 크롭 액티비티
 * 핀치 줌, 드래그로 이미지 위치/크기 조절 후 크롭
 */
public class ImageCropActivity extends BaseActivity {

    public static final String EXTRA_IMAGE_URI = "image_uri";
    public static final String EXTRA_CROPPED_IMAGE_PATH = "cropped_image_path";

    private CropImageView cropImageView;
    private MaterialButton btnReset;
    private MaterialButton btnConfirm;
    private ProgressBar progressBar;

    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_crop);

        initViews();
        setupToolbar();
        loadImage();
        setupListeners();
    }

    private void initViews() {
        cropImageView = findViewById(R.id.cropImageView);
        btnReset = findViewById(R.id.btnReset);
        btnConfirm = findViewById(R.id.btnConfirm);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("사진 편집");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
    }

    private void loadImage() {
        imageUri = getIntent().getParcelableExtra(EXTRA_IMAGE_URI);

        if (imageUri == null) {
            Toast.makeText(this, "이미지를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String scheme = imageUri.getScheme();

        // HTTP/HTTPS URL인 경우 Glide로 로드
        if ("http".equals(scheme) || "https".equals(scheme)) {
            loadImageFromUrl(imageUri.toString());
        } else {
            // 로컬 파일인 경우 기존 방식으로 로드
            loadLocalImage();
        }
    }

    private void loadImageFromUrl(String url) {
        progressBar.setVisibility(View.VISIBLE);

        Glide.with(this)
                .asBitmap()
                .load(url)
                .into(new CustomTarget<Bitmap>(1500, 1500) {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        progressBar.setVisibility(View.GONE);
                        cropImageView.setImageBitmap(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {
                    }

                    @Override
                    public void onLoadFailed(@Nullable android.graphics.drawable.Drawable errorDrawable) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ImageCropActivity.this, "이미지를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void loadLocalImage() {
        try {
            // 이미지 로드 (메모리 효율을 위해 샘플링)
            Bitmap bitmap = decodeSampledBitmapFromUri(imageUri, 1500, 1500);
            if (bitmap != null) {
                cropImageView.setImageBitmap(bitmap);
            } else {
                Toast.makeText(this, "이미지를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "이미지 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private Bitmap decodeSampledBitmapFromUri(Uri uri, int reqWidth, int reqHeight) {
        try {
            // 먼저 이미지 크기만 확인
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;

            InputStream inputStream = getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(inputStream, null, options);
            if (inputStream != null) inputStream.close();

            // 샘플 사이즈 계산
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

            // 실제 디코딩
            options.inJustDecodeBounds = false;
            inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            if (inputStream != null) inputStream.close();

            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private void setupListeners() {
        btnReset.setOnClickListener(v -> cropImageView.resetImage());

        btnConfirm.setOnClickListener(v -> cropAndSave());
    }

    private void cropAndSave() {
        progressBar.setVisibility(View.VISIBLE);
        btnConfirm.setEnabled(false);
        btnReset.setEnabled(false);

        new Thread(() -> {
            try {
                // 크롭된 비트맵 가져오기
                Bitmap croppedBitmap = cropImageView.getCroppedBitmap();

                if (croppedBitmap == null) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnConfirm.setEnabled(true);
                        btnReset.setEnabled(true);
                        Toast.makeText(this, "이미지 크롭 실패", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // 임시 파일로 저장
                File outputFile = new File(getCacheDir(), "cropped_image_" + System.currentTimeMillis() + ".jpg");
                FileOutputStream fos = new FileOutputStream(outputFile);
                croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                fos.close();

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);

                    // 결과 반환
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(EXTRA_CROPPED_IMAGE_PATH, outputFile.getAbsolutePath());
                    setResult(RESULT_OK, resultIntent);
                    finish();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnConfirm.setEnabled(true);
                    btnReset.setEnabled(true);
                    Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }
}
