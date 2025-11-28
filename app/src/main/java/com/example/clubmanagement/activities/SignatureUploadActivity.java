package com.example.clubmanagement.activities;

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

import com.example.clubmanagement.R;
import com.example.clubmanagement.utils.FirebaseManager;
import com.example.clubmanagement.utils.SignatureUtil;
import com.google.android.material.button.MaterialButton;

import java.io.IOException;

public class SignatureUploadActivity extends AppCompatActivity {

    private ImageView ivSignaturePreview;
    private TextView tvNoImage;
    private MaterialButton btnSelectImage;
    private MaterialButton btnUpload;
    private ProgressBar progressBar;

    private FirebaseManager firebaseManager;
    private Bitmap selectedBitmap;

    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signature_upload);

        firebaseManager = FirebaseManager.getInstance();

        initViews();
        setupImagePicker();
        setupClickListeners();
    }

    private void initViews() {
        ivSignaturePreview = findViewById(R.id.ivSignaturePreview);
        tvNoImage = findViewById(R.id.tvNoImage);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnUpload = findViewById(R.id.btnUpload);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        handleImageSelection(uri);
                    }
                }
        );
    }

    private void setupClickListeners() {
        btnSelectImage.setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
        });

        btnUpload.setOnClickListener(v -> {
            uploadSignature();
        });
    }

    private void handleImageSelection(Uri uri) {
        try {
            Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(
                    getContentResolver(), uri);

            // 서명 이미지 처리 (배경 제거, 크롭, 리사이즈)
            selectedBitmap = SignatureUtil.processSignatureImage(originalBitmap);

            // 미리보기 표시
            ivSignaturePreview.setImageBitmap(selectedBitmap);
            tvNoImage.setVisibility(View.GONE);
            btnUpload.setEnabled(true);

            Toast.makeText(this, "이미지가 처리되었습니다", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Toast.makeText(this, "이미지 로드 실패: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void uploadSignature() {
        if (selectedBitmap == null) {
            Toast.makeText(this, "이미지를 먼저 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = firebaseManager.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        // Bitmap -> byte array
        byte[] imageData = SignatureUtil.bitmapToByteArray(selectedBitmap);

        // Firebase Storage에 업로드
        firebaseManager.uploadSignatureImage(imageData, userId, "image",
                new FirebaseManager.SignatureCallback() {
                    @Override
                    public void onSuccess(String downloadUrl) {
                        runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(SignatureUploadActivity.this,
                                    "서명이 업로드되었습니다!",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(SignatureUploadActivity.this,
                                    "업로드 실패: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            btnUpload.setEnabled(false);
            btnSelectImage.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnUpload.setEnabled(selectedBitmap != null);
            btnSelectImage.setEnabled(true);
        }
    }
}
