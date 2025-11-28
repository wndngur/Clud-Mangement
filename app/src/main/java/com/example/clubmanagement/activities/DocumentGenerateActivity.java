package com.example.clubmanagement.activities;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.clubmanagement.R;
import com.example.clubmanagement.models.DocumentData;
import com.example.clubmanagement.models.SignatureData;
import com.example.clubmanagement.utils.FirebaseManager;
import com.example.clubmanagement.utils.PdfGenerator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;

public class DocumentGenerateActivity extends AppCompatActivity {

    private AutoCompleteTextView actvDocumentType;
    private TextInputEditText etTitle;
    private TextInputEditText etContent;
    private MaterialCheckBox cbRequireSignature;
    private MaterialButton btnGenerate;
    private ProgressBar progressBar;

    private FirebaseManager firebaseManager;

    private static final String[] DOCUMENT_TYPES = {
            "활동 보고서",
            "회의록",
            "가입 신청서",
            "일반 문서"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_generate);

        firebaseManager = FirebaseManager.getInstance();

        initViews();
        setupDocumentTypeDropdown();
        setupClickListeners();
    }

    private void initViews() {
        actvDocumentType = findViewById(R.id.actvDocumentType);
        etTitle = findViewById(R.id.etTitle);
        etContent = findViewById(R.id.etContent);
        cbRequireSignature = findViewById(R.id.cbRequireSignature);
        btnGenerate = findViewById(R.id.btnGenerate);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupDocumentTypeDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                DOCUMENT_TYPES
        );
        actvDocumentType.setAdapter(adapter);
        actvDocumentType.setText(DOCUMENT_TYPES[0], false);
    }

    private void setupClickListeners() {
        btnGenerate.setOnClickListener(v -> generateDocument());
    }

    private void generateDocument() {
        String documentType = actvDocumentType.getText().toString().trim();
        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String content = etContent.getText() != null ? etContent.getText().toString().trim() : "";
        boolean requiresSignature = cbRequireSignature.isChecked();

        if (title.isEmpty()) {
            Toast.makeText(this, "제목을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (content.isEmpty()) {
            Toast.makeText(this, "내용을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = firebaseManager.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        // 문서 데이터 생성
        DocumentData documentData = new DocumentData(
                null, // docId는 자동 생성
                title,
                documentType,
                content,
                requiresSignature,
                userId
        );

        if (requiresSignature) {
            // 서명이 필요한 경우, 서명 데이터 가져오기
            firebaseManager.getSignatureData(userId, new FirebaseManager.SignatureDataCallback() {
                @Override
                public void onSuccess(SignatureData signatureData) {
                    if (signatureData == null || signatureData.getActiveSignatureUrl() == null) {
                        runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(DocumentGenerateActivity.this,
                                    "서명을 먼저 등록해주세요",
                                    Toast.LENGTH_LONG).show();
                        });
                        return;
                    }

                    // 서명과 함께 PDF 생성
                    createPdf(documentData, signatureData.getActiveSignatureUrl());
                }

                @Override
                public void onFailure(Exception e) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(DocumentGenerateActivity.this,
                                "서명 정보 가져오기 실패: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
                }
            });
        } else {
            // 서명 없이 PDF 생성
            createPdf(documentData, null);
        }
    }

    private void createPdf(DocumentData documentData, String signatureUrl) {
        // PDF 저장 경로
        File documentsDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "ClubManagement"
        );

        if (!documentsDir.exists()) {
            documentsDir.mkdirs();
        }

        String fileName = documentData.getTitle() + "_" + System.currentTimeMillis() + ".pdf";
        File pdfFile = new File(documentsDir, fileName);

        // PDF 생성
        PdfGenerator.generatePdfWithSignature(
                documentData,
                signatureUrl,
                pdfFile,
                new PdfGenerator.PdfGenerationCallback() {
                    @Override
                    public void onSuccess(File pdfFile) {
                        // Firestore에 문서 정보 저장
                        firebaseManager.createDocument(documentData,
                                new FirebaseManager.DocumentCallback() {
                                    @Override
                                    public void onSuccess(DocumentData documentData) {
                                        runOnUiThread(() -> {
                                            showLoading(false);
                                            Toast.makeText(DocumentGenerateActivity.this,
                                                    "문서가 생성되었습니다!\n" + pdfFile.getAbsolutePath(),
                                                    Toast.LENGTH_LONG).show();
                                            finish();
                                        });
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        runOnUiThread(() -> {
                                            showLoading(false);
                                            Toast.makeText(DocumentGenerateActivity.this,
                                                    "문서 정보 저장 실패: " + e.getMessage(),
                                                    Toast.LENGTH_LONG).show();
                                        });
                                    }
                                });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(DocumentGenerateActivity.this,
                                    "PDF 생성 실패: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            btnGenerate.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnGenerate.setEnabled(true);
        }
    }
}
