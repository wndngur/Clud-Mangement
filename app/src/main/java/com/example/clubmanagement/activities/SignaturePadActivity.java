package com.example.clubmanagement.activities;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.clubmanagement.R;
import com.example.clubmanagement.utils.FirebaseManager;
import com.example.clubmanagement.utils.SignatureUtil;
import com.example.clubmanagement.views.SignaturePadView;
import com.google.android.material.button.MaterialButton;

public class SignaturePadActivity extends AppCompatActivity {

    private SignaturePadView signaturePadView;
    private MaterialButton btnClear;
    private MaterialButton btnSave;
    private ProgressBar progressBar;

    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signature_pad);

        firebaseManager = FirebaseManager.getInstance();

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        signaturePadView = findViewById(R.id.signaturePadView);
        btnClear = findViewById(R.id.btnClear);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        btnClear.setOnClickListener(v -> {
            signaturePadView.clear();
            Toast.makeText(this, "서명이 지워졌습니다", Toast.LENGTH_SHORT).show();
        });

        btnSave.setOnClickListener(v -> {
            saveSignature();
        });
    }

    private void saveSignature() {
        if (!signaturePadView.hasSignature()) {
            Toast.makeText(this, "서명을 먼저 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = firebaseManager.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        // 서명 Bitmap 가져오기
        Bitmap signatureBitmap = signaturePadView.getSignatureBitmap();

        if (signatureBitmap == null) {
            Toast.makeText(this, "서명을 가져올 수 없습니다", Toast.LENGTH_SHORT).show();
            showLoading(false);
            return;
        }

        // Bitmap -> byte array
        byte[] imageData = SignatureUtil.bitmapToByteArray(signatureBitmap);

        // Firebase Storage에 업로드
        firebaseManager.uploadSignatureImage(imageData, userId, "pad",
                new FirebaseManager.SignatureCallback() {
                    @Override
                    public void onSuccess(String downloadUrl) {
                        runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(SignaturePadActivity.this,
                                    "서명이 저장되었습니다!",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(SignaturePadActivity.this,
                                    "저장 실패: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            btnSave.setEnabled(false);
            btnClear.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnSave.setEnabled(true);
            btnClear.setEnabled(true);
        }
    }
}
