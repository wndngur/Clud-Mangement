package com.example.clubmanagement.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clubmanagement.R;
import com.example.clubmanagement.models.Club;
import com.example.clubmanagement.models.Member;
import com.example.clubmanagement.utils.ClubApplicationPdfGenerator;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * PDF 신청서 생성 Activity
 */
public class PdfGenerationActivity extends AppCompatActivity {

    private static final int REQUEST_STORAGE_PERMISSION = 100;
    private static final int REQUEST_SIGNATURE = 200;
    private static final int REQUEST_PROFESSOR_SIGNATURE = 201;

    private Toolbar toolbar;
    private ProgressBar progressBar;

    // 기본 정보
    private Spinner spinnerYear;
    private Spinner spinnerType;
    private TextInputEditText etProfessorName;
    private TextInputEditText etPresidentName;
    private TextInputEditText etVicePresidentName;
    private TextInputEditText etSecretaryName;
    private TextInputEditText etTreasurerName;

    // 서명 관리
    private MaterialCardView cardPresidentSign;
    private ImageView ivPresidentSign;
    private TextView tvPresidentSignStatus;
    private MaterialCardView cardProfessorSign;
    private ImageView ivProfessorSign;
    private TextView tvProfessorSignStatus;
    private MaterialButton btnCollectMemberSignatures;
    private TextView tvMemberSignStatus;

    // 회칙 파일
    private MaterialCardView cardClubRules;
    private TextView tvClubRulesStatus;
    private MaterialButton btnSelectRulesFile;

    // 생성 버튼
    private MaterialButton btnGeneratePdf;

    private FirebaseManager firebaseManager;
    private Club currentClub;
    private String clubId;
    private String clubName;
    private List<Member> memberList;

    // 서명 데이터
    private Bitmap presidentSignature;
    private Bitmap professorSignature;
    private Map<String, Bitmap> memberSignatures = new HashMap<>();

    // 회칙 파일 URI
    private Uri clubRulesUri;

    // 월별 활동 계획 및 예산
    private View[] monthPlanViews = new View[12];
    private EditText[] etMonthPlans = new EditText[12];
    private EditText[] etMonthBudgets = new EditText[12];
    private TextView tvTotalBudget;
    private Map<String, String> monthlyPlans = new HashMap<>();
    private Map<String, Long> monthlyBudgets = new HashMap<>();

    // Activity Result Launchers
    private ActivityResultLauncher<Intent> presidentSignatureLauncher;
    private ActivityResultLauncher<Intent> professorSignatureLauncher;
    private ActivityResultLauncher<Intent> memberSignatureLauncher;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private ActivityResultLauncher<Intent> manageStorageLauncher;

