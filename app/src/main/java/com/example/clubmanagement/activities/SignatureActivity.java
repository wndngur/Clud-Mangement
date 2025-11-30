package com.example.clubmanagement.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.clubmanagement.R;
import com.example.clubmanagement.views.SignatureView;
import com.google.android.material.button.MaterialButton;

import java.io.ByteArrayOutputStream;

/**
 * 서명 캡처 Activity
 * 사용자가 화면에 서명을 그리면 Base64 인코딩된 이미지로 반환
 */
public class SignatureActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE = "signature_title";
    public static final String EXTRA_SIGNER_NAME = "signer_name";
    public static final String EXTRA_SIGNER_ID = "signer_id";
    public static final String RESULT_SIGNATURE_BASE64 = "signature_base64";
    public static final String RESULT_SIGNER_ID = "signer_id";

    private Toolbar toolbar;
    private TextView tvTitle;
    private TextView tvSignerName;
    private SignatureView signatureView;
    private MaterialButton btnClear;
    private MaterialButton btnConfirm;

    private String signerId;
    private String signerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signature);

        // Intent에서 데이터 가져오기
        Intent intent = getIntent();
        String title = intent.getStringExtra(EXTRA_TITLE);
        signerName = intent.getStringExtra(EXTRA_SIGNER_NAME);
        signerId = intent.getStringExtra(EXTRA_SIGNER_ID);

        initViews();
        setupToolbar(title != null ? title : "서명");
        setupListeners();

        if (signerName != null) {
            tvSignerName.setText(signerName + " 님의 서명");
            tvSignerName.setVisibility(View.VISIBLE);
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvTitle = findViewById(R.id.tvTitle);
        tvSignerName = findViewById(R.id.tvSignerName);
        signatureView = findViewById(R.id.signatureView);
        btnClear = findViewById(R.id.btnClear);
        btnConfirm = findViewById(R.id.btnConfirm);
    }

    private void setupToolbar(String title) {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
    }

    private void setupListeners() {
        btnClear.setOnClickListener(v -> {
            signatureView.clear();
        });

        btnConfirm.setOnClickListener(v -> {
            if (signatureView.isEmpty()) {
                Toast.makeText(this, "서명을 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            // 서명을 Base64로 인코딩
            Bitmap signatureBitmap = signatureView.getCroppedSignatureBitmap();
            String base64Signature = bitmapToBase64(signatureBitmap);

            // 결과 반환
            Intent resultIntent = new Intent();
            resultIntent.putExtra(RESULT_SIGNATURE_BASE64, base64Signature);
            if (signerId != null) {
                resultIntent.putExtra(RESULT_SIGNER_ID, signerId);
            }

            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    /**
     * Base64 문자열을 Bitmap으로 변환하는 유틸리티 메서드
     */
    public static Bitmap base64ToBitmap(String base64String) {
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            return null;
        }
    }
}