    private String pendingMemberId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_generation);

        firebaseManager = FirebaseManager.getInstance();

        // Intent에서 동아리 정보 가져오기
        clubId = getIntent().getStringExtra("club_id");
        clubName = getIntent().getStringExtra("club_name");

        if (clubId == null) {
            Toast.makeText(this, "동아리 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupToolbar();
        setupActivityLaunchers();
        setupSpinners();
        setupListeners();
        loadClubInfo();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progressBar);

        spinnerYear = findViewById(R.id.spinnerYear);
        spinnerType = findViewById(R.id.spinnerType);
        etProfessorName = findViewById(R.id.etProfessorName);
        etPresidentName = findViewById(R.id.etPresidentName);
        etVicePresidentName = findViewById(R.id.etVicePresidentName);
        etSecretaryName = findViewById(R.id.etSecretaryName);
        etTreasurerName = findViewById(R.id.etTreasurerName);

        cardPresidentSign = findViewById(R.id.cardPresidentSign);
        ivPresidentSign = findViewById(R.id.ivPresidentSign);
        tvPresidentSignStatus = findViewById(R.id.tvPresidentSignStatus);
        cardProfessorSign = findViewById(R.id.cardProfessorSign);
        ivProfessorSign = findViewById(R.id.ivProfessorSign);
        tvProfessorSignStatus = findViewById(R.id.tvProfessorSignStatus);
        btnCollectMemberSignatures = findViewById(R.id.btnCollectMemberSignatures);
        tvMemberSignStatus = findViewById(R.id.tvMemberSignStatus);

        cardClubRules = findViewById(R.id.cardClubRules);
        tvClubRulesStatus = findViewById(R.id.tvClubRulesStatus);
        btnSelectRulesFile = findViewById(R.id.btnSelectRulesFile);

        btnGeneratePdf = findViewById(R.id.btnGeneratePdf);

        // 월별 활동 계획 및 예산
        tvTotalBudget = findViewById(R.id.tvTotalBudget);
        initMonthlyPlanViews();
    }

    private void initMonthlyPlanViews() {
        int[] monthPlanIds = {
                R.id.monthPlan1, R.id.monthPlan2, R.id.monthPlan3, R.id.monthPlan4,
                R.id.monthPlan5, R.id.monthPlan6, R.id.monthPlan7, R.id.monthPlan8,
                R.id.monthPlan9, R.id.monthPlan10, R.id.monthPlan11, R.id.monthPlan12
        };

        String[] monthLabels = {"1월", "2월", "3월", "4월", "5월", "6월", "7월", "8월", "9월", "10월", "11월", "12월"};

        for (int i = 0; i < 12; i++) {
            monthPlanViews[i] = findViewById(monthPlanIds[i]);
            if (monthPlanViews[i] != null) {
                TextView tvLabel = monthPlanViews[i].findViewById(R.id.tvMonthLabel);
                etMonthPlans[i] = monthPlanViews[i].findViewById(R.id.etPlan);
                etMonthBudgets[i] = monthPlanViews[i].findViewById(R.id.etBudget);

                if (tvLabel != null) {
                    tvLabel.setText(monthLabels[i]);
                }

                // 예산 입력 시 총합 계산
                final int monthIndex = i;
                if (etMonthBudgets[i] != null) {
                    etMonthBudgets[i].addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {}

                        @Override
                        public void afterTextChanged(Editable s) {
                            calculateTotalBudget();
                        }
                    });
                }
            }
        }
    }

    private void calculateTotalBudget() {
        long total = 0;
        for (int i = 0; i < 12; i++) {
            if (etMonthBudgets[i] != null) {
                String budgetStr = etMonthBudgets[i].getText().toString().trim();
                if (!budgetStr.isEmpty()) {
                    try {
                        total += Long.parseLong(budgetStr);
                    } catch (NumberFormatException e) {
                        // 무시
                    }
                }
            }
        }

        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.KOREA);
        tvTotalBudget.setText(formatter.format(total) + " 원");
    }

    private void collectMonthlyData() {
        monthlyPlans.clear();
        monthlyBudgets.clear();

        for (int i = 0; i < 12; i++) {
            String month = String.valueOf(i + 1);
            if (etMonthPlans[i] != null) {
                String plan = etMonthPlans[i].getText().toString().trim();
                monthlyPlans.put(month, plan);
            }
            if (etMonthBudgets[i] != null) {
                String budgetStr = etMonthBudgets[i].getText().toString().trim();
                long budget = 0;
                if (!budgetStr.isEmpty()) {
                    try {
                        budget = Long.parseLong(budgetStr);
                    } catch (NumberFormatException e) {
                        // 무시
                    }
                }
                monthlyBudgets.put(month, budget);
            }
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("신청서 PDF 생성");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupActivityLaunchers() {
        // 회장 서명 런처
        presidentSignatureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String base64 = result.getData().getStringExtra(SignatureActivity.RESULT_SIGNATURE_BASE64);
                        if (base64 != null) {
                            presidentSignature = SignatureActivity.base64ToBitmap(base64);
                            if (presidentSignature != null) {
                                ivPresidentSign.setImageBitmap(presidentSignature);
                                ivPresidentSign.setVisibility(View.VISIBLE);
                                tvPresidentSignStatus.setText("서명 완료");
                                tvPresidentSignStatus.setTextColor(getColor(android.R.color.holo_blue_dark));
                            }
                        }
                    }
                }
        );

        // 지도교수 서명 런처
        professorSignatureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String base64 = result.getData().getStringExtra(SignatureActivity.RESULT_SIGNATURE_BASE64);
                        if (base64 != null) {
                            professorSignature = SignatureActivity.base64ToBitmap(base64);
                            if (professorSignature != null) {
                                ivProfessorSign.setImageBitmap(professorSignature);
                                ivProfessorSign.setVisibility(View.VISIBLE);
                                tvProfessorSignStatus.setText("서명 완료");
                                tvProfessorSignStatus.setTextColor(getColor(android.R.color.holo_blue_dark));
                            }
                        }
                    }
                }
        );

        // 회원 서명 런처
        memberSignatureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String base64 = result.getData().getStringExtra(SignatureActivity.RESULT_SIGNATURE_BASE64);
                        String memberId = result.getData().getStringExtra(SignatureActivity.RESULT_SIGNER_ID);
                        if (base64 != null && memberId != null) {
                            Bitmap signature = SignatureActivity.base64ToBitmap(base64);
                            if (signature != null) {
                                memberSignatures.put(memberId, signature);
                                updateMemberSignatureStatus();
                            }
                        }
                    }
                }
        );

        // 파일 선택 런처
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        clubRulesUri = result.getData().getData();
                        if (clubRulesUri != null) {
                            // URI 권한 유지
                            getContentResolver().takePersistableUriPermission(
                                    clubRulesUri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                            );
                            tvClubRulesStatus.setText("파일 선택됨");
                            tvClubRulesStatus.setTextColor(getColor(android.R.color.holo_blue_dark));
                        }
                    }
                }
        );

        // 저장소 관리 권한 런처
        manageStorageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (Environment.isExternalStorageManager()) {
                            generatePdf();
                        } else {
                            Toast.makeText(this, "저장소 권한이 필요합니다", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void setupSpinners() {
        // 학년도 스피너
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        List<String> years = new ArrayList<>();
        for (int i = currentYear + 1; i >= currentYear - 2; i--) {
            years.add(String.valueOf(i));
        }
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);

        // 신청 유형 스피너
        String[] types = {"갱신", "신규"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);
    }

    private void setupListeners() {
        // 회장 서명
        cardPresidentSign.setOnClickListener(v -> {
            Intent intent = new Intent(this, SignatureActivity.class);
            intent.putExtra(SignatureActivity.EXTRA_TITLE, "회장 서명");
            intent.putExtra(SignatureActivity.EXTRA_SIGNER_NAME,
                    etPresidentName.getText() != null ? etPresidentName.getText().toString() : "");
            presidentSignatureLauncher.launch(intent);
        });

        // 지도교수 서명
        cardProfessorSign.setOnClickListener(v -> {
            Intent intent = new Intent(this, SignatureActivity.class);
            intent.putExtra(SignatureActivity.EXTRA_TITLE, "지도교수 서명");
            intent.putExtra(SignatureActivity.EXTRA_SIGNER_NAME,
                    etProfessorName.getText() != null ? etProfessorName.getText().toString() : "");
            professorSignatureLauncher.launch(intent);
        });

        // 회원 서명 수집
        btnCollectMemberSignatures.setOnClickListener(v -> {
            if (memberList == null || memberList.isEmpty()) {
                Toast.makeText(this, "회원 목록을 불러오는 중입니다", Toast.LENGTH_SHORT).show();
                return;
            }
            showMemberSignatureDialog();
        });

        // 회칙 파일 선택
        btnSelectRulesFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            String[] mimeTypes = {"application/pdf", "text/plain"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            filePickerLauncher.launch(intent);
        });

        // PDF 생성
        btnGeneratePdf.setOnClickListener(v -> {
            if (validateInput()) {
                checkPermissionAndGenerate();
            }
        });
    }

    private void loadClubInfo() {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.getClub(clubId, new FirebaseManager.ClubCallback() {
            @Override
            public void onSuccess(Club club) {
                currentClub = club;
                if (club != null) {
                    displayClubInfo();
                }
                loadMemberList();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(PdfGenerationActivity.this,
                        "동아리 정보 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayClubInfo() {
        if (currentClub == null) return;

        if (currentClub.getProfessor() != null) {
            etProfessorName.setText(currentClub.getProfessor());
        }
    }

    private void loadMemberList() {
        firebaseManager.getClubMembers(clubId, new FirebaseManager.MembersCallback() {
            @Override
            public void onSuccess(List<Member> members) {
                progressBar.setVisibility(View.GONE);
                memberList = members;

                // 임원 자동 설정
                for (Member member : members) {
                    if (member.getRole() != null) {
                        switch (member.getRole()) {
                            case "회장":
                                etPresidentName.setText(member.getName());
                                break;
                            case "부회장":
                                etVicePresidentName.setText(member.getName());
                                break;
                            case "총무":
                                etSecretaryName.setText(member.getName());
                                break;
                            case "회계":
                                etTreasurerName.setText(member.getName());
                                break;
                        }
                    }
                }

                updateMemberSignatureStatus();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(PdfGenerationActivity.this,
                        "회원 목록 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateMemberSignatureStatus() {
        if (memberList == null) return;

        int signed = memberSignatures.size();
        int total = memberList.size();
        tvMemberSignStatus.setText(signed + "/" + total + " 명 서명 완료");

        if (signed == total && total > 0) {
            tvMemberSignStatus.setTextColor(getColor(android.R.color.holo_blue_dark));
        } else {
            tvMemberSignStatus.setTextColor(getColor(android.R.color.darker_gray));
        }
    }

    private void showMemberSignatureDialog() {
        String[] memberNames = new String[memberList.size()];
        boolean[] signedStatus = new boolean[memberList.size()];

        for (int i = 0; i < memberList.size(); i++) {
            Member member = memberList.get(i);
            boolean isSigned = memberSignatures.containsKey(member.getUserId());
            memberNames[i] = member.getName() + (isSigned ? " (서명 완료)" : "");
            signedStatus[i] = isSigned;
        }

        new AlertDialog.Builder(this)
                .setTitle("회원 서명")
                .setItems(memberNames, (dialog, which) -> {
                    Member selectedMember = memberList.get(which);
                    Intent intent = new Intent(this, SignatureActivity.class);
                    intent.putExtra(SignatureActivity.EXTRA_TITLE, "회원 서명");
                    intent.putExtra(SignatureActivity.EXTRA_SIGNER_NAME, selectedMember.getName());
                    intent.putExtra(SignatureActivity.EXTRA_SIGNER_ID, selectedMember.getUserId());
                    memberSignatureLauncher.launch(intent);
                })
                .setNegativeButton("닫기", null)
                .show();
    }

    private boolean validateInput() {
        if (etPresidentName.getText() == null || etPresidentName.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "회장 이름을 입력해주세요", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (presidentSignature == null) {
            Toast.makeText(this, "회장 서명이 필요합니다", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void checkPermissionAndGenerate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 이상
            if (!Environment.isExternalStorageManager()) {
                new AlertDialog.Builder(this)
                        .setTitle("저장소 권한 필요")
                        .setMessage("PDF 파일을 저장하려면 저장소 관리 권한이 필요합니다.")
                        .setPositiveButton("설정으로 이동", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            manageStorageLauncher.launch(intent);
                        })
                        .setNegativeButton("취소", null)
                        .show();
                return;
            }
        } else {
            // Android 10 이하
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_STORAGE_PERMISSION);
                return;
            }
        }

        generatePdf();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                generatePdf();
            } else {
                Toast.makeText(this, "저장소 권한이 필요합니다", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void generatePdf() {
        progressBar.setVisibility(View.VISIBLE);
        btnGeneratePdf.setEnabled(false);

        // 월별 데이터 수집
        collectMonthlyData();

        new Thread(() -> {
            try {
                ClubApplicationPdfGenerator generator = new ClubApplicationPdfGenerator(this);

                // 기본 정보 설정
                generator.setClub(currentClub);
                generator.setAcademicYear(spinnerYear.getSelectedItem().toString());
                generator.setApplicationType(spinnerType.getSelectedItem().toString());

                // 임원 정보 설정
                String professor = etProfessorName.getText() != null ? etProfessorName.getText().toString() : "";
                String president = etPresidentName.getText() != null ? etPresidentName.getText().toString() : "";
                String vicePresident = etVicePresidentName.getText() != null ? etVicePresidentName.getText().toString() : "";
                String secretary = etSecretaryName.getText() != null ? etSecretaryName.getText().toString() : "";
                String treasurer = etTreasurerName.getText() != null ? etTreasurerName.getText().toString() : "";

                generator.setProfessorName(professor);
                generator.setOfficers(president, vicePresident, secretary, treasurer);

                // 회원 목록 설정
                if (memberList != null) {
                    generator.setMembers(memberList);
                }

                // 서명 설정
                generator.setPresidentSignature(presidentSignature);
                generator.setProfessorSignature(professorSignature);
                generator.setMemberSignatures(memberSignatures);

                // 회칙 파일 설정
                if (clubRulesUri != null) {
                    generator.setClubRulesUri(clubRulesUri);
                }

                // 월별 활동 계획 및 예산 설정
                generator.setMonthlyPlans(monthlyPlans);
                generator.setMonthlyBudgets(monthlyBudgets);

                // PDF 생성
                String filePath = generator.generatePdf();

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnGeneratePdf.setEnabled(true);

                    // 성공 다이얼로그
                    showPdfCompleteDialog(filePath);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnGeneratePdf.setEnabled(true);

                    String errorMsg = e.getMessage();
                    if (errorMsg != null && errorMsg.contains("폰트")) {
                        // 폰트 관련 오류인 경우 상세 안내
                        new AlertDialog.Builder(this)
                                .setTitle("PDF 생성 실패")
                                .setMessage("한글 폰트를 찾을 수 없습니다.\n\n" +
                                        "해결 방법:\n" +
                                        "1. 앱의 assets/fonts/ 폴더에 NanumGothic.ttf 파일을 추가하세요.\n" +
                                        "2. 또는 기기에 한글 폰트가 설치되어 있는지 확인하세요.\n\n" +
                                        "NanumGothic 폰트는 네이버에서 무료로 다운로드할 수 있습니다.")
                                .setPositiveButton("확인", null)
                                .show();
                    } else {
                        Toast.makeText(this, "PDF 생성 실패: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                    e.printStackTrace();
                });
            }
        }).start();
    }

    private void showPdfCompleteDialog(String filePath) {
        String[] options = {"PDF 열기", "카카오톡으로 공유", "다른 앱으로 공유", "닫기"};

        new AlertDialog.Builder(this)
                .setTitle("PDF 생성 완료")
                .setMessage("다운로드 폴더에 저장되었습니다.\n\n" + filePath)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // PDF 열기
                            openPdfFile(filePath);
                            break;
                        case 1: // 카카오톡으로 공유
                            shareToKakaoTalk(filePath);
                            break;
                        case 2: // 다른 앱으로 공유
                            shareToOtherApps(filePath);
                            break;
                        case 3: // 닫기
                            break;
                    }
                })
                .show();
    }

    private void openPdfFile(String filePath) {
        try {
            File file = new File(filePath);
            Uri uri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", file);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "PDF 파일을 열 수 없습니다", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareToKakaoTalk(String filePath) {
        try {
            File file = new File(filePath);
            Uri uri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", file);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // 카카오톡 패키지명 지정
            intent.setPackage("com.kakao.talk");

            try {
                startActivity(intent);
            } catch (android.content.ActivityNotFoundException e) {
                // 카카오톡이 설치되어 있지 않은 경우
                Toast.makeText(this, "카카오톡이 설치되어 있지 않습니다", Toast.LENGTH_SHORT).show();
                // 다른 앱으로 공유 대안 제시
                shareToOtherApps(filePath);
            }
        } catch (Exception e) {
            Toast.makeText(this, "공유 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void shareToOtherApps(String filePath) {
        try {
            File file = new File(filePath);
            Uri uri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", file);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_SUBJECT, currentClub.getName() + " 중앙동아리 신청서");
            intent.putExtra(Intent.EXTRA_TEXT, spinnerYear.getSelectedItem().toString() + "학년도 " +
                    currentClub.getName() + " 중앙동아리 신청서입니다.");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(intent, "PDF 공유하기"));
        } catch (Exception e) {
            Toast.makeText(this, "공유 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
